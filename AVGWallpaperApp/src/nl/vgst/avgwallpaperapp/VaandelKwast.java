package nl.vgst.avgwallpaperapp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class VaandelKwast extends Service {
	public static String VAANDEL_URL = "http://vaandel.vgst.nl/upload/vaandel.jpg";
	public static final int MINIMUM_DOWNLOAD_FREQUENCY = 1000;
	public static final int NOTIFICATION_ID = 42;
	public static final int[] vaandeldimensions = { 176, 144 };

	private int downloadfrequency;

	private boolean backupexisting = true;

	// wallpaper system handle
	private WallpaperManager wpm;
	private NotificationManager nm;

	// handler voor scheduling
	private Handler handler;
	private Runnable downloader;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("VK", "VaandelKwast start op...");
		// lees instellingen uit
		if (intent != null) {
			setDownloadFrequency(intent.getIntExtra("freq",
					MINIMUM_DOWNLOAD_FREQUENCY));
		} else {
			setDownloadFrequency(MINIMUM_DOWNLOAD_FREQUENCY);
		}
		if (backupexisting)
			backupWP();

		// start met het updaten van de wallpaper
		handler = new Handler();
		downloader = new Runnable() {
			public void run() {
				updateWallpaper();
			}
		};
		handler.post(downloader);

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// vraag wallpaper manager op van systeem
		wpm = WallpaperManager.getInstance(this);
		if (wpm == null) {
			throw new RuntimeException("Failed to obtain wallpaper manager");
		}
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (nm == null) {
			throw new RuntimeException("Failed to obtain notification manager");
		}

		// geen updates als scherm uit is geweest
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
					handler.removeCallbacks(downloader);
					updateWallpaper();
				}
			}
		}, new IntentFilter());
		
		//plaats notification
		NotificationCompat.Builder mbuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("AVG Wallpaper App")
				.setContentText("De VaandelKwast haalt het vaandel...")
				.setOngoing(true);
		nm.notify(NOTIFICATION_ID, mbuilder.build());
	}

	@Override
	public void onDestroy() {
		Log.d("VK", "VaandelKwast stopt...");
		restoreWP();
		// verwijder eventuele events die nog in de wachtrij zitten
		handler.removeCallbacks(downloader);
		nm.cancel(NOTIFICATION_ID);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void setDownloadFrequency(int newdf) {
		if (newdf < MINIMUM_DOWNLOAD_FREQUENCY) {
			downloadfrequency = MINIMUM_DOWNLOAD_FREQUENCY;
		} else {
			downloadfrequency = newdf;
		}
	}

	// backup de bestaande wallpaper
	private void backupWP() {

	}

	// zet backup van de wallpaper terug
	private void restoreWP() {
		try {
			wpm.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateWallpaper() {
		// haal asynchroon vaandel op
		new Thread(new Runnable() {
			public void run() {
				try {
					Log.d("VK", "Haal het vaandel...");

					// download het vaandel
					HttpURLConnection conn = (HttpURLConnection) new URL(
							VAANDEL_URL).openConnection();
					conn.setUseCaches(true);
					BufferedInputStream vaandelstream = new BufferedInputStream(
							conn.getInputStream());

					Log.d("VK", "Vaandel opgehaald, stel wallpaper in");
					Bitmap vaandelbitmap = BitmapFactory
							.decodeStream(vaandelstream);
					// schaal plaatje op de breedte, zodat de tijd goed te lezen
					// is
					// double factor =
					// (double)wpm.getDesiredMinimumWidth()/vaandelbitmap.getWidth();
					// vaandelbitmap = Bitmap.createScaledBitmap(vaandelbitmap,
					// (int)(vaandelbitmap.getWidth()*factor),
					// (int)(vaandelbitmap.getWidth()*factor), false);

					// stel de wallpaper opnieuw in
					wpm.setBitmap(vaandelbitmap);
				} catch (IOException e) {
					e.printStackTrace();
					Log.e("VK",
							"Fout bij het instellen van de wallpaper: "
									+ e.getMessage());
				}
			}
		}).start();

		// reschedule
		handler.postDelayed(downloader, downloadfrequency);
	}

}

package nl.vgst.avgwallpaperapp;

import static nl.vgst.avgwallpaperapp.MainActivity.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.*;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class VaandelKwast extends Service {
	private boolean backupexisting = true;

	// wallpaper system handle
	private WallpaperManager wpm;

	// notifications
	private NotificationManager nm;
	private final BroadcastReceiver bcrcv = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				handler.removeCallbacks(downloader);
				updateWallpaper();
			}
		}
	};;

	// handler voor scheduling
	private Handler handler;
	private final Runnable downloader = new Runnable() {
		public void run() {
			updateWallpaper();
		}
	};;

	private SharedPreferences settings;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("VK", "VaandelKwast start op...");
		// lees instellingen uit
		settings = getSharedPreferences(SETTINGS_ID, Context.MODE_PRIVATE);

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
		registerReceiver(bcrcv, new IntentFilter());

		// plaats notification
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
		//stop met notifications behandelen
		nm.cancel(NOTIFICATION_ID);
		unregisterReceiver(bcrcv);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void setDownloadFrequency(int newdf) {
		Editor ed = settings.edit();
		if (newdf < MINIMUM_DOWNLOAD_FREQUENCY) {
			ed.putInt("freq", MINIMUM_DOWNLOAD_FREQUENCY);
		} else {
			ed.putInt("freq", newdf);
		}
		ed.apply();
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
					Bitmap vaandelbitmap = getVaandel();
					// TODO hier schalen

					// stel de wallpaper in
					Log.d("VK", "Vaandel opgehaald, stel wallpaper in");
					if (vaandelbitmap != null) {
						wpm.setBitmap(vaandelbitmap);
					} else {
						Log.e("VK", "Bitmap is niet gegenereerd!");
					}

				} catch (IOException e) {
					e.printStackTrace();
					Log.e("VK",
							"Fout bij het instellen van de wallpaper: "
									+ e.getMessage());
				}
			}
		}).start();

		// reschedule
		handler.postDelayed(downloader,
				settings.getInt("freq", MINIMUM_DOWNLOAD_FREQUENCY));
	}
	
	public static Bitmap getVaandel() throws IOException{
		HttpURLConnection conn = (HttpURLConnection) new URL(
				VAANDEL_URL).openConnection();
		conn.setUseCaches(true);
		BufferedInputStream vaandelstream = new BufferedInputStream(
				conn.getInputStream());
		Bitmap vaandelbitmap = BitmapFactory
				.decodeStream(vaandelstream);
		vaandelstream.close();
		return vaandelbitmap;
	}

}

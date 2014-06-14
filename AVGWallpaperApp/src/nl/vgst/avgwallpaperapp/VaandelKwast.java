package nl.vgst.avgwallpaperapp;

import static nl.vgst.avgwallpaperapp.MainActivity.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class VaandelKwast extends Service {
	private final boolean backupexisting = true;

	// wallpaper system handle
	private WallpaperManager wpm;

	// notifications
	private NotificationManager nm;
	private final BroadcastReceiver bcrcv = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				VaandelKwast.this.handler.removeCallbacks(VaandelKwast.this.downloader);
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

		if (intent != null) {
			setDownloadFrequency(intent.getIntExtra("freq", DEFAULT_DOWNLOAD_FREQUENCY), this.settings);
		} else {
			setDownloadFrequency(DEFAULT_DOWNLOAD_FREQUENCY, this.settings);
		}
		if (this.backupexisting) {
			backupWP();
		}

		// start met het updaten van de wallpaper
		this.handler = new Handler();
		this.handler.post(this.downloader);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// lees instellingen uit
		this.settings = getSharedPreferences(SETTINGS_ID, Context.MODE_PRIVATE);

		// vraag wallpaper manager op van systeem
		this.wpm = WallpaperManager.getInstance(this);
		if (this.wpm == null) {
			throw new RuntimeException("Failed to obtain wallpaper manager");
		}

		// Haal notification manager op
		this.nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (this.nm == null) {
			throw new RuntimeException("Failed to obtain notification manager");
		}

		// geen updates als scherm uit is geweest
		registerReceiver(this.bcrcv, new IntentFilter());

		// plaats notification
		if (getNotificationEnabled(this.settings)) {
			placeNotification(this, this.nm);
		}
	}

	@Override
	public void onDestroy() {
		Log.d("VK", "VaandelKwast stopt...");
		restoreWP();
		// verwijder eventuele events die nog in de wachtrij zitten
		this.handler.removeCallbacks(this.downloader);
		removeNotification(this.nm);
		unregisterReceiver(this.bcrcv);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// backup de bestaande wallpaper
	private void backupWP() {

	}

	// zet backup van de wallpaper terug
	private void restoreWP() {
		try {
			this.wpm.clear();
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
						VaandelKwast.this.wpm.setBitmap(vaandelbitmap);
					} else {
						Log.e("VK", "Bitmap is niet gegenereerd!");
					}

				} catch (IOException e) {
					e.printStackTrace();
					Log.e("VK", "Fout bij het instellen van de wallpaper: " + e.getMessage());
				}
			}
		}).start();

		// reschedule
		this.handler.postDelayed(this.downloader, getDownloadFrequency(this.settings));
	}

	public static Bitmap getVaandel() throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(VAANDEL_URL).openConnection();
		conn.setUseCaches(true);
		BufferedInputStream vaandelstream = new BufferedInputStream(conn.getInputStream());
		Bitmap vaandelbitmap = BitmapFactory.decodeStream(vaandelstream);
		vaandelstream.close();
		return vaandelbitmap;
	}

}

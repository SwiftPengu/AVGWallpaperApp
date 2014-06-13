package nl.vgst.avgwallpaperapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class VaandelKwast extends Service {
	public static String VAANDEL_URL = "http://vaandel.vgst.nl/upload/vaandel.jpg";
	public static final int MINIMUM_DOWNLOAD_FREQUENCY = 1000;
	public static final int[] vaandeldimensions = {176,144};
	
	private int downloadfrequency;

	private boolean backupexisting = true;

	// wallpaper system handle
	private WallpaperManager wpm;

	// handler voor scheduling
	private Handler handler;
	private Runnable downloader;

	// bestand waar wallpaper tijdelijk in wordt opgeslagen
	private File vaandelfiguur;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("VK", "VaandelKwast start op...");
		// lees instellingen uit
		if(intent!=null){
			setDownloadFrequency(intent.getIntExtra("freq", MINIMUM_DOWNLOAD_FREQUENCY));
		}else{
			setDownloadFrequency(MINIMUM_DOWNLOAD_FREQUENCY);
		}
		if (backupexisting)
			backupWP();
		// open het wallpaperbestand
		vaandelfiguur = new File(getExternalCacheDir(), "vaandel.jpg");

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
	}

	@Override
	public void onDestroy() {
		Log.d("VK", "VaandelKwast stopt...");
		restoreWP();
		// verwijder eventuele events die nog in de wachtrij zitten
		handler.removeCallbacks(downloader);
		// TODO verbrand gedownload vaandel
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
					BufferedInputStream vaandelstream = new BufferedInputStream(
							conn.getInputStream());
					
					Log.d("VK", "Vaandel opgehaald, stel wallpaper in");
					Bitmap vaandelbitmap = BitmapFactory.decodeStream(vaandelstream);
					
					// stel de wallpaper opnieuw in
					wpm.setBitmap(vaandelbitmap);
					//fwpm.suggestDesiredDimensions((int)(vaandelbitmap.getWidth()*1.5), (int)(vaandelbitmap.getHeight()*1.5));
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

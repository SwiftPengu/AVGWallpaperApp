package nl.vgst.avgwallpaperapp;

import android.app.*;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainActivity extends Activity {
	private SharedPreferences settings;
	private NotificationManager nm;

	public static final String VAANDEL_URL = "http://vaandel.vgst.nl/upload/vaandel.jpg";
	public static final int MINIMUM_DOWNLOAD_FREQUENCY = 1000;
	public static final int DEFAULT_DOWNLOAD_FREQUENCY = 1500;
	public static final int NOTIFICATION_ID = 42;
	public static final String SETTINGS_ID = "nl.vgst.AVGWallpaperApp.prefs";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// stel settings in
		this.settings = getSharedPreferences(SETTINGS_ID, Context.MODE_PRIVATE);

		// notifications
		this.nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (this.nm == null) {
			throw new RuntimeException("Failed to obtain notification manager");
		}

		// Interval
		// stel standaard frequency in
		final EditText intervaltextfield = ((EditText) findViewById(R.id.main_txt_interval));
		intervaltextfield.setText(getDownloadFrequency(this.settings) + "");

		// gebruik listener om te detecteren wanneer waarde potentieel veranderd
		// is
		intervaltextfield.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			public void afterTextChanged(Editable s) {
				try {
					int value = Integer.parseInt(intervaltextfield.getText().toString());
					// commit settings
					setDownloadFrequency(value, MainActivity.this.settings);
					Log.d("MA", "Saved frequency (" + getDownloadFrequency(MainActivity.this.settings) + ")");
				} catch (NumberFormatException e) {
					// dont crash, but also dont update when the field
					// contains an illegal value
					intervaltextfield.setText(MainActivity.this.settings.getInt("freq", MINIMUM_DOWNLOAD_FREQUENCY) + "");
				}
			}
		});

		// Notification Toggle
		final ToggleButton notificationtoggler = (ToggleButton) findViewById(R.id.main_tog_shownotifications);
		notificationtoggler.setChecked(getNotificationEnabled(this.settings));
		notificationtoggler.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setNotificationsEnabled(isChecked, MainActivity.this.settings);
				if (isChecked) {
					if (isMyServiceRunning(VaandelKwast.class)) {
						placeNotification(MainActivity.this, MainActivity.this.nm);
					}
				} else {
					removeNotification(MainActivity.this.nm);
				}
			}
		});
		;
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateButton();
	}

	// start/stop knop
	public void changeHaalVaandel(View view) {
		Intent vaandelintent = new Intent(this, VaandelKwast.class);
		if (!isMyServiceRunning(VaandelKwast.class)) {
			// start de service
			int freq = -1;
			try {
				// parse commando, service handelt illegale intervallen af
				freq = Integer.parseInt(((TextView) (findViewById(R.id.main_txt_interval))).getText() + "");
			} catch (NumberFormatException e) {
			}
			vaandelintent.putExtra("freq", freq);
			startService(vaandelintent);
		} else {
			// stop de service
			stopService(vaandelintent);
		}
		updateButton();
	}

	public void peekVaandel(View view) {
		Intent gluurintent = new Intent(this, Sleutelgat.class);
		startActivity(gluurintent);
	}

	public void updateButton() {
		if (isMyServiceRunning(VaandelKwast.class)) {
			((Button) findViewById(R.id.main_btn_start)).setText("Stop");
		} else {
			((Button) findViewById(R.id.main_btn_start)).setText("Start");
		}
	}

	// http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-in-android
	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	// settings methods
	//

	public static void setDownloadFrequency(int newdf, SharedPreferences settings) {
		Editor ed = settings.edit();
		if (newdf < MINIMUM_DOWNLOAD_FREQUENCY) {
			ed.putInt("freq", MINIMUM_DOWNLOAD_FREQUENCY);
		} else {
			ed.putInt("freq", newdf);
		}
		ed.apply();
	}

	public static int getDownloadFrequency(SharedPreferences settings) {
		return settings.getInt("freq", DEFAULT_DOWNLOAD_FREQUENCY);
	}

	public static void setNotificationsEnabled(boolean enabled, SharedPreferences settings) {
		Editor ed = settings.edit();
		ed.putBoolean("shownotifications", enabled);
		ed.apply();
	}

	public static boolean getNotificationEnabled(SharedPreferences settings) {
		return settings.getBoolean("shownotifications", true);

	}

	public static void placeNotification(Context ct, NotificationManager nm) {
		NotificationCompat.Builder mbuilder = new NotificationCompat.Builder(ct).setSmallIcon(R.drawable.ic_launcher).setContentTitle("AVG Wallpaper App")
				.setContentText("De VaandelKwast haalt het vaandel...").setOngoing(true);
		nm.notify(NOTIFICATION_ID, mbuilder.build());
	}

	public static void removeNotification(NotificationManager nm) {
		nm.cancel(NOTIFICATION_ID);
	}
}

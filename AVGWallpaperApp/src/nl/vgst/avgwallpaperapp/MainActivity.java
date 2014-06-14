package nl.vgst.avgwallpaperapp;

import android.app.*;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.*;

public class MainActivity extends Activity {
	private SharedPreferences settings;
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
		settings = getSharedPreferences(SETTINGS_ID, Context.MODE_PRIVATE);
		if (!settings.contains("freq")) {
			Editor ed = settings.edit();
			ed.putInt("freq", DEFAULT_DOWNLOAD_FREQUENCY);
			ed.apply();
		}

		// stel standaard frequency in
		final EditText intervaltextfield = ((EditText) findViewById(R.id.main_txt_interval));
		intervaltextfield.setText(settings.getInt("freq",
				MINIMUM_DOWNLOAD_FREQUENCY) + "");

		// gebruik listener om te detecteren wanneer waarde potentieel veranderd
		// is
		intervaltextfield.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// update instellingen alleen als focus verloren wordt
				if (hasFocus == false) {
					try {
						int value = Integer.parseInt(intervaltextfield
								.getText().toString());
						//commit settings
						Editor ed = settings.edit();
						ed.putInt("freq", value);
						ed.apply();
					} catch (NumberFormatException e) {
						// dont crash, but also dont update when the field
						// contains an illegal value
						intervaltextfield.setText(settings.getInt("freq",
								MINIMUM_DOWNLOAD_FREQUENCY) + "");
					}
				}
			}
		});
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
				freq = Integer
						.parseInt(((TextView) (findViewById(R.id.main_txt_interval)))
								.getText() + "");
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
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}

package nl.vgst.avgwallpaperapp;

import static nl.vgst.avgwallpaperapp.MainActivity.SETTINGS_ID;
import static nl.vgst.avgwallpaperapp.MainActivity.getDownloadFrequency;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class Sleutelgat extends Activity {
	private SharedPreferences settings;

	// scheduling
	private Handler handler;
	private final Runnable updater = new Runnable() {
		public void run() {
			new VaandelStandaard().execute();
			// reschedule
			Sleutelgat.this.handler.postDelayed(this, getDownloadFrequency(Sleutelgat.this.settings));
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sleutelgat);
		this.handler = new Handler();
		this.settings = getSharedPreferences(SETTINGS_ID, Context.MODE_PRIVATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.handler.post(this.updater);
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.handler.removeCallbacks(this.updater);
	}

	public void stopgluren(View view) {
		finish();
	}

	/**
	 * Klasse die Bitmap asynchroon ophaalt en instelt in de View
	 * 
	 * @author Rick Hindriks
	 * 
	 */
	private class VaandelStandaard extends AsyncTask<Void, Integer, Bitmap> {
		@Override
		protected Bitmap doInBackground(Void... params) {
			try {
				return VaandelKwast.getVaandel();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e("SG", "Vaandel ophalen ging fout, is het er nog wel?");
			}
			return null;
		}

		@Override
		/**
		 * Deze functie draait op de UI thread, dus geen handler nodig
		 */
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (result != null) {
				// Stel plaatje in
				ImageView imgv = new ImageView(Sleutelgat.this);
				imgv.setImageBitmap(result);
				imgv.setAdjustViewBounds(true);
				// Haal oude plaatje weg
				((LinearLayout) findViewById(R.id.slgt_layout_image)).removeAllViews();
				// Stel nieuwe plaatje in
				((LinearLayout) findViewById(R.id.slgt_layout_image)).addView(imgv);
			}
		}
	}
}

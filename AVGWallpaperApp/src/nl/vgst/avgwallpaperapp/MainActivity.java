package nl.vgst.avgwallpaperapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateButton();
	}
	
	public void startHaalVaandel(View view){
		Intent vaandelintent = new Intent(this,VaandelKwast.class);
		if(!isMyServiceRunning(VaandelKwast.class)){
			//start de service
			int freq = -1;
			try{
				//parse commando, service handelt illegale intervallen af
				freq = Integer.parseInt(((TextView)(findViewById(R.id.main_txt_interval))).getText()+"");
			}catch(NumberFormatException e){}
			vaandelintent.putExtra("freq", freq);
			startService(vaandelintent);
		}else{
			//stop de service
			stopService(vaandelintent);
		}
		updateButton();
	}
	
	public void updateButton(){
		if(isMyServiceRunning(VaandelKwast.class)){
			((Button)findViewById(R.id.main_btn_start)).setText("Stop");
		}else{
			((Button)findViewById(R.id.main_btn_start)).setText("Start");
		}
	}
	
	//http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-in-android
	private boolean isMyServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}

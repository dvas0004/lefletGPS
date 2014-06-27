package me.davidvassallo.vehicletracker;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
  private TextView latituteField;
  private TextView longitudeField;
  private LocationManager locationManager;
  private Context mContext;
  private Intent serviceIntent;

  private WebView mapView = null;
  
//flag for GPS status
  boolean isGPSEnabled = false;

  // flag for network status
  boolean isNetworkEnabled = false;
  
  /**Intent Receiver**/
  private BroadcastReceiver receiver = new BroadcastReceiver() {

	    @Override
	    public void onReceive(Context context, Intent intent) {
	      Bundle bundle = intent.getExtras();
	      if (bundle != null) {
	        String lat = bundle.getString("lat");
	        String lng = bundle.getString("long");
	        
	        
	        latituteField = (TextView) findViewById(R.id.TextView02);
	        longitudeField = (TextView) findViewById(R.id.TextView04);
	        
	        latituteField.setText(lat);
	        longitudeField.setText(lng);
	        
	        Time time = new Time();
	        time.setToNow();
	        
	        if (mapView != null){
	        	mapView.loadUrl("javascript:updateLocation("+lat+","+lng+",\""+time.format("%d-%m-%Y %H:%M:%S")+"\")");
	        }
	        
	        CharSequence text = "GPS Location Updated";
	        int duration = Toast.LENGTH_SHORT;

	        Toast toast = Toast.makeText(context, text, duration);
	        toast.show();
	        
	      }
	    }
	  };
  
  /** Called when the activity is first created. */

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mContext = this;
    
    serviceIntent = new Intent(this, GPSService.class);
    
    latituteField = (TextView) findViewById(R.id.TextView02);
    longitudeField = (TextView) findViewById(R.id.TextView04);
    
    mapView = (WebView) findViewById(R.id.mapView);
    mapView.getSettings().setJavaScriptEnabled(true);
    mapView.loadUrl("file:///android_asset/index.html");


    locationManager = (LocationManager) mContext
            .getSystemService(LOCATION_SERVICE);

    // getting GPS status
    isGPSEnabled = locationManager
            .isProviderEnabled(LocationManager.GPS_PROVIDER);

    // getting network status
    isNetworkEnabled = locationManager
            .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

    if (!isGPSEnabled) {
    	DialogFragment confirmGPS = new GPSSettingsDialogFragment();
    	confirmGPS.show(getFragmentManager(), "gpsConfirm1");
    } else {
        // if GPS Enabled get lat/long using GPS Services
        if (isGPSEnabled) {
        	
        	latituteField.setText("Location not available");
            longitudeField.setText("Location not available");
            
            //here start service
            startService(serviceIntent);
            
        } else {
        	DialogFragment confirmGPS = new GPSSettingsDialogFragment();
        	confirmGPS.show(getFragmentManager(), "gpsConfirm2");
        }
     }
    
    Button gpsStopBtn = (Button) findViewById(R.id.stopService);
	gpsStopBtn.setOnClickListener(new View.OnClickListener() {
		            
		            public void onClick(View view) {
		                //here stop service
		            	stopService(serviceIntent);
		            }
		            
	});
	
  }

  /* Request updates at startup */
  @Override
  protected void onResume() {
    super.onResume();
    registerReceiver(receiver, new IntentFilter(GPSService.COORDSNOTIFICATION));
  }

  /* Remove the locationlistener updates when Activity is paused */
  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(receiver);
  }
  
  
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
    /**
     * Function to show settings alert dialog
     * */
	public class GPSSettingsDialogFragment extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setMessage("GPS is not enabled. Do you want to go to settings menu?")
	               .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog,int which) {
	                       Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	                       mContext.startActivity(intent);
	                   }
	               })
	               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which) {
			            dialog.cancel();
			            }
			        });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}
	

}


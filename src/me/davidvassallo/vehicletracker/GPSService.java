package me.davidvassallo.vehicletracker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import me.davidvassallo.vehicletracker.MainActivity.GPSSettingsDialogFragment;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.os.Process;

public class GPSService extends Service {
	  private Looper mServiceLooper;
	  private ServiceHandler mServiceHandler;
	  private Location location;
	  private LocationManager locationManager;
	  
	  public static final String COORDSNOTIFICATION = "me.davidvassallo.vehicletracker.service.receiver";
	  
	  // Send the notification.
      // Sets an ID for the notification
      int mNotificationId = 001;
      // Gets an instance of the NotificationManager service
      NotificationManager mNotifyMgr;
	  
	  
	  // The minimum distance to change Updates in meters
	  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5 meters

	  // The minimum time between updates in milliseconds
	  private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

	  // Handler that receives messages from the thread
	  private final class ServiceHandler extends Handler implements LocationListener {
	      public ServiceHandler(Looper looper) {
	          super(looper);
	      }
	      @Override
	      public void handleMessage(Message msg) {
	    	// if GPS Enabled get lat/long using GPS Services
	    	  
	    	  
	    	  	  if (msg.arg1==0){
	    	  		locationManager.removeUpdates((LocationListener) this);
	    	  	  }else {
	          
		              if (location == null) {
		            	  locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		                  locationManager.requestLocationUpdates(
		                          LocationManager.GPS_PROVIDER,
		                          MIN_TIME_BW_UPDATES,
		                          MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
		                  Log.d("GPS Enabled", "GPS Enabled");
		                  if (locationManager != null) {
		                      location = locationManager
		                              .getLastKnownLocation(LocationManager.GPS_PROVIDER);
		                   // Initialize the location fields
		                      if (location != null) {
		                        onLocationChanged(location);
		                      } 
		                  }
		              }
	    	  	  }
	         
	      }
		@Override
		public void onLocationChanged(Location location) {
			
			double lat = (double) (location.getLatitude());
		    double lng = (double) (location.getLongitude());

		    
		        try {
		        	FileWriter fstream = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/coOrds.txt",true);
		            BufferedWriter fbw = new BufferedWriter(fstream);
		            fbw.write(String.valueOf(lng)+","+String.valueOf(lat)+",0");
		            fbw.newLine();
		            fbw.close();
		        }
		        catch (IOException e) {
		            Log.e("Exception", "File write failed: " + e.toString());
		        } 
		    
		    Intent i = new Intent(COORDSNOTIFICATION);  
		    i.putExtra("lat", String.valueOf(lat));
		    i.putExtra("long", String.valueOf(lng));
		    
		    sendBroadcast(i);
		    			
		}
		@Override
		public void onProviderDisabled(String provider) {
			Log.e("Worker Thread", "Disabled provider " + provider);
			
		}
		@Override
		public void onProviderEnabled(String provider) {
			Log.e("Worker Thread", "Enabled provider " + provider);
			
		}
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
	  }

	  @Override
	  public void onCreate() {
		 
		mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		  
		// Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
		  
	    // Start up the thread running the service.  Note that we create a
	    // separate thread because the service normally runs in the process's
	    // main thread, which we don't want to block.  We also make it
	    // background priority so CPU-intensive work will not disrupt our UI.
	    HandlerThread thread = new HandlerThread("ServiceStartArguments",
	            Process.THREAD_PRIORITY_BACKGROUND);
	    thread.start();

	    
	    
	    // Get the HandlerThread's Looper and use it for our Handler
	    mServiceLooper = thread.getLooper();
	    mServiceHandler = new ServiceHandler(mServiceLooper);
	  }

	  @Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
	      Toast.makeText(this, "GPS service starting", Toast.LENGTH_SHORT).show();

	      // For each start request, send a message to start a job and deliver the
	      // start ID so we know which request we're stopping when we finish the job
	      Message msg = mServiceHandler.obtainMessage();
	      msg.arg1 = 1;
	      mServiceHandler.sendMessage(msg);

	      // If we get killed, after returning from here, restart
	      return START_STICKY;
	  }


	    /**
	     * Class for clients to access.  Because we know this service always
	     * runs in the same process as its clients, we don't need to deal with
	     * IPC.
	     */
	    public class LocalBinder extends Binder {
	        GPSService getService() {
	            return GPSService.this;
	        }
	    }


	    @Override
	    public void onDestroy() {
	    	
	    	Message msg = mServiceHandler.obtainMessage();
		      msg.arg1 = 0;
		      mServiceHandler.sendMessage(msg);
			
	        // Cancel the persistent notification.
	    	mNotifyMgr.cancel(mNotificationId);

	        // Tell the user we stopped.
	        Toast.makeText(this, "GPS service ended", Toast.LENGTH_SHORT).show();
	    }

	    @Override
	    public IBinder onBind(Intent intent) {
	        return null;
	    }

	    /**
	     * Show a notification while this service is running.
	     */
	    private void showNotification() {

	        // Set the icon, scrolling text and timestamp
	        Builder mBuilder =
	        	    new Notification.Builder(this)
	        .setSmallIcon(R.drawable.ic_launcher)
	        .setContentTitle("GPS Service")
	        .setContentText("GPS Service Running");

	        Intent resultIntent = new Intent(this, MainActivity.class);

	        // Because clicking the notification opens a new ("special") activity, there's
	        // no need to create an artificial back stack.
	        PendingIntent resultPendingIntent =
	            PendingIntent.getActivity(
	            this,
	            0,
	            resultIntent,
	            PendingIntent.FLAG_UPDATE_CURRENT
	        );
	        
	        mBuilder.setContentIntent(resultPendingIntent);

	        
	        // Builds the notification and issues it.
	        mNotifyMgr.notify(mNotificationId, mBuilder.build());
	    }

}
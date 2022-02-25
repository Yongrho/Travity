package com.travity.ui.workout;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class LocationServiceController extends Application {
    private static LocationServiceController mInstance;
    private LocationService locationService;
    private Intent intentLocationService;

    public static synchronized LocationServiceController getInstance() {
        if (mInstance == null) {
            mInstance = new LocationServiceController();
        }
        return mInstance;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        // Setup singleton instance
        mInstance = this;
    }
    public void startService(Context context){
        //start your service
        intentLocationService = new Intent(context, LocationService.class);
        context.startService(intentLocationService);
        context.bindService(intentLocationService, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    public void stopService(Context context){
        //stop service
        context.stopService(intentLocationService);
    }
    public LocationService getService() {
        return locationService;
    }

    // After the splash screen is implemented, serviceConnection should be move to it.
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            String name = className.getClassName();

            if (name.endsWith("LocationService")) {
                locationService = ((com.travity.ui.workout.LocationService.LocationServiceBinder) service).getService();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            if (className.getClassName().equals("LocationService")) {
                locationService = null;
            }
        }
    };
}
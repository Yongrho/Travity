/**
 * https://github.Com/mizutori/AndroidLocationStarterKit
 */
package com.travity.ui.workout;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.SystemClock;
//import android.support.v4.content.LocalBroadcastManager;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

//import androidx.annotation.RequiresApi;

import com.travity.data.WorkoutIntervalData;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class LocationService extends Service implements LocationListener, GpsStatus.Listener {
    public static final String LOG_TAG = LocationService.class.getSimpleName();

    private final LocationServiceBinder binder = new LocationServiceBinder();
    boolean isLocationManagerUpdatingLocation;

    ArrayList<Location> locationList;

    ArrayList<Location> oldLocationList;
    ArrayList<Location> noAccuracyLocationList;
    ArrayList<Location> inaccurateLocationList;
    ArrayList<Location> kalmanNGLocationList;

    boolean isLogging;
    float currentSpeed = 0.0f; // meters/second

    KalmanLatLong kalmanFilter;
    long runStartTimeInMillis;

    ArrayList<Integer> batteryLevelArray;
    ArrayList<Float> batteryLevelScaledArray;
    int batteryScale;
    int gpsCount;

    ArrayList<WorkoutIntervalData> splitsList;
    long createTime;

    public LocationService() {
    }

    @Override
    public void onCreate() {
        isLocationManagerUpdatingLocation = false;
        locationList = new ArrayList<>();
        noAccuracyLocationList = new ArrayList<>();
        oldLocationList = new ArrayList<>();
        inaccurateLocationList = new ArrayList<>();
        kalmanNGLocationList = new ArrayList<>();
        kalmanFilter = new KalmanLatLong(3);

        splitsList = new ArrayList<WorkoutIntervalData>();

        isLogging = false;

        batteryLevelArray = new ArrayList<>();
        batteryLevelScaledArray = new ArrayList<>();
        registerReceiver(this.batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        super.onStartCommand(i, flags, startId);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public void onRebind(Intent intent) {
        Log.d(LOG_TAG, "onRebind ");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind ");
        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy ");
    }

    //This is where we detect the app is being killed, thus stop service.
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(LOG_TAG, "onTaskRemoved ");
        this.stopUpdatingLocation();
        stopSelf();
    }

    /**
     * Binder class
     *
     * @author Takamitsu Mizutori
     *
     */
    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    /* LocationListener implementation */
    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            notifyLocationProviderStatusUpdated(false);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            notifyLocationProviderStatusUpdated(true);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                notifyLocationProviderStatusUpdated(false);
            } else {
                notifyLocationProviderStatusUpdated(true);
            }
        }
    }

    /* GpsStatus.Listener implementation */
    public void onGpsStatusChanged(int event) {
    }

    private void notifyLocationProviderStatusUpdated(boolean isLocationProviderAvailable) {
        //Broadcast location provider status change here
    }

    public void startLogging(){
        isLogging = true;
    }

    long elapsedTimeInSeconds = 0;

    public void stopLogging(){
        if (locationList.size() > 1) { // && batteryLevelArray.size() > 1){
            long currentTimeInMillis = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                currentTimeInMillis = (long)(SystemClock.elapsedRealtimeNanos() / 1000000);
            }
//            long elapsedTimeInSeconds = (currentTimeInMillis - runStartTimeInMillis) / 1000;
            elapsedTimeInSeconds = (currentTimeInMillis - runStartTimeInMillis) / 1000;

            float totalDistanceInMeters = 0;
            for(int i = 0; i < locationList.size() - 1; i++){
                totalDistanceInMeters +=  locationList.get(i).distanceTo(locationList.get(i + 1));
            }

            int batteryLevelStart = 100;
            int batteryLevelEnd = 80;

            float batteryLevelScaledStart = 1.0f;
            float batteryLevelScaledEnd = 1.0f;
/*
            int batteryLevelStart = batteryLevelArray.get(0).intValue();
            int batteryLevelEnd = batteryLevelArray.get(batteryLevelArray.size() - 1).intValue();

            float batteryLevelScaledStart = batteryLevelScaledArray.get(0).floatValue();
            float batteryLevelScaledEnd = batteryLevelScaledArray.get(batteryLevelScaledArray.size() - 1).floatValue();
*/
//            saveLog(elapsedTimeInSeconds, totalDistanceInMeters, gpsCount, batteryLevelStart, batteryLevelEnd, batteryLevelScaledStart, batteryLevelScaledEnd);
        }
        isLogging = false;
    }

    public void startUpdatingLocation() {
        if(this.isLocationManagerUpdatingLocation == false){
            Log.v(TAG, "startUpdatingLocation.....");

            oldLocation = null;
            workoutDistance = 0;
            intervalDistance = 0;
            intervalKilometers = 1000;
            workoutTime = 0;
            oldWorkoutTime = 0;
            previousTime = 0;
            elapsedTimeInSeconds = 0;

            isLocationManagerUpdatingLocation = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                runStartTimeInMillis = (long)(SystemClock.elapsedRealtimeNanos() / 1000000);
            }

            locationList.clear();

            oldLocationList.clear();
            noAccuracyLocationList.clear();
            inaccurateLocationList.clear();
            kalmanNGLocationList.clear();

            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            //Exception thrown when GPS or Network provider were not available on the user's device.
            try {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE); //setAccuracyは内部では、https://stackoverflow.Com/a/17874592/1709287の用にHorizontalAccuracyの設定に変換されている。
                    criteria.setPowerRequirement(Criteria.POWER_HIGH);
                criteria.setAltitudeRequired(false);
                criteria.setSpeedRequired(true);
                criteria.setCostAllowed(true);
                criteria.setBearingRequired(false);

                //API level 9 and up
                criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
                criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
                //criteria.setBearingAccuracy(Criteria.ACCURACY_HIGH);
                //criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);

                Integer gpsFreqInMillis = 5000;
                Integer gpsFreqInDistance = 5;  // in meters

                locationManager.addGpsStatusListener(this);
                locationManager.requestLocationUpdates(gpsFreqInMillis, gpsFreqInDistance, criteria, this, null);

                /* Battery Consumption Measurement */
                gpsCount = 0;
                batteryLevelArray.clear();
                batteryLevelScaledArray.clear();
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, e.getLocalizedMessage());
            } catch (SecurityException e) {
                Log.e(LOG_TAG, e.getLocalizedMessage());
            } catch (RuntimeException e) {
                Log.e(LOG_TAG, e.getLocalizedMessage());
            }
        }
    }

    public void stopUpdatingLocation(){
        if(this.isLocationManagerUpdatingLocation == true){
            Log.v(TAG, "stopUpdatingLocation.....");
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.removeUpdates(this);
            isLocationManagerUpdatingLocation = false;
        }
    }

    public long getElapsedTimeInSeconds() {
        return (long) workoutTime;
    }

    @Override
    public void onLocationChanged(final Location newLocation) {
        Log.d(TAG, "(" + newLocation.getLatitude() + "," + newLocation.getLongitude() + ")");
        gpsCount++;

        if (isLogging){
            //locationList.add(newLocation);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (filterAndAddLocation(newLocation) == false) {
                    return;
                }
            }
        }

        Intent intent = new Intent("LocationUpdated");
        intent.putExtra("location", newLocation);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
    }

    @SuppressLint("NewApi")
    private long getLocationAge(Location newLocation){
        long locationAge;
        if(android.os.Build.VERSION.SDK_INT >= 17) {
            long currentTimeInMilli = (long)(SystemClock.elapsedRealtimeNanos() / 1000000);
            long locationTimeInMilli = (long)(newLocation.getElapsedRealtimeNanos() / 1000000);
            locationAge = currentTimeInMilli - locationTimeInMilli;
        }else{
            locationAge = System.currentTimeMillis() - newLocation.getTime();
        }
        return locationAge;
    }

    private boolean filterAndAddLocation(Location location){
        long age = getLocationAge(location);

        if(age > 5 * 1000){ //more than 5 seconds
            Log.d(TAG, "Location is old");
            oldLocationList.add(location);
            return false;
        }

        if(location.getAccuracy() <= 0){
            Log.d(TAG, "Latitidue and longitude values are invalid.");
            noAccuracyLocationList.add(location);
            return false;
        }

        //setAccuracy(newLocation.getAccuracy());
        float horizontalAccuracy = location.getAccuracy();
        if(horizontalAccuracy > 10){ //10meter filter
            Log.d(TAG, "Accuracy is too low.");
            inaccurateLocationList.add(location);
            return false;
        }

        float Qvalue;

        long locationTimeInMillis = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            locationTimeInMillis = (long)(location.getElapsedRealtimeNanos() / 1000000);
        }
        long elapsedTimeInMillis = locationTimeInMillis - runStartTimeInMillis;
        Log.d(TAG, "elapsedTimeInMillis: " + elapsedTimeInMillis);

        if(currentSpeed == 0.0f){
            Qvalue = 3.0f; //3 meters per second
        }else{
            Qvalue = currentSpeed; // meters per second
        }

        kalmanFilter.Process(location.getLatitude(), location.getLongitude(), location.getAccuracy(), elapsedTimeInMillis, Qvalue);
        double predictedLat = kalmanFilter.get_lat();
        double predictedLng = kalmanFilter.get_lng();

        Location predictedLocation = new Location("");//provider name is unecessary
        predictedLocation.setLatitude(predictedLat);//your coords of course
        predictedLocation.setLongitude(predictedLng);
        float predictedDeltaInMeters =  predictedLocation.distanceTo(location);

        if (predictedDeltaInMeters > 60){
            Log.d(TAG, "Kalman Filter detects mal GPS, we should probably remove this from track");
            kalmanFilter.consecutiveRejectCount += 1;

            if(kalmanFilter.consecutiveRejectCount > 3){
                kalmanFilter = new KalmanLatLong(3); //reset Kalman Filter if it rejects more than 3 times in raw.
             }

             kalmanNGLocationList.add(location);
             return false;
        }else{
            kalmanFilter.consecutiveRejectCount = 0;
        }

        // Notify predicted location to UI
        Intent intent = new Intent("PredictLocation");
        intent.putExtra("location", predictedLocation);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);

        Log.d(TAG, "Location quality is good enough.");
        intervalRecord(location, elapsedTimeInMillis);
        currentSpeed = location.getSpeed();
        locationList.add(location);

        if (elapsedTimeInMillis < 0) {
            return false;
        }
        return true;
    }

    /* Battery Consumption */
    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryLevelScaled = batteryLevel / (float)scale;

            batteryLevelArray.add(Integer.valueOf(batteryLevel));
            batteryLevelScaledArray.add(Float.valueOf(batteryLevelScaled));
            batteryScale = scale;
        }
    };

    /* Data Logging */
    public synchronized void saveLog(long timeInSeconds, double distanceInMeters, int gpsCount, int batteryLevelStart, int batteryLevelEnd, float batteryLevelScaledStart, float batteryLevelScaledEnd) {
        SimpleDateFormat fileNameDateTimeFormat = new SimpleDateFormat("yyyy_MMdd_HHmm");
        String filePath = this.getExternalFilesDir(null).getAbsolutePath() + "/"
                + fileNameDateTimeFormat.format(new Date()) + "_battery" + ".csv";

        Log.d(TAG, "saving to " + filePath);
/*
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filePath, false);
            fileWriter.append("Time,Distance,GPSCount,BatteryLevelStart,BatteryLevelEnd,BatteryLevelStart(/" + batteryScale + ")," + "BatteryLevelEnd(/" + batteryScale + ")" + "\n");
            String record = "" + timeInSeconds + ',' + distanceInMeters + ',' + gpsCount + ',' + batteryLevelStart + ',' + batteryLevelEnd + ',' + batteryLevelScaledStart + ',' + batteryLevelScaledEnd + '\n';
            fileWriter.append(record);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
 */
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void saveLog(String bmpFilePath, float totalDistanceInKiloMeters, float elapsedTimeInSeconds) {
        SimpleDateFormat fileNameDateTimeFormat = new SimpleDateFormat("yyyy_MMdd_HHmm");
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/"
                + fileNameDateTimeFormat.format(new Date()) + ".csv";
//        String filePath = this.getExternalFilesDir(null).getAbsolutePath() + "/"
//                + fileNameDateTimeFormat.format(new Date()) + "_battery" + ".csv";

        Log.d(TAG, "saving to " + filePath);

        String timeInMinutes = TimeUtil.getDurationString((int) workoutTime);
        float paceInSecondsKilometers = elapsedTimeInSeconds / totalDistanceInKiloMeters;
        String paceInMinutesKilometers = TimeUtil.getDurationString((int) paceInSecondsKilometers);

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filePath, false);
            String record = "" + bmpFilePath + "," + String.format("%.2f", totalDistanceInKiloMeters) + ',' + timeInMinutes + ',' + paceInMinutesKilometers + '\n';
            fileWriter.append(record);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    private Location oldLocation = null;
    private double workoutDistance = 0;
    private double intervalDistance = 0;
    private int intervalKilometers = 1000;
    private double workoutTime = 0;
    private double oldWorkoutTime = 0;
    private double previousTime = 0;
//    private final ArrayList<IntervalData> intervalRecords = new ArrayList<IntervalData>();
    private static final int ONE_MINUTE = 60;
    private static final int ONE_KILOMETER = 1000;

    private void intervalRecord(Location location, long elapsedTime) {
        if (oldLocation != null)  {
            workoutTime = elapsedTime / 1000;
            // calculate distance & average pace
            double distance = location.distanceTo(oldLocation);

            Log.v(TAG, "workoutDistance = " + workoutDistance);
            Log.v(TAG, "distance = " + distance);

            if (workoutDistance + distance > intervalKilometers) {
                double ratio = (intervalKilometers - workoutDistance) / distance;
                double gapTime = (workoutTime - oldWorkoutTime) * ratio;
                double accumulatedTime = oldWorkoutTime + gapTime;
                double intervalTime = accumulatedTime - previousTime;
                String averagePaceKilometer = TimeUtil.getDurationString((int) intervalTime);
                previousTime += intervalTime;
                Log.v(TAG, "ratio = " + ratio);
                Log.v(TAG, "workoutTime = " + workoutTime);
                Log.v(TAG, "oldWorkoutTime = " + oldWorkoutTime);
                Log.v(TAG, "gapTime = " + gapTime);
                Log.v(TAG, "accumulatedTime = " + accumulatedTime);
                Log.v(TAG, "intervalTime = " + intervalTime);
                Log.v(TAG, "previousTime = " + previousTime);
                Log.v(TAG, "averagePaceKilometer = " + averagePaceKilometer);

                WorkoutIntervalData wid = new WorkoutIntervalData(0, 0,
                                                            (long) (intervalKilometers / ONE_KILOMETER),
                                                            (int) intervalTime,
                                                            (float) 0.0f, 0);
                splitsList.add(wid);

                Intent intent = new Intent("IntervalUpdated");
                LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
                /*
                Intent intent = new Intent("IntervalUpdated");
                intent.putExtra("km", (long) (intervalKilometers / ONE_KILOMETER));
                intent.putExtra("averagePace", (int) intervalTime);
//                intent.putExtra("averagePace", averagePaceKilometer);
                intent.putExtra("elevation", (float) 0.0f);
                LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
*/
                intervalKilometers += ONE_KILOMETER;
            }

            workoutDistance += distance;
            double distanceKilometers = distance / ONE_KILOMETER;
            double duration = workoutTime - oldWorkoutTime;
            double secondsInKilometers = duration / distanceKilometers;

            Log.v(TAG, "workoutTime = " + workoutTime);
            Log.v(TAG, "distanceKilometers = " + distanceKilometers);
            Log.v(TAG, "secondsInKilometers: " + secondsInKilometers);
            Log.v(TAG, "convert speed: " + TimeUtil.getDurationString((int) secondsInKilometers));
            String strAveragePace = TimeUtil.getDurationString((int) secondsInKilometers);
            oldWorkoutTime = workoutTime;

            Intent intent = new Intent("RecordUpdated");
            intent.putExtra("km", workoutDistance / ONE_KILOMETER);
            intent.putExtra("averagePace", strAveragePace);
            intent.putExtra("elevation", 0);
            LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
        }
        oldLocation = location;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
}

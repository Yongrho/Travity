package com.travity.ui.workout;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
//import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.lifecycle.Observer;
//import androidx.lifecycle.ViewModelProvider;

//import android.os.Bundle;

import com.travity.MainActivity;
import com.travity.data.ActivityType;
import com.travity.data.Constants;
import com.travity.data.StampData;
import com.travity.data.StampType;
import com.travity.data.WorkoutIntervalData;
import com.travity.data.WorkoutLocationData;
import com.travity.data.WorkoutResultData;
import com.travity.data.db.WorkoutIntervalsSQLiteHelper;
import com.travity.data.db.WorkoutLocationsSQLiteHelper;
import com.travity.data.db.WorkoutResultsSQLiteHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Intent;
import android.graphics.Color;
//import android.location.Location;
//import android.os.Bundle;

import androidx.preference.PreferenceManager;
//import android.support.v4.content.LocalBroadcastManager;
//import android.support.v7.app.AppCompatActivity;
//import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;

//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.model.BitmapDescriptor;
//import com.google.android.gms.maps.model.BitmapDescriptorFactory;
//import com.google.android.gms.maps.model.Circle;
//import com.google.android.gms.maps.model.CircleOptions;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;

//import java.io.File;
//import java.io.FileOutputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Timer;
//import java.util.TimerTask;

import com.travity.R;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.ui.IconGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutResultActivity extends AppCompatActivity
                                implements OnMapReadyCallback, ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "WorkoutResultActivity";
    private static final int DEFAULT_ZOOM = 15;
    private static final int MAP_THUMBNAIL_WIDTH = 64;
    private static final int MAP_THUMBNAIL_HEIGHT = 64;

    private GoogleMap mMap;

    private ImageButton startButton;
    private ImageButton stopButton;
    private TextView tvTime;
    private TextView tvDistance;
    private TextView tvAveragePace;

    public com.travity.ui.workout.LocationService locationService;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;

    private static final int PERMISSIONS_REQUEST_ACCESS_WRITE_STORAGE = 1;
    private boolean mWriteStoragePermissionGranted;

    WorkoutResultsSQLiteHelper dbResults;
    WorkoutLocationsSQLiteHelper dbLocations;
    WorkoutIntervalsSQLiteHelper dbIntervals;
    ArrayList<android.location.Location> locationList;

//    int activity;
    String createTime;
    Bitmap bitmap;
    boolean isMapLoaded;
    boolean isLayoutCompleted;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_workout_result);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                createTime = extras.getString("create_time");
                //activity = extras.getInt("activity");
            }
        }

        // get LocationService
        locationService = LocationServiceController.getInstance().getService();

        // open the database of the application context
        dbResults = new WorkoutResultsSQLiteHelper(this);
        dbLocations = new WorkoutLocationsSQLiteHelper(this);
        dbIntervals = new WorkoutIntervalsSQLiteHelper(this);

        SupportMapFragment mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
        mMapFragment.getView().getViewTreeObserver().addOnGlobalLayoutListener(this);

        tvTime = (TextView) findViewById(R.id.time);
        tvDistance = (TextView) findViewById(R.id.distance);
        tvAveragePace = (TextView) findViewById(R.id.average_pace);

        stopButton = (ImageButton) findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWorkout();
                LocationServiceController.getInstance().stopService(getBaseContext());
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("menu", 1);
                startActivity(intent);
            }
        });

        startButton = (ImageButton) findViewById(R.id.start_button);
        startButton.setVisibility(View.INVISIBLE);

        ArrayList<WorkoutLocationData> locations = dbLocations.getAllLocations(createTime);
        locationList = new ArrayList<Location>();
        for (int i = 0; i < locations.size(); i++) {
            Location location = new Location("");
            location.setLatitude(locations.get(i).getLatitude());
            location.setLongitude(locations.get(i).getLongitude());
            locationList.add(location);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        isLayoutCompleted = false;
        mMap = googleMap;
        if (!isMapLoaded) {
            isMapLoaded = true;
            return;
        }
        initMap();
    }

    @Override
    public void onGlobalLayout() {
        if (isLayoutCompleted) {
            return;
        }
        if (!isMapLoaded) {
            isMapLoaded = true;
            return;
        }
        isLayoutCompleted = true;
        initMap();
    }

    float totalDistanceInKiloMeters = 0;
    int elapsedTimeInSeconds = 0;
    int paceInMinutesKilometers = 0;

    private void initMap() {
        Polyline pathPolyline;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        getWriteStoragePermission();

        float totalDistanceInMeters = 0;
        for(int i = 0; i < locationList.size() - 1; i++){
            totalDistanceInMeters +=  locationList.get(i).distanceTo(locationList.get(i + 1));
        }

        totalDistanceInKiloMeters = totalDistanceInMeters / 1000;
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", totalDistanceInKiloMeters));

        elapsedTimeInSeconds = (int) locationService.getElapsedTimeInSeconds();
        tvTime.setText(TimeUtil.getDurationString((int) elapsedTimeInSeconds));

        paceInMinutesKilometers = (int) (elapsedTimeInSeconds / totalDistanceInKiloMeters);
        String strAveragePace = TimeUtil.getDurationString((int) paceInMinutesKilometers);
        tvAveragePace.setText(strAveragePace);

        if (locationList.size() < 2) {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        mLastKnownLocation = task.getResult();
                        if (mLastKnownLocation != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        }
                    }
                }
            });
            return;
        }

        Location fromLocation = locationList.get(0);
        LatLng from = new LatLng(((fromLocation.getLatitude())),
                ((fromLocation.getLongitude())));
        builder.include(from);

        Location toLocation = locationList.get(1);
        LatLng to = new LatLng(((toLocation.getLatitude())),
                ((toLocation.getLongitude())));
        builder.include(to);

        pathPolyline = mMap.addPolyline(new PolylineOptions()
                .add(from, to)
                .width(30).color(Color.parseColor("#801B60FE")).geodesic(true));

        Location location = null;
        List<LatLng> points = pathPolyline.getPoints();

        for (int i = 2; locationList.size() > i; i++){
            location = locationList.get(i);
            to = new LatLng(((location.getLatitude())), ((location.getLongitude())));
            points.add(to);
            pathPolyline.setPoints(points);
            builder.include(to);
        }

        IconGenerator iconFactory = new IconGenerator(this);
        Marker mMarkerStart = mMap.addMarker(new MarkerOptions().position(from));
        mMarkerStart.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("S")));
        Marker mMarkerEnd = mMap.addMarker(new MarkerOptions().position(to));
        mMarkerEnd.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("E")));

        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback () {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                bitmap = snapshot;
            }
        };

        try {
            LatLngBounds bounds = builder.build();
            int padding = 150; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.moveCamera(cu);
            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mMap.snapshot(callback);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveWorkout() {
        if (bitmap == null
                || bitmap.getWidth() < 64 || bitmap.getHeight() < 64) {
            return;
        }

        SimpleDateFormat fileNameDateTimeFormat = new SimpleDateFormat(Constants.DATETIME_FORMAT_FILENAME);
        String thumbFilename = fileNameDateTimeFormat.format(new Date()) + ".png";

        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 50, 10,
                                             bitmap.getWidth() - 50,
                                            bitmap.getHeight() - 10);
        FileOutputStream fout = null;
        Bitmap thumbImage = null;

        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), thumbFilename);
            fout = new FileOutputStream (file);
            thumbImage = ThumbnailUtils.extractThumbnail(croppedBitmap, MAP_THUMBNAIL_WIDTH, MAP_THUMBNAIL_HEIGHT);
            thumbImage.compress(Bitmap.CompressFormat.PNG,100, fout);
        } catch (Exception e){
            e.printStackTrace ();
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int activity = Integer.parseInt(prefs.getString("activity", "0"));

        String[] activityItems = getResources().getStringArray(R.array.activity_entries);
        int event = 0;

        String finishTime = fileNameDateTimeFormat.format(new Date());

        // put this workout into WorkoutResult db
        String title = TimeUtil.getDayToday() + " " + activityItems[activity];
        WorkoutResultData result = new WorkoutResultData(0, thumbFilename,
                                    0, title,
                                    activity, event,
                                    createTime, 0,
                                    totalDistanceInKiloMeters, elapsedTimeInSeconds,
                                    (int) paceInMinutesKilometers);
        long resultId = dbResults.createWorkoutResult(result);

        WorkoutIntervalData wid;
        for (int i = 0; i < locationService.splitsList.size(); i++) {
            wid = locationService.splitsList.get(i);
            wid.setWorkoutResultId(resultId);
            dbIntervals.createWorkoutInterval(wid);
        }

        // see if this result has some stamps
        if (totalDistanceInKiloMeters > 0.5f) {
            StampData stamp = new StampData(0, null, 0, createTime, ActivityType.ACTIVITY_RUNNING.getValue(),
                    StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!");
        }
    }

    public void takeThumbImage() {
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback () {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                bitmap = snapshot;
            }
        };
        mMap.snapshot(callback);
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getWriteStoragePermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            mWriteStoragePermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_ACCESS_WRITE_STORAGE);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mWriteStoragePermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mWriteStoragePermissionGranted = true;
                }
            }
        }
    }
}
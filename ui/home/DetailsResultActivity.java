package com.travity.ui.home;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
//import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.lifecycle.Observer;
//import androidx.lifecycle.ViewModelProvider;

//import android.os.Bundle;

import com.travity.data.WorkoutIntervalData;
import com.travity.data.WorkoutLocationData;
import com.travity.data.WorkoutResultData;
import com.travity.data.db.WorkoutIntervalsSQLiteHelper;
import com.travity.data.db.WorkoutLocationsSQLiteHelper;
import com.travity.data.db.WorkoutResultsSQLiteHelper;
import com.travity.ui.workout.TimeUtil;
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

//import android.support.v4.content.LocalBroadcastManager;
//import android.support.v7.app.AppCompatActivity ;
//import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailsResultActivity extends AppCompatActivity
                                implements OnMapReadyCallback, ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "WorkoutDetailsActivity";
    private static final int DEFAULT_ZOOM = 15;

    private GoogleMap mMap;

    private ImageButton startButton;
    private ImageButton stopButton;
    private EditText name;
    private TextView type, date;
    private TextView tvTime, tvDistance, tvAveragePace;
    private TableLayout tableLayout;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;

    private static final int PERMISSIONS_REQUEST_ACCESS_WRITE_STORAGE = 1;
    private boolean mWriteStoragePermissionGranted;

    WorkoutResultsSQLiteHelper dbResults;
    WorkoutLocationsSQLiteHelper dbLocations;
    WorkoutIntervalsSQLiteHelper dbIntervals;
    ArrayList<LatLng> listLatLng;
    ArrayList<WorkoutIntervalData> splitsList;

    WorkoutResultData result;
    long resultId;
    boolean isMapLoaded;
    boolean isLayoutCompleted;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_result);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                resultId = extras.getLong("resultId");
            }
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // open the database of the application context
        dbResults = new WorkoutResultsSQLiteHelper(this);
        dbLocations = new WorkoutLocationsSQLiteHelper(this);
        dbIntervals = new WorkoutIntervalsSQLiteHelper(this);

        SupportMapFragment mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        result = dbResults.readWorkoutResult(resultId);
        name = (EditText) findViewById(R.id.name);
        name.setText(result.getName());
        name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name.setFocusable(true);
                name.setCursorVisible(true);
            }
        });

        type = (TextView) findViewById(R.id.type);
        type.setText(TimeUtil.getDurationString(result.getTime()));

        date = (TextView) findViewById(R.id.date);
        //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat workoutDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm a");
        Date tmpDate = new Date();
        try {
            tmpDate = workoutDateFormat.parse(String.valueOf(result.getStartTime()));
            date.setText(simpleDateFormat.format(tmpDate));
        } catch (
                ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        tvTime = (TextView) findViewById(R.id.time);
        tvTime.setText(TimeUtil.getDurationString(result.getTime()));
        tvDistance = (TextView) findViewById(R.id.distance);
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", result.getDistance()));
        tvAveragePace = (TextView) findViewById(R.id.average_pace);
        tvAveragePace.setText(TimeUtil.getDurationString(result.getAverage()));
/*
        stopButton = (ImageButton) findViewById(R.id.stop_button);
        stopButton.setVisibility(View.INVISIBLE);

        startButton = (ImageButton) findViewById(R.id.start_button);
        startButton.setVisibility(View.INVISIBLE);
*/
        ArrayList<WorkoutLocationData> locations = dbLocations.getAllLocations(result.getStartTime());
        listLatLng = new ArrayList<LatLng>();
        for (int i = 0; i < locations.size(); i++) {
            LatLng location = new LatLng(locations.get(i).getLatitude(),
                                        locations.get(i).getLongitude());
            listLatLng.add(location);
        }

        tableLayout = (TableLayout) findViewById(R.id.table);
        splitsList = dbIntervals.getAllIntervals(resultId);
        if (splitsList.size() > 0) {
            refreshWorkoutInterval();
        } else {
            tableLayout.setVisibility(View.GONE);
            View divider = (View) findViewById(R.id.divider2);
            divider.setVisibility(View.GONE);
            TextView textView = (TextView) findViewById(R.id.label_splits);
            textView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        isLayoutCompleted = false;
        mMap = googleMap;
/*
        if (!isMapLoaded) {
            isMapLoaded = true;
            return;
        }

 */
        initMap();
    }

    @Override
    public void onGlobalLayout() {
/*
        if (isLayoutCompleted) {
            return;
        }
        if (!isMapLoaded) {
            isMapLoaded = true;
            return;
        }
        isLayoutCompleted = true;
 */
        initMap();
    }

    private void initMap() {
        Polyline pathPolyline;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        getWriteStoragePermission();

        if (listLatLng.size() < 2) {
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

        LatLng from = listLatLng.get(0);
        builder.include(from);

        LatLng to = listLatLng.get(1);
        builder.include(to);

        pathPolyline = mMap.addPolyline(new PolylineOptions()
                .add(from, to)
                .width(30).color(Color.parseColor("#801B60FE")).geodesic(true));

        List<LatLng> points = pathPolyline.getPoints();
        for (int i = 2; listLatLng.size() > i; i++){
            to = listLatLng.get(i);
            points.add(to);
            pathPolyline.setPoints(points);
            builder.include(to);
        }

        IconGenerator iconFactory = new IconGenerator(this);
        Marker mMarkerStart = mMap.addMarker(new MarkerOptions().position(from));
        mMarkerStart.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("S")));
        Marker mMarkerEnd = mMap.addMarker(new MarkerOptions().position(to));
        mMarkerEnd.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("E")));

        try {
            LatLngBounds bounds = builder.build();
            int padding = 150; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.moveCamera(cu);
            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
//                    mMap.snapshot(callback);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
/*
        LatLngBounds bounds = builder.build();
        int padding = 150; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.moveCamera(cu);
 */
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

    private void refreshWorkoutInterval() {
        WorkoutIntervalData wid;

        addItem(getResources().getString(R.string.caps_km),
                getResources().getString(R.string.average),
                getResources().getString(R.string.elevation), 14);

        for (int i = 0; i < splitsList.size(); i++) {
            wid = splitsList.get(i);
            addItem(String.valueOf(wid.getDistance()),
                    String.valueOf(wid.getAverage()),
                    String.valueOf(wid.getElevation()), 14);
        }
    }

    private void addItem(String distance, String averagePace, String elevation, int textSize) {
        TableRow tbrow = new TableRow(this);
        TextView tv1 = new TextView(this);
        tv1.setText(distance);
        tv1.setTextColor(Color.BLACK);
        tv1.setGravity(Gravity.LEFT);
        tv1.setTextSize(textSize);
        tv1.setPadding(10, 15, 20, 15);
        tbrow.addView(tv1);
        TextView tv2 = new TextView(this);
        tv2.setText(averagePace);
        tv2.setTextColor(Color.BLACK);
        tv2.setGravity(Gravity.LEFT);
        tv2.setTextSize(textSize);
        tv2.setPadding(20, 15, 20, 15);
        tbrow.addView(tv2);
        TextView tv3 = new TextView(this);
        tv3.setText(elevation);
        tv3.setTextColor(Color.BLACK);
        tv3.setGravity(Gravity.RIGHT);
        tv3.setTextSize(textSize);
        tv3.setPadding(20, 15, 0, 15);
        tbrow.addView(tv3);
        tableLayout.addView(tbrow);

        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
        ));
        v.setBackgroundColor(Color.parseColor("#E3E4E6"));
        tableLayout.addView(v);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                updateTitle();
                finish();
                return true;
            case R.id.remove:
                dbResults.deleteWorkoutResult(result);
                Intent intent = new Intent();
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_remove, menu);
        return true;
    }

    private void updateTitle() {
        String nameString = name.getText().toString();
        if (result.getName().equals(nameString)) {
            return;
        }

        result.setName(nameString);
        dbResults.updateWorkoutResult(result);
        Intent intent = new Intent();
        setResult(Activity.RESULT_OK, intent);
    }
}
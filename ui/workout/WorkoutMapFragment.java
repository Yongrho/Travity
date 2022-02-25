package com.travity.ui.workout;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
//import androidx.lifecycle.Observer;
//import androidx.lifecycle.ViewModelProvider;

//import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.travity.data.WorkoutIntervalData;
import com.travity.data.WorkoutLocationData;
import com.travity.data.db.WorkoutIntervalsSQLiteHelper;
import com.travity.data.db.WorkoutLocationsSQLiteHelper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
//import android.location.Location;
//import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
//import android.support.v4.content.LocalBroadcastManager;
//import android.support.v7.app.AppCompatActivity ;
import android.util.Log;
//import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.travity.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Logger;

import okhttp3.OkHttpClient;

import static android.provider.ContactsContract.ProviderStatus.STATUS;

public class WorkoutMapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "WorkoutMapFragment";

    private static final int LOCATION_UPDATE_MIN_DISTANCE = 10;
    private static final int LOCATION_UPDATE_MIN_TIME = 10000;
    private static final int DEFAULT_ZOOM = 15;

    public com.travity.ui.workout.LocationService locationService;
//    private MapView mapView;
    private GoogleMap map;

    private Marker userPositionMarker;
    private Circle locationAccuracyCircle;
    private BitmapDescriptor userPositionMarkerBitmapDescriptor;
    private Polyline runningPathPolyline;
//    private PolylineOptions polylineOptions;
    private int polylineWidth = 30;

    //boolean isZooming;
    //boolean isBlockingAutoZoom;
    boolean zoomable = true;

    Timer zoomBlockingTimer;
    boolean didInitialZoom;
    private Handler handlerOnUIThread;

    /* Filater */
    private Circle predictionRange;
    BitmapDescriptor oldLocationMarkerBitmapDescriptor;
    BitmapDescriptor noAccuracyLocationMarkerBitmapDescriptor;
    BitmapDescriptor inaccurateLocationMarkerBitmapDescriptor;
    BitmapDescriptor kalmanNGLocationMarkerBitmapDescriptor;
    ArrayList<Marker> malMarkers = new ArrayList<>();
    final Handler handler = new Handler();
    SharedViewModel viewModel;

    WorkoutLocationsSQLiteHelper dbLocations;
    int activity;
    String createTime;
    ImageButton pauseButton, resumeButton, stopButton;

    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location newLocation = intent.getParcelableExtra("location");

            WorkoutLocationData location = new WorkoutLocationData(0, createTime, newLocation.getLatitude(), newLocation.getLongitude());
            dbLocations.createWorkoutLocation(location);

            drawLocationAccuracyCircle(newLocation);
            drawUserPositionMarker(newLocation);

            if (WorkoutMapFragment.this.locationService.isLogging) {
                addPolyline();
            }
            zoomMapTo(newLocation);

            // Filter Visualization
            drawMalLocations();

//            loadNearByPlaces(newLocation);
        }
    };

    private BroadcastReceiver predictedLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location predictedLocation = intent.getParcelableExtra("location");
            drawPredictionRange(predictedLocation);
        }
    };

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        locationService = LocationServiceController.getInstance().getService();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                locationUpdateReceiver,
                new IntentFilter("LocationUpdated"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                predictedLocationReceiver,
                new IntentFilter("PredictLocation"));
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // open the database of the application context
        dbLocations = new WorkoutLocationsSQLiteHelper(getActivity());

        View root = inflater.inflate(R.layout.fragment_workout_map, container, false);

        SupportMapFragment mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        clearPolyline();
        clearMalMarkers();

        pauseButton = root.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setRunningStatus(1);
                pauseButton.setVisibility(View.GONE);
                resumeButton.setVisibility(View.VISIBLE);
                WorkoutMapFragment.this.locationService.stopLogging();
            }
        });

        resumeButton = root.findViewById(R.id.start_button);
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setRunningStatus(2);
                resumeButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                WorkoutMapFragment.this.locationService.startLogging();
            }
        });
        resumeButton.setVisibility(View.GONE);

        stopButton = root.findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setRunningStatus(0);
                WorkoutMapFragment.this.locationService.stopUpdatingLocation();
                WorkoutMapFragment.this.locationService.stopLogging();

                Intent intent = new Intent(getContext(), WorkoutResultActivity.class);
                intent.putExtra("create_time", createTime);
                intent.putExtra("activity", activity);
                startActivity(intent);
            }
        });

        oldLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.old_location_marker);
        noAccuracyLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.no_accuracy_location_marker);
        inaccurateLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.inaccurate_location_marker);
        kalmanNGLocationMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.kalman_ng_location_marker);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.getCreateTime().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String time) {
                createTime = time;
            }
        });
        viewModel.getRunningStatus().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer status) {
                switch (status) {
                    case 1:
                        pauseButton.setVisibility(View.GONE);
                        resumeButton.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        resumeButton.setVisibility(View.GONE);
                        pauseButton.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }
        });
        viewModel.getActivity().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer activity_type) {
                activity = activity_type;
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if (ActivityCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if(reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                    Log.d(TAG, "onCameraMoveStarted after user's zoom action");

                    zoomable = false;
                    if (zoomBlockingTimer != null) {
                        zoomBlockingTimer.cancel();
                    }

                    handlerOnUIThread = new Handler();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            handlerOnUIThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    zoomBlockingTimer = null;
                                    zoomable = true;
                                }
                            });
                        }
                    };
                    zoomBlockingTimer = new Timer();
                    zoomBlockingTimer.schedule(task, 10 * 1000);
                    Log.d(TAG, "start blocking auto zoom for 10 seconds");
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
}

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (locationUpdateReceiver != null) {
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locationUpdateReceiver);
            }

            if (predictedLocationReceiver != null) {
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(predictedLocationReceiver);
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void zoomMapTo(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (this.didInitialZoom == false) {
            try {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                this.didInitialZoom = true;
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (zoomable) {
            try {
                zoomable = false;
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng),
                        new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                zoomable = true;
                            }

                            @Override
                            public void onCancel() {
                                zoomable = true;
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void drawUserPositionMarker(Location location){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if(this.userPositionMarkerBitmapDescriptor == null){
            userPositionMarkerBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.user_position_point);
        }

        if (userPositionMarker == null) {
            userPositionMarker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .icon(this.userPositionMarkerBitmapDescriptor));
        } else {
            userPositionMarker.setPosition(latLng);
        }
    }

    private void drawLocationAccuracyCircle(Location location){
        if(location.getAccuracy() < 0){
            return;
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (this.locationAccuracyCircle == null) {
            this.locationAccuracyCircle = map.addCircle(new CircleOptions()
                    .center(latLng)
                    .fillColor(Color.argb(64, 0, 0, 0))
                    .strokeColor(Color.argb(64, 0, 0, 0))
                    .strokeWidth(0.0f)
                    .radius(location.getAccuracy())); //set readius to horizonal accuracy in meter.
        } else {
            this.locationAccuracyCircle.setCenter(latLng);
        }
    }

    private void addPolyline() {
        ArrayList<Location> locationList = locationService.locationList;

        if (runningPathPolyline == null) {
            if (locationList.size() > 1){
                Location fromLocation = locationList.get(locationList.size() - 2);
                Location toLocation = locationList.get(locationList.size() - 1);

                LatLng from = new LatLng(((fromLocation.getLatitude())),
                        ((fromLocation.getLongitude())));

                LatLng to = new LatLng(((toLocation.getLatitude())),
                        ((toLocation.getLongitude())));

                this.runningPathPolyline = map.addPolyline(new PolylineOptions()
                        .add(from, to)
                        .width(polylineWidth).color(Color.parseColor("#801B60FE")).geodesic(true));
                viewModel.setLocationList(locationList);
            }
        } else {
            Location toLocation = locationList.get(locationList.size() - 1);
            LatLng to = new LatLng(((toLocation.getLatitude())),
                    ((toLocation.getLongitude())));

            List<LatLng> points = runningPathPolyline.getPoints();
            points.add(to);

            runningPathPolyline.setPoints(points);
        }
    }

    private void clearPolyline() {
        if (runningPathPolyline != null) {
            runningPathPolyline.remove();
            runningPathPolyline = null;
        }
    }

    /* Filter Visualization */
    private void drawMalLocations(){
        drawMalMarkers(locationService.oldLocationList, oldLocationMarkerBitmapDescriptor);
        drawMalMarkers(locationService.noAccuracyLocationList, noAccuracyLocationMarkerBitmapDescriptor);
        drawMalMarkers(locationService.inaccurateLocationList, inaccurateLocationMarkerBitmapDescriptor);
        drawMalMarkers(locationService.kalmanNGLocationList, kalmanNGLocationMarkerBitmapDescriptor);
    }

    private void drawMalMarkers(ArrayList<Location> locationList, BitmapDescriptor descriptor){
        for(Location location : locationList){
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            Marker marker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .icon(descriptor));

            malMarkers.add(marker);
        }
    }

    private void drawPredictionRange(Location location){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (this.predictionRange == null) {
            this.predictionRange = map.addCircle(new CircleOptions()
                    .center(latLng)
                    .fillColor(Color.argb(50, 30, 207, 0))
                    .strokeColor(Color.argb(128, 30, 207, 0))
                    .strokeWidth(1.0f)
                    .radius(30)); //30 meters of the prediction range
        } else {
            this.predictionRange.setCenter(latLng);
        }

        this.predictionRange.setVisible(true);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                WorkoutMapFragment.this.predictionRange.setVisible(false);
            }
        }, 2000);
    }

    public void clearMalMarkers(){
        for (Marker marker : malMarkers){
            marker.remove();
        }
    }

    private static final int PROXIMITY_RADIUS = 500;

    private void loadNearByPlaces(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

// https://maps.googleapis.Com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&radius=500&types=food&name=harbour&key=YOUR_API_KEY

        StringBuilder googlePlacesUrl =
                new StringBuilder("https://maps.googleapis.Com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude);
        googlePlacesUrl.append("&radius=").append(PROXIMITY_RADIUS);
        googlePlacesUrl.append("&types=").append("park");
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + getResources().getString(R.string.google_maps_key));

        Log.v(TAG, "key: " + getResources().getString(R.string.google_maps_key));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, googlePlacesUrl.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {
                        Log.i(TAG, "onResponse: Result= " + result.toString());
                        parseLocationResult(result);
                    }
                },
                new Response.ErrorListener() {
                    @Override                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: Error= " + error);
                        Log.e(TAG, "onErrorResponse: Error= " + error.getMessage());
                    }
                });
        Volley.newRequestQueue(getContext()).add(request);

//        AppController.getInstance().addToRequestQueue(request);
    }

    private void parseLocationResult(JSONObject result) {

        String id, place_id, placeName = null, reference, icon, vicinity = null;
        double latitude, longitude;

        try {
            JSONArray jsonArray = result.getJSONArray("results");
            Log.v(TAG, "status: " + result.getString(STATUS));
            if (result.getString(STATUS).equalsIgnoreCase("OK")) {
//                map.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject place = jsonArray.getJSONObject(i);
/*
                    id = place.getString(SUPERMARKET_ID);
                    place_id = place.getString(PLACE_ID);
                    if (!place.isNull(NAME)) {
                        placeName = place.getString(NAME);
                    }
                    if (!place.isNull(VICINITY)) {
                        vicinity = place.getString(VICINITY);
                    }
                    latitude = place.getJSONObject(GEOMETRY).getJSONObject(LOCATION)
                            .getDouble(LATITUDE);
                    longitude = place.getJSONObject(GEOMETRY).getJSONObject(LOCATION)
                            .getDouble(LONGITUDE);
                    reference = place.getString(REFERENCE);
                    icon = place.getString(ICON);
/*
                    MarkerOptions markerOptions = new MarkerOptions();
                    LatLng latLng = new LatLng(latitude, longitude);
                    markerOptions.position(latLng);
                    markerOptions.title(placeName + " : " + vicinity);

                    map.addMarker(markerOptions);
 */
                }

                Toast.makeText(getContext(), jsonArray.length() + " Supermarkets found!",
                        Toast.LENGTH_LONG).show();

            } else if (result.getString(STATUS).equalsIgnoreCase("ZERO_RESULTS")) {
                Toast.makeText(getContext(), "No Supermarket found in 5KM radius!!!",
                        Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "parseLocationResult: Error=" + e.getMessage());
        }
    }

    private void getElevation(Location location) {
/*
        OkHttpClient client = new OkHttpClient().newBuilder().build();

        Request request = new Request.Builder()
                .url("https://maps.googleapis.Com/maps/api/elevation/json?locations=39.7391536,-104.9847034&key=YOUR_API_KEY")
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
 */
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        StringBuilder googlePlacesUrl =
                new StringBuilder("https://maps.googleapis.Com/maps/api/elevation/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + getResources().getString(R.string.google_maps_key));

        Log.v(TAG, "key: " + getResources().getString(R.string.google_maps_key));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, googlePlacesUrl.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {
                        Log.i(TAG, "onResponse: Result= " + result.toString());
                        parseLocationResult(result);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: Error= " + error);
                        Log.e(TAG, "onErrorResponse: Error= " + error.getMessage());
                    }
                });
        Volley.newRequestQueue(getContext()).add(request);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_menu_add, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                Intent intent = new Intent(getActivity(), WorkoutIntervalsActivity.class);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return false;
    }
}
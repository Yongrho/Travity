package com.travity.ui.workout;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
//import androidx.lifecycle.Observer;
//import androidx.lifecycle.ViewModelProvider;

//import android.os.Bundle;

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

import android.graphics.Color;
//import android.location.Location;
//import android.os.Bundle;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
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

public class WorkoutResultFragment extends Fragment
                    implements OnMapReadyCallback, ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "WorkoutResultFragment";

    private GoogleMap mMap;

    private ImageButton startButton;
    private ImageButton stopButton;
    private TextView tvTime;
    private TextView tvDistance;
    private TextView tvAveragePace;

    private static final int DEFAULT_ZOOM = 15;

    public com.travity.ui.workout.LocationService locationService;
    private SharedViewModel viewModel;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;

    private static final int PERMISSIONS_REQUEST_ACCESS_WRITE_STORAGE = 1;
    private boolean mWriteStroragePermissionGranted;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        locationService = LocationServiceController.getInstance().getService();
        View root = inflater.inflate(R.layout.fragment_workout_result, container, false);

        SupportMapFragment mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
        mMapFragment.getView().getViewTreeObserver().addOnGlobalLayoutListener(this);

        tvTime = (TextView) root.findViewById(R.id.time);
        tvDistance = (TextView) root.findViewById(R.id.distance);
        tvAveragePace = (TextView) root.findViewById(R.id.average_pace);

        stopButton = (ImageButton) root.findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(getContext(), WorkoutHomeActivity.class);
//                startActivity(intent);
                final NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.navigation_workout);
            }
        });

        startButton = (ImageButton) root.findViewById(R.id.start_button);
        startButton.setVisibility(View.INVISIBLE);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.getLocationService().observe(getViewLifecycleOwner(), new Observer<LocationService>() {
            @Override
            public void onChanged(@Nullable LocationService ls) {
                WorkoutResultFragment.this.locationService = ls;
            }
        });
    }

    float totalDistanceInKiloMeters = 0.0f;
    float elapsedTimeInSeconds = 0.0f;
    float paceInMinutesKilometers = 0.0f;
    private boolean isMapLoaded;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (!isMapLoaded) {
            isMapLoaded = true;
            return;
        }
        initMap();
    }

    @Override
    public void onGlobalLayout() {
        if (!isMapLoaded) {
            isMapLoaded = true;
            return;
        }
        initMap();
    }

    private void initMap() {
        Polyline pathPolyline;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        ArrayList<Location> locationList = locationService.locationList;
//        Log.v(TAG, "locationList.size(): " + locationList.size());

//        mMap = googleMap;
        getWriteStoragePermission();

        float totalDistanceInMeters = 0;
        for(int i = 0; i < locationList.size() - 1; i++){
            totalDistanceInMeters +=  locationList.get(i).distanceTo(locationList.get(i + 1));
        }

        totalDistanceInKiloMeters = totalDistanceInMeters / 1000;
//        Log.v(TAG, "totalDistanceInKiloMeters: " + totalDistanceInKiloMeters);
        tvDistance.setText(String.format("%.2f", totalDistanceInKiloMeters));

        elapsedTimeInSeconds = WorkoutResultFragment.this.locationService.getElapsedTimeInSeconds();
//        Log.v(TAG, "elapsedTimeInSeconds: " + elapsedTimeInSeconds);
        tvTime.setText(TimeUtil.getDurationString((int) elapsedTimeInSeconds));

        float elapsedTimeInMinutes = (float) elapsedTimeInSeconds / 60;
//        Log.v(TAG, "elapsedTimeInMinutes: " + elapsedTimeInMinutes);
        paceInMinutesKilometers = elapsedTimeInSeconds / totalDistanceInKiloMeters;
//        float averagePaceInMinutes = elapsedTimeInMinutes / totalDistanceInKiloMeters;
        String strAveragePace = TimeUtil.getDurationString((int) paceInMinutesKilometers);
//        Log.v(TAG, "paceInMinutesKilometers: " + paceInMinutesKilometers);
//        Log.v(TAG, "averagePaceInMinutes: " + averagePaceInMinutes);
//        Log.v(TAG, "strAveragePace: " + strAveragePace);
        tvAveragePace.setText(strAveragePace);

        if (locationList.size() < 2) {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
            if (ActivityCompat.checkSelfPermission(this.getContext(),
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

        IconGenerator iconFactory = new IconGenerator(getActivity());
        Marker mMarkerStart = mMap.addMarker(new MarkerOptions().position(from));
        mMarkerStart.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("Start")));
        Marker mMarkerEnd = mMap.addMarker(new MarkerOptions().position(to));
        mMarkerEnd.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("End")));

        try {
            LatLngBounds bounds = builder.build();
            int padding = 150; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,
                    padding);
            mMap.moveCamera(cu);
            mMap.animateCamera(cu, 2000,
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        takeThumbImage();
                    }

                    @Override
                    public void onCancel() {
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takeThumbImage() {
        SimpleDateFormat fileNameDateTimeFormat = new SimpleDateFormat("yyyyMMddHHmm");
        String thumbFilename = fileNameDateTimeFormat.format(new Date()) + ".png";

        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback () {
            Bitmap bitmap;
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                bitmap = snapshot;

                if (mWriteStroragePermissionGranted == false) {
                    return;
                }

                Log.v(TAG, "bitmap.getWidth(): " + bitmap.getWidth());
                Log.v(TAG, "bitmap.getHeight(): " + bitmap.getHeight());
                Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 50, 10, bitmap.getWidth() - 50, bitmap.getHeight() - 10);

                FileOutputStream fout = null;
                Bitmap thumbImage = null;

                try {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), thumbFilename);
                    fout = new FileOutputStream (file);
                    thumbImage = ThumbnailUtils.extractThumbnail(croppedBitmap, 64, 64);
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
                locationService.saveLog(thumbFilename, totalDistanceInKiloMeters, elapsedTimeInSeconds);
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
        if (ActivityCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            mWriteStroragePermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this.getActivity(),
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
        mWriteStroragePermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mWriteStroragePermissionGranted = true;
                }
            }
        }
    }
}
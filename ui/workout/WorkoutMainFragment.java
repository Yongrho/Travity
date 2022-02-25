package com.travity.ui.workout;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
//import androidx.lifecycle.Observer;
//import androidx.lifecycle.ViewModelProvider;

//import android.os.Bundle;

import com.travity.ProfileActivity;
import com.travity.data.Constants;
import com.travity.ui.home.SettingsActivity;
import com.travity.util.textdrawable.TextDrawable;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.content.Intent;
//import android.location.Location;
//import android.os.Bundle;

import androidx.preference.PreferenceManager;
//import android.support.v4.content.LocalBroadcastManager;
//import android.support.v7.app.AppCompatActivity;
//import android.view.View;
import android.widget.ImageButton;

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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.apache.commons.codec.binary.StringUtils;

public class WorkoutMainFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "WorkoutMainFragment";

    private GoogleMap mMap;
    ImageButton startButton, settingsButton, activityButton;
    private static final int REQUEST_LOCATION = 1;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int DEFAULT_ZOOM = 11;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;
    public com.travity.ui.workout.LocationService locationService;
//    private SharedViewModel viewModel;
//    private boolean isRunningLocationService = false;

    SharedPreferences prefs;
    int checkedActivity = 0;
    int counter = 5;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LocationServiceController.getInstance().startService(getContext());

        View root = inflater.inflate(R.layout.fragment_workout_main, container, false);

        SupportMapFragment mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        settingsButton = (ImageButton) root.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        startButton = (ImageButton) root.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationService = LocationServiceController.getInstance().getService();
                WorkoutMainFragment.this.locationService.startLogging();
                WorkoutMainFragment.this.locationService.startUpdatingLocation();

                Intent intent = new Intent(getContext(), WorkoutCountdownActivity.class);
                startActivity(intent);
            }
        });

        activityButton = (ImageButton) root.findViewById(R.id.activity_button);
        activityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseActivity();
            }
        });
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        updateActivity();

        setHasOptionsMenu(true);
        updateActionBar();
/*
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            SharedPreferences sp = getActivity().getSharedPreferences("bottomnavigationview", Activity.MODE_PRIVATE);
            String id = sp.getString("user_initial", "BV");
            if (!"".equals(id)) {
                actionBar.setHomeAsUpIndicator(TextDrawable.getIconInitialName(id));
            }
        }
*/
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivity();
    }

    private void updateActivity() {
        String prefActivity = prefs.getString("activity", "0");
//        Log.v(TAG, "prefActivity: " + prefActivity);
        if (android.text.TextUtils.isDigitsOnly(prefActivity)) {
            checkedActivity = Integer.parseInt(prefActivity);
        } else {
            checkedActivity = 0;
        }
        setActivityImage();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // refer to https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial
        mMap = googleMap;

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
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

                            /*
                            // Set the map's camera position to the current location of the device.
                            LatLng currentLatLng = new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude());
                            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(currentLatLng,
                                    DEFAULT_ZOOM);
                            mMap.moveCamera(update);
*/
                        } else {
                            Log.d(TAG, "Sorry. Location services not available.");

                            // Get the error dialog from Google Play services
                            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(0,
                                    getActivity(),
                                    9000);

                            // If Google Play services can provide an error dialog
                            if (errorDialog != null) {
                                // Create a new DialogFragment for the error dialog
                                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                                errorFragment.setDialog(errorDialog);
                                errorFragment.show(getChildFragmentManager(), "Sorry. Location services not available.");
                            }
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ActivityCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private void setActivityImage() {
        switch (checkedActivity) {
            case 1:
                activityButton.setImageResource(R.drawable.ic_directions_walk_black_24dp);
                break;
            case 2:
                activityButton.setImageResource(R.drawable.ic_terrain_black_24dp);
                break;
            case 3:
                activityButton.setImageResource(R.drawable.ic_directions_bike_black_24dp);
                break;
            default:
                activityButton.setImageResource(R.drawable.ic_directions_run_black_24dp);
                break;
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("activity", String.valueOf(checkedActivity));
        editor.commit();
    }

    private void chooseActivity() {
        String[] listItems = getResources().getStringArray(R.array.activity_entries);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.choose_activity);

        builder.setSingleChoiceItems(listItems, checkedActivity, new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                checkedActivity = which;
            }
        });

        builder.setPositiveButton("Done",new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                setActivityImage();
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateActionBar() {
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            SharedPreferences sp = getActivity().getSharedPreferences("travity", Activity.MODE_PRIVATE);
            String id = sp.getString("user_initial", "T");
            if (!"".equals(id)) {
                actionBar.setHomeAsUpIndicator(TextDrawable.getIconInitialName(id));
            }
        }
    }

    // This method is called when the next activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the activity with an OK result
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.REQUEST_CODE_PROFILE) {
                updateActionBar();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                // update profile
                intent = new Intent(getActivity(), ProfileActivity.class);
                startActivityForResult(intent, Constants.REQUEST_CODE_PROFILE);
                return true;
/*
            case R.id.add:
                // Create a new race
                intent = new Intent(getActivity(), CreateRaceActivity.class);
                intent.putExtra("event_id", ID_NONE);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
                return true;
 */
            case R.id.settings:
                // Move to Settings
                intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
//                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
                return true;
            default:
                break;
        }
        return false;
    }
}
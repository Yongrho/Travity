package com.travity.ui.group;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.travity.R;
import com.travity.data.ActivityType;
import com.travity.data.ChallengeData;
import com.travity.data.Constants;
import com.travity.data.EventData;
import com.travity.data.EventType;
import com.travity.data.GroupEventData;
import com.travity.data.db.EventsSQLiteHelper;
import com.travity.data.GroupData;
import com.travity.data.Member;
import com.travity.data.db.GroupsSQLiteHelper;
import com.travity.data.db.MembersSQLiteHelper;
import com.travity.data.db.GroupMembersSQLiteHelper;

import com.travity.data.PlaceData;
import com.travity.data.db.PlacesSQLiteHelper;
import com.travity.ui.SearchLocationActivity;
import com.travity.ui.SearchPlaceActivity;
import com.travity.ui.workout.TimeUtil;
import com.travity.ui.workout.WorkoutOnFragment;
import com.travity.ui.workout.WorkoutResultFragment;
import com.travity.util.imageloader.ImageLoader;
//import Com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker;
//import Com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.ui.IconGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CreateGroupEventActivity extends AppCompatActivity
                                implements OnMapReadyCallback {
    private static final String TAG = "CreateGroupEvent";
    private static final String EVENT_ID = "event_id";
    private static final String GROUP_ID = "group_id";
    private static final int ACTIVITY_REQUEST_CODE = 2;
    private static final float DEFAULT_ZOOM = 15.0f;
    private static final long ID_NONE = 0l;

    private GoogleMap mMap;

    EventsSQLiteHelper dbEvents;
    GroupsSQLiteHelper dbGroups;
    PlacesSQLiteHelper dbPlaces;

    TextInputEditText name, note;
    TextView activity;
    TextView startDate, startTime;
    TextView endDate, endTime;
    TextView feature, address;

    SimpleDateFormat simpleDateFormat;
    //SingleDateAndTimePickerDialog.Builder singleBuilder;
    GroupEventData data;
    int positionActivity;
    SupportMapFragment mapFragment;
    TableRow row6, row7, row7a, row7b, row7c;

    PlaceData newPlace;
    CameraUpdate cu;
    Bitmap bitmap = null;
    long groupId;
    long eventId = 0;
    int activityType = 0;

    String fromDate, toDate;
    String[] activityItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        this.simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (Constants.USE_LOCAL_DB) {
            // open the database of the application context
            dbEvents = new EventsSQLiteHelper(this);
            dbGroups = new GroupsSQLiteHelper(this);
        }
        dbPlaces = new PlacesSQLiteHelper(this);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                eventId = extras.getLong(Constants.ID_EVENT);
                groupId = extras.getLong(Constants.ID_GROUP);
                activityType = extras.getInt(Constants.FLAG_ACTIVITY);
            }
        }

        // load information of the event
        loadWidget();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (eventId > Constants.ID_NONE) {
            // for updating the event
            actionBar.setTitle(getResources().getString(R.string.event_update));
            if (Constants.USE_LOCAL_DB) {
                data = (GroupEventData) dbEvents.readEvent(eventId);
                updateWidget(data);
            } else {
                getGroupEventInfoFromServer(String.valueOf(eventId));
            }
        } else {
            actionBar.setTitle(getResources().getString(R.string.event_creation));
            Calendar cal = Calendar.getInstance();
            Date todayCalendar = cal.getTime();
            String nowDate = (String) DateFormat.format("MMM dd, yyyy", todayCalendar);
            String nowTime = (String) DateFormat.format("hh:mm a", todayCalendar);
            startDate.setText(nowDate);
            startTime.setText(nowTime);
        }
    }

    private void updateWidget(GroupEventData ged) {
        setVisibleMap(true);

        name.setText(ged.getName());
        activity.setText(activityItems[ged.getActivityType()]);

        String datetime = ged.getDate().toString();
        int index = datetime.length() - 9;
        String date = datetime.substring(0, index);
        startDate.setText(date);
        String time = datetime.substring(index + 1);
        startTime.setText(time);

        datetime = ged.getEndTime().toString();
        if (datetime.startsWith("+")) {
            endDate.setText(R.string.click_here);
            endTime.setText(R.string.click_here);
        } else {
            date = datetime.substring(0, index);
            endDate.setText(date);
            time = datetime.substring(index + 1);
            endTime.setText(time);
        }

        long placeId = ged.getPlaceId();
        if (placeId > Constants.ID_NONE) {
            PlaceData place = dbPlaces.readPlace(placeId);
            if (place == null) {
                setVisibleMap(false);
            } else {
                feature.setText(place.getFeature());
                address.setText(place.getAddress());

                row7b.setVisibility(View.GONE);
                row7c.setVisibility(View.VISIBLE);

                ImageLoader imageLoader = new ImageLoader(getBaseContext());
                ImageView image = findViewById(R.id.imageview_map);
                imageLoader.DisplayImage(place.getImage(), image);
            }
        } else {
            setVisibleMap(false);
        }

        note.setText(ged.getDescription());
    }

    private void loadWidget() {
//        GroupData data = (GroupData) dbGroups.readGroup(groupID);
        activityItems = getResources().getStringArray(R.array.activity_entries);

        name = findViewById(R.id.name);

        activity = findViewById(R.id.activity);
        activity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseActivity();
            }
        });
        activity.setText(activityItems[activityType]);

        startDate = findViewById(R.id.start_date);
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker(true, getResources().getString(R.string.start_date));
            }
        });

        startTime = findViewById(R.id.start_time);
        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timePicker(true, getResources().getString(R.string.start_time));
            }
        });

        endDate = findViewById(R.id.end_date);
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker(false, getResources().getString(R.string.start_date));
            }
        });

        endTime = findViewById(R.id.end_time);
        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timePicker(false, getResources().getString(R.string.end_time));
            }
        });

        row6 = findViewById(R.id.tr6);
        row6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // update event information
                Intent intent = new Intent(getBaseContext(), SearchPlaceActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
            }
        });

        row7 = findViewById(R.id.tr7);
        row7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), SearchPlaceActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
            }
        });
        row7a = findViewById(R.id.tr7a);
        row7a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // update event information
                Intent intent = new Intent(getBaseContext(), SearchPlaceActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
            }
        });
        row7b = findViewById(R.id.tr7b);
        row7b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // update event information
                Intent intent = new Intent(getBaseContext(), SearchPlaceActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
            }
        });
        row7c = findViewById(R.id.tr7c);

        feature = findViewById(R.id.feature);
        address = findViewById(R.id.address);
        setVisibleMap(false);

        note = findViewById(R.id.note);
    }

    private void chooseActivity() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.choose_activity));

        builder.setSingleChoiceItems(activityItems, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which){
                        Log.v(TAG, "which: " + which);
                        positionActivity = which;
                        activity.setText(activityItems[which]);
                    }
                });

        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                if (which == -1) {
                    positionActivity = 0;
                    activity.setText(activityItems[0]);
                }
                dialog.dismiss();
            }
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void datePicker(boolean start, String title) {
        MaterialDatePicker.Builder materialDateBuilder = MaterialDatePicker.Builder.datePicker();
        materialDateBuilder.setTitleText(title);

        final MaterialDatePicker materialDatePicker = materialDateBuilder.build();
        materialDatePicker.addOnPositiveButtonClickListener(
                new MaterialPickerOnPositiveButtonClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onPositiveButtonClick(Object selection) {
                String date = materialDatePicker.getHeaderText();
                if (start) {
                    startDate.setText(date);
                } else {
                    endDate.setText(date);
                }
            }
        });
        materialDatePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
    }

    private void timePicker(boolean start, String title) {
        Calendar now = Calendar.getInstance();
        MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                                                .setTimeFormat(TimeFormat.CLOCK_12H)
                                                .setHour(now.get(Calendar.HOUR_OF_DAY))
                                                .setMinute(now.get(Calendar.MINUTE))
                                                .setTitleText(title)
                                                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                                                .build();
        materialTimePicker.show(getSupportFragmentManager(), "MATERIAL_TIME_PICKER");
        materialTimePicker.addOnPositiveButtonClickListener(dialog -> {
            int newHour = materialTimePicker.getHour();
            int newMinute = materialTimePicker.getMinute();
            String time;
            boolean pm = false;

            if (newHour > 12) {
                pm = true;
                newHour -= 12;
            }

            if (newHour < 10) {
                time = "0" + String.valueOf(newHour);
            } else {
                time = String.valueOf(newHour);
            }

            if (newMinute < 10) {
                time += ":0";
            } else {
                time += ":";
            }

            time += String.valueOf(newMinute);
            if (pm) {
                time += " PM";
            } else {
                time += " AM";
            }

            if (start) {
                startTime.setText(time);
            } else {
                endTime.setText(time);
            }
        });
    }

    private void showAlertDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_alert)
                .setMessage(R.string.warning_wrong_time)
                .setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    private void setVisibleMap(boolean isVisible) {
        if (isVisible) {
            row6.setVisibility(View.GONE);
            row7.setVisibility(View.VISIBLE);
            row7a.setVisibility(View.VISIBLE);
            row7b.setVisibility(View.VISIBLE);
        } else {
            row6.setVisibility(View.VISIBLE);
            row7.setVisibility(View.GONE);
            row7a.setVisibility(View.GONE);
            row7b.setVisibility(View.GONE);
            row7c.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback () {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                bitmap = snapshot;
                saveImage();
            }
        };

        LatLng latlng = new LatLng(newPlace.getLatitude(),
                                    newPlace.getLongitude());
        mMap.addMarker(new MarkerOptions().position(latlng).title(newPlace.getFeature()));
        try {
            cu = CameraUpdateFactory.newLatLngZoom(latlng, DEFAULT_ZOOM);
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

    private String saveImage() {
        if (newPlace == null) {
            return null;
        }

        FileOutputStream out = null;
        SimpleDateFormat fileNameDateTimeFormat = new SimpleDateFormat("yyyyMMddHHmm");
        String filename = newPlace.getFeature() + "_" + fileNameDateTimeFormat.format(new Date()) + ".png";

        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename);
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e){
            e.printStackTrace ();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        return filename;
    }

    void getGroupEventInfoFromServer(String eid) {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("challenges").document(eid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                data = documentSnapshot.toObject(GroupEventData.class);
                updateWidget(data);
            }
        });
    }

    private void setGroupEventInfo(boolean isNew, EventData data) {
        final String uid = FirebaseAuth.getInstance().getUid();
        final String eid = String.valueOf(data.getId());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(eid)
            .set(data)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Intent intent = new Intent(getApplicationContext(), CreateGroupEventActivity.class);
                    setResult(Activity.RESULT_OK, intent);
                    if (Constants.USE_DEBUG) {
                        Log.v(TAG, "success to make a new event.");
                    }
                }
            });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // check if the request code is same as what is passed  here it is ACTIVITY_REQUEST_CODE
        if (requestCode == ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            newPlace = new PlaceData(0, null,
                                    data.getStringExtra("feature").toString(),
                                    data.getStringExtra("location").toString(),
                                    data.getDoubleExtra("latitude", 0),
                                    data.getDoubleExtra("longitude", 0));
            feature.setText(newPlace.getFeature());
            address.setText(newPlace.getAddress());

            row7c.setVisibility(View.GONE);
            setVisibleMap(true);
            mapFragment.getMapAsync(this);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = false;

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.done:
                ret = processEvent();
                if (ret) {
                    Intent intent = new Intent();
                    setResult(Activity.RESULT_OK, intent);
                }
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_done, menu);
        return true;
    }

    private boolean processEvent() {
        if (name.getText().toString().matches("")) {
            name.setError(getResources().getString(R.string.require));
            return false;
        }

        if (activity.getText().toString().equals("+")) {
            String[] activityItems = getResources().getStringArray(R.array.activity_entries);
            activity.setText(activityItems[0]);
        }

        String time = startTime.getText().toString();
        if (time.equals("+")) {
            time = "00:00 AM";
        }
        fromDate = startDate.getText().toString() + " " + time;

        time = endTime.getText().toString();
        if (time.equals("+")) {
            time = "00:00 AM";
        }
        toDate = endDate.getText().toString() + " " + time;

        Log.v(TAG, "fromDate: " + fromDate);
        Log.v(TAG, "toDate: " + toDate);

        if (!endDate.getText().toString().equals("+")) {
            if (TimeUtil.CompareDate(fromDate, toDate) > 0) {
                showAlertDialog();
                return false;
            }
        }

        boolean ret = false;
        if (eventId > Constants.ID_NONE) {
            ret = updateEvent(eventId);
        } else {
            ret = createEvent();
        }
        return ret;
    }

    private long createNewPlace(PlaceData place) {
        long placeId;

        place.setImage(saveImage());
        placeId = dbPlaces.createPlace(place);
        place.setId(placeId);
        return placeId;
    }

    private boolean updateEvent(long id) {
        boolean isUpdated = false;
        long placeId = data.getPlaceId();

        if (newPlace != null) {
            if (placeId > ID_NONE) {
                PlaceData oldPlace = dbPlaces.readPlace(placeId);
                if (oldPlace == null || newPlace.getLatitude() != oldPlace.getLatitude()
                        || newPlace.getLongitude() != oldPlace.getLongitude()) {
                    placeId = createNewPlace(newPlace);
                    isUpdated = true;
                }
            } else {
                placeId = createNewPlace(newPlace);
                isUpdated = true;
            }
        }

        if (isUpdated
            || !data.getName().equals(name.getText().toString())
            || data.getActivityType() != positionActivity
            || !data.getDate().equals(fromDate)
            || !data.getEndTime().equals(toDate)
            || !data.getDescription().equals(note.getText().toString())) {
            isUpdated = true;
        }

        if (isUpdated) {
            GroupEventData event = new GroupEventData(id,
                    null,
                    name.getText().toString(),
                    positionActivity,
                    0,
                    fromDate,
                    placeId,
                    false,
                    note.getText().toString(),
                    groupId,
                    0,
                    toDate);
            if (Constants.USE_LOCAL_DB) {
                dbEvents.updateEvent(event);
            } else {
                setGroupEventInfo(false, event);
            }
        }
        return isUpdated;
    }

    private boolean createEvent() {
        long placeId = Constants.ID_NONE;

        if (newPlace != null) {
            newPlace.setImage(saveImage());
            placeId = dbPlaces.createPlace(newPlace);
            newPlace.setId(placeId);
        }

        GroupEventData event = new GroupEventData(0,
                                        null,
                                        name.getText().toString(),
                                        positionActivity,
                                        0,
                                        fromDate,
                                        placeId,
                                        false,
                                        note.getText().toString(),
                                        groupId,
                                        0,
                                        toDate);
        if (Constants.USE_LOCAL_DB) {
            dbEvents.createEvent(event);
        } else {
            Random rand = new Random();
            int int_random = rand.nextInt(100);

            SimpleDateFormat idFormat = new SimpleDateFormat(Constants.DATETIME_FORMAT_FILENAME_SECOND,
                    Locale.getDefault());
            String idString = idFormat.format(new Date()) + String.valueOf(int_random);
            event.setId(Long.parseLong(idString));
            setGroupEventInfo(true, event);
        }
        return true;
    }
}

/*
var startfulldate = admin.firestore.Timestamp.fromDate(new Date(1556062581000));
    db.collection('mycollection')
      .where('start_time', '<=', startfulldate)
      .get()
      .then(snapshot => {
            var jsonvalue: any[] = [];
            snapshot.forEach(docs => {
              jsonvalue.push(docs.data())
               })
                 res.send(jsonvalue);
                 return;
                }).catch( error => {
                    res.status(500).send(error)
                });
 */
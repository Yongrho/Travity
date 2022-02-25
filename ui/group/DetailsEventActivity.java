package com.travity.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.travity.R;

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
import com.travity.ui.workout.WorkoutOnFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
//import Com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker;
//import Com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DetailsEventActivity extends AppCompatActivity {
    private static final String TAG = "DetailsEventActivity";
    private static final String GROUP_ID = "group_id";
    private static final String EVENT_ID = "event_id";
    private static final long ID_NONE = 0l;
    private static final String FLAG_TITLE = "title";
    private static final String FLAG_UPComING = "upComing";
    private static final int ACTIVITY_REQUEST_CODE = 1;

    EventsSQLiteHelper dbEvents;
    PlacesSQLiteHelper dbPlaces;
    GroupsSQLiteHelper dbGroups;
    GroupEventData event;

    TextView name, activity, date, groupName;
    TextView feature, address, description;
    ImageView map;
    TableRow row7, row7a, row7b;

    long eventId;
    boolean isUpdated = false;
    String groupNameString = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_event);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                eventId = extras.getLong(EVENT_ID);
                groupNameString = extras.getString(Constants.FLAG_GROUPNAME);
            }
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Group Event");
        actionBar.setDisplayHomeAsUpEnabled(true);

        name = findViewById(R.id.name);
        activity = findViewById(R.id.type);
        date = findViewById(R.id.date);
        groupName = findViewById(R.id.group_name);

        row7 = findViewById(R.id.tr7);
        row7a = findViewById(R.id.tr7a);
        row7b = findViewById(R.id.tr7b);

        feature = findViewById(R.id.feature);
        address = findViewById(R.id.address);
        map = findViewById(R.id.map);
        description = findViewById(R.id.description);

        // open the database of the application context
        dbPlaces = new PlacesSQLiteHelper(this);

        if (Constants.USE_LOCAL_DB) {
            dbEvents = new EventsSQLiteHelper(this);
            dbGroups = new GroupsSQLiteHelper(this);

            event = (GroupEventData) dbEvents.readEvent(eventId);
            refreshEvent(event);
        } else {
            getGroupEventInfoFromServer(String.valueOf(eventId));
        }
    }

    private void refreshEvent(GroupEventData data) {
        String[] activityItems = getResources().getStringArray(R.array.activity_entries);

        name.setText(data.getName());
        activity.setText(activityItems[data.getActivityType()]);
        date.setText(data.getDate());
        groupName.setText(groupNameString);

        long placeId = data.getPlaceId();
        if (placeId > Constants.ID_NONE) {
            PlaceData place = dbPlaces.readPlace(placeId);
            if (place == null) {
                row7.setVisibility(View.GONE);
                row7a.setVisibility(View.GONE);
                row7b.setVisibility(View.GONE);

                TableRow row6 = findViewById(R.id.tr6);
                row6.setVisibility(View.GONE);
                View view = findViewById(R.id.view);
                view.setVisibility(View.GONE);
            } else {
                Log.v(TAG, place.toString());

                feature.setText(place.getFeature());
                address.setText(place.getAddress());

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), place.getImage());
                Bitmap bmp = decodeFile(file);
                if (bmp != null) {
                    map.setImageBitmap(bmp);
                }
            }
        } else {
            row7.setVisibility(View.GONE);
            row7a.setVisibility(View.GONE);
            row7b.setVisibility(View.GONE);
        }
        description.setText(data.getDescription());
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);

            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE = 70;
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while(true){
                if (width_tmp > REQUIRED_SIZE || height_tmp > REQUIRED_SIZE) {
                    break;
                }
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }

    private void getGroupEventInfoFromServer(String eid) {
        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("events").document(eid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                GroupEventData ged = documentSnapshot.toObject(GroupEventData.class);
                refreshEvent(ged);
            }
        });
    }

    // This method is called when the next activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the activity with an OK result
        if (requestCode == ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (Constants.USE_LOCAL_DB) {
                    event = (GroupEventData) dbEvents.readEvent(eventId);
                    refreshEvent(event);
                } else {
                    getGroupEventInfoFromServer(String.valueOf(eventId));
                }
                isUpdated = true;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isUpdated) {
                    intent = new Intent();
                    setResult(Activity.RESULT_OK, intent);
                }
                finish();
                return true;
            case R.id.action_edit:
                intent = new Intent(getBaseContext(), CreateGroupEventActivity.class);
                intent.putExtra("event_id", event.getId());
                intent.putExtra(GROUP_ID, event.getGroupId());
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
                return true;

            case R.id.action_remove:
                if (Constants.USE_LOCAL_DB) {
                    dbEvents.deleteEvent(event);
                } else {
                    final FirebaseFirestore db = FirebaseFirestore.getInstance();
                    final String eid = String.valueOf(eventId);
                    db.collection("events").document(eid)
                            .delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "DocumentSnapshot successfully deleted!");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error deleting document", e);
                                }
                            });
                }
                intent = new Intent();
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_edit, menu);
        return true;
    }
}
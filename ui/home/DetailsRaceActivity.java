package com.travity.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.travity.R;

import com.travity.data.Constants;
import com.travity.data.GroupEventData;
import com.travity.data.RaceData;
import com.travity.data.db.EventsSQLiteHelper;
import com.travity.data.PlaceData;
import com.travity.data.db.PlacesSQLiteHelper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

public class DetailsRaceActivity extends AppCompatActivity {
    private static final String TAG = "DetailsEventActivity";
    private static final String GROUP_ID = "group_id";
    private static final String EVENT_ID = "event_id";
    private static final long ID_NONE = 0l;
    private static final String FLAG_TITLE = "title";
    private static final String FLAG_UPComING = "upComming";
    private static final int ACTIVITY_REQUEST_CODE = 1;

    EventsSQLiteHelper dbEvents;
    PlacesSQLiteHelper dbPlaces;
    RaceData raceData;

    TextView name, activity, date;
    TextView feature, address, description;
    TextView host, phone, category, period, homepage;
    ImageView map;
    TableRow row7, row7a, row7b;

    long raceId;
    boolean upComming;
    String title;
    boolean isUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_race);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                raceId = extras.getLong(Constants.ID_EVENT);
                title = extras.getString(Constants.FLAG_TITLE);
            }
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Race");
        actionBar.setDisplayHomeAsUpEnabled(true);

        // open the database of the application context
        dbPlaces = new PlacesSQLiteHelper(this);
        if (Constants.USE_LOCAL_DB) {
            dbEvents = new EventsSQLiteHelper(this);
            raceData = (RaceData) dbEvents.readEvent(raceId);
            refreshEvent(raceData);
        } else {
            getRaceInfoFromServer(String.valueOf(raceId));
        }

    }

    private void refreshEvent(RaceData rd) {
        String[] activityItems = getResources().getStringArray(R.array.activity_entries);

        Log.v(TAG, rd.toString());
        name = findViewById(R.id.name);
        name.setText(rd.getName());
        activity = findViewById(R.id.type);
        activity.setText(activityItems[rd.getActivityType()]);

        date = findViewById(R.id.date);
        date.setText(rd.getDate());

        host = findViewById(R.id.host);
        host.setText(rd.getHost());

        phone = findViewById(R.id.phone);
        phone.setText(rd.getPhone());

        category = findViewById(R.id.event);
        category.setText(rd.getCategory());

        homepage = findViewById(R.id.homepage);
        homepage.setText(rd.getHomepage());

        long placeId = rd.getPlaceId();
        if (placeId > Constants.ID_NONE) {
            PlaceData place = dbPlaces.readPlace(placeId);
            if (place == null) {
                row7 = findViewById(R.id.tr7);
                row7a = findViewById(R.id.tr7a);
                row7b = findViewById(R.id.tr7b);
                row7.setVisibility(View.GONE);
                row7a.setVisibility(View.GONE);
                row7b.setVisibility(View.GONE);

                TableRow row6 = findViewById(R.id.tr6);
                row6.setVisibility(View.GONE);
                View view = findViewById(R.id.view);
                view.setVisibility(View.GONE);
            } else {
                Log.v(TAG, place.toString());
                feature = findViewById(R.id.feature);
                feature.setText(place.getFeature());
                address = findViewById(R.id.address);
                address.setText(place.getAddress());

                map = findViewById(R.id.map);
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), place.getImage());
                Bitmap bmp = decodeFile(file);
                if (bmp != null) {
                    map.setImageBitmap(bmp);
                }
            }
        } else {
            row7 = findViewById(R.id.tr7);
            row7a = findViewById(R.id.tr7a);
            row7b = findViewById(R.id.tr7b);
            row7.setVisibility(View.GONE);
            row7a.setVisibility(View.GONE);
            row7b.setVisibility(View.GONE);
        }

        description = findViewById(R.id.description);
        description.setText(rd.getDescription());
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
                if(width_tmp > REQUIRED_SIZE || height_tmp > REQUIRED_SIZE)
                    break;
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

    private void getRaceInfoFromServer(String eid) {
        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("events").document(eid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                RaceData rd = documentSnapshot.toObject(RaceData.class);
                refreshEvent(rd);
            }
        });
    }

    // This method is called when the next activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the activity with an OK result
        if (requestCode == Constants.REQUEST_CODE_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                if (Constants.USE_LOCAL_DB) {
                    raceData = (RaceData) dbEvents.readEvent(raceId);
                    refreshEvent(raceData);
                } else {
                    getRaceInfoFromServer(String.valueOf(raceId));
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
            case R.id.add:
                intent = new Intent(getBaseContext(), CreateRaceActivity.class);
                intent.putExtra(Constants.ID_EVENT, raceData.getId());
                startActivityForResult(intent, Constants.REQUEST_CODE_ACTIVITY);
                return true;
/*				
            case R.id.action_remove:
                dbEvents.deleteEvent(race);
                intent = new Intent();
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;
*/				
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_add, menu);
        return true;
    }
}
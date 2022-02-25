package com.travity.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.travity.R;
import com.travity.data.ActivityType;
import com.travity.data.EventData;
import com.travity.data.EventType;
import com.travity.data.EventsAdapter;
import com.travity.data.db.EventsSQLiteHelper;
import com.travity.data.GroupData;
import com.travity.data.PlacesAdapter;
import com.travity.data.Member;
import com.travity.data.db.MembersSQLiteHelper;
import com.travity.data.db.GroupMembersSQLiteHelper;

import com.travity.data.PlacesAdapter;
import com.travity.ui.workout.WorkoutOnFragment;
import com.travity.ui.home.CreateRaceActivity;
import com.travity.ui.group.CreateGroupEventActivity;
//import Com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker;
//import Com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog;

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

public class SearchLocationActivity extends AppCompatActivity {
    private static final String TAG = "SearchLocationActivity";
    private static final String GROUP_ID = "group_id";

    EventsSQLiteHelper dbEvents;
    MembersSQLiteHelper dbMembers;
    GroupMembersSQLiteHelper dbGMs;

    EditText etSearch;
    ListView lvGroups;
    int groupID;
    int eventType;
    ArrayList<String> arrayList;
    PlacesAdapter placesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_location);
/*
        // open the database of the application context
        dbEvents = new EventsSQLiteHelper(this);
        dbMembers = new MembersSQLiteHelper(this);
        dbGMs = new GroupMembersSQLiteHelper(this);
*/
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                eventType = extras.getInt("eventType");
                groupID = extras.getInt(GROUP_ID);
            }
        }

        etSearch = findViewById(R.id.edittext_search);
        arrayList = new ArrayList<String>();
        Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
/*
        try {
            double latitude;
            double longitude;
            String addressLine;
            int i = 0;

            List<Address> address = geoCoder.getFromLocationName("Halifax", 10);
            placesAdapter = new PlacesAdapter(getBaseContext(), address);

            Log.v(TAG, "address.size(): " + String.valueOf(address.size()));
            while (i < address.size()) {
                latitude = address.get(i).getLatitude();
                longitude = address.get(i).getLongitude();
                addressLine = address.get(i).getAddressLine(0);
                Log.v(TAG, "latitude: " + String.valueOf(latitude));
                Log.v(TAG, "longitude: " + String.valueOf(longitude));
                Log.v(TAG, "address line 1: " + address.get(0).getAddressLine(0));
                Log.v(TAG, "address: " + address.toString());
                // this example should be changed with db
//                arrayList.add(addressLine);
                i++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, arrayList);

        lvGroups = (ListView) findViewById(R.id.listview_location);
        lvGroups.setAdapter(placesAdapter);
        lvGroups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Address address= (Address) parent.getAdapter().getItem(position);

                Intent intent;
                if (eventType == 0) {
                    intent = new Intent(getApplicationContext(), CreateGroupEventActivity.class);
                } else {
                    intent = new Intent(getApplicationContext(), CreateRaceActivity.class);
                }
                intent.putExtra("feature", address.getFeatureName());
                intent.putExtra("latitude", address.getLatitude());
                intent.putExtra("longitude", address.getLongitude());
                intent.putExtra("location", address.getAddressLine(0));
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Location");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.profile:
//                createEvent();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_profile, menu);
        return true;
    }
}
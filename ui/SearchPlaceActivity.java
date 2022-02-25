package com.travity.ui;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.travity.R;
import com.travity.data.EventData;
import com.travity.data.GroupData;
import com.travity.data.PlaceData;
import com.travity.data.PlacesAdapter;
import com.travity.data.db.EventsSQLiteHelper;
import com.travity.data.db.GroupMembersSQLiteHelper;
import com.travity.data.db.MembersSQLiteHelper;
import com.travity.data.db.PlacesSQLiteHelper;
import com.travity.ui.home.CreateRaceActivity;
import com.travity.ui.group.CreateGroupEventActivity;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SearchPlaceActivity extends AppCompatActivity {
    private static final String TAG = "SearchLocationActivity";
    private static final String GROUP_ID = "group_id";

    PlacesSQLiteHelper dbPlaces;
//    MembersSQLiteHelper dbMembers;
//    GroupMembersSQLiteHelper dbGMs;

//    EditText etSearch;
    ListView lvGroups;
//    int groupID;
    int eventType;
    ArrayList<PlaceData> arrayList;
    PlacesAdapter placesAdapter;
    ArrayList<PlaceData> places;
    PlaceData newPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_place);

        // open the database of the application context
        dbPlaces = new PlacesSQLiteHelper(this);

        String apiKey = getString(R.string.api_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME,
                                                        Place.Field.ADDRESS,
                                                        Place.Field.LAT_LNG));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName() + ", " +  place.getLatLng());
                Log.i(TAG, "Place: " + place.getName() + ", " +  place.getAddress());

                boolean found = false;
                PlaceData placeData;

                for (int i = 0; i < places.size(); i++) {
                    placeData = (PlaceData) places.get(i);
                    if (place.getName().equals(placeData.getFeature())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    newPlace = new PlaceData(0,
                                                null,
                                                place.getName(),
                                                place.getAddress(),
                                                place.getLatLng().latitude,
                                                place.getLatLng().longitude);
                    dbPlaces.createPlace(newPlace);
                }
                updatePlace(newPlace);
            }


            @Override
            public void onError(@NonNull Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

/*
        multiDexEnabled true
        implementation 'com.google.android.libraries.places:places:1.1.0'
        <string name="api_key">AIzaSyCr69G_Hryw9iN2dKOpYZlabMkLKEbSw6c</string>
        <activity
            android:name=".ui.SearchPlaceActivity"
            android:label="Place"/>

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

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, arrayList);

*/
        placesAdapter = new PlacesAdapter(getBaseContext(), arrayList);
        lvGroups = (ListView) findViewById(R.id.listview);
//        lvGroups.setAdapter(placesAdapter);
        lvGroups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Address address= (Address) parent.getAdapter().getItem(position);
                // update group information
                PlaceData entry= (PlaceData) parent.getAdapter().getItem(position);
                updatePlace(entry);
/*
                Intent intent;
                if (eventType == 0) {
                    intent = new Intent(getApplicationContext(), CreateGroupEventActivity.class);
                } else {
                    intent = new Intent(getApplicationContext(), CreateRaceActivity.class);
                }
                intent.putExtra("feature", entry.getFeature());
                intent.putExtra("latitude", entry.getLatitude());
                intent.putExtra("longitude", entry.getLongitude());
                intent.putExtra("location", entry.getAddress());
                setResult(Activity.RESULT_OK, intent);
 */
                finish();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Location");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        refreshGroups();
    }

    private void refreshGroups() {
        if (places != null) {
            places.clear();
        }

        places = dbPlaces.getAllPlaces();
        if (places.size() > 0) {
            placesAdapter.setGroups(places);
            lvGroups.setAdapter(placesAdapter);
        } else {
            lvGroups.setAdapter(null);
        }
    }

    private void updatePlace(PlaceData place) {
        Intent intent;
        if (eventType == 0) {
            intent = new Intent(getApplicationContext(), CreateGroupEventActivity.class);
        } else {
            intent = new Intent(getApplicationContext(), CreateRaceActivity.class);
        }
        intent.putExtra("feature", place.getFeature());
        intent.putExtra("latitude", place.getLatitude());
        intent.putExtra("longitude", place.getLongitude());
        intent.putExtra("location", place.getAddress());
        setResult(Activity.RESULT_OK, intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.options_menu_profile, menu);
        return true;
    }
}
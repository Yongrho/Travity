package com.travity.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.travity.ProfileActivity;
import com.travity.data.Constants;
import com.travity.data.EventData;
import com.travity.data.EventType;
import com.travity.data.EventsRecyclerViewAdapter;
import com.travity.data.GroupEventData;
import com.travity.data.db.EventsSQLiteHelper;

import com.travity.R;

import com.travity.data.EventsAdapter;
import com.travity.ui.workout.WorkoutOnFragment;
import com.travity.ui.group.CreateGroupEventActivity;
import com.travity.ui.group.DetailsEventActivity;
import com.travity.util.textdrawable.ColorGenerator;
import com.travity.util.textdrawable.TextDrawable;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeEventsFragment extends Fragment {
    private static final String TAG = "HomeEventsFragment";

    EventsSQLiteHelper dbEvents;
    ArrayList<EventData> events = new ArrayList<EventData>();
    private RecyclerView recyclerView;
    EventsRecyclerViewAdapter eventsAdapter;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_events, container, false);

        // open the database of the application context
        if (Constants.USE_LOCAL_DB) {
            dbEvents = new EventsSQLiteHelper(getContext());
        }

        recyclerView = (RecyclerView) root.findViewById(R.id.sectioned_recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        eventsAdapter = new EventsRecyclerViewAdapter(getContext(), events);
        eventsAdapter.setOnItemClickListener(new EventsRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v,  int position) {
                EventData entry= (EventData) eventsAdapter.getItem(position);

                Intent intent;
                if (entry.getEventType() == 0) {
                    intent = new Intent(getContext(), DetailsEventActivity.class);
                    intent.putExtra(Constants.ID_EVENT, entry.getId());
//                    intent.putExtra(Constants.FLAG_UPCOMING, true);
                } else {
                    intent = new Intent(getContext(), DetailsRaceActivity.class);
                    intent.putExtra(Constants.ID_EVENT, entry.getId());
                }
                startActivityForResult(intent, Constants.REQUEST_CODE_ACTIVITY);
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a new race
                Intent intent = new Intent(getActivity(), CreateRaceActivity.class);
                intent.putExtra(Constants.ID_EVENT, Constants.ID_NONE);
                startActivityForResult(intent, Constants.REQUEST_CODE_ACTIVITY);
            }
        });

        setHasOptionsMenu(true);
        updateActionBar();

        if (Constants.USE_LOCAL_DB) {
            events = dbEvents.getAllEvents(Constants.ID_NONE);
            refreshEvents();
        } else {
            getEventsFromServer();
        }
        return root;
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

    private void refreshEvents() {
        if (events.size() > 0) {
            eventsAdapter.setItems(events);
            recyclerView.setAdapter(eventsAdapter);
        } else {
            recyclerView.setAdapter(null);
        }
    }

    private void getEventsFromServer() {
/*
        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                EventData ed = document.toObject(EventData.class);
                                events.add(ed);
                            }
                            refreshEvents();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

 */
    }

    // This method is called when the next activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the activity with an OK result
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.REQUEST_CODE_PROFILE) {
                updateActionBar();
            } else if (requestCode == Constants.REQUEST_CODE_ACTIVITY) {
                events.clear();
                if (Constants.USE_LOCAL_DB) {
                    events = dbEvents.getAllEvents(Constants.ID_NONE);
                    refreshEvents();
                } else {
                    getEventsFromServer();
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_menu_home, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
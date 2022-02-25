package com.travity.ui.group;

//import android.app.Activity;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

//import com.travity.ui.group.NotificationsViewModel;
import com.travity.data.Constants;
import com.travity.data.EventData;
import com.travity.data.EventType;
import com.travity.data.GroupData;
import com.travity.data.GroupEventData;
import com.travity.data.db.EventsSQLiteHelper;
//import com.google.android.material.tabs.TabLayout;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

//import androidx.viewpager.widget.ViewPager;

import com.travity.R;
import com.travity.data.EventsAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GroupsEventsFragment extends Fragment {
    private static final String TAG = "GroupsEventsFragment";
    private static final String GROUP_ID = "group_id";
    private static final String EVENT_ID = "event_id";
    private static final String FLAG_TITLE = "title";
    private static final int ACTIVITY_REQUEST_CODE = 1;

    EventsSQLiteHelper dbEvents;
    ArrayList<EventData> events = new ArrayList<EventData>();
    EventsAdapter eventsAdapter;
    ListView lvEvents;
    long groupId;
    GroupsSharedViewModel viewModel;
    GroupData groupData;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_listview_groupevent, container, false);

        setHasOptionsMenu(true);

        String[] users = { "upcoming", "past"};
        Spinner spin = (Spinner) root.findViewById(R.id.upcoming);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, users);
        adapter.setDropDownViewResource(R.layout.spinner_list_dropdown);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);

        if (Constants.USE_LOCAL_DB) {
            // open the database of the application context
            dbEvents = new EventsSQLiteHelper(getContext());
        }
        eventsAdapter = new EventsAdapter(getContext(), events);

        lvEvents = (ListView) root.findViewById(R.id.listview);
        lvEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // update event information
                EventData entry= (EventData) parent.getAdapter().getItem(position);
                Intent intent = new Intent(getContext(), DetailsEventActivity.class);
                intent.putExtra(Constants.ID_EVENT, entry.getId());
                intent.putExtra(Constants.ID_GROUP, groupId);
                intent.putExtra(Constants.FLAG_GROUPNAME, groupData.getName());
                startActivity(intent);
            }
        });

        // initialise ViewModel here
        viewModel = new ViewModelProvider(requireActivity()).get(GroupsSharedViewModel.class);
        viewModel.getGroupId().observe(getViewLifecycleOwner(), new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long id) {
                groupId = id;
                refreshEvents();
            }
        });

        viewModel.getGroupData().observe(getViewLifecycleOwner(), new Observer<GroupData>() {
            @Override
            public void onChanged(@Nullable GroupData data) {
                groupData = data;
                if (Constants.USE_LOCAL_DB) {
                    events = dbEvents.getAllEvents(groupId);
                    refreshEvents();
                } else {
                    getGroupEventsFromServer(groupData.getId());
                }
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CreateGroupEventActivity.class);
                intent.putExtra(Constants.ID_EVENT, Constants.ID_NONE);
                intent.putExtra(Constants.ID_GROUP, groupData.getId());
                intent.putExtra(Constants.FLAG_ACTIVITY, groupData.getActivity());
                startActivityForResult(intent, Constants.REQUEST_CODE_ACTIVITY);
            }
        });
        return root;
    }

    private void refreshEvents() {
        if (events.size() > 0) {
            eventsAdapter.setEvents(events);
            lvEvents.setAdapter(eventsAdapter);
        } else {
            lvEvents.setAdapter(null);
        }
    }

    private void getGroupEventsFromServer(long gid) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        Date todayCalendar = cal.getTime();
        Date todayDate = new Date();

        try {
            todayDate = simpleDateFormat.parse(simpleDateFormat.format(todayCalendar));
        } catch (Exception e) {
        }

        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                /*                .whereEqualTo("groupId", gid)
                              .startAt("startTime", ">=", "1506816000") */
 /*               .whereGreaterThan("date", todayDate)*/
             .whereLessThan("date", todayDate)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                GroupEventData ged = document.toObject(GroupEventData.class);
                                Log.v(TAG, ged.toString());
//                                events.add(ged);
                            }
                            refreshEvents();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    // This method is called when the next activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the activity with an OK result
//        if (requestCode == ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                events.clear();
                if (Constants.USE_LOCAL_DB) {
                    events = dbEvents.getAllEvents(groupId);
                    refreshEvents();
                } else {
                    getGroupEventsFromServer(groupData.getId());
                }
            }
//        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavHostFragment.findNavController(GroupsEventsFragment.this)
                        .navigate(R.id.navigation_groups);
                return true;
            default:
                break;
        }
        return false;
    }
}
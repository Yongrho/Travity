package com.travity.ui.workout;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.fragment.NavHostFragment;

import com.travity.R;
import com.travity.data.EventType;
import com.travity.data.WorkoutIntervalData;
import com.travity.data.db.EventsSQLiteHelper;
import com.travity.data.db.WorkoutIntervalsSQLiteHelper;
import com.travity.ui.group.CreateGroupEventActivity;
import com.travity.ui.group.GroupsEventsFragment;

import java.util.ArrayList;

public class WorkoutIntervalsActivity extends AppCompatActivity {
    private static final String TAG = "WorkoutIntervalsActivity";
    private static final int TEXT_SIZE = 14;

    private SharedViewModel viewModel;
    private TableLayout tableLayout;

    public com.travity.ui.workout.LocationService locationService;
    long createTime;

    private final BroadcastReceiver intervalUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tableLayout.removeAllViews();
            refreshWorkoutInterval();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_intervals);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // get LocationService
        locationService = LocationServiceController.getInstance().getService();
        tableLayout = (TableLayout) findViewById(R.id.table);

        LocalBroadcastManager.getInstance(this).registerReceiver(intervalUpdateReceiver,
                                        new IntentFilter("IntervalUpdated"));

        refreshWorkoutInterval();
    }

    private void refreshWorkoutInterval() {
        ArrayList<WorkoutIntervalData> splitsList = locationService.splitsList;
        WorkoutIntervalData wid;

        addItem(getResources().getString(R.string.caps_km),
                getResources().getString(R.string.average),
                getResources().getString(R.string.elevation), 16);

        for (int i = 0; i < splitsList.size(); i++) {
            wid = splitsList.get(i);
            addItem(String.valueOf(wid.getDistance()),
                    String.valueOf(wid.getAverage()),
                    String.valueOf(wid.getElevation()), 14);
        }
    }

    private void addItem(String distance, String averagePace, String elevation, int textSize) {
        TableRow tbrow = new TableRow(this);
        TextView tv1 = new TextView(this);
        tv1.setText(distance);
        tv1.setTextColor(Color.BLACK);
        tv1.setGravity(Gravity.LEFT);
        tv1.setTextSize(textSize);
        tv1.setPadding(10, 15, 20, 15);
        tbrow.addView(tv1);
        TextView tv2 = new TextView(this);
        tv2.setText(averagePace);
        tv2.setTextColor(Color.BLACK);
        tv2.setGravity(Gravity.LEFT);
        tv2.setTextSize(textSize);
        tv2.setPadding(20, 15, 20, 15);
        tbrow.addView(tv2);
        TextView tv3 = new TextView(this);
        tv3.setText(elevation);
        tv3.setTextColor(Color.BLACK);
        tv3.setGravity(Gravity.RIGHT);
        tv3.setTextSize(textSize);
        tv3.setPadding(20, 15, 0, 15);
        tbrow.addView(tv3);
        tableLayout.addView(tbrow);

        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
        ));
        v.setBackgroundColor(Color.parseColor("#E3E4E6"));
        tableLayout.addView(v);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return false;
    }
}
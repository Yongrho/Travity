package com.travity.ui.home;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.AdapterView;
import android.widget.ListView;

import com.travity.R;
import com.travity.data.EventData;
import com.travity.data.EventsRecyclerViewAdapter;
import com.travity.data.HeaderItem;
import com.travity.data.ListItem;
import com.travity.data.WorkoutMonthlyData;
import com.travity.data.WorkoutResultData;
import com.travity.data.WorkoutResultsAdapter;
//import com.travity.data.WorkoutResultsItemAdapter;
//import com.travity.data.WorkoutResultsSectionModel;
import com.travity.data.WorkoutResultsHeader;
import com.travity.data.db.WorkoutResultsSQLiteHelper;
import com.travity.ui.group.DetailsEventActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import android.app.Activity;

public class HomeHistoryFragment extends Fragment {
    private static final String TAG = "HomeHistoryFragment";
    private static final int ACTIVITY_REQUEST_CODE = 1;

    WorkoutResultsSQLiteHelper dbResults;
    ArrayList<ListItem> items= new ArrayList<ListItem>();
    ArrayList<WorkoutResultData> results = new ArrayList<WorkoutResultData>();
    WorkoutResultsAdapter resultsAdapter;
    ListView lvResults;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // open the database of the application context
        dbResults = new WorkoutResultsSQLiteHelper(getContext());

        View root = inflater.inflate(R.layout.fragment_listview, container, false);

        resultsAdapter = new WorkoutResultsAdapter(getContext(), null);
        lvResults = (ListView) root.findViewById(R.id.listview);
        lvResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // update event information
                ListItem item = (ListItem) parent.getAdapter().getItem(position);
                if (!item.isHeader()) {
                    WorkoutResultData result = (WorkoutResultData) item;
                    Intent intent = new Intent(getContext(), DetailsResultActivity.class);
                    intent.putExtra("resultId", result.getId());
                    startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
                } /*else {

//                    resultsAdapter.changeHeader(position);
                    int visiblePosition = lvResults.getFirstVisiblePosition();
                    View listview = lvResults.getChildAt(position - visiblePosition);
//                    View listview = lvResults.getChildAt(position);
                    lvResults.getAdapter().getView(position, listview, lvResults);

                } */
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.add);
        fab.setVisibility(View.GONE);

        refreshItems();
        return root;
    }

    String month = null;
//    float totalDistance;
//    int count, totalTime, totalAverage;
    ArrayList<WorkoutMonthlyData> wmdArrayList;

    private WorkoutResultsHeader initWorkoutResultRecord(String tmpMonth, WorkoutResultData result) {
        boolean found = false;
        month = tmpMonth;
/*
        count = 1;
        totalDistance = result.getDistance();
        totalTime = result.getTime();
        totalAverage = result.getAverage();
*/
/*
        for (int i = 0; i < wmdArrayList.size(); i++) {
            WorkoutMonthlyData wmd = wmdArrayList.get(i);
            if (wmd.getActivityType() == result.getActivityType()
                    && wmd.getName().equals(tmpMonth)) {
                found = true;
                break;
            }
        }

        if (!found) {
            WorkoutMonthlyData wmd = new WorkoutMonthlyData(0, 1, 0,
                                                        tmpMonth,
                                                        result.getActivityType(),
                                                        result.getEventType(),
                                                        0, 0,
                                                        result.getDistance(),
                                                        result.getTime(),
                                                        result.getAverage());
            wmdArrayList.add(wmd);
        }
*/
        wmdArrayList = new ArrayList<WorkoutMonthlyData>();
        WorkoutMonthlyData wmd = new WorkoutMonthlyData(0, 1, 0,
                tmpMonth,
                result.getActivityType(),
                result.getEventType(),
                0, 0,
                result.getDistance(),
                result.getTime(),
                result.getAverage());
        wmdArrayList.add(wmd);

//        WorkoutResultsHeader workoutResultsHeader = new WorkoutResultsHeader("tmp");
        WorkoutResultsHeader workoutResultsHeader = new WorkoutResultsHeader(tmpMonth);
        items.add(workoutResultsHeader);
        return workoutResultsHeader;
    }

    private void addWorkoutResultRecord(WorkoutResultData result) {
/*
        count++;
        totalDistance += result.getDistance();
        totalTime += result.getTime();
        totalAverage += result.getAverage();
*/
        String date = String.valueOf(result.getStartTime());
        String tmpMonth = date.substring(0, 7);

        for (int i = 0; i < wmdArrayList.size(); i++) {
            WorkoutMonthlyData wmd = wmdArrayList.get(i);
            if (wmd.getActivityType() == result.getActivityType()
                    && wmd.getName().equals(tmpMonth)) {
                wmd.addCount();
                wmd.addDistance(result.getDistance());
                wmd.addTime(result.getTime());
                wmd.addAverage(result.getAverage());
                return;
            }
        }

        WorkoutMonthlyData wmd = new WorkoutMonthlyData(0, 1, 0,
                tmpMonth,
                result.getActivityType(),
                result.getEventType(),
                0, 0,
                result.getDistance(),
                result.getTime(),
                result.getAverage());
        wmdArrayList.add(wmd);
    }

    private WorkoutResultsHeader calculateMonthlyReport(WorkoutResultsHeader workoutResultsHeader, WorkoutResultData result, boolean isLast) {
        String date = String.valueOf(result.getStartTime());
        String tmpMonth = date.substring(0, 7);

        if (isLast) {
            if (month == null) {
                items.clear();
//                wmdArrayList = new ArrayList<WorkoutMonthlyData>();
                workoutResultsHeader = initWorkoutResultRecord(tmpMonth, result);
            } else {
                addWorkoutResultRecord(result);
            }
            items.add(result);
//            workoutResultsHeader.setHeader(tmpMonth, count, totalDistance, totalTime, totalAverage);
            workoutResultsHeader.setHeader(wmdArrayList);
            return workoutResultsHeader;
        }

        if (month == null) {
            items.clear();
//            wmdArrayList = new ArrayList<WorkoutMonthlyData>();
            workoutResultsHeader = initWorkoutResultRecord(tmpMonth, result);
        } else if (month.equals(tmpMonth)) {
            addWorkoutResultRecord(result);
        } else {
//            workoutResultsHeader.setHeader(tmpMonth, count, totalDistance, totalTime, totalAverage);
            workoutResultsHeader.setHeader(wmdArrayList);
//            wmdArrayList = new ArrayList<WorkoutMonthlyData>();
            workoutResultsHeader = initWorkoutResultRecord(tmpMonth, result);
        }
        items.add(result);
        return workoutResultsHeader;
    }

    private void refreshItems() {
        WorkoutResultData result = null;

        month = null;
/*
        totalDistance = 0.0f;
        count = 0;
        totalTime = 0;
        totalAverage = 0;
        if (wmdArrayList != null) {
            wmdArrayList.clear();
        }
*/

        items.clear();
        results.clear();

        results = dbResults.getAllEvents();
        if (results.size() <= 0) {
            return;
        }

        WorkoutResultsHeader headerItem = null;
        for (int i = 0; i < results.size() - 1; i++) {
            headerItem = calculateMonthlyReport(headerItem, results.get(i), false);
        }
        headerItem = calculateMonthlyReport(headerItem, results.get(results.size() - 1), true);
        resultsAdapter.setItems(items);
        lvResults.setAdapter(resultsAdapter);
    }

    // This method is called when the next activity finishes
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the activity with an OK result
        if (requestCode == ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                refreshItems();
            }
        }
    }
}
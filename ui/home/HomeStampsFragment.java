package com.travity.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

//import com.travity.ui.group.NotificationsViewModel;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.travity.R;
import com.travity.data.ActivityType;
import com.travity.data.StampData;
import com.travity.data.StampType;
import com.travity.data.StampsAdapter;
import com.travity.data.StampsSectionModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class HomeStampsFragment extends Fragment {
    private static final String TAG = "HomeStampsFragment";

    protected static final String RECYCLER_VIEW_TYPE = "recycler_view_type";
//    private RecyclerViewType recyclerViewType;
    private RecyclerView recyclerView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recyclerview, container, false);

        recyclerView = (RecyclerView) root.findViewById(R.id.sectioned_recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        FloatingActionButton fab = root.findViewById(R.id.add);
        fab.setVisibility(View.GONE);

        populateRecyclerView();

        return root;
    }

    //populate recycler view
    private void populateRecyclerView() {
        ArrayList<StampsSectionModel> stampsSectionArrayList = new ArrayList<StampsSectionModel>();
        ArrayList<StampData> stamps = new ArrayList<StampData>();

        stamps.add(new StampData(0, null, 0, "20210618", ActivityType.ACTIVITY_RUNNING.getValue(),
                StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!"));
        stamps.add(new StampData(0, null, 0, "20210619", ActivityType.ACTIVITY_RUNNING.getValue(),
                StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!"));
        stamps.add(new StampData(0, null, 0, "20210620", ActivityType.ACTIVITY_RUNNING.getValue(),
                StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!"));
        stamps.add(new StampData(0, null, 0, "20210621", ActivityType.ACTIVITY_RUNNING.getValue(),
                StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!"));

        stampsSectionArrayList.add(new StampsSectionModel("Section 1", stamps));

        stamps = new ArrayList<StampData>();

        stamps.add(new StampData(0, null, 0, "20210601", ActivityType.ACTIVITY_RUNNING.getValue(),
                StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!"));
        stamps.add(new StampData(0, null, 0, "20210602", ActivityType.ACTIVITY_RUNNING.getValue(),
                StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!"));
        stamps.add(new StampData(0, null, 0, "20210603", ActivityType.ACTIVITY_RUNNING.getValue(),
                StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!"));
        stamps.add(new StampData(0, null, 0, "20210604", ActivityType.ACTIVITY_RUNNING.getValue(),
                StampType.STAMP_DISTANCE.getValue(), 10, "Great!!!"));

        stampsSectionArrayList.add(new StampsSectionModel("Section 2", stamps));
        StampsAdapter adapter = new StampsAdapter(getContext(), stampsSectionArrayList);
        recyclerView.setAdapter(adapter);
    }
}
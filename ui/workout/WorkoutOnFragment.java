package com.travity.ui.workout;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentPagerAdapter;
//import androidx.lifecycle.Observer;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
//
//import com.travity.ui.group.NotificationsViewModel;
//import com.google.android.material.tabs.TabLayout;
//import androidx.annotation.Nullable;

//import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//import androidx.viewpager.widget.ViewPager;

import android.widget.ImageButton;
import android.widget.TextView;

import com.travity.R;
import com.travity.data.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkoutOnFragment extends Fragment {
    private static final String TAG = "WorkoutOnFragment";

    private TextView tvTime;
    private TextView tvDistance;
    private TextView tvAveragePace;
    private static final int ONE_HOUR = 3600;
    private static final int ONE_MINUTE = 60;
    private static final int ONE_KILOMETER = 1000;
    ImageButton pauseButton, resumeButton, stopButton;

    public com.travity.ui.workout.LocationService locationService;

    private int workoutTime = 0;
    private SharedViewModel viewModel;
    String createTime;
    int activity;

    private BroadcastReceiver recordUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            double distance = extras.getDouble("km");
            String averagePace = extras.getString("averagePace");

            tvDistance.setText(String.format("%.2f", distance));
            tvAveragePace.setText(averagePace);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        locationService = LocationServiceController.getInstance().getService();
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                recordUpdateReceiver,
                new IntentFilter("RecordUpdated"));

        View root = inflater.inflate(R.layout.fragment_workout_on, container, false);
        tvTime = (TextView) root.findViewById(R.id.textView);
        tvDistance = (TextView) root.findViewById(R.id.distance);
        tvAveragePace = (TextView) root.findViewById(R.id.average_pace);

        pauseButton = root.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setRunningStatus(1);
                pauseButton.setVisibility(View.GONE);
                resumeButton.setVisibility(View.VISIBLE);
                WorkoutOnFragment.this.locationService.stopLogging();
            }
        });

        resumeButton = root.findViewById(R.id.start_button);
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setRunningStatus(2);
                resumeButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                WorkoutOnFragment.this.locationService.startLogging();
            }
        });
        resumeButton.setVisibility(View.GONE);

        stopButton = root.findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
                WorkoutOnFragment.this.locationService.stopUpdatingLocation();
                WorkoutOnFragment.this.locationService.stopLogging();

                Intent intent = new Intent(getContext(), WorkoutResultActivity.class);
                intent.putExtra("create_time", createTime);
                intent.putExtra("activity", activity);
                startActivity(intent);
            }
        });

        SimpleDateFormat fileNameDateTimeFormat = new SimpleDateFormat(Constants.DATETIME_FORMAT_AMPM);
        String createTime = fileNameDateTimeFormat.format(new Date());
//        viewModel.setCreateTime(Long.parseLong(createTime));
        viewModel.setCreateTime(createTime);
        start(1000);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.getCreateTime().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String time) {
                createTime = time;
            }
        });
        viewModel.getRunningStatus().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer status) {
                switch (status) {
                    case 0:
                        stop();
                        break;
                    case 1:
                        pauseButton.setVisibility(View.GONE);
                        resumeButton.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        resumeButton.setVisibility(View.GONE);
                        pauseButton.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }
        });
        viewModel.getActivity().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer activity_type) {
                activity = activity_type;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (recordUpdateReceiver != null) {
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(recordUpdateReceiver);
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    private Runnable runnableCode = null;
    private Handler handler = new Handler();

    void startDelayed(final int intervalMS, int delayMS) {
        runnableCode = new Runnable() {

            @Override
            public void run() {
                handler.postDelayed(runnableCode, intervalMS);
                String sTime = TimeUtil.getDurationString(workoutTime);
                tvTime.setText(sTime);
                viewModel.setName(sTime);
                workoutTime += 1;
            }
        };
        handler.postDelayed(runnableCode, delayMS);
    }

    void start(final int intervalMS) {
        startDelayed(intervalMS, 0);
    }

    void stop() {
        handler.removeCallbacks(runnableCode);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_menu_add, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                Intent intent = new Intent(getActivity(), WorkoutIntervalsActivity.class);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return false;
    }
}
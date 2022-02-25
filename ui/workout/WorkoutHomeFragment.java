package com.travity.ui.workout;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.travity.R;
import com.travity.ui.workout.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class WorkoutHomeFragment extends Fragment {
    private String[] mTitle = {"Workout", "Map"};
//    private OnFragmentInteractionListener mListener;
    ActionBar actionBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_notifications, container, false);
        return fragmentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // find views by id
        ViewPager viewPager = view.findViewById(R.id.viewpager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());

        // add your fragment
//        adapter.addFrag(new WorkoutIntervalsFragment(), "Tab1");
        adapter.addFrag(new WorkoutOnFragment(), "Tab2");
        adapter.addFrag(new WorkoutMapFragment(), "Tab3");

        // set adapter on viewpage
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(1);

        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);

        // Attach the page change listener inside the activity
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int old_position = 1;

            // This method will be invoked when a new page becomes selected.
            @Override
            public void onPageSelected(int position) {
                actionBar.setTitle(mTitle[position]);
            }

            // This method will be invoked when the current page is scrolled
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Code goes here
            }

            // Called when the scroll state changes:
            // SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING
            @Override
            public void onPageScrollStateChanged(int state) {
                // Code goes here
            }
        });
    }
}


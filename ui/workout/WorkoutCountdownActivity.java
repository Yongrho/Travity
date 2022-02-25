package com.travity.ui.workout;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.travity.R;
import com.travity.ui.workout.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkoutCountdownActivity extends AppCompatActivity {
    private static final String TAG = "WorkoutCountdownActivity";
    int counter = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_countdown);

        counter = 5;
        TextView textView = findViewById(R.id.counter);
        new CountDownTimer(5000, 1000){
            public void onTick(long millisUntilFinished){
                textView.setText(String.valueOf(counter));
                counter--;
            }
            public  void onFinish(){
                textView.setText("Go!!");
                startWorkout();
            }
        }.start();
    }

    private void startWorkout() {
        Intent intent = new Intent(getBaseContext(), WorkoutHomeActivity.class);
        startActivity(intent);
    }
}


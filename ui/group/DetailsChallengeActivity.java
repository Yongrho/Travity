package com.travity.ui.group;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;

import com.travity.R;
import gujc.directtalk9.model.UserModel;

import com.bumptech.glide.Glide;
import com.travity.data.ChallengeData;
import com.travity.data.Constants;
import com.travity.data.EventType;
import com.travity.data.GroupData;
import com.travity.data.GroupEventData;
import com.travity.data.PlaceData;
import com.travity.data.WorkoutResultData;
import com.travity.data.db.ChallengesSQLiteHelper;
import com.travity.data.db.EventsSQLiteHelper;
import com.travity.data.db.PlacesSQLiteHelper;
import com.travity.data.db.WorkoutResultsSQLiteHelper;
import com.travity.ui.workout.TimeUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Locale;

public class DetailsChallengeActivity extends AppCompatActivity {
    private static final String TAG = "DetailsChallenge";
//    private static final String GROUP_ID = "group_id";
//    private static final String EVENT_ID = "event_id";
//    private static final long ID_NONE = 0l;
//    private static final String FLAG_TITLE = "title";
//    private static final String FLAG_UPComING = "upComing";
    private static final int ACTIVITY_REQUEST_CODE = 1;

    ChallengesSQLiteHelper dbChallenges;
    WorkoutResultsSQLiteHelper dbWorkoutResults;
    ChallengeData challenge;

    TextView name, activity, date;
    TextView distance, duration, endTime;
    TextView feature, address, description;
    ProgressBar progressBar;

    long challengeId;
//    boolean upComing;
//    String title;
    boolean isUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constants.USE_LOCAL_DB) {
            // open the database of the application context
            dbChallenges = new ChallengesSQLiteHelper(this);
        }
        dbWorkoutResults = new WorkoutResultsSQLiteHelper(this);

        setContentView(R.layout.activity_details_challenge);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                challengeId = extras.getLong(Constants.ID_CHALLENGE);
            }
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Challenge");
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (Constants.USE_LOCAL_DB) {
            challenge = dbChallenges.readChallenge(challengeId);
            refreshChallenge(challenge);
        } else {
            getChallengeInfoFromServer(String.valueOf(challengeId));
        }
    }

    @SuppressLint("SetTextI18n")
    private void refreshChallenge(ChallengeData data) {
        String[] activityItems = getResources().getStringArray(R.array.activity_entries);

        name = findViewById(R.id.name);
        name.setText(data.getName());

        int activityType = data.getActivityType();
        activity = findViewById(R.id.type);
        activity.setText(activityItems[activityType]);

        String fromDate = data.getStartTime();
        String toDate = data.getEndTime();

        ArrayList<WorkoutResultData> arrayList = null;
        arrayList = dbWorkoutResults.getAllDataByDate(activityType, fromDate, toDate);

        WorkoutResultData wrd;
        float totalDistance = 0.0f;
        for (int i = 0; i < arrayList.size(); i++) {
            wrd = arrayList.get(i);
            totalDistance += wrd.getDistance();
        }

        progressBar = findViewById(R.id.horizontal_progress_bar);
        int challengeDistance = data.getDistance();
        int percent = (int) (totalDistance * 100 / challengeDistance); // * 100;
        progressBar.setProgress(percent);

        TextView workoutDistance = findViewById(R.id.workout_distance);
        workoutDistance.setText(String.format(Locale.getDefault(), "%.2f",totalDistance)
                                + " (" + percent + "%)");

        TextView settingDistance = findViewById(R.id.challenge_distance);
        settingDistance.setText(challengeDistance + "km");

        distance = findViewById(R.id.distance);
        distance.setText(data.getDistance() + "km");
        date = findViewById(R.id.date);
        duration = findViewById(R.id.duration);
        duration.setText(TimeUtil.getConvertToMonthFromDate(fromDate)
                        + " - "
                        + TimeUtil.getConvertToMonthFromDate(toDate));

        description = findViewById(R.id.description);
        description.setText(data.getDescription());
    }

    private void getChallengeInfoFromServer(String cid) {
        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("challenges").document(cid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                final String uid = FirebaseAuth.getInstance().getUid();
                ChallengeData cd = documentSnapshot.toObject(ChallengeData.class);
                refreshChallenge(cd);
            }
        });
    }

    // This method is called when the next activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the activity with an OK result
        if (requestCode == ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (Constants.USE_LOCAL_DB) {
                    challenge = (ChallengeData) dbChallenges.readChallenge(challengeId);
                    refreshChallenge(challenge);
                } else {
                    getChallengeInfoFromServer(String.valueOf(challengeId));
                }
                isUpdated = true;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isUpdated) {
                    intent = new Intent();
                    setResult(Activity.RESULT_OK, intent);
                }
                finish();
                return true;
            case R.id.action_edit:
                intent = new Intent(getBaseContext(), CreateChallengeActivity.class);
                intent.putExtra(Constants.ID_CHALLENGE, challengeId);
                startActivityForResult(intent, Constants.REQUEST_CODE_ACTIVITY);
                return true;
            case R.id.action_remove:
                if (Constants.USE_LOCAL_DB) {
                    dbChallenges.deleteChallenge(challenge);
                } else {
                    final FirebaseFirestore db = FirebaseFirestore.getInstance();
                    final String cid = String.valueOf(challengeId);
                    db.collection("challenges").document(cid)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "DocumentSnapshot successfully deleted!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error deleting document", e);
                            }
                        });
                }
                intent = new Intent();
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_edit, menu);
        return true;
    }
}
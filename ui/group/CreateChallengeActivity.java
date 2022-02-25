package com.travity.ui.group;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.travity.R;
import com.travity.data.ChallengeData;
import com.travity.data.Constants;
import com.travity.data.db.ChallengesSQLiteHelper;
import com.travity.ui.workout.TimeUtil;

import com.travity.util.textdrawable.TextDrawable;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import static com.travity.ui.workout.TimeUtil.getConvertToDateFromMonth;

public class CreateChallengeActivity extends AppCompatActivity {
    private static final String TAG = "CreateChallenge";

    ChallengesSQLiteHelper dbChallenges;

    TextInputEditText name, note;
    TextView activity, distance;
    TextView startDate, endDate;

    SimpleDateFormat simpleDateFormat;
    ChallengeData data;
    int positionActivity, totalDistance;
    long challengeId = 0;

    String fromDate, toDate;
    String[] activityItems;
    private Uri challengePhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_challenge);

        this.simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        // open the database of the application context
        dbChallenges = new ChallengesSQLiteHelper(this);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                challengeId = extras.getLong(Constants.ID_CHALLENGE);
            }
        }

        // load information of the challenge
        loadWidget();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (challengeId > Constants.ID_NONE) {
            // for updating the challenge
            actionBar.setTitle(getResources().getString(R.string.challenge_update));
            if (Constants.USE_LOCAL_DB) {
                updateWidget(challengeId);
            } else {
                getChallengeInfoFromServer(String.valueOf(challengeId));
            }
        } else {
            actionBar.setTitle(getResources().getString(R.string.challenge_creation));
            Calendar cal = Calendar.getInstance();
            Date todayCalendar = cal.getTime();
            String nowDate = (String) DateFormat.format("MMM dd, yyyy", todayCalendar);
            fromDate = TimeUtil.getConvertToDateFromMonth(nowDate);
            startDate.setText(nowDate);
        }
    }

    private void updateWidget(long cid) {
        data = dbChallenges.readChallenge(cid);
        name.setText(data.getName());

        activity.setText(activityItems[data.getActivityType()]);

        totalDistance = data.getDistance();
        distance.setText(String.valueOf(totalDistance) + "km");

        String datetime = data.getStartTime().toString();
        fromDate = datetime;
        startDate.setText(TimeUtil.getConvertToMonthFromDate(datetime));

        datetime = data.getEndTime().toString();
        toDate = datetime;
        if (datetime.startsWith("+")) {
            endDate.setText(R.string.click_here);
        } else {
            endDate.setText(TimeUtil.getConvertToMonthFromDate(datetime));
        }

        note.setText(data.getDescription());
    }

    private void loadWidget() {
        activityItems = getResources().getStringArray(R.array.activity_entries);

        name = findViewById(R.id.name);

        activity = findViewById(R.id.activity);
        activity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseActivity();
            }
        });

        distance = findViewById(R.id.distance);
        distance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterDistanceDialog();
            }
        });

        startDate = findViewById(R.id.start_date);
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker(true, getResources().getString(R.string.start_date));
            }
        });

        endDate = findViewById(R.id.end_date);
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker(false, getResources().getString(R.string.end_date));
            }
        });
        note = findViewById(R.id.note);
    }

    private void chooseActivity() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.choose_activity));

        builder.setSingleChoiceItems(activityItems, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which){
                        positionActivity = which;
                        activity.setText(activityItems[which]);
                    }
                });

        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                if (which == -1) {
                    positionActivity = 0;
                    activity.setText(activityItems[0]);
                }
                dialog.dismiss();
            }
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void datePicker(boolean start, String title) {
        MaterialDatePicker.Builder materialDateBuilder = MaterialDatePicker.Builder.datePicker();
        materialDateBuilder.setTitleText(title);

        final MaterialDatePicker materialDatePicker = materialDateBuilder.build();
        materialDatePicker.addOnPositiveButtonClickListener(
                new MaterialPickerOnPositiveButtonClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onPositiveButtonClick(Object selection) {
                String date = materialDatePicker.getHeaderText();
                String dateString = TimeUtil.getConvertToDateFromMonth(date);

                if (start) {
                    startDate.setText(date);
                    fromDate = dateString;
                } else {
                    endDate.setText(date);
                    toDate = dateString;
                }
            }
        });
        materialDatePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
    }

    private void enterDistanceDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.dialogbox_editbox, null);
        MaterialAlertDialogBuilder alertDialog = new MaterialAlertDialogBuilder(this);
        alertDialog.setTitle("Enter a distance");
        //alertDialog.setMessage("Put a distance");
//        alertDialog.setMessage(R.string.warning_wrong_time);

        final TextInputEditText dialogboxDistance = (TextInputEditText) view.findViewById(R.id.dialogbox_distance);

        alertDialog.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                totalDistance = Integer.parseInt(dialogboxDistance.getText().toString());
                distance.setText(dialogboxDistance.getText().toString() + "km");
            }
        });

        alertDialog.setView(view);
        alertDialog.show();
    }

    private void showAlertDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_alert)
                .setMessage(R.string.warning_wrong_time)
                .setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    void getChallengeInfoFromServer(String cid) {
//        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        String gid = String.valueOf(groupId);

        DocumentReference docRef = FirebaseFirestore.getInstance().collection("challenges").document(cid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                data = documentSnapshot.toObject(ChallengeData.class);
                name.setText(data.getName());
                activity.setText(activityItems[data.getActivityType()]);
                totalDistance = data.getDistance();
                distance.setText(String.valueOf(totalDistance) + "km");

                String datetime = data.getStartTime().toString();
                fromDate = datetime;
                startDate.setText(TimeUtil.getConvertToMonthFromDate(datetime));

                datetime = data.getEndTime().toString();
                toDate = datetime;
                if (datetime.startsWith("+")) {
                    endDate.setText(R.string.click_here);
                } else {
                    endDate.setText(TimeUtil.getConvertToMonthFromDate(datetime));
                }

                note.setText(data.getDescription());

/*
                if (data.getImage() != null && !"".equals(data.getImage())) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();

                    // Create a storage reference from our app
                    StorageReference storageRef = storage.getReference();

                    Glide.with(getBaseContext())
                            .load(storageRef.child("challengePhoto/" + data.getImage()))
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(image);
                }
 */
            }
        });
    }

    private void setChallengeInfo(boolean isNew, ChallengeData data) {
        final String uid = FirebaseAuth.getInstance().getUid();
        final String cid = String.valueOf(data.getId());
/*
        if (challengePhotoUri != null) {
            data.setImage(cid);
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference riversRef = storageRef.child("challengePhoto/" + totalDistance);
        UploadTask uploadTask = riversRef.putFile(challengePhotoUri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                if (Constants.USE_DEBUG) {
                    Log.v(TAG, "fail to upload a photo of the challenge.");
                }
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (Constants.USE_DEBUG) {
                    Log.v(TAG, "success to upload a photo of the challenge.");
                }
            }
        });
*/
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("challenges").document(cid)
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent intent = new Intent(getApplicationContext(), CreateChallengeActivity.class);
                        setResult(Activity.RESULT_OK, intent);

                        if (Constants.USE_DEBUG) {
                            Log.v(TAG, "success to make a new challenge.");
                        }
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = false;

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.done:
                ret = processEvent();
                if (ret) {
                    Intent intent = new Intent(getApplicationContext(), CreateChallengeActivity.class);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_done, menu);
        return true;
    }

    private boolean processEvent() {
        if (name.getText().toString().matches("")) {
            name.setError(getResources().getString(R.string.require));
            return false;
        }

        if (activity.getText().toString().equals("+")) {
            String[] activityItems = getResources().getStringArray(R.array.activity_entries);
            activity.setText(activityItems[0]);
        }

        if (!endDate.getText().toString().equals("+")) {
            if (TimeUtil.CompareDate2(fromDate, toDate) > 0) {
                showAlertDialog();
                return false;
            }
        }

        if (challengeId > Constants.ID_NONE) {
            updateEvent(challengeId);
        } else {
            createEvent();
        }
        return true;
    }

    private String getImagePath(int distance) {
        return createIconDistance(totalDistance);
/*
        if (Constants.USE_LOCAL_DB) {
            return createIconDistance(totalDistance);
        }
        return String.valueOf(distance);
 */
    }

    private void updateEvent(long id) {
        boolean isUpdated = false;

        if (isUpdated
            || !data.getName().equals(name.getText().toString())
            || data.getActivityType() != positionActivity
            || !data.getStartTime().equals(fromDate)
            || !data.getEndTime().equals(toDate)
            || !data.getDescription().equals(note.getText().toString())) {
            isUpdated = true;
        }

        if (isUpdated) {
            ChallengeData challenge = new ChallengeData(id,
                                        getImagePath(totalDistance),
                                        0,
                                        name.getText().toString(),
                                        positionActivity,
                                        totalDistance,
                                        fromDate,
                                        toDate,
                                        note.getText().toString());
            if (Constants.USE_LOCAL_DB) {
                dbChallenges.updateChallenge(challenge);
            } else {
                challenge.setUid(data.getUid());
//                challengePhotoUri = Uri.fromFile(new File(createIconDistance(totalDistance)));
                setChallengeInfo(false, challenge);
            }
        }
    }

    private void createEvent() {
        ChallengeData challenge = new ChallengeData(0,
                                        getImagePath(totalDistance),
                                        0,
                                        name.getText().toString(),
                                        positionActivity,
                                        totalDistance,
                                        fromDate,
                                        toDate,
                                        note.getText().toString());
        if (Constants.USE_LOCAL_DB) {
            dbChallenges.createChallenge(challenge);
        } else {
            final String uid = FirebaseAuth.getInstance().getUid();
            Random rand = new Random();
            int int_random = rand.nextInt(100);

            SimpleDateFormat idFormat = new SimpleDateFormat(Constants.DATETIME_FORMAT_FILENAME_SECOND,
                                                            Locale.getDefault());
            String cid = idFormat.format(new Date()) + String.valueOf(int_random);
            challenge.setId(Long.parseLong(cid));
            challenge.setUid(uid);
//            challengePhotoUri = Uri.fromFile(new File(createIconDistance(totalDistance)));
            setChallengeInfo(true, challenge);
        }
    }

    public String createIconDistance(int distance) {
        String filename = null;
        String iconString;
        int fontSize = 50;

        if (distance >= 100) {
            iconString = "99K+";
            fontSize = 40;
        } else {
            iconString = distance + "K";
        }
        filename = TextDrawable.createInitialIconOnFile(null, iconString, fontSize,
                                        Constants.ICON_INITIAL_WIDTH,
                                        Constants.ICON_INITIAL_HEIGHT);
        return filename;
    }
/*
        String iconFilename = distance + "K.png";

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), iconFilename);
        if (file.exists()) {
            return iconFilename;
        }

        // declare the color generator and drawable builder
        ColorGenerator mColorGenerator = ColorGenerator.MATERIAL;
        TextDrawable.IShapeBuilder mDrawableBuilder;

        mDrawableBuilder = TextDrawable.builder();
        mDrawableBuilder.beginConfig().bold();
        mDrawableBuilder.beginConfig().textColor(0xff000000);
        mDrawableBuilder.round();

        String iconString;
        if (distance > 100) {
            iconString = "99K+";
            mDrawableBuilder.beginConfig().fontSize(40);
        } else {
            iconString = distance + "K";
            mDrawableBuilder.beginConfig().fontSize(50);
        }

        int width = Constants.ICON_INITIAL_WIDTH;
        int height = Constants.ICON_INITIAL_HEIGHT;
        TextDrawable drawable = mDrawableBuilder.buildRound(iconString, mColorGenerator.getRandomColor());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            bitmap.Compress(Bitmap.CompressFormat.PNG,100, fout);
        } catch (Exception e){
            e.printStackTrace ();
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        return iconFilename;
    }
 */
}
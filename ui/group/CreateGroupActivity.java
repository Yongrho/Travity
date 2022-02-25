package com.travity.ui.group;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.travity.MainActivity;
import com.travity.R;
import gujc.directtalk9.model.UserModel;

import com.travity.data.ActivityType;
import com.travity.data.Constants;
import com.travity.data.GroupData;
import com.travity.data.GroupMemberData;
import com.travity.data.db.GroupsSQLiteHelper;
import com.travity.ui.workout.TimeUtil;
import com.travity.util.textdrawable.TextDrawable;
//import Com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker;
//import Com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

public class CreateGroupActivity extends AppCompatActivity {
    private static final String TAG = "CreateGroupActivity";
    private static final int ALBUM_REQUEST_CODE = 100;
    private static final int ACTIVITY_REQUEST_CODE = 1;

    GroupsSQLiteHelper dbGroups;
    GroupData data;
    ImageView image;

    TextView activity, creationDate;
    TextInputEditText name, description;
    TableRow trImage;

    String imageFilePath = null, host;
//    String idString;
    int positionActivity = 0;
    long groupId;
    String[] activityItems;
    private Uri groupPhotoUri;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // open the database of the application context
        dbGroups = new GroupsSQLiteHelper(this);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                groupId = extras.getLong(Constants.ID_GROUP);
            }
        }

        activityItems = getResources().getStringArray(R.array.activity_entries);
        sharedPreferences = getSharedPreferences("travity", Activity.MODE_PRIVATE);
        host = sharedPreferences.getString("user_name", "");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.group_creation);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        loadWidget();

        if (groupId > Constants.ID_NONE) {
            if (actionBar != null) {
                actionBar.setTitle(R.string.group_update);
            }
            if (Constants.USE_LOCAL_DB) {
                updateWidget(groupId);
            } else {
                getGroupInfoFromServer();
            }
        } else {
            activity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chooseActivity();
                }
            });
        }
    }

    private void updateWidget(long id) {
        data = dbGroups.readGroup(id);

        String imageFilePath = data.getImage();
        if (imageFilePath != null) {
            Uri uri = Uri.fromFile(new File(imageFilePath));
            image.setImageURI(uri);
        }
        name.setText(data.getName());
        creationDate.setText(data.getCreationDate());

        activity.setText(activityItems[data.getActivity()]);
        description.setText(data.getDescription());
    }

    private void loadWidget() {
        image = findViewById(R.id.image);
        trImage = findViewById(R.id.trImage);
        trImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePhoto();
            }
        });

        name = findViewById(R.id.name);

        creationDate = findViewById(R.id.creation_date);
        creationDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker();
            }
        });

        activity = findViewById(R.id.activity);
        description = findViewById(R.id.description);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.group_creation);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void chooseActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.choose_activity));

        builder.setSingleChoiceItems(activityItems, 0,
                                    new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which){
                positionActivity = which;
            }
        });
        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                if (which == -1) {
                    activity.setText(activityItems[positionActivity]);
                }
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void datePicker() {
        MaterialDatePicker.Builder materialDateBuilder = MaterialDatePicker.Builder.datePicker();
        materialDateBuilder.setTitleText(R.string.creation_date);

        final MaterialDatePicker materialDatePicker = materialDateBuilder.build();
        materialDatePicker.addOnPositiveButtonClickListener(
                new MaterialPickerOnPositiveButtonClickListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onPositiveButtonClick(Object selection) {
                        creationDate.setText(materialDatePicker.getHeaderText());
                    }
                });
        materialDatePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
    }

    private void choosePhoto() {
        Intent intent = new Intent();
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, ALBUM_REQUEST_CODE);
    }

    private String getPath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();
        return path;
    }

    void getGroupInfoFromServer(){
        String gid = String.valueOf(groupId);

        DocumentReference docRef = FirebaseFirestore.getInstance().collection("groups").document(gid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                data = documentSnapshot.toObject(GroupData.class);
                name.setText(data.getName());
                creationDate.setText(data.getCreationDate());
                activity.setText(activityItems[data.getActivity()]);
                description.setText(data.getDescription());

                if (data.getImage() != null && !"".equals(data.getImage())) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();

                    // Create a storage reference from our app
                    StorageReference storageRef = storage.getReference();

                    Glide.with(getBaseContext())
                            .load(storageRef.child("groupPhoto/" + data.getImage()))
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(image);
                }
            }
        });
    }

    private void setGroupInfo(boolean isNew, GroupData data) {
        final String uid = FirebaseAuth.getInstance().getUid();
        final String gid = String.valueOf(data.getId());

        if (groupPhotoUri != null) {
            data.setImage(gid);
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference riversRef = storageRef.child("groupPhoto/" + gid);
        UploadTask uploadTask = riversRef.putFile(groupPhotoUri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                if (Constants.USE_DEBUG) {
                    Log.v(TAG, "fail to upload a photo of the group.");
                }
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (Constants.USE_DEBUG) {
                    Log.v(TAG, "success to upload a photo of the group.");
                }
            }
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (isNew) {
            GroupMemberData gmd = new GroupMemberData(uid, gid);
            db.collection("groups_members").document(gid)
                    .set(gmd)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (Constants.USE_DEBUG) {
                                Log.v(TAG, "success to make a new member of group.");
                            }
                        }
                    });
        }

        db.collection("groups").document(gid)
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent intent = new Intent(getApplicationContext(), CreateGroupActivity.class);
                        setResult(Activity.RESULT_OK, intent);

                        if (Constants.USE_DEBUG) {
                            Log.v(TAG, "success to make a new group.");
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ALBUM_REQUEST_CODE) {
                try {
                    groupPhotoUri = data.getData();
                    imageFilePath = getPath(groupPhotoUri);
                    image.setImageURI(groupPhotoUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        boolean ret = false;

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.done:
                String groupName = name.getText().toString();
                if (groupName.matches("")) {
                    name.setError(getResources().getString(R.string.require));
                    return true;
                }

                ret = processGroup(groupName);
                if (ret) {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean updateGroup(String name) {
        boolean isUpdated = false;

        if (isUpdated
                || !data.getName().equals(name)
                || !data.getCreationDate().equals(creationDate.getText().toString())
                || !data.getDescription().equals(description.getText().toString())) {
            isUpdated = true;
        }

        if (isUpdated) {
            GroupData group = new GroupData(groupId,
                                    createIconGroupName(name),
                                    name,
                                    host,
                                    creationDate.getText().toString(),
                                    data.getActivity(),
                                    description.getText().toString());
            if (Constants.USE_LOCAL_DB) {
                dbGroups.updateGroup(group);

                Intent intent = new Intent(getApplicationContext(), CreateGroupActivity.class);
                intent.putExtra(Constants.ID_GROUP, groupId);
                intent.putExtra(Constants.FLAG_TITLE, name);
                setResult(Activity.RESULT_OK, intent);
            } else {
                groupPhotoUri = Uri.fromFile(new File(createIconGroupName(name)));
                setGroupInfo(false, group);
            }
        }
        return isUpdated;
    }

    private boolean createGroup(String name) {
        // save the new group
        GroupData group = new GroupData(0,
                                        createIconGroupName(name),
                                        name,
                                        host,
                                        creationDate.getText().toString(),
                                        positionActivity,
                                        description.getText().toString());

        if (Constants.USE_LOCAL_DB) {
            dbGroups.createGroup(group);
            Intent intent = new Intent(getApplicationContext(), CreateGroupActivity.class);
            setResult(Activity.RESULT_OK, intent);
        } else {
            Random rand = new Random();
            int int_random = rand.nextInt(100);

            SimpleDateFormat idFormat = new SimpleDateFormat(Constants.DATETIME_FORMAT_FILENAME_SECOND,
                                                                            Locale.getDefault());
            String idString = idFormat.format(new Date()) + String.valueOf(int_random);
            group.setId(Long.parseLong(idString));
            groupPhotoUri = Uri.fromFile(new File(createIconGroupName(name)));
            setGroupInfo(true, group);
        }
        return true;
    }

    private boolean processGroup(String name) {
        boolean ret = false;

        if (groupId > Constants.ID_NONE) {
            ret = updateGroup(name);
        } else {
            ret = createGroup(name);
        }
        return ret;
    }


    private String createIconGroupName(String name) {
        String filepath = imageFilePath;
        if (filepath != null) {
            return imageFilePath;
        }

        String initial = null;
        String[] tmpName = name.split(" ");
        if (tmpName.length > 1) {
            initial = tmpName[0].toUpperCase().substring(0, 1)
                        + tmpName[tmpName.length - 1].toUpperCase().substring(0, 1);
        } else {
            initial = name.substring(0, 2);
        }

        String iconFilename = "/group_" + initial.toLowerCase() + ".png";
        filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + iconFilename;
        File file = new File(filepath);
        if (!file.exists()) {
            TextDrawable.createInitialIconOnFile(iconFilename, initial, 50 * 2,
                                            Constants.ICON_INITIAL_WIDTH * 2,
                                            Constants.ICON_INITIAL_HEIGHT * 2);
        }
        return filepath;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_done, menu);
        return true;
    }
}
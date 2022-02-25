package com.travity.ui.group;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;

import com.travity.R;
import com.travity.data.Constants;
import com.travity.data.Member;
import com.travity.data.db.MembersSQLiteHelper;
import com.travity.data.db.GroupMembersSQLiteHelper;
import com.travity.util.textdrawable.TextDrawable;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class CreateMemberActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";
    private static final int ALBUM_REQUEST_CODE = 100;
    private static final String GROUP_ID = "group_id";

    MembersSQLiteHelper dbMembers;
    GroupMembersSQLiteHelper dbGMs;

    ImageView image;
    TextView birthday;
    TextInputEditText name, email, phone, address;
    TableRow trImage;

    String imageFilePath = null;
    long groupID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_member);

        // open the database of the application context
        dbMembers = new MembersSQLiteHelper(this);
        dbGMs = new GroupMembersSQLiteHelper(this);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                groupID = extras.getLong(GROUP_ID);
            }
        }

        name = findViewById(R.id.name);
        birthday = findViewById(R.id.birthday);
        birthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker();
            }
        });

        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        address = findViewById(R.id.address);

        image = findViewById(R.id.image);
        trImage = findViewById(R.id.trImage);
        trImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePhoto();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.member_creation));
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    public void datePicker() {
        MaterialDatePicker.Builder materialDateBuilder = MaterialDatePicker.Builder.datePicker();
        materialDateBuilder.setTitleText(R.string.birthday);

        final MaterialDatePicker materialDatePicker = materialDateBuilder.build();
        materialDatePicker.addOnPositiveButtonClickListener(
                new MaterialPickerOnPositiveButtonClickListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onPositiveButtonClick(Object selection) {
                        birthday.setText(materialDatePicker.getHeaderText());
                    }
                });
        materialDatePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
    }

    public void choosePhoto() {
        Intent intent = new Intent();
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, ALBUM_REQUEST_CODE);
    }

    public String getPath(Uri uri) {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ALBUM_REQUEST_CODE) {
                try {
                    Uri uri = data.getData();
                    imageFilePath = getPath(uri);
                    image.setImageURI(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.done:
                // save the new group
                String nameString = name.getText().toString();
                Member member = new Member(0,
                                            createIconMemberName(nameString),
                                            nameString,
                                            birthday.getText().toString(),
                                            email.getText().toString(),
                                            phone.getText().toString(),
                                            address.getText().toString(),
                                            null);
                dbMembers.createMember(member);

                long memberId = dbMembers.getId(member);
                dbGMs.createGroupMember(groupID, memberId);

                Intent intent = new Intent(getApplicationContext(), GroupsMembersFragment.class);
                setResult(Activity.RESULT_OK, intent);
                finish();
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

    public String createIconMemberName(String name) {
        String filepath = imageFilePath;
        if (filepath != null) {
            return imageFilePath;
        }

        String initial = null;

        if (name == null) {
            String emailAddress = email.getText().toString();
            String[] tmpName = emailAddress.split("\\.");
            if (tmpName.length > 1) {
                initial = String.valueOf(tmpName[0].toUpperCase().charAt(0))
                        + String.valueOf(tmpName[1].toUpperCase().charAt(0));
            } else {
                initial = emailAddress.substring(0, 2);
            }
        } else {
            String[] tmpName = name.split(" ");
            if (tmpName.length > 1) {
                initial = String.valueOf(tmpName[0].toUpperCase().charAt(0))
                        + String.valueOf(tmpName[tmpName.length - 1].toUpperCase().charAt(0));
            } else {
                initial = name.substring(0, 2);
            }
        }

        String iconFilename = "/member_" + initial.toLowerCase() + ".png";
        filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + iconFilename;
        File file = new File(filepath);
        if (!file.exists()) {
            TextDrawable.createInitialIconOnFile(iconFilename, initial, 50 * 2,
                                            Constants.ICON_INITIAL_WIDTH * 2,
                                            Constants.ICON_INITIAL_HEIGHT * 2);
        }
        return filepath;
    }
}
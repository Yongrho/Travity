package com.travity.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;

import com.travity.R;
import gujc.directtalk9.model.UserModel;

import com.bumptech.glide.Glide;
import com.travity.data.Constants;
import com.travity.data.Member;
import com.travity.data.db.MembersSQLiteHelper;
import com.travity.data.db.GroupMembersSQLiteHelper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DetailsMemberActivity extends AppCompatActivity {
    private static final String TAG = "DetailsMemberActivity";
    private static final String MEMBER_ID = "member_id";
    private static final int ACTIVITY_REQUEST_CODE = 1;

    MembersSQLiteHelper dbMembers;
    TextView name, birthday, email, phone, address;
    ImageView image;
    long memberID;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_member);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = null;
        if (savedInstanceState == null) {
            extras = getIntent().getExtras();
            if (extras == null) {
                finish();
            }
        }

        if (Constants.USE_LOCAL_DB) {
            // open the database of the application context
            dbMembers = new MembersSQLiteHelper(this);
            memberID = extras.getLong(MEMBER_ID);

            Member member = dbMembers.readMember(memberID);
            name = findViewById(R.id.name);
            name.setText(member.getName());
            birthday  = findViewById(R.id.birthday);
            birthday.setText(member.getBirthday());
            email = findViewById(R.id.email);
            email.setText(member.getEmail());
            phone = findViewById(R.id.phone);
            phone.setText(member.getPhone());
            address = findViewById(R.id.address);
            address.setText(member.getAddress());
        } else {
            getMembersInfoFromServer(extras.getString(MEMBER_ID));
        }
    }

    private void getMembersInfoFromServer(String uid) {
        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                final String uid = FirebaseAuth.getInstance().getUid();

                UserModel um = documentSnapshot.toObject(UserModel.class);
                if (uid.equals(um.getUid())) {
                    View space = findViewById(R.id.space);
                    space.setVisibility(View.GONE);
                    ImageView message = findViewById(R.id.message);
                    message.setVisibility(View.GONE);
                    ImageView email_image = findViewById(R.id.email_image);
                    email_image.setVisibility(View.GONE);
                    ImageView phone_image = findViewById(R.id.phone_image);
                    phone_image.setVisibility(View.GONE);
                }
                name = findViewById(R.id.name);
                name.setText(um.getUsernm());
                birthday  = findViewById(R.id.birthday);
                birthday.setText(um.getBirthday());
                email = findViewById(R.id.email);
                email.setText(um.getUserid());
                phone = findViewById(R.id.phone);
                phone.setText(um.getPhone());
                address = findViewById(R.id.address);
                address.setText(um.getAddress());
                image = findViewById(R.id.photo);

                FirebaseStorage storage = FirebaseStorage.getInstance();
                // Create a storage reference from our app
                StorageReference storageRef = storage.getReference();
                Glide.with(getBaseContext())
                        .load(storageRef.child("userPhoto/" + um.getUserphoto()))
                        .into(image);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.profile:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.options_menu_profile, menu);
        return true;
    }
}
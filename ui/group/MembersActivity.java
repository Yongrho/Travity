package com.travity.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity ;

import com.travity.R;
import com.travity.data.GroupData;
import com.travity.data.db.GroupMembersSQLiteHelper;
import com.travity.data.db.GroupsSQLiteHelper;
import com.travity.data.Member;
import com.travity.data.MembersAdapter;
import com.travity.data.db.MembersSQLiteHelper;

import java.util.ArrayList;
import java.util.List;

public class MembersActivity extends AppCompatActivity {
    private static final String TAG = "MembersActivity";
    private static final String GROUP_ID = "group_id";
    private static final int ACTIVITY_REQUEST_CODE = 1;

    GroupMembersSQLiteHelper dbGMs;
    MembersSQLiteHelper dbMembers;
    GroupsSQLiteHelper dbGroups;
    ArrayList<Member> members = new ArrayList<Member>();
    MembersAdapter membersAdapter;
    ListView lvMembers;
    long groupID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_members);

        // open the database of the application context
        dbGMs = new GroupMembersSQLiteHelper(this);
        dbMembers = new MembersSQLiteHelper(this);
        dbGroups = new GroupsSQLiteHelper(this);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                groupID = extras.getInt(GROUP_ID);
            }
        }

        membersAdapter = new MembersAdapter(getBaseContext(), members);
        lvMembers = (ListView) findViewById(R.id.listview_members);
        lvMembers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                // update member's information
                Member entry= (Member) parent.getAdapter().getItem(position);
//                Intent intent = new Intent(getBaseContext(), CreateMemberActivity.class);
//                intent.putExtra(GROUP_ID, entry.getId());
//                startActivity(intent);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            GroupData group = dbGroups.readGroup(groupID);
            actionBar.setTitle(group.getName());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        members.clear();

        List<Long> listGMs = dbGMs.getAllMembers(groupID);
        for (int i = 0; i < listGMs.size(); i++) {
            members.add(dbMembers.readMember(listGMs.get(i)));
        }

        if (members.size() >= 1) {
            membersAdapter.setMembers(members);
            lvMembers.setAdapter(membersAdapter);
        }
    }

    /*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // check if the request code is same as what is passed  here it is ACTIVITY_REQUEST_CODE
        if (requestCode == ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            Log.v("test", "onActivityResult.getString: " + data.getStringExtra("activity"));
//            tvActivityName.setText(data.getStringExtra("activity"));
        }
    }
*/
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.profile:
                // save a new member
                Intent intent = new Intent(getBaseContext(), CreateMemberActivity.class);
                intent.putExtra(GROUP_ID, groupID);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu_profile, menu);
        return true;
    }
}
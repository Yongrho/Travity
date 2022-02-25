package com.travity.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.widget.AdapterView;
import android.widget.ListView;

import com.travity.R;
import gujc.directtalk9.model.UserModel;

import com.travity.data.Constants;
import com.travity.data.GroupData;
import com.travity.data.UsersAdapter;
import com.travity.data.db.GroupMembersSQLiteHelper;
import com.travity.data.db.GroupsSQLiteHelper;
import com.travity.data.Member;
import com.travity.data.MembersAdapter;
import com.travity.data.db.MembersSQLiteHelper;
import com.travity.ui.home.CreateRaceActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.List;

public class GroupsMembersFragment extends Fragment {
    private static final String TAG = "GroupsMembersFragment";
    private static final int ACTIVITY_REQUEST_CODE = 1;
    private static final String GROUP_ID = "group_id";
    private static final String MEMBER_ID = "member_id";

    GroupMembersSQLiteHelper dbGMs;
    MembersSQLiteHelper dbMembers;
    GroupsSQLiteHelper dbGroups;
    ArrayList<Member> members = new ArrayList<Member>();
    MembersAdapter membersAdapter;
    ListView lvMembers;
    long groupID;
    private GroupsSharedViewModel viewModel;

    ArrayList<UserModel> users = new ArrayList<UserModel>();
    UsersAdapter usersAdapter;
    int countDocuments = 0;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_listview, container, false);

        if (Constants.USE_LOCAL_DB) {
            // open the database of the application context
            dbGMs = new GroupMembersSQLiteHelper(getContext());
            dbMembers = new MembersSQLiteHelper(getContext());
            dbGroups = new GroupsSQLiteHelper(getContext());
            membersAdapter = new MembersAdapter(getContext(), members);
        } else {
            usersAdapter = new UsersAdapter(getContext(), users);
        }

        lvMembers = (ListView) root.findViewById(R.id.listview);
        lvMembers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Intent intent = new Intent(getContext(), DetailsMemberActivity.class);

                // update member's information
                if (Constants.USE_LOCAL_DB) {
                    Member entry = (Member) parent.getAdapter().getItem(position);
                    intent.putExtra(MEMBER_ID, entry.getId());
                } else {
                    UserModel entry = (UserModel) parent.getAdapter().getItem(position);
                    intent.putExtra(MEMBER_ID, entry.getUid());
                }
                startActivity(intent);
            }
        });

        // initialise ViewModel here
        viewModel = new ViewModelProvider(requireActivity()).get(GroupsSharedViewModel.class);
        viewModel.getGroupId().observe(getViewLifecycleOwner(), new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long id) {
                groupID = id;
                refreshMembers();
            }
        });

        viewModel.getGroupData().observe(getViewLifecycleOwner(), new Observer<GroupData>() {
            @Override
            public void onChanged(@Nullable GroupData data) {
                getMemberIdsFromServer(String.valueOf(data.getId()));
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save a new member
                Intent intent = new Intent(getContext(), CreateMemberActivity.class);
                intent.putExtra(GROUP_ID, groupID);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
            }
        });

        return root;
    }

    private void getMembersInfoFromServer(String uid) {
        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                UserModel um = documentSnapshot.toObject(UserModel.class);
                users.add(um);
                countDocuments--;
                if (countDocuments <= 0) {
                    refreshUsers();
                }
            }
        });
    }

    private void getMemberIdsFromServer(String gid) {
        users.clear();

        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("groups_members")
            .whereEqualTo("gid", gid)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.get("uid"));
                            getMembersInfoFromServer((String) document.get("uid"));
                            countDocuments++;
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            });
    }

    private void refreshUsers() {
        if (users.size() > 0) {
            usersAdapter.setMembers(users);
            lvMembers.setAdapter(usersAdapter);
        } else {
            lvMembers.setAdapter(null);
        }
    }

    private void refreshMembers() {
        members = dbMembers.getAllMembers();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is ACTIVITY_REQUEST_CODE
        if (requestCode == ACTIVITY_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            if (Constants.USE_LOCAL_DB) {
                refreshMembers();
            } else {
                getMemberIdsFromServer(String.valueOf(groupID));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                NavHostFragment.findNavController(GroupsMembersFragment.this)
                        .navigate(R.id.navigation_groups);
                return true;
/*
            case R.id.add:
                // save a new member
                Intent intent = new Intent(getContext(), CreateMemberActivity.class);
                intent.putExtra(GROUP_ID, groupID);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
                return true;
 */
        }
        return super.onOptionsItemSelected(item);
    }
/*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_menu_add, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
 */
}
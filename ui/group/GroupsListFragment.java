package com.travity.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.travity.R;
import gujc.directtalk9.model.UserModel;

import com.travity.data.Constants;
import com.travity.data.GroupData;
import com.travity.data.GroupMemberData;
import com.travity.data.GroupsAdapter;
import com.travity.data.db.GroupsSQLiteHelper;
import com.travity.util.textdrawable.TextDrawable;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class GroupsListFragment extends Fragment {
    private static final String TAG = "GroupsList";
    private static final int ACTIVITY_REQUEST_CODE = 1;

    private static final String GROUP_ID = "group_id";
    GroupsSQLiteHelper dbGroups;
    ArrayList<GroupData> groups;
    private GroupsSharedViewModel viewModel;
    GroupsAdapter groupsAdapter;
    private RecyclerView recyclerView;
    boolean needRefresh = false;
    int countDocuments = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        setHasOptionsMenu(true);
        if (Constants.USE_LOCAL_DB) {
            dbGroups = new GroupsSQLiteHelper(getActivity());
        }

        // initialise ViewModel here
        viewModel = new ViewModelProvider(requireActivity()).get(GroupsSharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_recyclerview, container, false);

        recyclerView = (RecyclerView) root.findViewById(R.id.sectioned_recycler_view);
        recyclerView.setHasFixedSize(true);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        groupsAdapter = new GroupsAdapter(getContext(), groups);
        groupsAdapter.setOnItemClickListener(new GroupsAdapter.ClickItemListener() {
            @Override
            public void onItemClick(View view, int position) {
                // list item was clicked
                GroupData entry = (GroupData) groupsAdapter.getItem(position);

                Bundle bundle = new Bundle();
                if (Constants.USE_LOCAL_DB) {
                    viewModel.setGroupId(entry.getId());
                    GroupData group = dbGroups.readGroup(entry.getId());
                    bundle.putString("title", group.getName());
                } else {
                    viewModel.setGroupData(entry);
                    bundle.putString("title", entry.getName());
                }
                final NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.navigation_groups_home, bundle);
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a new group
                Intent intent = new Intent(getActivity(), CreateGroupActivity.class);
                intent.putExtra(Constants.ID_GROUP, Constants.ID_NONE);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
            }
        });

        // get all groups
        if (Constants.USE_LOCAL_DB) {
            groups = dbGroups.getAllGroups();
            refreshGroups();
        } else {
            getGroupsFromServer();
        }

        setHasOptionsMenu(true);
        updateActionBar();
        return root;
    }

    private void updateActionBar() {
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            SharedPreferences sp = getActivity().getSharedPreferences("travity", Activity.MODE_PRIVATE);
            String id = sp.getString("user_initial", "T");
            if (!"".equals(id)) {
                actionBar.setHomeAsUpIndicator(TextDrawable.getIconInitialName(id));
            }
        }
    }

    private void getGroupsInfoFromServer(String gid) {
       // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("groups").document(gid);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                GroupData gd = documentSnapshot.toObject(GroupData.class);
                groups.add(gd);
                countDocuments--;
                if (countDocuments <= 0) {
                    refreshGroups();
                }
            }
        });
    }

    private void getGroupsFromServer() {
        final String uid = FirebaseAuth.getInstance().getUid();

        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("groups_members")
            .whereEqualTo("uid", uid)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        groups = new ArrayList<GroupData>();
                        countDocuments = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            Log.d(TAG, document.getId() + " => " + document.get("gid"));
                            getGroupsInfoFromServer(document.getId());
                            countDocuments++;
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            });
    }

    private void refreshGroups() {
        if (groups.size() >= 1) {
            groupsAdapter.setItems(groups);
        }
        recyclerView.setAdapter(groupsAdapter);
        needRefresh = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // Check that it is the activity with an OK result
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.REQUEST_CODE_PROFILE) {
                updateActionBar();
            } else if (requestCode == ACTIVITY_REQUEST_CODE) {
                if (Constants.USE_LOCAL_DB) {
                    groups = dbGroups.getAllGroups();
                    refreshGroups();
                } else {
                    getGroupsFromServer();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
            default:
                break;
        }
        return false;
    }

}


package com.travity.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import com.travity.R;
import gujc.directtalk9.model.UserModel;

import com.travity.data.ChallengeData;
import com.travity.data.ChallengesAdapter;
import com.travity.data.Constants;
import com.travity.data.GroupData;
import com.travity.data.db.ChallengesSQLiteHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class GroupsChallengeFragment extends Fragment {
    private static final String TAG = "GroupsChallengeFragment";

    ChallengesSQLiteHelper dbChallenges;
    ArrayList<ChallengeData> challenges = new ArrayList<ChallengeData>();
    ChallengesAdapter challengesAdapter;
    ListView lvEvents;
    long challengeID;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (Constants.USE_LOCAL_DB) {
            // open the database of the application context
            dbChallenges = new ChallengesSQLiteHelper(getContext());
        }

        View root = inflater.inflate(R.layout.fragment_listview, container, false);

        challengesAdapter = new ChallengesAdapter(getContext(), challenges);
        lvEvents = (ListView) root.findViewById(R.id.listview);
        lvEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // update event information
                ChallengeData entry= (ChallengeData) parent.getAdapter().getItem(position);
                Intent intent = new Intent(getContext(), DetailsChallengeActivity.class);
                intent.putExtra(Constants.ID_CHALLENGE, entry.getId());
                startActivity(intent);
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a new challenge
                Intent intent = new Intent(getActivity(), CreateChallengeActivity.class);
                startActivityForResult(intent, Constants.REQUEST_CODE_ACTIVITY);
            }
        });

        // get all groups
        if (Constants.USE_LOCAL_DB) {
            challenges = dbChallenges.getAllChallenges();
            refreshChallenges();
        } else {
            getChallengesFromServer();
        }

        return root;
    }

    private void getChallengesFromServer() {
        final String uid = FirebaseAuth.getInstance().getUid();

        // Get a reference to our posts
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("challenges")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            challenges = new ArrayList<ChallengeData>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ChallengeData cd = document.toObject(ChallengeData.class);
                                challenges.add(cd);
                            }
                            refreshChallenges();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void refreshChallenges() {
        if (challenges.size() > 0) {
            challengesAdapter.setChallenges(challenges);
            lvEvents.setAdapter(challengesAdapter);
        } else {
            lvEvents.setAdapter(null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // Check that it is the activity with an OK result
        if (resultCode == Activity.RESULT_OK) {
//            if (requestCode == Constants.REQUEST_CODE_ACTIVITY) {
                challenges.clear();
                if (Constants.USE_LOCAL_DB) {
                    challenges = dbChallenges.getAllChallenges();
                    refreshChallenges();
                } else {
                    getChallengesFromServer();
                }
//            }
        }
    }
}


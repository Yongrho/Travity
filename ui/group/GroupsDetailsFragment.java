package com.travity.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
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
import com.travity.data.Constants;
import com.travity.data.GroupData;
import com.travity.data.db.GroupsSQLiteHelper;
import com.travity.util.imageloader.ImageLoader;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.File;

public class GroupsDetailsFragment extends Fragment {
    private static final String TAG = "GroupsDetailsFragment";

    GroupsSharedViewModel viewModel;
    GroupsSQLiteHelper dbGroups;
    ImageView image;
    TextView name, date, activity, description;
    ImageLoader imageLoader;
    long groupId = 0;

    @Override
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_groups_details, container, false);

        setHasOptionsMenu(true);

        // open the database of the application context
        dbGroups = new GroupsSQLiteHelper(getContext());

        image = root.findViewById(R.id.image);
        name = root.findViewById(R.id.name);
        date = root.findViewById(R.id.date);
        activity = root.findViewById(R.id.activity);
        description = root.findViewById(R.id.description);


        // initialise ViewModel here
        viewModel = new ViewModelProvider(requireActivity()).get(GroupsSharedViewModel.class);
        viewModel.getGroupId().observe(getViewLifecycleOwner(), new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long id) {
                groupId = id;
                refreshGroupDetails(id);
            }
        });

        viewModel.getGroupData().observe(getViewLifecycleOwner(), new Observer<GroupData>() {
            @Override
            public void onChanged(@Nullable GroupData data) {
                refreshGroupDetails(data);
            }
        });
        return root;
    }

    private void refreshGroupDetails(GroupData group) {
        String[] activityItems = getResources().getStringArray(R.array.activity_entries);
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();
        Glide.with(getContext())
                .load(storageRef.child("groupPhoto/" + group.getImage()))
                .into(image);

        name.setText(group.getName());
        date.setText(group.getCreationDate());
        activity.setText(activityItems[group.getActivity()]);
        description.setText(group.getDescription());
    }

    private void refreshGroupDetails(long id) {
        String[] activityItems = getResources().getStringArray(R.array.activity_entries);
        GroupData group = dbGroups.readGroup(id);

        String imageFilePath = group.getImage();
        if (imageFilePath != null) {
            Uri uri = Uri.fromFile(new File(imageFilePath));
            image.setImageURI(uri);
        }

        name.setText(group.getName());
        date.setText(group.getCreationDate());
        activity.setText(activityItems[group.getActivity()]);
        description.setText(group.getDescription());
    }

    // This method is called when the next activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the activity with an OK result
        if (requestCode == Constants.REQUEST_CODE_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(data.getStringExtra(Constants.FLAG_TITLE));
                }
                refreshGroupDetails(data.getLongExtra(Constants.ID_GROUP, Constants.ID_NONE));
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_menu_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                NavHostFragment.findNavController(this).navigate(R.id.navigation_groups);
                return true;
            case R.id.action_edit:
                Intent intent = new Intent(getActivity(), CreateGroupActivity.class);
                intent.putExtra(Constants.ID_GROUP, groupId);
                startActivityForResult(intent, Constants.REQUEST_CODE_ACTIVITY);
                return true;
            case R.id.action_remove:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
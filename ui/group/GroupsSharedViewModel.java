package com.travity.ui.group;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.travity.data.GroupData;

import java.util.ArrayList;

public class GroupsSharedViewModel extends ViewModel {
    /**
     * Live Data Instance
     */
    private MutableLiveData<String> mName = new MutableLiveData<>();
    public void setName(String name) {
        mName.setValue(name);
    }
    public LiveData<String> getName() {
        return mName;
    }

    private MutableLiveData<Long> mGroupId = new MutableLiveData<>();
    public void setGroupId(Long id) {
        mGroupId.setValue(id);
    }
    public LiveData<Long> getGroupId() {
        return mGroupId;
    }

    private MutableLiveData<GroupData> mGroupData = new MutableLiveData<>();
    public void setGroupData(GroupData data) {
        mGroupData.setValue(data);
    }
    public LiveData<GroupData> getGroupData() {
        return mGroupData;
    }
}

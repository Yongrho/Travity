package com.travity.ui.workout;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class SharedViewModel extends ViewModel {
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
/*
    private MutableLiveData<IntervalData> mItem = new MutableLiveData<>();
    public void setItem(IntervalData data) {
       mItem.setValue(data);
    }
    public LiveData<IntervalData> getItem() {
        return mItem;
    }
*/
    private MutableLiveData<ArrayList<Location>> mLocationList = new MutableLiveData<>();
    public void setLocationList(ArrayList<Location> locationList) {
        mLocationList.setValue(locationList);
    }
    public LiveData<ArrayList<Location>> getLocationList() {
        return mLocationList;
    }

    private MutableLiveData<LocationService> mLocationService = new MutableLiveData<>();
    public void setLocationService(LocationService locationservice) {
        mLocationService.setValue(locationservice);
    }
    public LiveData<LocationService> getLocationService() {
        return mLocationService;
    }

    private final MutableLiveData<String> mCreateTime = new MutableLiveData<>();
    public void setCreateTime(String time) {
        mCreateTime.setValue(time);
    }
    public LiveData<String> getCreateTime() {
        return mCreateTime;
    }

    private final MutableLiveData<Integer> mRunningStatus = new MutableLiveData<>();
    public void setRunningStatus(Integer running) {
        mRunningStatus.setValue(running);
    }
    public LiveData<Integer> getRunningStatus() {
        return mRunningStatus;
    }

    private final MutableLiveData<Integer> mActivity = new MutableLiveData<>();
    public void setActivity(Integer activity) {
        mActivity.setValue(activity);
    }
    public LiveData<Integer> getActivity() {
        return mActivity;
    }
}

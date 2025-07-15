package com.example.sukarelawanmy.feature.viewmodel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sukarelawanmy.model.UserModel;

public class ShareViewModel extends ViewModel {


    private final MutableLiveData<UserModel> user = new MutableLiveData<>();

    public void setUser(UserModel userModel) {
        user.setValue(userModel);
    }

    public LiveData<UserModel> getUser() {
        return user;
    }
}

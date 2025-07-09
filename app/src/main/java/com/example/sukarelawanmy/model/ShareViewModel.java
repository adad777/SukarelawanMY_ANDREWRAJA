package com.example.sukarelawanmy.model;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
public class ShareViewModel extends ViewModel {


    private final MutableLiveData<UserModel> user = new MutableLiveData<>();

    public void setUser(UserModel userModel) {
        user.setValue(userModel);
    }

    public LiveData<UserModel> getUser() {
        return user;
    }
}

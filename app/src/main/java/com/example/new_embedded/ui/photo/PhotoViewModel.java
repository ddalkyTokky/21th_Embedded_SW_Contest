package com.example.new_embedded.ui.photo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PhotoViewModel extends ViewModel {
    private final MutableLiveData<String> mText;

    public PhotoViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
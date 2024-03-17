package com.example.new_embedded.ui.arrow;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ArrowViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ArrowViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
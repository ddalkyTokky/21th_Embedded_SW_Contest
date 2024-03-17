package com.example.new_embedded.ui.photo;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.new_embedded.MainActivity;
import com.example.new_embedded.aws.DownloadPhoto;
import com.example.new_embedded.databinding.FragmentPhotoBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class PhotoFragment extends Fragment {

    private FragmentPhotoBinding binding;
    //==================================================
    private MainActivity mActivity = null;
    private ImageView photoIV = null;
    private ProgressBar downloadPB = null;
    private FloatingActionButton reloadFAB = null;
    private FloatingActionButton downloadFAB = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        PhotoViewModel slideshowViewModel =
                new ViewModelProvider(this).get(PhotoViewModel.class);

        binding = FragmentPhotoBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.textPhoto;
//        photoViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        //====================================================
        loadImg();

        photoIV = binding.photoImage;
        reloadFAB = binding.reloadFloatActionButton;
        downloadFAB = binding.downloadFloatActionButton;
        downloadPB = binding.downloadProgress;

        reloadFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadImg();
            }
        });
        downloadFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendImg();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (MainActivity) context;
    }
    private void resendImg(){
        DownloadPhoto temp_object = new DownloadPhoto(mActivity.getApplicationContext(), downloadPB);
    }

    private void loadImg(){
        try {
            File files = new File(mActivity.getFilesDir().getAbsolutePath() + "/metro.jpg");

            if (files.exists() == true) {
                Bitmap myBitmap = BitmapFactory.decodeFile(files.getAbsolutePath());
                photoIV.setImageBitmap(myBitmap);

                int x = myBitmap.getWidth();
                int y = myBitmap.getHeight();

//                Toast.makeText(mActivity, String.valueOf(x) + ":" + String.valueOf(y), Toast.LENGTH_LONG).show();

                if(x > y) {
                    photoIV.setRotation(90);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
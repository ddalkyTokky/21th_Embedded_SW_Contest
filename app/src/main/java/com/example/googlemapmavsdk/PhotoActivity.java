package com.example.googlemapmavsdk;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.net.URL;

public class PhotoActivity extends AppCompatActivity {
    private ImageView mImage;
    private Button mResendBtn;
    private Button mChangeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        mImage = findViewById(R.id.imageview_photo);

        changeImg();

        mResendBtn = (Button) findViewById(R.id.button_resend);
        mResendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendImg();
            }
        });

        mChangeBtn = (Button) findViewById(R.id.button_change);
        mChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeImg();
            }
        });
    }

    private void resendImg(){
        AWSUtils awsutils = new AWSUtils(getApplicationContext());
    }

    private void changeImg(){
        String error = "";
        try {
            File files = new File(this.getFilesDir().getAbsolutePath() + "/metro.jpg");

            if (files.exists() == true) {
                Bitmap myBitmap = BitmapFactory.decodeFile(files.getAbsolutePath());
                mImage.setImageBitmap(myBitmap);
                error = "Image Changed!!";
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = e.toString();
        }
        Toast.makeText(getApplication(), error, Toast.LENGTH_LONG).show();
    }
}
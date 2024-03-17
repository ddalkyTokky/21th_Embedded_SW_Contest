package com.example.new_embedded.aws;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

public class DownloadPhoto {
    public DownloadPhoto(Context applicationContext, ProgressBar downloadPB) {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                applicationContext,
                "ap-northeast-2:7f7da145-bfdc-4c7a-bbb2-3aa9a4ee0e2d", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2 // 리전
        );

        // 반드시 호출해야 한다.
        TransferNetworkLossHandler.getInstance(applicationContext);

        // TransferUtility 객체 생성
        TransferUtility transferUtility = TransferUtility.builder()
                .context(applicationContext)
                .defaultBucket("soonyongbucket") // 디폴트 버킷 이름.
                .s3Client(new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2)))
                .build();

        // 다운로드 실행. object: "SomeFile.mp4". 두 번째 파라메터는 Local경로 File 객체.
//        Toast.makeText(applicationContext, applicationContext.getFilesDir().getAbsolutePath(), Toast.LENGTH_LONG).show();
        TransferObserver downloadObserver = transferUtility.download("metro.jpg", new File(applicationContext.getFilesDir().getAbsolutePath() + "/metro.jpg"));

        // 다운로드 과정을 알 수 있도록 Listener를 추가할 수 있다.
        downloadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if(state == TransferState.COMPLETED){
                    Toast.makeText(applicationContext, "DOWNLOAD Completed!", Toast.LENGTH_LONG).show();
                    if(downloadPB != null) {
                        downloadPB.setVisibility(View.INVISIBLE);
                    }
                }
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                try {
                    int done = (int) (bytesCurrent / bytesTotal * 100.0);
                    if(downloadPB != null) {
                        downloadPB.setProgress(done);
                        downloadPB.setVisibility(View.VISIBLE);
                    }
//                    Toast.makeText(applicationContext, "DOWNLOAD - - ID: " + String.valueOf(id) + "percent done = " + done, Toast.LENGTH_LONG).show();
                }
                catch (Exception e) {
//                    Toast.makeText(applicationContext, "DOWNLOAD ERROR - - ID: " + String.valueOf(id) + " - - EROOR: " + e.getMessage().toString(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onError(int id, Exception e) {
//                Toast.makeText(applicationContext, "DOWNLOAD ERROR - - ID: " + String.valueOf(id) + " - - EROOR: " + e.getMessage().toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

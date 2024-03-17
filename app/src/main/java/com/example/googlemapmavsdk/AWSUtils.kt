package com.example.googlemapmavsdk

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File

class AWSUtils (applicationContext: Context) {
    init{
// Cognito 샘플 코드. CredentialsProvider 객체 생성
        val credentialsProvider = CognitoCachingCredentialsProvider(
                applicationContext,
                "ap-northeast-2:7f7da145-bfdc-4c7a-bbb2-3aa9a4ee0e2d", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2 // 리전
        )

        // 반드시 호출해야 한다.
        TransferNetworkLossHandler.getInstance(applicationContext)

        // TransferUtility 객체 생성
        val transferUtility = TransferUtility.builder()
                .context(applicationContext)
                .defaultBucket("soonyongbucket") // 디폴트 버킷 이름.
                .s3Client(AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2)))
                .build()

        // 다운로드 실행. object: "SomeFile.mp4". 두 번째 파라메터는 Local경로 File 객체.
        Toast.makeText(applicationContext, applicationContext.filesDir.absolutePath, Toast.LENGTH_LONG).show()
        val downloadObserver = transferUtility.download("metro.jpg", File(applicationContext.filesDir.absolutePath + "/metro.jpg"))

        // 다운로드 과정을 알 수 있도록 Listener를 추가할 수 있다.
        downloadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    Toast.makeText(applicationContext, "DOWNLOAD Completed!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onProgressChanged(id: Int, current: Long, total: Long) {
                try {
                    val done = (((current.toDouble() / total) * 100.0).toInt()) //as Int
                    Toast.makeText(applicationContext, "DOWNLOAD - - ID: $id, percent done = $done", Toast.LENGTH_LONG).show()
                }
                catch (e: Exception) {

                    Toast.makeText(applicationContext, "Trouble calculating progress percent", Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(id: Int, ex: Exception) {
                Toast.makeText(applicationContext, "DOWNLOAD ERROR - - ID: $id - - EX: ${ex.message.toString()}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
package com.example.googlemapmavsdk;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class ArrowActivity extends AppCompatActivity implements SensorEventListener {
    private TextView txtResult;
    private double now_long;
    private double now_lat;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    private float azimuthinDegress = 0f;
    private float mCurrentDegress = 0f;

    private double bearing = 0f;

    private ImageView mArrow;

    private Double carLat;
    private Double carLong;
    private long lastUpdate = 0;
    private long lastGsendTime = 0;
    private Handler mHandler; // Our main handler that will receive callback notifications
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private TextView mReadBuffer;
    private ConnectThread mConnectThread; // bluetooth background worker thread to send and receive data
    private ConnectThread mConnectThread2; // bluetooth background worker thread to send and receive data
    private int alpha;
    private int gamma = -1;
    private int distance = -1;
    private int floor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrow);

        connectBluetooth();

        //Intent=============================

        Intent intent = getIntent(); /*데이터 수신*/

        double intentLatLong[] = intent.getExtras().getDoubleArray("carLatLong"); /*배열*/
        alpha = intent.getExtras().getInt("alpha");
        floor = intent.getExtras().getInt("floor");

        //Toast.makeText(getApplication(), "done", Toast.LENGTH_SHORT).show();

        carLat = intentLatLong[0];
        carLong = intentLatLong[1];

        //View=============================

        txtResult = (TextView) findViewById(R.id.txtResult);

        //Sensor===========================

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mArrow = (ImageView) findViewById(R.id.arrowImage);

        //GPS================================

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ArrowActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1, gpsLocationListener);
            lm.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 10, 1, gpsLocationListener);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 1, gpsLocationListener);
        }
    }

    final LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            String provider = location.getProvider();
//            if (Objects.equals(provider, "gps")) {
            now_long = location.getLongitude();
            now_lat = location.getLatitude();
//            }

            // 위도, 경도를 라디안 단위로 변환
            double w1 = now_lat * Math.PI / 180;
            double w2 = carLat * Math.PI / 180;
            double r1 = now_long * Math.PI / 180;
            double r2 = carLong * Math.PI / 180;

            double y = Math.sin(r2 - r1) * Math.cos(w2);
            double x = (Math.cos(w1) * Math.sin(w2)) - (Math.sin(w1) * Math.cos(w2) * Math.cos(r2 - r1));
            double seta = Math.atan2(y, x); // 방위각 (라디안)
            bearing = (seta * 180 / Math.PI + 360) % 360; // 방위각 (디그리, 정규화 완료)
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if ((System.currentTimeMillis() - lastGsendTime) > 1000) {
            if (gamma == -1) {
                if (mConnectThread != null) {
                    mConnectThread.write("g");
                }
                if (mConnectThread2 != null) {
                    mConnectThread2.write("g");
                }
            }
            lastGsendTime = System.currentTimeMillis();
        }

        if (sensorEvent.sensor == mAccelerometer) {
            System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometerSet = true;
        } else if (sensorEvent.sensor == mMagnetometer) {
            System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            azimuthinDegress = (int) (Math.toDegrees(SensorManager.getOrientation(mR, mOrientation)[0]) + 360) % 360;

            if ((System.currentTimeMillis() - lastUpdate) > 100) {
                float arrowDegree;
                String print_txt = "";
                if ((gamma >= 0) && (gamma <= 359)){
                    print_txt = " " + distance + "m \n";
                    if((alpha >= 0) && (alpha <= 359)){
                        arrowDegree = (540 + alpha - azimuthinDegress + gamma) % 360;
                    }
                    else{
                        arrowDegree = (float) bearing - azimuthinDegress;
                    }
                }
                else{
                    print_txt += " 거리정보 없음 \n";
                    arrowDegree = (float) bearing - azimuthinDegress;
                }
                if(floor == 999){
                    print_txt += " 층수정보 없음 ";
                }
                else{
                    if(floor >= 1) {
                        print_txt += " " + floor + "층 ";
                    }
                    else if (floor < 0){
                        print_txt += " B" + (floor * -1) + "층 ";
                    }
                }
                RotateAnimation ra = new RotateAnimation(mCurrentDegress, arrowDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                ra.setDuration(250);
                ra.setFillAfter(true);
                mArrow.startAnimation(ra);
                txtResult.setText(print_txt);
                lastUpdate = System.currentTimeMillis();
                mCurrentDegress = arrowDegree;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void connectBluetooth() {
        mConnectThread = ((StoreDevice) getApplication()).globalConnectThread;
        mConnectThread2 = ((StoreDevice) getApplication()).globalConnectThread2;

        mReadBuffer = (TextView) findViewById(R.id.textview_readbuffer);

        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE_READ) {
                        String readMessage;
                        readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);
//                        mReadBuffer.setText(readMessage);

                        int num = 0;
                        int num2 = 0;
                        boolean gamma_or_distance = true;
                        for (int i = 0; i < readMessage.length(); i++) {
                            char temp_item = readMessage.charAt(i);
                            if (temp_item == 'g') {
                                gamma_or_distance = true;
                            }
                            if (temp_item == 'd') {
                                gamma_or_distance = false;
                            }
                            if ((temp_item >= '0') && (temp_item <= '9')) {
                                if (gamma_or_distance) {
                                    num *= 10;
                                    num += (temp_item - 48);
                                } else {
                                    num2 *= 10;
                                    num2 += (temp_item - 48);
                                }
                            }
                        }
                        gamma = num;
                        distance = num2;
//                        Toast.makeText(getApplication(), "alpha: " + alpha + "\ngamma: " + gamma + "\nfloor: " + floor, Toast.LENGTH_SHORT).show();
                    }

                    if (msg.what == CONNECTING_STATUS) {
                        char[] sConnected;
                        if (msg.arg1 == 1) {
                            Toast.makeText(getApplication(), getString(R.string.BTConnected) + msg.obj, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplication(), getString(R.string.BTconnFail), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
        }
        if (mConnectThread != null) {
            try {
                mConnectThread.changeContextHandler(getApplicationContext(), mHandler);
            } catch (Exception e) {
            }
        }
        if (mConnectThread2 != null) {
            try {
                mConnectThread2.changeContextHandler(getApplicationContext(), mHandler);
            } catch (Exception e) {
            }
        }
    }
}
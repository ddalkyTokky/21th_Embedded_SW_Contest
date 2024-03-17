package com.example.new_embedded.ui.arrow;

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.new_embedded.MainActivity;
import com.example.new_embedded.connect.ConnectThread;
import com.example.new_embedded.connect.GlobalDeviceList;
import com.example.new_embedded.databinding.FragmentArrowBinding;

import java.nio.charset.StandardCharsets;

public class ArrowFragment extends Fragment implements SensorEventListener {

    private FragmentArrowBinding binding;

    //=======================================
    private MainActivity mActivity = null;
    private ImageView arrowIV;
    private TextView distFloorTV;
    //Sensor===============================
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    //GPS=================================
    private double now_long;
    private double now_lat;
    //Angle===============================
    private double bearing = 0f;
    private long lastRotate = 0;
    private long lastGsend = 0;
    private float azimuthinDegress = 0f;
    private float mCurrentDegress = 0f;
    private int gamma = -1;
    private int distance = -1;
    //Bluetooth=========================
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private Handler mHandler = null;
    private ConnectThread mConnectThread; // bluetooth background worker thread to send and receive data
    private ConnectThread mConnectThread2; // bluetooth background worker thread to send and receive data

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ArrowViewModel arrowViewModel =
                new ViewModelProvider(this).get(ArrowViewModel.class);

        binding = FragmentArrowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.textArrow;
//        arrowViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        //==============================

        arrowIV = binding.arrowImage;
        distFloorTV = binding.distFloorText;

        //Sensor===========================

        mSensorManager = (SensorManager) mActivity.getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //GPS================================

        final LocationManager lm = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(mActivity.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1, gpsLocationListener);
            lm.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 10, 1, gpsLocationListener);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 1, gpsLocationListener);
        }

        //Bluetooth==================================

        loadBluetooth();

        return root;
    }

    final LocationListener gpsLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            String provider = location.getProvider();
//            if (Objects.equals(provider, "gps")) {
            now_long = location.getLongitude();
            now_lat = location.getLatitude();
//            }

            // 위도, 경도를 라디안 단위로 변환
            double w1 = now_lat * Math.PI / 180;
            double w2 = mActivity.carLat * Math.PI / 180;
            double r1 = now_long * Math.PI / 180;
            double r2 = mActivity.carLong * Math.PI / 180;

            double y = Math.sin(r2 - r1) * Math.cos(w2);
            double x = (Math.cos(w1) * Math.sin(w2)) - (Math.sin(w1) * Math.cos(w2) * Math.cos(r2 - r1));
            double seta = Math.atan2(y, x); // 방위각 (라디안)
            bearing = (seta * 180 / Math.PI + 360) % 360; // 방위각 (디그리, 정규화 완료)
        }
    };

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
    public void onSensorChanged(SensorEvent event) {
        if ((System.currentTimeMillis() - lastGsend) > 1000) {
            if (gamma == -1) {
                if (mConnectThread != null) {
                    mConnectThread.write("g");
                }
                if (mConnectThread2 != null) {
                    mConnectThread2.write("g");
                }
            }
            lastGsend = System.currentTimeMillis();
        }

        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            azimuthinDegress = (int) (Math.toDegrees(SensorManager.getOrientation(mR, mOrientation)[0]) + 360) % 360;

            if ((System.currentTimeMillis() - lastRotate) > 100) {
                float arrowDegree;
                String print_txt = "";
                if ((gamma >= 0) && (gamma <= 359)) {
                    print_txt = distance + "m\n";
                    if ((mActivity.alpha >= 0) && (mActivity.alpha <= 359)) {
                        arrowDegree = (540 + mActivity.alpha - azimuthinDegress + gamma) % 360;
                    } else {
                        arrowDegree = (float) bearing - azimuthinDegress;
                    }
                } else {
                    print_txt += "거리정보 없음\n";
                    arrowDegree = (float) bearing - azimuthinDegress;
                }
                if (mActivity.floor == 999) {
                    print_txt += "층수정보 없음";
                } else {
                    if (mActivity.floor >= 1) {
                        print_txt += mActivity.floor + "층";
                    } else if (mActivity.floor < 0) {
                        print_txt += "B" + (mActivity.floor * -1) + "층";
                    }
                }
                RotateAnimation ra = new RotateAnimation(mCurrentDegress, arrowDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                ra.setDuration(250);
                ra.setFillAfter(true);
                arrowIV.startAnimation(ra);
                lastRotate = System.currentTimeMillis();
                mCurrentDegress = arrowDegree;

                distFloorTV.setText(print_txt);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void loadBluetooth() {
        mConnectThread = mActivity.globalConnectThread;
        mConnectThread2 = mActivity.globalConnectThread2;

        if(mHandler == null) {
            mHandler = createHandler();
        }

        if (mConnectThread != null) {
            try {
                mConnectThread.changeContextHandler(mActivity.getApplicationContext(), mHandler);
            } catch (Exception e) {
            }
        }
        if (mConnectThread2 != null) {
            try {
                mConnectThread2.changeContextHandler(mActivity.getApplicationContext(), mHandler);
            } catch (Exception e) {
            }
        }
    }

    private Handler createHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String readMessage;
                    readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);

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
                }
            }
        };
    }
}
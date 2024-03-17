package com.example.new_embedded.ui.map;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.new_embedded.MainActivity;
import com.example.new_embedded.R;
import com.example.new_embedded.aws.DownloadPhoto;
import com.example.new_embedded.connect.ConnectThread;
import com.example.new_embedded.databinding.FragmentMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.nio.charset.StandardCharsets;

public class MapFragment extends Fragment implements OnMapReadyCallback, SensorEventListener {

    private FragmentMapBinding binding;

    //Bluetooth==============================
    private ConnectThread mConnectThread; // bluetooth background worker thread to send and receive data
    private ConnectThread mConnectThread2; // bluetooth background worker thread to send and receive data
    private Handler mHandler;
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private boolean alpha_floor_flag = false;
    private int alpha_floor_stack = 0;
    //=========================================
    private GoogleMap mMap;
    private double now_long;
    private double now_lat;
    private Marker carMarker = null;
    private static final float MISSION_MARKER_COLOR = 180f;
    private static final float ZOOM_SCALE = 17f;
    private MainActivity mActivity = null;
    private Marker humanMarker = null;
    CameraPosition cameraPosition;

    //================================

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MapViewModel homeViewModel =
                new ViewModelProvider(this).get(MapViewModel.class);

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.textHome;
//        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        //=================================================

        final LocationManager lm = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(mActivity.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            lm.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 10, 1, gpsLocationListener);
        }

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
//            if (Objects.equals(provider, "fused")) {
//                now_long = location.getLongitude();
//                now_lat = location.getLatitude();
//            }

            if (humanMarker != null) {
                humanMarker.remove();
            }
            humanMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(now_lat, now_long)).title("My Location"));

            // 위도, 경도를 라디안 단위로 변환
            double w1 = now_lat * Math.PI / 180;
            double w2 = mActivity.carLat * Math.PI / 180;
            double r1 = now_long * Math.PI / 180;
            double r2 = mActivity.carLong * Math.PI / 180;

            double y = Math.sin(r2 - r1) * Math.cos(w2);
            double x = Math.cos(w1) * Math.sin(w2) - Math.sin(w1) * Math.cos(w2) * Math.cos(r2 - r1);
            double seta = Math.atan2(y, x); // 방위각 (라디안)
            double bearing = (seta * 180 / Math.PI + 360) % 360; // 방위각 (디그리, 정규화 완료)

            cameraPosition = new CameraPosition.Builder().target(new LatLng(mActivity.carLat, mActivity.carLong))      // Sets the center of the map to Mountain View
                    .zoom(ZOOM_SCALE)                   // Sets the zoom
                    .bearing((float) bearing)      // Sets the orientation of the camera to east
                    .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 100, null);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (MainActivity) context;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //=============================================

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (mConnectThread != null) {
                mConnectThread.write("s");
            }
        } catch (Exception e) {
        }
        try {
            if (mConnectThread2 != null) {
                mConnectThread2.write("s");
            }
        } catch (Exception e) {
        }
    }

    //=============================================
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng initialPoint = new LatLng(mActivity.carLat, mActivity.carLong);
        carMarker = mMap.addMarker(new MarkerOptions().position(initialPoint).icon(BitmapDescriptorFactory.defaultMarker(MISSION_MARKER_COLOR)).title("My Car"));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(ZOOM_SCALE));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(initialPoint));

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void loadBluetooth() {
        mConnectThread = mActivity.globalConnectThread;
        mConnectThread2 = mActivity.globalConnectThread2;

        if (mHandler == null) {
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
                    String readMessage = null;
                    readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);

                    String input_str = "";
                    for (int i = 0; i < readMessage.length(); i++) {
                        char temp_item = readMessage.charAt(i);
                        if ((temp_item >= '0') && (temp_item <= '9') ||
                                (temp_item >= 'a') && (temp_item <= 'z') ||
                                (temp_item >= 'A') && (temp_item <= 'Z') ||
                                (temp_item == '-')) {
                            input_str += temp_item;
                        }
                    }
                    if (input_str.equals("e")) {
                        mActivity.carLat = now_lat;
                        mActivity.carLong = now_long;
                        Toast.makeText(mActivity.getApplication(), "시동 꺼짐", Toast.LENGTH_LONG).show();
                        carMarker.remove();
                        carMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(mActivity.carLat, mActivity.carLong)).icon(BitmapDescriptorFactory.defaultMarker(MISSION_MARKER_COLOR)).title("My Car"));
                        alpha_floor_stack = 0;
                        alpha_floor_flag = true;
                        DownloadPhoto temp_object = new DownloadPhoto(mActivity.getApplicationContext(), null);

                    } else if (alpha_floor_flag) {
                        try {
                            if (alpha_floor_stack == 0) {
                                mActivity.alpha = Integer.valueOf(input_str);
                                alpha_floor_stack = (1 - alpha_floor_stack);
                            } else if (alpha_floor_stack == 1) {
                                mActivity.floor = Integer.valueOf(input_str);
                                alpha_floor_stack = (1 - alpha_floor_stack);
                                alpha_floor_flag = false;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        };
    }

    private void stopSend() {
        try {
            if (mConnectThread != null) {
                mConnectThread.write("s");
            }
        } catch (Exception e) {
        }
        try {
            if (mConnectThread2 != null) {
                mConnectThread2.write("s");
            }
        } catch (Exception e) {
        }
    }
}
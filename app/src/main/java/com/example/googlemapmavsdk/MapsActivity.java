package com.example.googlemapmavsdk;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.googlemapmavsdk.databinding.ActivityMapsBinding;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {
    private GoogleMap mMap;
    private Marker humanMarker = null;
    private Marker carMarker = null;
    private ActivityMapsBinding binding;
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectThread mConnectThread; // bluetooth background worker thread to send and receive data
    private ConnectThread mConnectThread2; // bluetooth background worker thread to send and receive data
    private double carLat = 37.601070088505644;
    private double carLong = 126.865068843289;
    private static final float ZOOM_SCALE = 17f;
    private static final float MISSION_MARKER_COLOR = 180f;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private double now_long;
    private double now_lat;
    private int alpha = -1;
    private int floor = 999;
    private boolean alpha_floor_flag = false;
    private int alpha_floor_stack = 0;

//    private long lastGPSUpdate = 0;

    CameraPosition cameraPosition;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            lm.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 10, 1, gpsLocationListener);
        }
        connectBluetooth();
    }

    final LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
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
            double w2 = carLat * Math.PI / 180;
            double r1 = now_long * Math.PI / 180;
            double r2 = carLong * Math.PI / 180;

            double y = Math.sin(r2 - r1) * Math.cos(w2);
            double x = Math.cos(w1) * Math.sin(w2) - Math.sin(w1) * Math.cos(w2) * Math.cos(r2 - r1);
            double seta = Math.atan2(y, x); // 방위각 (라디안)
            double bearing = (seta * 180 / Math.PI + 360) % 360; // 방위각 (디그리, 정규화 완료)

            cameraPosition = new CameraPosition.Builder().target(new LatLng(carLat, carLong))      // Sets the center of the map to Mountain View
                    .zoom(ZOOM_SCALE)                   // Sets the zoom
                    .bearing((float) bearing)      // Sets the orientation of the camera to east
                    .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 100, null);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng initialPoint = new LatLng(carLat, carLong);
        carMarker = mMap.addMarker(new MarkerOptions().position(initialPoint).icon(BitmapDescriptorFactory.defaultMarker(MISSION_MARKER_COLOR)).title("My Car"));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(ZOOM_SCALE));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(initialPoint));

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.button_photo:
                intent = new Intent(getApplicationContext(), PhotoActivity.class);

                startActivity(intent);
                break;
            case R.id.button_arrow:
                intent = new Intent(getApplicationContext(), ArrowActivity.class);

                double[] intentLatLong = {carLat, carLong};
                intent.putExtra("carLatLong", intentLatLong);
                intent.putExtra("alpha", alpha);
                intent.putExtra("floor", floor);

                startActivity(intent);
                break;
            case R.id.button_bluetooth:
                intent = new Intent(getApplicationContext(), BluetoothActivity.class);

                startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        connectBluetooth();
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

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
    }

    private void connectBluetooth() {
        mConnectThread = ((StoreDevice) getApplication()).globalConnectThread;
        mConnectThread2 = ((StoreDevice) getApplication()).globalConnectThread2;
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
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
                            carLat = now_lat;
                            carLong = now_long;
                            Toast.makeText(getApplication(), "car LAT LONG changed!!", Toast.LENGTH_LONG).show();
                            carMarker.remove();
                            carMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(carLat, carLong)).icon(BitmapDescriptorFactory.defaultMarker(MISSION_MARKER_COLOR)).title("My Car"));
                            alpha_floor_stack = 0;
                            alpha_floor_flag = true;
                            AWSUtils awsutils = new AWSUtils(getApplicationContext());
                        } else if (alpha_floor_flag) {
                            try {
                                if (alpha_floor_stack == 0) {
                                    alpha = Integer.valueOf(input_str);
                                    alpha_floor_stack = (1 - alpha_floor_stack);
                                } else if (alpha_floor_stack == 1) {
                                    floor = Integer.valueOf(input_str);
                                    alpha_floor_stack = (1 - alpha_floor_stack);
                                    alpha_floor_flag = false;
                                }
                            } catch (Exception e) {
                            }
                        }
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
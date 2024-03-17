package com.example.new_embedded.ui.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.new_embedded.MainActivity;
import com.example.new_embedded.R;
import com.example.new_embedded.connect.ConnectThread;
import com.example.new_embedded.connect.GlobalDeviceList;
import com.example.new_embedded.databinding.FragmentBluetoothBinding;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class BluetoothFragment extends Fragment {

    private FragmentBluetoothBinding binding;
    //====================================
    private final String TAG = this.getClass().getSimpleName();
    private Handler mHandler; // Our main handler that will receive callback notifications
    private Handler mHandler2; // Our main handler that will receive callback notifications
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    //layouts=============================
    private MainActivity mActivity = null;
    private TextView onoffTV;
    private SwitchCompat onoffSC;
    private ListView deviceLV;

    //Connection===================================
    private TextView lastTouchedTV = null;
    private TextView lastTouchedTV2 = null;
    private int deviceStack = 0;
    //bluetooths==========================
    private ArrayAdapter<String> mBTArrayAdapter;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ConnectThread mConnectThread; // bluetooth background worker thread to send and receive data
    private ConnectThread mConnectThread2; // bluetooth background worker thread to send and receive data

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        BluetoothViewModel bluetoothViewModel =
                new ViewModelProvider(this).get(BluetoothViewModel.class);

        binding = FragmentBluetoothBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.textBluetooth;
//        galleryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        //handler=============================

        createHandler();

        //bluetooths=========================

        mBTArrayAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        //permission=========================

        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        //Views==============================
        onoffTV = binding.onoffText;
        onoffSC = binding.onoffSwitch;
        deviceLV = binding.deviceList;

        deviceLV.setAdapter(mBTArrayAdapter); // assign model to view
        deviceLV.setOnItemClickListener(mDeviceClickListener);

        //bluetooth===========================

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
        }
        else {
            onoffSC.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                   @Override
                                                   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                       if (isChecked) {
                                                           bluetoothOn();
                                                       } else {
                                                           bluetoothOff();
                                                           onoffTV.setText("블루투스 꺼짐  ");
                                                       }
                                                   }
                                               }
            );
        }

        bluetoothOnOffCheck();

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

    //=====================================================

    @Override
    public void onResume() {
        super.onResume();

        bluetoothOnOffCheck();
    }

    //bluetooth========================================

    private void bluetoothOnOffCheck(){
        if (mBTAdapter.isEnabled()) {
            onoffTV.setText("블루투스 켜짐  ");
            onoffSC.setChecked(true);
            listPairedDevices();
        }
        else{
            onoffTV.setText("블루투스 꺼짐  ");
            onoffSC.setChecked(false);
        }
    }

    private void bluetoothOn() {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
    }

    @SuppressLint("MissingPermission")
    private void bluetoothOff() {
        mBTAdapter.disable(); // turn off
        mBTArrayAdapter.clear();
    }

    @SuppressLint("MissingPermission")
    private void listPairedDevices() {
        mBTArrayAdapter.clear();
        mPairedDevices = mBTAdapter.getBondedDevices();
        if (mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (!mBTAdapter.isEnabled()) {
                return;
            }

            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            ((TextView) view).setTextColor(ContextCompat.getColor(mActivity, R.color.sky_blue));

            if(deviceStack == 0){
                if(lastTouchedTV != null) {
                    lastTouchedTV.setTextColor(ContextCompat.getColor(mActivity, R.color.black));
                }
                lastTouchedTV = (TextView) view;

                mConnectThread = new ConnectThread(TAG, mBTAdapter, mHandler, address, mActivity.getApplicationContext(), name);
                mConnectThread.start();

                mActivity.globalConnectThread = mConnectThread;

                deviceStack = 1;
            }
            else if(deviceStack == 1){
                if(lastTouchedTV2 != null) {
                    lastTouchedTV2.setTextColor(ContextCompat.getColor(mActivity, R.color.black));
                }
                lastTouchedTV2 = (TextView) view;

                mConnectThread2 = new ConnectThread(TAG, mBTAdapter, mHandler2, address, mActivity.getApplicationContext(), name);
                mConnectThread2.start();

                mActivity.globalConnectThread2 = mConnectThread2;

                deviceStack = 0;
            }
        }
    };

    private void createHandler(){
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    lastTouchedTV.setTextColor(ContextCompat.getColor(mActivity, R.color.green));
                }
                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1){
                    }
                    else {
                        lastTouchedTV.setTextColor(ContextCompat.getColor(mActivity, R.color.black));
                        mActivity.globalConnectThread = null;
                        deviceStack = 0;
                    }
                }
            }
        };

        mHandler2 = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    lastTouchedTV2.setTextColor(ContextCompat.getColor(mActivity, R.color.green));
                }
                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1){
                    }
                    else {
                        lastTouchedTV2.setTextColor(ContextCompat.getColor(mActivity, R.color.black));
                        mActivity.globalConnectThread2 = null;
                        deviceStack = 1;
                    }
                }
            }
        };
    }
}
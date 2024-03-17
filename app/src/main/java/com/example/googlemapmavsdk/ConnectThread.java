package com.example.googlemapmavsdk;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

public class ConnectThread extends Thread {
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private String mTAG;
    private BluetoothAdapter mBTAdapter;
    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket; // bi-directional client-to-client data path
    private String mAddress;
    private String mName;
    private Context mContext;
    private String error1;
    private String error2;

    public ConnectThread(String TAG, BluetoothAdapter inAdapter, Handler inHandler, String inAddress, Context inContext, String inName) {
        mTAG = TAG;
        mBTAdapter = inAdapter;
        mHandler = inHandler;
        mAddress = inAddress;
        mContext = inContext;
        mName = inName;

        BluetoothDevice device = mBTAdapter.getRemoteDevice(mAddress);
        try {
            mBTSocket = createBluetoothSocket(device);
        } catch (IOException e) {
//            Toast.makeText(mContext, mContext.getString(R.string.ErrSockCrea), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        boolean fail = false;
        mBTAdapter.cancelDiscovery();
        // Establish the Bluetooth socket connection.
        try {
            mBTSocket.connect();
        } catch (IOException e) {
            error1 = e.toString();
            try {
                fail = true;
                mBTSocket.close();
                mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                        .sendToTarget();
            } catch (IOException e2) {
                error2 = e2.toString();
                //insert code to deal with this
//                Toast.makeText(mContext, mContext.getString(R.string.ErrSockCrea), Toast.LENGTH_SHORT).show();
            }
        }
        if (!fail) {
            mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
            mConnectedThread.start();

            mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, mName)
                    .sendToTarget();
        } else {
            mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, error1 + error2)
                    .sendToTarget();
        }
    }

    public void write(String input) {
        if (mConnectedThread != null) //First check to make sure thread created
            mConnectedThread.write(input);
    }

    public void changeContextHandler(Context inContext, Handler inHandler) {
        mContext = inContext;
        mHandler = inHandler;
        mConnectedThread.changeHandler(mHandler);
    }

    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(mTAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }
}

package com.coderzheaven.bluetoothdemo.threads;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.coderzheaven.bluetoothdemo.constants.Constants;

import java.io.IOException;
import java.util.UUID;

// Client which initiates a connection //

public class ClientThread extends Thread {

    private BluetoothSocket bTSocket = null;
    public static final String TAG = "ClientThread";

    public BluetoothSocket connect(BluetoothAdapter bTAdapter, BluetoothDevice bTDevice, UUID mUUID, Handler mHandler) {

        try {
            bTSocket = bTDevice.createRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            Log.d(TAG, "Could not create RFCOMM socket:" + e.toString());
            return bTSocket;
        }

        bTAdapter.cancelDiscovery();

        try {
            bTSocket.connect();
        } catch (IOException e) {
            Log.d(TAG, "Could not connect: " + e.toString());
            try {
                bTSocket.close();
            } catch (IOException close) {
                Log.d(TAG, "Could not close connection:" + e.toString());
                return bTSocket;
            }
            return bTSocket;
        }

        byte[] bytes = bTDevice.getName().getBytes();
        byte[] buffer = new byte[1024];
        mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME, 0, -1, bytes)
                .sendToTarget();

        return bTSocket;
    }

    public boolean cancel() {
        if (bTSocket != null) {
            try {
                bTSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Could not close connection:" + e.toString());
                return false;
            }
        }
        return true;
    }
}
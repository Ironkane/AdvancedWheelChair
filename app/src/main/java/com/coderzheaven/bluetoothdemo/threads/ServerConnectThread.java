package com.coderzheaven.bluetoothdemo.threads;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.coderzheaven.bluetoothdemo.constants.Constants;

import java.io.IOException;
import java.util.UUID;

public class ServerConnectThread  extends Thread{

    private BluetoothSocket bTSocket;
    Handler mHandler;
    public static final String TAG = "ServerConnectThread";

    public BluetoothSocket acceptConnection(BluetoothAdapter bTAdapter, UUID mUUID, Handler mHandler) {

        BluetoothServerSocket temp = null;
        this.mHandler = mHandler;
        try {
            temp = bTAdapter.listenUsingRfcommWithServiceRecord("Service_Name", mUUID);
        } catch (IOException e) {
            Log.d(TAG, "Could not get a BluetoothServerSocket:" + e.toString());
        }
        while (true) {
            try {
                bTSocket = temp.accept();
            } catch (IOException e) {
                Log.d(TAG, "Could not accept an incoming connection.");
                break;
            }
            if (bTSocket != null) {
                try {
                    temp.close();

                    byte[] bytes0 = "Connected".getBytes();
                    byte[] buffer0 = new byte[1024];
                    mHandler.obtainMessage(Constants.MESSAGE_SERVER_CONNECTED, 0, -1, bytes0)
                            .sendToTarget();

                } catch (IOException e) {
                    Log.d(TAG, "Could not close ServerSocket:" + e.toString());
                }
                break;
            }
        }

        return bTSocket;
    }

    public void closeConnection() {
        if (bTSocket != null) {
            try {
                bTSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Could not close connection:" + e.toString());
            }
        }
    }
}
package com.coderzheaven.bluetoothdemo.threads;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.coderzheaven.bluetoothdemo.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DeviceConnectThread extends Thread {

    private final BluetoothSocket mSocket;
    private final InputStream mInStream;
    private final OutputStream mOutStream;
    Handler mHandler;
    public static final String TAG = "DeviceConnectThread";

    public DeviceConnectThread(BluetoothSocket socket, Handler mHandler) {
        this.mHandler = mHandler;
        mSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mInStream = tmpIn;
        mOutStream = tmpOut;
    }

    public void run() {

        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes = 0; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.i(TAG, e.toString());
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mOutStream.write(bytes);
            mHandler.obtainMessage(Constants.MESSAGE_WRITE, 0, -1, bytes)
                    .sendToTarget();
        } catch (IOException e) {
            Log.i(TAG, "Write Error : " + e.toString());
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mSocket.close();
        } catch (IOException e) {
        }
    }
}
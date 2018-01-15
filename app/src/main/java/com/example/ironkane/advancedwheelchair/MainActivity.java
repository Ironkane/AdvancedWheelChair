package com.example.ironkane.advancedwheelchair;


import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coderzheaven.bluetoothdemo.constants.Constants;
import com.coderzheaven.bluetoothdemo.threads.ClientThread;
import com.coderzheaven.bluetoothdemo.threads.DeviceConnectThread;
import com.coderzheaven.bluetoothdemo.threads.ServerConnectThread;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;



public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener, View.OnClickListener


{

   // private final Handler mHandler = new Handler();
    private Runnable mTimer1;
    private Runnable mTimer2;
    private LineGraphSeries<DataPoint> tempseries;
    private LineGraphSeries<DataPoint> pulseseries;


    /*BLUETOOTH DECLARATIONS*/
    String[] values = new String[]{
            "Check Bluetooth Compatibility",
            "Turn On Bluetooth",
            "Make Discoverable",
            "Show Paired And Online BT devices",
            "Cancel Discovery",
            "Disconnect",
            "Turn Off Bluetooth",
    };

    BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVERABLE_BT = 0;

    private static final String TAG = "Bluetooth";

    ListView listView;
    private FrameLayout frameLayout;
    ArrayList<BluetoothDevice> devices;
    ArrayList<String> allDevices;
    private BluetoothDevice deviceToConnect;

    private static final UUID MY_UUID_SECURE =
          UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
            //UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

    private BluetoothSocket curBTSocket = null;

    ClientThread connectThread;
    DeviceConnectThread deviceConnectThread;
    ServerConnectThread serverConnectThread;

    AlertDialog alertDialogObject;
    ArrayAdapter<String> devicesListAdapter;

    LinearLayout linSendMessage;
    Button btnSend;
    EditText edtMessage;
    public static final String DEV_CONNECTED = "DEVICE_CONNECTED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FrameLayout bluetoothview = (FrameLayout) findViewById(R.id.bluetooth_activity);
        bluetoothview.setVisibility(View.GONE);


        //TEMPERATURE GRAPH CODE HERE//
        final GraphView graph = (GraphView) findViewById(R.id.tempgraph);
        // data
        tempseries = new LineGraphSeries<DataPoint>();
        graph.addSeries(tempseries);
        tempseries.setColor(Color.RED);
        // customize a little bit viewport
        Viewport tempviewport = graph.getViewport();
        tempviewport.setYAxisBoundsManual(true);
        tempviewport.setMinY(0);
        tempviewport.setMaxY(60);
        tempviewport.setScrollable(true);



        //PULSE GRAPH CODE HERE//

        final GraphView pulsegraph = (GraphView)findViewById(R.id.pulsegraph);
        pulseseries = new LineGraphSeries<DataPoint>();
        pulsegraph.addSeries(pulseseries);
        pulseseries.setColor(Color.GREEN);
        Viewport pulseviewport = pulsegraph.getViewport();
        pulseviewport.setYAxisBoundsManual(true);
        pulseviewport.setMinY(0);
        pulseviewport.setMaxY(150);
        pulseviewport.setScrollable(true);




        /*BLUETOOTH CODE HERE*/


        //setContentView(R.layout.activity_main);

        frameLayout = (FrameLayout) findViewById(R.id
                .bluetooth_activity);

        linSendMessage = (LinearLayout) findViewById(R.id.l1);
        listView = (ListView) findViewById(R.id.list);
        btnSend = (Button) findViewById(R.id.btnSend);
        edtMessage = (EditText) findViewById(R.id.edtMessage);

        btnSend.setOnClickListener(this);

        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bReciever, filter);




    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        TextView pulse = (TextView) findViewById(R.id.content_pulserate);
        pulse.setVisibility(View.GONE);

        TextView pulse2 = (TextView) findViewById(R.id.content_pulserate2);
        pulse2.setVisibility(View.GONE);

        ImageView pulse_logo = (ImageView) findViewById(R.id.pulserate_logo);
        pulse_logo.setVisibility(View.GONE);

        TextView temp = (TextView) findViewById(R.id.content_temperature);
        temp.setVisibility(View.GONE);

        TextView temp2 = (TextView) findViewById(R.id.content_temperature2);
        temp2.setVisibility(View.GONE);

        ImageView temp_logo = (ImageView) findViewById(R.id.temperature_logo);
        temp_logo.setVisibility(View.GONE);

        FrameLayout j_frame = (FrameLayout) findViewById(R.id.joystick_mainframe);
        j_frame.setVisibility(View.GONE);

        final FrameLayout temp_graph = (FrameLayout) findViewById(R.id.temperature_graph);
        temp_graph.setVisibility(View.GONE);

        FrameLayout pulse_graph = (FrameLayout) findViewById(R.id.pulse_graph);
        pulse_graph.setVisibility(View.GONE);

        TextView tvalue = (TextView) findViewById(R.id.value_temperature);
        tvalue.setVisibility(View.GONE);
        TextView pvalue= (TextView) findViewById(R.id.value_pulserate);
        pvalue.setVisibility(View.GONE);
        TextView tvalue2 = (TextView) findViewById(R.id.value_temperature2);
        tvalue2.setVisibility(View.GONE);
        TextView pvalue2= (TextView) findViewById(R.id.value_pulserate2);
        pvalue2.setVisibility(View.GONE);

        FrameLayout bluetooth_view = (FrameLayout) findViewById(R.id.bluetooth_activity);
        bluetooth_view.setVisibility(View.GONE);

        if (id == R.id.navbar_monitor) {

            pulse.setVisibility(View.VISIBLE);
            pulse_logo.setVisibility(View.VISIBLE);
            temp.setVisibility(View.VISIBLE);
            temp_logo.setVisibility(View.VISIBLE);
            temp_graph.setVisibility(View.VISIBLE);
            pulse_graph.setVisibility(View.VISIBLE);
            tvalue.setVisibility(View.VISIBLE);
            pvalue.setVisibility(View.VISIBLE);

        } else if (id == R.id.navbar_control) {

            j_frame.setVisibility(View.VISIBLE);
            pulse2.setVisibility(View.VISIBLE);
            temp2.setVisibility(View.VISIBLE);
            tvalue2.setVisibility(View.VISIBLE);
            pvalue2.setVisibility(View.VISIBLE);

            JoystickView joystick = (JoystickView) findViewById(R.id.joystick_view);
            joystick.setFixedCenter(true);

            joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
                @Override
                public void onMove(int angle, int strength) {
                    // do whatever you want
                }
            });

        } else if (id == R.id.navbar_emergency) {

        } else if (id == R.id.navbar_connect) {

            bluetooth_view.setVisibility(View.VISIBLE);



        } else if (id == R.id.navbar_sos) {

        } else if (id == R.id.navbar_report) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    @Override
    public void onResume() {
        super.onResume();

        if (mBluetoothAdapter.isEnabled()) {
            startAsServer();
        }

        mTimer1 = new Runnable() {
            @Override
            public void run() {
                tempseries.resetData(generatetempData());
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.postDelayed(mTimer1, 1000);

        mTimer2 = new Runnable() {
            @Override
            public void run() {
                pulseseries.resetData(generatepulseData());
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.postDelayed(mTimer2, 1000);
    }


    @Override
    public void onPause() {
        mHandler.removeCallbacks(mTimer1);
        mHandler.removeCallbacks(mTimer2);
        super.onPause();
    }

    private DataPoint[] generatetempData() {
        int count = 9;
        DataPoint[] values = new DataPoint[count];
        for (int i=0; i<count; i++) {
            double x = i;
            double f = mRand.nextDouble()*0.15+0.3;
            double y = Math.sin(i*f+2) + mRand.nextDouble()*30;
            TextView tempvalue = (TextView) findViewById(R.id.value_temperature);
            tempvalue.setText("" +String.format("%.2f",y) + " C");
            TextView tempvalue2 = (TextView) findViewById(R.id.value_temperature2);
            tempvalue2.setText("" +String.format("%.2f",y) + " C");
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }


    private DataPoint[] generatepulseData() {
        int count = 9;
        DataPoint[] values = new DataPoint[count];
        for (int i=0; i<count; i++) {
            double x = i;
            double f = mRand.nextDouble()*0.15+0.3;
            double y = Math.sin(i*f+2) + mRand.nextDouble()*80;
            TextView pulsevalue = (TextView) findViewById(R.id.value_pulserate);
            pulsevalue.setText("" +String.format("%.2f",y) + " bpm");
            TextView pulsevalue2 = (TextView) findViewById(R.id.value_pulserate2);
            pulsevalue2.setText("" +String.format("%.2f",y) + " bpm");
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }

    double mLastRandom = 2;
    Random mRand = new Random();
    private double getRandom() {
        return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
    }





    /*BLUETOOTH FUNCTIONS*/


    private void turnOn() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void makeDiscoverable() {
        if (!mBluetoothAdapter.isDiscovering()) {
            showMessage("Making Discoverable...");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(enableBtIntent, REQUEST_DISCOVERABLE_BT);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {

        switch (position) {
            case 0:
                checkCompatibility();
                break;
            case 1:
                turnOn();
                break;
            case 2:
                makeDiscoverable();
                break;
            case 3:
                startDiscovery();
                break;
            case 4:
                cancelDiscovery();
                break;
            case 5:
                disconnect();
                break;
            default:
                turnOff();

        }

    }

    private void disconnect() {
        if (curBTSocket != null) {
            try {
                curBTSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private void startDiscovery() {
        showMessage("Starting Discovery...");
        getPairedDevices();
        mBluetoothAdapter.startDiscovery();
    }

    private void cancelDiscovery() {
        showMessage("Cancelling Discovery...");
        unregisterReceiver(bReciever);
        mBluetoothAdapter.cancelDiscovery();
    }

    private void getPairedDevices() {

        if (devices == null)
            devices = new ArrayList<BluetoothDevice>();
        else
            devices.clear();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice curDevice : pairedDevices) {
                devices.add(curDevice);
            }
            Log.i(TAG, "Paired Number of Devices : " + pairedDevices.size());
            showPairedList();
        }
    }

    private void turnOff() {
        mBluetoothAdapter.disable();
    }

    private void checkCompatibility() {
        // Phone does not support Bluetooth so let the user know and exit.
        if (mBluetoothAdapter == null) {
            showMessage("Your phone does not support Bluetooth");
        }
        else if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMessage("Your phone does not support Bluetooth LE");
            finish();
        }
        else {
            showMessage("Your phone supports Bluetooth ");
        }



    }

    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice curDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(curDevice);
            }
            Log.i(TAG, "All BT Devices : " + devices.size());
            if (devices.size() > 0) {
                showPairedList();
            }
        }
    };

    public void connectAsClient() {
        showMessage("Connecting for online Bluetooth devices...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (deviceToConnect != null) {
                    if (connectThread != null) {
                        connectThread.cancel();
                        connectThread = null;
                        linSendMessage.setVisibility(View.GONE);
                    }
                    connectThread = new ClientThread();
                    curBTSocket = connectThread.connect(mBluetoothAdapter, deviceToConnect, MY_UUID_SECURE, mHandler);
                    connectThread.start();
                }
            }
        }).start();
    }

    public void killServerThread() {
        if (serverConnectThread != null) {
            serverConnectThread.closeConnection();
            serverConnectThread = null;
            linSendMessage.setVisibility(View.GONE);
        }
    }

    private void startAsServer() {
        showMessage("Listening for online Bluetooth devices...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                serverConnectThread = new ServerConnectThread();
                curBTSocket = serverConnectThread.acceptConnection(mBluetoothAdapter, MY_UUID_SECURE, mHandler);
            }
        }).start();
    }


    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            byte[] buf = (byte[]) msg.obj;

            switch (msg.what) {

                case Constants.MESSAGE_WRITE:
                    // construct a string from the buffer
                    String writeMessage = new String(buf);
                    Log.i(TAG, "Write Message : " + writeMessage);
                    showMessage("Message Sent : " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(buf, 0, msg.arg1);
                    Log.i(TAG, "readMessage : " + readMessage);
                    showMessage("Message Received : " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName = new String(buf);
                    showMessage("Connected to " + mConnectedDeviceName);
                    linSendMessage.setVisibility(View.VISIBLE);
                    sendMessageToDevice();
                    break;
                case Constants.MESSAGE_SERVER_CONNECTED:
                    showMessage("CONNECTED");
                    Log.i(TAG, "Connected...");
                    linSendMessage.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    public void sendMessageToDevice() {
        deviceConnectThread = new DeviceConnectThread(curBTSocket, mHandler);
        deviceConnectThread.start();
        String message = edtMessage.getText().toString().trim();
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            deviceConnectThread.write(send);
        }
    }

    public void showMessage(String message) {
      /*  Snackbar snackbar = Snackbar
                .make(constraintLayout, message, Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        view.setBackgroundColor(Color.GREEN);
        TextView textView = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.BLACK);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.BOTTOM;
        view.setLayoutParams(params);
        snackbar.show();*/

        Snackbar.make(frameLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

    }

    public void showPairedList() {

        List<String> tempDevices = new ArrayList<String>();

        for (BluetoothDevice b : devices) {
            String paired = "Paired";
            if (b.getBondState() != 12) {
                paired = "Not Paired";
            }
            tempDevices.add(b.getName() + " - [ " + paired + " ] ");
        }

        if (allDevices == null)
            allDevices = new ArrayList<String>();
        else
            allDevices.clear();

        allDevices.addAll(tempDevices);

        if (devicesListAdapter == null) {

            ListView devicesList = new ListView(this);
            devicesList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            devicesListAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, allDevices);
            devicesList.setAdapter(devicesListAdapter);
            //Create sequence of items
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Paired/Unpaired BT Devices");
            dialogBuilder.setView(devicesList);
            //Create alert dialog object via builder
            final AlertDialog alertDialogObject = dialogBuilder.create();
            devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    deviceToConnect = devices.get(position);
                    devicesListAdapter = null;
                    alertDialogObject.dismiss();
                    Log.i(TAG, "Connecting to device :" + deviceToConnect.getName());
                    showMessage("Connecting to device " + deviceToConnect.getName());

                    //Now this is not the server...
                    killServerThread();

                    connectAsClient();
                }
            });
            //Show the dialog
            alertDialogObject.show();
            alertDialogObject.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    devicesListAdapter = null;
                }
            });
        } else {
            devicesListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        sendMessageToDevice();
    }





}

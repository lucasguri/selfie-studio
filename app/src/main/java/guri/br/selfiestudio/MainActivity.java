package guri.br.selfiestudio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    //Declaring variables
    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("37877b80-ef9c-11e4-b80c-0800200c9a66");
    private BluetoothAdapter mBluetoothAdapter;
    public Button turnOn, turnOff, setVisible, discovery, showPairedDevices, btnCamera, btnRemoteCamera;
    public ListView listView;
    private ArrayList<BluetoothDevice> devices;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter mArrayAdapter;
    private BroadcastReceiver mReceiver;
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    protected static final int REMOTE_CAMERA = -1;
    private static final String TAG = "debugging";
    private ConnectedThread connectedThread;
    private static final String ACTIVE_CAMERA = "1";
    private AcceptThread acceptThread;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            Log.i(TAG, "in handler");
            super.handleMessage(msg);
            String s = "";
            switch(msg.what){
                case SUCCESS_CONNECT:
                    // DO something
                    Toast.makeText(getApplicationContext(), "CONNECT", Toast.LENGTH_SHORT).show();
                    s = "successfully connected";
                    connectedThread.write(s.getBytes());
                    Log.i(TAG, "connected");
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String string = new String(readBuf);
                    Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
                    startCamera();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initiate variables
        init();

        startAcceptThread();

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(intent);
            }
        });
        //Sets up bluetooth.
        turnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnBt();
            }
        });

        //Turns off bluetooth.
        turnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOffBt();
            }
        });

        //Show paired devices in the list view.
        showPairedDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPairedDevices();
            }
        });

        discovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDiscoveredDevices();
            }
        });

        //Enable visibility.
        setVisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBluetoothVisible();
            }
        });

        btnRemoteCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.obtainMessage(REMOTE_CAMERA).sendToTarget();

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = devices.get(position);
                ConnectThread connect = new ConnectThread(selectedDevice);
                connect.start();
                Toast.makeText(getApplicationContext(), selectedDevice.toString() , Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void setBluetoothVisible() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    private void showDiscoveredDevices() {
        if (mBluetoothAdapter.isEnabled()){
            devices.clear();
            mArrayAdapter.clear();
            if (!mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.startDiscovery();
            Toast.makeText(getApplicationContext(), "Searching for devices...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Turn the bluetooth on to discover devices.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPairedDevices() {
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (mBluetoothAdapter.isEnabled()){
            devices.clear();
            mArrayAdapter.clear();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    devices.add(device);
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else {
                Toast.makeText(getApplicationContext(), "There are no paired devices", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is off", Toast.LENGTH_SHORT).show();
        }
    }

    private void startAcceptThread() {
        if (mBluetoothAdapter.isEnabled()){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    private void turnOffBt() {
        if (mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "Bluetooth off", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is off", Toast.LENGTH_SHORT).show();
        }
    }

    private void turnOnBt() {
        if (mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is already turned on", Toast.LENGTH_SHORT).show();
        }else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        while (true){
            if (mBluetoothAdapter.isEnabled()){
                startAcceptThread();
                break;
            }
        }
    }

    /**
     * Initiate variables.
     */
    private void init(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        turnOn = (Button) findViewById(R.id.on_button);
        turnOff = (Button) findViewById(R.id.off_button);
        setVisible = (Button) findViewById(R.id.visibility_button);
        discovery = (Button) findViewById(R.id.discovery_button);
        btnCamera = (Button) findViewById(R.id.btn_camera);
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        showPairedDevices = (Button) findViewById(R.id.paired_devices_button);
        devices = new ArrayList<BluetoothDevice>();
        listView = (ListView) findViewById(R.id.devices_list_view);
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mArrayAdapter);
        btnRemoteCamera = (Button) findViewById(R.id.remote_camera);

        // Create a BroadcastReceiver for ACTION_FOUND
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //Add the device to an array of devices.
                    devices.add(device);
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    // run some code
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    // run some code
                }
                else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if(mBluetoothAdapter.getState() == mBluetoothAdapter.STATE_OFF){
                        turnOnBt();
                    }
                }
            }
        };
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    private class AcceptThread extends Thread {
        public static final String NAME = "SelfieStudio";
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                    Log.i(TAG, "connection accepted");
                } catch (IOException e) {
                    Log.i(TAG, e.getMessage());
                    Log.i(TAG, e.getMessage());
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                        // TODO

                        Log.i(TAG, "mmServerSocket closed");
                    } catch (IOException e) {
                        Log.i(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        private void manageConnectedSocket(BluetoothSocket socket) {
            ConnectedThread connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
                Log.i(TAG, "mmServerSocket closed");
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
            connectedThread = new ConnectedThread(mmSocket);
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
                Log.i(TAG, "mmSocket.closed");
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket) {
        ConnectedThread connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
        connectedThread.checkAccess();

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void startCamera() {
        //Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        //startActivity(intent);
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode){
            case  RESULT_OK:
                break;
            case RESULT_CANCELED:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Unregister the mReceiver.
        unregisterReceiver(mReceiver);
    }

}
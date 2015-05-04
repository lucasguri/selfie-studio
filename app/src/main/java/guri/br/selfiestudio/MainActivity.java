package guri.br.selfiestudio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity {

    //Declaring variables
    private static final int REQUEST_ENABLE_BT = 1;

    public static BluetoothAdapter mBluetoothAdapter;
    public Button turnOn, turnOff, setVisible, discovery, showPairedDevices, btnCamera;
    public ListView listView;
    private ArrayList<BluetoothDevice> devices;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter mArrayAdapter;
    private BroadcastReceiver mReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initiate variables
        init();

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
                if (mBluetoothAdapter.isEnabled()){
                    mArrayAdapter.clear();
                    // If there are paired devices
                    if (pairedDevices.size() > 0) {
                        // Loop through paired devices
                        for (BluetoothDevice device : pairedDevices) {
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
        });

        discovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isEnabled()){
                    mArrayAdapter.clear();
                    if (!mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.startDiscovery();
                    Toast.makeText(getApplicationContext(), "Searching for devices...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Turn the bluetooth on to discover devices.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Enable visibility.
        setVisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.cancelDiscovery();
                }
                BluetoothDevice selectedDevice = devices.get(position);
                ConnectThread connect = new ConnectThread(selectedDevice);
                connect.start();
                Toast.makeText(getApplicationContext(), selectedDevice.toString() , Toast.LENGTH_SHORT).show();
            }
        });
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



    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Unregister the mReceiver.
        unregisterReceiver(mReceiver);
    }

}
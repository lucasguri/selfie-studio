package guri.br.selfiestudio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter;
    Button turnOn, turnOff, setVisible, discovery, showPairedDevices;
    ListView listView;
    ArrayList<BluetoothDevice> devices;
    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter mArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initiate variables
        init();

        //Sets up bluetooth.
        turnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(),"Bluetooth is already turned on", Toast.LENGTH_SHORT).show();
                }else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        });

        //Turns off bluetooth.
        turnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isEnabled()){
                    mBluetoothAdapter.disable();
                    Toast.makeText(getApplicationContext(), "Bluetooth off", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth is off", Toast.LENGTH_SHORT).show();
                }
            }
        });

        showPairedDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isEnabled()){
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
    }

    private void init(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        turnOn = (Button) findViewById(R.id.on_button);
        turnOff = (Button) findViewById(R.id.off_button);
        showPairedDevices = (Button) findViewById(R.id.paired_devices_button);
        setVisible = (Button) findViewById(R.id.visibility_button);
        devices = new ArrayList<BluetoothDevice>();
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        listView = (ListView) findViewById(R.id.devices_list_view);
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mArrayAdapter);
    }
}
package com.example.whl_1108;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private String UUID_READ = "19B10001-E8F2-537E-4F6C-D104768A1214";
    private String UUID_WRITE = "19B10002-E8F2-537E-4F6C-D104768A1214";
    private final static int REQUEST_ENABLE_BT = 1;
    public final static int SCAN_PERIOD= 5000;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    ArrayList<BluetoothDevice> device_lists = new ArrayList<BluetoothDevice>();
    private boolean connected_= false;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    Button DisconnectButton;
    TextView tv_status_;
    ListView device_list_;
    private ArrayAdapter<String> bt_ArrayAdapter;
    private BluetoothGatt ble_gatt_;
    private Handler scan_handler;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public Map<String, String> uuids = new HashMap<String, String>();





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_status_ = (TextView) findViewById(R.id.tv_status);

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });

        DisconnectButton = (Button) findViewById(R.id.DisconnectBtn);
        DisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_status_.setText("Disconnecting from device\n");
                ble_gatt_.disconnect();
            }
        });

        bt_ArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        device_list_ = (ListView) findViewById(R.id.device_list);
        device_list_.setAdapter(bt_ArrayAdapter);
        device_list_.setOnItemClickListener(DeviceClickListener);


        stopScanningButton.setVisibility(View.INVISIBLE);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();


        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            bt_ArrayAdapter.add(result.getDevice().getName() + "\n" + result.getDevice().getAddress());
            device_lists.add(result.getDevice());

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");

        scan_handler= new Handler();
        scan_handler.postDelayed( this::stopScanning, SCAN_PERIOD );

        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        tv_status_.setText("Stop Scanning...");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    private AdapterView.OnItemClickListener DeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(!btAdapter.isEnabled()){
                Toast.makeText(getBaseContext(), "Bluetooth unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            connectDevice(device);
        }
    };

    private void connectDevice( BluetoothDevice _device ) {
        // update the status
        tv_status_.setText( "Connecting to " + _device.getName() );
        GattClientCallback gatt_client_cb= new GattClientCallback();
        ble_gatt_= _device.connectGatt( this, false, gatt_client_cb );
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == BluetoothGatt.GATT_FAILURE){
                disconnectGattServer();
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED){
                connected_= true;
                gatt.discoverServices();
                tv_status_.setText("Connected...");
                Log.d( TAG, "Connected to the GATT server" );
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                tv_status_.setText("Disconnected...");
                disconnectGattServer();
                connected_= false;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                List<BluetoothGattService> services = gatt.getServices();
                for(BluetoothGattService service : services){
                    for(BluetoothGattCharacteristic characteristic : service.getCharacteristics()){
                        if(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)){
                            gatt.readCharacteristic(characteristic);
                        }
                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)){
                            gatt.setCharacteristicNotification(characteristic, true);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            if(status==BluetoothGatt.GATT_SUCCESS){
                if(onReadValueListener == null) return;
                scan_handler.post(
                        ()->onReadValueListener.onValue(gatt.getDevice(), onReadValueListener.formatter(characteristic))
                );
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if(onNotifyValueListener == null) return;
            scan_handler.post(()->onNotifyValueListener.onValue(gatt.getDevice(), onNotifyValueListener.formatter(characteristic))
            );
        }
    }

    public void disconnectGattServer() {
        Log.d(TAG, "Closing Gatt connection");
        // reset the connection flag
        connected_ = false;
        // disconnect and close the gatt
        if (ble_gatt_ != null) {
            ble_gatt_.disconnect();
            ble_gatt_.close();
        }
    }
    public boolean hasProperty(BluetoothGattCharacteristic characteristic, int property)
    {
        int prop = characteristic.getProperties() & property;
        return prop == property;
    }
    public interface OnNotifyValueListener <T>{
        void onValue(BluetoothDevice deivce, T value);
        T formatter(BluetoothGattCharacteristic characteristic);
    }

    public interface OnReadValueListener <T>{
        void onValue(BluetoothDevice deivce, T value);
        T formatter(BluetoothGattCharacteristic characteristic);
    }

    private OnNotifyValueListener onNotifyValueListener = null;
    public MainActivity setOnNotifyValueListener(OnNotifyValueListener onNotifyValueListener) {
        this.onNotifyValueListener = onNotifyValueListener;
        return this;
    }


    private OnReadValueListener onReadValueListener = null;
    public MainActivity setOnReadValueListener(OnReadValueListener onReadValueListener) {
        this.onReadValueListener = onReadValueListener;
        return this;
    }

}
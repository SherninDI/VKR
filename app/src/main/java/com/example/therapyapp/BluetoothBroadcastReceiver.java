package com.example.therapyapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    private static String TAG = "BroadcastReceiver";
    private final BluetoothDiscoveryDeviceListener listener;
    private final Context context;


    public BluetoothBroadcastReceiver(Context context, BluetoothDiscoveryDeviceListener listener, BluetoothHandler bluetoothHandler) {
        this.listener = listener;
        this.context = context;
        this.listener.setBluetoothController(bluetoothHandler);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case BluetoothDevice.ACTION_FOUND :
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Device discovered! " + BluetoothHandler.deviceToString(device));
                listener.onDeviceDiscovered(device);
                break;

            case BluetoothAdapter.ACTION_DISCOVERY_STARTED :
                Log.e(TAG,"Start ");
                // BluetoothDeviceListProvider.this.finish();
                break;

            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                listener.onDeviceDiscoveryEnd();
                Log.e(TAG,"Finish ");
                // BluetoothDeviceListProvider.this.finish();
                break;

            case BluetoothAdapter.ACTION_STATE_CHANGED:
                listener.onBluetoothStatusChanged();
                Log.e(TAG,"state changed ");
                break;

            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                listener.onDevicePairingEnded();
                Log.e(TAG,"bond changed ");
                break;
            default:

                break;
        }

    }
    // Called when device discovery starts.
    public void onDeviceDiscoveryStarted() {
        listener.onDeviceDiscoveryStarted();
    }

    //Called when device discovery ends.
    public void onDeviceDiscoveryEnd() {
        listener.onDeviceDiscoveryEnd();
    }

    //Called when the Bluetooth has been enabled.
    public void onBluetoothTurningOn() {
        listener.onBluetoothTurningOn();
    }


    public void close() {
        context.unregisterReceiver(this);
    }
}

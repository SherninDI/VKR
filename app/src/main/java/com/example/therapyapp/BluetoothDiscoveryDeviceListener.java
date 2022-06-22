package com.example.therapyapp;

import android.bluetooth.BluetoothDevice;

public interface BluetoothDiscoveryDeviceListener {

    void onDeviceDiscovered(BluetoothDevice device);

    void onDeviceDiscoveryStarted();

    void setBluetoothPairingHandler(BluetoothPairingHandler bluetooth);

    void onDeviceDiscoveryEnd();

    void onBluetoothStatusChanged();

    void onBluetoothTurningOn();

    void onDevicePairingEnded();
}

package com.example.therapyapp;

import android.bluetooth.BluetoothDevice;

public interface BluetoothDiscoveryDeviceListener {
    /**
     * Called when a new device has been found.
     *
     * @param device the device found.
     */
    void onDeviceDiscovered(BluetoothDevice device);

    /**
     * Called when device discovery starts.
     */
    void onDeviceDiscoveryStarted();

    /**
     * Called on creation to inject a {@link BluetoothHandler} component to handle Bluetooth.
     *
     * @param bluetooth the controller for the Bluetooth.
     */
    void setBluetoothController(BluetoothHandler bluetooth);

    /**
     * Called when discovery ends.
     */
    void onDeviceDiscoveryEnd();

    /**
     * Called when the Bluetooth status changes.
     */
    void onBluetoothStatusChanged();

    /**
     * Called when the Bluetooth has been enabled.
     */
    void onBluetoothTurningOn();

    /**
     * Called when a device pairing ends.
     */
    void onDevicePairingEnded();
}

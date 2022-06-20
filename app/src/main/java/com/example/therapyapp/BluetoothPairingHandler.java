package com.example.therapyapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.Closeable;

public class BluetoothPairingHandler implements Closeable {
    //Tag string used for logging.
    private static final String TAG = "BluetoothPairingHandler";

    //Interface for Bluetooth OS services
    private final BluetoothAdapter bluetoothAdapter;

    //Class used to handle communication with OS about Bluetooth system events.
    private final BluetoothBroadcastReceiver bluetoothBroadcastReceiver;

    // The activity which is using this controller.
    private final Activity context;

    // Used as a simple way of synchronization between turning on the Bluetooth and starting a device discovery.
    private boolean bluetoothDiscoveryScheduled;

    /*** Used as a temporary field for the currently bounding device. This field makes this whole* class not Thread Safe.*/
    private BluetoothDevice boundingDevice;

    /**
     * Instantiates a new BluetoothPairingHandler.
     *
     * @param context  the activity which is using this controller.
     * @param listener a callback for handling Bluetooth events.
     */
    public BluetoothPairingHandler(Activity context, BluetoothAdapter adapter, BluetoothDiscoveryDeviceListener listener) {
        this.context = context;
        this.bluetoothAdapter = adapter;
        this.bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(context, listener, this);
    }



    /**
     * Checks if the Bluetooth is already enabled on this device.
     *
     * @return true if the Bluetooth is on, false otherwise.
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Starts the discovery of new Bluetooth devices nearby.
     */
    public void startDiscovery() {
        bluetoothBroadcastReceiver.onDeviceDiscoveryStarted();

        // This line of code is very important. In Android >= 6.0 you have to ask for the runtime
        // permission as well in order for the discovery to get the devices ids. If you don't do
        // this, the discovery won't find any device.
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != 0) {

                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }

        // If another discovery is in progress, cancels it before starting the new one.
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Tries to start the discovery. If the discovery returns false, this means that the
        // bluetooth has not started yet.
        Log.d(TAG, "Bluetooth starting discovery.");
        if (!bluetoothAdapter.startDiscovery()) {
            Toast.makeText(context, "Error while starting device discovery!", Toast.LENGTH_SHORT)
                    .show();
            Log.d(TAG, "StartDiscovery returned false. Maybe Bluetooth isn't on?");

            // Ends the discovery.
            bluetoothBroadcastReceiver.onDeviceDiscoveryEnd();
        }
    }

    /**
     * Turns on the Bluetooth.
     */
    public void turnOnBluetooth() {
        Log.d(TAG, "Enabling Bluetooth.");
        bluetoothBroadcastReceiver.onBluetoothTurningOn();
        bluetoothAdapter.enable();
    }

    /**
     * Performs the device pairing.
     *
     * @param device the device to pair with.
     * @return true if the pairing was successful, false otherwise.
     */
    public boolean pair(BluetoothDevice device) {
        // Stops the discovery and then creates the pairing.
        if (bluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "Bluetooth cancelling discovery.");
            bluetoothAdapter.cancelDiscovery();
        }
        Log.d(TAG, "Bluetooth bonding with device: " + deviceToString(device));
        boolean outcome = device.createBond();
        Log.d(TAG, "Bounding outcome : " + outcome);

        // If the outcome is true, we are bounding with this device.
        if (outcome) {
            this.boundingDevice = device;
        }
        return outcome;
    }

    /**
     * Checks if a device is already paired.
     *
     * @param device the device to check.
     * @return true if it is already paired, false otherwise.
     */
    public boolean isAlreadyPaired(BluetoothDevice device) {
        return bluetoothAdapter.getBondedDevices().contains(device);
    }

    /**
     * Converts a BluetoothDevice to its String representation.
     *
     * @param device the device to convert to String.
     * @return a String representation of the device.
     */
    public static String deviceToString(BluetoothDevice device) {
        return "[Address: " + device.getAddress() + ", Name: " + device.getName() + "]";
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void close() {
        this.bluetoothBroadcastReceiver.close();
    }

    /**
     * Checks if a deviceDiscovery is currently running.
     *
     * @return true if a deviceDiscovery is currently running, false otherwise.
     */
    public boolean isDiscovering() {
        return bluetoothAdapter.isDiscovering();
    }

    /**
     * Cancels a device discovery.
     */
    public void cancelDiscovery() {
        if(bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
            bluetoothBroadcastReceiver.onDeviceDiscoveryEnd();
        }
    }

    /**
     * Turns on the Bluetooth and executes a device discovery when the Bluetooth has turned on.
     */
    public void turnOnBluetoothAndScheduleDiscovery() {
        this.bluetoothDiscoveryScheduled = true;
        turnOnBluetooth();
    }

    /**
     * Called when the Bluetooth status changed.
     */
    public void onBluetoothStatusChanged() {
        // Does anything only if a device discovery has been scheduled.
        if (bluetoothDiscoveryScheduled) {

            int bluetoothState = bluetoothAdapter.getState();
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_ON:
                    // Bluetooth is ON.
                    Log.d(TAG, "Bluetooth succesfully enabled, starting discovery");
                    startDiscovery();
                    // Resets the flag since this discovery has been performed.
                    bluetoothDiscoveryScheduled = false;
                    break;
                case BluetoothAdapter.STATE_OFF:
                    // Bluetooth is OFF.
                    Log.d(TAG, "Error while turning Bluetooth on.");
                    Toast.makeText(context, "Error while turning Bluetooth on.", Toast.LENGTH_SHORT);
                    // Resets the flag since this discovery has been performed.
                    bluetoothDiscoveryScheduled = false;
                    break;
                default:
                    // Bluetooth is turning ON or OFF. Ignore.
                    break;
            }
        }
    }

    /**
     * Returns the status of the current pairing and cleans up the state if the pairing is done.
     *
     * @return the current pairing status.
     * @see BluetoothDevice#getBondState()
     */
    public int getPairingDeviceStatus() {
        if (this.boundingDevice == null) {
            throw new IllegalStateException("No device currently bounding");
        }
        int bondState = this.boundingDevice.getBondState();
        // If the new state is not BOND_BONDING, the pairing is finished, cleans up the state.
        if (bondState != BluetoothDevice.BOND_BONDING) {
            this.boundingDevice = null;
        }
        return bondState;
    }

    /**
     * Gets the name of the currently pairing device.
     *
     * @return the name of the currently pairing device.
     */


    /**
     * Gets the name of a device. If the device name is not available, returns the device address.
     *
     * @param device the device whose name to return.
     * @return the name of the device or its address if the name is not available.
     */
    public static String getDeviceName(BluetoothDevice device) {
        String deviceName = device.getName();
        if (deviceName == null) {
            deviceName = device.getAddress();
        }
        return deviceName;
    }

    /**
     * Returns if there's a pairing currently being done through this app.
     *
     * @return true if a pairing is in progress through this app, false otherwise.
     */
    public boolean isPairingInProgress() {
        return this.boundingDevice != null;
    }

    /**
     * Gets the currently bounding device.
     *
     * @return the {@link #boundingDevice}.
     */
    public BluetoothDevice getBoundingDevice() {
        return boundingDevice;
    }

}

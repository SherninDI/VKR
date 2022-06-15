package com.example.therapyapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;


public class BluetoothConnectHandler {
    private static final String TAG = "BluetoothConnectHandler";
    private static final boolean D = true;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler bluetoothHandler;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int bluetoothState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private boolean AllowInsecure;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothConnectHandler(Handler handler, BluetoothAdapter adapter) {
        bluetoothAdapter = adapter;
        bluetoothState = STATE_NONE;
        bluetoothHandler = handler;
        AllowInsecure = true;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + bluetoothState + " -> " + state);
        bluetoothState = state;

        // Give the new state to the Handler so the UI Activity can update
        bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return bluetoothState;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (bluetoothState == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel(); connectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel(); connectedThread = null;}

        // Start the thread to connect with the given device
        try {
            connectThread = new ConnectThread(device);
            connectThread.start();
            setState(STATE_CONNECTING);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel(); connectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(GroupFragment.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        bluetoothHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (connectedThread != null) connectedThread.shutdown();
        if (connectThread != null) {
            connectThread.cancel(); connectThread = null;}
        if (connectedThread != null) {
            connectedThread.cancel(); connectedThread = null;}
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (bluetoothState != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GroupFragment.TOAST, "Unable to connect device");
        msg.setData(bundle);
        bluetoothHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GroupFragment.TOAST, "Device connection was lost");
        msg.setData(bundle);
        bluetoothHandler.sendMessage(msg);
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice device) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            bluetoothDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
//                Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
//                tmp = (BluetoothSocket) m.invoke(device, 1);
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            bluetoothSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                bluetoothSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                connectionFailed();
                // Close the socket
                try {
                    bluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnectHandler.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(bluetoothSocket, bluetoothDevice);
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        private boolean stop = false;
        private boolean hasReadAnything = false;

        public void shutdown() {
            stop = true;
            if (! hasReadAnything) return;
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of InputStream failed.");
                }
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            int bytes;
            int availableBytes = 0;
            while (!stop) {
                try {

                    availableBytes = inputStream.available();
                    if (availableBytes > 0) {
                        byte[] buffer = new byte[availableBytes];
                        bytes = inputStream.read(buffer);

                        if (bytes > 0) {
                            bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                        }
                    }

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                // Share the sent message back to the UI Activity
                bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}

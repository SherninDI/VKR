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

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler bluetoothHandler;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int bluetoothState;

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private boolean AllowInsecure;

    public BluetoothConnectHandler(Handler handler, BluetoothAdapter adapter) {
        bluetoothAdapter = adapter;
        bluetoothState = STATE_NONE;
        bluetoothHandler = handler;
        AllowInsecure = true;
    }

    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + bluetoothState + " -> " + state);
        bluetoothState = state;

        // Give the new state to the Handler so the UI Activity can update
        bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return bluetoothState;
    }

    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        if (bluetoothState == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel(); connectThread = null;}
        }

        if (connectedThread != null) {
            connectedThread.cancel(); connectedThread = null;}

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

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");


        if (connectThread != null) {
            connectThread.cancel(); connectThread = null;}

        if (connectedThread != null) {
            connectedThread.cancel(); connectedThread = null;}

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message msg = bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(GroupFragment.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        bluetoothHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (connectedThread != null) connectedThread.shutdown();
        if (connectThread != null) {
            connectThread.cancel(); connectThread = null;}
        if (connectedThread != null) {
            connectedThread.cancel(); connectedThread = null;}
        setState(STATE_NONE);
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (bluetoothState != STATE_CONNECTED) return;
            r = connectedThread;
        }
        r.write(out);
    }


    private void connectionFailed() {
        setState(STATE_NONE);
        Message msg = bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GroupFragment.TOAST, "Unable to connect device");
        msg.setData(bundle);
        bluetoothHandler.sendMessage(msg);
    }

    private void connectionLost() {
        setState(STATE_NONE);

        Message msg = bluetoothHandler.obtainMessage(GroupFragment.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GroupFragment.TOAST, "Device connection was lost");
        msg.setData(bundle);
        bluetoothHandler.sendMessage(msg);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice device) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            bluetoothDevice = device;
            BluetoothSocket tmp = null;


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
            bluetoothAdapter.cancelDiscovery();

            try {
                bluetoothSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                connectionFailed();
                try {
                    bluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            synchronized (BluetoothConnectHandler.this) {
                connectThread = null;
            }

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

    private class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

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

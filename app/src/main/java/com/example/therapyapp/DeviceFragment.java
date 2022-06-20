package com.example.therapyapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import com.example.therapyapp.databinding.FragmentDeviceBinding;

public class DeviceFragment extends Fragment implements DeviceListInteractionListener<BluetoothDevice> {
    private static String TAG = "Device Fragment";
    private FragmentDeviceBinding binding;

    private FloatingActionButton fab;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothPairingHandler bluetoothPairingHandler;
    private DeviceRecyclerViewAdapter deviceListAdapter;
    private RecyclerViewSupport rvDevices;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentDeviceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fab = getActivity().findViewById(R.id.fab);

        boolean hasBluetooth = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (!hasBluetooth) {
            AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
            dialog.setTitle("Ошибка");
            dialog.setMessage("Устройство не поддерживает Bluetooth");
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Closes the dialog and terminates the activity.
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    });
            dialog.setCancelable(false);
            dialog.show();
        }

        deviceListAdapter = new DeviceRecyclerViewAdapter(this);
        rvDevices = getView().findViewById(R.id.devices_list);
        rvDevices.setLayoutManager(new LinearLayoutManager(getActivity()));

        View emptyView = getView().findViewById(R.id.empty_list);
        rvDevices.setEmptyView(emptyView);

        BluetoothManager bluetoothManager = (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        rvDevices.setAdapter(deviceListAdapter);
        bluetoothPairingHandler = new BluetoothPairingHandler(getActivity(), bluetoothAdapter, deviceListAdapter);

        fab.setImageResource(R.drawable.ic_bluetooth_white_24dp);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!bluetoothPairingHandler.isBluetoothEnabled()){
                    bluetoothPairingHandler.turnOnBluetoothAndScheduleDiscovery();
                } else {
                    if (!bluetoothPairingHandler.isDiscovering()) {
                        bluetoothPairingHandler.startDiscovery();
                        Snackbar.make(view, "Поиск устройств...", Snackbar.LENGTH_LONG)
                                .show();
                    } else {
                        bluetoothPairingHandler.cancelDiscovery();
                        Snackbar.make(view, "Поиск устройств остановлен", Snackbar.LENGTH_LONG)
                                    .show();
                    }
                }
            }
        });

    }

    @Override
    public void onItemClick(BluetoothDevice device) {

        if (bluetoothPairingHandler.isAlreadyPaired(device)) {
            Log.d(TAG, "Device already paired!");
//            Toast.makeText(getActivity(), R.string.device_already_paired, Toast.LENGTH_SHORT).show();

            if (device != null) {
                String address = device.getAddress();
                Log.e(TAG, address);
                Intent frag_to_act = new Intent(getActivity().getBaseContext(), MainActivity.class);
                frag_to_act.putExtra("device_address", address);
                getActivity().startActivity(frag_to_act);

//                Intent intent = new Intent(getActivity().getBaseContext(), DataActivity.class);
//                getActivity().startActivity(intent);
            }

        } else {
            Log.d(TAG, "Device not paired. Pairing.");
            boolean outcome = bluetoothPairingHandler.pair(device);

            // Prints a message to the user.
            String deviceName = BluetoothPairingHandler.getDeviceName(device);
            if (outcome) {
                // The pairing has started, shows a progress dialog.
                Log.d(TAG, "Showing pairing dialog");
//                bondingProgressDialog = ProgressDialog.show(this, "", "Pairing with device " + deviceName + "...", true, false);
            } else {
                Log.d(TAG, "Error while pairing with device " + deviceName + "!");
//                Toast.makeText(this, "Error while pairing with device " + deviceName + "!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void startLoading() {
        rvDevices.startLoading();
        fab.setImageResource(R.drawable.ic_bluetooth_searching_white_24dp);
    }

    @Override
    public void endLoading(boolean partialResults) {
        rvDevices.endLoading();
        if (!partialResults) {
            fab.setImageResource(R.drawable.ic_bluetooth_white_24dp);
        }
    }

    @Override
    public void endLoadingWithDialog(boolean error, BluetoothDevice device) {
        String deviceName = BluetoothPairingHandler.getDeviceName(device);
        // Gets the message to print.
        if (error) {
            Log.e(TAG,"Failed pairing with device " + deviceName + "!");
        } else {
           Log.e(TAG,"Succesfully paired with device " + deviceName + "!");
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bluetoothPairingHandler.isDiscovering()) {
           bluetoothPairingHandler.cancelDiscovery();
        }
        binding = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG,"stop");
        // Stoops the discovery.
        if (this.bluetoothPairingHandler != null) {
            this.bluetoothPairingHandler.cancelDiscovery();
        }
    }

    @Override
    public void onDestroy() {
        bluetoothPairingHandler.close();
        super.onDestroy();
        binding = null;
    }

}
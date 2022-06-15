package com.example.therapyapp;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DeviceRecyclerViewAdapter extends RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder> implements BluetoothDiscoveryDeviceListener {
    private final List<BluetoothDevice> devices;
    private final DeviceListInteractionListener<BluetoothDevice> listener;
    private BluetoothHandler bluetoothHandler;

    public DeviceRecyclerViewAdapter(DeviceListInteractionListener<BluetoothDevice> listener) {
        this.devices = new ArrayList<>();

        this.listener = listener;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_device, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.itemList = devices.get(position);
        holder.itemImageView.setImageResource(getDeviceIcon(devices.get(position)));
        holder.itemDeviceName.setText(devices.get(position).getName());
        holder.itemDeviceAddress.setText(devices.get(position).getAddress());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(holder.itemList);
                }
            }
        });

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return devices.size();
    }

    /**
     * Returns the icon shown on the left of the device inside the list.
     *
     * @param device the device for the icon to get.
     * @return a resource drawable id for the device icon.
     */
    private int getDeviceIcon(BluetoothDevice device) {
        if (bluetoothHandler.isAlreadyPaired(device)) {
            return R.drawable.ic_bluetooth_connected_black_24dp;
        } else {
            return R.drawable.ic_bluetooth_black_24dp;
        }
    }

    @Override
    public void onDeviceDiscovered(BluetoothDevice device) {
        listener.endLoading(true);
        devices.add(device);
        notifyDataSetChanged();
    }

    @Override
    public void onDeviceDiscoveryStarted() {
        cleanView();
        listener.startLoading();
    }


    public void cleanView() {
        devices.clear();
        notifyDataSetChanged();
    }

    @Override
    public void setBluetoothController(BluetoothHandler bluetoothHandler) {
        this.bluetoothHandler = bluetoothHandler;
    }

    @Override
    public void onDeviceDiscoveryEnd() {
        listener.endLoading(false);
    }


    @Override
    public void onBluetoothStatusChanged() {
        // Notifies the Bluetooth controller.
        bluetoothHandler.onBluetoothStatusChanged();
    }

    @Override
    public void onBluetoothTurningOn() {
        listener.startLoading();
    }

    @Override
    public void onDevicePairingEnded() {
        if (bluetoothHandler.isPairingInProgress()) {
            BluetoothDevice device = bluetoothHandler.getBoundingDevice();
            switch (bluetoothHandler.getPairingDeviceStatus()) {
                case BluetoothDevice.BOND_BONDING:
                    // Still pairing, do nothing.
                    break;
                case BluetoothDevice.BOND_BONDED:
                    // Successfully paired.
                    listener.endLoadingWithDialog(false, device);

                    // Updates the icon for this element.
                    notifyDataSetChanged();
                    break;
                case BluetoothDevice.BOND_NONE:
                    // Failed pairing.
                    listener.endLoadingWithDialog(true, device);
                    break;
            }
        }
    }

    // stores and recycles views as they are scrolled off screen
    class ViewHolder extends RecyclerView.ViewHolder {
        final View itemView;
        final ImageView itemImageView;
        final TextView itemDeviceAddress;
        final TextView itemDeviceName;
        BluetoothDevice itemList;

        public ViewHolder(View view) {
            super(view);
            itemView = view;
            itemImageView = itemView.findViewById(R.id.device_icon);
            itemDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            itemDeviceName = itemView.findViewById(R.id.tvDeviceName);
        }

    }
}

package com.example.therapyapp;

import android.app.Application;

public class Globals extends Application {
    private String deviceAddress;

    public String getGlobalDeviceAddress() {
        return deviceAddress;
    }

    public void setGlobalDeviceAddress(String address) {
        deviceAddress = address;
    }
}

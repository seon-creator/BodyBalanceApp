package com.example.myapplication;

import android.app.Application;
import android.bluetooth.BluetoothSocket;

public class Bluetooth extends Application {
    private BluetoothSocket bluetoothSocket;

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }

    public boolean isBluetoothConnected() {
        return bluetoothSocket != null && bluetoothSocket.isConnected();
    }
}

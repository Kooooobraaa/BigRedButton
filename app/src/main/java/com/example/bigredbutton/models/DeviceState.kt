package com.example.bigredbutton.models

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket

data class DeviceState(
    val device: BluetoothDevice?,
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val socket: BluetoothSocket? = null,
    val error: String? = null
)


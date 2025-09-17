package com.example.bigredbutton

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothViewModel(app: Application) : AndroidViewModel(app) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter() // Deprecated, but required for classic BT
    val discoveredDevices = mutableStateListOf<BluetoothDevice>()
    val deviceConnections = mutableStateMapOf<Int, BluetoothSocket?>() // slot index to socket
    var isDiscovering by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val deviceNamePrefix = BLUETOOTH_DEVICE_NAME
    private var receiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = try {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } catch (e: Exception) { null }
                    val name = try { device?.name } catch (e: SecurityException) { null }
                    if (device != null && name?.startsWith(deviceNamePrefix) == true) {
                        if (discoveredDevices.none { it.address == device.address }) {
                            discoveredDevices.add(device)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isDiscovering = false
                }
            }
        }
    }

    fun startDiscovery() {
        discoveredDevices.clear()
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            getApplication<Application>().registerReceiver(discoveryReceiver, filter)
            receiverRegistered = true
        }
        isDiscovering = try { bluetoothAdapter?.startDiscovery() ?: false } catch (e: SecurityException) { errorMessage = e.message; false }
    }

    fun stopDiscovery() {
        try { bluetoothAdapter?.cancelDiscovery() } catch (_: SecurityException) {}
        isDiscovering = false
    }

    fun connectToDevice(slot: Int, device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stopDiscovery()
                val uuid = try { device.uuids?.firstOrNull()?.uuid ?: SPP_UUID } catch (e: SecurityException) { SPP_UUID }
                val socket = try { device.createRfcommSocketToServiceRecord(uuid) } catch (e: SecurityException) { null }
                // Try to connect, handle SecurityException explicitly
                try {
                    socket?.connect()
                } catch (se: SecurityException) {
                    errorMessage = "Missing Bluetooth permission: ${se.localizedMessage}"
                    return@launch
                }
                deviceConnections[slot]?.close()
                deviceConnections[slot] = socket
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Connection failed: ${e.localizedMessage}"
            }
        }
    }

    fun disconnectFromSlot(slot: Int) {
        try { deviceConnections[slot]?.close() } catch (_: Exception) {}
        deviceConnections.remove(slot)
    }

    override fun onCleared() {
        super.onCleared()
        if (receiverRegistered) {
            getApplication<Application>().unregisterReceiver(discoveryReceiver)
            receiverRegistered = false
        }
        deviceConnections.values.toList().forEach { socket -> try { socket?.close() } catch (_: Exception) {} }
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

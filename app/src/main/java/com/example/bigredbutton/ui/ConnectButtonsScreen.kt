package com.example.bigredbutton.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bigredbutton.BluetoothMessageManager
import com.example.bigredbutton.models.DeviceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectButtonsScreen(
    onNavigateBack: () -> Unit,
    onOpenBluetoothSettings: (Context) -> Unit,
    modifier: Modifier = Modifier,
    deviceStates: List<DeviceState>? = null,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()
    val bluetoothPermissionGranted = if (!isInPreview) {
        if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    } else false
    val bluetoothManager = if (!isInPreview) context.getSystemService(BluetoothManager::class.java) else null
    val bluetoothAdapter = if (!isInPreview && bluetoothPermissionGranted) bluetoothManager?.adapter else null
    val pairedDevices = if (!isInPreview) {
        remember {
            bluetoothAdapter?.bondedDevices?.filter {
                it.name == "BigRedB_1" || it.name == "BigRedB_2" || it.name == "BigRedB_3"
            } ?: emptyList()
        }
    } else emptyList<BluetoothDevice>()
    val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun setError(name: String, error: String) {
        updateDeviceState(name) { it.copy(isConnecting = false,
            isConnected = false,
            error = error,
            socket = null) }
    }

    suspend fun connectToDevice(device: BluetoothDevice, name: String) {
        updateDeviceState(name) { it.copy(isConnecting = true, error = null) }
        withContext(Dispatchers.IO) {
            try {
                val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
                withContext(Dispatchers.Main) {
                    updateDeviceState(name) { it.copy(isConnecting = false,
                        isConnected = true,
                        socket = socket,
                        error = null) }
                    // Start listening for messages from this device
                    BluetoothMessageManager.listenForMessages(socket)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    setError(name, "Connection failed: ${e.localizedMessage}")
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun disconnectFromDevice(device: BluetoothDevice, name: String) {
        // device parameter is unused, but kept for signature compatibility
        updateDeviceState(name) { it.copy(isConnecting = true, error = null) }
        withContext(Dispatchers.IO) {
            try {
                val idx = deviceStates?.indexOfFirst { it.name == name } ?: -1
                val socket = deviceStates?.getOrNull(idx)?.socket
                socket?.close()
                withContext(Dispatchers.Main) {
                    updateDeviceState(name) { it.copy(isConnecting = false,
                        isConnected = false,
                        socket = null,
                        error = null) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setError(name, "Disconnect failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Buttons") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    deviceStates?.forEach { state ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                            Text(state.name)
                            StatusIcon(isConnected = state.isConnected)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.device == null) {
                                Text("Not Paired", color = Color.Gray)
                                Button(onClick = {}, enabled = false) {
                                    Text("Connect")
                                }
                            } else if (state.isConnected) {
                                Button(onClick = { coroutineScope.launch { disconnectFromDevice(state.device, state.name) } }, enabled = !state.isConnecting) {
                                    Text("Disconnect")
                                }
                            } else {
                                Button(onClick = { coroutineScope.launch { connectToDevice(state.device, state.name) } }, enabled = !state.isConnecting) {
                                    Text(if (state.isConnecting) "Connecting..." else "Connect")
                                }
                            }
                            if (state.error != null) {
                                Text(state.error, color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(72.dp))
                Button(onClick = { onOpenBluetoothSettings(context) }) {
                    Text("Open Bluetooth Settings")
                }
            }
        }
    }

    // On entering the screen, update deviceStates with paired device info
    LaunchedEffect(pairedDevices) {
        pairedDevices.forEach { btDevice ->
            updateDeviceState(btDevice.name) { state ->
                if (state.device == null || state.address != btDevice.address) {
                    state.copy(device = btDevice, address = btDevice.address ?: "Unavailable")
                } else state
            }
        }
    }
}


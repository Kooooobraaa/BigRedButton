package com.example.bigredbutton.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import java.util.UUID
import androidx.compose.ui.tooling.preview.Preview
import android.util.Log

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

    // Runtime permission handling for BLUETOOTH_CONNECT and BLUETOOTH_SCAN (Android 12+).
    val initialConnectGranted = if (!isInPreview) {
        if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(context, "android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED
        } else true
    } else false

    val initialScanGranted = if (!isInPreview) {
        if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(context, "android.permission.BLUETOOTH_SCAN") == PackageManager.PERMISSION_GRANTED
        } else true
    } else false

    var bluetoothConnectGranted by remember { mutableStateOf(initialConnectGranted) }
    var bluetoothScanGranted by remember { mutableStateOf(initialScanGranted) }

    // Combined permission state: both required on API 31+
    val bluetoothPermissionsGranted = if (!isInPreview) {
        if (Build.VERSION.SDK_INT >= 31) {
            bluetoothConnectGranted && bluetoothScanGranted
        } else true
    } else false

    // Launcher to request both permissions when needed
    val permissionLauncher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { results: Map<String, Boolean> ->
        bluetoothConnectGranted = results["android.permission.BLUETOOTH_CONNECT"] == true
        bluetoothScanGranted = results["android.permission.BLUETOOTH_SCAN"] == true
        Log.d("ConnectButtons", "Permissions result: CONNECT=${bluetoothConnectGranted}, SCAN=${bluetoothScanGranted}")
    }

    // Bluetooth manager/adapter should be obtained only when not in preview and both permissions are granted
    val bluetoothManager = if (!isInPreview && bluetoothPermissionsGranted) rememberUpdatedState(context.getSystemService(BluetoothManager::class.java)) else rememberUpdatedState<BluetoothManager?>(null)
    val bluetoothAdapter = bluetoothManager.value?.adapter

    // Paired devices — recompute when adapter changes or when permission is granted
    val pairedDevices = if (!isInPreview) {
        remember(bluetoothAdapter, bluetoothPermissionsGranted) {
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
        // Extra runtime guards
        if (Build.VERSION.SDK_INT >= 31 && !bluetoothPermissionsGranted) {
            withContext(Dispatchers.Main) {
                updateDeviceState(name) { it.copy(error = "Missing BLUETOOTH_CONNECT permission") }
            }
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            withContext(Dispatchers.Main) {
                updateDeviceState(name) { it.copy(error = "Bluetooth adapter unavailable or disabled") }
            }
            return
        }

        updateDeviceState(name) { it.copy(isConnecting = true, error = null) }
        withContext(Dispatchers.IO) {
            try {
                val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                bluetoothAdapter.cancelDiscovery()
                socket.connect()
                withContext(Dispatchers.Main) {
                    updateDeviceState(name) { it.copy(isConnecting = false,
                        isConnected = true,
                        socket = socket,
                        error = null) }
                    // Start listening for messages from this device
                    try {
                        BluetoothMessageManager.listenForMessages(socket)
                    } catch (e: Exception) {
                        // If listening fails, record error but keep the connected state
                        Log.e("ConnectButtons", "listenForMessages failed", e)
                        updateDeviceState(name) { it.copy(error = "Listener failed: ${e.localizedMessage}") }
                    }
                }
            } catch (e: Exception) {
                // Catch any exception (including SecurityException) to avoid crashing the app
                Log.e("ConnectButtons", "connectToDevice failed", e)
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
                // If we are missing permission on Android 12+, show a button to request it
                if (Build.VERSION.SDK_INT >= 31 && !isInPreview && !bluetoothPermissionsGranted) {
                    Text("Bluetooth permission required to discover paired devices.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { permissionLauncher.launch(arrayOf("android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN")) }) {
                        Text("Grant Bluetooth Permissions")
                    }
                }

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
                                Button(
                                    onClick = {
                                        // Ensure runtime permission is granted on Android 12+
                                        if (Build.VERSION.SDK_INT >= 31 && !bluetoothPermissionsGranted) {
                                            // Request both permissions first
                                            permissionLauncher.launch(arrayOf("android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"))
                                            updateDeviceState(state.name) { it.copy(error = "Requesting Bluetooth permissions") }
                                            Log.w("ConnectButtons", "Requested BLUETOOTH_CONNECT+SCAN before connecting")
                                        } else {
                                            // device is non-null here (we're in the else branch where state.device != null)
                                            val dev = state.device
                                            coroutineScope.launch {
                                                try {
                                                    connectToDevice(dev, state.name)
                                                } catch (e: Exception) {
                                                    Log.e("ConnectButtons", "connectToDevice threw", e)
                                                    updateDeviceState(state.name) { it.copy(error = "Connect failed: ${e.localizedMessage}") }
                                                }
                                            }
                                        }
                                    },
                                    enabled = !state.isConnecting
                                ) {
                                    Text(if (state.isConnecting) "Connecting..." else "Connect")
                                }
                            }
                            if (state.error != null) {
                                Text(state.error, color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Diagnostic info to help debug why Connect may remain disabled
                Spacer(modifier = Modifier.height(12.dp))
                if (!isInPreview) {
                    val connectText = if (Build.VERSION.SDK_INT >= 31) "BLUETOOTH_CONNECT granted: $bluetoothConnectGranted" else "BLUETOOTH_CONNECT not required for this Android version"
                    val scanText = if (Build.VERSION.SDK_INT >= 31) "BLUETOOTH_SCAN granted: $bluetoothScanGranted" else "BLUETOOTH_SCAN not required for this Android version"
                    Text(connectText, color = Color.Gray, fontSize = 12.sp)
                    Text(scanText, color = Color.Gray, fontSize = 12.sp)

                    if (pairedDevices.isEmpty()) {
                        Text("No paired BigRedB_* devices found.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        val names = pairedDevices.joinToString { it.name ?: it.address ?: "Unknown" }
                        Text("Paired devices: $names", color = Color.Gray, fontSize = 12.sp)
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
    LaunchedEffect(pairedDevices, deviceStates) {
        // If we don't have a deviceStates list, nothing to update
        val states = deviceStates ?: return@LaunchedEffect

        // For each expected state (BigRedB_1..3), try to find a matching paired device
        states.forEach { expectedState ->
            val match = pairedDevices.firstOrNull { pd ->
                // Prefer matching by name (if both have a name), fall back to address
                (pd.name != null && pd.name == expectedState.name) ||
                    (pd.address != null && expectedState.address != "Unavailable" && pd.address == expectedState.address)
            }

            if (match != null) {
                updateDeviceState(expectedState.name) { state ->
                    if (state.device == null || state.address != match.address) {
                        state.copy(device = match, address = match.address ?: "Unavailable")
                    } else state
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectButtonsScreenPreview() {
    val mockDevices = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false),
        DeviceState(device = null, name = "BigRedB_3", address = "Unavailable", isConnected = false)
    )

    com.example.bigredbutton.ui.theme.BigRedButtonTheme {
        ConnectButtonsScreen(
            onNavigateBack = {},
            onOpenBluetoothSettings = {},
            modifier = Modifier,
            deviceStates = mockDevices,
            updateDeviceState = { _, _ -> }
        )
    }
}

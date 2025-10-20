package com.example.bigredbutton.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.bigredbutton.BluetoothMessageManager
import com.example.bigredbutton.models.DeviceState
import com.example.bigredbutton.util.formatMillisecondsToTimeWithMillisTwoDigits

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun StopwatchScreen(onNavigateBack: () -> Unit,
                    modifier: Modifier = Modifier,
                    deviceStates: List<DeviceState>,
                    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit) {
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var timeReference by remember { mutableLongStateOf(0L) }

    var winnerId by remember { mutableStateOf("") }
    val latestBluetoothMessage by BluetoothMessageManager.latestMessage.collectAsState()

    var showOptionsDialog by remember { mutableStateOf(false) }
    var isPreparationEnabled by remember { mutableStateOf(false) }
    var preparationSeconds by remember { mutableIntStateOf(3) }
    var preparationCountdown by remember { mutableIntStateOf(0) }
    var isInPreparationPhase by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    // Debug controls (same pattern as ReflexGameScreen)
    var showDebugControls by remember { mutableStateOf(false) }
    var debugSelectedDevice by remember { mutableStateOf(deviceStates.firstOrNull()?.name ?: "") }
    var debugMenuExpanded by remember { mutableStateOf(false) }

    // Ensure default debug selected device is valid
    LaunchedEffect(deviceStates) {
        if (debugSelectedDevice.isEmpty() && deviceStates.isNotEmpty()) debugSelectedDevice = deviceStates.first().name
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            timeReference = if (elapsedTime == 0L) {
                SystemClock.elapsedRealtime()
            } else {
                SystemClock.elapsedRealtime() - elapsedTime
            }

            while (isRunning) {
                elapsedTime = SystemClock.elapsedRealtime() - timeReference
                delay(10)
            }
        }
    }

    var hasStarted by remember { mutableStateOf(false) }

    fun startStopwatch() {
        if (isPreparationEnabled) {
            isInPreparationPhase = true
            preparationCountdown = preparationSeconds
            winnerId = ""
            coroutineScope.launch {
                for (i in preparationSeconds downTo 1) {
                    preparationCountdown = i
                    delay(1000)
                }
                isInPreparationPhase = false
                BluetoothMessageManager.clearMessage()
                isRunning = true
                hasStarted = true
            }
        } else {
            BluetoothMessageManager.sendMessageToAll("reset")
            BluetoothMessageManager.clearMessage()
            winnerId = ""
            isRunning = true
            hasStarted = true
        }
    }

    fun pauseStopwatch() {
        isRunning = false
    }

    fun resetStopwatch() {
        isRunning = false
        hasStarted = false
        elapsedTime = 0L
        winnerId = ""
        BluetoothMessageManager.sendMessageToAll("reset")
    }

    val formattedStopwatchTime = formatMillisecondsToTimeWithMillisTwoDigits(elapsedTime)

    val backgroundColor = when {
        isRunning -> Color(0xFF4CAF50)
        !isRunning && hasStarted -> Color(0xFFFFF9C4)
        else -> Color.White
    }

    LaunchedEffect(isRunning, latestBluetoothMessage) {
        if (isRunning && hasStarted && winnerId.isEmpty() && !latestBluetoothMessage.isNullOrEmpty()) {
            winnerId = latestBluetoothMessage!!
            BluetoothMessageManager.clearMessage()
            BluetoothMessageManager.sendMessageToAll(winnerId)
            pauseStopwatch()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stopwatch") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = { showOptionsDialog = true }) {
                        Text("Options")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            DeviceStatusRow(
                deviceStates,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                WinnerDisplay(winnerId)
                if (isInPreparationPhase) {
                    Text(
                        text = "Get Ready: $preparationCountdown",
                        fontSize = 72.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                } else {
                    Text(
                        text = formattedStopwatchTime,
                        fontSize = 72.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val startPauseBgColor = if (isRunning) Color(0xFFFFCC80) else Color(0xFF4CAF50)
                    val startPauseText = if (isRunning) "Pause" else "Start"
                    val startPauseTextColor = Color.Black
                    val resetBgColor = Color(0xFFFF4E5F)
                    val resetTextColor = Color.Black
                    OutlinedButton(
                        onClick = {
                            if (isRunning) pauseStopwatch() else if (!isInPreparationPhase) startStopwatch()
                        },
                        modifier = Modifier
                            .height(64.dp)
                            .weight(2f)
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = startPauseBgColor)
                    ) {
                        Text(startPauseText, color = startPauseTextColor, fontSize = 28.sp)
                    }
                    OutlinedButton(onClick = { resetStopwatch() },
                        modifier = Modifier
                            .height(64.dp)
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 2.dp, end = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = resetBgColor)
                    ) {
                        Text("Reset", color = resetTextColor, fontSize = 22.sp)
                    }
                }
            }

            // Debug panel (visible if enabled in Options)
            if (showDebugControls) {
                OutlinedCard(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Debug Controls", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Device selector dropdown
                            Box {
                                Button(onClick = { debugMenuExpanded = true }) {
                                    Text(debugSelectedDevice.ifEmpty { "Select device" })
                                }
                                DropdownMenu(expanded = debugMenuExpanded, onDismissRequest = { debugMenuExpanded = false }) {
                                    deviceStates.forEach { device ->
                                        DropdownMenuItem(text = { Text(device.name) }, onClick = {
                                            debugSelectedDevice = device.name
                                            debugMenuExpanded = false
                                        })
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(onClick = {
                                if (debugSelectedDevice.isNotEmpty()) {
                                    BluetoothMessageManager.debugInjectMessage(debugSelectedDevice)
                                }
                            }) {
                                Text("Simulate Press")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(onClick = { BluetoothMessageManager.clearMessage() }) {
                                Text("Clear Message")
                            }
                        }
                    }
                }
            }

            if (showOptionsDialog) {
                AlertDialog(
                    onDismissRequest = { showOptionsDialog = false },
                    title = { Text("Options") },
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isPreparationEnabled,
                                    onCheckedChange = { isPreparationEnabled = it }
                                )
                                Text("Enable Preparation Phase")
                            }
                            // Debug toggle in Options
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showDebugControls, onCheckedChange = { showDebugControls = it })
                                Text("Show Debug Controls")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Preparation Seconds: ")
                                OutlinedTextField(
                                    value = preparationSeconds.toString(),
                                    onValueChange = { value ->
                                        val intValue = value.toIntOrNull()?.coerceIn(1, 99) ?: 3
                                        preparationSeconds = intValue
                                    },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp),
                                    enabled = isPreparationEnabled,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showOptionsDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StopwatchScreenPreview() {
    val mockDevices = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false),
        DeviceState(device = null, name = "BigRedB_3", address = "Unavailable", isConnected = false)
    )

    com.example.bigredbutton.ui.theme.BigRedButtonTheme {
        StopwatchScreen(
            onNavigateBack = {},
            deviceStates = mockDevices,
            updateDeviceState = { _, _ -> }
        )
    }
}

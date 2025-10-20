package com.example.bigredbutton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigredbutton.BluetoothMessageManager
import com.example.bigredbutton.models.DeviceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.tooling.preview.Preview
import com.example.bigredbutton.ui.theme.BigRedButtonTheme

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ReflexGameScreen(
    onNavigateBack: () -> Unit,
    deviceStates: List<DeviceState>,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit,
    modifier: Modifier = Modifier
) {
    // Game settings
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showScoreDialog by remember { mutableStateOf(false) }
    var isPreparationEnabled by remember { mutableStateOf(false) }
    var preparationSeconds by remember { mutableIntStateOf(3) }
    var preparationCountdown by remember { mutableIntStateOf(0) }

    var roundsTotal by remember { mutableIntStateOf(5) }
    var currentRound by remember { mutableIntStateOf(0) }

    var minRandomSeconds by remember { mutableIntStateOf(1) }
    var maxRandomSeconds by remember { mutableIntStateOf(10) }

    // Debug controls
    var showDebugControls by remember { mutableStateOf(false) }
    var debugSelectedDevice by remember { mutableStateOf(deviceStates.firstOrNull()?.name ?: "") }
    var debugMenuExpanded by remember { mutableStateOf(false) }

    // Safe runtime check for debug build (avoids unresolved BuildConfig during static analysis)
    val isDebugBuild = remember {
        try {
            val bc = Class.forName("com.example.bigredbutton.BuildConfig")
            val f = bc.getField("DEBUG")
            f.getBoolean(null)
        } catch (_: Exception) {
            false
        }
    }

    // Game runtime state
    var isRunning by remember { mutableStateOf(false) }
    var isInPreparationPhase by remember { mutableStateOf(false) }
    var isGreen by remember { mutableStateOf(false) }
    var winnerId by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val latestBluetoothMessage by BluetoothMessageManager.latestMessage.collectAsState()

    // Scores
    val deviceScores = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(deviceStates) {
        deviceStates.forEach { device ->
            if (deviceScores[device.name] == null) deviceScores[device.name] = 0
        }
        // ensure default debug selected device is valid
        if (debugSelectedDevice.isEmpty() && deviceStates.isNotEmpty()) debugSelectedDevice = deviceStates.first().name
    }

    // Control flag to stop rounds loop
    var keepPlaying by remember { mutableStateOf(false) }

    fun resetGame() {
        isRunning = false
        keepPlaying = false
        isGreen = false
        isInPreparationPhase = false
        currentRound = 0
        winnerId = ""
        deviceScores.keys.forEach { deviceScores[it] = 0 }
        BluetoothMessageManager.sendMessageToAll("reset")
        BluetoothMessageManager.clearMessage()
    }

    fun startRounds() {
        if (isRunning) return
        // validate interval
        if (minRandomSeconds <= 0) minRandomSeconds = 1
        if (maxRandomSeconds < minRandomSeconds) maxRandomSeconds = minRandomSeconds

        keepPlaying = true
        isRunning = true
        currentRound = 0
        winnerId = ""
        BluetoothMessageManager.clearMessage()

        coroutineScope.launch {
            while (keepPlaying && currentRound < roundsTotal) {
                currentRound += 1
                // Preparation phase
                if (isPreparationEnabled) {
                    isInPreparationPhase = true
                    preparationCountdown = preparationSeconds
                    winnerId = ""
                    for (i in preparationSeconds downTo 1) {
                        preparationCountdown = i
                        delay(1000)
                    }
                    isInPreparationPhase = false
                }

                // Random wait before "go"
                isGreen = false
                winnerId = ""
                BluetoothMessageManager.clearMessage()
                val waitSeconds = if (minRandomSeconds == maxRandomSeconds) minRandomSeconds else Random.nextInt(minRandomSeconds, maxRandomSeconds + 1)
                delay(waitSeconds * 1000L)

                // Turn green and wait for press
                isGreen = true

                // Wait until a winner is set by the listener (see LaunchedEffect)
                // We'll wait with a timeout equal to maxRandomSeconds + 5s to avoid infinite waiting
                val timeoutMillis = (maxRandomSeconds + 5) * 1000L
                var elapsed = 0L
                val tick = 50L
                while (keepPlaying && winnerId.isEmpty() && elapsed < timeoutMillis) {
                    delay(tick)
                    elapsed += tick
                }

                // If no one pressed in time, mark no winner (keep winnerId empty)
                // Announce reset to devices so LEDs/buttons can reset
                BluetoothMessageManager.sendMessageToAll(if (winnerId.isNotEmpty()) winnerId else "no_winner")

                // Brief pause showing result then reset for next round
                isGreen = false
                delay(1000)

                // Continue to next round automatically
            }

            // finished playing
            isRunning = false
            keepPlaying = false
        }
    }

    // Listen for device button presses
    LaunchedEffect(latestBluetoothMessage, isGreen, isRunning) {
        if (isRunning && isGreen && !latestBluetoothMessage.isNullOrEmpty() && winnerId.isEmpty()) {
            // First press after green
            winnerId = latestBluetoothMessage!!
            // assign point
            deviceScores[winnerId] = (deviceScores[winnerId] ?: 0) + 1
            // notify devices and freeze until next round
            BluetoothMessageManager.sendMessageToAll(winnerId)
            BluetoothMessageManager.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reflex Game") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = { showScoreDialog = true }, modifier = Modifier.padding(end = 8.dp)) { Text("Score") }
                    Button(onClick = { showOptionsDialog = true }) { Text("Options") }
                }
            )
        }
    ) { paddingValues ->
        val backgroundColor = when {
            isGreen -> Color(0xFF4CAF50)
            !isGreen && isRunning -> Color(0xFFFFF9C4)
            else -> Color.White
        }

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
                        text = "Round: $currentRound / $roundsTotal",
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Text(
                        text = if (isRunning) if (isGreen) "GO!" else "Wait for green..." else "Press Start",
                        fontSize = 32.sp,
                        color = if (isGreen) Color.White else Color.Black,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
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
                                        // Inject the message as if the device pressed its button
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

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val startBg = if (isRunning) Color(0xFFFFCC80) else Color(0xFF4CAF50)
                    val startText = if (isRunning) "Running" else "Start"
                    OutlinedButton(
                        onClick = {
                            if (isRunning) return@OutlinedButton
                            else {
                                startRounds()
                            }
                        },
                        modifier = Modifier
                            .height(64.dp)
                            .weight(2f)
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = startBg)
                    ) {
                        Text(startText, color = Color.Black, fontSize = 28.sp)
                    }

                    OutlinedButton(
                        onClick = { resetGame() },
                        modifier = Modifier
                            .height(64.dp)
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 2.dp, end = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4E5F))
                    ) {
                        Text("Reset", color = Color.Black, fontSize = 22.sp)
                    }
                }
            }

            if (showScoreDialog) {
                AlertDialog(
                    onDismissRequest = { showScoreDialog = false },
                    title = { Text("Scores") },
                    text = {
                        Column {
                            Text("Round: $currentRound / $roundsTotal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                            deviceStates.forEach { device ->
                                val score = deviceScores[device.name] ?: 0
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Text(device.name, modifier = Modifier.weight(1f))
                                        Text(score.toString(), modifier = Modifier.width(32.dp), fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showScoreDialog = false }) { Text("Close") }
                    }
                )
            }

            if (showOptionsDialog) {
                AlertDialog(
                    onDismissRequest = { showOptionsDialog = false },
                    title = { Text("Options") },
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isPreparationEnabled, onCheckedChange = { isPreparationEnabled = it })
                                Text("Enable Preparation Phase")
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
                                    enabled = isPreparationEnabled
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rounds: ")
                                OutlinedTextField(
                                    value = roundsTotal.toString(),
                                    onValueChange = { value ->
                                        val intValue = value.toIntOrNull()?.coerceIn(1, 999) ?: 5
                                        roundsTotal = intValue
                                    },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Random interval min (s): ")
                                OutlinedTextField(
                                    value = minRandomSeconds.toString(),
                                    onValueChange = { value ->
                                        val intValue = value.toIntOrNull()?.coerceIn(1, 999) ?: 1
                                        minRandomSeconds = intValue
                                    },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("max (s): ")
                                OutlinedTextField(
                                    value = maxRandomSeconds.toString(),
                                    onValueChange = { value ->
                                        val intValue = value.toIntOrNull()?.coerceIn(1, 999) ?: 10
                                        maxRandomSeconds = intValue
                                    },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                            }

                            // Show the debug toggle in Options (match ColorWordGameScreen: always visible)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showDebugControls, onCheckedChange = { showDebugControls = it })
                                Text("Show Debug Controls")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            // ensure min <= max
                            if (minRandomSeconds > maxRandomSeconds) {
                                val tmp = minRandomSeconds
                                minRandomSeconds = maxRandomSeconds
                                maxRandomSeconds = tmp
                            }
                            showOptionsDialog = false
                        }) {
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
fun ReflexGameScreenPreview() {
    val mockDevices = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false)
    )

    BigRedButtonTheme {
        ReflexGameScreen(onNavigateBack = {}, deviceStates = mockDevices, updateDeviceState = { _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun ReflexGameScoreDialogPreview() {
    val mockDevices = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false)
    )
    // sample scores for preview
    val sampleScores = mapOf("BigRedB_1" to 3, "BigRedB_2" to 1)

    BigRedButtonTheme {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Scores") },
            text = {
                Column {
                    Text("Round: 2 / 5", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    mockDevices.forEach { device ->
                        val score = sampleScores[device.name] ?: 0
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(device.name, modifier = Modifier.weight(1f))
                                Text(score.toString(), modifier = Modifier.width(32.dp), fontSize = 20.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {}) { Text("Close") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReflexGameOptionsDialogPreview() {
    var isPreparationEnabled by remember { mutableStateOf(true) }
    var preparationSeconds by remember { mutableIntStateOf(3) }
    var roundsTotal by remember { mutableIntStateOf(5) }
    var minRandomSeconds by remember { mutableIntStateOf(1) }
    var maxRandomSeconds by remember { mutableIntStateOf(10) }

    BigRedButtonTheme {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Options") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPreparationEnabled, onCheckedChange = { isPreparationEnabled = it })
                        Text("Enable Preparation Phase")
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
                            enabled = isPreparationEnabled
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rounds: ")
                        OutlinedTextField(
                            value = roundsTotal.toString(),
                            onValueChange = { value ->
                                val intValue = value.toIntOrNull()?.coerceIn(1, 999) ?: 5
                                roundsTotal = intValue
                            },
                            singleLine = true,
                            modifier = Modifier.width(64.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Random interval min (s): ")
                        OutlinedTextField(
                            value = minRandomSeconds.toString(),
                            onValueChange = { value ->
                                val intValue = value.toIntOrNull()?.coerceIn(1, 999) ?: 1
                                minRandomSeconds = intValue
                            },
                            singleLine = true,
                            modifier = Modifier.width(64.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("max (s): ")
                        OutlinedTextField(
                            value = maxRandomSeconds.toString(),
                            onValueChange = { value ->
                                val intValue = value.toIntOrNull()?.coerceIn(1, 999) ?: 10
                                maxRandomSeconds = intValue
                            },
                            singleLine = true,
                            modifier = Modifier.width(64.dp)
                        )
                    }

                    // Show the debug toggle in Options (match ColorWordGameScreen: always visible)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = true, onCheckedChange = {})
                        Text("Show Debug Controls")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {}) { Text("OK") }
            }
        )
    }
}

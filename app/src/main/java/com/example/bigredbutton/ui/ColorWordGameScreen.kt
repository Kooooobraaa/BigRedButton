package com.example.bigredbutton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigredbutton.BluetoothMessageManager
import com.example.bigredbutton.models.DeviceState
import com.example.bigredbutton.ui.theme.BigRedButtonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ColorEntry(
    val key: String, // internal key
    val color: Color,
    val translations: Map<String, String> // language code -> word
)

private val SUPPORTED_LANGUAGES = listOf(
    "en" to "English",
    "cs" to "Czech",
    "sk" to "Slovak",
    "pl" to "Polish",
    "de" to "German"
)

private val COLOR_ENTRIES = listOf(
    ColorEntry("red", Color(0xFFF44336), mapOf("en" to "Red", "cs" to "Červená", "sk" to "Červená", "pl" to "Czerwony", "de" to "Rot")),
    ColorEntry("blue", Color(0xFF2196F3), mapOf("en" to "Blue", "cs" to "Modrá", "sk" to "Modrá", "pl" to "Niebieski", "de" to "Blau")),
    ColorEntry("green", Color(0xFF4CAF50), mapOf("en" to "Green", "cs" to "Zelená", "sk" to "Zelená", "pl" to "Zielony", "de" to "Grün")),
    ColorEntry("yellow", Color(0xFFFFEB3B), mapOf("en" to "Yellow", "cs" to "Žlutá", "sk" to "Žltá", "pl" to "Żółty", "de" to "Gelb")),
    ColorEntry("black", Color(0xFF000000), mapOf("en" to "Black", "cs" to "Černá", "sk" to "Čierna", "pl" to "Czarny", "de" to "Schwarz")),
    ColorEntry("white", Color(0xFFFFFFFF), mapOf("en" to "White", "cs" to "Bílá", "sk" to "Biela", "pl" to "Biały", "de" to "Weiß")),
    ColorEntry("orange", Color(0xFFFF9800), mapOf("en" to "Orange", "cs" to "Oranžová", "sk" to "Oranžová", "pl" to "Pomarańczowy", "de" to "Orange")),
    ColorEntry("purple", Color(0xFF9C27B0), mapOf("en" to "Purple", "cs" to "Fialová", "sk" to "Fialová", "pl" to "Fioletowy", "de" to "Violett"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ColorWordGameScreen(
    onNavigateBack: () -> Unit,
    deviceStates: List<DeviceState>,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit,
    modifier: Modifier = Modifier
) {
    // Options & dialogs
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showScoreDialog by remember { mutableStateOf(false) }
    var isPreparationEnabled by remember { mutableStateOf(false) }
    var preparationSeconds by remember { mutableIntStateOf(3) }
    var preparationCountdown by remember { mutableIntStateOf(0) }

    var roundsTotal by remember { mutableIntStateOf(10) }
    var currentRound by remember { mutableIntStateOf(0) }

    var roundDurationSec by remember { mutableIntStateOf(5) }

    var selectedLanguage by remember { mutableStateOf("en") }

    // Scoring customization
    var matchScore by remember { mutableIntStateOf(1) }
    var mismatchScore by remember { mutableIntStateOf(-1) }
    var earlyPenaltyScore by remember { mutableIntStateOf(-1) }

    // Detect debug build safely at runtime (avoids unresolved BuildConfig in analysis)
    val isDebugBuild = remember {
        try {
            val bc = Class.forName("com.example.bigredbutton.BuildConfig")
            val f = bc.getField("DEBUG")
            f.getBoolean(null)
        } catch (_: Exception) {
            false
        }
    }

    // Debug controls (match ReflexGameScreen pattern)
    var showDebugControls by remember { mutableStateOf(false) }
    var debugSelectedDevice by remember { mutableStateOf(deviceStates.firstOrNull()?.name ?: "") }
    var debugMenuExpanded by remember { mutableStateOf(false) }

    // Game runtime
    var isRunning by remember { mutableStateOf(false) }
    var isInPreparationPhase by remember { mutableStateOf(false) }
    var isWordVisible by remember { mutableStateOf(false) }
    var displayedWord by remember { mutableStateOf("") }
    var displayedColor by remember { mutableStateOf(Color.Black) }
    var displayedColorKey by remember { mutableStateOf("") }
    var winnerId by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val latestBluetoothMessage by BluetoothMessageManager.latestMessage.collectAsState()

    // Scores
    val deviceScores = remember { mutableStateMapOf<String, Int>() }
    // Use an immutable Set for state to avoid mutable-collection-in-state warnings
    val blockedDevices = remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(deviceStates) {
        deviceStates.forEach { device ->
            if (deviceScores[device.name] == null) deviceScores[device.name] = 0
        }
        if (debugSelectedDevice.isEmpty() && deviceStates.isNotEmpty()) debugSelectedDevice = deviceStates.first().name
    }

    var keepPlaying by remember { mutableStateOf(false) }

    fun resetGame() {
        isRunning = false
        keepPlaying = false
        isInPreparationPhase = false
        isWordVisible = false
        currentRound = 0
        winnerId = ""
        deviceScores.keys.forEach { deviceScores[it] = 0 }
        blockedDevices.value = emptySet()
        BluetoothMessageManager.sendMessageToAll("reset")
        BluetoothMessageManager.clearMessage()
    }

    fun pickNextWord() {
        val wordEntry = COLOR_ENTRIES.random()
        val colorEntry = COLOR_ENTRIES.random()
        displayedWord = wordEntry.translations[selectedLanguage] ?: wordEntry.translations["en"] ?: wordEntry.key
        displayedColor = colorEntry.color
        displayedColorKey = colorEntry.key
        // clear blocked devices each new round
        blockedDevices.value = emptySet()
    }

    fun startRounds() {
        if (isRunning) return
        keepPlaying = true
        isRunning = true
        currentRound = 0
        winnerId = ""
        BluetoothMessageManager.clearMessage()

        coroutineScope.launch {
            while (keepPlaying && currentRound < roundsTotal) {
                currentRound += 1

                if (isPreparationEnabled) {
                    isInPreparationPhase = true
                    preparationCountdown = preparationSeconds
                    for (i in preparationSeconds downTo 1) {
                        preparationCountdown = i
                        delay(1000)
                    }
                    isInPreparationPhase = false
                }

                // Show word
                pickNextWord()
                isWordVisible = true
                winnerId = ""
                BluetoothMessageManager.clearMessage()

                // Wait for press or timeout
                val timeoutMillis = roundDurationSec * 1000L
                var elapsed = 0L
                val tick = 50L
                while (keepPlaying && winnerId.isEmpty() && elapsed < timeoutMillis) {
                    delay(tick)
                    elapsed += tick
                }

                // Announce result
                BluetoothMessageManager.sendMessageToAll(winnerId.ifEmpty { "no_winner" })

                // Brief pause
                isWordVisible = false
                delay(800)
            }

            isRunning = false
            keepPlaying = false
        }
    }

    // Handle early device presses (penalize if presses occur before word is visible)
    LaunchedEffect(latestBluetoothMessage, isWordVisible, isRunning, isInPreparationPhase) {
        if (isRunning && !isWordVisible && !latestBluetoothMessage.isNullOrEmpty()) {
            val id = latestBluetoothMessage!!
            // ignore if already blocked this round
            if (!blockedDevices.value.contains(id)) {
                // apply early penalty
                deviceScores[id] = (deviceScores[id] ?: 0) + earlyPenaltyScore
                blockedDevices.value = blockedDevices.value + id
                BluetoothMessageManager.sendMessageToAll("penalty:$id")
                BluetoothMessageManager.clearMessage()
            }
        }
    }

    // Handle device presses during displayed word
    LaunchedEffect(latestBluetoothMessage, isWordVisible, isRunning) {
        if (isRunning && isWordVisible && !latestBluetoothMessage.isNullOrEmpty() && winnerId.isEmpty()) {
            val id = latestBluetoothMessage!!
            // ignore if blocked due to early press
            if (blockedDevices.value.contains(id)) {
                // ignore press
                BluetoothMessageManager.clearMessage()
                return@LaunchedEffect
            }
            // check if displayed word matches the font color key
            val wordKeyForDisplayedWord = COLOR_ENTRIES.find { it.translations[selectedLanguage] == displayedWord }?.key
            val isMatch = displayedColorKey == wordKeyForDisplayedWord
            // assign score using customizable values
            deviceScores[id] = (deviceScores[id] ?: 0) + if (isMatch) matchScore else mismatchScore
            winnerId = id
            BluetoothMessageManager.sendMessageToAll(id)
            BluetoothMessageManager.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Word") },
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White)
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
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isWordVisible) {
                        // If color is white, add a shadow outline so it's visible on white background
                        if (displayedColorKey == "white") {
                            // Draw a simple outline for white text by layering offset black texts behind the white text
                            Box(modifier = Modifier.padding(24.dp)) {
                                Text(text = displayedWord, color = Color.Black, fontSize = 72.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(1.dp, 0.dp))
                                Text(text = displayedWord, color = Color.Black, fontSize = 72.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset((-1).dp, 0.dp))
                                Text(text = displayedWord, color = Color.Black, fontSize = 72.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(0.dp, 1.dp))
                                Text(text = displayedWord, color = Color.Black, fontSize = 72.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(0.dp, (-1).dp
                                ))
                                Text(text = displayedWord, color = displayedColor, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = displayedWord,
                                color = displayedColor,
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        Text(
                            text = if (isRunning) "Wait for next word..." else "Press Start",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
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

                                Button(onClick = { if (debugSelectedDevice.isNotEmpty()) BluetoothMessageManager.debugInjectMessage(debugSelectedDevice) }) {
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
                        onClick = { if (!isRunning) startRounds() },
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
                                        val intValue = value.toIntOrNull()?.coerceIn(1, 999) ?: 10
                                        roundsTotal = intValue
                                    },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Round duration (s): ")
                                OutlinedTextField(
                                    value = roundDurationSec.toString(),
                                    onValueChange = { value ->
                                        val intValue = value.toIntOrNull()?.coerceIn(1, 60) ?: 5
                                        roundDurationSec = intValue
                                    },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Language: ")
                                Spacer(modifier = Modifier.width(8.dp))
                                var langExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Button(onClick = { langExpanded = true }) { Text(SUPPORTED_LANGUAGES.find { it.first == selectedLanguage }?.second ?: "English") }
                                    DropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                                        SUPPORTED_LANGUAGES.forEach { (code, label) ->
                                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedLanguage = code; langExpanded = false })
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Match score: ")
                                OutlinedTextField(
                                    value = matchScore.toString(),
                                    onValueChange = { value -> matchScore = value.toIntOrNull() ?: matchScore },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Mismatch score: ")
                                OutlinedTextField(
                                    value = mismatchScore.toString(),
                                    onValueChange = { value -> mismatchScore = value.toIntOrNull() ?: mismatchScore },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Early penalty (negative): ")
                                OutlinedTextField(
                                    value = earlyPenaltyScore.toString(),
                                    onValueChange = { value -> earlyPenaltyScore = value.toIntOrNull() ?: earlyPenaltyScore },
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showDebugControls, onCheckedChange = { showDebugControls = it })
                                Text("Show Debug Controls")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showOptionsDialog = false
                        }) { Text("OK") }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorWordGameScreenPreview() {
    val mockDevices = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false)
    )

    BigRedButtonTheme {
        ColorWordGameScreen(onNavigateBack = {}, deviceStates = mockDevices, updateDeviceState = { _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun ColorWordScoreDialogPreview() {
    val mockDevices = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false)
    )
    val sampleScores = mapOf("BigRedB_1" to 2, "BigRedB_2" to -1)

    BigRedButtonTheme {
        AlertDialog(onDismissRequest = {}, title = { Text("Scores") }, text = {
            Column {
                Text("Round: 3 / 10", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                mockDevices.forEach { device ->
                    val score = sampleScores[device.name] ?: 0
                    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            Text(device.name, modifier = Modifier.weight(1f))
                            Text(score.toString(), modifier = Modifier.width(32.dp), fontSize = 20.sp)
                        }
                    }
                }
            }
        }, confirmButton = { Button(onClick = {}) { Text("Close") } })
    }
}

@Preview(showBackground = true)
@Composable
fun ColorWordOptionsDialogPreview() {
    var isPreparationEnabled by remember { mutableStateOf(true) }
    var preparationSeconds by remember { mutableIntStateOf(3) }
    var roundsTotal by remember { mutableIntStateOf(10) }
    var roundDurationSec by remember { mutableIntStateOf(5) }
    var selectedLanguage by remember { mutableStateOf("en") }
    var showDebugControls by remember { mutableStateOf(true) }

    BigRedButtonTheme {
        AlertDialog(onDismissRequest = {}, title = { Text("Options") }, text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPreparationEnabled, onCheckedChange = { isPreparationEnabled = it })
                    Text("Enable Preparation Phase")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Preparation Seconds: ")
                    OutlinedTextField(value = preparationSeconds.toString(), onValueChange = {}, singleLine = true, modifier = Modifier.width(64.dp), enabled = isPreparationEnabled)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rounds: ")
                    OutlinedTextField(value = roundsTotal.toString(), onValueChange = {}, singleLine = true, modifier = Modifier.width(64.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Round duration (s): ")
                    OutlinedTextField(value = roundDurationSec.toString(), onValueChange = {}, singleLine = true, modifier = Modifier.width(64.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Language: ")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SUPPORTED_LANGUAGES.find { it.first == selectedLanguage }?.second ?: "English")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showDebugControls, onCheckedChange = {})
                    Text("Show Debug Controls")
                }
            }
        }, confirmButton = { Button(onClick = {}) { Text("OK") } })
    }
}

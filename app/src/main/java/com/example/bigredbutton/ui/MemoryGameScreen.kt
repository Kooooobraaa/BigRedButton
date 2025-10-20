package com.example.bigredbutton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.tooling.preview.Preview
import com.example.bigredbutton.models.DeviceState
import com.example.bigredbutton.BluetoothMessageManager
import com.example.bigredbutton.ui.theme.BigRedButtonTheme
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun MemoryGameScreen(
    onNavigateBack: () -> Unit,
    deviceStates: List<DeviceState>,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit,
    modifier: Modifier = Modifier
) {
    // Game settings
    var sequenceLength by remember { mutableIntStateOf(10) }
    var showPrompts by remember { mutableStateOf(true) }

    // Game options: number of players (default 2)
    var numberOfPlayers by remember { mutableIntStateOf(2) }

    // Players are named "Player 1", "Player 2", ... according to numberOfPlayers
    val playerNames = remember(numberOfPlayers) {
        List(numberOfPlayers) { index -> "Player ${index + 1}" }
    }

    // Player scores (keyed by player name)
    val playerScores = remember { mutableStateMapOf<String, Int>() }

    // Game state
    var sequence by remember { mutableStateOf(listOf<Int>()) }
    var expectedIndex by remember { mutableIntStateOf(0) }
    // previewSeconds controls how long the full sequence is shown before being hidden
    var previewSeconds by remember { mutableIntStateOf(3) }
    // previewing true while the full sequence is visible after Start
    var previewing by remember { mutableStateOf(false) }
    // per-item visibility flags (true = visible), sized to sequenceLength when sequence is generated
    val visibleCards = remember { mutableStateListOf<Boolean>() }
    // coroutine scope for the preview timer
    val coroutineScope = rememberCoroutineScope()

    // Running / UI state that was previously removed accidentally
    var isRunning by remember { mutableStateOf(false) }
    var showScoreDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var lastResultText by remember { mutableStateOf("") }
    var currentPlayerIndex by remember { mutableIntStateOf(0) }

    // Listen for Bluetooth messages from physical devices
    val latestBluetoothMessage by BluetoothMessageManager.latestMessage.collectAsState()

    // Debug controls (visible only in debug builds)
    var debugSelectedDevice by remember { mutableStateOf(deviceStates.firstOrNull()?.name ?: "") }
    var debugMenuExpanded by remember { mutableStateOf(false) }
    var showDebugControls by remember { mutableStateOf(false) }

    fun generateSequence() {
        // Always generate values for the three physical Bluetooth buttons (1..3).
        sequence = List(sequenceLength) { Random.nextInt(1, 4) }
        // initialize visibleCards to all visible for preview
        visibleCards.clear()
        sequence.forEach { _ -> visibleCards.add(true) }
    }

    fun startGame() {
        generateSequence()
        expectedIndex = 0
        isRunning = true
        lastResultText = ""
        // start preview: keep all cards visible for previewSeconds, then hide them
        previewing = true
        coroutineScope.launch {
            // small initial delay to ensure UI updates
            if (previewSeconds > 0) {
                delay(previewSeconds * 1000L)
            }
            // hide all cards after preview
            for (i in visibleCards.indices) visibleCards[i] = false
            previewing = false
        }
    }

    fun resetGame() {
        isRunning = false
        sequence = emptyList()
        visibleCards.clear()
        expectedIndex = 0
        previewing = false
        lastResultText = ""
    }

    fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % playerNames.size
        resetGame()
    }

    fun onButtonPressed(buttonNumber: Int) {
        if (!isRunning || previewing) return
        val expected = sequence.getOrNull(expectedIndex) ?: return
        if (buttonNumber == expected) {
            // correct press: reveal this card and award point
            if (expectedIndex in visibleCards.indices) visibleCards[expectedIndex] = true
            val playerName = playerNames[currentPlayerIndex]
            playerScores[playerName] = (playerScores[playerName] ?: 0) + 1
            expectedIndex++
            // check for completion of the full sequence
            if (expectedIndex >= sequence.size) {
                lastResultText = "Completed all rounds!"
                isRunning = false
                showGameOverDialog = true
            }
        } else {
            // wrong press -> game over for this player
            lastResultText = "Wrong button: expected $expected"
            isRunning = false
            showGameOverDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Game") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = { showScoreDialog = true }, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Score")
                    }
                    Button(onClick = { showOptionsDialog = true }) {
                        Text("Options")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Player: ${playerNames[currentPlayerIndex]}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Item: ${if (sequence.isNotEmpty()) minOf(expectedIndex + 1, sequence.size) else 0} / $sequenceLength", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            // Prompt area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                val promptText = if (!isRunning) "Press Start"
                else {
                        "Repeat the sequence"
                }
                Text(promptText, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sequence display: per-item cards (visible during preview and revealed on correct presses)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sequence.forEachIndexed { idx, value ->
                    OutlinedCard(modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            val content = if (visibleCards.getOrNull(idx) == true) value.toString() else "?"
                            Text(content, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    // Prepare a fresh sequence but don't start automatically
                    generateSequence()
                    expectedIndex = 0
                    isRunning = false
                    lastResultText = ""
                }) { Text("New Game") }
                Button(onClick = { nextPlayer() }) { Text("Next Player") }
                Button(onClick = { if (!isRunning) startGame() else { /* no-op */ } }) { Text("Start") }
            }

            // Debug panel for testing Bluetooth input locally (visible when enabled in Options)
            if (showDebugControls) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedCard(modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Debug Controls", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                if (debugSelectedDevice.isNotEmpty()) BluetoothMessageManager.debugInjectMessage(debugSelectedDevice)
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

            Spacer(modifier = Modifier.height(8.dp))

            if (lastResultText.isNotEmpty()) {
                Text(lastResultText)
            }

        }
    }

    if (showScoreDialog) {
        AlertDialog(
            onDismissRequest = { showScoreDialog = false },
            title = { Text("Scores") },
            text = {
                Column {
                    playerNames.forEach { name ->
                        val score = playerScores[name] ?: 0
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.weight(1f))
                            Text(score.toString())
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showScoreDialog = false }) { Text("Close") } }
        )
    }

    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Options") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sequence Length", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = sequenceLength.toString(),
                            onValueChange = { v -> sequenceLength = v.toIntOrNull()?.coerceIn(1, 99) ?: sequenceLength },
                            modifier = Modifier.width(72.dp),
                            singleLine = true
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Players", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = numberOfPlayers.toString(),
                            onValueChange = { v -> numberOfPlayers = v.toIntOrNull()?.coerceIn(1, 6) ?: numberOfPlayers },
                            modifier = Modifier.width(72.dp),
                            singleLine = true
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showPrompts, onCheckedChange = { showPrompts = it })
                        Text("Show prompts")
                    }
                    // Show the debug toggle in Options (match ReflexGameScreen)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showDebugControls, onCheckedChange = { showDebugControls = it })
                        Text("Show Debug Controls")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Preview Seconds", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = previewSeconds.toString(),
                            onValueChange = { v -> previewSeconds = v.toIntOrNull()?.coerceIn(0, 10) ?: previewSeconds },
                            modifier = Modifier.width(72.dp),
                            singleLine = true
                        )
                    }
                 }
             },
             confirmButton = { Button(onClick = { showOptionsDialog = false }) { Text("Close") } }
         )
     }

    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { showGameOverDialog = false },
            title = { Text("Round finished") },
            text = { Text(lastResultText) },
            confirmButton = {
                Button(onClick = {
                    showGameOverDialog = false
                    lastResultText = ""
                }) { Text("OK") }
            }
        )
    }

    // React to Bluetooth messages by mapping device name to button index (1..3)
    LaunchedEffect(latestBluetoothMessage, isRunning) {
        val msg = latestBluetoothMessage
        if (isRunning && !msg.isNullOrEmpty()) {
            // Try direct mapping first (device name present in playerNames)
            var handled = false
            val deviceIndex = playerNames.indexOf(msg)
            if (deviceIndex >= 0 && deviceIndex < numberOfPlayers) {
                onButtonPressed(deviceIndex + 1)
                handled = true
            } else {
                // Fallback: parse trailing number from message like "BigRedB_2" and map
                // only to the three physical buttons (1..3).
                val digits = "\\d+".toRegex().find(msg)?.value
                val parsed = digits?.toIntOrNull()
                if (parsed != null && parsed in 1..3) {
                    onButtonPressed(parsed)
                    handled = true
                }
            }
            // clear message to avoid repeated handling
            if (handled) BluetoothMessageManager.clearMessage()
        }
    }

    // Initialize or reset player scores when numberOfPlayers changes.
    LaunchedEffect(numberOfPlayers) {
        playerScores.clear()
        playerNames.forEach { name -> playerScores[name] = 0 }
        if (currentPlayerIndex >= playerNames.size) currentPlayerIndex = 0
    }
}

@Preview(showBackground = true)
@Composable
fun MemoryGameScreenPreview() {
    val mockDevices = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false),
        DeviceState(device = null, name = "BigRedB_3", address = "Unavailable", isConnected = false)
    )
    BigRedButtonTheme {
        MemoryGameScreen(onNavigateBack = {}, deviceStates = mockDevices, updateDeviceState = { _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun MemoryGameScoreDialogPreview() {
    val sampleScores = mapOf("Player 1" to 5, "Player 2" to 2)
    BigRedButtonTheme {
        AlertDialog(
            onDismissRequest = { /* preview only */ },
            title = { Text("Scores") },
            text = {
                Column {
                    sampleScores.forEach { (name, score) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.weight(1f))
                            Text(score.toString())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { /* preview */ }) { Text("Close") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MemoryGameOptionsDialogPreview() {
    var seqLength by remember { mutableIntStateOf(10) }
    var showPrompts by remember { mutableStateOf(true) }
    var showDebugControls by remember { mutableStateOf(false) }
    var previewSeconds by remember { mutableIntStateOf(3) }
    BigRedButtonTheme {
        AlertDialog(
            onDismissRequest = { /* preview only */ },
            title = { Text("Options") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sequence Length", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = seqLength.toString(),
                            onValueChange = { v -> seqLength = v.toIntOrNull()?.coerceIn(1, 99) ?: seqLength },
                            modifier = Modifier.width(72.dp),
                            singleLine = true
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Players", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = "2",
                            onValueChange = { /* no-op */ },
                            modifier = Modifier.width(72.dp),
                            singleLine = true
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showPrompts, onCheckedChange = { showPrompts = it })
                        Text("Show prompts")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showDebugControls, onCheckedChange = { showDebugControls = it })
                        Text("Show Debug Controls")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Preview Seconds", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = previewSeconds.toString(),
                            onValueChange = { v -> previewSeconds = v.toIntOrNull()?.coerceIn(0, 10) ?: previewSeconds },
                            modifier = Modifier.width(72.dp),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { /* preview */ }) { Text("Close") } }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MemoryGameOverDialogPreview() {
    BigRedButtonTheme {
        AlertDialog(
            onDismissRequest = { /* preview only */ },
            title = { Text("Round finished") },
            text = { Text("Wrong button: expected 2") },
            confirmButton = { TextButton(onClick = { /* preview */ }) { Text("OK") } }
        )
    }
}

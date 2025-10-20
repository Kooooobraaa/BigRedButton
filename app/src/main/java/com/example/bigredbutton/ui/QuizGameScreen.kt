package com.example.bigredbutton.ui

import android.media.MediaPlayer
import android.os.CountDownTimer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigredbutton.BluetoothMessageManager
import com.example.bigredbutton.models.DeviceState
import com.example.bigredbutton.ui.theme.BigRedButtonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.bigredbutton.R
import androidx.compose.ui.tooling.preview.Preview
import com.example.bigredbutton.util.formatMillisecondsToTimeWithMillis

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun QuizGameScreen(
    onNavigateBack: () -> Unit,
    deviceStates: List<DeviceState>,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputMinutes by remember { mutableStateOf("") }
    var inputSeconds by remember { mutableStateOf("") }
    var initialTimeMillis by remember { mutableLongStateOf(60000L) }

    var timeLeftInMillis by remember { mutableLongStateOf(initialTimeMillis) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var countDownTimer: CountDownTimer? by remember { mutableStateOf(null) }

    var winnerId by remember { mutableStateOf("") }

    val formattedTime = formatMillisecondsToTimeWithMillis(timeLeftInMillis)
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    fun playEndSound() {
        try {
            // Replace with your own sound resource if needed
             val mediaPlayer = MediaPlayer.create(context, R.raw.timer_end)
             mediaPlayer.setOnCompletionListener { it.release() }
             mediaPlayer.start()
        } catch (_: Exception) {
            // ignore
        }
    }

    var hasStarted by remember { mutableStateOf(false) }

    var showOptionsDialog by remember { mutableStateOf(false) }
    var isPreparationEnabled by remember { mutableStateOf(false) }
    var preparationSeconds by remember { mutableIntStateOf(3) }
    var preparationCountdown by remember { mutableIntStateOf(0) }
    var isInPreparationPhase by remember { mutableStateOf(false) }
    // Debug controls (always declared; UI visibility is guarded by runtime isDebugBuild)
    var showDebugControls by remember { mutableStateOf(false) }
    var debugSelectedDevice by remember { mutableStateOf(deviceStates.firstOrNull()?.name ?: "") }
    var debugMenuExpanded by remember { mutableStateOf(false) }
    // ensure default debug selected device is valid
    LaunchedEffect(deviceStates) {
        if (debugSelectedDevice.isEmpty() && deviceStates.isNotEmpty()) debugSelectedDevice = deviceStates.first().name
    }

    val coroutineScope = rememberCoroutineScope()

    val latestBluetoothMessage by BluetoothMessageManager.latestMessage.collectAsState()

    // Score dialog state
    var showScoreDialog by remember { mutableStateOf(false) }
    // Map device name to score
    val deviceScores = remember { mutableStateMapOf<String, Int>() }
    // Initialize scores for all devices if not present
    LaunchedEffect(deviceStates) {
        deviceStates.forEach { device ->
            if (deviceScores[device.name] == null) deviceScores[device.name] = 0
        }
    }

    // State for answer correctness dialog
    var showAnswerDialog by remember { mutableStateOf(false) }

    // Track devices that are blocked from winning until next round
    // prefer immutable Set inside state to avoid mutation-in-state warnings
    val blockedDevices = remember { mutableStateOf(setOf<String>()) }
    var roundNumber by remember { mutableIntStateOf(1) }

    // Score settings
    var correctScoreValue by remember { mutableIntStateOf(2) }
    var incorrectScoreValue by remember { mutableIntStateOf(-1) }

    fun startTimer() {
        if (isPreparationEnabled) {
            isInPreparationPhase = true
            preparationCountdown = preparationSeconds
            winnerId = ""
            hasStarted = true
            coroutineScope.launch {
                for (i in preparationSeconds downTo 1) {
                    preparationCountdown = i
                    delay(1000)
                }
                isInPreparationPhase = false
                countDownTimer?.cancel()
                BluetoothMessageManager.clearMessage()
                countDownTimer = object : CountDownTimer(timeLeftInMillis, 100) {
                    override fun onTick(millisUntilFinished: Long) {
                        timeLeftInMillis = millisUntilFinished
                    }

                    override fun onFinish() {
                        timeLeftInMillis = 0
                        isTimerRunning = false
                        playEndSound()
                    }
                }.start()
                isTimerRunning = true
                winnerId = ""
                BluetoothMessageManager.sendMessageToAll("reset")
            }
        } else {
            countDownTimer?.cancel()
            BluetoothMessageManager.clearMessage()
            countDownTimer = object : CountDownTimer(timeLeftInMillis, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftInMillis = millisUntilFinished
                }

                override fun onFinish() {
                    timeLeftInMillis = 0
                    isTimerRunning = false
                    playEndSound()
                }
            }.start()
            isTimerRunning = true
            hasStarted = true
            winnerId = ""
            BluetoothMessageManager.sendMessageToAll("reset")
        }
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = initialTimeMillis
        isTimerRunning = false
        hasStarted = false
        winnerId = ""
        BluetoothMessageManager.sendMessageToAll("reset")
    }

    fun setTimerDuration() {
        val minutes = inputMinutes.toLongOrNull() ?: 0L
        val seconds = inputSeconds.toLongOrNull()?.coerceIn(0, 59) ?: 0L
        initialTimeMillis = maxOf(1L, minutes * 60 + seconds) * 1000L
        resetTimer()
        focusManager.clearFocus()
    }

    val backgroundColor = when {
        isTimerRunning -> Color(0xFF4CAF50)
        !isTimerRunning && timeLeftInMillis == 0L && hasStarted -> Color(0xFFFF4E5F)
        !isTimerRunning && hasStarted -> Color(0xFFFFF9C4)
        else -> Color.White
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Game") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = { showScoreDialog = true },
                    modifier = Modifier.padding(end = 8.dp)) {
                        Text("Score")
                    }
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
                        text = formattedTime,
                        fontSize = 72.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    val startPauseBgColor = if (isTimerRunning) Color(0xFFFFCC80) else Color(0xFF4CAF50)
                    val startPauseText = if (isTimerRunning) "Pause" else "Start"
                    val startPauseTextColor = Color.Black
                    val resetBgColor = Color(0xFFFF4E5F)
                    val resetTextColor = Color.Black
                    OutlinedButton(
                        onClick = {
                            if (isTimerRunning) pauseTimer() else startTimer()
                        },
                        enabled = timeLeftInMillis > 0,
                        modifier = Modifier
                            .height(64.dp)
                            .weight(2f)
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = startPauseBgColor)
                    ) {
                        Text(startPauseText, color = startPauseTextColor, fontSize = 28.sp)
                    }
                    OutlinedButton(
                        onClick = { resetTimer() },
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

                // Debug panel (visible only in debug builds)
                // (uses isDebugBuild instead of BuildConfig.DEBUG to avoid unresolved references in preview/editor)
                // Visible when the Show Debug Controls option is enabled (Options dialog controls checkbox is only shown in debug builds)
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
            }

            if (showScoreDialog) {
                AlertDialog(
                    onDismissRequest = { showScoreDialog = false },
                    title = { Text("Scores") },
                    text = {
                        Column {
                            Text("Round: $roundNumber", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
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
                                        //IconButton(onClick = {}) { Text("-") }
                                        Text(score.toString(), modifier = Modifier.width(32.dp), fontSize = 20.sp)
                                        //IconButton(onClick = {}) { Text("+") }
                                        //Button(onClick = {}, modifier = Modifier.padding(start = 8.dp)) { Text("Reset") }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showScoreDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            if (showOptionsDialog) {
                AlertDialog(
                    onDismissRequest = { showOptionsDialog = false },
                    title = { Text("Options") },
                    text = {
                        Column {
                            // Score settings box
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Score Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Correct answer", modifier = Modifier.weight(1f))
                                        OutlinedTextField(
                                            value = correctScoreValue.toString(),
                                            onValueChange = { value ->
                                                correctScoreValue = value.toIntOrNull() ?: correctScoreValue
                                            },
                                            singleLine = true,
                                            modifier = Modifier.width(64.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Incorrect answer", modifier = Modifier.weight(1f))
                                        OutlinedTextField(
                                            value = incorrectScoreValue.toString(),
                                            onValueChange = { value ->
                                                incorrectScoreValue = value.toIntOrNull() ?: incorrectScoreValue
                                            },
                                            singleLine = true,
                                            modifier = Modifier.width(64.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isPreparationEnabled,
                                    onCheckedChange = { isPreparationEnabled = it }
                                )
                                Text("Enable Preparation Phase")
                            }
                            // Show the debug toggle in Options (match ColorWordGameScreen and ReflexGameScreen)
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
                                    modifier = Modifier.width(48.dp),
                                    enabled = isPreparationEnabled,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Set round duration: ")
                                OutlinedTextField(
                                    value = inputMinutes,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                            inputMinutes = newValue
                                        }
                                    },
                                    label = { Text("Min") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                                OutlinedTextField(
                                    value = inputSeconds,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                                            inputSeconds = newValue
                                        }
                                    },
                                    label = { Text("Sec") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.width(64.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showOptionsDialog = false
                            setTimerDuration() }) {
                            Text("Apply and Close")
                        }
                    }
                )
            }

            // Winner/answer dialog
            if (showAnswerDialog && winnerId.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showAnswerDialog = false },
                    title = { Text("Was the answer correct?") },
                    text = { Text("Device: $winnerId") },
                    confirmButton = {
                        Button(onClick = {
                            // Add correctScoreValue points for correct answer
                            deviceScores[winnerId] = (deviceScores[winnerId] ?: 0) + correctScoreValue
                            showAnswerDialog = false
                            winnerId = ""
                            blockedDevices.value = emptySet()
                        }) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            // Add incorrectScoreValue points for wrong answer and block device
                            deviceScores[winnerId] = (deviceScores[winnerId] ?: 0) + incorrectScoreValue
                            blockedDevices.value = blockedDevices.value + winnerId
                            showAnswerDialog = false
                            winnerId = ""
                        }) {
                            Text("No")
                        }
                    }
                )
            }

            // Round info and next round button
            Column(
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 64.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("Round: $roundNumber", style = MaterialTheme.typography.titleMedium)
                Row {
                    Button(onClick = {
                        roundNumber = 1
                        blockedDevices.value = emptySet()
                        winnerId = ""
                        hasStarted = false
                        isTimerRunning = false
                        timeLeftInMillis = initialTimeMillis
                        deviceScores.keys.forEach { deviceScores[it] = 0 }
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("New Game")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        roundNumber++
                        blockedDevices.value = emptySet()
                        winnerId = ""
                        hasStarted = false
                        isTimerRunning = false
                        timeLeftInMillis = initialTimeMillis
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Start Next Round")
                    }
                }
            }
        }
    }
    LaunchedEffect(isTimerRunning, latestBluetoothMessage) {
        if (isTimerRunning && hasStarted && winnerId.isEmpty() && !latestBluetoothMessage.isNullOrEmpty()) {
            val candidate = latestBluetoothMessage!!
            if (!blockedDevices.value.contains(candidate)) {
                winnerId = candidate
                BluetoothMessageManager.clearMessage()
                BluetoothMessageManager.sendMessageToAll(winnerId)
                pauseTimer()
                showAnswerDialog = true
            } else {
                // Ignore blocked device, clear message and keep timer running
                BluetoothMessageManager.clearMessage()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            countDownTimer?.cancel()
        }
    }
    LaunchedEffect(initialTimeMillis) {
        if (!isTimerRunning) {
            timeLeftInMillis = initialTimeMillis
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuizGameScreenPreview() {
    val mockDevices = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false),
        DeviceState(device = null, name = "BigRedB_3", address = "Unavailable", isConnected = false)
    )
    BigRedButtonTheme {
        QuizGameScreen(
            onNavigateBack = {},
            deviceStates = mockDevices,
            updateDeviceState = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScoreDialogPreview() {
    val deviceScores = remember { mutableStateMapOf("BigRedB_1" to 3, "BigRedB_2" to 1) }
    val deviceStates = listOf(
        DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false)
    )
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Scores") },
        text = {
            Column {
                Text("Round: 1", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
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
        confirmButton = { Button(onClick = {}) { Text("Close") } }
    )
}

@Preview(showBackground = true)
@Composable
fun OptionsDialogPreview() {
    var isPreparationEnabled by remember { mutableStateOf(true) }
    var preparationSeconds by remember { mutableIntStateOf(5) }
    var inputMinutes by remember { mutableStateOf("1") }
    var inputSeconds by remember { mutableStateOf("30") }
    var correctScoreValue by remember { mutableIntStateOf(2) }
    var incorrectScoreValue by remember { mutableIntStateOf(-1) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Options") },
        text = {
            Column {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Score Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Correct answer", modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = correctScoreValue.toString(),
                                onValueChange = {},
                                singleLine = true,
                                modifier = Modifier.width(64.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Incorrect answer", modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = incorrectScoreValue.toString(),
                                onValueChange = {},
                                singleLine = true,
                                modifier = Modifier.width(64.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPreparationEnabled, onCheckedChange = { isPreparationEnabled = it })
                    Text("Enable Preparation Phase")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Preparation Seconds: ")
                    OutlinedTextField(
                        value = preparationSeconds.toString(),
                        onValueChange = {},
                        singleLine = true,
                        modifier = Modifier.width(48.dp),
                        enabled = isPreparationEnabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Set round duration: ")
                    OutlinedTextField(
                        value = inputMinutes,
                        onValueChange = {},
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(64.dp)
                    )
                    OutlinedTextField(
                        value = inputSeconds,
                        onValueChange = {},
                        label = { Text("Sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(64.dp)
                    )
                }
            }
        },
        confirmButton = { Button(onClick = {}) { Text("Apply and Close") } }
    )
}

@Preview(showBackground = true)
@Composable
fun AnswerDialogPreview() {
    val winnerId = "BigRedB_1"
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Was the answer correct?") },
        text = { Text("Button: $winnerId") },
        confirmButton = { Button(onClick = {}) { Text("Yes") } },
        dismissButton = { Button(onClick = {}) { Text("No") } }
    )
}

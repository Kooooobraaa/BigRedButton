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
    val blockedDevices = remember { mutableStateOf(mutableSetOf<String>()) }
    var roundNumber by remember { mutableIntStateOf(1) }

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
            }

            if (showScoreDialog) {
                AlertDialog(
                    onDismissRequest = { showScoreDialog = false },
                    title = { Text("Scores") },
                    text = {
                        Column {
                            deviceStates.forEach { device ->
                                val score = deviceScores[device.name] ?: 0
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(device.name, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { deviceScores[device.name] = score - 1 }) {
                                        Text("-")
                                    }
                                    Text(score.toString(), modifier = Modifier.width(32.dp), fontSize = 20.sp)
                                    IconButton(onClick = { deviceScores[device.name] = score + 1 }) {
                                        Text("+")
                                    }
                                    Button(onClick = { deviceScores[device.name] = 0 }, modifier = Modifier.padding(start = 8.dp)) {
                                        Text("Reset")
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isPreparationEnabled,
                                    onCheckedChange = { isPreparationEnabled = it }
                                )
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
                            // Add 2 points for correct answer
                            deviceScores[winnerId] = (deviceScores[winnerId] ?: 0) + 2
                            showAnswerDialog = false
                            winnerId = ""
                        }) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            // Subtract 1 point for wrong answer and block device
                            deviceScores[winnerId] = (deviceScores[winnerId] ?: 0) - 1
                            blockedDevices.value.add(winnerId)
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
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("Round: $roundNumber", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    roundNumber++
                    blockedDevices.value.clear()
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
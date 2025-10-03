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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            }
        } else {
            countDownTimer?.cancel()
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
                Text(
                    text = formattedTime,
                    fontSize = 72.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    val startPauseBgColor = if (isTimerRunning) Color(0xFFFFCC80) else Color(0xFF4CAF50)
                    val startPauseText = if (isTimerRunning) "Pause" else "Start"
                    val startPauseTextColor = Color.Black
                    val resetBgColor = Color(0xFFFF4E5F)
                    val resetTextColor = Color.Black
                    Button(
                        onClick = {
                            if (isTimerRunning) pauseTimer() else startTimer()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = startPauseBgColor)
                    ) {
                        Text(startPauseText, color = startPauseTextColor)
                    }
                    Button(
                        onClick = { resetTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = resetBgColor)
                    ) {
                        Text("Reset", color = resetTextColor)
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
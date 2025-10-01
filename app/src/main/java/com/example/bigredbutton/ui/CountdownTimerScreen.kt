package com.example.bigredbutton.ui

import android.media.MediaPlayer
import android.os.CountDownTimer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigredbutton.BluetoothMessageManager
import com.example.bigredbutton.R
import com.example.bigredbutton.models.DeviceState
import com.example.bigredbutton.util.formatMillisecondsToTimeWithMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val DEFAULT_INITIAL_MINUTES = 2L

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun CountdownTimerScreen(
    onNavigateBack: () -> Unit,
    deviceStates: List<DeviceState>,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputMinutes by remember { mutableStateOf("") }
    var inputSeconds by remember { mutableStateOf("") }
    var initialTimeMillis by remember { mutableLongStateOf(DEFAULT_INITIAL_MINUTES * 60 * 1000L) }

    var timeLeftInMillis by remember { mutableLongStateOf(initialTimeMillis) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var countDownTimer: CountDownTimer? by remember { mutableStateOf(null) }

    var winnerId by remember { mutableStateOf("") }
    val latestBluetoothMessage by BluetoothMessageManager.latestMessage.collectAsState()

    val formattedTime = formatMillisecondsToTimeWithMillis(timeLeftInMillis)
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    fun playEndSound() {
        try {
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

    LaunchedEffect(isTimerRunning, latestBluetoothMessage) {
        if (isTimerRunning && hasStarted && winnerId.isEmpty() && !latestBluetoothMessage.isNullOrEmpty()) {
            winnerId = latestBluetoothMessage!!
            BluetoothMessageManager.clearMessage()
            BluetoothMessageManager.sendMessageToAll(winnerId)
            pauseTimer()
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

    val backgroundColor = when {
        isTimerRunning -> Color(0xFF4CAF50)
        !isTimerRunning && timeLeftInMillis == 0L && hasStarted -> Color(0xFFFF4E5F)
        !isTimerRunning && hasStarted -> Color(0xFFFFF9C4)
        else -> Color.White
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Countdown Timer") },
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
                    Button(
                        onClick = {
                            if (isTimerRunning) pauseTimer() else {
                                if (timeLeftInMillis > 0) startTimer()
                            }
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
                    Button(
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
                                Text("Set timer duration: ")
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CountdownTimerScreenPreview() {
    val mockDevices = listOf(
        com.example.bigredbutton.models.DeviceState(device = null, name = "BigRedB_1", address = "00:11:22:33:44:55", isConnected = true),
        com.example.bigredbutton.models.DeviceState(device = null, name = "BigRedB_2", address = "Unavailable", isConnected = false),
        com.example.bigredbutton.models.DeviceState(device = null, name = "BigRedB_3", address = "Unavailable", isConnected = false)
    )

    com.example.bigredbutton.ui.theme.BigRedButtonTheme {
        CountdownTimerScreen(
            onNavigateBack = {},
            deviceStates = mockDevices,
            updateDeviceState = { _, _ -> }
        )
    }
}

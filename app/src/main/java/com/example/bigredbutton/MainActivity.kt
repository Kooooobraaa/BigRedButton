package com.example.bigredbutton // Replace with your actual package name

import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigredbutton.ui.theme.BigRedButtonTheme
import kotlin.math.max

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BigRedButtonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimerScreen()
                }
            }
        }
    }
}

// Initial time for the timer (e.g., 2 minutes)
const val DEFAULT_INITIAL_MINUTES = 2L


@Composable
fun TimerScreen() {
    // Input state for user-defined minutes
    var inputMinutes by remember { mutableStateOf(DEFAULT_INITIAL_MINUTES.toString())}
    var initialTimeMillis by remember { mutableStateOf(DEFAULT_INITIAL_MINUTES * 60 * 1000L)}

    // Timer state
    var timeLeftInMillis by remember { mutableStateOf(initialTimeMillis) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var countDownTimer: CountDownTimer? by remember { mutableStateOf(null) }

    val formattedTime = formatTime(timeLeftInMillis)
    val focusManager = LocalFocusManager.current

    fun startTimer() {
        countDownTimer?.cancel() // Cancel any existing timer
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                isTimerRunning = false
                // Optionally, add a sound or vibration here
            }
        }.start()
        isTimerRunning = true
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = initialTimeMillis // Reset to the user-set or default initial time
        isTimerRunning = false
    }

    fun setTimerDuration() {
        val minutes = inputMinutes.toLongOrNull() ?: DEFAULT_INITIAL_MINUTES
        initialTimeMillis = max(1, minutes) * 60 * 1000L // ensure at least 1 minute
        resetTimer() // Reset the timer to the new duration
        focusManager.clearFocus() // Hide keyboard
    }

    // Ensure timer is properly cancelled when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            countDownTimer?.cancel()
        }
    }

    // Reset timeLeftInMillis if initialTimeMillis changes and timer is not running
    LaunchedEffect(initialTimeMillis) {
        if (!isTimerRunning) {
            timeLeftInMillis = initialTimeMillis
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Input section for setting timer duration
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            OutlinedTextField(
                value = inputMinutes,
                onValueChange = {newValue ->
                    // Allow only digits and limit length if desired
                    if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                        inputMinutes = newValue
                    }
                },
                label = { Text("Minutes") },
                keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done // Show "Done" on keyboard
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                enabled = !isTimerRunning // Disable input while timer is running
            )
            Button(
                onClick = {setTimerDuration()},
                enabled = !isTimerRunning // Disable set while timer is running
            ) {
                Text("Set")
            }
        }
        Text(
            text = formattedTime,
            fontSize = 72.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (isTimerRunning) {
                        pauseTimer()
                    } else {
                        if (timeLeftInMillis > 0) { // Only start if there's time left
                            startTimer()
                        }
                    }
                },
                enabled = timeLeftInMillis > 0 // Disable start/pause if timer is at 0
            ) {
                Text(if (isTimerRunning) "Pause" else "Start")
            }
            Button(
                onClick = { resetTimer() }
            ) {
                Text("Reset")
            }
        }
    }
}

// Helper function to format milliseconds into MM:SS
fun formatTime(millis: Long): String {
    val minutes = (millis / 1000) / 60
    val seconds = (millis / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BigRedButtonTheme {
        TimerScreen()
    }
}
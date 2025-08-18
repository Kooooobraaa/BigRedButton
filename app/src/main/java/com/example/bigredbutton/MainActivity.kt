package com.example.bigredbutton // Replace with your actual package name

import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigredbutton.ui.theme.BigRedButtonTheme

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

@Composable
fun TimerScreen() {
    // Timer state
    var timeLeftInMillis by remember { mutableStateOf(INITIAL_TIME_MILLIS) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var countDownTimer: CountDownTimer? by remember { mutableStateOf(null) }

    val formattedTime = formatTime(timeLeftInMillis)

    fun startTimer() {
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
        timeLeftInMillis = INITIAL_TIME_MILLIS
        isTimerRunning = false
    }

    // Ensure timer is properly cancelled when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            countDownTimer?.cancel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

// Initial time for the timer (e.g., 5 minutes)
const val INITIAL_TIME_MILLIS = 5 * 60 * 1000L // 5 minutes in milliseconds


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BigRedButtonTheme {
        TimerScreen()
    }
}
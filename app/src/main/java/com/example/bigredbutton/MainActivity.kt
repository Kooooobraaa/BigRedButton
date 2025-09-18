package com.example.bigredbutton // Replace with your actual package name

import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigredbutton.ui.theme.BigRedButtonTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max

enum class Screen {
    Home,
    GamesSelection,
    CountdownTimer,
    Stopwatch,
    ConnectButtons
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BigRedButtonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigator() // Main navigator composable
                }
            }
        }
    }
}

// --- App Navigator ---
@Composable
fun AppNavigator() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onConnectButtons = { currentScreen = Screen.ConnectButtons },
            onGames = { currentScreen = Screen.GamesSelection }
        )
        Screen.ConnectButtons -> ConnectButtonsScreen(
            onNavigateBack = { currentScreen = Screen.Home }
        )
        Screen.GamesSelection -> GamesSelectionScreen(
            onCountdownSelected = { currentScreen = Screen.CountdownTimer },
            onStopwatchSelected = { currentScreen = Screen.Stopwatch },
            onNavigateBack = { currentScreen = Screen.Home }
        )
        Screen.CountdownTimer -> CountdownTimerScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection }
        )
        Screen.Stopwatch -> StopwatchScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection }
        )
    }
}

@Composable
fun HomeScreen(
    onConnectButtons: () -> Unit,
    onGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Big Red Button Game App", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConnectButtons,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Connect Buttons")
        }
        Button(
            onClick = onGames,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Games")
        }
    }
}

// --- Connect Buttons Screen Composable ---
@Composable
fun StatusIcon(isConnected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                shape = CircleShape
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectButtonsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Placeholder connection status variables
    val isButton1Connected = false
    val isButton2Connected = false
    val isButton3Connected = false

    // Added Scaffold for TopAppBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Buttons")},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(modifier = modifier.border(1.dp, Color.Gray).padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Button 1 ready")
                StatusIcon(isConnected = isButton1Connected)
            }
            Column(modifier = modifier.border(1.dp, Color.Gray).padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Button 2 ready")
                StatusIcon(isConnected = isButton2Connected)
            }
            Column(modifier = modifier.border(1.dp, Color.Gray).padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Button 3 ready")
                StatusIcon(isConnected = isButton3Connected)
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp), // Additional screen padding
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Connect Buttons Screen", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Functionality to connect external buttons will be implemented here.", style = MaterialTheme.typography.bodyMedium)
        }

    }

}
// --- Selection Screen Composable ---
@Composable
fun GamesSelectionScreen(
    onCountdownSelected: () -> Unit,
    onStopwatchSelected: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onCountdownSelected,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Countdown")
        }
        Button(
            onClick = onStopwatchSelected,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Stopwatch")
        }
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

// --- Countdown Timer Screen Composable ---
// Initial time for the timer (e.g., 2 minutes)
const val DEFAULT_INITIAL_MINUTES = 2L

@OptIn(ExperimentalMaterial3Api::class) // For OutlinedTextField
@Composable
fun CountdownTimerScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    // Input state for user-defined minutes
    var inputMinutes by remember { mutableStateOf(DEFAULT_INITIAL_MINUTES.toString()) }
    var initialTimeMillis by remember { mutableLongStateOf(DEFAULT_INITIAL_MINUTES * 60 * 1000L) }

    // Timer state
    var timeLeftInMillis by remember { mutableLongStateOf(initialTimeMillis) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var countDownTimer: CountDownTimer? by remember { mutableStateOf(null) }

    val formattedTime = formatMillisecondsToTime(timeLeftInMillis) // Using generic formatter
    val focusManager = LocalFocusManager.current

    fun startTimer() {
        countDownTimer?.cancel() // Cancel any existing timer
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 100) {
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

    // Added Scaffold for TopAppBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Countdown Timer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp), // Additional screen padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                OutlinedTextField(
                    value = inputMinutes,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                            inputMinutes = newValue
                        }
                    },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = !isTimerRunning
                )
                Button(
                    onClick = { setTimerDuration() },
                    enabled = !isTimerRunning
                ) {
                    Text("Set")
                }
            }

            Text(
                text = formattedTime,
                fontSize = 72.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp) // Added padding for tenths of a second
            )
            // Display tenths of a second for countdown
            Text(
                text = String.format(Locale.getDefault(), ".%01d", (timeLeftInMillis % 1000) / 100),
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (isTimerRunning) pauseTimer() else {
                            if (timeLeftInMillis > 0) startTimer()
                        }
                    },
                    enabled = timeLeftInMillis > 0
                ) {
                    Text(if (isTimerRunning) "Pause" else "Start")
                }
                Button(onClick = { resetTimer() }) {
                    Text("Reset")
                }
            }
        }
    }
}

// --- Stopwatch Screen Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    var elapsedTime by remember { mutableLongStateOf(0L) } // Time in milliseconds
    var isRunning by remember { mutableStateOf(false) }
    // Store the time when the stopwatch was started, or the point from which it was resumed
    var timeReference by remember { mutableLongStateOf(0L) }

    // This LaunchedEffect will run the stopwatch logic when isRunning is true
    LaunchedEffect(isRunning) {
        if (isRunning) {
            // If starting from 0 or resuming, set the reference point
            timeReference = if (elapsedTime == 0L) { // Starting fresh or after reset
                SystemClock.elapsedRealtime()
            } else { // Resuming
                // Adjust timeReference to account for the already elapsed time before pausing
                SystemClock.elapsedRealtime() - elapsedTime
            }

            while (isRunning) {
                elapsedTime = SystemClock.elapsedRealtime() - timeReference
                delay(10) // Update UI roughly every 10 milliseconds for smooth display
            }
        }
    }

    fun startStopwatch() {
        isRunning = true
    }

    fun pauseStopwatch() {
        isRunning = false
    }

    fun resetStopwatch() {
        isRunning = false
        elapsedTime = 0L
    }

    val formattedStopwatchTime = formatMillisecondsToTimeWithMillis(elapsedTime)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stopwatch") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp),      // Additional screen padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formattedStopwatchTime.substringBeforeLast('.'), // MM:SS
                fontSize = 72.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "." + formattedStopwatchTime.substringAfterLast('.', "00"), // .ss (milliseconds)
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp) // Align decimal better
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (isRunning) {
                            pauseStopwatch()
                        } else {
                            startStopwatch()
                        }
                    }
                ) {
                    Text(if (isRunning) "Pause" else if (elapsedTime > 0L) "Resume" else "Start")
                }
                Button(
                    onClick = { resetStopwatch() },
                    enabled = elapsedTime > 0L // Enable reset only if time has passed or it's running
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

// --- Helper function to format milliseconds into MM:SS ---
// Used by Countdown Timer
fun formatMillisecondsToTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds)
}

// --- Helper function to format milliseconds into MM:SS.mm (for Stopwatch) ---
// mm represents hundredths of a second
fun formatMillisecondsToTimeWithMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (millis % 1000) / 10 // Get hundredths of a second
    return String.format(Locale.getDefault(),"%02d:%02d.%02d", minutes, seconds, hundredths)
}


// --- Previews (Optional, but good for development) ---
@Preview(showBackground = true, name = "Home Screen Preview")
@Composable
fun HomeScreenPreview() {
    BigRedButtonTheme {
        HomeScreen(onConnectButtons = {}, onGames = {})
    }
}

@Preview(showBackground = true, name = "Connect Buttons Screen Preview")
@Composable
fun ConnectButtonsScreenPreview() {
    BigRedButtonTheme {
        ConnectButtonsScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true, name = "Selection Screen Preview")
@Composable
fun SelectionScreenPreview() {
    BigRedButtonTheme {
        GamesSelectionScreen(onCountdownSelected = {}, onStopwatchSelected = {}, onNavigateBack = {})
    }
}

@Preview(showBackground = true, name = "Countdown Timer Preview")
@Composable
fun CountdownTimerScreenPreview() {
    BigRedButtonTheme {
        CountdownTimerScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true, name = "Stopwatch Preview")
@Composable
fun StopwatchScreenPreview() {
    BigRedButtonTheme {
        StopwatchScreen(onNavigateBack = {})
    }
}

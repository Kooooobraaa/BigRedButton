package com.example.bigredbutton // Replace with your actual package name

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.provider.Settings
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bigredbutton.ui.theme.BigRedButtonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import java.util.UUID
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
                    val deviceStates = remember { mutableStateListOf<DeviceState>() }

                    AppNavigator(deviceStates = deviceStates,
                        updateDeviceState = { name, update ->
                            val idx = deviceStates.indexOfFirst { it.name == name }
                            if (idx != -1) deviceStates[idx] = update(deviceStates[idx])
                        }) // Main navigator composable
                }
            }
        }
    }
}

// --- App Navigator ---
@Composable
fun AppNavigator(
    deviceStates: List<DeviceState>,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onConnectButtons = { currentScreen = Screen.ConnectButtons },
            onGames = { currentScreen = Screen.GamesSelection }
        )
        Screen.ConnectButtons -> ConnectButtonsScreen(
            onNavigateBack = { currentScreen = Screen.Home },
            onOpenBluetoothSettings = { context ->
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(intent)
            },
            deviceStates = deviceStates,
            updateDeviceState = updateDeviceState
        )
        Screen.GamesSelection -> GamesSelectionScreen(
            onCountdownSelected = { currentScreen = Screen.CountdownTimer },
            onStopwatchSelected = { currentScreen = Screen.Stopwatch },
            onNavigateBack = { currentScreen = Screen.Home }
        )
        Screen.CountdownTimer -> CountdownTimerScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection },
            deviceStates = deviceStates,
            updateDeviceState = updateDeviceState
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

data class DeviceState(
    val device: BluetoothDevice?,
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val socket: BluetoothSocket? = null,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectButtonsScreen(
    onNavigateBack: () -> Unit,
    onOpenBluetoothSettings: (Context) -> Unit,
    modifier: Modifier = Modifier,
    deviceStates: List<DeviceState>? = null,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()
    val bluetoothPermissionGranted = if (!isInPreview) {
        if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    } else false
    val bluetoothManager = if (!isInPreview) context.getSystemService(BluetoothManager::class.java) else null
    val bluetoothAdapter = if (!isInPreview && bluetoothPermissionGranted) bluetoothManager?.adapter else null
    val pairedDevices = if (!isInPreview) {
        remember {
            bluetoothAdapter?.bondedDevices?.filter {
                it.name == "BigRedB_1" || it.name == "BigRedB_2" || it.name == "BigRedB_3"
            } ?: emptyList()
        }
    } else emptyList<BluetoothDevice>()
    val deviceNames = listOf("BigRedB_1", "BigRedB_2", "BigRedB_3")
    val initialDeviceStates = deviceNames.map { name ->
        val device = pairedDevices.find { it.name == name }
        DeviceState(
            device = device,
            name = name,
            address = device?.address ?: "Unavailable"
        )
    }
    val stateList = remember { mutableStateListOf<DeviceState>().apply { addAll(deviceStates ?: initialDeviceStates) } }
    val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun setError(name: String, error: String) {
        updateDeviceState(name) { it.copy(isConnecting = false, isConnected = false, error = error, socket = null) }
    }

    suspend fun connectToDevice(device: BluetoothDevice, name: String) {
        updateDeviceState(name) { it.copy(isConnecting = true, error = null) }
        withContext(Dispatchers.IO) {
            try {
                val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
                withContext(Dispatchers.Main) {
                    updateDeviceState(name) { it.copy(isConnecting = false, isConnected = true, socket = socket, error = null) }
                    // Start listening for messages from this device
                    BluetoothMessageManager.listenForMessages(socket)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    setError(name, "Connection failed: ${e.localizedMessage}")
                }
            }
        }
    }
    suspend fun disconnectFromDevice(device: BluetoothDevice, name: String) {
        // device parameter is unused, but kept for signature compatibility
        updateDeviceState(name) { it.copy(isConnecting = true, error = null) }
        withContext(Dispatchers.IO) {
            try {
                val idx = stateList.indexOfFirst { it.name == name }
                val socket = stateList.getOrNull(idx)?.socket
                socket?.close()
                withContext(Dispatchers.Main) {
                    updateDeviceState(name) { it.copy(isConnecting = false, isConnected = false, socket = null, error = null) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setError(name, "Disconnect failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Buttons") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                stateList.forEach { state ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                        Text(state.name)
                        StatusIcon(isConnected = state.isConnected)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.device == null) {
                            Text("Not Paired", color = Color.Gray)
                            Button(onClick = {}, enabled = false) {
                                Text("Connect")
                            }
                        } else if (state.isConnected) {
                            Button(onClick = { coroutineScope.launch { disconnectFromDevice(state.device, state.name) } }, enabled = !state.isConnecting) {
                                Text("Disconnect")
                            }
                        } else {
                            Button(onClick = { coroutineScope.launch { connectToDevice(state.device, state.name) } }, enabled = !state.isConnecting) {
                                Text(if (state.isConnecting) "Connecting..." else "Connect")
                            }
                        }
                        if (state.error != null) {
                            Text(state.error, color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onOpenBluetoothSettings(context) }) {
                Text("Open Bluetooth Settings")
            }
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
fun CountdownTimerScreen(
    onNavigateBack: () -> Unit,
    deviceStates: List<DeviceState>,
    updateDeviceState: (name: String, update: (DeviceState) -> DeviceState) -> Unit,
    modifier: Modifier = Modifier
    ) {
    // Input state for user-defined minutes
    var inputMinutes by remember { mutableStateOf(DEFAULT_INITIAL_MINUTES.toString()) }
    var inputSeconds by remember { mutableStateOf("0") }
    var initialTimeMillis by remember { mutableLongStateOf(DEFAULT_INITIAL_MINUTES * 60 * 1000L) }


    //var inputMinutes by remember { mutableStateOf(DEFAULT_INITIAL_MINUTES.toString()) }
    //var initialTimeMillis by remember { mutableLongStateOf(DEFAULT_INITIAL_MINUTES * 60 * 1000L) }

    // Timer state
    var timeLeftInMillis by remember { mutableLongStateOf(initialTimeMillis) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var countDownTimer: CountDownTimer? by remember { mutableStateOf(null) }

    // Winner state
    var winnerId by remember { mutableStateOf("") }
    val latestBluetoothMessage by BluetoothMessageManager.latestMessage.collectAsState()

    val formattedTime = formatMillisecondsToTime(timeLeftInMillis) // Using generic formatter
    val focusManager = LocalFocusManager.current

    fun startTimer() {
        countDownTimer?.cancel() // Cancel any existing timer
        BluetoothMessageManager.clearMessage() // Clear any stale Bluetooth message before starting
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
        winnerId = "" // Reset winner when timer starts
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = initialTimeMillis // Reset to the user-set or default initial time
        isTimerRunning = false
        winnerId = "" // Reset winner when timer is reset
        BluetoothMessageManager.sendMessageToAll("reset") // Send reset message to all Arduinos
    }

    fun setTimerDuration() {
        val minutes = inputMinutes.toLongOrNull() ?: 0L
        val seconds = inputSeconds.toLongOrNull()?.coerceIn(0, 59) ?: 0L
        initialTimeMillis = max(1, minutes * 60 + seconds) * 1000L // at least 1 second
        resetTimer()
        focusManager.clearFocus()
    }

    // Bluetooth message winner logic
    LaunchedEffect(isTimerRunning, latestBluetoothMessage) {
        if (isTimerRunning && winnerId.isEmpty() && !latestBluetoothMessage.isNullOrEmpty()) {
            winnerId = latestBluetoothMessage!!
            BluetoothMessageManager.clearMessage()
            // Send winner ID to all connected Arduinos
            BluetoothMessageManager.sendMessageToAll(winnerId)
        }
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
            verticalArrangement = Arrangement.Top
        ) {
            // Device status row
            DeviceStatusRow(deviceStates)

            WinnerDisplay(winnerId)

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
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        //.weight(1f)
                        .width(100.dp),
                    enabled = !isTimerRunning
                )
                OutlinedTextField(
                    value = inputSeconds,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                            inputSeconds = newValue
                        }
                    },
                    label = { Text("Seconds") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .width(100.dp),
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
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formattedStopwatchTime,
                fontSize = 72.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (isRunning) pauseStopwatch() else startStopwatch()
                    }
                ) {
                    Text(if (isRunning) "Pause" else "Start")
                }
                Button(onClick = { resetStopwatch() }) {
                    Text("Reset")
                }
            }
        }
    }
}

// --- Time Formatters ---
fun formatMillisecondsToTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun formatMillisecondsToTimeWithMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (millis % 1000) / 10 // Get tenths of a second
    return String.format(Locale.getDefault(), "%02d:%02d.%01d", minutes, seconds, milliseconds)
}

// -------------------------------------------------------------
// --- Reusable Composable ---
// -------------------------------------------------------------

// --- Game Button Status ---
// Displays connection status icons for multiple devices in a row
@Composable
fun DeviceStatusRow(deviceStates: List<DeviceState>, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        deviceStates.forEach { state ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusIcon(isConnected = state.isConnected)
                Text(state.name, fontSize = 12.sp)
            }
        }
    }
}

// --- Winner Display ---
// Displays the winner ID or a placeholder message
@Composable
fun WinnerDisplay(winnerId: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(bottom = 32.dp)
    ) {
        Text(
            text = "... and the winner is ... ",
            fontSize = 32.sp,
            color = Color.Gray
        )
        Text(
            text = winnerId,
            fontSize = 48.sp,
            color = if (winnerId.isNotEmpty()) Color.Red else Color.Transparent
        )
    }
}


// --- Preview Composable ---
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BigRedButtonTheme {
        HomeScreen(
            onConnectButtons = {},
            onGames = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectButtonsScreenPreview() {
    val mockDevices = listOf(
        DeviceState(
            device = null,
            name = "BigRedB_1",
            address = "00:11:22:33:44:55",
            isConnected = true,
            isConnecting = false
        ),
        DeviceState(
            device = null,
            name = "BigRedB_2",
            address = "Unavailable",
            isConnected = false,
            isConnecting = false
        ),
        DeviceState(
            device = null,
            name = "BigRedB_3",
            address = "Unavailable",
            isConnected = false,
            isConnecting = false
        )
    )

    BigRedButtonTheme {
        ConnectButtonsScreen(
            onNavigateBack = {},
            onOpenBluetoothSettings = {},
            modifier = Modifier,
            deviceStates = mockDevices,
            updateDeviceState = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GamesSelectionScreenPreview() {
    BigRedButtonTheme {
        GamesSelectionScreen(
            onCountdownSelected = {},
            onStopwatchSelected = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CountdownTimerScreenPreview() {
    val mockDevices = listOf(
        DeviceState(
            device = null,
            name = "BigRedB_1",
            address = "00:11:22:33:44:55",
            isConnected = true,
            isConnecting = false
        ),
        DeviceState(
            device = null,
            name = "BigRedB_2",
            address = "Unavailable",
            isConnected = false,
            isConnecting = false
        ),
        DeviceState(
            device = null,
            name = "BigRedB_3",
            address = "Unavailable",
            isConnected = false,
            isConnecting = false
        )
    )
    BigRedButtonTheme {
        CountdownTimerScreen(
            onNavigateBack = {},
            deviceStates = mockDevices,
            updateDeviceState = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StopwatchScreenPreview() {
    BigRedButtonTheme {
        StopwatchScreen(
            onNavigateBack = {}
        )
    }
}

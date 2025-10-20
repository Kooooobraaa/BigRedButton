package com.example.bigredbutton

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.bigredbutton.models.DeviceState
import com.example.bigredbutton.ui.ConnectButtonsScreen
import com.example.bigredbutton.ui.CountdownTimerScreen
import com.example.bigredbutton.ui.GamesSelectionScreen
import com.example.bigredbutton.ui.HomeScreen
import com.example.bigredbutton.ui.QuizGameScreen
import com.example.bigredbutton.ui.StopwatchScreen
import com.example.bigredbutton.ui.theme.BigRedButtonTheme
import com.example.bigredbutton.ui.ReflexGameScreen
import com.example.bigredbutton.ui.ColorWordGameScreen
import com.example.bigredbutton.ui.MemoryGameScreen

enum class Screen {
    Home,
    GamesSelection,
    CountdownTimer,
    Stopwatch,
    ConnectButtons,
    QuizGame,
    ReflexGame,
    ColorWord,
    MemoryGame
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
                    val deviceNames = listOf("BigRedB_1", "BigRedB_2", "BigRedB_3")
                    val deviceStates = remember {
                        mutableStateListOf<DeviceState>().apply{
                            deviceNames.forEach { name ->
                                add(
                                    DeviceState(
                                        device = null,
                                        name = name,
                                        address = "Unavailable"
                                    )
                                )
                            }
                        } }

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
            onNavigateBack = { currentScreen = Screen.Home },
            onQuizGameSelected = { currentScreen = Screen.QuizGame },
            onReflexGameSelected = { currentScreen = Screen.ReflexGame },
            onColorWordSelected = { currentScreen = Screen.ColorWord },
            onMemoryGameSelected = { currentScreen = Screen.MemoryGame }
        )
        Screen.CountdownTimer -> CountdownTimerScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection },
            deviceStates = deviceStates,
            updateDeviceState = updateDeviceState
        )
        Screen.Stopwatch -> StopwatchScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection },
            deviceStates = deviceStates,
            updateDeviceState = updateDeviceState
        )
        Screen.QuizGame -> QuizGameScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection },
            deviceStates = deviceStates,
            updateDeviceState = updateDeviceState
        )
        Screen.ReflexGame -> ReflexGameScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection },
            deviceStates = deviceStates,
            updateDeviceState = updateDeviceState
        )
        Screen.ColorWord -> ColorWordGameScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection },
            deviceStates = deviceStates,
            updateDeviceState = updateDeviceState
        )
        Screen.MemoryGame -> MemoryGameScreen(
            onNavigateBack = { currentScreen = Screen.GamesSelection },
            deviceStates = deviceStates,
            updateDeviceState = updateDeviceState
        )
    }
}

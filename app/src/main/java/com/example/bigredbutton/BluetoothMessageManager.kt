package com.example.bigredbutton

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

object BluetoothMessageManager {
    private val _latestMessage = MutableStateFlow<String?>(null)
    val latestMessage: StateFlow<String?> = _latestMessage

    private val scope = CoroutineScope(Dispatchers.IO)

    fun listenForMessages(socket: BluetoothSocket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("BigRedB_")) {
                        _latestMessage.value = line.trim()
                    }
                }
            } catch (e: Exception) {
                // Exception caught, can log if needed
            }
        }
    }

    fun clearMessage() {
        _latestMessage.value = null
    }
}

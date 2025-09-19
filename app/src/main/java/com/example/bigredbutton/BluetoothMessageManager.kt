package com.example.bigredbutton

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList

object BluetoothMessageManager {
    private val _latestMessage = MutableStateFlow<String?>(null)
    val latestMessage: StateFlow<String?> = _latestMessage

    private val scope = CoroutineScope(Dispatchers.IO)

    // Store all connected sockets
    private val connectedSockets = CopyOnWriteArrayList<BluetoothSocket>()

    fun addSocket(socket: BluetoothSocket) {
        if (!connectedSockets.contains(socket)) {
            connectedSockets.add(socket)
        }
    }

    fun removeSocket(socket: BluetoothSocket) {
        connectedSockets.remove(socket)
    }

    fun sendMessageToAll(message: String) {
        for (socket in connectedSockets) {
            try {
                val out: OutputStream = socket.outputStream
                out.write((message + "\n").toByteArray())
                out.flush()
            } catch (e: Exception) {
                // Optionally remove socket if error occurs
            }
        }
    }

    fun listenForMessages(socket: BluetoothSocket) {
        addSocket(socket)
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
            } finally {
                removeSocket(socket)
            }
        }
    }

    fun clearMessage() {
        _latestMessage.value = null
    }
}

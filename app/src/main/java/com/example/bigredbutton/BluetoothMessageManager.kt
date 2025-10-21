package com.example.bigredbutton

import android.bluetooth.BluetoothSocket
import android.util.Log
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
        try {
            if (!connectedSockets.contains(socket)) {
                connectedSockets.add(socket)
                Log.d("BluetoothMessageMgr", "Socket added: ${socket.remoteDevice?.address}")
            }
        } catch (e: Exception) {
            Log.e("BluetoothMessageMgr", "addSocket failed", e)
        }
    }

    fun removeSocket(socket: BluetoothSocket) {
        try {
            connectedSockets.remove(socket)
            Log.d("BluetoothMessageMgr", "Socket removed: ${socket.remoteDevice?.address}")
        } catch (e: Exception) {
            Log.e("BluetoothMessageMgr", "removeSocket failed", e)
        }
    }

    fun sendMessageToAll(message: String) {
        for (socket in connectedSockets) {
            try {
                val out: OutputStream = socket.outputStream
                out.write((message + "\n").toByteArray())
                out.flush()
            } catch (e: Exception) {
                Log.e("BluetoothMessageMgr", "sendMessageToAll failed for socket: ${socket.remoteDevice?.address}", e)
                // Optionally remove socket if error occurs
            }
        }
    }

    fun listenForMessages(socket: BluetoothSocket) {
        addSocket(socket)
        scope.launch {
            try {
                Log.d("BluetoothMessageMgr", "Listening on socket from: ${socket.remoteDevice?.address}")
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                while (true) {
                    val line = try { reader.readLine() } catch (e: Exception) {
                        Log.e("BluetoothMessageMgr", "readLine failed", e)
                        null
                    } ?: break
                    if (line.startsWith("BigRedB_")) {
                        _latestMessage.value = line.trim()
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothMessageMgr", "listenForMessages coroutine failed", e)
            } finally {
                removeSocket(socket)
            }
        }
    }

    fun clearMessage() {
        _latestMessage.value = null
    }

    // Debug helper: inject a message into latestMessage for local testing without real Bluetooth hardware
    fun debugInjectMessage(message: String) {
        _latestMessage.value = message
    }
}

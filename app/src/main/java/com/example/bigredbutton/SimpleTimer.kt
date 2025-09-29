package com.example.bigredbutton

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

class SimpleTimer(
    private val intervalMillis: Long = 10L,
    private val onTick: (elapsedMillis: Long) -> Unit,
    private val onFinish: (() -> Unit)? = null
) {
    private var isRunning = false
    private var startTime = 0L
    private var elapsedMillis = 0L
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        isRunning = true
        startTime = SystemClock.elapsedRealtime() - elapsedMillis
        tick()
    }

    fun pause() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    fun reset() {
        isRunning = false
        elapsedMillis = 0L
        handler.removeCallbacksAndMessages(null)
        onTick(elapsedMillis)
    }

    private fun tick() {
        if (!isRunning) return
        elapsedMillis = SystemClock.elapsedRealtime() - startTime
        onTick(elapsedMillis)
        handler.postDelayed({ tick() }, intervalMillis)
    }
}


package com.example.bigredbutton

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper

class SimpleCountdownTimer(
    private val totalMillis: Long,
    private val intervalMillis: Long = 100L,
    private val onTick: (millisUntilFinished: Long) -> Unit,
    private val onFinish: () -> Unit
) {
    private var countDownTimer: CountDownTimer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(totalMillis, intervalMillis) {
            override fun onTick(millisUntilFinished: Long) {
                mainHandler.post { onTick(millisUntilFinished) }
            }
            override fun onFinish() {
                mainHandler.post { onFinish() }
            }
        }.start()
    }

    fun pause() {
        countDownTimer?.cancel()
    }

    fun reset() {
        pause()
        mainHandler.post { onTick(totalMillis) }
    }
}

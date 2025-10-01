package com.example.bigredbutton.util

import java.util.Locale

fun formatMillisecondsToTimeWithMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (millis % 1000) / 100 // Get tenths of a second
    return String.format(Locale.getDefault(), "%02d:%02d.%01d", minutes, seconds, milliseconds)
}

fun formatMillisecondsToTimeWithMillisTwoDigits(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (millis % 1000) / 10 // Get hundredths of a second
    return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, milliseconds)
}


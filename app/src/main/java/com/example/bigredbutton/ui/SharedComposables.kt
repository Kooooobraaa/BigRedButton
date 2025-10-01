package com.example.bigredbutton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigredbutton.models.DeviceState

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

@Composable
fun DeviceStatusRow(deviceStates: List<DeviceState>, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        deviceStates.forEach { state ->
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                StatusIcon(isConnected = state.isConnected)
                Text(state.name, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WinnerDisplay(winnerId: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(bottom = 32.dp)
    ) {
        var winnerText = "... and the winner is ..."
        if (winnerId.isNotEmpty()) {
            winnerText = winnerId
        }
        Text(
            text = winnerText,
            fontSize = if(winnerId.isEmpty()) 32.sp else 48.sp,
            color = if (winnerId.isEmpty()) Color.Gray else Color.Red
        )
    }
}


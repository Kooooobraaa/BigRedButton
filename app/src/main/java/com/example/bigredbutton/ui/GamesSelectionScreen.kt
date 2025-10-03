package com.example.bigredbutton.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.example.bigredbutton.ui.theme.BigRedButtonTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesSelectionScreen(
    onCountdownSelected: () -> Unit,
    onStopwatchSelected: () -> Unit,
    onNavigateBack: () -> Unit,
    onQuizGameSelected: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Game") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                onClick = onQuizGameSelected,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Quiz Game")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GamesSelectionScreenPreview() {
    BigRedButtonTheme {
        GamesSelectionScreen(
            onCountdownSelected = {},
            onStopwatchSelected = {},
            onNavigateBack = {},
            onQuizGameSelected = {}
        )
    }
}

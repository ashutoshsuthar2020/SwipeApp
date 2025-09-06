package com.example.swipeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    onEnableFeatureClicked: () -> Unit,
    isServiceActive: Boolean,
    maxInterval: String,
    onMaxIntervalChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onEnableFeatureClicked) {
            Text(if (isServiceActive) "Stop" else "Start")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = maxInterval,
            onValueChange = onMaxIntervalChange,
            label = { Text("Max Swipe Interval (ms)") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Swipe Service is ${if (isServiceActive) "Running" else "Stopped"}")
    }
}

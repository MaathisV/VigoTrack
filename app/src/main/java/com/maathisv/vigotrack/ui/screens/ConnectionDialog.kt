package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionDialog(
    scannedDevices: List<String>, // Replace String with your Device model later
    onDismiss: () -> Unit,
    onDeviceSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Polar Device") },
        text = {
            // This LazyColumn replaces your DeviceListAdapter
            LazyColumn {
                items(scannedDevices) { deviceId ->
                    Text(
                        text = "Device: $deviceId",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(deviceId) }
                            .padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
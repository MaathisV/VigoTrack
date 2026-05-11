package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.Sensor

@Composable
fun ConnectionDialog(
    scannedDevices: List<Sensor>,
    connectedDevicesList: List<String>,
    connectingId: String?,
    onDismiss: () -> Unit,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Device Management") },
        text = {
            // Use weight(1f) and fill = false to make the column handle lists correctly
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {

                // --- SECTION 1: CONNECTED DEVICES ---
                Text(
                    text = "Connected Devices",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (connectedDevicesList.isEmpty()) {
                    Text(
                        text = "No devices currently connected",
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Column {
                        connectedDevicesList.forEach { id ->
                            ListItem(
                                headlineContent = { Text("Polar Watch ($id)") },
                                trailingContent = {
                                    TextButton(onClick = { onDisconnect(id) }) {
                                        Text("Disconnect", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // --- SECTION 2: AVAILABLE DEVICES ---
                Text(
                    text = "Available Nearby",
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                val availableDevices = scannedDevices.filter { it.deviceId !in connectedDevicesList }

                if (availableDevices.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Searching...", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    // weight(1f) allows the list to be scrollable if it gets long
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(availableDevices) { sensor ->
                            val isThisDeviceConnecting = connectingId == sensor.deviceId

                            ListItem(
                                headlineContent = { Text(sensor.name) },
                                supportingContent = {
                                    if (isThisDeviceConnecting) {
                                        Text("Initiating connection...", color = MaterialTheme.colorScheme.secondary)
                                    } else {
                                        Text("ID: ${sensor.deviceId}")
                                    }
                                },
                                trailingContent = {
                                    if (isThisDeviceConnecting) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        Button(onClick = { onConnect(sensor) }) {
                                            Text("Connect")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
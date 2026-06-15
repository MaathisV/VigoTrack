package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Sensor

@Composable
fun ConfigDeviceTab(
    scannedDevices: List<Sensor>,
    connectedDevicesList: List<Sensor>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit,
    onRenameSensor: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Text(
            text = "Appareils connectés",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        var renamingDeviceId by remember { mutableStateOf<String?>(null) }
        var renameText by remember { mutableStateOf("") }

        if (connectedDevicesList.isEmpty()) {
            Text(
                text = "Aucun appareil connecté actuellement",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Column {
                connectedDevicesList.forEach { sensor ->
                    val state = deviceConnectionStates[sensor.deviceId]
                    val isConnecting = state == ConnectionState.CONNECTING
                    val stateSuffix = when (state) {
                        ConnectionState.FEATURES_READY -> " (Prêt)"
                        ConnectionState.CONNECTING -> " (Connexion…)"
                        else -> ""
                    }

                    if (renamingDeviceId == sensor.deviceId) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                label = { Text("Nom personnalisé") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                if (renameText.isNotBlank()) {
                                    onRenameSensor(sensor.deviceId, renameText.trim())
                                }
                                renamingDeviceId = null
                            }) { Text("Enregistrer") }
                            TextButton(onClick = { renamingDeviceId = null }) { Text("Annuler") }
                        }
                    } else {
                        ListItem(
                            headlineContent = {
                                Text("${sensor.effectiveName}$stateSuffix")
                            },
                            supportingContent = { Text("${sensor.vendor.uppercase()} · ID : ${sensor.deviceId}") },
                            trailingContent = {
                                if (isConnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = {
                                            renameText = sensor.effectiveName
                                            renamingDeviceId = sensor.deviceId
                                        }) { Text("Renommer") }
                                        TextButton(onClick = { onDisconnect(sensor.deviceId) }) {
                                            Text("Déconnecter", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Disponibles à proximité",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        val connectedIds = connectedDevicesList.map { it.deviceId }.toSet()
        val availableDevices = scannedDevices.filter { it.deviceId !in connectedIds }

        if (availableDevices.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Recherche…", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(availableDevices) { sensor ->
                    val isThisDeviceConnecting = connectingId == sensor.deviceId

                    ListItem(
                        headlineContent = { Text(sensor.effectiveName) },
                        supportingContent = {
                            if (isThisDeviceConnecting) {
                                Text("Initialisation de la connexion…", color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text("${sensor.vendor.uppercase()} · ID : ${sensor.deviceId}")
                            }
                        },
                        trailingContent = {
                            if (isThisDeviceConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Button(onClick = { onConnect(sensor) }) {
                                    Text("Connecter")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

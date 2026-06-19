package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
    savedDevicesList: List<Sensor>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit,
    onForget: (String) -> Unit,
    onRenameSensor: (String, String) -> Unit,
    availableSettings: Map<String, Map<String, Set<Int>>> = emptyMap(),
    selectedSettings: Map<String, Pair<Int, Int>> = emptyMap(),
    deviceAvailableDataTypes: Map<String, Set<String>> = emptyMap(),
    onSensorSettingsChanged: (String, String, Int, Int) -> Unit = { _, _, _, _ -> },
    onQuerySettings: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Text(
            text = "Appareils enregistrés",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        var expandedDeviceId by remember { mutableStateOf<String?>(null) }

        if (savedDevicesList.isEmpty()) {
            Text(
                text = "Aucun appareil enregistré",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Column {
                savedDevicesList.forEach { sensor ->
                    val state = deviceConnectionStates[sensor.deviceId]
                    val isConnected = state == ConnectionState.CONNECTED || state == ConnectionState.FEATURES_READY
                    val isConnecting = state == ConnectionState.CONNECTING
                    val stateSuffix = when (state) {
                        ConnectionState.FEATURES_READY -> " (Prêt)"
                        ConnectionState.CONNECTED -> " (Connecté)"
                        ConnectionState.CONNECTING -> " (Connexion…)"
                        else -> " (Déconnecté)"
                    }

                    Column {
                        ListItem(
                            headlineContent = { Text("${sensor.effectiveName}$stateSuffix") },
                            supportingContent = { Text("${sensor.vendor.uppercase()} · ID : ${sensor.deviceId}") },
                            trailingContent = {
                                if (isConnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else if (isConnected) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (expandedDeviceId == sensor.deviceId) "▼" else "▶",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(onClick = { onDisconnect(sensor.deviceId) }) {
                                            Text("Déconnecter", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(onClick = { onConnect(sensor) }) {
                                            Text("Connecter")
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        TextButton(onClick = { onForget(sensor.deviceId) }) {
                                            Text("Oublier", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.clickable(enabled = isConnected) {
                                expandedDeviceId = if (expandedDeviceId == sensor.deviceId) null else sensor.deviceId
                                if (expandedDeviceId == sensor.deviceId) {
                                    onQuerySettings(sensor.deviceId)
                                }
                            }
                        )

                        if (isConnected && expandedDeviceId == sensor.deviceId) {
                            SensorSettingsSection(
                                deviceId = sensor.deviceId,
                                sensorName = sensor.effectiveName,
                                onRenameSensor = { name -> onRenameSensor(sensor.deviceId, name) },
                                availableSettings = availableSettings,
                                selectedSettings = selectedSettings,
                                deviceAvailableDataTypes = deviceAvailableDataTypes,
                                onSettingsChanged = onSensorSettingsChanged,
                                isConnected = !isConnecting
                            )
                        }
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

        val savedIds = savedDevicesList.map { it.deviceId }.toSet()
        val availableDevices = scannedDevices.filter { it.deviceId !in savedIds }

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

@Composable
private fun SensorSettingsSection(
    deviceId: String,
    sensorName: String,
    onRenameSensor: (String) -> Unit,
    availableSettings: Map<String, Map<String, Set<Int>>>,
    selectedSettings: Map<String, Pair<Int, Int>>,
    deviceAvailableDataTypes: Map<String, Set<String>>,
    onSettingsChanged: (String, String, Int, Int) -> Unit,
    isConnected: Boolean
) {
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(sensorName) }
    var expandedFeature by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRenaming) {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        onRenameSensor(renameText.trim())
                        isRenaming = false
                    }
                }) { Text("Sauver") }
            } else {
                Text("Nom : $sensorName", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    renameText = sensorName
                    isRenaming = true
                }) { Text("Modifier") }
            }
        }

        Text(
            text = "Paramètres de streaming",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val supportedFeatures = deviceAvailableDataTypes[deviceId] ?: emptySet()
        listOf("ACC", "ECG").filter { it in supportedFeatures }.forEach { feature ->
            val key = "$deviceId:$feature"
            val settingsMap = availableSettings[key]
            val (currentRate, currentRes) = selectedSettings[key] ?: (0 to 0)
            val displayRate = if (currentRate > 0) "$currentRate Hz" else "Max"
            val displayRes = if (currentRes > 0) "${currentRes}-bit" else "Max"

            if (settingsMap != null && isConnected) {
                val isExpanded = expandedFeature == key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFeature = if (isExpanded) null else key }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(feature, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("$displayRate / $displayRes", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isExpanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall)
                }

                if (isExpanded) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        settingsMap["SAMPLE_RATE"]?.let { rates ->
                            Text("Fréquence", style = MaterialTheme.typography.bodySmall)
                            Column(modifier = Modifier.selectableGroup()) {
                                rates.forEach { rate ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = currentRate == rate,
                                                onClick = { onSettingsChanged(deviceId, feature, rate, currentRes) }
                                            )
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = currentRate == rate,
                                            onClick = null
                                        )
                                        Text("$rate Hz", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        settingsMap["RESOLUTION"]?.let { resolutions ->
                            Text("Résolution", style = MaterialTheme.typography.bodySmall)
                            Column(modifier = Modifier.selectableGroup()) {
                                resolutions.forEach { res ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = currentRes == res,
                                                onClick = { onSettingsChanged(deviceId, feature, currentRate, res) }
                                            )
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = currentRes == res,
                                            onClick = null
                                        )
                                        Text("${res}-bit", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(feature, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$displayRate / $displayRes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

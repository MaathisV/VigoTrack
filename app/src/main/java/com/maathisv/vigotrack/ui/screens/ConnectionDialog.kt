package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Sensor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDialog(
    scannedDevices: List<Sensor>,
    connectedDevicesList: List<String>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    patients: List<Patient>,
    currentLogUri: String,
    namingTemplate: String,
    onDismiss: () -> Unit,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit,
    onAddPatient: (String) -> Unit,
    onDeletePatient: (Patient) -> Unit,
    onPickLogFolder: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onResetTemplate: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Devices") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Patients") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Settings") }
                )
            }
        },
        text = {
            when (selectedTab) {
                0 -> DeviceTab(
                    scannedDevices = scannedDevices,
                    connectedDevicesList = connectedDevicesList,
                    deviceConnectionStates = deviceConnectionStates,
                    connectingId = connectingId,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect
                )
                1 -> PatientTab(
                    patients = patients,
                    onAddPatient = onAddPatient,
                    onDeletePatient = onDeletePatient
                )
                2 -> SettingsTab(
                    currentLogUri = currentLogUri,
                    namingTemplate = namingTemplate,
                    onPickLogFolder = onPickLogFolder,
                    onTemplateChange = onTemplateChange,
                    onResetTemplate = onResetTemplate
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DeviceTab(
    scannedDevices: List<Sensor>,
    connectedDevicesList: List<String>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
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
                    val state = deviceConnectionStates[id]
                    val isConnecting = state == ConnectionState.CONNECTING
                    val stateSuffix = when (state) {
                        ConnectionState.FEATURES_READY -> " (Ready)"
                        ConnectionState.CONNECTING -> " (Connecting...)"
                        else -> ""
                    }
                    ListItem(
                        headlineContent = { Text("Polar Device ($id)$stateSuffix") },
                        trailingContent = {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                TextButton(onClick = { onDisconnect(id) }) {
                                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

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
}

@Composable
private fun PatientTab(
    patients: List<Patient>,
    onAddPatient: (String) -> Unit,
    onDeletePatient: (Patient) -> Unit
) {
    var patientName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Text(
            text = "Add New Patient",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("Patient Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (patientName.isNotBlank()) {
                        onAddPatient(patientName.trim())
                        patientName = ""
                    }
                }
            ) {
                Text("Add")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Saved Patients",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (patients.isEmpty()) {
            Text(
                text = "No patients saved yet",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(patients, key = { it.id }) { patient ->
                    ListItem(
                        headlineContent = { Text(patient.name) },
                        supportingContent = { Text("ID: ${patient.id}") },
                        trailingContent = {
                            IconButton(onClick = { onDeletePatient(patient) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete patient",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    currentLogUri: String,
    namingTemplate: String,
    onPickLogFolder: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onResetTemplate: () -> Unit
) {
    var showPlaceholders by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Export Folder",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Data files are saved to the selected folder. Use \"/\" in the template to create subfolders.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = if (currentLogUri.isBlank()) "No folder selected" else currentLogUri,
            onValueChange = {},
            readOnly = true,
            label = { Text("Current Folder URI") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPickLogFolder) {
            Text("Change Export Folder")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "File Naming Template",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = namingTemplate,
            onValueChange = onTemplateChange,
            label = { Text("Template") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onResetTemplate) {
                Text("Reset to Default")
            }
            TextButton(onClick = { showPlaceholders = true }) {
                Text("Placeholders...")
            }
        }
    }

    if (showPlaceholders) {
        AlertDialog(
            onDismissRequest = { showPlaceholders = false },
            title = { Text("Available Placeholders") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("{stage} - Stage name")
                    Text("{patient} - Patient name")
                    Text("{category} - Activity category (BILAN / ACTIVITE)")
                    Text("{activity} - Activity type (e.g. MARCHE, TDM6)")
                    Text("{sensor} - Sensor identifier")
                    Text("{device} - Alias for {sensor}")
                    Text("{tag} - Data stream type (HR, PPI, ACC, ECG)")
                    Text("{date} - Date (YYYY-MM-DD)")
                    Text("{time} - Time (HH-MM-SS)")
                    Text("{datetime} - Combined date_time")
                    Text("{timestamp} - Unix epoch ms")
                    Text("")
                    Text("Use / in your template to create folder levels.")
                    Text("Default: {stage}/{patient}/{category}/{activity}_{datetime}/{sensor}_{tag}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaceholders = false }) {
                    Text("Close")
                }
            }
        )
    }
}

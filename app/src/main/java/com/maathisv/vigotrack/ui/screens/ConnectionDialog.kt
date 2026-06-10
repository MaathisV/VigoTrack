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
import com.maathisv.vigotrack.models.SensorPatientLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDialog(
    scannedDevices: List<Sensor>,
    connectedDevicesList: List<Sensor>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    patients: List<Patient>,
    sensorPatientLinks: List<SensorPatientLink>,
    currentLogUri: String,
    namingTemplate: String,
    showFeatures: Map<String, Boolean>,
    logFeatures: Map<String, Boolean>,
    onDismiss: () -> Unit,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit,
    onRenameSensor: (String, String) -> Unit,
    onAddPatient: (String) -> Unit,
    onDeletePatient: (Patient) -> Unit,
    onPickLogFolder: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onResetTemplate: () -> Unit,
    onCreateSensorPatientLink: (Long?, String, List<String>) -> Unit,
    onDeleteSensorPatientLink: (SensorPatientLink) -> Unit,
    onToggleShowFeature: (String) -> Unit,
    onToggleLogFeature: (String) -> Unit
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
                    text = { Text("Links") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Patients") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
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
                    onDisconnect = onDisconnect,
                    onRenameSensor = onRenameSensor
                )
                1 -> LinksTab(
                    connectedDevicesList = connectedDevicesList,
                    patients = patients,
                    sensorPatientLinks = sensorPatientLinks,
                    onAddLink = onCreateSensorPatientLink,
                    onDeleteLink = onDeleteSensorPatientLink
                )
                2 -> PatientTab(
                    patients = patients,
                    onAddPatient = onAddPatient,
                    onDeletePatient = onDeletePatient
                )
                3 -> SettingsTab(
                    currentLogUri = currentLogUri,
                    namingTemplate = namingTemplate,
                    showFeatures = showFeatures,
                    logFeatures = logFeatures,
                    onPickLogFolder = onPickLogFolder,
                    onTemplateChange = onTemplateChange,
                    onResetTemplate = onResetTemplate,
                    onToggleShowFeature = onToggleShowFeature,
                    onToggleLogFeature = onToggleLogFeature
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
    connectedDevicesList: List<Sensor>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit,
    onRenameSensor: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Text(
            text = "Connected Devices",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        var renamingDeviceId by remember { mutableStateOf<String?>(null) }
        var renameText by remember { mutableStateOf("") }

        if (connectedDevicesList.isEmpty()) {
            Text(
                text = "No devices currently connected",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Column {
                connectedDevicesList.forEach { sensor ->
                    val state = deviceConnectionStates[sensor.deviceId]
                    val isConnecting = state == ConnectionState.CONNECTING
                    val stateSuffix = when (state) {
                        ConnectionState.FEATURES_READY -> " (Ready)"
                        ConnectionState.CONNECTING -> " (Connecting...)"
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
                                label = { Text("Custom name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                if (renameText.isNotBlank()) {
                                    onRenameSensor(sensor.deviceId, renameText.trim())
                                }
                                renamingDeviceId = null
                            }) { Text("Save") }
                            TextButton(onClick = { renamingDeviceId = null }) { Text("Cancel") }
                        }
                    } else {
                        ListItem(
                            headlineContent = {
                                Text("${sensor.effectiveName}$stateSuffix")
                            },
                            supportingContent = { Text("ID: ${sensor.deviceId}") },
                            trailingContent = {
                                if (isConnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = {
                                            renameText = sensor.effectiveName
                                            renamingDeviceId = sensor.deviceId
                                        }) { Text("Rename") }
                                        TextButton(onClick = { onDisconnect(sensor.deviceId) }) {
                                            Text("Disconnect", color = MaterialTheme.colorScheme.error)
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
            text = "Available Nearby",
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
                Text("Searching...", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(availableDevices) { sensor ->
                    val isThisDeviceConnecting = connectingId == sensor.deviceId

                    ListItem(
                        headlineContent = { Text(sensor.effectiveName) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinksTab(
    connectedDevicesList: List<Sensor>,
    patients: List<Patient>,
    sensorPatientLinks: List<SensorPatientLink>,
    onAddLink: (Long?, String, List<String>) -> Unit,
    onDeleteLink: (SensorPatientLink) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Text(
            text = "Patient-Sensor Links",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("New Link") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        if (sensorPatientLinks.isEmpty()) {
            Text(
                text = "No links configured. Add one to auto-link patients in new sessions.",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sensorPatientLinks, key = { it.id }) { link ->
                    val patientName = patients.find { it.id == link.patientId }?.name ?: "Unknown"
                    val sensorName = connectedDevicesList.find { it.deviceId == link.sensorId }?.effectiveName ?: link.sensorId
                    ListItem(
                        headlineContent = { Text("$patientName → $sensorName") },
                        supportingContent = { Text("Features: ${link.features.joinToString(", ")}") },
                        trailingContent = {
                            IconButton(onClick = { onDeleteLink(link) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete link",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddLinkDialog(
            patients = patients,
            sensors = connectedDevicesList,
            onDismiss = { showAddDialog = false },
            onConfirm = { patientId, sensorId, features ->
                onAddLink(patientId, sensorId, features)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLinkDialog(
    patients: List<Patient>,
    sensors: List<Sensor>,
    onDismiss: () -> Unit,
    onConfirm: (Long?, String, List<String>) -> Unit
) {
    var selectedPatientId by remember { mutableStateOf<Long?>(null) }
    var patientDropdownExpanded by remember { mutableStateOf(false) }
    var selectedSensorId by remember { mutableStateOf("") }
    var sensorDropdownExpanded by remember { mutableStateOf(false) }
    var hrEnabled by remember { mutableStateOf(true) }
    var ppiEnabled by remember { mutableStateOf(true) }
    var accEnabled by remember { mutableStateOf(true) }
    var ecgEnabled by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Patient-Sensor Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = patientDropdownExpanded,
                    onExpandedChange = { patientDropdownExpanded = it }
                ) {
                    val selectedPatientName = patients.find { it.id == selectedPatientId }?.name ?: "Select a patient"
                    OutlinedTextField(
                        value = selectedPatientName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Patient") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = patientDropdownExpanded,
                        onDismissRequest = { patientDropdownExpanded = false }
                    ) {
                        patients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text(patient.name) },
                                onClick = {
                                    selectedPatientId = patient.id
                                    patientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = sensorDropdownExpanded,
                    onExpandedChange = { sensorDropdownExpanded = it }
                ) {
                    val selectedSensorName = sensors.find { it.deviceId == selectedSensorId }?.effectiveName ?: selectedSensorId
                    OutlinedTextField(
                        value = selectedSensorName.ifEmpty { "Select a sensor" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sensor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sensorDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sensorDropdownExpanded,
                        onDismissRequest = { sensorDropdownExpanded = false }
                    ) {
                        sensors.forEach { sensor ->
                            DropdownMenuItem(
                                text = { Text(sensor.effectiveName) },
                                onClick = {
                                    selectedSensorId = sensor.deviceId
                                    sensorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Default Features", style = MaterialTheme.typography.labelSmall)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hrEnabled, onCheckedChange = { hrEnabled = it })
                        Text("Heart Rate (HR)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ppiEnabled, onCheckedChange = { ppiEnabled = it })
                        Text("PP Interval (PPI)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = accEnabled, onCheckedChange = { accEnabled = it })
                        Text("Accelerometer (ACC)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ecgEnabled, onCheckedChange = { ecgEnabled = it })
                        Text("ECG")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val features = mutableListOf<String>()
                    if (hrEnabled) features.add("HR")
                    if (ppiEnabled) features.add("PPI")
                    if (accEnabled) features.add("ACC")
                    if (ecgEnabled) features.add("ECG")
                    onConfirm(selectedPatientId, selectedSensorId, features)
                },
                enabled = selectedPatientId != null && selectedSensorId.isNotBlank()
            ) { Text("Create Link") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
    showFeatures: Map<String, Boolean>,
    logFeatures: Map<String, Boolean>,
    onPickLogFolder: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onResetTemplate: () -> Unit,
    onToggleShowFeature: (String) -> Unit,
    onToggleLogFeature: (String) -> Unit
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Visibilité des données",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        val features = listOf("HR", "PPI", "ACC", "ECG")
        val featureLabels = mapOf("HR" to "Fréquence cardiaque", "PPI" to "Intervalle PP", "ACC" to "Accéléromètre", "ECG" to "ECG")

        features.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = featureLabels[feature] ?: feature,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showFeatures[feature] ?: true,
                        onCheckedChange = { onToggleShowFeature(feature) }
                    )
                    Text("Afficher", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = logFeatures[feature] ?: true,
                        onCheckedChange = { onToggleLogFeature(feature) }
                    )
                    Text("Enregistrer", style = MaterialTheme.typography.labelSmall)
                }
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

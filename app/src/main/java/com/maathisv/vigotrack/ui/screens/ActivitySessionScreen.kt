package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityStatus
import com.maathisv.vigotrack.models.Patient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySessionScreen(
    activityId: String,
    homeViewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val allActivities by homeViewModel.activities.collectAsState(initial = emptyList<ActivitySession>())
    val allLiveData by homeViewModel.sensorLiveData.collectAsState()

    // 3. FIX: Don't define 'activity' twice.
    val activity = allActivities.find { it.id == activityId }

    var showLinkModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.activityType?.displayName ?: "Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("<") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activity == null) {
                item { Text("Activity not found") }
            }
            else {
                item { SessionStatusCard(activity) }

                if (activity.status != ActivityStatus.COMPLETED) {
                    item { StartStopControls(activity, homeViewModel) }
                }

                item { Text("Linked Sensors", style = MaterialTheme.typography.titleMedium) }

                if (activity.links.isEmpty()) {
                    item { Text("No sensors linked.") }
                } else {
                    // 4. FIX: ActivityLink is nested inside ActivitySession
                    items(activity.links) { link ->
                        SensorLinkCard(link = link)
                    }
                }

                if (activity.status != ActivityStatus.COMPLETED) {
                    item {
                        Button(
                            onClick = { showLinkModal = true },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text("Configure New Sensor")
                        }
                    }
                }

                if (activity.status != ActivityStatus.SCHEDULED) {
                    item { Text("Live Data", style = MaterialTheme.typography.titleMedium) }
                    items(activity.links) { link ->
                        SensorDataCard(link = link, homeViewModel = homeViewModel)
                    }
                }
            }

        }
    }
    if (showLinkModal) {
        val patients by homeViewModel.patients.collectAsState()
        LinkConfigurationModal(
            activityId = activityId,
            patients = patients,
            homeViewModel = homeViewModel,
            onDismiss = { showLinkModal = false },
            onSave = { patientId, patientName, sensor, features ->
                homeViewModel.addLink(activityId, patientId, patientName, sensor, features)
                showLinkModal = false
            }
        )
    }
}

@Composable
fun SensorDataCard(
    link: ActivitySession.ActivityLink,
    homeViewModel: HomeViewModel
) {
    // 1. Observe the live data map from the ViewModel
    val allLiveData by homeViewModel.sensorLiveData.collectAsState()

    // 2. Extract the data for THIS specific sensor
    val sensorData = allLiveData[link.sensorId]

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Patient: ${link.patientName}", style = MaterialTheme.typography.titleMedium)
            Text(text = "ID: ${link.sensorId}", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Heart Rate Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HR", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${sensorData?.get("HR") ?: "--"}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("bpm", style = MaterialTheme.typography.labelSmall)
                }

                // PPI Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PPI", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${sensorData?.get("PPI") ?: "--"}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("ms", style = MaterialTheme.typography.labelSmall)
                }

                // ACC Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ACC (X,Y,Z)", style = MaterialTheme.typography.labelSmall)
                    val x = sensorData?.get("ACC_X") ?: "0"
                    val y = sensorData?.get("ACC_Y") ?: "0"
                    val z = sensorData?.get("ACC_Z") ?: "0"
                    Text(text = "$x", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "$y", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "$z", style = MaterialTheme.typography.bodyMedium)
                }

                // ECG Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ECG", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${sensorData?.get("ECG") ?: "--"}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text("uV", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkConfigurationModal(
    activityId: String,
    patients: List<Patient>,
    homeViewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, List<String>) -> Unit // (PatientId, PatientName, Sensor, Features)
) {
    var patientName by remember { mutableStateOf("") }
    var selectedPatientId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var patientDropdownExpanded by remember { mutableStateOf(false) }

    val availableSensors by homeViewModel.connectedDevicesList.collectAsState()
    var selectedSensorId by remember { mutableStateOf("") }
    var sensorDropdownExpanded by remember { mutableStateOf(false) }

    var hrEnabled by remember { mutableStateOf(true) }
    var ppiEnabled by remember { mutableStateOf(true) }
    var accEnabled by remember { mutableStateOf(true) }
    var ecgEnabled by remember { mutableStateOf(false) }

    val availableFeatures = remember(selectedSensorId) {
        if (selectedSensorId.isNotBlank()) {
            homeViewModel.getAvailableFeaturesForDevice(selectedSensorId)
        } else {
            emptySet()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Sensor Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = patientDropdownExpanded,
                    onExpandedChange = { patientDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = {
                            patientName = it
                            selectedPatientId = null
                            patientDropdownExpanded = true
                        },
                        label = { Text("Patient Name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = patientDropdownExpanded,
                        onDismissRequest = { patientDropdownExpanded = false }
                    ) {
                        if (patients.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No patients saved yet") },
                                onClick = { patientDropdownExpanded = false }
                            )
                        } else {
                            patients.forEach { patient ->
                                DropdownMenuItem(
                                    text = { Text(patient.name) },
                                    onClick = {
                                        patientName = patient.name
                                        selectedPatientId = patient.id
                                        patientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = sensorDropdownExpanded,
                    onExpandedChange = { sensorDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSensorId.ifEmpty { "Select a connected sensor" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Available Sensors") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sensorDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = sensorDropdownExpanded,
                        onDismissRequest = { sensorDropdownExpanded = false }
                    ) {
                        if (availableSensors.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No sensors found. Connect in Home first.") },
                                onClick = { sensorDropdownExpanded = false }
                            )
                        } else {
                            availableSensors.forEach { sensorId ->
                                DropdownMenuItem(
                                    text = { Text(sensorId) },
                                    onClick = {
                                        selectedSensorId = sensorId
                                        sensorDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Features", style = MaterialTheme.typography.labelSmall)
                if (availableFeatures.isNotEmpty()) {
                    Text("Device supports: ${availableFeatures.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    FeatureCheckbox("Heart Rate (HR)", hrEnabled, enabled = availableFeatures.contains("HR")) { hrEnabled = it }
                    FeatureCheckbox("PP Interval (PPI)", ppiEnabled, enabled = availableFeatures.contains("PPI")) { ppiEnabled = it }
                    FeatureCheckbox("Accelerometer (ACC)", accEnabled, enabled = availableFeatures.contains("ACC")) { accEnabled = it }
                    FeatureCheckbox("ECG", ecgEnabled, enabled = availableFeatures.contains("ECG")) { ecgEnabled = it }
                }
                if (availableFeatures.isEmpty() && selectedSensorId.isNotBlank()) {
                    Text("Feature info not yet available — ensure device is connected long enough", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                    onSave(selectedPatientId, patientName, selectedSensorId, features)
                },
                enabled = selectedSensorId.isNotBlank() && patientName.isNotBlank()
            ) { Text("Link") }
        }
    )
}

@Composable
fun FeatureCheckbox(label: String, isChecked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isChecked && enabled, onCheckedChange = { if (enabled) onCheckedChange(it) }, enabled = enabled)
        Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
    }
}
@Composable
fun SensorLinkCard(link: ActivitySession.ActivityLink) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Patient: ${link.patientName}", style = MaterialTheme.typography.bodyLarge)
            Text("Sensor ID: ${link.sensorId}", style = MaterialTheme.typography.bodySmall)
            Text("Features: ${link.featuresToTrack.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun StartStopControls(activity: ActivitySession, homeViewModel: HomeViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { homeViewModel.toggleSession(activity) },
            colors = if (activity.isRunning)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else ButtonDefaults.buttonColors(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if (activity.isRunning) "STOP SESSION" else "START SESSION")
        }
    }
}

@Composable
fun SessionStatusCard(activity: ActivitySession) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Status: ${activity.status}",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(text = "Activity ID: ${activity.id}")
        }
    }
}
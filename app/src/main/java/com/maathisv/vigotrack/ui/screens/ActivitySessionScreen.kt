package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySessionScreen(
    activityId: String,
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    onTypeChanged: (String) -> Unit = {}
) {
    val allActivities by homeViewModel.activities.collectAsState(initial = emptyList())
    val allLiveData by homeViewModel.sensorLiveData.collectAsState()
    val connectedSensors by homeViewModel.connectedDevicesList.collectAsState()
    val preLinks by homeViewModel.sensorPatientLinks.collectAsState()
    val patients by homeViewModel.patients.collectAsState()

    val activity = allActivities.find { it.id == activityId }
    var showLinkModal by remember { mutableStateOf(false) }

    val bilanTypes = ActivityType.entries.filter { it.category == ActivityCategory.BILAN }
    val activiteTypes = ActivityType.entries.filter { it.category == ActivityCategory.ACTIVITE }

    var currentType by remember(activity) { mutableStateOf(activity?.activityType) }

    val checkedSensorIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(activityId) {
        activity?.links?.forEach { link ->
            checkedSensorIds[link.sensorId] = true
        }
        preLinks.forEach { preLink ->
            if (!checkedSensorIds.containsKey(preLink.sensorId)) {
                checkedSensorIds[preLink.sensorId] = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (activity?.isRunning == true) "En cours"
                        else currentType?.displayName ?: "Session"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("<") }
                }
            )
        }
    ) { padding ->
        if (activity == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Activity not found") }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SessionStatusCard(activity)
                }

                if (activity.status != ActivityStatus.COMPLETED) {
                    if (activity.activityType.category != ActivityCategory.BILAN) {
                        item {
                            ActivityTypeChips(
                                activiteTypes = activiteTypes,
                                bilanTypes = bilanTypes,
                                selectedType = currentType,
                                onTypeSelected = { newType ->
                                    if (newType != currentType) {
                    if (activity.isRunning) {
                                            homeViewModel.splitActivityOnTypeChange(
                                                activity, newType
                                            ) { newId ->
                                                onTypeChanged(newId)
                                            }
                                        } else {
                                            currentType = newType
                                            homeViewModel.updateActivityType(activity, newType)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item { Text("Patients", style = MaterialTheme.typography.titleMedium) }

                    val displayLinks = buildDisplayLinks(preLinks, activity.links, patients, connectedSensors)

                    if (displayLinks.isEmpty()) {
                        item { Text("No patients linked. Configure a sensor below.", style = MaterialTheme.typography.bodySmall) }
                    } else {
                        items(displayLinks, key = { it.sensorId }) { displayItem ->
                            val isChecked = checkedSensorIds[displayItem.sensorId] ?: false
                            PatientCheckboxRow(
                                displayItem = displayItem,
                                isChecked = isChecked,
                                onCheckedChange = { checked ->
                                    checkedSensorIds[displayItem.sensorId] = checked
                                    if (checked) {
                                        val alreadyLinked = activity.links.any { it.sensorId == displayItem.sensorId }
                                        if (!alreadyLinked) {
                                            homeViewModel.addLink(
                                                activityId,
                                                displayItem.patientId,
                                                displayItem.patientName,
                                                displayItem.sensorId,
                                                displayItem.features
                                            )
                                        }
                if (activity.isRunning) {
                                            homeViewModel.startPatientStream(displayItem.sensorId, displayItem.features)
                                        }
                                    } else {
                                        homeViewModel.stopPatientStream(displayItem.sensorId)
                                        homeViewModel.removeLink(activityId, displayItem.sensorId)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = { showLinkModal = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Configure New Sensor") }
                    }

                    item { StartStopControls(activity, homeViewModel, checkedSensorIds.keys) }
                }

                if (activity.status != ActivityStatus.SCHEDULED) {
                    item { Text("Live Data", style = MaterialTheme.typography.titleMedium) }
                    val activeLinks = activity.links.filter { checkedSensorIds[it.sensorId] == true }
                    if (activeLinks.isEmpty()) {
                        item { Text("No active sensors.", style = MaterialTheme.typography.bodySmall) }
                    } else {
                        items(activeLinks) { link ->
                            val sensorName = connectedSensors.find { it.deviceId == link.sensorId }?.effectiveName
                            SensorDataCard(link = link, sensorName = sensorName, homeViewModel = homeViewModel)
                        }
                    }
                }
            }
        }
    }

    if (showLinkModal) {
        LinkConfigurationModal(
            activityId = activityId,
            patients = patients,
            homeViewModel = homeViewModel,
            onDismiss = { showLinkModal = false },
            onSave = { patientId, patientName, sensorId, features ->
                homeViewModel.addLink(activityId, patientId, patientName, sensorId, features)
                checkedSensorIds[sensorId] = true
                if (activity?.isRunning == true) {
                    homeViewModel.startPatientStream(sensorId, features)
                }
                showLinkModal = false
            }
        )
    }
}

private data class DisplayLink(
    val patientId: Long?,
    val patientName: String,
    val sensorId: String,
    val sensorDisplayName: String?,
    val features: List<String>
)

private fun buildDisplayLinks(
    preLinks: List<SensorPatientLink>,
    activityLinks: List<ActivitySession.ActivityLink>,
    patients: List<Patient>,
    connectedSensors: List<Sensor>
): List<DisplayLink> {
    val result = mutableListOf<DisplayLink>()

    preLinks.forEach { link ->
        val sensor = connectedSensors.find { it.deviceId == link.sensorId }
        val patient = patients.find { it.id == link.patientId }
        result.add(
            DisplayLink(
                patientId = link.patientId,
                patientName = patient?.name ?: "Unknown",
                sensorId = link.sensorId,
                sensorDisplayName = sensor?.effectiveName,
                features = link.features
            )
        )
    }

    activityLinks.forEach { link ->
        if (result.none { it.sensorId == link.sensorId && it.patientId == link.patientId }) {
            val sensor = connectedSensors.find { it.deviceId == link.sensorId }
            result.add(
                DisplayLink(
                    patientId = link.patientId,
                    patientName = link.patientName,
                    sensorId = link.sensorId,
                    sensorDisplayName = sensor?.effectiveName,
                    features = link.featuresToTrack
                )
            )
        }
    }

    return result
}

@Composable
private fun ActivityTypeChips(
    activiteTypes: List<ActivityType>,
    bilanTypes: List<ActivityType>,
    selectedType: ActivityType?,
    onTypeSelected: (ActivityType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Activity Type", style = MaterialTheme.typography.labelMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            activiteTypes.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.displayName) }
                )
            }
        }
    }
}

@Composable
private fun PatientCheckboxRow(
    displayItem: DisplayLink,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(displayItem.patientName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${displayItem.sensorDisplayName ?: displayItem.sensorId} — ${displayItem.features.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SensorDataCard(
    link: ActivitySession.ActivityLink,
    sensorName: String? = null,
    homeViewModel: HomeViewModel
) {
    val allLiveData by homeViewModel.sensorLiveData.collectAsState()

    val sensorData = allLiveData[link.sensorId]

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Patient: ${link.patientName}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Sensor: ${sensorName ?: link.sensorId}", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HR", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${sensorData?.get("HR") ?: "--"}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("bpm", style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PPI", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${sensorData?.get("PPI") ?: "--"}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("ms", style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ACC (X,Y,Z)", style = MaterialTheme.typography.labelSmall)
                    val x = sensorData?.get("ACC_X") ?: "0"
                    val y = sensorData?.get("ACC_Y") ?: "0"
                    val z = sensorData?.get("ACC_Z") ?: "0"
                    Text(text = "$x", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "$y", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "$z", style = MaterialTheme.typography.bodyMedium)
                }

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
    onSave: (Long?, String, String, List<String>) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var selectedPatientId by remember { mutableStateOf<Long?>(null) }
    var patientDropdownExpanded by remember { mutableStateOf(false) }

    val connectedSensors by homeViewModel.connectedDevicesList.collectAsState()
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
                    val selectedSensorName = connectedSensors.find { it.deviceId == selectedSensorId }?.effectiveName ?: selectedSensorId
                    OutlinedTextField(
                        value = selectedSensorName.ifEmpty { "Select a connected sensor" },
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
                        if (connectedSensors.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No sensors found. Connect in Home first.") },
                                onClick = { sensorDropdownExpanded = false }
                            )
                        } else {
                            connectedSensors.forEach { sensor ->
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
fun SensorLinkCard(link: ActivitySession.ActivityLink, sensorName: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Patient: ${link.patientName}", style = MaterialTheme.typography.bodyLarge)
            Text("Sensor: ${sensorName ?: link.sensorId}", style = MaterialTheme.typography.bodySmall)
            Text("Features: ${link.featuresToTrack.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun StartStopControls(activity: ActivitySession, homeViewModel: HomeViewModel, checkedSensorIds: Set<String> = emptySet()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { homeViewModel.toggleSession(activity, checkedSensorIds) },
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
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Status: ${activity.status}",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(text = "Type: ${activity.activityType.displayName}")
            Text(text = "Date: ${dateFormat.format(Date(activity.scheduledDate))}")
        }
    }
}

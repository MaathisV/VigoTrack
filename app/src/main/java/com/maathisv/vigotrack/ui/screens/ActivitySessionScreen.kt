package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.*
import com.maathisv.vigotrack.ui.components.AppTopBar
import com.maathisv.vigotrack.ui.components.MiniGraph
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

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
    val showFeatures by homeViewModel.showFeatures.collectAsState()

    val activity = allActivities.find { it.id == activityId }

    val bilanTypes = ActivityType.entries.filter { it.category == ActivityCategory.BILAN }
    val activiteTypes = ActivityType.entries.filter { it.category == ActivityCategory.ACTIVITE }

    var currentType by remember(activity) { mutableStateOf(activity?.activityType) }

    val checkedSensorIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(activityId) {
        activity?.links?.forEach { link ->
            checkedSensorIds["${link.sensorId}_${link.patientId}"] = true
        }
        preLinks.forEach { preLink ->
            val key = "${preLink.sensorId}_${preLink.patientId}"
            if (!checkedSensorIds.containsKey(key)) {
                checkedSensorIds[key] = true
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (activity?.isRunning == true) "En cours"
                        else currentType?.displayName ?: "Session",
                onBack = onBack
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

                if (activity.status != ActivityStatus.STALE) {
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
                            items(displayLinks, key = { "${it.sensorId}_${it.patientId}" }) { displayItem ->
                                val itemKey = "${displayItem.sensorId}_${displayItem.patientId}"
                                val isChecked = checkedSensorIds[itemKey] ?: false
                                PatientCheckboxRow(
                                    displayItem = displayItem,
                                    isChecked = isChecked,
                                    onCheckedChange = { checked ->
                                        checkedSensorIds[itemKey] = checked
                                        if (checked) {
                                            val alreadyLinked = activity.links.any { it.sensorId == displayItem.sensorId && it.patientId == displayItem.patientId }
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
                                            homeViewModel.removeLink(activityId, displayItem.sensorId, displayItem.patientId)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item { StartStopControls(activity, homeViewModel, checkedSensorIds.keys.toSet()) }
                }

                if (activity.status != ActivityStatus.SCHEDULED) {
                    item { Text("Live Data", style = MaterialTheme.typography.titleMedium) }
                    val activeLinks = activity.links.filter { checkedSensorIds["${it.sensorId}_${it.patientId}"] == true }
                    if (activeLinks.isEmpty()) {
                        item { Text("No active sensors.", style = MaterialTheme.typography.bodySmall) }
                    } else {
                        items(activeLinks) { link ->
                            val sensorName = connectedSensors.find { it.deviceId == link.sensorId }?.effectiveName
                            SensorDataCard(
                                link = link,
                                sensorName = sensorName,
                                sensorData = allLiveData[link.sensorId],
                                showFeatures = showFeatures
                            )
                        }
                    }
                }
            }
        }
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

private data class FeatureData(
    val label: String,
    val valueText: String,
    val unit: String,
    val valueColor: Color,
    val graphValue: Float?,
    val graphColor: Color,
    val maxPoints: Int = 100
)

@Composable
private fun FeatureCell(data: FeatureData, modifier: Modifier) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(70.dp)) {
            Text(data.label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = data.valueText,
                style = MaterialTheme.typography.titleLarge,
                color = data.valueColor
            )
            Text(data.unit, style = MaterialTheme.typography.labelSmall)
        }
        MiniGraph(
            currentValue = data.graphValue,
            lineColor = data.graphColor,
            maxPoints = data.maxPoints,
            modifier = Modifier.weight(1f).height(36.dp)
        )
    }
}

@Composable
fun SensorDataCard(
    link: ActivitySession.ActivityLink,
    sensorName: String? = null,
    sensorData: Map<String, Any>?,
    showFeatures: Map<String, Boolean> = emptyMap(),
    columns: Int = 2
) {
    val hrValue = (sensorData?.get("HR") as? Number)?.toFloat()
    val ppiValue = (sensorData?.get("PPI") as? Number)?.toFloat()
    val ecgValue = (sensorData?.get("ECG") as? Number)?.toFloat()

    val accX = (sensorData?.get("ACC_X") as? Number)?.toFloat() ?: 0f
    val accY = (sensorData?.get("ACC_Y") as? Number)?.toFloat() ?: 0f
    val accZ = (sensorData?.get("ACC_Z") as? Number)?.toFloat() ?: 0f
    val rawMag = sqrt(accX * accX + accY * accY + accZ * accZ)
    val gravityRemoved = if (rawMag >= 1000f) rawMag - 1000f else 1000f - rawMag
    val accMagnitude = gravityRemoved * 9.81f / 1000f

    val features = listOfNotNull(
        if (link.streamHR && showFeatures.getOrDefault("HR", true)) FeatureData(
            label = "HR", valueText = "${hrValue?.toInt() ?: "--"}", unit = "bpm",
            valueColor = MaterialTheme.colorScheme.primary,
            graphValue = hrValue, graphColor = MaterialTheme.colorScheme.primary
        ) else null,
        if (link.streamPPI && showFeatures.getOrDefault("PPI", true)) FeatureData(
            label = "PPI", valueText = "${ppiValue?.toInt() ?: "--"}", unit = "ms",
            valueColor = MaterialTheme.colorScheme.onSurface,
            graphValue = ppiValue, graphColor = MaterialTheme.colorScheme.secondary
        ) else null,
        if (link.streamACC && showFeatures.getOrDefault("ACC", true)) FeatureData(
            label = "ACC", valueText = "%.1f".format(accMagnitude), unit = "m/s²",
            valueColor = MaterialTheme.colorScheme.onSurface,
            graphValue = if (accMagnitude > 0f) accMagnitude else null,
            graphColor = MaterialTheme.colorScheme.tertiary
        ) else null,
        if (link.streamECG && showFeatures.getOrDefault("ECG", true)) FeatureData(
            label = "ECG", valueText = "${ecgValue?.toInt() ?: "--"}", unit = "uV",
            valueColor = MaterialTheme.colorScheme.error,
            graphValue = ecgValue, graphColor = MaterialTheme.colorScheme.error,
            maxPoints = 200
        ) else null
    )

    val rows = features.chunked(columns)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Patient: ${link.patientName}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Sensor: ${sensorName ?: link.sensorId}", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { f -> FeatureCell(f, Modifier.weight(1f)) }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun StartStopControls(activity: ActivitySession, homeViewModel: HomeViewModel, checkedKeys: Set<String> = emptySet()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                when (activity.status) {
                    ActivityStatus.COMPLETED -> homeViewModel.resumeActivity(activity)
                    else -> homeViewModel.toggleSession(activity, checkedKeys)
                }
            },
            colors = when {
                activity.isRunning -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                activity.status == ActivityStatus.COMPLETED -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                else -> ButtonDefaults.buttonColors()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                when (activity.status) {
                    ActivityStatus.COMPLETED -> "REPRENDRE"
                    ActivityStatus.IN_PROGRESS -> "STOP SESSION"
                    else -> "START SESSION"
                }
            )
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

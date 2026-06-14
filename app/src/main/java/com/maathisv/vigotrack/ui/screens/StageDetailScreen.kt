package com.maathisv.vigotrack.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityStatus
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivityType
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Stage
import com.maathisv.vigotrack.ui.components.AppTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageDetailScreen(
    stageId: Long,
    homeViewModel: HomeViewModel,
    onBilanClick: (Long) -> Unit,
    onActivityClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val stages by homeViewModel.stages.collectAsState()
    val stage = stages.find { it.id == stageId }
    val activities by homeViewModel.getActivitiesForStage(stageId).collectAsState(initial = emptyList())

    var showConnectionDialog by remember { mutableStateOf(false) }

    val connectionState by homeViewModel.connectionState.collectAsState()
    val deviceConnectionStates by homeViewModel.deviceConnectionStates.collectAsState()
    val scannedDevices by homeViewModel.scannedDevices.collectAsState()
    val connectedDevicesList by homeViewModel.connectedDevicesList.collectAsState()
    val connectingId by homeViewModel.isConnectingToId.collectAsState()
    val patients by homeViewModel.patients.collectAsState()
    val sensorPatientLinks by homeViewModel.sensorPatientLinks.collectAsState()
    val currentLogUri by homeViewModel.currentLogUri.collectAsState()
    val namingTemplate by homeViewModel.namingTemplate.collectAsState()
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            homeViewModel.updateLogUri(it.toString())
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stage?.name ?: "Stage",
                onBack = onBack,
                onSettingsClick = { showConnectionDialog = true }
            )
        }
    ) { padding ->
        if (stage == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Stage non trouvé") }
        } else {
            StageDetailContent(
                stage = stage,
                activities = activities,
                onBilanClick = { onBilanClick(stageId) },
                onActivityClick = onActivityClick,
                onRecordClick = {
                    val newId = UUID.randomUUID().toString()
                    homeViewModel.createActivityInStage(newId, stage.id, ActivityType.MARCHE)
                    onActivityClick(newId)
                },
                onMarkStale = { activityId -> homeViewModel.markActivityAsStale(activityId) }
            )
        }
    }

    if (showConnectionDialog) {
        val showFeatures by homeViewModel.showFeatures.collectAsState()
        val logFeatures by homeViewModel.logFeatures.collectAsState()
        ConnectionDialog(
            scannedDevices = scannedDevices,
            connectedDevicesList = connectedDevicesList,
            deviceConnectionStates = deviceConnectionStates,
            connectingId = connectingId,
            patients = patients,
            sensorPatientLinks = sensorPatientLinks,
            currentLogUri = currentLogUri,
            namingTemplate = namingTemplate,
            showFeatures = showFeatures,
            logFeatures = logFeatures,
            onDismiss = { showConnectionDialog = false },
            onConnect = { sensor -> homeViewModel.connectToDevice(sensor) },
            onDisconnect = { id -> homeViewModel.disconnectFromDevice(id) },
            onRenameSensor = { deviceId, name -> homeViewModel.renameSensor(deviceId, name) },
            onAddPatient = { name -> homeViewModel.addPatient(name) },
            onDeletePatient = { patient -> homeViewModel.deletePatient(patient) },
            onPickLogFolder = { folderPickerLauncher.launch(null) },
            onTemplateChange = { homeViewModel.updateNamingTemplate(it) },
            onResetTemplate = { homeViewModel.resetNamingTemplate() },
            onCreateSensorPatientLink = { patientId, sensorId, features ->
                homeViewModel.createSensorPatientLink(patientId, sensorId, features)
            },
            onDeleteSensorPatientLink = { link ->
                homeViewModel.deleteSensorPatientLink(link)
            },
            onToggleShowFeature = { feature -> homeViewModel.toggleShowFeature(feature) },
            onToggleLogFeature = { feature -> homeViewModel.toggleLogFeature(feature) }
        )
    }
}

@Composable
private fun StageDetailContent(
    stage: Stage,
    activities: List<ActivitySession>,
    onBilanClick: () -> Unit,
    onActivityClick: (String) -> Unit,
    onRecordClick: () -> Unit,
    onMarkStale: (String) -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEEE dd MMMM", Locale.getDefault()) }

    val activitiesByDay = activities.groupBy { dayFormat.format(Date(it.scheduledDate)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stage.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "${dateFormat.format(Date(stage.startDate))} — ${dateFormat.format(Date(stage.endDate))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Button(
                onClick = onBilanClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) { Text("BILAN", style = MaterialTheme.typography.titleMedium) }
        }

        item {
            Button(
                onClick = onRecordClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Enregistrer", style = MaterialTheme.typography.titleMedium) }
        }

        item {
            Text("Activités", style = MaterialTheme.typography.titleMedium)
        }

        if (activities.isEmpty()) {
            item { Text("Aucune activité pour ce stage.", style = MaterialTheme.typography.bodySmall) }
        } else {
            activitiesByDay.forEach { (day, dayActivities) ->
                item {
                    Text(
                        text = day.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(dayActivities, key = { it.id }) { session ->
                    ActivityRow(session = session, onClick = { onActivityClick(session.id) }, onMarkStale = onMarkStale)
                }
            }
        }

        // Planning section — kept for reference if needed later
        // if (activities.isNotEmpty()) {
        //     item {
        //         HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        //         Text("Planning", style = MaterialTheme.typography.titleMedium)
        //     }
        //     val patientsInStage = activities.flatMap { it.links }
        //         .distinctBy { it.patientId }
        //         .mapNotNull { link -> patients.find { it.id == link.patientId } }
        //     if (patientsInStage.isEmpty()) {
        //         item { Text("Aucun patient lié.", style = MaterialTheme.typography.bodySmall) }
        //     } else {
        //         patientsInStage.forEach { patient ->
        //             val patientActivities = activities.filter { act ->
        //                 act.links.any { it.patientId == patient.id }
        //             }
        //             val total = patientActivities.size
        //             val done = patientActivities.count { it.status == ActivityStatus.COMPLETED }
        //             item {
        //                 PlanningRow(
        //                     patientName = patient.name,
        //                     done = done,
        //                     total = total
        //                 )
        //             }
        //         }
        //     }
        // }
    }
}

@Composable
private fun ActivityRow(session: ActivitySession, onClick: () -> Unit, onMarkStale: (String) -> Unit = {}) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val patientNames = session.links.map { it.patientName }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when (session.status) {
                ActivityStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
                ActivityStatus.SCHEDULED -> MaterialTheme.colorScheme.surface
                ActivityStatus.STALE -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (session.status) {
                    ActivityStatus.COMPLETED -> Icons.Default.CheckCircle
                    ActivityStatus.IN_PROGRESS -> Icons.Default.PlayArrow
                    ActivityStatus.SCHEDULED -> Icons.Default.DateRange
                    ActivityStatus.STALE -> Icons.Default.CheckCircle
                },
                contentDescription = session.status.name,
                modifier = Modifier.size(24.dp),
                tint = when (session.status) {
                    ActivityStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
                    ActivityStatus.SCHEDULED -> MaterialTheme.colorScheme.onSurfaceVariant
                    ActivityStatus.STALE -> MaterialTheme.colorScheme.error
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.activityType.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (session.isStale) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (session.isStale) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "INVALIDÉ",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (session.activityType.category) {
                            ActivityCategory.ACTIVITE -> MaterialTheme.colorScheme.tertiaryContainer
                            ActivityCategory.BILAN -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = session.activityType.category.displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (patientNames.isNotEmpty()) {
                    Text(
                        text = patientNames.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val durationText = formatDuration(session)
                if (durationText != null) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Plus")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (session.isStale) "Déjà invalidé" else "Invalider") },
                        onClick = {
                            showMenu = false
                            if (!session.isStale) onMarkStale(session.id)
                        },
                        enabled = !session.isStale
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = timeFormat.format(Date(session.scheduledDate)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun formatDuration(session: ActivitySession): String? {
    if (session.isRunning) return "En cours…"
    if (session.endTime != null) {
        val diffMs = session.accumulatedTimeMs
        val totalMinutes = diffMs / 60_000
        if (totalMinutes < 1) return "< 1 min"
        return if (totalMinutes < 60) {
            "${totalMinutes}min"
        } else {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (mins > 0) "${hours}h ${mins}min" else "${hours}h"
        }
    }
    return null
}

// PlanningRow kept for reference — remove if no longer needed
// @Composable
// private fun PlanningRow(patientName: String, done: Int, total: Int) {
//     Card(modifier = Modifier.fillMaxWidth()) {
//         Row(
//             modifier = Modifier.padding(12.dp),
//             verticalAlignment = Alignment.CenterVertically,
//             horizontalArrangement = Arrangement.SpaceBetween
//         ) {
//             Text(patientName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
//             Text(
//                 text = "$done / $total",
//                 style = MaterialTheme.typography.bodyMedium,
//                 color = if (done == total) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
//             )
//         }
//     }
// }

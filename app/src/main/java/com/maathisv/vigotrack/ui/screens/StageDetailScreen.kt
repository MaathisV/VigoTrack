package com.maathisv.vigotrack.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityType
import com.maathisv.vigotrack.models.Stage
import com.maathisv.vigotrack.ui.components.ActivityCard
import com.maathisv.vigotrack.ui.components.AppTopBar
import com.maathisv.vigotrack.ui.components.ConfigDialog
import com.maathisv.vigotrack.ui.viewmodel.HomeViewModel
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

    var showConfigDialog by remember { mutableStateOf(false) }

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
                onSettingsClick = { showConfigDialog = true }
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

    if (showConfigDialog) {
        val showFeatures by homeViewModel.showFeatures.collectAsState()
        val logFeatures by homeViewModel.logFeatures.collectAsState()
        ConfigDialog(
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
            onDismiss = { showConfigDialog = false },
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
                    ActivityCard(session = session, onClick = { onActivityClick(session.id) }, onMarkStale = onMarkStale)
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

package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityStatus
import com.maathisv.vigotrack.models.ActivityType
import com.maathisv.vigotrack.ui.components.ActivityTypeChips
import com.maathisv.vigotrack.ui.components.AppTopBar
import com.maathisv.vigotrack.ui.components.CompactSensorDataCard
import com.maathisv.vigotrack.ui.components.EditActivityDialog
import com.maathisv.vigotrack.ui.components.PatientCheckboxRow
import com.maathisv.vigotrack.ui.components.SensorDataCard
import com.maathisv.vigotrack.ui.components.SessionStatusCard
import com.maathisv.vigotrack.ui.components.StartStopControls
import com.maathisv.vigotrack.ui.components.buildDisplayLinks
import com.maathisv.vigotrack.ui.viewmodel.HomeViewModel
import com.maathisv.vigotrack.ui.viewmodel.ServerHealth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySessionScreen(
    activityId: String,
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    onTypeChanged: (String) -> Unit = {}
) {
    val allActivities by homeViewModel.activities.collectAsState(initial = emptyList())
    val allActivityTypes by homeViewModel.activityTypes.collectAsState()
    val allLiveData by homeViewModel.sensorLiveData.collectAsState()
    val connectedSensors by homeViewModel.connectedDevicesList.collectAsState()
    val preLinks by homeViewModel.sensorPatientLinks.collectAsState()
    val patients by homeViewModel.patients.collectAsState()
    val showFeatures by homeViewModel.showFeatures.collectAsState()
    val serverHealth by homeViewModel.serverHealth.collectAsState()

    val activity = allActivities.find { it.id == activityId }

    var currentType by remember(activity) { mutableStateOf(activity?.activityType) }

    val stageActivities = remember(allActivities, activity?.stageId) {
        activity?.stageId?.let { sid -> allActivities.filter { it.stageId == sid } } ?: emptyList()
    }

    val bilanTypes = remember(allActivityTypes, stageActivities) {
        val fromTable = allActivityTypes.filter { it.category == ActivityCategory.BILAN }
        val fromStage = stageActivities.map { it.activityType }.filter { ActivityType.fromName(it.name) == null }
        (fromTable + fromStage).distinctBy { it.name }
    }

    val activiteTypes = remember(allActivityTypes, stageActivities) {
        val fromTable = allActivityTypes.filter { it.category == ActivityCategory.ACTIVITE }
        val fromStage = stageActivities.map { it.activityType }.filter { ActivityType.fromName(it.name) == null }
        (fromTable + fromStage).distinctBy { it.name }
    }

    var editActivity by remember { mutableStateOf<ActivitySession?>(null) }

    val checkedSensorIds = remember { mutableStateMapOf<String, Boolean>() }
    var compactView by remember { mutableStateOf(false) }

    val activityLoaded = activity != null
    LaunchedEffect(activityId, activityLoaded) {
        if (activity != null) {
            activity.links.forEach { link ->
                checkedSensorIds["${link.sensorId}_${link.patientId}"] = true
            }
            if (activity.links.isEmpty()) {
                preLinks.forEach { preLink ->
                    val key = "${preLink.sensorId}_${preLink.patientId}"
                    if (!checkedSensorIds.containsKey(key)) {
                        checkedSensorIds[key] = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (activity?.isRunning == true) "En cours"
                        else activity?.customName ?: currentType?.let { ct -> allActivityTypes.find { it.name == ct.name }?.displayName ?: ct.displayName } ?: "Session",
                onBack = onBack,
                serverHealth = serverHealth
            )
        }
    ) { padding ->
        if (activity == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Activité non trouvée") }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SessionStatusCard(
                        activity = activity,
                        onMarkStale = { homeViewModel.markActivityAsStale(activityId) },
                        onUnmarkStale = { homeViewModel.unmarkActivityAsStale(activityId) },
                        onEdit = { editActivity = it }
                    )
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
                            item { Text("Aucun patient lié. Configurez un capteur ci-dessous.", style = MaterialTheme.typography.bodySmall) }
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
                    item {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !compactView,
                                onClick = { compactView = false },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                label = { Text("Graph") }
                            )
                            SegmentedButton(
                                selected = compactView,
                                onClick = { compactView = true },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                label = { Text("Compact") }
                            )
                        }
                    }
                    item { Text("Données en direct", style = MaterialTheme.typography.titleMedium) }
                    val activeLinks = activity.links.filter { checkedSensorIds["${it.sensorId}_${it.patientId}"] == true }
                    if (activeLinks.isEmpty()) {
                        item { Text("Aucun capteur actif.", style = MaterialTheme.typography.bodySmall) }
                    } else if (compactView) {
                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                activeLinks.forEach { link ->
                                    val sensorName = connectedSensors.find { it.deviceId == link.sensorId }?.effectiveName
                                    CompactSensorDataCard(
                                        link = link,
                                        sensorName = sensorName,
                                        sensorData = allLiveData[link.sensorId],
                                        showFeatures = showFeatures,
                                        modifier = Modifier.width(250.dp)
                                    )
                                }
                            }
                        }
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

    editActivity?.let { activity ->
        EditActivityDialog(
            activity = activity,
            onDismiss = { editActivity = null },
            onConfirm = { name, date ->
                homeViewModel.updateActivityName(activity.id, name)
                homeViewModel.updateActivityDate(activity.id, date)
                editActivity = null
            }
        )
    }
}
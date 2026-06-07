package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityStatus
import com.maathisv.vigotrack.models.ActivityType
import com.maathisv.vigotrack.models.Patient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BilanScreen(
    stageId: Long,
    homeViewModel: HomeViewModel,
    onActivityClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val stages by homeViewModel.stages.collectAsState()
    val stage = stages.find { it.id == stageId }
    val activities by homeViewModel.getActivitiesForStage(stageId).collectAsState(initial = emptyList())
    val patients by homeViewModel.patients.collectAsState()
    val sensorPatientLinks by homeViewModel.sensorPatientLinks.collectAsState()

    val bilanTypes = ActivityType.entries.filter { it.category == ActivityCategory.BILAN }
    val completedMap = remember(activities) { buildCompletedMap(activities) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bilans — ${stage?.name ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("<") }
                }
            )
        }
    ) { padding ->
        if (patients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("No patients registered.") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item { BilanHeaderRow(bilanTypes = bilanTypes) }

                items(patients, key = { it.id }) { patient ->
                    PatientBilanRow(
                        patient = patient,
                        bilanTypes = bilanTypes,
                        completedMap = completedMap,
                        onClick = { bilanType ->
                            val existing = activities.find { a ->
                                a.activityType == bilanType &&
                                a.links.any { it.patientId == patient.id }
                            }
                            if (existing != null) {
                                onActivityClick(existing.id)
                            } else {
                                val preLink = sensorPatientLinks.find { it.patientId == patient.id }
                                homeViewModel.createActivityAndLink(
                                    type = bilanType,
                                    date = System.currentTimeMillis(),
                                    stageId = stageId,
                                    patientId = patient.id,
                                    patientName = patient.name,
                                    sensorId = preLink?.sensorId ?: "",
                                    features = preLink?.features ?: emptyList(),
                                    onCreated = { activityId -> onActivityClick(activityId) }
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
private fun BilanHeaderRow(bilanTypes: List<ActivityType>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Patient",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f)
        )
        bilanTypes.forEach { type ->
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(64.dp)
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun PatientBilanRow(
    patient: Patient,
    bilanTypes: List<ActivityType>,
    completedMap: Map<Pair<Long, String>, Boolean>,
    onClick: (ActivityType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = patient.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        bilanTypes.forEach { type ->
            val isCompleted = completedMap[Pair(patient.id, type.name)] == true
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    .clickable { onClick(type) },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun buildCompletedMap(activities: List<ActivitySession>): Map<Pair<Long, String>, Boolean> {
    val map = mutableMapOf<Pair<Long, String>, Boolean>()
    activities.forEach { activity ->
        if (activity.activityType.category == ActivityCategory.BILAN &&
            activity.status == ActivityStatus.COMPLETED
        ) {
            activity.links.forEach { link ->
                link.patientId?.let { pid ->
                    map[Pair(pid, activity.activityType.name)] = true
                }
            }
        }
    }
    return map
}

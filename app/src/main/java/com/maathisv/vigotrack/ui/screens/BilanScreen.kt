package com.maathisv.vigotrack.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityStatus
import com.maathisv.vigotrack.models.ActivityType
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.ui.components.AppTopBar
import com.maathisv.vigotrack.ui.components.ConfigDialog
import com.maathisv.vigotrack.ui.viewmodel.HomeViewModel

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
    val allActivityTypes by homeViewModel.activityTypes.collectAsState()
    val patients by homeViewModel.patients.collectAsState()
    val sensorPatientLinks by homeViewModel.sensorPatientLinks.collectAsState()

    val bilanTypes = remember(allActivityTypes, activities) {
        val fromTable = allActivityTypes.filter { it.category == ActivityCategory.BILAN }
        val fromStage = activities
            .filter { it.activityType.category == ActivityCategory.BILAN }
            .map { it.activityType }
        (fromTable + fromStage).distinctBy { it.name }
    }
    val completedMap = remember(activities) { buildCompletedMap(activities) }

    var showConfigDialog by remember { mutableStateOf(false) }

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
                title = "Bilans — ${stage?.name ?: ""}",
                onBack = onBack,
                onSettingsClick = { showConfigDialog = true }
            )
        }
    ) { padding ->
        if (patients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Aucun patient enregistré.") }
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
                                !a.isStale &&
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

    ConfigDialog(
        homeViewModel = homeViewModel,
        show = showConfigDialog,
        onDismiss = { showConfigDialog = false },
        onPickLogFolder = { folderPickerLauncher.launch(null) }
    )
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
                        contentDescription = "Terminé",
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
        if (!activity.isStale &&
            activity.activityType.category == ActivityCategory.BILAN &&
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

package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.SensorPatientLink

private val featureLabels = mapOf(
    "HR" to "Fréquence cardiaque (FC)",
    "PPI" to "Intervalle PP (PPI)",
    "ACC" to "Accéléromètre (ACC)",
    "ECG" to "ECG"
)

@Composable
fun ConfigPatientTab(
    patients: List<Patient>,
    connectedDevicesList: List<Sensor>,
    savedSensors: List<Sensor>,
    deviceAvailableDataTypes: Map<String, Set<String>>,
    sensorPatientLinks: List<SensorPatientLink>,
    onAddPatient: (String) -> Unit,
    onDeletePatient: (Patient) -> Unit,
    onCreateLink: (Long?, String, List<String>) -> Unit,
    onDeleteLink: (SensorPatientLink) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var linkDialogPatientId by remember { mutableStateOf<Long?>(null) }
    var deleteLinkTarget by remember { mutableStateOf<SensorPatientLink?>(null) }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Text(
            text = "Ajouter un patient",
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
                label = { Text("Nom du patient") },
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
                Text("Ajouter")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Patients enregistrés",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (patients.isEmpty()) {
            Text(
                text = "Aucun patient enregistré",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(patients, key = { it.id }) { patient ->
                    val patientLinks = sensorPatientLinks.filter { it.patientId == patient.id }

                    ListItem(
                        headlineContent = { Text(patient.name) },
                        supportingContent = {
                            if (patientLinks.isNotEmpty()) {
                                Column {
                                    patientLinks.forEach { link ->
                                        val sensorName = savedSensors.find { it.deviceId == link.sensorId }?.effectiveName ?: link.sensorId
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { deleteLinkTarget = link },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "→ $sensorName: ${link.features.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text("Aucun lien", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { linkDialogPatientId = patient.id }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Lier un capteur",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDeletePatient(patient) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer le patient",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (linkDialogPatientId != null) {
        AddLinkDialog(
            selectedPatientId = linkDialogPatientId,
            sensors = connectedDevicesList,
            deviceAvailableDataTypes = deviceAvailableDataTypes,
            onDismiss = { linkDialogPatientId = null },
            onConfirm = { _, sensorId, features ->
                onCreateLink(linkDialogPatientId, sensorId, features)
                linkDialogPatientId = null
            }
        )
    }

    if (deleteLinkTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteLinkTarget = null },
            title = { Text("Supprimer le lien") },
            text = {
                val link = deleteLinkTarget!!
                val patientName = patients.find { it.id == link.patientId }?.name ?: "Inconnu"
                val sensorName = savedSensors.find { it.deviceId == link.sensorId }?.effectiveName ?: link.sensorId
                Text("Supprimer le lien entre $patientName et $sensorName (${link.features.joinToString(", ")}) ?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteLink(deleteLinkTarget!!)
                    deleteLinkTarget = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteLinkTarget = null }) { Text("Annuler") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLinkDialog(
    selectedPatientId: Long?,
    sensors: List<Sensor>,
    deviceAvailableDataTypes: Map<String, Set<String>>,
    onDismiss: () -> Unit,
    onConfirm: (Long?, String, List<String>) -> Unit
) {
    var selectedSensorId by remember { mutableStateOf("") }
    var sensorDropdownExpanded by remember { mutableStateOf(false) }
    val featureStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(selectedSensorId) {
        featureStates.clear()
        val supported = deviceAvailableDataTypes[selectedSensorId] ?: emptySet()
        featureLabels.keys.filter { it in supported }.forEach { feature ->
            featureStates[feature] = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau lien patient-capteur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sensors.isEmpty()) {
                    Text(
                        "Aucun capteur connecté",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = sensorDropdownExpanded,
                        onExpandedChange = { sensorDropdownExpanded = it }
                    ) {
                        val selectedSensorName = sensors.find { it.deviceId == selectedSensorId }?.effectiveName ?: selectedSensorId
                        OutlinedTextField(
                            value = selectedSensorName.ifEmpty { "Sélectionner un capteur" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Capteur") },
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

                    if (selectedSensorId.isNotBlank()) {
                        val supported = deviceAvailableDataTypes[selectedSensorId] ?: emptySet()
                        if (supported.isEmpty()) {
                            Text(
                                "Aucun flux de données disponible",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("Fonctionnalités", style = MaterialTheme.typography.labelSmall)
                            Column {
                                featureLabels.forEach { (code, label) ->
                                    if (code in supported) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = featureStates[code] ?: false,
                                                onCheckedChange = { featureStates[code] = it }
                                            )
                                            Text(label)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val features = featureStates.filter { it.value }.keys.toList()
                    onConfirm(selectedPatientId, selectedSensorId, features)
                },
                enabled = selectedSensorId.isNotBlank() && featureStates.any { it.value }
            ) { Text("Créer le lien") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

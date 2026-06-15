package com.maathisv.vigotrack.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.SensorPatientLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigLinksTab(
    connectedDevicesList: List<Sensor>,
    patients: List<Patient>,
    sensorPatientLinks: List<SensorPatientLink>,
    onAddLink: (Long?, String, List<String>) -> Unit,
    onDeleteLink: (SensorPatientLink) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Text(
            text = "Liens patient-capteur",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Nouveau lien") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        if (sensorPatientLinks.isEmpty()) {
            Text(
                text = "Aucun lien configuré. Ajoutez-en un pour lier automatiquement les patients dans les nouvelles sessions.",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sensorPatientLinks, key = { it.id }) { link ->
                    val patientName = patients.find { it.id == link.patientId }?.name ?: "Inconnu"
                    val sensorName = connectedDevicesList.find { it.deviceId == link.sensorId }?.effectiveName ?: link.sensorId
                    ListItem(
                        headlineContent = { Text("$patientName → $sensorName") },
                        supportingContent = { Text("Fonctionnalités : ${link.features.joinToString(", ")}") },
                        trailingContent = {
                            IconButton(onClick = { onDeleteLink(link) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer le lien",
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
        title = { Text("Nouveau lien patient-capteur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = patientDropdownExpanded,
                    onExpandedChange = { patientDropdownExpanded = it }
                ) {
                    val selectedPatientName = patients.find { it.id == selectedPatientId }?.name ?: "Sélectionner un patient"
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

                Text("Fonctionnalités par défaut", style = MaterialTheme.typography.labelSmall)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hrEnabled, onCheckedChange = { hrEnabled = it })
                        Text("Fréquence cardiaque (HR)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ppiEnabled, onCheckedChange = { ppiEnabled = it })
                        Text("Intervalle PP (PPI)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = accEnabled, onCheckedChange = { accEnabled = it })
                        Text("Accéléromètre (ACC)")
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
            ) { Text("Créer le lien") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

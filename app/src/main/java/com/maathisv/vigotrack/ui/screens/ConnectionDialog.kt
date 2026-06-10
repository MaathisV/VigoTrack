package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.SensorPatientLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDialog(
    scannedDevices: List<Sensor>,
    connectedDevicesList: List<Sensor>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    patients: List<Patient>,
    sensorPatientLinks: List<SensorPatientLink>,
    currentLogUri: String,
    namingTemplate: String,
    showFeatures: Map<String, Boolean>,
    logFeatures: Map<String, Boolean>,
    onDismiss: () -> Unit,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit,
    onRenameSensor: (String, String) -> Unit,
    onAddPatient: (String) -> Unit,
    onDeletePatient: (Patient) -> Unit,
    onPickLogFolder: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onResetTemplate: () -> Unit,
    onCreateSensorPatientLink: (Long?, String, List<String>) -> Unit,
    onDeleteSensorPatientLink: (SensorPatientLink) -> Unit,
    onToggleShowFeature: (String) -> Unit,
    onToggleLogFeature: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Appareils") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Liens") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Patients") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Paramètres") }
                )
            }
        },
        text = {
            when (selectedTab) {
                0 -> DeviceTab(
                    scannedDevices = scannedDevices,
                    connectedDevicesList = connectedDevicesList,
                    deviceConnectionStates = deviceConnectionStates,
                    connectingId = connectingId,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onRenameSensor = onRenameSensor
                )
                1 -> LinksTab(
                    connectedDevicesList = connectedDevicesList,
                    patients = patients,
                    sensorPatientLinks = sensorPatientLinks,
                    onAddLink = onCreateSensorPatientLink,
                    onDeleteLink = onDeleteSensorPatientLink
                )
                2 -> PatientTab(
                    patients = patients,
                    onAddPatient = onAddPatient,
                    onDeletePatient = onDeletePatient
                )
                3 -> SettingsTab(
                    currentLogUri = currentLogUri,
                    namingTemplate = namingTemplate,
                    showFeatures = showFeatures,
                    logFeatures = logFeatures,
                    onPickLogFolder = onPickLogFolder,
                    onTemplateChange = onTemplateChange,
                    onResetTemplate = onResetTemplate,
                    onToggleShowFeature = onToggleShowFeature,
                    onToggleLogFeature = onToggleLogFeature
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun DeviceTab(
    scannedDevices: List<Sensor>,
    connectedDevicesList: List<Sensor>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit,
    onRenameSensor: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Text(
            text = "Appareils connectés",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        var renamingDeviceId by remember { mutableStateOf<String?>(null) }
        var renameText by remember { mutableStateOf("") }

        if (connectedDevicesList.isEmpty()) {
            Text(
                text = "Aucun appareil connecté actuellement",
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Column {
                connectedDevicesList.forEach { sensor ->
                    val state = deviceConnectionStates[sensor.deviceId]
                    val isConnecting = state == ConnectionState.CONNECTING
                    val stateSuffix = when (state) {
                        ConnectionState.FEATURES_READY -> " (Prêt)"
                        ConnectionState.CONNECTING -> " (Connexion…)"
                        else -> ""
                    }

                    if (renamingDeviceId == sensor.deviceId) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                label = { Text("Nom personnalisé") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                if (renameText.isNotBlank()) {
                                    onRenameSensor(sensor.deviceId, renameText.trim())
                                }
                                renamingDeviceId = null
                            }) { Text("Enregistrer") }
                            TextButton(onClick = { renamingDeviceId = null }) { Text("Annuler") }
                        }
                    } else {
                        ListItem(
                            headlineContent = {
                                Text("${sensor.effectiveName}$stateSuffix")
                            },
                                supportingContent = { Text("ID : ${sensor.deviceId}") },
                            trailingContent = {
                                if (isConnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = {
                                            renameText = sensor.effectiveName
                                            renamingDeviceId = sensor.deviceId
                                        }) { Text("Renommer") }
                                        TextButton(onClick = { onDisconnect(sensor.deviceId) }) {
                                            Text("Déconnecter", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Disponibles à proximité",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        val connectedIds = connectedDevicesList.map { it.deviceId }.toSet()
        val availableDevices = scannedDevices.filter { it.deviceId !in connectedIds }

        if (availableDevices.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Recherche…", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(availableDevices) { sensor ->
                    val isThisDeviceConnecting = connectingId == sensor.deviceId

                    ListItem(
                        headlineContent = { Text(sensor.effectiveName) },
                        supportingContent = {
                            if (isThisDeviceConnecting) {
                                Text("Initialisation de la connexion…", color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text("ID : ${sensor.deviceId}")
                            }
                        },
                        trailingContent = {
                            if (isThisDeviceConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Button(onClick = { onConnect(sensor) }) {
                                    Text("Connecter")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinksTab(
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

@Composable
private fun PatientTab(
    patients: List<Patient>,
    onAddPatient: (String) -> Unit,
    onDeletePatient: (Patient) -> Unit
) {
    var patientName by remember { mutableStateOf("") }

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
                    ListItem(
                        headlineContent = { Text(patient.name) },
                        supportingContent = { Text("ID : ${patient.id}") },
                        trailingContent = {
                            IconButton(onClick = { onDeletePatient(patient) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer le patient",
                                    tint = MaterialTheme.colorScheme.error
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
private fun SettingsTab(
    currentLogUri: String,
    namingTemplate: String,
    showFeatures: Map<String, Boolean>,
    logFeatures: Map<String, Boolean>,
    onPickLogFolder: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onResetTemplate: () -> Unit,
    onToggleShowFeature: (String) -> Unit,
    onToggleLogFeature: (String) -> Unit
) {
    var showPlaceholders by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Dossier d'exportation",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Les fichiers de données sont enregistrés dans le dossier sélectionné. Utilisez \"/\" dans le modèle pour créer des sous-dossiers.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = if (currentLogUri.isBlank()) "Aucun dossier sélectionné" else currentLogUri,
            onValueChange = {},
            readOnly = true,
            label = { Text("URI du dossier actuel") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPickLogFolder) {
            Text("Changer le dossier d'exportation")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Modèle de nommage des fichiers",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = namingTemplate,
            onValueChange = onTemplateChange,
            label = { Text("Modèle") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onResetTemplate) {
                Text("Réinitialiser")
            }
            TextButton(onClick = { showPlaceholders = true }) {
                Text("Espaces réservés…")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Visibilité des données",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        val features = listOf("HR", "PPI", "ACC", "ECG")
        val featureLabels = mapOf("HR" to "Fréquence cardiaque", "PPI" to "Intervalle PP", "ACC" to "Accéléromètre", "ECG" to "ECG")

        features.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = featureLabels[feature] ?: feature,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showFeatures[feature] ?: true,
                        onCheckedChange = { onToggleShowFeature(feature) }
                    )
                    Text("Afficher", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = logFeatures[feature] ?: true,
                        onCheckedChange = { onToggleLogFeature(feature) }
                    )
                    Text("Enregistrer", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showPlaceholders) {
        AlertDialog(
            onDismissRequest = { showPlaceholders = false },
            title = { Text("Espaces réservés disponibles") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("{stage} - Nom du Stage")
                    Text("{patient} - Nom du patient")
                    Text("{category} - Catégorie d'activité (BILAN / ACTIVITÉ)")
                    Text("{activity} - Type d'activité (ex. MARCHE, TDM6)")
                    Text("{sensor} - Identifiant du capteur")
                    Text("{device} - Alias de {sensor}")
                    Text("{tag} - Type de flux de données (HR, PPI, ACC, ECG)")
                    Text("{date} - Date (AAAA-MM-JJ)")
                    Text("{time} - Heure (HH-MM-SS)")
                    Text("{datetime} - Date_heure combinée")
                    Text("{timestamp} - Timestamp Unix (ms)")
                    Text("")
                    Text("Utilisez / dans votre modèle pour créer des niveaux de dossiers.")
                    Text("Défaut : {stage}/{patient}/{category}/{activity}_{datetime}/{sensor}_{tag}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaceholders = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

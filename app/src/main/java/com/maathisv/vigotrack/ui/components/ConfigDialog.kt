package com.maathisv.vigotrack.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.SensorPatientLink
import com.maathisv.vigotrack.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDialog(
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
    availableSettings: Map<String, Map<String, Set<Int>>> = emptyMap(),
    selectedSettings: Map<String, Pair<Int, Int>> = emptyMap(),
    deviceAvailableDataTypes: Map<String, Set<String>> = emptyMap(),
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
    onToggleLogFeature: (String) -> Unit,
    onSensorSettingsChanged: (String, String, Int, Int) -> Unit = { _, _, _, _ -> },
    onQuerySettings: (String) -> Unit = {}
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
                    text = { Text("Patients") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Paramètres") }
                )
            }
        },
        text = {
            when (selectedTab) {
                0 -> ConfigDeviceTab(
                    scannedDevices = scannedDevices,
                    connectedDevicesList = connectedDevicesList,
                    deviceConnectionStates = deviceConnectionStates,
                    connectingId = connectingId,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onRenameSensor = onRenameSensor,
                    availableSettings = availableSettings,
                    selectedSettings = selectedSettings,
                    deviceAvailableDataTypes = deviceAvailableDataTypes,
                    onSensorSettingsChanged = onSensorSettingsChanged,
                    onQuerySettings = onQuerySettings
                )
                1 -> ConfigPatientTab(
                    patients = patients,
                    connectedDevicesList = connectedDevicesList,
                    deviceAvailableDataTypes = deviceAvailableDataTypes,
                    sensorPatientLinks = sensorPatientLinks,
                    onAddPatient = onAddPatient,
                    onDeletePatient = onDeletePatient,
                    onCreateLink = onCreateSensorPatientLink,
                    onDeleteLink = onDeleteSensorPatientLink
                )
                2 -> ConfigSettingsTab(
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
fun ConfigDialog(
    homeViewModel: HomeViewModel,
    show: Boolean,
    onDismiss: () -> Unit,
    onPickLogFolder: () -> Unit
) {
    if (!show) return
    val connectionState by homeViewModel.connectionState.collectAsState()
    val deviceConnectionStates by homeViewModel.deviceConnectionStates.collectAsState()
    val scannedDevices by homeViewModel.scannedDevices.collectAsState()
    val connectedDevicesList by homeViewModel.connectedDevicesList.collectAsState()
    val connectingId by homeViewModel.isConnectingToId.collectAsState()
    val patients by homeViewModel.patients.collectAsState()
    val sensorPatientLinks by homeViewModel.sensorPatientLinks.collectAsState()
    val currentLogUri by homeViewModel.currentLogUri.collectAsState()
    val namingTemplate by homeViewModel.namingTemplate.collectAsState()
    val showFeatures by homeViewModel.showFeatures.collectAsState()
    val logFeatures by homeViewModel.logFeatures.collectAsState()
    val availableSettings by homeViewModel.availableSettings.collectAsState()
    val selectedSettings by homeViewModel.selectedSettings.collectAsState()
    val deviceDataTypes by homeViewModel.deviceAvailableDataTypes.collectAsState()
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
        availableSettings = availableSettings,
        selectedSettings = selectedSettings,
        deviceAvailableDataTypes = deviceDataTypes,
        onDismiss = onDismiss,
        onConnect = { homeViewModel.connectToDevice(it) },
        onDisconnect = { homeViewModel.disconnectFromDevice(it) },
        onRenameSensor = { deviceId, name -> homeViewModel.renameSensor(deviceId, name) },
        onAddPatient = { name -> homeViewModel.addPatient(name) },
        onDeletePatient = { patient -> homeViewModel.deletePatient(patient) },
        onPickLogFolder = onPickLogFolder,
        onTemplateChange = { homeViewModel.updateNamingTemplate(it) },
        onResetTemplate = { homeViewModel.resetNamingTemplate() },
        onCreateSensorPatientLink = { pid, sid, features ->
            homeViewModel.createSensorPatientLink(pid, sid, features)
        },
        onDeleteSensorPatientLink = { link -> homeViewModel.deleteSensorPatientLink(link) },
        onToggleShowFeature = { feature -> homeViewModel.toggleShowFeature(feature) },
        onToggleLogFeature = { feature -> homeViewModel.toggleLogFeature(feature) },
        onSensorSettingsChanged = { deviceId, feature, rate, resolution ->
            homeViewModel.setSensorSettings(deviceId, feature, rate, resolution)
        },
        onQuerySettings = { deviceId -> homeViewModel.queryAvailableSettings(deviceId) }
    )
}

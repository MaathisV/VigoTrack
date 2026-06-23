package com.maathisv.vigotrack.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.SensorPatientLink
import com.maathisv.vigotrack.ui.viewmodel.HomeViewModel
import com.maathisv.vigotrack.ui.viewmodel.ServerHealth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDialog(
    scannedDevices: List<Sensor>,
    connectedDevicesList: List<Sensor>,
    savedDevicesList: List<Sensor>,
    deviceConnectionStates: Map<String, ConnectionState>,
    connectingId: String?,
    patients: List<Patient>,
    sensorPatientLinks: List<SensorPatientLink>,
    currentLogUri: String,
    namingTemplate: String,
    showFeatures: Map<String, Boolean>,
    logFeatures: Map<String, Boolean>,
    serverUrl: String,
    serverFeatures: Map<String, Boolean>,
    serverHealth: ServerHealth = ServerHealth.Unknown,
    authToken: String = "",
    dbName: String = "vigotrack",
    onUpdateAuthToken: (String) -> Unit = {},
    onUpdateDbName: (String) -> Unit = {},
    onScanQrCode: () -> Unit = {},
    onTestServerConnection: () -> Unit = {},
    availableSettings: Map<String, Map<String, Set<Int>>> = emptyMap(),
    selectedSettings: Map<String, Pair<Int, Int>> = emptyMap(),
    deviceAvailableDataTypes: Map<String, Set<String>> = emptyMap(),
    savedSensors: List<Sensor> = emptyList(),
    onDismiss: () -> Unit,
    onConnect: (Sensor) -> Unit,
    onDisconnect: (String) -> Unit,
    onForget: (String) -> Unit,
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
    onToggleServerFeature: (String) -> Unit = {},
    onUpdateServerUrl: (String) -> Unit = {},
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
                    savedDevicesList = savedDevicesList,
                    deviceConnectionStates = deviceConnectionStates,
                    connectingId = connectingId,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onForget = onForget,
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
                    savedSensors = savedSensors,
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
                    serverUrl = serverUrl,
                    serverFeatures = serverFeatures,
                    serverHealth = serverHealth,
                    authToken = authToken,
                    dbName = dbName,
                    onPickLogFolder = onPickLogFolder,
                    onTemplateChange = onTemplateChange,
                    onResetTemplate = onResetTemplate,
                    onToggleShowFeature = onToggleShowFeature,
                    onToggleLogFeature = onToggleLogFeature,
                    onToggleServerFeature = onToggleServerFeature,
                    onUpdateServerUrl = onUpdateServerUrl,
                    onUpdateAuthToken = onUpdateAuthToken,
                    onUpdateDbName = onUpdateDbName,
                    onScanQrCode = onScanQrCode,
                    onTestServerConnection = onTestServerConnection
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
    onPickLogFolder: () -> Unit,
    onForget: (String) -> Unit = { homeViewModel.forgetDevice(it) }
) {
    LaunchedEffect(show) {
        if (show) homeViewModel.startScanning()
    }
    LaunchedEffect(show) {
        if (show) homeViewModel.refreshServerHealth()
    }
    if (!show) return
    val deviceConnectionStates by homeViewModel.deviceConnectionStates.collectAsState()
    val scannedDevices by homeViewModel.scannedDevices.collectAsState()
    val connectedDevicesList by homeViewModel.connectedDevicesList.collectAsState()
    val savedDevicesList by homeViewModel.savedSensorsList.collectAsState()
    val connectingId by homeViewModel.isConnectingToId.collectAsState()
    val patients by homeViewModel.patients.collectAsState()
    val sensorPatientLinks by homeViewModel.sensorPatientLinks.collectAsState()
    val currentLogUri by homeViewModel.currentLogUri.collectAsState()
    val namingTemplate by homeViewModel.namingTemplate.collectAsState()
    val showFeatures by homeViewModel.showFeatures.collectAsState()
    val logFeatures by homeViewModel.logFeatures.collectAsState()
    val serverUrl by homeViewModel.serverUrl.collectAsState()
    val serverFeatures by homeViewModel.serverFeatures.collectAsState()
    val serverHealth by homeViewModel.serverHealth.collectAsState()
    val authToken by homeViewModel.authToken.collectAsState()
    val dbName by homeViewModel.dbName.collectAsState()
    val availableSettings by homeViewModel.availableSettings.collectAsState()
    val selectedSettings by homeViewModel.selectedSettings.collectAsState()
    val deviceDataTypes by homeViewModel.deviceAvailableDataTypes.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    ConfigDialog(
        scannedDevices = scannedDevices,
        connectedDevicesList = connectedDevicesList,
        savedDevicesList = savedDevicesList,
        deviceConnectionStates = deviceConnectionStates,
        connectingId = connectingId,
        patients = patients,
        sensorPatientLinks = sensorPatientLinks,
        currentLogUri = currentLogUri,
        namingTemplate = namingTemplate,
        showFeatures = showFeatures,
        logFeatures = logFeatures,
        serverUrl = serverUrl,
        serverFeatures = serverFeatures,
        serverHealth = serverHealth,
        authToken = authToken,
        dbName = dbName,
        onUpdateAuthToken = { homeViewModel.updateAuthToken(it) },
        onUpdateDbName = { homeViewModel.updateDbName(it) },
        onTestServerConnection = {
            val url = serverUrl
            val token = authToken
            if (url.isNotBlank()) {
                homeViewModel.testServerConnection(url, token) { /* UI update handled by StateFlow */ }
            }
        },
        onScanQrCode = {
            GmsBarcodeScanning.getClient(context)
                .startScan()
                .addOnSuccessListener { barcode ->
                    val raw = barcode.rawValue ?: return@addOnSuccessListener
                    if ("|" in raw) {
                        val parts = raw.split("|", limit = 3)
                        homeViewModel.updateServerUrl(parts[0])
                        if (parts.size >= 2) homeViewModel.updateAuthToken(parts[1])
                        if (parts.size >= 3) homeViewModel.updateDbName(parts[2])
                    } else {
                        homeViewModel.updateAuthToken(raw)
                    }
                }
        },
        availableSettings = availableSettings,
        selectedSettings = selectedSettings,
        deviceAvailableDataTypes = deviceDataTypes,
        savedSensors = savedDevicesList,
        onDismiss = onDismiss,
        onConnect = { homeViewModel.connectToDevice(it) },
        onDisconnect = { homeViewModel.disconnectFromDevice(it) },
        onForget = onForget,
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
        onToggleServerFeature = { feature -> homeViewModel.toggleServerFeature(feature) },
        onUpdateServerUrl = { url -> homeViewModel.updateServerUrl(url) },
        onSensorSettingsChanged = { deviceId, feature, rate, resolution ->
            homeViewModel.setSensorSettings(deviceId, feature, rate, resolution)
        },
        onQuerySettings = { deviceId -> homeViewModel.queryAvailableSettings(deviceId) }
    )
}

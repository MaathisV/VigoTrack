package com.maathisv.vigotrack.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityType
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.ui.components.ActivityCard
import com.maathisv.vigotrack.ui.components.CreateActivityCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onActivityClick: (String) -> Unit
) {
    val activities by viewModel.activities.collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<ActivityType?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val connectedDevicesList by viewModel.connectedDevicesList.collectAsState() // Get the list
    val connectingId by viewModel.isConnectingToId.collectAsState()
    var showConnectionDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // If the user says "Allow", we start the scan immediately
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            viewModel.startScanning()
            showConnectionDialog = true
        }
    }

    val patients by viewModel.patients.collectAsState()
    val currentLogUri by viewModel.currentLogUri.collectAsState()
    val namingTemplate by viewModel.namingTemplate.collectAsState()
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.updateLogUri(it.toString())
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VigoTrack") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // This is your static icon button
                    IconButton(onClick = { showConnectionDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Device Management"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                CreateActivityCard(onClick = { showDialog = true })
            }

            items(activities) { session: ActivitySession ->
                 ActivityCard(session = session, onClick = { onActivityClick(session.id) })
            }
        }
    }

    if (showDialog) {
        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showDialog = false; selectedType = null },
            title = { Text("New Activity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Activités", style = MaterialTheme.typography.titleSmall)
                    ActivityType.entries.filter { it.category == ActivityCategory.ACTIVITE }.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedType = type }
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text(type.displayName)
                        }
                    }

                    HorizontalDivider()

                    Text("Bilans", style = MaterialTheme.typography.titleSmall)
                    ActivityType.entries.filter { it.category == ActivityCategory.BILAN }.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedType = type }
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text(type.displayName)
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = dateFormat.format(Date(selectedDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scheduled Date") },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Select date")
                        }
                    )

                    OutlinedTextField(
                        value = timeFormat.format(Date(selectedDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scheduled Time") },
                        modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedType?.let {
                            viewModel.createActivity(it, selectedDate)
                            showDialog = false
                        }
                    },
                    enabled = selectedType != null
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; selectedType = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    selectedDate = calendar.timeInMillis
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            scannedDevices = scannedDevices,
            connectedDevicesList = connectedDevicesList,
            connectingId = connectingId,
            patients = patients,
            currentLogUri = currentLogUri,
            namingTemplate = namingTemplate,
            onDismiss = { showConnectionDialog = false },
            onConnect = { id -> viewModel.connectToDevice(id) },
            onDisconnect = { id -> viewModel.disconnectFromDevice(id) },
            onAddPatient = { name -> viewModel.addPatient(name) },
            onDeletePatient = { patient -> viewModel.deletePatient(patient) },
            onPickLogFolder = { folderPickerLauncher.launch(null) },
            onTemplateChange = { viewModel.updateNamingTemplate(it) },
            onResetTemplate = { viewModel.resetNamingTemplate() }
        )}
}
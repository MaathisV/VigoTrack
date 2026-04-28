package com.maathisv.vigotrack.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.ui.components.ActivityCard
import com.maathisv.vigotrack.ui.components.CreateActivityCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val activities by viewModel.activities.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newActivityName by remember { mutableStateOf("") }

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
                ActivityCard(session = session)
            }
        }
    }

    // 3. The Pop-up Dialog Logic
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Activity") },
            text = {
                OutlinedTextField(
                    value = newActivityName,
                    onValueChange = { newActivityName = it },
                    label = { Text("Activity Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newActivityName.isNotBlank()) {
                        viewModel.onCreateActivityClicked(newActivityName)
                        newActivityName = "" // clear the text
                        showDialog = false   // close dialog
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            scannedDevices = scannedDevices,
            connectedDevicesList = connectedDevicesList, // Pass it here
            connectingId = connectingId,
            onDismiss = { showConnectionDialog = false },
            onConnect = { id -> viewModel.connectToDevice(id) },
            onDisconnect = { id -> viewModel.disconnectFromDevice(id) }
        )}
}
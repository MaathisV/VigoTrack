package com.maathisv.vigotrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
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

    // Scaffold provides the standard top bar and background
    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    var showConnectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VigoTrack") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Dynamic button based on state
                    when (connectionState) {
                        ConnectionState.CONNECTED -> {
                            Button(onClick = { viewModel.disconnectFromDevice("YOUR_DEVICE_ID") }) {
                                Text("Disconnect")
                            }
                        }
                        ConnectionState.CONNECTING -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        ConnectionState.NOT_CONNECTED -> {
                            IconButton(onClick = { showConnectionDialog = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Connect")
                            }
                        }
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
            onDismiss = { showConnectionDialog = false },
            onDeviceSelected = { deviceId ->
                viewModel.connectToDevice(deviceId)
                showConnectionDialog = false
            }
        )
    }
}
package com.maathisv.vigotrack.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.Stage
import com.maathisv.vigotrack.ui.components.AppTopBar
import com.maathisv.vigotrack.ui.components.ConfigDialog
import com.maathisv.vigotrack.ui.components.StageDialog
import com.maathisv.vigotrack.ui.components.ImportResultDialog
import com.maathisv.vigotrack.ui.components.ImportSummaryDialog
import com.maathisv.vigotrack.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagesListScreen(
    homeViewModel: HomeViewModel,
    onStageClick: (Long) -> Unit
) {
    val stages by homeViewModel.stages.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var stageToEdit by remember { mutableStateOf<Stage?>(null) }
    var stageToDelete by remember { mutableStateOf<Stage?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }

    val importState by homeViewModel.importState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            homeViewModel.startScanning()
        }
    }

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

    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { homeViewModel.parseImportConfig(it) }
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
            AppTopBar(
                title = "Stages",
                onSettingsClick = { showConfigDialog = true }
            )
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Actions")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Créer un stage") },
                        onClick = {
                            showFabMenu = false
                            showCreateDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Importer une config") },
                        onClick = {
                            showFabMenu = false
                            importFilePickerLauncher.launch(arrayOf("application/json"))
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (stages.isEmpty()) {
                item { Text("Aucun stage. Appuyez sur + pour en créer un.") }
            } else {
                items(stages, key = { it.id }) { stage ->
                    StageCard(
                        stage = stage,
                        onClick = { onStageClick(stage.id) },
                        onEdit = { stageToEdit = stage },
                        onDelete = { stageToDelete = stage }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        StageDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, start, end ->
                homeViewModel.createStage(name, start, end)
                showCreateDialog = false
            }
        )
    }

    stageToEdit?.let { stage ->
        StageDialog(
            initialStage = stage,
            onDismiss = { stageToEdit = null },
            onConfirm = { name, start, end ->
                homeViewModel.updateStage(stage.copy(name = name, startDate = start, endDate = end))
                stageToEdit = null
            }
        )
    }

    stageToDelete?.let { stage ->
        AlertDialog(
            onDismissRequest = { stageToDelete = null },
            title = { Text("Supprimer le stage ${stage.name} ?") },
            text = { Text("Toutes les activités liées seront également supprimées.") },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.deleteStage(stage)
                    stageToDelete = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { stageToDelete = null }) { Text("Annuler") }
            }
        )
    }

    ConfigDialog(
        homeViewModel = homeViewModel,
        show = showConfigDialog,
        onDismiss = { showConfigDialog = false },
        onPickLogFolder = { folderPickerLauncher.launch(null) }
    )

    when (val state = importState) {
        is HomeViewModel.ImportState.Preview -> {
            ImportSummaryDialog(
                preview = state.preview,
                importing = false,
                onConfirm = { homeViewModel.confirmImport(state.uri) },
                onDismiss = { homeViewModel.dismissImportResult() }
            )
        }
        is HomeViewModel.ImportState.Importing -> {
            ImportSummaryDialog(
                preview = com.maathisv.vigotrack.data.ImportPreview(0, 0, 0, 0),
                importing = true,
                onConfirm = {},
                onDismiss = {}
            )
        }
        is HomeViewModel.ImportState.Done -> {
            ImportResultDialog(
                result = state.result,
                onDismiss = { homeViewModel.dismissImportResult() }
            )
        }
        is HomeViewModel.ImportState.Error -> {
            AlertDialog(
                onDismissRequest = { homeViewModel.dismissImportResult() },
                title = { Text("Erreur", color = MaterialTheme.colorScheme.error) },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { homeViewModel.dismissImportResult() }) {
                        Text("OK")
                    }
                }
            )
        }
        null -> {}
    }
}

@Composable
private fun StageCard(stage: Stage, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stage.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dateFormat.format(Date(stage.startDate))} — ${dateFormat.format(Date(stage.endDate))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Plus")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

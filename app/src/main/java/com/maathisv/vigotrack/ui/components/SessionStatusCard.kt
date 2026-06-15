package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivitySession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun SessionStatusCard(
    activity: ActivitySession,
    onMarkStale: () -> Unit = {},
    onUnmarkStale: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activity.isStale) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activity.isStale) "INVALIDÉ" else "Statut : ${activity.status}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (activity.isStale) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(text = "Type : ${activity.activityType.displayName}")
                    Text(text = "Date : ${dateFormat.format(Date(activity.scheduledDate))}")
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Plus")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (activity.isStale) {
                            DropdownMenuItem(
                                text = { Text("Annuler l'invalidation") },
                                onClick = {
                                    showMenu = false
                                    onUnmarkStale()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Invalider") },
                                onClick = {
                                    showMenu = false
                                    onMarkStale()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
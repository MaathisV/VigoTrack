package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private fun formatDuration(startTime: Long?, endTime: Long?, isRunning: Boolean): String? {
    if (isRunning) return "En cours…"
    if (startTime != null && endTime != null) {
        val diffMs = endTime - startTime
        val totalMinutes = diffMs / 60_000
        if (totalMinutes < 1) return "< 1 min"
        return if (totalMinutes < 60) {
            "${totalMinutes}min"
        } else {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (mins > 0) "${hours}h ${mins}min" else "${hours}h"
        }
    }
    return null
}


@Composable
fun ActivityCard(session: ActivitySession, onClick: () -> Unit, onMarkStale: (String) -> Unit = {}) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val patientNames = session.links.map { it.patientName }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when (session.status) {
                ActivityStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
                ActivityStatus.SCHEDULED -> MaterialTheme.colorScheme.surface
                ActivityStatus.STALE -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (session.status) {
                    ActivityStatus.COMPLETED -> Icons.Default.CheckCircle
                    ActivityStatus.IN_PROGRESS -> Icons.Default.PlayArrow
                    ActivityStatus.SCHEDULED -> Icons.Default.DateRange
                    ActivityStatus.STALE -> Icons.Default.CheckCircle
                },
                contentDescription = session.status.name,
                modifier = Modifier.size(24.dp),
                tint = when (session.status) {
                    ActivityStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
                    ActivityStatus.SCHEDULED -> MaterialTheme.colorScheme.onSurfaceVariant
                    ActivityStatus.STALE -> MaterialTheme.colorScheme.error
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.activityType.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (session.isStale) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (session.isStale) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "INVALIDÉ",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (session.activityType.category) {
                            ActivityCategory.ACTIVITE -> MaterialTheme.colorScheme.tertiaryContainer
                            ActivityCategory.BILAN -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = session.activityType.category.displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (patientNames.isNotEmpty()) {
                    Text(
                        text = patientNames.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val durationText = formatDuration(session.startTime, session.endTime, session.isRunning)
                if (durationText != null) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        text = { Text(if (session.isStale) "Déjà invalidé" else "Invalider") },
                        onClick = {
                            showMenu = false
                            if (!session.isStale) onMarkStale(session.id)
                        },
                        enabled = !session.isStale
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = timeFormat.format(Date(session.scheduledDate)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

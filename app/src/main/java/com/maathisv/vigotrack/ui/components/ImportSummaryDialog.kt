package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.maathisv.vigotrack.data.ImportPreview

@Composable
fun ImportSummaryDialog(
    preview: ImportPreview,
    importing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!importing) onDismiss() },
        title = { Text("Importer une configuration") },
        text = {
            if (importing) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    buildString {
                        appendLine("Les éléments suivants vont être importés :")
                        appendLine()
                        appendLine("\u2022 ${preview.patientCount} patient${if (preview.patientCount > 1) "s" else ""}")
                        appendLine("\u2022 ${preview.stageCount} stage${if (preview.stageCount > 1) "s" else ""}")
                        appendLine("\u2022 ${preview.activityCount} activité${if (preview.activityCount > 1) "s" else ""}")
                        append("\u2022 ${preview.linkCount} lien${if (preview.linkCount > 1) "s" else ""}")
                    }
                )
            }
        },
        confirmButton = {
            if (!importing) {
                TextButton(onClick = onConfirm) { Text("Importer") }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !importing
            ) { Text("Annuler") }
        }
    )
}

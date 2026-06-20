package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.data.ImportResult

@Composable
fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (result.errors.isEmpty()) "Import terminé" else "Import terminé avec erreurs",
                color = if (result.errors.isEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Section("Patients (${result.patientsCreated.size + result.patientsSkipped.size})")
                result.patientsCreated.forEach { name ->
                    ItemRow(Icons.Default.Check, Color(0xFF4CAF50), name)
                }
                result.patientsSkipped.forEach { name ->
                    ItemRow(Icons.Default.Warning, Color.Gray, "$name (déjà existant)")
                }

                Section("Stages (${result.stagesCreated.size})")
                result.stagesCreated.forEach { name ->
                    ItemRow(Icons.Default.Check, Color(0xFF4CAF50), name)
                }

                Section("Activités (${result.activitiesCreated.size})")
                result.activitiesCreated.forEach { name ->
                    ItemRow(Icons.Default.Check, Color(0xFF4CAF50), name)
                }
                Text(
                    text = "Liens créés : ${result.linksCreated}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )

                if (result.errors.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Erreurs (${result.errors.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    result.errors.forEach { error ->
                        ItemRow(Icons.Default.Close, MaterialTheme.colorScheme.error, error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ItemRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

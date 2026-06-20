package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfigSettingsTab(
    currentLogUri: String,
    namingTemplate: String,
    showFeatures: Map<String, Boolean>,
    logFeatures: Map<String, Boolean>,
    onPickLogFolder: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onResetTemplate: () -> Unit,
    onToggleShowFeature: (String) -> Unit,
    onToggleLogFeature: (String) -> Unit
) {
    var showPlaceholders by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Dossier d'exportation",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Les fichiers de données sont enregistrés dans le dossier sélectionné. Utilisez \"/\" dans le modèle pour créer des sous-dossiers.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = currentLogUri.ifBlank { "Aucun dossier sélectionné" },
            onValueChange = {},
            readOnly = true,
            label = { Text("URI du dossier actuel") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPickLogFolder) {
            Text("Changer le dossier d'exportation")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Modèle de nommage des fichiers",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = namingTemplate,
            onValueChange = onTemplateChange,
            label = { Text("Modèle") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        val templateSafe = remember(namingTemplate) {
            listOf("{activity_id}", "{datetime}", "{timestamp}", "{sensor}")
                .any { namingTemplate.contains(it) }
        }

        if (!templateSafe) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text(
                    text = "Ce modèle risque de mélanger les données de différentes sessions. Ajoutez {activity_id}, {datetime}, {timestamp} ou {sensor}.",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onResetTemplate) {
                Text("Réinitialiser")
            }
            TextButton(onClick = { showPlaceholders = true }) {
                Text("Espaces réservés…")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Visibilité des données",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        val features = listOf("HR", "PPI", "ACC", "ECG")
        val featureLabels = mapOf("HR" to "Fréquence cardiaque", "PPI" to "Intervalle PP", "ACC" to "Accéléromètre", "ECG" to "ECG")

        features.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = featureLabels[feature] ?: feature,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showFeatures[feature] ?: true,
                        onCheckedChange = { onToggleShowFeature(feature) }
                    )
                    Text("Afficher", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = logFeatures[feature] ?: true,
                        onCheckedChange = { onToggleLogFeature(feature) }
                    )
                    Text("Enregistrer", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showPlaceholders) {
        AlertDialog(
            onDismissRequest = { showPlaceholders = false },
            title = { Text("Espaces réservés disponibles") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("{stage} - Nom du Stage")
                    Text("{patient} - Nom du patient")
                    Text("{category} - Catégorie d'activité (BILAN / ACTIVITÉ)")
                    Text("{activity} - Type d'activité (ex. MARCHE, TDM6)")
                    Text("{activity_id} - Identifiant unique de l'activité")
                    Text("{sensor} - Identifiant du capteur")
                    Text("{device} - Alias de {sensor}")
                    Text("{tag} - Type de flux de données (HR, PPI, ACC, ECG)")
                    Text("{date} - Date (AAAA-MM-JJ)")
                    Text("{time} - Heure (HH-MM-SS)")
                    Text("{datetime} - Date_heure combinée")
                    Text("{timestamp} - Timestamp Unix (ms)")
                    Text("")
                    Text("Utilisez / dans votre modèle pour créer des niveaux de dossiers.")
                    Text("Défaut : {stage}/{patient}/{category}/{activity_id}_{activity}/{sensor}_{datetime}_{tag}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaceholders = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.SensorPatientLink

private val featureDisplayName = mapOf("HR" to "FC", "PPI" to "PPI", "ACC" to "ACC", "ECG" to "ECG", "EULER" to "EULER", "QUATERNION" to "QUATERNION", "FREE_ACCELERATION" to "FREE_ACCELERATION")

data class DisplayLink(
    val patientId: Long?,
    val patientName: String,
    val sensorId: String,
    val sensorDisplayName: String?,
    val features: List<String>
)

fun buildDisplayLinks(
    preLinks: List<SensorPatientLink>,
    activityLinks: List<ActivitySession.ActivityLink>,
    patients: List<Patient>,
    connectedSensors: List<Sensor>
): List<DisplayLink> {
    val result = mutableListOf<DisplayLink>()

    preLinks.forEach { link ->
        val sensor = connectedSensors.find { it.deviceId == link.sensorId }
        val patient = patients.find { it.id == link.patientId }
        result.add(
            DisplayLink(
                patientId = link.patientId,
                patientName = patient?.name ?: "Inconnu",
                sensorId = link.sensorId,
                sensorDisplayName = sensor?.effectiveName,
                features = link.features
            )
        )
    }

    activityLinks.forEach { link ->
        if (result.none { it.sensorId == link.sensorId && it.patientId == link.patientId }) {
            val sensor = connectedSensors.find { it.deviceId == link.sensorId }
            result.add(
                DisplayLink(
                    patientId = link.patientId,
                    patientName = link.patientName,
                    sensorId = link.sensorId,
                    sensorDisplayName = sensor?.effectiveName,
                    features = link.featuresToTrack
                )
            )
        }
    }

    return result
}

@Composable
fun PatientCheckboxRow(
    displayItem: DisplayLink,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(displayItem.patientName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${displayItem.sensorDisplayName ?: displayItem.sensorId} — ${displayItem.features.joinToString(", ") { featureDisplayName[it] ?: it }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
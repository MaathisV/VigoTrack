package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivitySession
import kotlin.math.sqrt


private data class FeatureData(
    val label: String,
    val valueText: String,
    val unit: String,
    val valueColor: Color,
    val graphValue: Float?,
    val graphColor: Color,
    val maxPoints: Int = 100
)

@Composable
private fun FeatureCell(data: FeatureData, modifier: Modifier) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(70.dp)) {
            Text(data.label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = data.valueText,
                style = MaterialTheme.typography.titleLarge,
                color = data.valueColor
            )
            Text(data.unit, style = MaterialTheme.typography.labelSmall)
        }
        MiniGraph(
            currentValue = data.graphValue,
            lineColor = data.graphColor,
            maxPoints = data.maxPoints,
            modifier = Modifier.weight(1f).height(36.dp)
        )
    }
}


@Composable
fun SensorDataCard(
    link: ActivitySession.ActivityLink,
    sensorName: String? = null,
    sensorData: Map<String, Any>?,
    showFeatures: Map<String, Boolean> = emptyMap(),
    columns: Int = 2
) {
    val hrValue = (sensorData?.get("HR") as? Number)?.toFloat()
    val ppiValue = (sensorData?.get("PPI") as? Number)?.toFloat()
    val ecgValue = (sensorData?.get("ECG") as? Number)?.toFloat()

    val accX = (sensorData?.get("ACC_X") as? Number)?.toFloat() ?: 0f
    val accY = (sensorData?.get("ACC_Y") as? Number)?.toFloat() ?: 0f
    val accZ = (sensorData?.get("ACC_Z") as? Number)?.toFloat() ?: 0f
    val rawMag = sqrt(accX * accX + accY * accY + accZ * accZ)
    val gravityRemoved = if (rawMag >= 1000f) rawMag - 1000f else 1000f - rawMag
    val accMagnitude = gravityRemoved * 9.81f / 1000f

    val eulerRoll = (sensorData?.get("EULER_ROLL") as? Number)?.toFloat()
    val eulerPitch = (sensorData?.get("EULER_PITCH") as? Number)?.toFloat()
    val eulerYaw = (sensorData?.get("EULER_YAW") as? Number)?.toFloat()

    val freeAccX = (sensorData?.get("FREE_ACC_X") as? Number)?.toFloat()
    val freeAccY = (sensorData?.get("FREE_ACC_Y") as? Number)?.toFloat()
    val freeAccZ = (sensorData?.get("FREE_ACC_Z") as? Number)?.toFloat()

    val eulerText = if (eulerRoll != null) "%.1f°.%.1f°.%.1f°".format(eulerRoll, eulerPitch, eulerYaw) else "--"
    val freeAccText = if (freeAccX != null) "%.2f/%.2f/%.2f".format(freeAccX, freeAccY, freeAccZ) else "--"

    val features = listOfNotNull(
        if (link.streamHR && showFeatures.getOrDefault("HR", true)) FeatureData(
            label = "HR", valueText = "${hrValue?.toInt() ?: "--"}", unit = "bpm",
            valueColor = MaterialTheme.colorScheme.primary,
            graphValue = hrValue, graphColor = MaterialTheme.colorScheme.primary
        ) else null,
        if (link.streamPPI && showFeatures.getOrDefault("PPI", true)) FeatureData(
            label = "PPI", valueText = "${ppiValue?.toInt() ?: "--"}", unit = "ms",
            valueColor = MaterialTheme.colorScheme.onSurface,
            graphValue = ppiValue, graphColor = MaterialTheme.colorScheme.secondary
        ) else null,
        if (link.streamACC && showFeatures.getOrDefault("ACC", true)) FeatureData(
            label = "ACC", valueText = "%.1f".format(accMagnitude), unit = "m/s²",
            valueColor = MaterialTheme.colorScheme.onSurface,
            graphValue = if (accMagnitude > 0f) accMagnitude else null,
            graphColor = MaterialTheme.colorScheme.tertiary
        ) else null,
        if (link.streamECG && showFeatures.getOrDefault("ECG", true)) FeatureData(
            label = "ECG", valueText = "${ecgValue?.toInt() ?: "--"}", unit = "uV",
            valueColor = MaterialTheme.colorScheme.error,
            graphValue = ecgValue, graphColor = MaterialTheme.colorScheme.error,
            maxPoints = 200
        ) else null,
        if (link.streamEULER && showFeatures.getOrDefault("EULER", true)) FeatureData(
            label = "EULER", valueText = eulerText, unit = "R/P/Y",
            valueColor = MaterialTheme.colorScheme.onSurface,
            graphValue = if (eulerRoll != null) eulerRoll else null,
            graphColor = MaterialTheme.colorScheme.tertiary
        ) else null,
        if (link.streamFREE_ACCEL && showFeatures.getOrDefault("FREE_ACCELERATION", true)) FeatureData(
            label = "FREE ACC", valueText = freeAccText, unit = "m/s²",
            valueColor = MaterialTheme.colorScheme.onSurface,
            graphValue = if (freeAccX != null) freeAccX else null,
            graphColor = MaterialTheme.colorScheme.secondary
        ) else null
    )

    val rows = features.chunked(columns)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Patient : ${link.patientName}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Capteur : ${sensorName ?: link.sensorId}", style = MaterialTheme.typography.labelSmall)

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { f -> FeatureCell(f, Modifier.weight(1f)) }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 12.dp)
            )

            Column(
                modifier = Modifier.width(70.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Intensité",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "--",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CompactSensorDataCard(
    link: ActivitySession.ActivityLink,
    sensorName: String? = null,
    sensorData: Map<String, Any>?,
    showFeatures: Map<String, Boolean> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val hrValue = (sensorData?.get("HR") as? Number)?.toInt()
    val ppiValue = (sensorData?.get("PPI") as? Number)?.toInt()
    val ecgValue = (sensorData?.get("ECG") as? Number)?.toInt()

    val accX = (sensorData?.get("ACC_X") as? Number)?.toFloat() ?: 0f
    val accY = (sensorData?.get("ACC_Y") as? Number)?.toFloat() ?: 0f
    val accZ = (sensorData?.get("ACC_Z") as? Number)?.toFloat() ?: 0f
    val rawMag = sqrt(accX * accX + accY * accY + accZ * accZ)
    val gravityRemoved = if (rawMag >= 1000f) rawMag - 1000f else 1000f - rawMag
    val accMagnitude = gravityRemoved * 9.81f / 1000f

    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${link.patientName} (${sensorName ?: link.sensorId})",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (link.streamHR && showFeatures.getOrDefault("HR", true)) {
                        Text("HR: ${hrValue ?: "--"}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (link.streamPPI && showFeatures.getOrDefault("PPI", true)) {
                        Text("PPI: ${ppiValue ?: "--"}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (link.streamACC && showFeatures.getOrDefault("ACC", true)) {
                        Text("ACC: ${"%.1f".format(accMagnitude)}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (link.streamECG && showFeatures.getOrDefault("ECG", true)) {
                        Text("ECG: ${ecgValue ?: "--"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            VerticalDivider(
                modifier = Modifier.height(IntrinsicSize.Min).padding(horizontal = 8.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(50.dp)
            ) {
                Text("Intensité", style = MaterialTheme.typography.labelSmall)
                Text("--", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}


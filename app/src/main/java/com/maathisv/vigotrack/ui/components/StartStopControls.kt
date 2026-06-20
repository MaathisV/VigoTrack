package com.maathisv.vigotrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityStatus
import com.maathisv.vigotrack.ui.viewmodel.HomeViewModel


@Composable
fun StartStopControls(activity: ActivitySession, homeViewModel: HomeViewModel, checkedKeys: Set<String> = emptySet()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { homeViewModel.toggleSession(activity, checkedKeys) },
            colors = when {
                activity.isRunning -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                activity.status == ActivityStatus.COMPLETED -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                else -> ButtonDefaults.buttonColors()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                when (activity.status) {
                    ActivityStatus.COMPLETED -> "REPRENDRE"
                    ActivityStatus.IN_PROGRESS -> "ARRÊTER LA SESSION"
                    else -> "DÉMARRER LA SESSION"
                }
            )
        }
    }
}

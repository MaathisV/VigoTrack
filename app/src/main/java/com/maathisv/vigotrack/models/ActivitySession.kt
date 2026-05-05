package com.maathisv.vigotrack.models

import com.maathisv.vigotrack.data.entities.ActivitySessionEntity
import java.util.UUID

data class ActivitySession(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val links: List<ActivityLink> = emptyList(),
    val scheduledDate: Long, // Use Timestamp
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isRunning: Boolean = false
) {
    data class ActivityLink(
        val patientId: String,
        val sensorId: String,
        val patientName: String = "Unknown",
        val featuresToTrack: List<String>
    )
}
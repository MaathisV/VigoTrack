package com.maathisv.vigotrack.models

import java.util.UUID

data class ActivitySession(
    val id: String = UUID.randomUUID().toString(),
    val activityType: ActivityType,
    val links: List<ActivityLink> = emptyList(),
    val scheduledDate: Long,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isRunning: Boolean = false,
    val stageId: Long? = null
) {
    // Determine status based on flags
    val status: ActivityStatus
        get() = when {
            endTime != null -> ActivityStatus.COMPLETED
            isRunning -> ActivityStatus.IN_PROGRESS
            else -> ActivityStatus.SCHEDULED
        }

    data class ActivityLink(
        val patientId: Long?,
        val patientName: String,
        val sensorId: String,
        val featuresToTrack: List<String> = emptyList()
    ) {
        // UI Helpers: The screen is looking for these booleans
        val streamHR: Boolean get() = featuresToTrack.contains("HR")
        val streamECG: Boolean get() = featuresToTrack.contains("ECG")
        val streamACC: Boolean get() = featuresToTrack.contains("ACC")
    }
}

enum class ActivityStatus {
    SCHEDULED, IN_PROGRESS, COMPLETED
}
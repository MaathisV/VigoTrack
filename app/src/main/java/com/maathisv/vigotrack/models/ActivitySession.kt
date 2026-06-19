package com.maathisv.vigotrack.models

import java.util.UUID

data class ActivitySession(
    val id: String = UUID.randomUUID().toString(),
    val activityType: ActivityType,
    val links: List<ActivityLink> = emptyList(),
    val scheduledDate: Long,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val accumulatedTimeMs: Long = 0,
    val isRunning: Boolean = false,
    val stageId: Long? = null,
    val isStale: Boolean = false
) {
    // Determine status based on flags
    val status: ActivityStatus
        get() = when {
            isStale -> ActivityStatus.STALE
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
        val streamHR: Boolean get() = featuresToTrack.contains("HR")
        val streamPPI: Boolean get() = featuresToTrack.contains("PPI")
        val streamACC: Boolean get() = featuresToTrack.contains("ACC")
        val streamECG: Boolean get() = featuresToTrack.contains("ECG")
        val streamEULER: Boolean get() = featuresToTrack.contains("EULER")
        val streamQUATERNION: Boolean get() = featuresToTrack.contains("QUATERNION")
        val streamFreeACCEL: Boolean get() = featuresToTrack.contains("FREE_ACCELERATION")
    }
}

enum class ActivityStatus {
    SCHEDULED, IN_PROGRESS, COMPLETED, STALE
}
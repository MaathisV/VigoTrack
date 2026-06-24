package com.maathisv.vigotrack.data.mappers

import com.maathisv.vigotrack.data.ActivityWithLinks
import com.maathisv.vigotrack.data.entities.ActivityLinkEntity
import com.maathisv.vigotrack.data.entities.ActivitySessionEntity
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityType

fun ActivityWithLinks.toDomain(): ActivitySession {
    return ActivitySession(
        id = activity.id,
        activityType = ActivityType.fromName(activity.activityType)
            ?: ActivityType(activity.activityType, activity.activityType, ActivityCategory.ACTIVITE),
        scheduledDate = activity.scheduledDate,
        startTime = activity.startTime,
        endTime = activity.endTime,
        accumulatedTimeMs = activity.accumulatedTimeMs,
        isRunning = activity.isRunning,
        stageId = activity.stageId,
        isStale = activity.isStale,
        customName = activity.customName,
        links = links.map { it.toDomain() }
    )
}

fun ActivityLinkEntity.toDomain(): ActivitySession.ActivityLink {
    return ActivitySession.ActivityLink(
        patientId = patientId,
        patientName = patientName,
        sensorId = sensorId,
        featuresToTrack = features.split(",").filter { it.isNotBlank() }
    )
}

fun ActivitySession.toEntity(): ActivitySessionEntity {
    return ActivitySessionEntity(
        id = id,
        activityType = activityType.name,
        scheduledDate = scheduledDate,
        startTime = startTime,
        endTime = endTime,
        accumulatedTimeMs = accumulatedTimeMs,
        isRunning = isRunning,
        stageId = stageId,
        isStale = isStale,
        customName = customName
    )
}

fun ActivitySession.ActivityLink.toEntity(activityId: String): ActivityLinkEntity {
    return ActivityLinkEntity(
        parentActivityId = activityId,
        patientId = patientId,
        patientName = patientName,
        sensorId = sensorId,
        features = featuresToTrack.joinToString(",")
    )
}
package com.maathisv.vigotrack.data.mappers

import com.maathisv.vigotrack.data.ActivityWithLinks
import com.maathisv.vigotrack.data.entities.ActivityLinkEntity
import com.maathisv.vigotrack.data.entities.ActivitySessionEntity
import com.maathisv.vigotrack.models.ActivitySession

// 1. Map the POJO (DB) -> Domain (App)
fun ActivityWithLinks.toDomain(): ActivitySession {
    return ActivitySession(
        id = activity.id,
        name = activity.name,
        scheduledDate = activity.scheduledDate,
        startTime = activity.startTime,
        endTime = activity.endTime,
        isRunning = activity.isRunning,
        links = links.map { it.toDomain() }
    )
}

// 2. Map the Link Entity (DB) -> Domain Link (App)
fun ActivityLinkEntity.toDomain(): ActivitySession.ActivityLink {
    return ActivitySession.ActivityLink(
        patientId = patientId,
        sensorId = sensorId,
        patientName = patientId,
        featuresToTrack = features.split(",").filter { it.isNotBlank() }
    )
}

// 3. Map Domain Session (App) -> Entity (DB)
fun ActivitySession.toEntity(): ActivitySessionEntity {
    return ActivitySessionEntity(
        id = id,
        name = name,
        scheduledDate = scheduledDate,
        startTime = startTime,
        endTime = endTime,
        isRunning = isRunning
    )
}

// 4. Map Domain Link (App) -> Entity (DB) - THIS IS THE ONE CAUSING THE ERROR
fun ActivitySession.ActivityLink.toEntity(activityId: String): ActivityLinkEntity {
    return ActivityLinkEntity(
        parentActivityId = activityId,
        patientId = patientId,
        sensorId = sensorId,
        features = featuresToTrack.joinToString(",")
    )
}
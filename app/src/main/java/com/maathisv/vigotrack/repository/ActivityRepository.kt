package com.maathisv.vigotrack.repository

import com.maathisv.vigotrack.data.ActivityDataSource
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityType
import java.util.UUID

class ActivityRepository(val dataSource: ActivityDataSource) {

    // Source of truth (Mapping happens inside the DataSource implementation)
    val allActivities = dataSource.getAllActivities()

    fun getActivitiesByStage(stageId: Long) = dataSource.getActivitiesByStage(stageId)

    // 1. Create a fresh Activity (No links yet)
    suspend fun createActivity(type: ActivityType, scheduledDate: Long, stageId: Long? = null, activityId: String? = null) {
        val newActivity = ActivitySession(
            id = activityId ?: UUID.randomUUID().toString(),
            activityType = type,
            scheduledDate = scheduledDate,
            links = emptyList(),
            stageId = stageId
        )
        dataSource.insertActivity(newActivity)
    }

    // 2. Add a link (Relational way: Just insert the new row)
    suspend fun addLinkToActivity(activityId: String, link: ActivitySession.ActivityLink) {
        dataSource.insertLink(activityId, link)
    }

    // 3. Update activity status/times
    suspend fun startActivity(activity: ActivitySession) {
        val startedActivity = activity.copy(
            startTime = System.currentTimeMillis(),
            endTime = null,
            isRunning = true
        )
        dataSource.updateActivity(startedActivity)
    }

    suspend fun stopActivity(activity: ActivitySession) {
        val now = System.currentTimeMillis()
        val elapsedThisSegment = activity.startTime?.let { now - it } ?: 0L
        val stoppedActivity = activity.copy(
            endTime = now,
            isRunning = false,
            accumulatedTimeMs = activity.accumulatedTimeMs + elapsedThisSegment
        )
        dataSource.updateActivity(stoppedActivity)
    }

    // Add this so the ViewModel can push any state change
    suspend fun updateActivity(activity: ActivitySession) {
        dataSource.updateActivity(activity)
    }

    suspend fun removeLinkFromActivity(activityId: String, sensorId: String, patientId: Long?) {
        dataSource.deleteLink(activityId, sensorId, patientId)
    }

    suspend fun deleteActivitiesByStage(stageId: Long) {
        dataSource.deleteActivitiesByStage(stageId)
    }
}
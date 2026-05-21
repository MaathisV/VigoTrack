package com.maathisv.vigotrack.repository

import com.maathisv.vigotrack.data.ActivityDataSource
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityType
import java.util.UUID

class ActivityRepository(private val dataSource: ActivityDataSource) {

    // Source of truth (Mapping happens inside the DataSource implementation)
    val allActivities = dataSource.getAllActivities()

    // 1. Create a fresh Activity (No links yet)
    suspend fun createActivity(type: ActivityType, scheduledDate: Long) {
        val newActivity = ActivitySession(
            id = UUID.randomUUID().toString(),
            activityType = type,
            scheduledDate = scheduledDate,
            links = emptyList()
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
            isRunning = true
        )
        dataSource.updateActivity(startedActivity)
    }

    suspend fun stopActivity(activity: ActivitySession) {
        val stoppedActivity = activity.copy(
            endTime = System.currentTimeMillis(),
            isRunning = false
        )
        dataSource.updateActivity(stoppedActivity)
    }

    // Add this so the ViewModel can push any state change
    suspend fun updateActivity(activity: ActivitySession) {
        dataSource.updateActivity(activity)
    }
}
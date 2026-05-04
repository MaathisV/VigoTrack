package com.maathisv.vigotrack.repository

import com.maathisv.vigotrack.data.ActivityDataSource
import com.maathisv.vigotrack.models.ActivitySession
import java.util.UUID

class ActivityRepository(private val dataSource: ActivityDataSource) {

    // This is the source of truth from the Database
    val allActivities = dataSource.getAllActivities()

    // 1. Creation - Renamed from 'schedule' to 'create' to match your preference
    suspend fun createActivity(name: String, scheduledDate: Long) {
        val newActivity = ActivitySession(
            id = UUID.randomUUID().toString(),
            name = name,
            scheduledDate = scheduledDate,
            links = emptyList()
        )
        dataSource.insertActivity(newActivity)
    }

    // 2. Adding a link (Patient/Sensor pair) to an existing Activity
    suspend fun addLinkToActivity(activityId: String, link: ActivitySession.ActivityLink) {
        // We retrieve the latest activities from the stream
        val currentActivities = allActivities.first()
        val activity = currentActivities.find { it.id == activityId }

        activity?.let {
            val updatedActivity = it.copy(links = it.links + link)
            dataSource.updateActivity(updatedActivity)
        }
    }

    // 3. Execution - Marking when the workout actually begins
    suspend fun startActivity(activity: ActivitySession) {
        val startedActivity = activity.copy(
            startTime = System.currentTimeMillis()
        )
        dataSource.updateActivity(startedActivity)
    }
}
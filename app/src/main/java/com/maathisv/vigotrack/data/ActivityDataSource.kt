package com.maathisv.vigotrack.data

import com.maathisv.vigotrack.data.dao.ActivityDao
import com.maathisv.vigotrack.data.mappers.*
import com.maathisv.vigotrack.models.ActivitySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ActivityDataSource {
    fun getAllActivities(): Flow<List<ActivitySession>>
    suspend fun insertActivity(activity: ActivitySession)
    suspend fun updateActivity(activity: ActivitySession)
    suspend fun deleteActivity(id: String)
    suspend fun insertLink(activityId: String, link: ActivitySession.ActivityLink)
}

class RoomActivityDataSource(private val dao: ActivityDao) : ActivityDataSource {

    override fun getAllActivities(): Flow<List<ActivitySession>> =
        dao.getActivitiesWithLinks().map { poos ->
            poos.map { it.toDomain() }
        }

    override suspend fun insertActivity(activity: ActivitySession) {
        dao.insertActivity(activity.toEntity())
    }

    override suspend fun updateActivity(activity: ActivitySession) {
        dao.updateActivity(activity.toEntity())
    }

    override suspend fun insertLink(activityId: String, link: ActivitySession.ActivityLink) {
        dao.insertLink(link.toEntity(activityId))
    }

    override suspend fun deleteActivity(id: String) = dao.deleteById(id)
}
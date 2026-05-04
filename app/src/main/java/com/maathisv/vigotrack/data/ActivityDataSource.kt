package com.maathisv.vigotrack.data

import com.maathisv.vigotrack.data.dao.ActivityDao
import com.maathisv.vigotrack.models.ActivitySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ActivityDataSource {
    fun getAllActivities(): Flow<List<ActivitySession>>
    suspend fun insertActivity(activity: ActivitySession)
    suspend fun updateActivity(activity: ActivitySession)
    suspend fun deleteActivity(id: String)
}

class RoomActivityDataSource(private val dao: ActivityDao) : ActivityDataSource {
    override fun getAllActivities(): Flow<List<ActivitySession>> = dao.getAllActivities().map {  ->
        entities.map { it.toDomainModel() }
    }

    override suspend fun insertActivity(activity: ActivitySession) {
        dao.insert(activity.toEntity())
    }

    override suspend fun updateActivity(activity: ActivitySession) {
        dao.update(activity.toEntity())
    }

    override suspend fun deleteActivity(id: String) = dao.deleteById(id)
}
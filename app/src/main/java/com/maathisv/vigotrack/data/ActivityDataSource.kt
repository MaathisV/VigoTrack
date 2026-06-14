package com.maathisv.vigotrack.data

import com.maathisv.vigotrack.data.dao.ActivityDao
import com.maathisv.vigotrack.data.mappers.toDomain
import com.maathisv.vigotrack.data.mappers.toEntity
import com.maathisv.vigotrack.models.ActivitySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ActivityDataSource {
    fun getAllActivities(): Flow<List<ActivitySession>>
    fun getActivitiesByStage(stageId: Long): Flow<List<ActivitySession>>
    suspend fun insertActivity(activity: ActivitySession)
    suspend fun updateActivity(activity: ActivitySession)
    suspend fun deleteActivity(id: String)
    suspend fun insertLink(activityId: String, link: ActivitySession.ActivityLink)
    suspend fun deleteLink(activityId: String, sensorId: String, patientId: Long?)
}

class RoomActivityDataSource(private val dao: ActivityDao) : ActivityDataSource {

    override fun getAllActivities(): Flow<List<ActivitySession>> =
        dao.getActivitiesWithLinks().map { poos ->
            poos.map { it.toDomain() }
        }

    override fun getActivitiesByStage(stageId: Long): Flow<List<ActivitySession>> =
        dao.getActivitiesByStage(stageId).map { poos ->
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

    override suspend fun deleteLink(activityId: String, sensorId: String, patientId: Long?) {
        dao.deleteLinkByActivityAndSensor(activityId, sensorId, patientId)
    }
}
package com.maathisv.vigotrack.data

import com.maathisv.vigotrack.data.dao.ActivityTypeDao
import com.maathisv.vigotrack.data.entities.ActivityTypeEntity
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ActivityTypeDataSource {
    fun getAll(): Flow<List<ActivityType>>
    fun getByCategory(category: ActivityCategory): Flow<List<ActivityType>>
    suspend fun saveTypes(types: List<ActivityType>)
    suspend fun seedIfEmpty()
}

class RoomActivityTypeDataSource(private val dao: ActivityTypeDao) : ActivityTypeDataSource {

    override fun getAll(): Flow<List<ActivityType>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getByCategory(category: ActivityCategory): Flow<List<ActivityType>> =
        dao.getByCategory(category.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveTypes(types: List<ActivityType>) {
        dao.insertAll(types.map { it.toEntity() })
    }

    override suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.insertAll(ActivityType.entries.map { it.toEntity() })
        }
    }
}

private fun ActivityTypeEntity.toDomain() = ActivityType(
    name = name,
    displayName = displayName,
    category = try { ActivityCategory.valueOf(category) } catch (_: IllegalArgumentException) { ActivityCategory.ACTIVITE }
)

private fun ActivityType.toEntity() = ActivityTypeEntity(
    name = name,
    displayName = displayName,
    category = category.name
)

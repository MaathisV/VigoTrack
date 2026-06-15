package com.maathisv.vigotrack.data

import com.maathisv.vigotrack.data.dao.StageDao
import com.maathisv.vigotrack.data.entities.StageEntity
import com.maathisv.vigotrack.models.Stage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StageDataSource {
    fun getAllStages(): Flow<List<Stage>>
    suspend fun insertStage(stage: Stage): Long
    suspend fun deleteStage(stage: Stage)
}

class RoomStageDataSource(private val dao: StageDao) : StageDataSource {

    override fun getAllStages(): Flow<List<Stage>> =
        dao.getAllStages().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertStage(stage: Stage): Long {
        return dao.insertStage(stage.toEntity())
    }

    override suspend fun deleteStage(stage: Stage) {
        dao.deleteStage(stage.toEntity())
    }
}

private fun StageEntity.toDomain() = Stage(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate
)

private fun Stage.toEntity() = StageEntity(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate
)

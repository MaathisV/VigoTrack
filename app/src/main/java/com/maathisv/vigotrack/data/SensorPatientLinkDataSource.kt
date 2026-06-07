package com.maathisv.vigotrack.data

import com.maathisv.vigotrack.data.dao.SensorPatientLinkDao
import com.maathisv.vigotrack.data.entities.SensorPatientLinkEntity
import com.maathisv.vigotrack.models.SensorPatientLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SensorPatientLinkDataSource {
    fun getAllLinks(): Flow<List<SensorPatientLink>>
    suspend fun insertLink(patientId: Long?, sensorId: String, features: List<String>)
    suspend fun deleteLink(link: SensorPatientLink)
}

class RoomSensorPatientLinkDataSource(private val dao: SensorPatientLinkDao) : SensorPatientLinkDataSource {

    override fun getAllLinks(): Flow<List<SensorPatientLink>> =
        dao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertLink(patientId: Long?, sensorId: String, features: List<String>) {
        dao.insert(
            SensorPatientLinkEntity(
                patientId = patientId,
                sensorId = sensorId,
                features = features.joinToString(",")
            )
        )
    }

    override suspend fun deleteLink(link: SensorPatientLink) {
        dao.delete(link.toEntity())
    }
}

private fun SensorPatientLinkEntity.toDomain() = SensorPatientLink(
    id = id,
    patientId = patientId,
    sensorId = sensorId,
    features = features.split(",").filter { it.isNotBlank() }
)

private fun SensorPatientLink.toEntity() = SensorPatientLinkEntity(
    id = id,
    patientId = patientId,
    sensorId = sensorId,
    features = features.joinToString(",")
)

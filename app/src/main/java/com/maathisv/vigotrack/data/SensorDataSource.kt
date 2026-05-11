package com.maathisv.vigotrack.data

import com.maathisv.vigotrack.data.dao.SensorDao
import com.maathisv.vigotrack.data.entities.SensorEntity
import com.maathisv.vigotrack.models.Sensor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. The Interface (Seen by Repository and ViewModel)
interface SensorDataSource {
    fun getSavedSensors(): Flow<List<Sensor>>
    suspend fun saveSensor(sensor: Sensor)
    suspend fun deleteSensor(deviceId: String)
}

// Implement the interface for Room usage
// Other Data Storage usage need the creation of a class that implement the interface
class RoomSensorDataSource(private val sensorDao: SensorDao) : SensorDataSource {

    override fun getSavedSensors(): Flow<List<Sensor>> {
        return sensorDao.getAllSensors().map { entities ->
            entities.map { entity ->
                Sensor(
                    deviceId = entity.deviceId,
                    address = entity.address,
                    name = entity.name
                )
            }
        }
    }

    override suspend fun saveSensor(sensor: Sensor) {
        sensorDao.insertSensor(
            SensorEntity(
                deviceId = sensor.deviceId,
                address = sensor.address,
                name = sensor.name
            )
        )
    }

    override suspend fun deleteSensor(deviceId: String) {
        sensorDao.deleteById(deviceId)
    }
}
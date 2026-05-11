package com.maathisv.vigotrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.maathisv.vigotrack.data.entities.SensorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Query("SELECT * FROM sensors")
    fun getAllSensors(): Flow<List<SensorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensor(sensor: SensorEntity)

    @Delete
    suspend fun deleteSensor(sensor: SensorEntity)

    @Query("DELETE FROM sensors WHERE deviceId = :deviceId")
    suspend fun deleteById(deviceId: String)
}
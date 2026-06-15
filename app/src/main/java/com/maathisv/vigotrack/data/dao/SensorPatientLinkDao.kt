package com.maathisv.vigotrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.maathisv.vigotrack.data.entities.SensorPatientLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorPatientLinkDao {
    @Query("SELECT * FROM sensor_patient_links")
    fun getAll(): Flow<List<SensorPatientLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: SensorPatientLinkEntity)

    @Delete
    suspend fun delete(link: SensorPatientLinkEntity)
}

package com.maathisv.vigotrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.maathisv.vigotrack.data.entities.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Insert
    suspend fun insertPatient(patient: PatientEntity): Long

    @Delete
    suspend fun deletePatient(patient: PatientEntity)
}

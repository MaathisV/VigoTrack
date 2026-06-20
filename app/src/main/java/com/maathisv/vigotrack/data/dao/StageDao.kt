package com.maathisv.vigotrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.maathisv.vigotrack.data.entities.StageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StageDao {
    @Query("SELECT * FROM stages ORDER BY startDate DESC")
    fun getAllStages(): Flow<List<StageEntity>>

    @Insert
    suspend fun insertStage(stage: StageEntity): Long

    @Update
    suspend fun updateStage(stage: StageEntity)

    @Delete
    suspend fun deleteStage(stage: StageEntity)
}

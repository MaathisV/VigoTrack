package com.maathisv.vigotrack.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.maathisv.vigotrack.data.entities.ActivityTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTypeDao {
    @Query("SELECT * FROM activity_types ORDER BY name")
    fun getAll(): Flow<List<ActivityTypeEntity>>

    @Query("SELECT * FROM activity_types WHERE category = :category ORDER BY name")
    fun getByCategory(category: String): Flow<List<ActivityTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<ActivityTypeEntity>)

    @Query("SELECT COUNT(*) FROM activity_types")
    suspend fun count(): Int
}

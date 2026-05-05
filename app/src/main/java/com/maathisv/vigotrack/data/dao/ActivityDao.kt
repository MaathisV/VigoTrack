package com.maathisv.vigotrack.data.dao

import androidx.room.*
import com.maathisv.vigotrack.data.ActivityWithLinks
import com.maathisv.vigotrack.data.entities.ActivityLinkEntity
import com.maathisv.vigotrack.data.entities.ActivitySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY scheduledDate ASC")
    fun getAllActivities(): Flow<List<ActivitySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivitySessionEntity)

    @Update
    suspend fun updateActivity(activity: ActivitySessionEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction // Important: Room does 2 queries, so this ensures consistency
    @Query("SELECT * FROM activities")
    fun getActivitiesWithLinks(): Flow<List<ActivityWithLinks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(toEntity: ActivityLinkEntity)

    @Update
    suspend fun updateLink(link: ActivityLinkEntity)

    @Query("DELETE FROM activity_links WHERE parentActivityId = :activity")
    suspend fun deleteLinkByActivity(activity: String)

    @Query("DELETE FROM activity_links WHERE linkId = :linkId")
    suspend fun deleteLinkById(linkId: Long)
}
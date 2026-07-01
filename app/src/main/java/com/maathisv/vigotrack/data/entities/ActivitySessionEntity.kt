package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "activities",
    indices = [
        Index(value = ["scheduledDate"]),
        Index(value = ["isRunning"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["stageId"])
    ]
)
data class ActivitySessionEntity(
    @PrimaryKey val id: String,
    val activityType: String,
    val scheduledDate: Long,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val accumulatedTimeMs: Long = 0,
    val isRunning: Boolean = false,
    val stageId: Long? = null,
    val isStale: Boolean = false,
    val customName: String? = null
)
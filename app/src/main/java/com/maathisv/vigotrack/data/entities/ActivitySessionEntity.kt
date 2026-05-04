package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivitySessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val scheduledDate: Long,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isRunning: Boolean = false
)
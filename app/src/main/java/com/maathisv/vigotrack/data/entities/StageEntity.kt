package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stages")
data class StageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: Long,
    val endDate: Long
)

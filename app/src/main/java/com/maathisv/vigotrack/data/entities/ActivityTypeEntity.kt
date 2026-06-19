package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_types")
data class ActivityTypeEntity(
    @PrimaryKey val name: String,
    val displayName: String,
    val category: String
)

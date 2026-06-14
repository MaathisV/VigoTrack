package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sensors",
    indices = [
        Index(value = ["address"], unique = true),
        Index(value = ["lastSeen"])  // for querying recent sensors
    ]
)

data class SensorEntity(
    @PrimaryKey val deviceId: String,
    val address: String,
    val name: String,
    val displayName: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val vendor: String = "polar"
)
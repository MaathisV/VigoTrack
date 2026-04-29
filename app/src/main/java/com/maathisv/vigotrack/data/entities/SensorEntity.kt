package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensors")
data class SensorEntity(
    @PrimaryKey val deviceId: String,
    val address: String,
    val name: String,
    val lastSeen: Long = System.currentTimeMillis()
)
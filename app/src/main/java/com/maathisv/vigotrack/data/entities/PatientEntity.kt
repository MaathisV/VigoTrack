package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCalibrated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

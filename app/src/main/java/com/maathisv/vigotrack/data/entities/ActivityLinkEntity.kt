package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_links",
    indices = [  // ADD THIS
        Index(value = ["parentActivityId"]),
        Index(value = ["sensorId"]),
        Index(value = ["patientId"])
              ],
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentActivityId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(  // ADD THIS
            entity = SensorEntity::class,
            parentColumns = ["deviceId"],
            childColumns = ["sensorId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ActivityLinkEntity(
    @PrimaryKey(autoGenerate = true) val linkId: Long = 0,
    val parentActivityId: String,
    val patientId: String,
    val sensorId: String,
    val features: String
)
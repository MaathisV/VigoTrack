package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_links",
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentActivityId"],
            onDelete = ForeignKey.CASCADE // Clean up links if activity is deleted
        )
    ]
)
data class ActivityLinkEntity(
    @PrimaryKey(autoGenerate = true) val linkId: Long = 0,
    val parentActivityId: String,
    val patientId: String,
    val sensorId: String,
    val featuresJson: String
)
package com.maathisv.vigotrack.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensor_patient_links",
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["sensorId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = SensorEntity::class,
            parentColumns = ["deviceId"],
            childColumns = ["sensorId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class SensorPatientLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long?,
    val sensorId: String,
    val features: String
)

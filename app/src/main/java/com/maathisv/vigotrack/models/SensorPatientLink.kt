package com.maathisv.vigotrack.models

data class SensorPatientLink(
    val id: Long = 0,
    val patientId: Long?,
    val sensorId: String,
    val features: List<String> = emptyList()
)

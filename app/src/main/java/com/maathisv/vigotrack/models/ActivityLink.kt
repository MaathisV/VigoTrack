package com.maathisv.vigotrack.models

data class ActivityLink(
    val patientId: String,
    val sensorMac: String,
    val featuresToTrack: List<String> = emptyList()
)
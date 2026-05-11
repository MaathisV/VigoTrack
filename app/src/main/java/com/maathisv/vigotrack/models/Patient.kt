package com.maathisv.vigotrack.models

data class Patient(
    val id: Long = 0,
    val name: String,
    val isCalibrated: Boolean = false
)
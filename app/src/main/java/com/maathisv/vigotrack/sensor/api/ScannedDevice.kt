package com.maathisv.vigotrack.sensor.api

data class ScannedDevice(
    val deviceId: String,
    val address: String,
    val name: String,
    val vendorName: String
)

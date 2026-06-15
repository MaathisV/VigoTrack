package com.maathisv.vigotrack.models

data class Sensor(
    var address: String, // MAC address
    val deviceId: String, // unique ID
    val name: String = "Polar Pacer Pro",
    val displayName: String? = null,
    val imageUrl: String = "",
    val vendor: String = "polar"
) {
    val effectiveName: String get() = displayName ?: name
}
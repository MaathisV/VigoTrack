package com.maathisv.vigotrack.util

import com.maathisv.vigotrack.sensor.api.SensorDataPoint

interface DataSerializer {
    val contentType: String
    val urlPath: String
    val extraHeaders: Map<String, String>
    fun serialize(deviceId: String, dataType: String, points: List<SensorDataPoint>, tags: Map<String, String>): String
}
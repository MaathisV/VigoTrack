package com.maathisv.vigotrack.sensor.api

sealed interface SensorEvent {
    data class DeviceConnecting(val deviceId: String) : SensorEvent
    data class DeviceConnected(val deviceId: String, val address: String, val name: String) : SensorEvent
    data class DeviceDisconnected(val deviceId: String) : SensorEvent
    data class FeaturesReady(val deviceId: String, val dataTypes: Set<SensorDataType>) : SensorEvent
    data class StreamStarted(val deviceId: String, val dataType: SensorDataType) : SensorEvent
    data class DataReceived(val deviceId: String, val dataPoint: SensorDataPoint) : SensorEvent
    data class Error(val deviceId: String, val error: Throwable) : SensorEvent
}

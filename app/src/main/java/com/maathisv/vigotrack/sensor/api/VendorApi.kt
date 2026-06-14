package com.maathisv.vigotrack.sensor.api

import kotlinx.coroutines.flow.Flow

interface VendorApi {
    val vendorName: String
    fun startScanning(): Flow<ScannedDevice>
    suspend fun connectToDevice(deviceId: String)
    fun disconnectFromDevice(deviceId: String)
    fun getAvailableDataTypes(deviceId: String): Set<SensorDataType>
    fun startStreaming(deviceId: String, dataType: SensorDataType, settings: Any? = null)
    fun stopStreaming(deviceId: String, dataType: SensorDataType)
    val events: Flow<SensorEvent>
    fun onForegroundEntered() = Unit
}

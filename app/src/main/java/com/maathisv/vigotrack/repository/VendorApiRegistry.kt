package com.maathisv.vigotrack.repository

import com.maathisv.vigotrack.sensor.api.ScannedDevice
import com.maathisv.vigotrack.sensor.api.SensorDataType
import com.maathisv.vigotrack.sensor.api.SensorEvent
import com.maathisv.vigotrack.sensor.api.VendorApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class VendorApiRegistry(
    private val vendors: List<VendorApi>
) {
    val allEvents: Flow<SensorEvent> = merge(*vendors.map { it.events }.toTypedArray())

    fun findByVendorName(name: String): VendorApi? =
        vendors.find { it.vendorName == name }

    fun allScanResults(): Flow<ScannedDevice> =
        merge(*vendors.map { it.startScanning() }.toTypedArray())

    suspend fun connectToDevice(deviceId: String, vendorName: String) {
        findByVendorName(vendorName)?.connectToDevice(deviceId)
    }

    fun disconnectFromDevice(deviceId: String, vendorName: String) {
        findByVendorName(vendorName)?.disconnectFromDevice(deviceId)
    }

    fun onForegroundEntered() {
        vendors.forEach { it.onForegroundEntered() }
    }

    fun getAvailableDataTypes(deviceId: String, vendorName: String): Set<SensorDataType> =
        findByVendorName(vendorName)?.getAvailableDataTypes(deviceId) ?: emptySet()

    fun startStreaming(deviceId: String, vendorName: String, dataType: SensorDataType, settings: Any? = null) {
        findByVendorName(vendorName)?.startStreaming(deviceId, dataType, settings)
    }

    fun stopStreaming(deviceId: String, vendorName: String, dataType: SensorDataType) {
        findByVendorName(vendorName)?.stopStreaming(deviceId, dataType)
    }
}

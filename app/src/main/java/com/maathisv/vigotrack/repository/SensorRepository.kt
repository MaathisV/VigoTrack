package com.maathisv.vigotrack.repository

import android.content.Context
import android.util.Log
import com.maathisv.vigotrack.data.SensorDataSource
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.StreamIdentifier
import com.maathisv.vigotrack.sensor.api.SensorDataPoint
import com.maathisv.vigotrack.sensor.api.SensorDataType
import com.maathisv.vigotrack.sensor.api.SensorEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SensorRepository(
    private val context: Context,
    private val dataSource: SensorDataSource,
    private val vendorRegistry: VendorApiRegistry
) {
    private val tag = "VigoTrack"
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(tag, "Unhandled error in repository scope", throwable)
    }
    private val repositoryScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)


    private val _deviceConnectionStates = MutableStateFlow<Map<String, ConnectionState>>(emptyMap())
    val deviceConnectionStates: StateFlow<Map<String, ConnectionState>> = _deviceConnectionStates.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = _deviceConnectionStates
        .map { states ->
            when {
                states.any { it.value == ConnectionState.FEATURES_READY } -> ConnectionState.FEATURES_READY
                states.any { it.value == ConnectionState.CONNECTED } -> ConnectionState.CONNECTED
                states.any { it.value == ConnectionState.CONNECTING } -> ConnectionState.CONNECTING
                else -> ConnectionState.NOT_CONNECTED
            }
        }
        .stateIn(repositoryScope, SharingStarted.Eagerly, ConnectionState.NOT_CONNECTED)

    private val _connectedDeviceIds = MutableStateFlow<Set<String>>(emptySet())
    val connectedDeviceIds: StateFlow<Set<String>> = _connectedDeviceIds.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<Set<Sensor>>(emptySet())
    val discoveredDevices: StateFlow<Set<Sensor>> = _discoveredDevices.asStateFlow()

    val savedSensors: Flow<List<Sensor>> = dataSource.getSavedSensors()

    private val _availableStreamDataTypes = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val availableStreamDataTypes: StateFlow<Map<String, Set<String>>> = _availableStreamDataTypes.asStateFlow()

    private val connectingLock = mutableSetOf<String>()
    private val pendingReconnectStreams = mutableMapOf<String, Set<String>>()
    private val activeStreams = mutableSetOf<StreamIdentifier>()
    private val previousHr = mutableMapOf<String, Int>()

    private val _liveData = MutableStateFlow<Map<String, Map<String, Any>>>(emptyMap())
    val liveData = _liveData.asStateFlow()

    private val _sensorDataFlow = MutableSharedFlow<Pair<String, SensorDataPoint>>(extraBufferCapacity = 64)
    val sensorDataFlow: SharedFlow<Pair<String, SensorDataPoint>> = _sensorDataFlow.asSharedFlow()

    init {
        observeVendorEvents()
    }

    private fun observeVendorEvents() {
        repositoryScope.launch {
            vendorRegistry.allEvents.collect { event ->
                when (event) {
                    is SensorEvent.DeviceConnecting -> {
                        _deviceConnectionStates.update { it + (event.deviceId to ConnectionState.CONNECTING) }
                    }
                    is SensorEvent.DeviceConnected -> {
                        _connectedDeviceIds.update { it + event.deviceId }
                        _deviceConnectionStates.update { it + (event.deviceId to ConnectionState.CONNECTED) }
                        saveDiscoveredDevice(event.deviceId, event.address, event.name)
                    }
                    is SensorEvent.DeviceDisconnected -> {
                        _connectedDeviceIds.update { it - event.deviceId }
                        _availableStreamDataTypes.update { it - event.deviceId }
                        _deviceConnectionStates.update { it - event.deviceId }
                        previousHr.remove(event.deviceId)
                        val deviceStreams = activeStreams.filter { it.first == event.deviceId }.map { it.second }.toSet()
                        if (deviceStreams.isNotEmpty()) {
                            pendingReconnectStreams[event.deviceId] = deviceStreams
                        }
                        activeStreams.removeAll { it.first == event.deviceId }
                    }
                    is SensorEvent.FeaturesReady -> {
                        _deviceConnectionStates.update { it + (event.deviceId to ConnectionState.FEATURES_READY) }
                        _availableStreamDataTypes.update { current ->
                            val existing = current[event.deviceId] ?: emptySet()
                            current + (event.deviceId to (existing + event.dataTypes.map { it.name }))
                        }
                        pendingReconnectStreams[event.deviceId]?.let { savedFeatures ->
                            val available = _availableStreamDataTypes.value[event.deviceId] ?: emptySet()
                            val (ready, remaining) = savedFeatures.partition { it in available }
                            if (remaining.isEmpty()) pendingReconnectStreams.remove(event.deviceId)
                            else pendingReconnectStreams[event.deviceId] = remaining.toSet()
                            ready.forEach { feature -> startFeatureStream(event.deviceId, feature) }
                        }
                    }
                    is SensorEvent.StreamStarted -> { /* logged by vendor */ }
                    is SensorEvent.DataReceived -> {
                        val corrected = when (val dp = event.dataPoint) {
                            is SensorDataPoint.HeartRate -> dp.hr.correctZero(event.deviceId, previousHr) { hr -> dp.copy(hr = hr) }
                            is SensorDataPoint.Ppi -> dp.hr.correctZero(event.deviceId, previousHr) { hr -> dp.copy(hr = hr) }
                            else -> dp
                        }
                        if (corrected != null) {
                            _sensorDataFlow.tryEmit(event.deviceId to corrected)
                            updateLiveDataFromDataPoint(event.deviceId, corrected)
                        }
                    }
                    is SensorEvent.Error -> {
                        Log.e(tag, "Vendor error for ${event.deviceId}", event.error)
                    }
                }
            }
        }
    }

    private suspend fun saveDiscoveredDevice(deviceId: String, address: String, name: String) {
        val existing = dataSource.getSavedSensors().first().find { it.deviceId == deviceId }
        if (existing == null) {
            dataSource.saveSensor(
                Sensor(
                    deviceId = deviceId,
                    address = address,
                    name = name
                )
            )
        }
    }

    private suspend fun connectWithRetry(deviceId: String, vendorName: String = "polar", maxRetries: Int = 3) {
        if (!connectingLock.add(deviceId)) {
            Log.d(tag, "Connection already in progress for $deviceId, skipping")
            return
        }
        try {
            _deviceConnectionStates.update { it + (deviceId to ConnectionState.CONNECTING) }
            repeat(maxRetries) { attempt ->
                try {
                    vendorRegistry.connectToDevice(deviceId, vendorName)
                    return
                } catch (e: Exception) {
                    if (attempt < maxRetries - 1) {
                        Log.w(tag, "Retry $attempt for $deviceId: ${e.message}")
                        delay(2000)
                    }
                }
            }
            _deviceConnectionStates.update { it + (deviceId to ConnectionState.NOT_CONNECTED) }
            Log.e(tag, "Could not reconnect to $deviceId after $maxRetries attempts")
        } finally {
            connectingLock.remove(deviceId)
        }
    }

    private fun Int.correctZero(
        deviceId: String,
        previousMap: MutableMap<String, Int>,
        onSubstitute: (corrected: Int) -> SensorDataPoint
    ): SensorDataPoint? {
        if (this == 0) {
            val prev = previousMap[deviceId] ?: return null
            return onSubstitute(prev)
        }
        previousMap[deviceId] = this
        return onSubstitute(this)
    }

    private fun updateLiveDataFromDataPoint(deviceId: String, dp: SensorDataPoint) {
        when (dp) {
            is SensorDataPoint.HeartRate -> {
                updateLiveData(deviceId, "HR", dp.hr)
            }
            is SensorDataPoint.Ppi -> {
                updateLiveData(deviceId, "PPI", dp.ppiMs)
            }
            is SensorDataPoint.Accelerometer -> {
                _liveData.update { currentMap ->
                    val deviceMap = currentMap[deviceId].orEmpty() + mapOf(
                        "ACC_X" to dp.x,
                        "ACC_Y" to dp.y,
                        "ACC_Z" to dp.z
                    )
                    currentMap + (deviceId to deviceMap)
                }
            }
            is SensorDataPoint.Electrocardiogram -> {
                updateLiveData(deviceId, "ECG", dp.voltage)
            }
            is SensorDataPoint.EulerAngles -> {
                _liveData.update { currentMap ->
                    val deviceMap = currentMap[deviceId].orEmpty() + mapOf(
                        "EULER_ROLL" to dp.roll,
                        "EULER_PITCH" to dp.pitch,
                        "EULER_YAW" to dp.yaw
                    )
                    currentMap + (deviceId to deviceMap)
                }
            }
            is SensorDataPoint.Quaternion -> {
                _liveData.update { currentMap ->
                    val deviceMap = currentMap[deviceId].orEmpty() + mapOf(
                        "QUAT_W" to dp.w, "QUAT_X" to dp.x,
                        "QUAT_Y" to dp.y, "QUAT_Z" to dp.z
                    )
                    currentMap + (deviceId to deviceMap)
                }
            }
            is SensorDataPoint.FreeAcceleration -> {
                _liveData.update { currentMap ->
                    val deviceMap = currentMap[deviceId].orEmpty() + mapOf(
                        "FREE_ACC_X" to dp.x, "FREE_ACC_Y" to dp.y, "FREE_ACC_Z" to dp.z
                    )
                    currentMap + (deviceId to deviceMap)
                }
            }
            is SensorDataPoint.Gyroscope -> {
                _liveData.update { currentMap ->
                    val deviceMap = currentMap[deviceId].orEmpty() + mapOf(
                        "GYRO_X" to dp.x, "GYRO_Y" to dp.y, "GYRO_Z" to dp.z
                    )
                    currentMap + (deviceId to deviceMap)
                }
            }
        }
    }

    fun startScanning() {
        _discoveredDevices.value = emptySet()
        vendorRegistry.allScanResults()
            .onEach { info ->
                val newSensor = Sensor(
                    deviceId = info.deviceId.ifEmpty { info.address },
                    address = info.address,
                    name = info.name,
                    vendor = info.vendorName
                )
                _discoveredDevices.update { it + newSensor }
            }
            .launchIn(repositoryScope)
    }

    suspend fun connectToDevice(sensor: Sensor) {
        connectWithRetry(sensor.deviceId, sensor.vendor)
    }

    fun requestDisconnect(deviceId: String) {
        _availableStreamDataTypes.value.keys.firstOrNull { it == deviceId }?.let { id ->
            getVendorForDevice(deviceId)?.let { vendor ->
                vendorRegistry.disconnectFromDevice(deviceId, vendor)
            }
        }
    }

    suspend fun forgetDevice(deviceId: String) {
        Log.d(tag, "Forgetting device: $deviceId")
        requestDisconnect(deviceId)
        stopActivityStreaming(deviceId)
        _connectedDeviceIds.update { it - deviceId }
        _availableStreamDataTypes.update { it - deviceId }
        _deviceConnectionStates.update { it - deviceId }
        _liveData.update { it - deviceId }
        previousHr.remove(deviceId)
        dataSource.deleteSensor(deviceId)
        Log.d(tag, "Device forgotten: $deviceId")
    }

    private fun getVendorForDevice(deviceId: String): String? {
        val saved = dataSource.getSavedSensors().let { flow ->
            runBlocking {
                flow.first().find { it.deviceId == deviceId }
            }
        }
        return saved?.vendor
    }

    fun onForegroundEntered() {
        vendorRegistry.onForegroundEntered()
    }

    fun updateSensorDisplayName(deviceId: String, name: String) {
        repositoryScope.launch {
            dataSource.updateSensorName(deviceId, name)
        }
    }

    private fun updateLiveData(deviceId: String, feature: String, value: Any) {
        _liveData.update { currentMap ->
            val deviceMap = currentMap[deviceId].orEmpty() + (feature to value)
            currentMap + (deviceId to deviceMap)
        }
    }

    fun getAvailableFeaturesForDevice(deviceId: String): Set<String> {
        return _availableStreamDataTypes.value[deviceId] ?: emptySet()
    }

    suspend fun getAvailableSettings(deviceId: String, feature: String): Map<String, Set<Int>>? {
        val vendor = getVendorForDevice(deviceId) ?: return null
        val dataType = try { SensorDataType.valueOf(feature) } catch (_: IllegalArgumentException) { return null }
        return vendorRegistry.getAvailableSettings(deviceId, vendor, dataType)
    }

    fun startFeatureStream(deviceId: String, feature: String, settings: Any? = null) {
        val available = _availableStreamDataTypes.value[deviceId] ?: emptySet()
        if (feature !in available) {
            Log.w(tag, "Cannot start $feature yet. $deviceId is not ready.")
            return
        }

        val id = StreamIdentifier(deviceId, feature)
        if (id in activeStreams) {
            Log.d(tag, "Stream $feature already active for $deviceId. Skipping.")
            return
        }

        val vendor = getVendorForDevice(deviceId) ?: "polar"
        val dataType = SensorDataType.valueOf(feature)

        vendorRegistry.startStreaming(deviceId, vendor, dataType, settings)
        activeStreams.add(id)
        Log.d(tag, "Started streaming $feature for $deviceId via $vendor")
    }

    fun stopActivityStreaming(deviceId: String) {
        val vendor = getVendorForDevice(deviceId)
        activeStreams
            .filter { it.first == deviceId }
            .toSet()
            .forEach { id ->
                if (vendor != null) {
                    val dataType = SensorDataType.valueOf(id.second)
                    vendorRegistry.stopStreaming(deviceId, vendor, dataType)
                }
                activeStreams.remove(id)
            }
    }
}
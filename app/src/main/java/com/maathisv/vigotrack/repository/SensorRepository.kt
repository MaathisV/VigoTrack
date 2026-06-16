package com.maathisv.vigotrack.repository

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.RequiresPermission
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
    private val TAG = "VigoTrack"
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled error in repository scope", throwable)
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

    private val activeStreams = mutableSetOf<StreamIdentifier>()
    private val previousHr = mutableMapOf<String, Int>()

    private val _liveData = MutableStateFlow<Map<String, Map<String, Any>>>(emptyMap())
    val liveData = _liveData.asStateFlow()

    private val _sensorDataFlow = MutableSharedFlow<Pair<String, SensorDataPoint>>(extraBufferCapacity = 64)
    val sensorDataFlow: SharedFlow<Pair<String, SensorDataPoint>> = _sensorDataFlow.asSharedFlow()

    init {
        observeVendorEvents()
        autoReconnectToSavedDevices()
        registerBleAclReceiver()
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
                    }
                    is SensorEvent.FeaturesReady -> {
                        _deviceConnectionStates.update { it + (event.deviceId to ConnectionState.FEATURES_READY) }
                        _availableStreamDataTypes.update { current ->
                            val existing = current[event.deviceId] ?: emptySet()
                            current + (event.deviceId to (existing + event.dataTypes.map { it.name }))
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
                        Log.e(TAG, "Vendor error for ${event.deviceId}", event.error)
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

    private fun autoReconnectToSavedDevices() {
        repositoryScope.launch {
            dataSource.getSavedSensors().first().forEach { saved ->
                Log.d(TAG, "Auto-reconnecting to saved device: ${saved.deviceId} (${saved.name})")
                connectWithRetry(saved.deviceId, saved.vendor, saved.address)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun connectWithRetry(deviceId: String, vendorName: String = "polar", address: String? = null, maxRetries: Int = 3) {
        if (address != null && isDeviceSystemConnected(address) && deviceId !in _connectedDeviceIds.value) {
            Log.d(TAG, "Forcing clean reconnection for $deviceId")
            _deviceConnectionStates.update { it + (deviceId to ConnectionState.CONNECTING) }
            try {
                vendorRegistry.disconnectFromDevice(deviceId, vendorName)
                delay(500)
                vendorRegistry.connectToDevice(deviceId, vendorName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-establish session for $deviceId", e)
                _deviceConnectionStates.update { it + (deviceId to ConnectionState.NOT_CONNECTED) }
            }
            return
        }
        _deviceConnectionStates.update { it + (deviceId to ConnectionState.CONNECTING) }
        repeat(maxRetries) { attempt ->
            try {
                vendorRegistry.connectToDevice(deviceId, vendorName)
                return
            } catch (e: Exception) {
                if (attempt < maxRetries - 1) {
                    Log.w(TAG, "Retry $attempt for $deviceId: ${e.message}")
                    delay(2000)
                }
            }
        }
        _deviceConnectionStates.update { it + (deviceId to ConnectionState.NOT_CONNECTED) }
        Log.e(TAG, "Could not reconnect to $deviceId after $maxRetries attempts")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun isDeviceSystemConnected(address: String): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter ?: return false
            val device = adapter.getRemoteDevice(address)
            bluetoothManager.getConnectionState(device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
        } catch (e: Exception) {
            false
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

    private fun registerBleAclReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.applicationContext.registerReceiver(bleAclReceiver, filter)
    }

    private val bleAclReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            val macAddress = device?.address ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> handleSystemBleReconnection(macAddress)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleSystemBleDisconnection(macAddress)
            }
        }
    }

    private fun handleSystemBleReconnection(macAddress: String) {
        repositoryScope.launch {
            val knownDevices = dataSource.getSavedSensors().first()
            val saved = knownDevices.firstOrNull { it.deviceId == macAddress || it.address == macAddress }
            if (saved != null && saved.deviceId !in _connectedDeviceIds.value) {
                Log.d(TAG, "System BLE reconnection detected for: ${saved.deviceId}")
                connectWithRetry(saved.deviceId, saved.vendor, saved.address)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleSystemBleDisconnection(macAddress: String) {
        repositoryScope.launch {
            val saved = dataSource.getSavedSensors().first().firstOrNull {
                it.deviceId == macAddress || it.address == macAddress
            }
            if (saved == null || saved.deviceId !in _connectedDeviceIds.value) return@launch
            delay(2000)
            if (!isDeviceSystemConnected(saved.address)) {
                Log.d(TAG, "System BLE disconnection confirmed for: ${saved.deviceId}")
                _connectedDeviceIds.update { it - saved.deviceId }
                _availableStreamDataTypes.update { it - saved.deviceId }
                _deviceConnectionStates.update { it - saved.deviceId }
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
        connectWithRetry(sensor.deviceId, sensor.vendor, sensor.address)
    }

    fun requestDisconnect(deviceId: String) {
        _availableStreamDataTypes.value.keys.firstOrNull { it == deviceId }?.let { id ->
            getVendorForDevice(deviceId)?.let { vendor ->
                vendorRegistry.disconnectFromDevice(deviceId, vendor)
            }
        }
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

    fun startFeatureStream(deviceId: String, feature: String, settings: Any? = null) {
        val available = _availableStreamDataTypes.value[deviceId] ?: emptySet()
        if (feature !in available) {
            Log.w(TAG, "Cannot start $feature yet. $deviceId is not ready.")
            return
        }

        val id = StreamIdentifier(deviceId, feature)
        if (id in activeStreams) {
            Log.d(TAG, "Stream $feature already active for $deviceId. Skipping.")
            return
        }

        val vendor = getVendorForDevice(deviceId) ?: "polar"
        val dataType = SensorDataType.valueOf(feature)

        vendorRegistry.startStreaming(deviceId, vendor, dataType, settings)
        activeStreams.add(id)
        Log.d(TAG, "Started streaming $feature for $deviceId via $vendor")
    }

    suspend fun startActivityStreaming(session: ActivitySession) {
        session.links.forEach { link ->
            link.featuresToTrack.forEach { feature ->
                startFeatureStream(link.sensorId, feature)
                delay(300)
            }
        }
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
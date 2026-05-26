package com.maathisv.vigotrack.repository

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.maathisv.vigotrack.data.SensorDataSource
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.StreamIdentifier
import com.polar.androidcommunications.api.ble.model.DisInfo
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarBleApi.PolarDeviceDataType
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarHealthThermometerData
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarPpiData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class SensorRepository(
    private val context: Context,
    private val dataSource: SensorDataSource
) {
    private val TAG = "VigoTrack"
    private val repositoryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING)
        )
    }

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

    private val _readyFeatures = MutableStateFlow<Map<String, Set<PolarBleApi.PolarBleSdkFeature>>>(emptyMap())

    private val _availableStreamDataTypes = MutableStateFlow<Map<String, Set<PolarDeviceDataType>>>(emptyMap())
    val availableStreamDataTypes: StateFlow<Map<String, Set<PolarDeviceDataType>>> = _availableStreamDataTypes.asStateFlow()

    private val activeJobs = mutableMapOf<StreamIdentifier, Job>()

    private val _liveData = MutableStateFlow<Map<String, Map<String, Any>>>(emptyMap())
    val liveData = _liveData.asStateFlow()

    private val _hrLogFlow = MutableSharedFlow<Pair<String, PolarHrData>>(extraBufferCapacity = 64)
    val hrLogFlow: SharedFlow<Pair<String, PolarHrData>> = _hrLogFlow.asSharedFlow()

    private val _ppiLogFlow = MutableSharedFlow<Pair<String, PolarPpiData>>(extraBufferCapacity = 64)
    val ppiLogFlow: SharedFlow<Pair<String, PolarPpiData>> = _ppiLogFlow.asSharedFlow()

    private val _accLogFlow = MutableSharedFlow<Pair<String, PolarAccelerometerData>>(extraBufferCapacity = 64)
    val accLogFlow: SharedFlow<Pair<String, PolarAccelerometerData>> = _accLogFlow.asSharedFlow()

    private val _ecgLogFlow = MutableSharedFlow<Pair<String, PolarEcgData>>(extraBufferCapacity = 64)
    val ecgLogFlow: SharedFlow<Pair<String, PolarEcgData>> = _ecgLogFlow.asSharedFlow()

    init {
        setupPolarCallbacks()
        autoReconnectToSavedDevices()
        registerBleAclReceiver()
    }

    private fun autoReconnectToSavedDevices() {
        repositoryScope.launch {
            // delay(5000)
            dataSource.getSavedSensors().first().forEach { saved ->
                Log.d(TAG, "Auto-reconnecting to saved device: ${saved.deviceId} (${saved.name})")
                connectWithRetry(saved.deviceId, saved.address)
                delay(5000)
            }
        }
    }

    private suspend fun connectWithRetry(deviceId: String, address: String? = null, maxRetries: Int = 3) {
        _deviceConnectionStates.update { it + (deviceId to ConnectionState.CONNECTING) }
        repeat(maxRetries) { attempt ->
            try {
                api.connectToDevice(deviceId)
                delay(2000)
                if (deviceId in _connectedDeviceIds.value) return
                if (address != null && isDeviceSystemConnected(address)) {
                    _connectedDeviceIds.update { it + deviceId }
                    _deviceConnectionStates.update { it + (deviceId to ConnectionState.CONNECTED) }
                    return
                }
                if (attempt < maxRetries - 1) {
                    Log.w(TAG, "Connection to $deviceId not confirmed on attempt $attempt, retrying...")
                }
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

    private fun setupPolarCallbacks() {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "Phone BLE is: ${if (powered) "ON" else "OFF"}")
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                _deviceConnectionStates.update { it + (polarDeviceInfo.deviceId to ConnectionState.CONNECTING) }
                Log.d("VigoTrack", "Connecting to: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Connected to ${polarDeviceInfo.deviceId}")
                _connectedDeviceIds.update { it + polarDeviceInfo.deviceId }
                _deviceConnectionStates.update { it + (polarDeviceInfo.deviceId to ConnectionState.CONNECTED) }

                repositoryScope.launch {
                    dataSource.saveSensor(
                        Sensor(
                            deviceId = polarDeviceInfo.deviceId,
                            address = polarDeviceInfo.address,
                            name = polarDeviceInfo.name
                        )
                    )
                }
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Disconnected from ${polarDeviceInfo.deviceId}")
                _connectedDeviceIds.update { it - polarDeviceInfo.deviceId }
                _readyFeatures.update { it - polarDeviceInfo.deviceId }
                _availableStreamDataTypes.update { it - polarDeviceInfo.deviceId }
                _deviceConnectionStates.update { it - polarDeviceInfo.deviceId }
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d(TAG, "Feature Ready: $feature for $identifier")

                _readyFeatures.update { currentMap ->
                    val features = currentMap[identifier] ?: emptySet()
                    currentMap + (identifier to (features + feature))
                }
            }

            override fun bleSdkFeaturesReadiness(identifier: String, ready: List<PolarBleApi.PolarBleSdkFeature>, unavailable: List<PolarBleApi.PolarBleSdkFeature>) {
                Log.d(TAG, "Features readiness for $identifier. Ready: $ready, Unavailable: $unavailable")
                _readyFeatures.update { currentMap ->
                    val existing = currentMap[identifier] ?: emptySet()
                    currentMap + (identifier to (existing + ready.toSet()))
                }

                if (ready.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING)) {
                    repositoryScope.launch {
                        try {
                            val types = api.getAvailableOnlineStreamDataTypes(identifier)
                            updateAvailableDataTypes(identifier, types)
                            Log.d(TAG, "Available online stream types for $identifier: $types")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to get online stream types for $identifier", e)
                        }
                    }
                }

                if (ready.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)) {
                    repositoryScope.launch {
                        try {
                            val types = api.getAvailableHRServiceDataTypes(identifier)
                            updateAvailableDataTypes(identifier, types)
                            Log.d(TAG, "Available HR service types for $identifier: $types")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to get HR service types for $identifier", e)
                        }
                    }
                }
            }

            override fun disInformationReceived(identifier: String, disInfo: DisInfo) {
                Log.d("VigoTrack", "DIS info received from $identifier: $disInfo")
            }

            override fun htsNotificationReceived(identifier: String, data: PolarHealthThermometerData) {}

        })
    }

    private fun registerBleAclReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        context.applicationContext.registerReceiver(bleAclReceiver, filter)
    }

    private val bleAclReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                device?.let {
                    val macAddress = it.address ?: return@let
                    handleSystemBleReconnection(macAddress)
                }
            }
        }
    }

    private fun handleSystemBleReconnection(macAddress: String) {
        repositoryScope.launch {
            val knownDevices = dataSource.getSavedSensors().first()
            val saved = knownDevices.firstOrNull { it.deviceId == macAddress || it.address == macAddress }
            if (saved != null && saved.deviceId !in _connectedDeviceIds.value) {
                Log.d(TAG, "System BLE reconnection detected for: ${saved.deviceId}")
                connectWithRetry(saved.deviceId, saved.address)
            }
        }
    }

    fun startScanning() {
        _discoveredDevices.value = emptySet()
        api.searchForDevice()
            .onEach { info ->
                val newSensor = Sensor(
                    deviceId = info.deviceId.ifEmpty { info.address },
                    address = info.address,
                    name = info.name
                )
                _discoveredDevices.update { it + newSensor }
            }
            .launchIn(repositoryScope)
    }

    fun requestConnect(sensor: Sensor) {
        try {
            val identifier = sensor.deviceId.ifEmpty { sensor.address }
            Log.d(TAG, "Attempting connection to identifier: $identifier")
            api.connectToDevice(identifier)
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
        }
    }

    fun requestDisconnect(deviceId: String) {
        api.disconnectFromDevice(deviceId)
    }

    fun connectToDevice(sensor: Sensor) {
        val identifier = sensor.deviceId.ifEmpty { sensor.address }
        connectByIdentifier(identifier)
    }

    private fun connectByIdentifier(identifier: String) {
        if (_connectedDeviceIds.value.contains(identifier)) {
            Log.d(TAG, "Already connected to $identifier, skipping.")
            return
        }
        Log.d(TAG, "Connecting to: $identifier")
        _deviceConnectionStates.update { it + (identifier to ConnectionState.CONNECTING) }
        try {
            api.connectToDevice(identifier)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to $identifier", e)
            _deviceConnectionStates.update { it + (identifier to ConnectionState.NOT_CONNECTED) }
        }
    }

    private fun updateLiveData(deviceId: String, feature: String, value: Any) {
        _liveData.update { currentMap ->
            val deviceMap = currentMap[deviceId].orEmpty() + (feature to value)
            currentMap + (deviceId to deviceMap)
        }
    }

    private fun updateAvailableDataTypes(deviceId: String, types: Set<PolarDeviceDataType>) {
        _availableStreamDataTypes.update { current ->
            val existing = current[deviceId] ?: emptySet()
            current + (deviceId to (existing + types))
        }
    }

    fun getAvailableFeaturesForDevice(deviceId: String): Set<String> {
        val types = _availableStreamDataTypes.value[deviceId] ?: return emptySet()
        return types.mapNotNull { type ->
            when (type) {
                PolarDeviceDataType.HR -> "HR"
                PolarDeviceDataType.PPI -> "PPI"
                PolarDeviceDataType.ACC -> "ACC"
                PolarDeviceDataType.ECG -> "ECG"
                else -> null
            }
        }.toSet()
    }

    suspend fun getSupportedSettings(deviceId: String, feature: PolarBleApi.PolarDeviceDataType): PolarSensorSetting {
        return api.requestStreamSettings(deviceId, feature)
    }

    fun startFeatureStream(deviceId: String, feature: String, settings: PolarSensorSetting? = null) {
        val readyForDevice = _readyFeatures.value[deviceId] ?: emptySet()

        val polarFeature = when(feature) {
            "HR" -> PolarBleApi.PolarBleSdkFeature.FEATURE_HR
            "PPI" -> PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
            "ACC" -> PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
            "ECG" -> PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
            else -> null
        }

        if (polarFeature != null && !readyForDevice.contains(polarFeature)) {
            Log.w(TAG, "Cannot start $feature yet. $deviceId is not ready.")
            return
        }

        val id = StreamIdentifier(deviceId, feature)
        if (activeJobs.containsKey(id) && activeJobs[id]?.isActive == true) {
            Log.d(TAG, "Stream $feature already active for $deviceId. Skipping.")
            return
        }
        activeJobs[id]?.cancel()

        val job = when (feature) {
            "HR" -> api.startHrStreaming(deviceId)
                .onEach { data ->
                    _hrLogFlow.tryEmit(deviceId to data)
                    val sample = data.samples.first()
                    Log.d(TAG, "HR data received: ${sample.hr} for $deviceId")
                    updateLiveData(deviceId, "HR", sample.hr)
                }
                .catch { e ->
                    Log.e(TAG, "Stream failed: HR for $deviceId", e)
                }
                .launchIn(repositoryScope)

            "PPI" -> api.startPpiStreaming(deviceId)
                .onEach { data ->
                    _ppiLogFlow.tryEmit(deviceId to data)
                    val sample = data.samples.first()
                    Log.d(TAG, "PPI data received: ${sample.ppi} for $deviceId")
                    updateLiveData(deviceId, "PPI", sample.ppi)
                    updateLiveData(deviceId, "HR", sample.hr)
                }
                .catch { e -> Log.e(TAG, "Stream failed: PPI for $deviceId", e) }
                .launchIn(repositoryScope)

            "ACC" -> {
                repositoryScope.launch {
                    try {
                        val accSettings = settings ?: api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC).maxSettings()

                        api.startAccStreaming(deviceId, accSettings)
                            .onEach { data ->
                                _accLogFlow.tryEmit(deviceId to data)
                                val sample = data.samples.first()
                                _liveData.update { currentMap ->
                                    val deviceMap = currentMap[deviceId].orEmpty() + mapOf(
                                        "ACC_X" to sample.x,
                                        "ACC_Y" to sample.y,
                                        "ACC_Z" to sample.z
                                    )
                                    currentMap + (deviceId to deviceMap)
                                }
                            }
                            .catch { e -> Log.e(TAG, "ACC Stream internal fail", e) }
                            .launchIn(repositoryScope)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get ACC settings or start stream", e)
                    }
                }
                Job()
            }
            "ECG" -> {
                repositoryScope.launch {
                    try {
                        val ecgSettings = settings ?: api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ECG).maxSettings()

                        api.startEcgStreaming(deviceId, ecgSettings)
                            .onEach { data ->
                                _ecgLogFlow.tryEmit(deviceId to data)
                                val sample = data.samples.first()
                                val voltage = when (sample) {
                                    is com.polar.sdk.api.model.EcgSample -> sample.voltage
                                    is com.polar.sdk.api.model.FecgSample -> sample.ecg
                                }
                                _liveData.update { currentMap ->
                                    val deviceMap = currentMap[deviceId].orEmpty() + mapOf(
                                        "ECG" to voltage
                                    )
                                    currentMap + (deviceId to deviceMap)
                                }
                            }
                            .catch { e -> Log.e(TAG, "ECG Stream internal fail", e) }
                            .launchIn(repositoryScope)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get ECG settings or start stream", e)
                    }
                }
                Job()
            }
            else -> {
                Log.e(TAG, "Unsupported feature: $feature")
                Job()
            }
        }

        activeJobs[id] = job
        Log.d(TAG, "Started streaming $feature for $deviceId")
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
        activeJobs.keys
            .filter { it.first == deviceId }
            .toList()
            .forEach { id ->
                activeJobs[id]?.cancel()
                activeJobs.remove(id)
            }
        // api.disconnectFromDevice(deviceId)

        // 3. Optional: reconnect after a second if you want to keep it "Connected" but not "Streaming"
        // repositoryScope.launch {
            //delay(2000)
            //connectByIdentifier(deviceId)
        //}
    }


}
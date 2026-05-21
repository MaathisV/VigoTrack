package com.maathisv.vigotrack.repository

import android.content.Context
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
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHealthThermometerData
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

    private val _connectionState = MutableStateFlow(ConnectionState.NOT_CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceIds = MutableStateFlow<Set<String>>(emptySet())
    val connectedDeviceIds: StateFlow<Set<String>> = _connectedDeviceIds.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<Set<Sensor>>(emptySet())
    val discoveredDevices: StateFlow<Set<Sensor>> = _discoveredDevices.asStateFlow()

    private val _readyFeatures = MutableStateFlow<Map<String, Set<PolarBleApi.PolarBleSdkFeature>>>(emptyMap())


    private val activeJobs = mutableMapOf<StreamIdentifier, Job>()

    private val _liveData = MutableStateFlow<Map<String, Map<String, Any>>>(emptyMap())
    val liveData = _liveData.asStateFlow()


    init {
        setupPolarCallbacks()

        repositoryScope.launch {
            delay(5000)
            dataSource.getSavedSensors().first().forEach { savedSensor ->
                Log.d(TAG, "Attempting background reconnect to: ${savedSensor.deviceId}")
                try {
                    api.connectToDevice(savedSensor.deviceId)
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-connect failed for ${savedSensor.deviceId}", e)
                }
            }
        }
    }

    private fun setupPolarCallbacks() {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "Phone BLE is: ${if (powered) "ON" else "OFF"}")
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.CONNECTING
                Log.d("VigoTrack", "Connecting to: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Connected to ${polarDeviceInfo.deviceId}")
                _connectedDeviceIds.update { it + polarDeviceInfo.deviceId }
                _connectionState.value = ConnectionState.CONNECTED

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
                if (_connectedDeviceIds.value.isEmpty()) {
                    _connectionState.value = ConnectionState.NOT_CONNECTED
                }
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d(TAG, "Feature Ready: $feature for $identifier")

                _readyFeatures.update { currentMap ->
                    val features = currentMap[identifier] ?: emptySet()
                    currentMap + (identifier to (features + feature))
                }
            }

            override fun disInformationReceived(identifier: String, disInfo: DisInfo) {
                Log.d("VigoTrack", "DIS info received from $identifier: $disInfo")
            }

            override fun htsNotificationReceived(identifier: String, data: PolarHealthThermometerData) {}

        })
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
        _connectionState.value = ConnectionState.CONNECTING
        try {
            api.connectToDevice(identifier)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to $identifier", e)
            _connectionState.value = ConnectionState.NOT_CONNECTED
        }
    }

    private fun updateLiveData(deviceId: String, feature: String, value: Any) {
        _liveData.update { currentMap ->
            val deviceMap = currentMap[deviceId].orEmpty() + (feature to value)
            currentMap + (deviceId to deviceMap)
        }
    }

    suspend fun getSupportedSettings(deviceId: String, feature: PolarBleApi.PolarDeviceDataType): PolarSensorSetting {
        return api.requestStreamSettings(deviceId, feature)
    }

    fun startFeatureStream(deviceId: String, feature: String, settings: PolarSensorSetting? = null) {
        // 1. CHECK IF FEATURE IS READY
        // val readyForDevice = _readyFeatures.value[deviceId] ?: emptySet()

        // Map our string names to Polar SdkFeatures
        val polarFeature = when(feature) {
            "HR" -> PolarBleApi.PolarBleSdkFeature.FEATURE_HR
            "PPI" -> PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
            "ACC" -> PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
            else -> null
        }

        // If it's not ready yet, don't crash! Just log it and wait.
        /*if (polarFeature != null && !readyForDevice.contains(polarFeature)) {
            Log.w(TAG, "Cannot start $feature yet. $deviceId is not ready.")
            return
        }*/

        val id = StreamIdentifier(deviceId, feature)
        if (activeJobs.containsKey(id) && activeJobs[id]?.isActive == true) {
            Log.d(TAG, "Stream $feature already active for $deviceId. Skipping.")
            return
        }
        activeJobs[id]?.cancel()

        val job = when (feature) {
            "HR" -> api.startHrStreaming(deviceId)
                .onEach { data ->
                    val sample = data.samples.first()
                    Log.d(TAG, "HR data received: ${sample.hr} for $deviceId")
                    updateLiveData(deviceId, "HR", sample.hr)
                    updateLiveData(deviceId, "HR_SAMPLE", sample)
                }
                .catch { e ->
                    Log.e(TAG, "Stream failed: HR for $deviceId", e)
                }
                .launchIn(repositoryScope)

            "PPI" -> api.startPpiStreaming(deviceId)
                .onEach { data ->
                    val sample = data.samples.first()
                    Log.d(TAG, "PPI data received: ${sample.ppi} for $deviceId")
                    updateLiveData(deviceId, "PPI", sample.ppi)
                    updateLiveData(deviceId, "HR", sample.hr)
                    updateLiveData(deviceId, "PPI_SAMPLE", sample)
                }
                .catch { e -> Log.e(TAG, "Stream failed: PPI for $deviceId", e) }
                .launchIn(repositoryScope)

            "ACC" -> {
                repositoryScope.launch {
                    try {
                        // If settings are null, fetch default from device automatically
                        val accSettings = settings ?: api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC).maxSettings()

                        api.startAccStreaming(deviceId, accSettings)
                            .onEach { data ->
                                val sample = data.samples.first()
                                // Update live data with the first triplet for UI
                                updateLiveData(deviceId, "ACC_X", sample.x)
                                updateLiveData(deviceId, "ACC_Y", sample.y)
                                updateLiveData(deviceId, "ACC_Z", sample.z)
                                // Store the whole sample list if needed for logger
                                updateLiveData(deviceId, "ACC_FULL", data.samples)
                            }
                            .catch { e -> Log.e(TAG, "ACC Stream internal fail", e) }
                            .launchIn(repositoryScope)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get ACC settings or start stream", e)
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
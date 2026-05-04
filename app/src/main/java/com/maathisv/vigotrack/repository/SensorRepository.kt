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
            dataSource.getSavedSensors().first().forEach { savedSensor ->
                Log.d(TAG, "Auto-connecting to saved device: ${savedSensor.deviceId}")
                connectByIdentifier(savedSensor.deviceId)
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
        val id = StreamIdentifier(deviceId, feature)
        if (activeJobs.containsKey(id)) return

        val job = when (feature) {
            "HR" -> api.startHrStreaming(deviceId)
                .onEach { data -> updateLiveData(deviceId, "HR", data.samples.first().hr) }
                .catch { e -> Log.e(TAG, "Stream failed: HR", e) }
                .launchIn(repositoryScope)

            "ECG" -> {
                requireNotNull(settings) { "ECG requires settings" }
                api.startEcgStreaming(deviceId, settings)
                    .onEach { data -> updateLiveData(deviceId, "ECG", data.samples) }
                    .catch { e -> Log.e(TAG, "Stream failed: ECG", e) }
                    .launchIn(repositoryScope)
            }

            "ACC" -> {
                requireNotNull(settings) { "ACC requires settings" }
                api.startAccStreaming(deviceId, settings)
                    .onEach { data -> updateLiveData(deviceId, "ACC", data.samples) }
                    .catch { e -> Log.e(TAG, "Stream failed: ACC", e) }
                    .launchIn(repositoryScope)
            }
            else -> return
        }

        activeJobs[id] = job
    }

    fun startActivityStreaming(session: ActivitySession) {
        session.links.forEach { link ->
            link.featuresToTrack.forEach { feature ->
                startFeatureStream(link.sensorId, feature)
            }
        }
    }

    fun stopActivityStreaming(deviceId: String) {
        activeJobs.keys
            .filter { it.first == deviceId }
            .forEach { id ->
                activeJobs[id]?.cancel()
                activeJobs.remove(id)
            }
    }


}
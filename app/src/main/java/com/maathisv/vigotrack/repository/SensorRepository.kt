package com.maathisv.vigotrack.repository

import android.content.Context
import android.util.Log
import com.maathisv.vigotrack.data.SensorDataSource
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.models.Sensor
import com.polar.androidcommunications.api.ble.model.DisInfo
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHealthThermometerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*


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


    init {
        setupPolarCallbacks()
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

                // CHECK: If Online Streaming is ready, NOW we can start HR
                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING) {
                    _connectionState.value = ConnectionState.FEATURES_READY
                    startHrStreaming(identifier)
                }
            }

            override fun disInformationReceived(identifier: String, disInfo: DisInfo) {
                Log.d("VigoTrack", "DIS info received from $identifier: $disInfo")
            }

            override fun htsNotificationReceived(identifier: String, data: PolarHealthThermometerData) {}


            repositoryScope.lauch()

        })
    }


    private fun startHrStreaming(deviceId: String) {
        api.startHrStreaming(deviceId)
            .onEach { hrData ->
                Log.d(TAG, "HR for $deviceId: ${hrData.samples.first().hr}")
            }
            .catch { e -> Log.e(TAG, "Stream failed for $deviceId", e) }
            .launchIn(repositoryScope)
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
}
package com.maathisv.vigotrack.sensor.polar

import android.content.Context
import android.util.Log
import com.maathisv.vigotrack.sensor.api.ScannedDevice
import com.maathisv.vigotrack.sensor.api.SensorDataType
import com.maathisv.vigotrack.sensor.api.SensorEvent
import com.maathisv.vigotrack.sensor.api.VendorApi
import com.polar.androidcommunications.api.ble.model.DisInfo
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarDeviceNotFound
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHealthThermometerData
import com.polar.sdk.api.model.PolarSensorSetting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class PolarVendorApi(
    private val context: Context
) : VendorApi {

    override val vendorName: String = "polar"

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP
            )
        )
    }

    private val _events = MutableSharedFlow<SensorEvent>(extraBufferCapacity = 64)
    override val events: Flow<SensorEvent> = _events.asSharedFlow()

    private val activeStreamJobs = mutableMapOf<Pair<String, SensorDataType>, Job>()

    init {
        setupCallbacks()
    }

    private fun setupCallbacks() {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d("PolarVendorApi", "BLE: ${if (powered) "ON" else "OFF"}")
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                _events.tryEmit(SensorEvent.DeviceConnecting(polarDeviceInfo.deviceId))
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                _events.tryEmit(
                    SensorEvent.DeviceConnected(
                        deviceId = polarDeviceInfo.deviceId,
                        address = polarDeviceInfo.address,
                        name = polarDeviceInfo.name
                    )
                )
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                _events.tryEmit(SensorEvent.DeviceDisconnected(polarDeviceInfo.deviceId))
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d("PolarVendorApi", "Feature ready: $feature for $identifier")
                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP) {
                    scope.launch {
                        try {
                            api.setLocalTime(identifier, LocalDateTime.now())
                            Log.d("PolarVendorApi", "Time synced for $identifier")
                        } catch (e: Exception) {
                            Log.w("PolarVendorApi", "Time sync failed for $identifier", e)
                        }
                    }
                }
            }

            override fun bleSdkFeaturesReadiness(identifier: String, ready: List<PolarBleApi.PolarBleSdkFeature>, unavailable: List<PolarBleApi.PolarBleSdkFeature>) {
                Log.d("PolarVendorApi", "Features readiness for $identifier. Ready: $ready")
                if (ready.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING)) {
                    scope.launch {
                        try {
                            val types = api.getAvailableOnlineStreamDataTypes(identifier)
                            val mapped = types.mapNotNull { it.toSensorDataType() }.toSet()
                            _events.tryEmit(SensorEvent.FeaturesReady(identifier, mapped))
                        } catch (e: Exception) {
                            Log.e("PolarVendorApi", "Failed to get stream types", e)
                        }
                    }
                }
                if (ready.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)) {
                    scope.launch {
                        try {
                            val types = api.getAvailableHRServiceDataTypes(identifier)
                            val mapped = types.mapNotNull { it.toSensorDataType() }.toSet()
                            _events.tryEmit(SensorEvent.FeaturesReady(identifier, mapped))
                        } catch (e: Exception) {
                            Log.e("PolarVendorApi", "Failed to get HR types", e)
                        }
                    }
                }
            }

            override fun disInformationReceived(identifier: String, disInfo: DisInfo) {
                Log.d("PolarVendorApi", "DIS info: $disInfo")
            }

            override fun htsNotificationReceived(identifier: String, data: PolarHealthThermometerData) {}
        })
    }

    override fun startScanning(): Flow<ScannedDevice> {
        return api.searchForDevice().map { info ->
            ScannedDevice(
                deviceId = info.deviceId.ifEmpty { info.address },
                address = info.address,
                name = info.name,
                vendorName = vendorName
            )
        }
    }

    override suspend fun connectToDevice(deviceId: String) {
        api.connectToDevice(deviceId)
    }

    override fun disconnectFromDevice(deviceId: String) {
        activeStreamJobs.filterKeys { it.first == deviceId }.values.forEach { it.cancel() }
        activeStreamJobs.keys.filter { it.first == deviceId }.forEach { activeStreamJobs.remove(it) }
        api.disconnectFromDevice(deviceId)
    }

    override fun getAvailableDataTypes(deviceId: String): Set<SensorDataType> {
        return SensorDataType.entries.filter { it.toPolarDeviceDataType() != null }.toSet()
    }

    override fun startStreaming(deviceId: String, dataType: SensorDataType, settings: Any?) {
        if (activeStreamJobs.containsKey(deviceId to dataType)) return

        val job = when (dataType) {
            SensorDataType.HR -> startHrStream(deviceId)
            SensorDataType.PPI -> startPpiStream(deviceId)
            SensorDataType.ACC -> startAccStream(deviceId, settings as? PolarSensorSetting)
            SensorDataType.ECG -> startEcgStream(deviceId, settings as? PolarSensorSetting)
            else -> {
                Log.w("PolarVendorApi", "Unsupported data type: $dataType")
                return
            }
        }
        if (job != null) {
            activeStreamJobs[deviceId to dataType] = job
            _events.tryEmit(SensorEvent.StreamStarted(deviceId, dataType))
        }
    }

    override fun stopStreaming(deviceId: String, dataType: SensorDataType) {
        activeStreamJobs[deviceId to dataType]?.cancel()
        activeStreamJobs.remove(deviceId to dataType)
    }

    private fun startHrStream(deviceId: String): Job =
        api.startHrStreaming(deviceId)
            .retry(3) { cause ->
                if (cause is PolarDeviceNotFound) { delay(2000); true } else false
            }
            .onEach { data ->
                data.samples.forEach { sample ->
                    _events.tryEmit(SensorEvent.DataReceived(deviceId, sample.toHeartRateDataPoint()))
                }
            }
            .catch { e -> Log.e("PolarVendorApi", "HR stream failed", e) }
            .launchIn(scope)

    private fun startPpiStream(deviceId: String): Job =
        api.startPpiStreaming(deviceId)
            .retry(3) { cause ->
                if (cause is PolarDeviceNotFound) { delay(2000); true } else false
            }
            .onEach { data ->
                data.samples.forEach { sample ->
                    _events.tryEmit(SensorEvent.DataReceived(deviceId, sample.toPpiDataPoint()))
                }
            }
            .catch { e -> Log.e("PolarVendorApi", "PPI stream failed", e) }
            .launchIn(scope)

    private fun startAccStream(deviceId: String, settings: PolarSensorSetting?): Job =
        scope.launch {
            var retries = 0
            while (retries < 3) {
                try {
                    val accSettings = settings ?: api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC).maxSettings()
                    api.startAccStreaming(deviceId, accSettings)
                        .onEach { data ->
                            data.toAccelerometerDataPoints().forEach { dp ->
                                _events.tryEmit(SensorEvent.DataReceived(deviceId, dp))
                            }
                        }
                        .catch { e -> Log.e("PolarVendorApi", "ACC stream fail", e) }
                        .collect()
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: PolarDeviceNotFound) {
                    retries++; if (retries < 3) delay(2000) else Log.e("PolarVendorApi", "ACC failed", e)
                } catch (e: Exception) {
                    Log.e("PolarVendorApi", "ACC failed", e); break
                }
            }
        }

    private fun startEcgStream(deviceId: String, settings: PolarSensorSetting?): Job =
        scope.launch {
            var retries = 0
            while (retries < 3) {
                try {
                    val ecgSettings = settings ?: api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ECG).maxSettings()
                    api.startEcgStreaming(deviceId, ecgSettings)
                        .onEach { data ->
                            data.toEcgDataPoints().forEach { dp ->
                                _events.tryEmit(SensorEvent.DataReceived(deviceId, dp))
                            }
                        }
                        .catch { e -> Log.e("PolarVendorApi", "ECG stream fail", e) }
                        .collect()
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: PolarDeviceNotFound) {
                    retries++; if (retries < 3) delay(2000) else Log.e("PolarVendorApi", "ECG failed", e)
                } catch (e: Exception) {
                    Log.e("PolarVendorApi", "ECG failed", e); break
                }
            }
        }

    override fun onForegroundEntered() {
        api.foregroundEntered()
    }

    fun destroy() {
        scope.cancel()
    }
}

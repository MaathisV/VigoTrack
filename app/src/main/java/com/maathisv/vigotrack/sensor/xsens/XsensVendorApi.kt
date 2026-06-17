package com.maathisv.vigotrack.sensor.xsens

import android.util.Log
import com.maathisv.vigotrack.sensor.api.ScannedDevice
import com.maathisv.vigotrack.sensor.api.SensorDataType
import com.maathisv.vigotrack.sensor.api.SensorEvent
import com.maathisv.vigotrack.sensor.api.VendorApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Placeholder XsensVendorApi.
 *
 * To activate:
 * 1. Add XsensDotSdkCore2.aar + XsensDotCore.aar to app/libs/
 * 2. Add compile files to build.gradle
 * 3. Implement startScanning(), connectToDevice(), etc. using XsensDotManager
 * 4. Replace emptyFlow() with real SDK flows
 * 5. Emit SensorEvent.DataReceived(deviceId, eulerDataPoint) from callbacks
 */
class XsensVendorApi : VendorApi {

    override val vendorName: String = "xsens"

    /**
     * Internal event channel used as a bridge from SDK callbacks to the Flow.
     * Once the AAR is linked, SDK callbacks write to this channel:
     *
     *   sensorManager.setDeviceStateCallback(object : XsensDotDeviceStateCallback {
     *       override fun onDeviceConnected(address: String) {
     *           commandChannel.trySend(SensorEvent.DeviceConnected(address, address, "Xsens Dot"))
     *       }
     *       ...
     *   })
     */
    private val commandChannel = Channel<SensorEvent>(Channel.BUFFERED)

    override val events: Flow<SensorEvent> = commandChannel.receiveAsFlow()

    override fun startScanning(): Flow<ScannedDevice> {
        // TODO: use XsensDotManager.getInstance().startScan(context, callback)
        //       or XsensDotConnectionManager.
        Log.d("XsensVendorApi", "Scanning not implemented — requires Xsens AAR")
        return emptyFlow()
    }

    override suspend fun connectToDevice(deviceId: String) {
        // TODO: XsensDotConnectionManager.connect(deviceId)
        Log.d("XsensVendorApi", "connectToDevice not implemented — requires Xsens AAR")
    }

    override fun disconnectFromDevice(deviceId: String) {
        // TODO: XsensDotConnectionManager.disconnect(deviceId)
    }

    override suspend fun getAvailableSettings(deviceId: String, dataType: SensorDataType): Map<String, Set<Int>>? = null

    override fun getAvailableDataTypes(deviceId: String): Set<SensorDataType> {
        // All Xsens Dot devices support Euler, quaternion, and free acceleration
        return setOf(SensorDataType.EULER, SensorDataType.QUATERNION, SensorDataType.FREE_ACCELERATION)
    }

    override fun startStreaming(deviceId: String, dataType: SensorDataType, settings: Any?) {
        // TODO:
        //   val manager = XsensDotConnectionManager(deviceId)
        //   when (dataType) {
        //       SensorDataType.EULER -> manager.enableEuler(frameCallback { euler ->
        //           commandChannel.trySend(SensorEvent.DataReceived(deviceId,
        //               SensorDataPoint.EulerAngles(euler.roll, euler.pitch, euler.yaw)))
        //       })
        //       SensorDataType.QUATERNION -> ...
        //       SensorDataType.FREE_ACCELERATION -> ...
        //   }
        Log.d("XsensVendorApi", "startStreaming not implemented — requires Xsens AAR")
    }

    override fun stopStreaming(deviceId: String, dataType: SensorDataType) {
        // TODO
    }
}

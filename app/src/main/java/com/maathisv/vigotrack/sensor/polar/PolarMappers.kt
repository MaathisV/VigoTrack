package com.maathisv.vigotrack.sensor.polar

import com.maathisv.vigotrack.sensor.api.SensorDataPoint
import com.maathisv.vigotrack.sensor.api.SensorDataType
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarPpiData

// Polar SDK timestamps for PPI use Polar epoch (2000-01-01).
// Convert to Unix epoch nanoseconds for consistency.
private const val POLAR_TO_UNIX_EPOCH_NS = 946684800_000_000_000L

internal fun PolarHrData.PolarHrSample.toHeartRateDataPoint(): SensorDataPoint.HeartRate =
    SensorDataPoint.HeartRate(
        hr = hr,
        rrAvailable = rrAvailable,
        contactStatus = contactStatus,
        contactStatusSupported = contactStatusSupported,
        ppgQuality = ppgQuality,
        correctedHr = correctedHr,
        rrsMs = rrsMs,
        timestamp = System.currentTimeMillis() * 1_000_000L
    )

internal fun PolarPpiData.PolarPpiSample.toPpiDataPoint(): SensorDataPoint.Ppi =
    SensorDataPoint.Ppi(
        ppiMs = ppi,
        hr = hr,
        errorEstimate = errorEstimate,
        blockerBit = blockerBit,
        skinContactStatus = skinContactStatus,
        skinContactSupported = skinContactSupported,
        timestamp = timeStamp.toLong() + POLAR_TO_UNIX_EPOCH_NS
    )

internal fun PolarAccelerometerData.toAccelerometerDataPoints(): List<SensorDataPoint.Accelerometer> =
    samples.map { sample ->
        SensorDataPoint.Accelerometer(
            x = sample.x.toFloat(),
            y = sample.y.toFloat(),
            z = sample.z.toFloat(),
            timestamp = sample.timeStamp + POLAR_TO_UNIX_EPOCH_NS
        )
    }

internal fun PolarEcgData.toEcgDataPoints(): List<SensorDataPoint.Electrocardiogram> =
    samples.map { sample ->
        when (sample) {
            is com.polar.sdk.api.model.EcgSample -> SensorDataPoint.Electrocardiogram(
                voltage = sample.voltage.toFloat(),
                timestamp = sample.timeStamp + POLAR_TO_UNIX_EPOCH_NS
            )
            is com.polar.sdk.api.model.FecgSample -> SensorDataPoint.Electrocardiogram(
                voltage = sample.ecg.toFloat(),
                bioz = sample.bioz,
                status = sample.status,
                timestamp = sample.timeStamp + POLAR_TO_UNIX_EPOCH_NS
            )
        }
    }

internal fun PolarBleApi.PolarDeviceDataType.toSensorDataType(): SensorDataType? = when (this) {
    PolarBleApi.PolarDeviceDataType.HR -> SensorDataType.HR
    PolarBleApi.PolarDeviceDataType.PPI -> SensorDataType.PPI
    PolarBleApi.PolarDeviceDataType.ACC -> SensorDataType.ACC
    PolarBleApi.PolarDeviceDataType.ECG -> SensorDataType.ECG
    else -> null
}

internal fun SensorDataType.toPolarDeviceDataType(): PolarBleApi.PolarDeviceDataType? = when (this) {
    SensorDataType.HR -> PolarBleApi.PolarDeviceDataType.HR
    SensorDataType.PPI -> PolarBleApi.PolarDeviceDataType.PPI
    SensorDataType.ACC -> PolarBleApi.PolarDeviceDataType.ACC
    SensorDataType.ECG -> PolarBleApi.PolarDeviceDataType.ECG
    else -> null
}

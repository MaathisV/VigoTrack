package com.maathisv.vigotrack.sensor.xsens

import com.maathisv.vigotrack.sensor.api.SensorDataType

// Placeholder: will convert XsensDot SDK types to SensorDataPoint when AAR is linked
// Example structure for when SDK is available:
//
// internal fun XsensEuler.toDataPoint(): SensorDataPoint.EulerAngles =
//     SensorDataPoint.EulerAngles(roll = roll, pitch = pitch, yaw = yaw)

internal fun SensorDataType.isXsensType(): Boolean = this in setOf(
    SensorDataType.EULER,
    SensorDataType.QUATERNION,
    SensorDataType.FREE_ACCELERATION
)

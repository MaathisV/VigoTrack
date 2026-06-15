package com.maathisv.vigotrack.sensor.api

sealed interface SensorDataPoint {
    val timestamp: Long
    val dataType: SensorDataType

    data class HeartRate(
        val hr: Int,
        val rrAvailable: Boolean = false,
        val contactStatus: Boolean = false,
        val contactStatusSupported: Boolean = false,
        val ppgQuality: Int = 0,
        val correctedHr: Int = 0,
        val rrsMs: List<Int> = emptyList(),
        override val timestamp: Long = System.currentTimeMillis() * 1_000_000L
    ) : SensorDataPoint {
        override val dataType = SensorDataType.HR
    }

    data class Ppi(
        val ppiMs: Int,
        val hr: Int,
        val errorEstimate: Int = 0,
        val blockerBit: Boolean = false,
        val skinContactStatus: Boolean = false,
        val skinContactSupported: Boolean = false,
        override val timestamp: Long = System.nanoTime()
    ) : SensorDataPoint {
        override val dataType = SensorDataType.PPI
    }

    data class Accelerometer(
        val x: Float,
        val y: Float,
        val z: Float,
        override val timestamp: Long = System.nanoTime()
    ) : SensorDataPoint {
        override val dataType = SensorDataType.ACC
    }

    data class Electrocardiogram(
        val voltage: Float,
        val bioz: Int = 0,
        val status: UByte = 0.toUByte(),
        override val timestamp: Long = System.nanoTime()
    ) : SensorDataPoint {
        override val dataType = SensorDataType.ECG
    }

    data class Gyroscope(
        val x: Float, val y: Float, val z: Float,
        override val timestamp: Long = System.nanoTime()
    ) : SensorDataPoint {
        override val dataType = SensorDataType.GYRO
    }

    data class EulerAngles(
        val roll: Float, val pitch: Float, val yaw: Float,
        override val timestamp: Long = System.nanoTime()
    ) : SensorDataPoint {
        override val dataType = SensorDataType.EULER
    }

    data class Quaternion(
        val w: Float, val x: Float, val y: Float, val z: Float,
        override val timestamp: Long = System.nanoTime()
    ) : SensorDataPoint {
        override val dataType = SensorDataType.QUATERNION
    }

    data class FreeAcceleration(
        val x: Float, val y: Float, val z: Float,
        override val timestamp: Long = System.nanoTime()
    ) : SensorDataPoint {
        override val dataType = SensorDataType.FREE_ACCELERATION
    }
}

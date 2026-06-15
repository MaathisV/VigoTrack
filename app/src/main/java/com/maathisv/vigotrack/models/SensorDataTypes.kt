
package com.maathisv.vigotrack.models

typealias StreamIdentifier = Pair<String, String>

data class StreamConfig(
    val type: Feature,
    val rawSettings: Any? = null
)

enum class Feature {
    HR, ECG, ACC, GYRO, PPG, EULER, QUATERNION, FREE_ACCELERATION
}


package com.maathisv.vigotrack.models

typealias StreamIdentifier = Pair<String, String>

// Brand Agnostic : Repository Cast type correctly
data class StreamConfig(
    val type: Feature,
    val rawSettings: Any? = null
)

enum class Feature {
    HR, ECG, ACC, GYRO, PPG
}
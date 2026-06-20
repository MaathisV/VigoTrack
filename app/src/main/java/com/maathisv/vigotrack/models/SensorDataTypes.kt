
package com.maathisv.vigotrack.models

typealias StreamIdentifier = Pair<String, String>

enum class Feature {
    HR, ECG, ACC, GYRO, PPG, EULER, QUATERNION, FREE_ACCELERATION
}

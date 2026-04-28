package com.maathisv.vigotrack

import android.app.Application
import com.maathisv.vigotrack.repository.ActivityRepository
import com.maathisv.vigotrack.repository.DeviceRepository

class VigoTrackApplication : Application() {
    val activityRepository by lazy { ActivityRepository() }
    val deviceRepository by lazy { DeviceRepository(this) }
}
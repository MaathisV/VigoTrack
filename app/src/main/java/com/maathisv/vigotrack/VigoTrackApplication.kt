package com.maathisv.vigotrack

import android.app.Application
import com.maathisv.vigotrack.data.RoomSensorDataSource
import com.maathisv.vigotrack.data.SensorDataSource
import com.maathisv.vigotrack.repository.ActivityRepository
import com.maathisv.vigotrack.repository.SensorRepository
import com.maathisv.vigotrack.data.VigoTrackDatabase

class VigoTrackApplication : Application() {
    private val database by lazy {
        VigoTrackDatabase.getDatabase(this)
    }
    // Room Datasource implementation
    private val sensorDataSource: SensorDataSource by lazy {
        RoomSensorDataSource(database.sensorDao())
    }

    val activityRepository by lazy { ActivityRepository() }
    val sensorRepository by lazy { SensorRepository(this, sensorDataSource) }
}
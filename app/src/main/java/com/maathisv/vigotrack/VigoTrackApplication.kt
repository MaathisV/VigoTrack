package com.maathisv.vigotrack

import android.app.Application
import com.maathisv.vigotrack.data.ActivityDataSource
import com.maathisv.vigotrack.data.PatientDataSource
import com.maathisv.vigotrack.data.RoomActivityDataSource
import com.maathisv.vigotrack.data.RoomPatientDataSource
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
    val sensorRepository by lazy { SensorRepository(this, sensorDataSource) }

    private val activityDataSource: ActivityDataSource by lazy {
        RoomActivityDataSource(database.activityDao())
    }
    val activityRepository by lazy { ActivityRepository(activityDataSource) }

    val patientDataSource: PatientDataSource by lazy {
        RoomPatientDataSource(database.patientDao())
    }
}
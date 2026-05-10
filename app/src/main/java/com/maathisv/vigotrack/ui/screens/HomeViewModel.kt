package com.maathisv.vigotrack.ui.screens

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.repository.ActivityRepository
import com.maathisv.vigotrack.repository.SensorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.services.*
import com.maathisv.vigotrack.services.PolarService

class HomeViewModel(
    private val application: Application,
    private val activityRepo: ActivityRepository,
    private val sensorRepo: SensorRepository
) : AndroidViewModel(application) {

    val connectionState = sensorRepo.connectionState
    val activities = activityRepo.allActivities
    val sensorLiveData = sensorRepo.liveData

    val connectedDevicesList = sensorRepo.connectedDeviceIds
        .map { it.toList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scannedDevices: StateFlow<List<Sensor>> = sensorRepo.discoveredDevices
        .map { it.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isConnectingToId = MutableStateFlow<String?>(null)
    val isConnectingToId: StateFlow<String?> = _isConnectingToId.asStateFlow()

    //val sensorLiveData: StateFlow<Map<String, StreamingData>> = sensorRepo.liveData
    //    .map { it }
    //    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun startScanning() {
        sensorRepo.startScanning()
    }

    fun connectToDevice(sensor: Sensor) {
        _isConnectingToId.value = sensor.deviceId
        viewModelScope.launch(Dispatchers.IO) {
            sensorRepo.requestConnect(sensor)
        }
    }

    fun disconnectFromDevice(id: String) {
        _isConnectingToId.value = null
        viewModelScope.launch(Dispatchers.IO) {
            sensorRepo.requestDisconnect(id)
        }
    }

    fun createActivity(name: String, date: Long) {
        viewModelScope.launch {
            activityRepo.createActivity(name, date)
        }
    }

    fun addLink(activityId: String, patientId: String, sensorId: String, features: List<String>) {
        viewModelScope.launch {
            val link = ActivitySession.ActivityLink(
                patientId = patientId,
                sensorId = sensorId,
                patientName = patientId,
                featuresToTrack = features
            )
            activityRepo.addLinkToActivity(activityId, link)
        }
    }

    fun toggleSession(activity: ActivitySession) {
        viewModelScope.launch {
            // Use getApplication() here!
            val intent = Intent(getApplication(), PolarService::class.java)

            if (activity.isRunning) {
                activityRepo.stopActivity(activity)
                activity.links.forEach { sensorRepo.stopActivityStreaming(it.sensorId) }

                intent.action = ACTION_STOP_STREAMS
                getApplication<Application>().startService(intent)
            } else {
                activityRepo.startActivity(activity)
                activity.links.forEach { link ->
                    sensorRepo.startActivityStreaming(activity)

                    val serviceIntent = Intent(getApplication(), PolarService::class.java).apply {
                        action = ACTION_START_STREAMS
                        putExtra(EXTRA_SENSOR_ID, link.sensorId)
                    }
                    getApplication<Application>().startService(serviceIntent)
                }
            }
        }
    }
}
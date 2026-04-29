package com.maathisv.vigotrack.ui.screens

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

class HomeViewModel(
    private val activityRepo: ActivityRepository,
    private val deviceRepo: SensorRepository
) : ViewModel() {

    val connectionState = deviceRepo.connectionState
    val activities = activityRepo.activities

    val connectedDevicesList = deviceRepo.connectedDeviceIds
        .map { it.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scannedDevices: StateFlow<List<Sensor>> = deviceRepo.discoveredDevices
        .map { it.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isConnectingToId = MutableStateFlow<String?>(null)
    val isConnectingToId: StateFlow<String?> = _isConnectingToId.asStateFlow()

    fun startScanning() {
        deviceRepo.startScanning()
    }

    fun connectToDevice(sensor: Sensor) {
        _isConnectingToId.value = sensor.deviceId
        viewModelScope.launch(Dispatchers.IO) {
            deviceRepo.requestConnect(sensor)
        }
    }

    fun disconnectFromDevice(id: String) {
        _isConnectingToId.value = null

        viewModelScope.launch(Dispatchers.IO) {
            deviceRepo.requestDisconnect(id)
        }
    }

    fun onCreateActivityClicked(name: String) {
        activityRepo.createActivity(name)
    }
}
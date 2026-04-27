package com.maathisv.vigotrack.ui.screens

import androidx.lifecycle.ViewModel
import com.maathisv.vigotrack.repository.ActivityRepository
import com.maathisv.vigotrack.models.ConnectionState
import com.maathisv.vigotrack.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(private val repository: ActivityRepository) : ViewModel() {
    // The UI observes this. Whenever the repo updates, the UI updates automatically.
    val activities = repository.activities
    private val deviceRepository = DeviceRepository()

    val connectionState: StateFlow<ConnectionState> = deviceRepository.connectionState

    // Mocking scanned devices for now
    private val _scannedDevices = MutableStateFlow<List<String>>(emptyList())
    val scannedDevices: StateFlow<List<String>> = _scannedDevices

    // Map to your reference: connectToDevice(device)
    fun connectToDevice(deviceId: String) {
        deviceRepository.requestConnect(deviceId)
    }

    // Map to your reference: disconnectFromDevice()
    fun disconnectFromDevice(deviceId: String) {
        deviceRepository.requestDisconnect(deviceId)
    }

    fun onCreateActivityClicked(name: String) {
        repository.createActivity(name)
    }
}
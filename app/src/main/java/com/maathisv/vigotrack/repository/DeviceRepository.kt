package com.maathisv.vigotrack.repository

import com.maathisv.vigotrack.models.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.polar.sdk.api.PolarBleApi


class DeviceRepository(/* Add PolarBleApi instance here later */) {

    private val _connectionState = MutableStateFlow(ConnectionState.NOT_CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Map to your reference: requestConnect(deviceId)
    fun requestConnect(deviceId: String) {
        _connectionState.value = ConnectionState.CONNECTING
        // TODO: Call PolarBleApi connectToDevice(deviceId)
        // Listeners in the SDK will eventually update state to CONNECTED or NOT_CONNECTED on failure
    }

    // Map to your reference: requestDisconnect()
    fun requestDisconnect(deviceId: String) {
        // TODO: Call PolarBleApi disconnectFromDevice(deviceId)
        _connectionState.value = ConnectionState.NOT_CONNECTED
    }

    // You will also add scanning logic here to feed the dialog
}
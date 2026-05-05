package com.maathisv.vigotrack

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maathisv.vigotrack.services.PolarService
import com.maathisv.vigotrack.ui.navigation.VigoTrackNavGraph
import com.maathisv.vigotrack.ui.screens.HomeViewModel

class MainActivity : ComponentActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        add(Manifest.permission.POST_NOTIFICATIONS)
        add(Manifest.permission.BODY_SENSORS)
    }.toTypedArray()

    private lateinit var viewModel: HomeViewModel

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasRequiredPermissions()) {
            startForegroundShield()
        } else {
            Log.e("VigoTrack", "Required permissions denied. Service cannot start.")
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions()) {
            startForegroundShield()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as VigoTrackApplication

        val factory = viewModelFactory {
            initializer {
                HomeViewModel(app.activityRepository, app.sensorRepository)
            }
        }

        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        // Launch permissions
        permissionLauncher.launch(requiredPermissions)

        setContent {
            VigoTrackNavGraph(homeViewModel = viewModel)
        }
    }

    // Using the Service as a 'Foreground Shield' keeps the app process high-priority,
    // allowing the singleton SensorRepository to maintain active BLE streams without the
    // complexity of Service binding or moving the PolarBleApi instance.
    // Service binding can be added later if needed
    private fun startForegroundShield() {
        val serviceIntent = Intent(this, PolarService::class.java)
        startForegroundService(serviceIntent)
    }
}
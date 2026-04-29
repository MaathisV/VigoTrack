package com.maathisv.vigotrack

import android.Manifest
import android.content.Intent
import android.os.Build
import com.maathisv.vigotrack.ui.screens.HomeViewModel
import com.maathisv.vigotrack.ui.screens.HomeScreen
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maathisv.vigotrack.services.PolarService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as VigoTrackApplication

        val factory = viewModelFactory {
            initializer {
                HomeViewModel(app.activityRepository, app.sensorRepository)
            }
        }

        val viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        val serviceIntent = Intent(this, PolarService::class.java)

        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
            val notificationGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false

            if (activityGranted) {
                startForegroundService(serviceIntent)
            } else {
                Log.e("VigoTrack", "Activity Recognition denied. Service cannot start.")
            }
        }

        launcher.launch(
            arrayOf(
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )


        setContent { HomeScreen(viewModel) }
    }
}
package com.maathisv.vigotrack

import com.maathisv.vigotrack.ui.screens.HomeViewModel
import com.maathisv.vigotrack.ui.screens.HomeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as VigoTrackApplication

        val factory = viewModelFactory {
            initializer {
                HomeViewModel(app.activityRepository, app.deviceRepository)
            }
        }

        val viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        setContent { HomeScreen(viewModel) }
    }
}
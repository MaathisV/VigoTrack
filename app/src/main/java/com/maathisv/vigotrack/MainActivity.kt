package com.maathisv.vigotrack

import com.maathisv.vigotrack.ui.screens.HomeViewModel
import com.maathisv.vigotrack.ui.screens.HomeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.maathisv.vigotrack.repository.ActivityRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val repository = ActivityRepository()
    
    val viewModel = HomeViewModel(repository)
    
    setContent {
        HomeScreen(viewModel = viewModel)
    }
    }
}
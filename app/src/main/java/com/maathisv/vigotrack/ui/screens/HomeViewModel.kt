package com.maathisv.vigotrack.ui.screens

import androidx.lifecycle.ViewModel
import com.maathisv.vigotrack.repository.ActivityRepository

class HomeViewModel(private val repository: ActivityRepository) : ViewModel() {
    // The UI observes this. Whenever the repo updates, the UI updates automatically.
    val activities = repository.activities

    fun onCreateActivityClicked(name: String) {
        repository.createActivity(name)
    }
}
package com.maathisv.vigotrack.repository

import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ActivityRepository {
    // Keep your data in a simple memory list for now
    private val _activities = MutableStateFlow<List<ActivitySession>>(emptyList())
    val activities = _activities.asStateFlow()

    fun createActivity(name: String) {
        val newActivity = ActivitySession(id = UUID.randomUUID().toString(), name = name)
        _activities.value += newActivity
    }

    fun addLinkToActivity(activityId: String, link: ActivityLink) {
        // TODO
    }
}
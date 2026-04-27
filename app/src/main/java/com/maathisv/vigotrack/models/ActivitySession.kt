package com.maathisv.vigotrack.models

data class ActivitySession(
    val id: String,
    val name: String,
    val links: List<ActivityLink> = emptyList(),
    val startTime: Long? = null
)
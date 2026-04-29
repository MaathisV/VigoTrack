package com.maathisv.vigotrack.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.maathisv.vigotrack.R
import com.maathisv.vigotrack.VigoTrackApplication
import com.maathisv.vigotrack.repository.SensorRepository

class PolarService : LifecycleService() {

    private val repository: SensorRepository by lazy {
        (application as VigoTrackApplication).sensorRepository
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "polar_service_channel"

        val channel = NotificationChannel(
            channelId,
            "Heart Rate Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("VigoTrack Active")
            .setContentText("Monitoring Polar sensors in background...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a valid icon from your res/drawable
            .setOngoing(true) // Keeps the notification from being swiped away
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 101
    }
}
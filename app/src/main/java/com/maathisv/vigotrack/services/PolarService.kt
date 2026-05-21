package com.maathisv.vigotrack.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.maathisv.vigotrack.R
import com.maathisv.vigotrack.VigoTrackApplication
import com.maathisv.vigotrack.repository.SensorRepository
import com.maathisv.vigotrack.util.DataLogger
import com.polar.sdk.api.model.PolarHrData
import kotlinx.coroutines.launch
import androidx.core.net.toUri

const val ACTION_START_STREAMS = "com.maathisv.vigotrack.START_STREAMS"
const val ACTION_STOP_STREAMS = "com.maathisv.vigotrack.STOP_STREAMS"
const val EXTRA_SENSOR_IDS = "extra_sensor_ids"

class PolarService : LifecycleService() {
    private val loggers = mutableMapOf<String, DataLogger>()

    private val repository: SensorRepository by lazy {
        (application as VigoTrackApplication).sensorRepository
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, createNotification())

        if (intent == null) return START_STICKY

        when (intent.action) {
            ACTION_START_STREAMS -> {
                val sensorIds = intent.getStringArrayExtra(EXTRA_SENSOR_IDS) ?: return START_STICKY
                val prefs = getSharedPreferences("vigo_prefs", MODE_PRIVATE)
                val uriString = prefs.getString("log_uri", null)

                if (uriString != null) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    sensorIds.forEach { sensorId ->
                        loggers[sensorId] = DataLogger(applicationContext, uriString.toUri(), sensorId)
                    }
                    observeSensorData()
                }
            }
            ACTION_STOP_STREAMS -> {
                stopLogging()
            }
        }
        return START_STICKY
    }

    private fun observeSensorData() {
        lifecycleScope.launch {
            repository.hrLogFlow.collect { (deviceId, data) ->
                val logger = loggers[deviceId] ?: return@collect
                data.samples.forEach { s -> onHrReceived(logger, s) }
            }
        }
        lifecycleScope.launch {
            repository.ppiLogFlow.collect { (deviceId, data) ->
                val logger = loggers[deviceId] ?: return@collect
                data.samples.forEach { s ->
                    onPpiReceived(logger, s.timeStamp.toLong(), s.ppi, s.errorEstimate, s.hr,
                        s.blockerBit, s.skinContactStatus, s.skinContactSupported)
                }
            }
        }
        lifecycleScope.launch {
            repository.accLogFlow.collect { (deviceId, data) ->
                val logger = loggers[deviceId] ?: return@collect
                data.samples.forEach { s ->
                    onAccReceived(logger, s.timeStamp, s.x, s.y, s.z)
                }
            }
        }
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

    fun stopLogging() {
        loggers.values.forEach { it.closeAll() }
        loggers.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onHrReceived(logger: DataLogger, sample: PolarHrData.PolarHrSample) {
        val timestamp = System.nanoTime()
        logger.logData(
            tag = "HR",
            header = "TIMESTAMP HR PPQ_QUALITY CORRECTED_HR RR_AVAILABLE CONTACT_SUPPORTED CONTACT_STATUS RR(ms)",
            dataLine = "$timestamp ${sample.hr} ${sample.ppgQuality} ${sample.correctedHr} ${sample.rrAvailable} ${sample.contactStatusSupported} ${sample.contactStatus} ${if (sample.rrsMs.isEmpty()) "NA" else sample.rrsMs.joinToString(" ")}"
        )
    }

    private fun onAccReceived(logger: DataLogger, timestamp: Long, x: Int, y: Int, z: Int) {
        logger.logData(
            tag = "ACC",
            header = "TIMESTAMP X(mg) Y(mg) Z(mg)",
            dataLine = "$timestamp $x $y $z"
        )
    }

    private fun onPpiReceived(logger: DataLogger, timestamp: Long, ppi: Int, errorEstimate: Int, hr: Int,
                              blockerBit: Boolean, skinContactStatus: Boolean, skinContactSupported: Boolean) {
        logger.logData(
            tag = "PPI",
            header = "TIMESTAMP PPI(ms) ERROR_ESTIMATE BLOCKER_BIT SKIN_CONTACT_STATUS SKIN_CONTACT_SUPPORT HR",
            dataLine = "$timestamp $ppi $errorEstimate $blockerBit $skinContactStatus $skinContactSupported $hr"
        )
    }


}
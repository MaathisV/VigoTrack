package com.maathisv.vigotrack.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.maathisv.vigotrack.R
import com.maathisv.vigotrack.VigoTrackApplication
import com.maathisv.vigotrack.repository.SensorRepository
import com.maathisv.vigotrack.sensor.api.SensorDataPoint
import com.maathisv.vigotrack.util.DataLogger
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val ACTION_START_STREAMS = "com.maathisv.vigotrack.START_STREAMS"
const val ACTION_STOP_STREAMS = "com.maathisv.vigotrack.STOP_STREAMS"
const val EXTRA_SENSOR_IDS = "extra_sensor_ids"
const val EXTRA_ACTIVITY_NAME = "extra_activity_name"
const val EXTRA_ACTIVITY_CATEGORY = "extra_activity_category"
const val EXTRA_PATIENT_NAME = "extra_patient_name"
const val EXTRA_PATIENT_NAMES = "extra_patient_names"
const val EXTRA_STAGE_NAME = "extra_stage_name"
const val EXTRA_SESSION_DATE = "extra_session_date"

const val DEFAULT_TEMPLATE = "{stage}/{patient}/{category}/{activity}_{datetime}/{sensor}_{tag}"

class SensorService : LifecycleService() {
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
                val patientNames = intent.getStringArrayExtra(EXTRA_PATIENT_NAMES)
                val activityName = intent.getStringExtra(EXTRA_ACTIVITY_NAME) ?: ""
                val activityCategory = intent.getStringExtra(EXTRA_ACTIVITY_CATEGORY) ?: ""
                val stageName = intent.getStringExtra(EXTRA_STAGE_NAME) ?: "NoStage"
                val sessionDate = intent.getLongExtra(EXTRA_SESSION_DATE, System.currentTimeMillis())

                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfTime = SimpleDateFormat("HH-mm-ss", Locale.US)
                val dateStr = sdfDate.format(Date(sessionDate))
                val timeStr = sdfTime.format(Date(sessionDate))

                val prefs = getSharedPreferences("vigo_prefs", MODE_PRIVATE)
                val uriString = prefs.getString("log_uri", null)
                val template = prefs.getString("file_naming_template", null) ?: DEFAULT_TEMPLATE

                val logFeatures = setOf("HR", "PPI", "ACC", "ECG").filter {
                    prefs.getBoolean("log_$it", true)
                }.toSet()

                if (uriString != null) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    sensorIds.forEachIndexed { i, sensorId ->
                        val patientName = patientNames?.getOrElse(i) { "" } ?: ""
                        val staticValues = mapOf(
                            "stage" to stageName,
                            "patient" to patientName,
                            "category" to activityCategory,
                            "activity" to activityName,
                            "device" to sensorId,
                            "sensor" to sensorId,
                            "date" to dateStr,
                            "time" to timeStr,
                            "datetime" to "${dateStr}_${timeStr}",
                            "timestamp" to sessionDate.toString()
                        )
                        loggers[sensorId] = DataLogger(
                            applicationContext, uriString.toUri(), template, staticValues
                        )
                    }
                    observeSensorData(logFeatures)
                }
            }
            ACTION_STOP_STREAMS -> {
                stopLogging()
            }
        }
        return START_STICKY
    }

    private fun observeSensorData(logFeatures: Set<String> = setOf("HR", "PPI", "ACC", "ECG")) {
        lifecycleScope.launch {
            repository.sensorDataFlow.collect { (deviceId, dataPoint) ->
                val logger = loggers[deviceId] ?: return@collect
                if (dataPoint.dataType.name in logFeatures) {
                    when (dataPoint) {
                        is SensorDataPoint.HeartRate -> logHr(logger, dataPoint)
                        is SensorDataPoint.Ppi -> logPpi(logger, dataPoint)
                        is SensorDataPoint.Accelerometer -> logAcc(logger, dataPoint)
                        is SensorDataPoint.Electrocardiogram -> logEcg(logger, dataPoint)
                        else -> {} // Xsens types not yet logged
                    }
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "sensor_service_channel"

        val channel = NotificationChannel(
            channelId,
            "Suivi des capteurs",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("VigoTrack Actif")
            .setContentText("Surveillance des capteurs en arrière-plan…")
            .setSmallIcon(R.drawable.vigotrack_icon_foreground)
            .setOngoing(true)
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

    private fun logHr(logger: DataLogger, sample: SensorDataPoint.HeartRate) {
        logger.logData(
            tag = "HR",
            header = "TIMESTAMP HR PPQ_QUALITY CORRECTED_HR RR_AVAILABLE CONTACT_SUPPORTED CONTACT_STATUS RR(ms)",
            dataLine = "${sample.timestamp} ${sample.hr} ${sample.ppgQuality} ${sample.correctedHr} ${sample.rrAvailable} ${sample.contactStatusSupported} ${sample.contactStatus} ${if (sample.rrsMs.isEmpty()) "NA" else sample.rrsMs.joinToString(" ")}"
        )
    }

    private fun logAcc(logger: DataLogger, sample: SensorDataPoint.Accelerometer) {
        logger.logData(
            tag = "ACC",
            header = "TIMESTAMP X(mg) Y(mg) Z(mg)",
            dataLine = "${sample.timestamp} ${sample.x.toInt()} ${sample.y.toInt()} ${sample.z.toInt()}"
        )
    }

    private fun logPpi(logger: DataLogger, sample: SensorDataPoint.Ppi) {
        logger.logData(
            tag = "PPI",
            header = "TIMESTAMP PPI(ms) ERROR_ESTIMATE BLOCKER_BIT SKIN_CONTACT_STATUS SKIN_CONTACT_SUPPORT HR",
            dataLine = "${sample.timestamp} ${sample.ppiMs} ${sample.errorEstimate} ${sample.blockerBit} ${sample.skinContactStatus} ${sample.skinContactSupported} ${sample.hr}"
        )
    }

    private fun logEcg(logger: DataLogger, sample: SensorDataPoint.Electrocardiogram) {
        logger.logData(
            tag = "ECG",
            header = "TIMESTAMP VOLTAGE(uV)",
            dataLine = "${sample.timestamp} ${sample.voltage}"
        )
    }
}

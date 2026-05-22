package com.maathisv.vigotrack.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataLogger(
    private val context: Context,
    private val rootUri: Uri,
    sensorId: String,
    namingTemplate: String = "{date}_{activity}_{patient}_{device}_{tag}",
    activityName: String = "",
    patientName: String = ""
) {
    private val deviceId = sensorId
    private val streams = mutableMapOf<String, OutputStream>()
    private val headersWritten = mutableSetOf<String>()
    private val resolvedPrefix: String

    init {
        val now = System.currentTimeMillis()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfTime = SimpleDateFormat("HH-mm-ss", Locale.US)
        val dateStr = sdfDate.format(Date(now))
        val timeStr = sdfTime.format(Date(now))

        val staticValues = mapOf(
            "activity" to sanitize(activityName),
            "patient" to sanitize(patientName),
            "device" to sanitize(sensorId),
            "sensor" to sanitize(sensorId),
            "date" to dateStr,
            "time" to timeStr,
            "datetime" to "${dateStr}_${timeStr}",
            "timestamp" to now.toString()
        )

        var resolved = namingTemplate
        staticValues.forEach { (key, value) ->
            resolved = resolved.replace("{$key}", value)
        }
        resolvedPrefix = resolved
    }

    fun logData(tag: String, header: String, dataLine: String) {
        val tagValue = sanitize(tag)
        val filename = resolvedPrefix.replace("{tag}", tagValue)
        val streamKey = filename

        try {
            val stream = streams.getOrPut(streamKey) {
                val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
                val file = rootDoc?.findFile(filename) ?: rootDoc?.createFile("text/plain", filename)
                context.contentResolver.openOutputStream(file!!.uri, "wa")!!
            }

            if (!headersWritten.contains(streamKey)) {
                stream.write("$header\n".toByteArray())
                headersWritten.add(streamKey)
            }

            stream.write("$dataLine\n".toByteArray())
            stream.flush()
        } catch (e: Exception) {
            Log.e("DataLogger", "Error logging for $deviceId: ${e.message}")
        }
    }

    fun closeAll() {
        streams.values.forEach { it.close() }
        streams.clear()
        headersWritten.clear()
    }

    private fun sanitize(value: String): String {
        return value.replace(Regex("""[\\/:*?"<>| ]"""), "_")
            .replace(Regex("""_+"""), "_")
            .trim('_')
    }
}
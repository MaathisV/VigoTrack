package com.maathisv.vigotrack.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStream

class DataLogger(private val context: Context, private val rootUri: Uri, sensorId: String) {

    private val deviceId = sensorId
    private val streams = mutableMapOf<String, OutputStream>()
    private val headersWritten = mutableSetOf<String>()

    fun logData(tag: String, header: String, dataLine: String) {
        val streamKey = "${deviceId}_$tag"

        try {
            val stream = streams.getOrPut(streamKey) {
                val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
                // Distinct filename for every watch: polar_123_HR.txt, polar_456_HR.txt
                val filename = "${deviceId}_${tag}.txt"

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
}
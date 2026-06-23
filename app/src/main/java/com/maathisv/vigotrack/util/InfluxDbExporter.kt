package com.maathisv.vigotrack.util

import android.util.Log
import com.maathisv.vigotrack.sensor.api.SensorDataPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val FLUSH_INTERVAL_MS = 5000L
private const val MAX_BATCH_SIZE = 100
private const val CONNECT_TIMEOUT_S = 10L
private const val WRITE_TIMEOUT_S = 30L

class InfluxDbExporter(
    private val serverUrl: String,
    private val activityId: String,
    private val activeFeatures: Set<String>,
    private val serializer: DataSerializer,
    private val onSendResult: ((Boolean) -> Unit)? = null,
    private val stageName: String = "NoStage",
    private val activityName: String = "",
    private val activityCategory: String = "",
    private val vendorMap: Map<String, String> = emptyMap()
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_S, TimeUnit.SECONDS)
        .build()

    private val buffer = mutableMapOf<String, MutableList<SensorDataPoint>>()

    fun observe(flow: SharedFlow<Pair<String, SensorDataPoint>>, scope: CoroutineScope) {
        scope.launch {
            launch {
                while (isActive) {
                    delay(FLUSH_INTERVAL_MS)
                    flush()
                }
            }
            scope.launch {
                flow.collect { (deviceId, dataPoint) ->
                    if (dataPoint.dataType.name in activeFeatures) {
                        val key = "$deviceId:${dataPoint.dataType.name}"
                        val list = buffer.getOrPut(key) { mutableListOf() }
                        list.add(dataPoint)
                        if (list.size >= MAX_BATCH_SIZE) {
                            flushBuffer(key, list)
                        }
                    }
                }
            }
        }
    }

    private fun flush() {
        val entries = buffer.entries.toList()
        for ((key, list) in entries) {
            if (list.isNotEmpty()) {
                flushBuffer(key, list)
            }
        }
    }

    private fun flushBuffer(key: String, list: MutableList<SensorDataPoint>) {
        if (list.isEmpty()) return
        val snapshot = list.toList()
        list.clear()
        sendBatch(key, snapshot)
    }

    private fun sendBatch(key: String, points: List<SensorDataPoint>) {
        val (deviceId, dataType) = key.split(":", limit = 2)
        val tags = mapOf(
            "deviceId" to deviceId,
            "activityId" to activityId,
            "stage" to stageName,
            "activity" to activityName,
            "category" to activityCategory,
            "vendor" to (vendorMap[deviceId] ?: "Unknown")
        )
        val body = serializer.serialize(deviceId, dataType, points, tags)
        if (body.isEmpty()) return
        val requestBody = body.toRequestBody(serializer.contentType.toMediaTypeOrNull())
        val requestBuilder = Request.Builder()
            .url("${serverUrl.trim()}${serializer.urlPath}")
            .post(requestBody)
        serializer.extraHeaders.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Send failed: ${e.message}")
                onSendResult?.invoke(false)
            }
            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                if (!success) {
                    Log.w(TAG, "Server error: ${response.code} ${response.body?.string()}")
                }
                response.close()
                onSendResult?.invoke(success)
            }
        })
    }

    companion object {
        private const val TAG = "InfluxDbExporter"
    }
}

class InfluxLineProtocolSerializer(
    private val authToken: String = "",
    private val dbName: String = "vigotrack",
    private val org: String = "vigo"
) : DataSerializer {

    override val contentType = "text/plain"

    override val urlPath = "/api/v2/write?org=$org&bucket=${dbName.trim()}&precision=ns"

    override val extraHeaders: Map<String, String>
        get() = if (authToken.isNotBlank()) mapOf("Authorization" to "Token $authToken") else emptyMap()

    override fun serialize(deviceId: String, dataType: String, points: List<SensorDataPoint>, tags: Map<String, String>): String {
        val tagStr = tags.entries.joinToString(",") { "${it.key}=${it.value}" }
        return points.joinToString("\n") { p ->
            "$dataType,$tagStr ${toFields(p)} ${p.timestamp}"
        }
    }

    private fun toFields(p: SensorDataPoint): String = when (p) {
        is SensorDataPoint.HeartRate -> buildString {
            append("hr=${p.hr}i")
            append(",rrAvailable=${if (p.rrAvailable) 't' else 'f'}")
            append(",contactStatus=${if (p.contactStatus) 't' else 'f'}")
            append(",contactStatusSupported=${if (p.contactStatusSupported) 't' else 'f'}")
            append(",ppgQuality=${p.ppgQuality}i")
            append(",correctedHr=${p.correctedHr}i")
            if (p.rrsMs.isNotEmpty()) {
                append(",rrsMs=\"${p.rrsMs.joinToString(",")}\"")
            }
        }
        is SensorDataPoint.Ppi -> buildString {
            append("ppiMs=${p.ppiMs}i")
            append(",hr=${p.hr}i")
            append(",errorEstimate=${p.errorEstimate}i")
            append(",blockerBit=${if (p.blockerBit) 't' else 'f'}")
            append(",skinContactStatus=${if (p.skinContactStatus) 't' else 'f'}")
            append(",skinContactSupported=${if (p.skinContactSupported) 't' else 'f'}")
        }
        is SensorDataPoint.Accelerometer -> "x=${p.x},y=${p.y},z=${p.z}"
        is SensorDataPoint.Electrocardiogram -> buildString {
            append("voltage=${p.voltage}")
            append(",bioz=${p.bioz}i")
            append(",status=${p.status.toInt()}i")
        }
        is SensorDataPoint.Gyroscope -> "x=${p.x},y=${p.y},z=${p.z}"
        is SensorDataPoint.EulerAngles -> "roll=${p.roll},pitch=${p.pitch},yaw=${p.yaw}"
        is SensorDataPoint.Quaternion -> "w=${p.w},x=${p.x},y=${p.y},z=${p.z}"
        is SensorDataPoint.FreeAcceleration -> "x=${p.x},y=${p.y},z=${p.z}"
    }
}
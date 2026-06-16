package com.maathisv.vigotrack.util

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.maathisv.vigotrack.models.ActivitySession
import androidx.core.net.toUri

private const val TAG = "ExportMarker"

fun toggleActivityExportMarker(
    context: Context,
    logUri: String,
    template: String,
    stageName: String,
    activity: ActivitySession,
    create: Boolean
) {
    for (link in activity.links) {
        val dir = resolveActivityExportDir(context, logUri, template, stageName, activity, link) ?: continue
        try {
            if (create) {
                if (findInvalidMarker(dir) == null) {
                    dir.createFile("text/plain", "_INVALIDE")
                    Log.d(TAG, "Created _INVALIDE for ${activity.id}")
                }
            } else {
                val marker = findInvalidMarker(dir)
                if (marker != null) {
                    marker.delete()
                    Log.d(TAG, "Deleted _INVALIDE for ${activity.id}")
                } else {
                    Log.d(TAG, "No _INVALIDE found for ${activity.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ${if (create) "create" else "delete"} _INVALIDE for ${activity.id}: ${e.message}")
        }
    }
}

fun resolveActivityExportDir(
    context: Context,
    logUri: String,
    template: String,
    stageName: String,
    activity: ActivitySession,
    link: ActivitySession.ActivityLink
): DocumentFile? {
    if (logUri.isBlank()) return null

    val values = mapOf(
        "stage" to stageName,
        "patient" to link.patientName.ifBlank { "NoPatient" },
        "category" to activity.activityType.category.name,
        "activity" to activity.activityType.displayName,
        "activity_id" to activity.id,
        "device" to link.sensorId,
        "sensor" to link.sensorId,
        "tag" to "",
        "date" to "",
        "time" to "",
        "datetime" to "",
        "timestamp" to ""
    )

    var dirPath = template
    val lastSlash = dirPath.lastIndexOf('/')
    dirPath = if (lastSlash >= 0) dirPath.substring(0, lastSlash) else ""
    values.forEach { (key, value) ->
        dirPath = dirPath.replace("{$key}", sanitizeDirName(value))
    }

    val segments = dirPath.split("/").filter { it.isNotBlank() }
    if (segments.isEmpty()) return null

    val rootUri = logUri.toUri()
    var current = DocumentFile.fromTreeUri(context, rootUri) ?: return null
    for (segment in segments) {
        current = current.findFile(segment) ?: return null
    }
    return current
}

fun findInvalidMarker(dir: DocumentFile): DocumentFile? {
    return dir.listFiles().find { it.name?.startsWith("_INVALIDE") == true }
}

fun sanitizeDirName(value: String): String {
    return value.replace(Regex("""[\\/:*?"<>| ]"""), "_")
        .replace(Regex("""_+"""), "_")
        .trim('_')
}

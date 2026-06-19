package com.maathisv.vigotrack.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStream

class DataLogger(
    private val context: Context,
    private val rootUri: Uri,
    pathTemplate: String,
    staticValues: Map<String, String>
) {
    private val streams = mutableMapOf<String, OutputStream>()
    private val headersWritten = mutableSetOf<String>()
    private val dirCache = mutableMapOf<String, DocumentFile?>()
    private val resolvedPrefix: String

    init {
        var resolved = pathTemplate
        staticValues.forEach { (key, value) ->
            resolved = resolved.replace("{$key}", sanitize(value))
        }
        resolvedPrefix = resolved
    }

    fun logData(tag: String, header: String, dataLine: String) {
        val fullPath = resolvedPrefix.replace("{tag}", sanitize(tag))
        val segments = fullPath.split("/").filter { it.isNotBlank() }
        if (segments.isEmpty()) return

        try {
            val stream = streams.getOrPut(fullPath) {
                val dirSegments = segments.dropLast(1)
                val leafDir = if (dirSegments.isEmpty()) {
                    DocumentFile.fromTreeUri(context, rootUri)
                } else {
                    val dirPath = dirSegments.joinToString("/")
                    dirCache.getOrPut(dirPath) { buildDirHierarchy(dirSegments) }
                }

                val filename = segments.last()
                val file = leafDir?.findFile(filename) ?: leafDir?.createFile("text/plain", filename)
                context.contentResolver.openOutputStream(file!!.uri, "wa")!!
            }

            if (!headersWritten.contains(fullPath)) {
                stream.write("$header\n".toByteArray())
                headersWritten.add(fullPath)
            }

            stream.write("$dataLine\n".toByteArray())
            stream.flush()
        } catch (e: Exception) {
            Log.e("DataLogger", "Error logging: ${e.message}")
        }
    }

    fun closeAll() {
        streams.values.forEach { it.close() }
        streams.clear()
        headersWritten.clear()
        dirCache.clear()
    }

    private fun buildDirHierarchy(dirSegments: List<String>): DocumentFile? {
        var current = DocumentFile.fromTreeUri(context, rootUri) ?: return null
        for (segment in dirSegments) {
            current = current.findFile(segment) ?: current.createDirectory(segment) ?: return null
        }
        return current
    }

    private fun sanitize(value: String): String {
        return value.replace(Regex("""[\\/:*?"<>| ]"""), "_")
            .replace(Regex("""_+"""), "_")
            .trim('_')
    }
}

package com.maathisv.vigotrack.util

import android.content.Context
import androidx.core.content.edit
import com.maathisv.vigotrack.services.DEFAULT_TEMPLATE

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("vigo_prefs", Context.MODE_PRIVATE)

    var logUri: String
        get() = prefs.getString("log_uri", "") ?: ""
        set(value) = prefs.edit { putString("log_uri", value) }

    var fileNamingTemplate: String
        get() = prefs.getString("file_naming_template", DEFAULT_TEMPLATE) ?: DEFAULT_TEMPLATE
        set(value) = prefs.edit { putString("file_naming_template", value) }

    fun resetNamingTemplate() {
        fileNamingTemplate = DEFAULT_TEMPLATE
    }

    fun isShowFeatureEnabled(feature: String): Boolean = prefs.getBoolean("show_$feature", true)

    fun setShowFeature(feature: String, enabled: Boolean) {
        prefs.edit { putBoolean("show_$feature", enabled) }
    }

    fun isLogFeatureEnabled(feature: String): Boolean = prefs.getBoolean("log_$feature", true)

    fun setLogFeature(feature: String, enabled: Boolean) {
        prefs.edit { putBoolean("log_$feature", enabled) }
    }

    fun getAllShowFeatures(): Map<String, Boolean> = FEATURES.associateWith { isShowFeatureEnabled(it) }

    fun getAllLogFeatures(): Map<String, Boolean> = FEATURES.associateWith { isLogFeatureEnabled(it) }

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) = prefs.edit { putString("server_url", value) }

    var authToken: String
        get() = prefs.getString("auth_token", "") ?: ""
        set(value) = prefs.edit { putString("auth_token", value) }

    var dbName: String
        get() = prefs.getString("db_name", "vigotrack") ?: "vigotrack"
        set(value) = prefs.edit { putString("db_name", value) }

    fun isServerFeatureEnabled(feature: String): Boolean = prefs.getBoolean("server_$feature", false)

    fun setServerFeature(feature: String, enabled: Boolean) {
        prefs.edit { putBoolean("server_$feature", enabled) }
    }

    fun getAllServerFeatures(): Map<String, Boolean> = FEATURES.associateWith { isServerFeatureEnabled(it) }

    var serverLastSuccess: Boolean
        get() = prefs.getBoolean("server_last_success", false)
        set(value) = prefs.edit { putBoolean("server_last_success", value) }

    var serverLastCheckedMs: Long
        get() = prefs.getLong("server_last_checked_ms", 0L)
        set(value) = prefs.edit { putLong("server_last_checked_ms", value) }

    fun getActiveFeatures(): Set<String> {
        return FEATURES.filter { isShowFeatureEnabled(it) || isLogFeatureEnabled(it) }.toSet()
    }

    fun getSampleRate(deviceId: String, feature: String): Int =
        prefs.getInt("${deviceId}_${feature}_sample_rate", 0)

    fun setSampleRate(deviceId: String, feature: String, rate: Int) {
        prefs.edit { putInt("${deviceId}_${feature}_sample_rate", rate) }
    }

    fun getResolution(deviceId: String, feature: String): Int =
        prefs.getInt("${deviceId}_${feature}_resolution", 0)

    fun setResolution(deviceId: String, feature: String, resolution: Int) {
        prefs.edit { putInt("${deviceId}_${feature}_resolution", resolution) }
    }

    companion object {
        private val FEATURES = listOf("HR", "PPI", "ACC", "ECG", "EULER", "QUATERNION", "FREE_ACCELERATION")
    }
}

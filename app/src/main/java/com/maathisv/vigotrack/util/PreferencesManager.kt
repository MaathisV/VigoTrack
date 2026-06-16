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

    fun getActiveFeatures(): Set<String> {
        return FEATURES.filter { isShowFeatureEnabled(it) || isLogFeatureEnabled(it) }.toSet()
    }

    companion object {
        private val FEATURES = listOf("HR", "PPI", "ACC", "ECG", "EULER", "QUATERNION", "FREE_ACCELERATION")
    }
}

package com.maathisv.vigotrack.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maathisv.vigotrack.data.PatientDataSource
import com.maathisv.vigotrack.data.SensorPatientLinkDataSource
import com.maathisv.vigotrack.data.StageDataSource
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityType
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Sensor
import com.maathisv.vigotrack.models.SensorPatientLink
import com.maathisv.vigotrack.models.Stage
import com.maathisv.vigotrack.repository.ActivityRepository
import com.maathisv.vigotrack.repository.SensorRepository
import com.maathisv.vigotrack.services.ACTION_START_STREAMS
import com.maathisv.vigotrack.services.ACTION_STOP_STREAMS
import com.maathisv.vigotrack.services.EXTRA_ACTIVITY_CATEGORY
import com.maathisv.vigotrack.services.EXTRA_ACTIVITY_ID
import com.maathisv.vigotrack.services.EXTRA_ACTIVITY_NAME
import com.maathisv.vigotrack.services.EXTRA_PATIENT_NAMES
import com.maathisv.vigotrack.services.EXTRA_SENSOR_IDS
import com.maathisv.vigotrack.services.EXTRA_SESSION_DATE
import com.maathisv.vigotrack.services.EXTRA_STAGE_NAME
import com.maathisv.vigotrack.services.SensorService
import com.maathisv.vigotrack.util.PreferencesManager
import com.maathisv.vigotrack.util.toggleActivityExportMarker
import com.polar.sdk.api.model.PolarSensorSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val application: Application,
    private val activityRepo: ActivityRepository,
    private val sensorRepo: SensorRepository,
    private val patientDataSource: PatientDataSource,
    private val stageDataSource: StageDataSource,
    private val sensorPatientLinkDataSource: SensorPatientLinkDataSource
) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application)

    val connectionState = sensorRepo.connectionState
    val deviceConnectionStates = sensorRepo.deviceConnectionStates
    val activities = activityRepo.allActivities
    val sensorLiveData = sensorRepo.liveData

    val connectedDevicesList = combine(
        sensorRepo.connectedDeviceIds,
        sensorRepo.savedSensors
    ) { ids, saved ->
        ids.mapNotNull { id -> saved.find { it.deviceId == id } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val scannedDevices: StateFlow<List<Sensor>> = sensorRepo.discoveredDevices
        .map { it.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isConnectingToId = MutableStateFlow<String?>(null)
    val isConnectingToId: StateFlow<String?> = _isConnectingToId.asStateFlow()

    val patients: StateFlow<List<Patient>> = patientDataSource.getAllPatients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stages: StateFlow<List<Stage>> = stageDataSource.getAllStages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sensorPatientLinks: StateFlow<List<SensorPatientLink>> = sensorPatientLinkDataSource.getAllLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceAvailableDataTypes = sensorRepo.availableStreamDataTypes

    private val _availableSettings = MutableStateFlow<Map<String, Map<String, Set<Int>>>>(emptyMap())
    val availableSettings: StateFlow<Map<String, Map<String, Set<Int>>>> = _availableSettings.asStateFlow()

    private val _selectedSettings = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val selectedSettings: StateFlow<Map<String, Pair<Int, Int>>> = _selectedSettings.asStateFlow()

    fun queryAvailableSettings(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val supported = deviceAvailableDataTypes.value[deviceId] ?: emptySet()
            listOf("ACC", "ECG").filter { it in supported }.forEach { feature ->
                val result = sensorRepo.getAvailableSettings(deviceId, feature)
                if (result != null) {
                    _availableSettings.value = _availableSettings.value + ("$deviceId:$feature" to result)
                    _selectedSettings.value = _selectedSettings.value + ("$deviceId:$feature" to (
                        prefsManager.getSampleRate(deviceId, feature) to prefsManager.getResolution(deviceId, feature)
                    ))
                }
            }
        }
    }

    fun setSensorSettings(deviceId: String, feature: String, sampleRate: Int, resolution: Int) {
        prefsManager.setSampleRate(deviceId, feature, sampleRate)
        prefsManager.setResolution(deviceId, feature, resolution)
        _selectedSettings.value = _selectedSettings.value + ("$deviceId:$feature" to (sampleRate to resolution))
    }

    private fun buildSensorSettings(deviceId: String, feature: String): Any? {
        val sampleRate = prefsManager.getSampleRate(deviceId, feature)
        val resolution = prefsManager.getResolution(deviceId, feature)
        if (sampleRate == 0 && resolution == 0) return null
        val map = mutableMapOf<PolarSensorSetting.SettingType, Int>()
        if (sampleRate > 0) map[PolarSensorSetting.SettingType.SAMPLE_RATE] = sampleRate
        if (resolution > 0) map[PolarSensorSetting.SettingType.RESOLUTION] = resolution
        return PolarSensorSetting(map)
    }

    fun getAvailableFeaturesForDevice(deviceId: String): Set<String> {
        return sensorRepo.getAvailableFeaturesForDevice(deviceId)
    }

    private val _currentLogUri = MutableStateFlow(prefsManager.logUri)
    val currentLogUri: StateFlow<String> = _currentLogUri.asStateFlow()

    private val _namingTemplate = MutableStateFlow(prefsManager.fileNamingTemplate)
    val namingTemplate: StateFlow<String> = _namingTemplate.asStateFlow()

    private val _showFeatures = MutableStateFlow(prefsManager.getAllShowFeatures())
    val showFeatures: StateFlow<Map<String, Boolean>> = _showFeatures.asStateFlow()

    private val _logFeatures = MutableStateFlow(prefsManager.getAllLogFeatures())
    val logFeatures: StateFlow<Map<String, Boolean>> = _logFeatures.asStateFlow()

    fun toggleShowFeature(feature: String) {
        val current = _showFeatures.value.toMutableMap()
        current[feature] = !(current[feature] ?: true)
        _showFeatures.value = current
        prefsManager.setShowFeature(feature, current[feature] ?: true)
    }

    fun toggleLogFeature(feature: String) {
        val current = _logFeatures.value.toMutableMap()
        current[feature] = !(current[feature] ?: true)
        _logFeatures.value = current
        prefsManager.setLogFeature(feature, current[feature] ?: true)
    }

    private fun getActiveFeatures(): Set<String> = prefsManager.getActiveFeatures()

    fun updateLogUri(uri: String) {
        prefsManager.logUri = uri
        _currentLogUri.value = uri
    }

    fun updateNamingTemplate(template: String) {
        prefsManager.fileNamingTemplate = template
        _namingTemplate.value = template
    }

    fun resetNamingTemplate() {
        prefsManager.resetNamingTemplate()
        _namingTemplate.value = prefsManager.fileNamingTemplate
    }

    fun startScanning() {
        sensorRepo.startScanning()
    }

    fun connectToDevice(sensor: Sensor) {
        _isConnectingToId.value = sensor.deviceId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sensorRepo.connectToDevice(sensor)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Connection failed", e)
            } finally {
                _isConnectingToId.value = null
            }
        }
    }

    fun disconnectFromDevice(id: String) {
        _isConnectingToId.value = null
        viewModelScope.launch(Dispatchers.IO) {
            sensorRepo.requestDisconnect(id)
        }
    }

    fun renameSensor(deviceId: String, newName: String) {
        sensorRepo.updateSensorDisplayName(deviceId, newName)
    }

    fun createActivity(type: ActivityType, date: Long, stageId: Long? = null, onCreated: ((String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = java.util.UUID.randomUUID().toString()
            activityRepo.createActivity(type, date, stageId, id)
            withContext(Dispatchers.Main) {
                onCreated?.invoke(id)
            }
        }
    }

    fun createActivityAndLink(
        type: ActivityType,
        date: Long,
        stageId: Long?,
        patientId: Long?,
        patientName: String,
        sensorId: String,
        features: List<String>,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = java.util.UUID.randomUUID().toString()
            activityRepo.createActivity(type, date, stageId, id)
            if (sensorId.isNotBlank()) {
                val finalPatientId = if (patientId != null && patients.value.any { it.id == patientId }) {
                    patientId
                } else {
                    patientDataSource.insertPatient(Patient(name = patientName))
                }
                val link = ActivitySession.ActivityLink(
                    patientId = finalPatientId,
                    patientName = patientName,
                    sensorId = sensorId,
                    featuresToTrack = features
                )
                activityRepo.addLinkToActivity(id, link)
            }
            withContext(Dispatchers.Main) {
                onCreated(id)
            }
        }
    }

    fun addLink(activityId: String, patientId: Long?, patientName: String, sensorId: String, features: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPatientId = if (patientId != null && patients.value.any { it.id == patientId }) {
                patientId
            } else {
                patientDataSource.insertPatient(Patient(name = patientName))
            }
            val link = ActivitySession.ActivityLink(
                patientId = finalPatientId,
                patientName = patientName,
                sensorId = sensorId,
                featuresToTrack = features
            )
            activityRepo.addLinkToActivity(activityId, link)
        }
    }

    fun removeLink(activityId: String, sensorId: String, patientId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.removeLinkFromActivity(activityId, sensorId, patientId)
        }
    }

    fun toggleSession(activity: ActivitySession, checkedKeys: Set<String> = emptySet()) {
        viewModelScope.launch {
            val intent = Intent(getApplication(), SensorService::class.java)

            if (activity.isRunning) {
                activityRepo.stopActivity(activity)
                activity.links.forEach { sensorRepo.stopActivityStreaming(it.sensorId) }

                intent.action = ACTION_STOP_STREAMS
                getApplication<Application>().startService(intent)
            } else {
                val existingLinks = activity.links.map { it.sensorId to it.patientId }.toSet()
                val allActiveLinks = activity.links.toMutableList()

                val checkedPairs = checkedKeys.mapNotNull { key ->
                    val parts = key.split("_")
                    if (parts.size >= 2) {
                        val sensorId = parts.first()
                        val patientId = parts.drop(1).joinToString("_").let { str ->
                            if (str == "null") null else str.toLongOrNull()
                        }
                        sensorId to patientId
                    } else null
                }.toSet()

                sensorPatientLinks.value
                    .filter { link ->
                        val pair = link.sensorId to link.patientId
                        pair in checkedPairs && pair !in existingLinks
                    }
                    .forEach { link ->
                        val patientName = patients.value.find { it.id == link.patientId }?.name ?: "Inconnu"
                        val finalPatientId = if (link.patientId != null && patients.value.any { it.id == link.patientId }) {
                            link.patientId
                        } else {
                            patientDataSource.insertPatient(Patient(name = patientName))
                        }
                        val newLink = ActivitySession.ActivityLink(
                            patientId = finalPatientId,
                            patientName = patientName,
                            sensorId = link.sensorId,
                            featuresToTrack = link.features
                        )
                        activityRepo.addLinkToActivity(activity.id, newLink)
                        allActiveLinks.add(newLink)
                    }

                activityRepo.startActivity(activity)

                val activeFeatures = getActiveFeatures()
                val updatedActivity = activity.copy(
                    links = allActiveLinks.map { link ->
                        link.copy(featuresToTrack = link.featuresToTrack.filter { it in activeFeatures })
                    }
                )
                updatedActivity.links.forEach { link ->
                    link.featuresToTrack.forEach { feature ->
                        val settings = buildSensorSettings(link.sensorId, feature)
                        sensorRepo.startFeatureStream(link.sensorId, feature, settings)
                        delay(300)
                    }
                }

                val sensorIds = allActiveLinks.map { it.sensorId }.toTypedArray()
                val patientNames = allActiveLinks.map { it.patientName }.toTypedArray()
                val stageName = activity.stageId?.let { id ->
                    stages.value.find { it.id == id }?.name
                } ?: "NoStage"
                val intent = Intent(getApplication(), SensorService::class.java).apply {
                    action = ACTION_START_STREAMS
                    putExtra(EXTRA_SENSOR_IDS, sensorIds)
                    putExtra(EXTRA_PATIENT_NAMES, patientNames)
                    putExtra(EXTRA_ACTIVITY_NAME, activity.activityType.displayName)
                    putExtra(EXTRA_ACTIVITY_CATEGORY, activity.activityType.category.name)
                    putExtra(EXTRA_ACTIVITY_ID, activity.id)
                    putExtra(EXTRA_STAGE_NAME, stageName)
                    putExtra(EXTRA_SESSION_DATE, System.currentTimeMillis())
                }
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun addPatient(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            patientDataSource.insertPatient(Patient(name = name))
        }
    }

    fun deletePatient(patient: Patient) {
        viewModelScope.launch(Dispatchers.IO) {
            sensorPatientLinks.value.filter { it.patientId == patient.id }
                .forEach { sensorPatientLinkDataSource.deleteLink(it) }
            patientDataSource.deletePatient(patient)
        }
    }

    fun createSensorPatientLink(patientId: Long?, sensorId: String, features: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            sensorPatientLinkDataSource.insertLink(patientId, sensorId, features)
        }
    }

    fun deleteSensorPatientLink(link: SensorPatientLink) {
        viewModelScope.launch(Dispatchers.IO) {
            sensorPatientLinkDataSource.deleteLink(link)
        }
    }

    fun getActivitiesForStage(stageId: Long): Flow<List<ActivitySession>> =
        activityRepo.getActivitiesByStage(stageId)

    fun createActivityInStage(id: String, stageId: Long, type: ActivityType) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.createActivity(type, System.currentTimeMillis(), stageId, id)
        }
    }

    fun createStage(name: String, start: Long, end: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            stageDataSource.insertStage(Stage(name = name, startDate = start, endDate = end))
        }
    }

    fun updateActivityType(activity: ActivitySession, newType: ActivityType) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.updateActivity(activity.copy(activityType = newType))
        }
    }

    fun splitActivityOnTypeChange(activity: ActivitySession, newType: ActivityType, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.stopActivity(activity)
            val newId = java.util.UUID.randomUUID().toString()
            activityRepo.createActivity(newType, System.currentTimeMillis(), activity.stageId, newId)
            activity.links.forEach { link ->
                activityRepo.addLinkToActivity(newId, link)
            }
            val newActivity = activity.copy(
                id = newId, activityType = newType,
                links = activity.links, scheduledDate = System.currentTimeMillis()
            )
            activityRepo.startActivity(newActivity)
            withContext(Dispatchers.Main) {
                onComplete(newId)
            }
        }
    }

    fun startPatientStream(sensorId: String, features: List<String>) {
        viewModelScope.launch {
            val activeFeatures = getActiveFeatures()
            features.filter { it in activeFeatures }.forEach { feature ->
                val settings = buildSensorSettings(sensorId, feature)
                sensorRepo.startFeatureStream(sensorId, feature, settings)
            }
        }
    }

    fun stopPatientStream(sensorId: String) {
        viewModelScope.launch {
            sensorRepo.stopActivityStreaming(sensorId)
        }
    }

    fun markActivityAsStale(activityId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activities = activityRepo.allActivities.first()
            val activity = activities.find { it.id == activityId } ?: return@launch
            activityRepo.updateActivity(activity.copy(isStale = true))
            val stageName = stages.value.find { it.id == activity.stageId }?.name ?: "NoStage"
            toggleActivityExportMarker(application, _currentLogUri.value, _namingTemplate.value, stageName, activity, create = true)
        }
    }

    fun unmarkActivityAsStale(activityId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activities = activityRepo.allActivities.first()
            val activity = activities.find { it.id == activityId } ?: return@launch
            activityRepo.updateActivity(activity.copy(isStale = false))
            val stageName = stages.value.find { it.id == activity.stageId }?.name ?: "NoStage"
            toggleActivityExportMarker(application, _currentLogUri.value, _namingTemplate.value, stageName, activity, create = false)
        }
    }
}

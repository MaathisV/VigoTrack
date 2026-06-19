package com.maathisv.vigotrack.data

import android.content.Context
import android.net.Uri
import com.maathisv.vigotrack.models.ActivityCategory
import com.maathisv.vigotrack.models.ActivitySession
import com.maathisv.vigotrack.models.ActivityType
import com.maathisv.vigotrack.models.Patient
import com.maathisv.vigotrack.models.Stage

import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

data class ImportPreview(
    val patientCount: Int,
    val stageCount: Int,
    val activityCount: Int,
    val linkCount: Int
)

data class ImportResult(
    val patientsCreated: List<String> = emptyList(),
    val patientsSkipped: List<String> = emptyList(),
    val stagesCreated: List<String> = emptyList(),
    val activitiesCreated: List<String> = emptyList(),
    val linksCreated: Int = 0,
    val errors: List<String> = emptyList()
)

class ConfigImporter(
    private val patientDataSource: PatientDataSource,
    private val stageDataSource: StageDataSource,
    private val activityDataSource: ActivityDataSource,
    private val activityTypeDataSource: ActivityTypeDataSource
) {

    suspend fun parse(context: Context, uri: Uri): ImportPreview {
        val jsonString = readJson(context, uri)
        val json = JSONObject(jsonString)
        validate(json)

        val patientCount = countPatients(json)
        val stageCount = countStages(json)
        var activityCount = 0
        var linkCount = 0

        val stages = json.getJSONArray("stages")
        for (i in 0 until stages.length()) {
            val stage = stages.getJSONObject(i)
            activityCount += countActivities(stage, "activities")
            activityCount += countActivities(stage, "bilans")
            linkCount += countLinks(stage, "activities")
            linkCount += countLinks(stage, "bilans")
        }

        return ImportPreview(
            patientCount = patientCount,
            stageCount = stageCount,
            activityCount = activityCount,
            linkCount = linkCount
        )
    }

    suspend fun import(context: Context, uri: Uri): ImportResult {
        val jsonString = readJson(context, uri)
        val json = JSONObject(jsonString)
        validate(json)

        val errors = mutableListOf<String>()
        val patientsCreated = mutableListOf<String>()
        val patientsSkipped = mutableListOf<String>()
        val stagesCreated = mutableListOf<String>()
        val activitiesCreated = mutableListOf<String>()
        var linksCreated = 0

        val existingPatients = patientDataSource.getAllPatients().first()

        val patientsArray = json.optJSONArray("patients")
        if (patientsArray != null) {
            for (i in 0 until patientsArray.length()) {
                val patientObj = patientsArray.getJSONObject(i)
                val name = patientObj.getString("name")
                val exists = existingPatients.any { it.name == name }
                if (exists) {
                    patientsSkipped.add(name)
                } else {
                    patientDataSource.insertPatient(Patient(name = name))
                    patientsCreated.add(name)
                }
            }
        }

        val allPatients = patientDataSource.getAllPatients().first()

        val stagesArray = json.getJSONArray("stages")
        for (i in 0 until stagesArray.length()) {
            val stageObj = stagesArray.getJSONObject(i)
            val stageName = stageObj.getString("name")
            val startDate = parseDate(stageObj.getString("startDate"))
            val endDate = parseDate(stageObj.getString("endDate"))

            if (startDate == 0L || endDate == 0L) {
                errors.add("$stageName : dates invalides")
                continue
            }

            val stageId = stageDataSource.insertStage(
                Stage(name = stageName, startDate = startDate, endDate = endDate)
            )
            stagesCreated.add(stageName)

            val customTypes = mutableListOf<ActivityType>()

            linksCreated += importActivityGroup(
                stageObj, "activities", stageId, allPatients, activitiesCreated, errors, customTypes
            )
            linksCreated += importActivityGroup(
                stageObj, "bilans", stageId, allPatients, activitiesCreated, errors, customTypes
            )

            if (customTypes.isNotEmpty()) {
                activityTypeDataSource.saveTypes(customTypes)
            }
        }

        return ImportResult(
            patientsCreated = patientsCreated,
            patientsSkipped = patientsSkipped,
            stagesCreated = stagesCreated,
            activitiesCreated = activitiesCreated,
            linksCreated = linksCreated,
            errors = errors
        )
    }

    private suspend fun importActivityGroup(
        stageObj: JSONObject,
        key: String,
        stageId: Long,
        allPatients: List<Patient>,
        activitiesCreated: MutableList<String>,
        errors: MutableList<String>,
        customTypes: MutableList<ActivityType>
    ): Int {
        val array = stageObj.optJSONArray(key) ?: return 0
        var linksCreated = 0

        for (i in 0 until array.length()) {
            var typeDisplay = ""
            var typeStr = ""
            try {
                val item = array.getJSONObject(i)
                typeStr = item.getString("type")
                typeDisplay = item.optString("typeDisplayName", typeStr)
                val typeCategoryStr = item.optString("typeCategory", "ACTIVITE")

                val activityType = ActivityType.fromName(typeStr)
                    ?: ActivityType(
                        name = typeStr,
                        displayName = typeDisplay,
                        category = try {
                            ActivityCategory.valueOf(typeCategoryStr)
                        } catch (_: IllegalArgumentException) {
                            ActivityCategory.ACTIVITE
                        }
                    )

                if (ActivityType.fromName(typeStr) == null && customTypes.none { it.name == typeStr }) {
                    customTypes.add(activityType)
                }

                val dateStr = item.getString("scheduledDate")
                val scheduledDate = parseDate(dateStr)
                if (scheduledDate == 0L) {
                    errors.add("$typeStr / $dateStr : date invalide")
                    continue
                }

                val activityId = UUID.randomUUID().toString()
                activityDataSource.insertActivity(
                    ActivitySession(
                        id = activityId,
                        activityType = activityType,
                        scheduledDate = scheduledDate,
                        stageId = stageId
                    )
                )
                activitiesCreated.add(typeDisplay)

                val linksArray = item.optJSONArray("links")
                if (linksArray != null) {
                    for (j in 0 until linksArray.length()) {
                        var patientName = ""
                        var sensorId = ""
                        try {
                            val linkObj = linksArray.getJSONObject(j)
                            patientName = linkObj.getString("patientName")
                            sensorId = linkObj.optString("sensorId", "")
                            val featuresArray = linkObj.optJSONArray("features")
                            val features = if (featuresArray != null) {
                                (0 until featuresArray.length()).map { featuresArray.getString(it) }
                            } else {
                                listOf("HR")
                            }

                            if (sensorId.isBlank()) {
                                errors.add("$typeStr / $patientName / \"\" : capteur requis")
                                continue
                            }

                            val patient = allPatients.find { it.name == patientName }
                            if (patient == null) {
                                errors.add("$typeStr / $patientName / $sensorId : patient introuvable")
                                continue
                            }

                            activityDataSource.insertLink(
                                activityId,
                                ActivitySession.ActivityLink(
                                    patientId = patient.id,
                                    patientName = patientName,
                                    sensorId = sensorId,
                                    featuresToTrack = features
                                )
                            )
                            linksCreated++
                        } catch (e: Exception) {
                            errors.add("$typeStr / $patientName / $sensorId : ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                errors.add("$typeStr : ${e.message}")
            }
        }

        return linksCreated
    }

    private fun readJson(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Impossible de lire le fichier")
    }

    private fun validate(obj: JSONObject) {
        val version = obj.optInt("version", -1)
        if (version != 1) {
            throw IllegalArgumentException("Version $version non supportée (attendue: 1)")
        }
        if (!obj.has("stages") || obj.getJSONArray("stages").length() == 0) {
            throw IllegalArgumentException("Le fichier ne contient aucun stage")
        }
    }

    companion object {
        private val DATE_FORMATS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )

        fun parseDate(dateStr: String): Long {
            for (fmt in DATE_FORMATS) {
                try {
                    return SimpleDateFormat(fmt, Locale.US).parse(dateStr)?.time ?: 0
                } catch (_: Exception) {}
            }
            return 0
        }

        private fun countPatients(json: JSONObject): Int {
            val arr = json.optJSONArray("patients") ?: return 0
            return arr.length()
        }

        private fun countStages(json: JSONObject): Int {
            return json.getJSONArray("stages").length()
        }

        private fun countActivities(stage: JSONObject, key: String): Int {
            val arr = stage.optJSONArray(key) ?: return 0
            return arr.length()
        }

        private fun countLinks(stage: JSONObject, key: String): Int {
            val arr = stage.optJSONArray(key) ?: return 0
            var count = 0
            for (i in 0 until arr.length()) {
                val links = arr.getJSONObject(i).optJSONArray("links")
                count += links?.length() ?: 0
            }
            return count
        }
    }
}

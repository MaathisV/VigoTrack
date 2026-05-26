package com.maathisv.vigotrack.data

import com.maathisv.vigotrack.data.dao.PatientDao
import com.maathisv.vigotrack.data.entities.PatientEntity
import com.maathisv.vigotrack.models.Patient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PatientDataSource {
    fun getAllPatients(): Flow<List<Patient>>
    suspend fun insertPatient(patient: Patient): Long
    suspend fun deletePatient(patient: Patient)
}

class RoomPatientDataSource(private val patientDao: PatientDao) : PatientDataSource {

    override fun getAllPatients(): Flow<List<Patient>> =
        patientDao.getAllPatients().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertPatient(patient: Patient): Long {
        return patientDao.insertPatient(patient.toEntity())
    }

    override suspend fun deletePatient(patient: Patient) {
        patientDao.deletePatient(patient.toEntity())
    }
}

private fun PatientEntity.toDomain() = Patient(
    id = id,
    name = name,
    isCalibrated = isCalibrated
)

private fun Patient.toEntity() = PatientEntity(
    id = id,
    name = name,
    isCalibrated = isCalibrated
)

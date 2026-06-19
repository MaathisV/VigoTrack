package com.maathisv.vigotrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.maathisv.vigotrack.data.dao.ActivityDao
import com.maathisv.vigotrack.data.dao.ActivityTypeDao
import com.maathisv.vigotrack.data.dao.PatientDao
import com.maathisv.vigotrack.data.dao.SensorDao
import com.maathisv.vigotrack.data.dao.SensorPatientLinkDao
import com.maathisv.vigotrack.data.dao.StageDao
import com.maathisv.vigotrack.data.entities.ActivityLinkEntity
import com.maathisv.vigotrack.data.entities.ActivitySessionEntity
import com.maathisv.vigotrack.data.entities.ActivityTypeEntity
import com.maathisv.vigotrack.data.entities.PatientEntity
import com.maathisv.vigotrack.data.entities.SensorEntity
import com.maathisv.vigotrack.data.entities.SensorPatientLinkEntity
import com.maathisv.vigotrack.data.entities.StageEntity

@Database(
    entities = [
        SensorEntity::class,
        ActivitySessionEntity::class,
        ActivityLinkEntity::class,
        PatientEntity::class,
        SensorPatientLinkEntity::class,
        StageEntity::class,
        ActivityTypeEntity::class
    ],
    version = 8
)

abstract class VigoTrackDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun activityDao(): ActivityDao
    abstract fun patientDao(): PatientDao
    abstract fun sensorPatientLinkDao(): SensorPatientLinkDao
    abstract fun stageDao(): StageDao
    abstract fun activityTypeDao(): ActivityTypeDao

    companion object {
        @Volatile
        private var INSTANCE: VigoTrackDatabase? = null

        fun getDatabase(context: Context): VigoTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                VigoTrackDatabase::class.java,
                                "vigo_track_database"
                            ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
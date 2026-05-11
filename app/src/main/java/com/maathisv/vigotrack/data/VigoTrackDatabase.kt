package com.maathisv.vigotrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.maathisv.vigotrack.data.dao.*
import com.maathisv.vigotrack.data.entities.*

@Database(
    entities = [
        SensorEntity::class,
        ActivitySessionEntity::class,
        ActivityLinkEntity::class,
        PatientEntity::class
    ],
    version = 2
)

abstract class VigoTrackDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun activityDao(): ActivityDao
    abstract fun patientDao(): PatientDao

    companion object {
        @Volatile
        private var INSTANCE: VigoTrackDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `patients` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`isCalibrated` INTEGER NOT NULL DEFAULT 0, " +
                            "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): VigoTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VigoTrackDatabase::class.java,
                    "vigo_track_database"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
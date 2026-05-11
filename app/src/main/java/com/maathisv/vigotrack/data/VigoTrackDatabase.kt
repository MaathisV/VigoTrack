package com.maathisv.vigotrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.maathisv.vigotrack.data.dao.*
import com.maathisv.vigotrack.data.entities.*

@Database(
    entities = [
        SensorEntity::class,
        ActivitySessionEntity::class,
        ActivityLinkEntity::class
    ],
    version = 1
)

abstract class VigoTrackDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: VigoTrackDatabase? = null

        fun getDatabase(context: Context): VigoTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VigoTrackDatabase::class.java,
                    "vigo_track_database"
                ).fallbackToDestructiveMigration(true).build() // WILL DELETE ALL DATA IF SCHEMA CHANGE !
                INSTANCE = instance
                instance
            }
        }
    }
}
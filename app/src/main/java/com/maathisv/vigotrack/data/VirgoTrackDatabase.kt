package com.maathisv.vigotrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.maathisv.vigotrack.data.entities.SensorEntity
import com.maathisv.vigotrack.data.dao.SensorDao

@Database(entities = [SensorEntity::class], version = 1)
abstract class VigoTrackDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao

    companion object {
        @Volatile
        private var INSTANCE: VigoTrackDatabase? = null

        fun getDatabase(context: Context): VigoTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VigoTrackDatabase::class.java,
                    "vigo_track_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
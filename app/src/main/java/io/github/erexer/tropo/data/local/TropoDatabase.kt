package com.tropo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LocationEntity::class, WeatherEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TropoDatabase : RoomDatabase() {

    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: TropoDatabase? = null

        fun getInstance(context: Context): TropoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TropoDatabase::class.java,
                    "tropo_weather.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
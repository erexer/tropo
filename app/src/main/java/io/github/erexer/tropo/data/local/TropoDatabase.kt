package io.github.erexer.tropo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WeatherEntity::class, LocationEntity::class], version = 1, exportSchema = false)
abstract class TropoDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
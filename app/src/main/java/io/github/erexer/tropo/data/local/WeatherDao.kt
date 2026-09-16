package io.github.erexer.tropo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather WHERE locationId = :locationId")
    suspend fun getWeather(locationId: Long): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM locations WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedLocation(): LocationEntity?

    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Query("UPDATE locations SET isSelected = (id = :selectedId)")
    suspend fun setSelectedLocation(selectedId: Long)
}
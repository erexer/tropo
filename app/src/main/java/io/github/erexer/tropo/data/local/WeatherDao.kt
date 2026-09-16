package com.tropo.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class LocationWithWeather(
    @Embedded val location: LocationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "locationId"
    )
    val weather: WeatherEntity?
)

@Dao
interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM locations WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryLocation(): LocationEntity?

    @Transaction
    @Query("SELECT * FROM locations WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryLocationWithWeather(): LocationWithWeather?

    @Transaction
    @Query("SELECT * FROM locations")
    fun getAllLocationsWithWeather(): Flow<List<LocationWithWeather>>
}
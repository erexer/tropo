package io.github.erexer.tropo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather WHERE locationId = :locationId")
    suspend fun getWeather(locationId: Long): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM locations WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedLocation(): LocationEntity?

    @Query("SELECT * FROM locations WHERE isSelected = 1 LIMIT 1")
    fun observeSelectedLocation(): Flow<LocationEntity?>

    @Query(
        """
        SELECT weather.* FROM weather
        INNER JOIN locations ON weather.locationId = locations.id
        WHERE locations.isSelected = 1 LIMIT 1
        """,
    )
    fun observeSelectedWeather(): Flow<WeatherEntity?>

    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :locationId LIMIT 1")
    suspend fun getLocation(locationId: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE isFavorite = 1 ORDER BY favoriteOrder, name COLLATE NOCASE")
    suspend fun getFavoriteLocations(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Query("UPDATE locations SET isSelected = (id = :selectedId)")
    suspend fun setSelectedLocation(selectedId: Long)

    @Transaction
    suspend fun selectLocation(location: LocationEntity) {
        val saved = getLocation(location.id)
        insertLocations(
            listOf(
                location.copy(
                    isSelected = false,
                    isFavorite = saved?.isFavorite ?: location.isFavorite,
                    favoriteOrder = saved?.favoriteOrder ?: location.favoriteOrder,
                ),
            ),
        )
        setSelectedLocation(location.id)
    }

    @Query("UPDATE locations SET isFavorite = :isFavorite, favoriteOrder = :favoriteOrder WHERE id = :locationId")
    suspend fun updateFavorite(
        locationId: Long,
        isFavorite: Boolean,
        favoriteOrder: Int,
    )

    @Query("UPDATE locations SET favoriteOrder = :favoriteOrder WHERE id = :locationId AND isFavorite = 1")
    suspend fun updateFavoriteOrder(
        locationId: Long,
        favoriteOrder: Int,
    )

    @Query("UPDATE locations SET admin1 = :admin1, population = :population WHERE id = :locationId")
    suspend fun updateLocationDetails(
        locationId: Long,
        admin1: String?,
        population: Long?,
    )

    @Query("SELECT * FROM hourly_forecasts WHERE locationId = :locationId ORDER BY forecastTime")
    suspend fun getHourlyForecast(locationId: Long): List<HourlyForecastEntity>

    @Query("DELETE FROM hourly_forecasts WHERE locationId = :locationId")
    suspend fun deleteHourlyForecast(locationId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyForecast(forecasts: List<HourlyForecastEntity>)

    @Transaction
    suspend fun replaceHourlyForecast(
        locationId: Long,
        forecasts: List<HourlyForecastEntity>,
    ) {
        deleteHourlyForecast(locationId)
        insertHourlyForecast(forecasts)
    }

    @Query("SELECT * FROM daily_forecasts WHERE locationId = :locationId ORDER BY date")
    suspend fun getDailyForecast(locationId: Long): List<DailyForecastEntity>

    @Query("DELETE FROM daily_forecasts WHERE locationId = :locationId")
    suspend fun deleteDailyForecast(locationId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyForecast(forecasts: List<DailyForecastEntity>)

    @Transaction
    suspend fun replaceDailyForecast(
        locationId: Long,
        forecasts: List<DailyForecastEntity>,
    ) {
        deleteDailyForecast(locationId)
        if (forecasts.isNotEmpty()) {
            insertDailyForecast(forecasts)
        }
    }

    @Query("SELECT * FROM air_quality WHERE locationId = :locationId ORDER BY forecastTime")
    suspend fun getAirQuality(locationId: Long): List<AirQualityEntity>

    @Query("DELETE FROM air_quality WHERE locationId = :locationId")
    suspend fun deleteAirQuality(locationId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAirQuality(points: List<AirQualityEntity>)

    @Transaction
    suspend fun replaceAirQuality(
        locationId: Long,
        points: List<AirQualityEntity>,
    ) {
        deleteAirQuality(locationId)
        if (points.isNotEmpty()) {
            insertAirQuality(points)
        }
    }
}

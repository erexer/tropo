package io.github.erexer.tropo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_forecast_cache WHERE id = 1")
    suspend fun getCachedForecast(): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(entity: WeatherEntity)
}
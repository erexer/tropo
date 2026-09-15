package io.github.erexer.tropo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.erexer.tropo.data.HourlyPoint

@Entity(tableName = "weather_forecast_cache")
data class WeatherEntity(
    @PrimaryKey val id: Int = 1,
    val timestamp: Long,
    val points: List<HourlyPoint>
)
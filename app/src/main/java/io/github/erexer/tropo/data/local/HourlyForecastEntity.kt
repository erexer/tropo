package io.github.erexer.tropo.data.local

import androidx.room.Entity

@Entity(tableName = "hourly_forecasts", primaryKeys = ["locationId", "forecastTime"])
data class HourlyForecastEntity(
    val locationId: Long,
    val forecastTime: String,
    val temperature: Float,
    val precipProb: Float,
    val precipInches: Float,
    val dewPoint: Float,
    val windSpeed: Float,
    val windDirectionDeg: Float,
    val uvIndex: Float,
    val fetchedAt: Long,
    val rainInches: Float? = null,
    val snowCentimeters: Float? = null,
    val weatherCode: Int? = null,
    val humidity: Float? = null,
    val feelsLike: Float? = null,
    val windGusts: Float? = null,
    val cloudCover: Float? = null,
    val freezingLevelMeters: Float? = null,
    val snowDepthMeters: Float? = null,
    val visibilityMeters: Float? = null,
)

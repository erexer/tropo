package io.github.erexer.tropo.data.local

import androidx.room.Entity

@Entity(tableName = "daily_forecasts", primaryKeys = ["locationId", "date"])
data class DailyForecastEntity(
    val locationId: Long,
    val date: String,
    val timezone: String,
    val temperatureMax: Float,
    val temperatureMin: Float,
    val precipitationSum: Float,
    val precipitationProbabilityMax: Float,
    val sunrise: String,
    val sunset: String,
    val weatherCode: Int,
    val fetchedAt: Long,
    val uvIndexMax: Float? = null,
    val daylightSeconds: Float? = null,
    val precipitationHours: Float? = null,
)

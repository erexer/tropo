package io.github.erexer.tropo.data.local

import androidx.room.Entity

@Entity(tableName = "air_quality", primaryKeys = ["locationId", "forecastTime"])
data class AirQualityEntity(
    val locationId: Long,
    val forecastTime: String,
    val timezone: String,
    val usAqi: Float,
    val pm25: Float?,
    val fetchedAt: Long,
)

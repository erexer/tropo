package io.github.erexer.tropo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey val locationId: Long,
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "0.0") val feelsLike: Double = 0.0,
    @ColumnInfo(defaultValue = "1") val isDay: Boolean = true,
    val windDirectionDeg: Double? = null,
    val windGusts: Double? = null,
    val cloudCover: Int? = null,
)

package io.github.erexer.tropo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey val locationId: Long,
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val updatedAt: Long
)
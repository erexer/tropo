package com.tropo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weather_cache",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["locationId"])]
)
data class WeatherEntity(
    @PrimaryKey val locationId: String,
    val currentTemperature: Double,
    val windSpeed: Double,
    val weatherCode: Int,
    val conditionDescription: String,
    val updatedAtTimestamp: Long
)
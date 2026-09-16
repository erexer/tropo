package io.github.erexer.tropo.data.repository

import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherEntity
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun getCurrentWeather(): WeatherEntity?
    suspend fun getSelectedLocation(): LocationEntity?
    suspend fun searchLocations(query: String): List<LocationEntity>
    suspend fun selectLocation(location: LocationEntity)
    fun isCelsius(): Flow<Boolean>
    suspend fun setCelsius(isCelsius: Boolean)
}
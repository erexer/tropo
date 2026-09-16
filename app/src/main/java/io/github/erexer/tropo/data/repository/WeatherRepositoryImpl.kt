package io.github.erexer.tropo.data.repository

import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherDao
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.preferences.UserPreferences
import io.github.erexer.tropo.data.remote.GeocodingApi
import io.github.erexer.tropo.data.remote.WeatherApiClient
import kotlinx.coroutines.flow.Flow

class WeatherRepositoryImpl(
    private val weatherDao: WeatherDao,
    private val weatherApiClient: WeatherApiClient,
    private val geocodingApi: GeocodingApi,
    private val userPreferences: UserPreferences
) : WeatherRepository {

    override suspend fun getCurrentWeather(): WeatherEntity? {
        val location = getSelectedLocation() ?: return null
        return try {
            val response = weatherApiClient.fetchWeather(location.latitude, location.longitude)
            val current = response.currentWeather ?: return weatherDao.getWeather(location.id)
            val entity = WeatherEntity(
                locationId = location.id,
                temperature = current.temperature,
                weatherCode = current.weathercode,
                humidity = 0,
                windSpeed = current.windspeed,
                updatedAt = System.currentTimeMillis()
            )
            weatherDao.insertWeather(entity)
            entity
        } catch (e: Exception) {
            weatherDao.getWeather(location.id)
        }
    }

    override suspend fun getSelectedLocation(): LocationEntity? {
        return weatherDao.getSelectedLocation() ?: LocationEntity(
            id = 2988507, name = "Paris", country = "France", latitude = 48.8534, longitude = 2.3488, isSelected = true
        ).also { weatherDao.insertLocations(listOf(it)) }
    }

    override suspend fun searchLocations(query: String): List<LocationEntity> {
        val response = geocodingApi.searchLocation(query)
        return response.results?.map {
            LocationEntity(id = it.id, name = it.name, country = it.country, latitude = it.latitude, longitude = it.longitude)
        } ?: emptyList()
    }

    override suspend fun selectLocation(location: LocationEntity) {
        weatherDao.insertLocations(listOf(location.copy(isSelected = true)))
        weatherDao.setSelectedLocation(location.id)
    }

    override fun isCelsius(): Flow<Boolean> = userPreferences.useCelsius

    override suspend fun setCelsius(isCelsius: Boolean) {
        userPreferences.setUseCelsius(isCelsius)
    }
}
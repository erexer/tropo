package io.github.erexer.tropo.data

import io.github.erexer.tropo.data.local.WeatherDao
import io.github.erexer.tropo.data.local.WeatherEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WeatherRepository(
    private val apiService: WeatherApiService,
    private val weatherDao: WeatherDao
) {
    fun getHourlyForecast(lat: Double, lon: Double): Flow<Result<List<HourlyPoint>>> = flow {
        val cachedEntity = weatherDao.getCachedForecast()
        if (cachedEntity != null) {
            emit(Result.success(cachedEntity.points))
        }

        try {
            val response = apiService.getHourlyForecast(lat, lon)
            val hourly = response.hourly
            val points = hourly.time.mapIndexed { index, timeStr ->
                HourlyPoint(
                    hourLabel = timeStr.substringAfter("T").take(5),
                    temperature = hourly.temperature.getOrElse(index) { 0f },
                    precipProb = hourly.precipProbability.getOrElse(index) { 0f },
                    precipInches = hourly.precipitation.getOrElse(index) { 0f },
                    dewPoint = hourly.dewPoint.getOrElse(index) { 0f },
                    windSpeed = hourly.windSpeed.getOrElse(index) { 0f },
                    windDirectionDeg = hourly.windDirection.getOrElse(index) { 0f },
                    uvIndex = hourly.uvIndex.getOrElse(index) { 0f }
                )
            }.take(24)

            weatherDao.insertForecast(
                WeatherEntity(timestamp = System.currentTimeMillis(), points = points)
            )

            emit(Result.success(points))
        } catch (e: Exception) {
            if (cachedEntity == null) {
                emit(Result.failure(e))
            }
        }
    }
}
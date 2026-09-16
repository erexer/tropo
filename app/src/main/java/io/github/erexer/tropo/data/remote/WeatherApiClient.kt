package com.tropo.data.remote

import com.tropo.data.local.WeatherEntity
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current_weather") val currentWeather: CurrentWeatherDto
) {
    fun toEntity(locationId: String): WeatherEntity = WeatherEntity(
        locationId = locationId,
        currentTemperature = currentWeather.temperature,
        windSpeed = currentWeather.windSpeed,
        weatherCode = currentWeather.weatherCode,
        conditionDescription = parseWmoCode(currentWeather.weatherCode),
        updatedAtTimestamp = System.currentTimeMillis()
    )
}

@Serializable
data class CurrentWeatherDto(
    val temperature: Double,
    @SerialName("windspeed") val windSpeed: Double,
    @SerialName("weathercode") val weatherCode: Int,
    @SerialName("is_day") val isDay: Int,
    val time: String
)

private fun parseWmoCode(code: Int): String = when (code) {
    0 -> "Clear Sky"
    1, 2, 3 -> "Partly Cloudy"
    45, 48 -> "Foggy"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rain"
    71, 73, 75 -> "Snow"
    80, 81, 82 -> "Rain Showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Unknown"
}

class WeatherApiClient(
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {
    suspend fun getForecast(latitude: Double, longitude: Double): OpenMeteoResponse {
        return httpClient.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current_weather", true)
        }.body()
    }
}
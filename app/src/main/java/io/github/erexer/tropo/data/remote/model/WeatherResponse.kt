package io.github.erexer.tropo.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    @SerialName("current") val currentWeather: CurrentWeather? = null,
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m")
    val temperature: Double,
    @SerialName("relative_humidity_2m")
    val humidity: Int,
    @SerialName("apparent_temperature")
    val feelsLike: Double,
    @SerialName("wind_speed_10m")
    val windspeed: Double,
    @SerialName("weather_code")
    val weathercode: Int,
    @SerialName("is_day")
    val isDay: Int,
    val time: String,
    @SerialName("wind_direction_10m") val windDirection: Double? = null,
    @SerialName("wind_gusts_10m") val windGusts: Double? = null,
    @SerialName("cloud_cover") val cloudCover: Int? = null,
)

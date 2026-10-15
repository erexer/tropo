package io.github.erexer.tropo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HourlyData(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Float>,
    @SerialName("precipitation_probability") val precipProbability: List<Float>,
    @SerialName("precipitation") val precipitation: List<Float>,
    @SerialName("dew_point_2m") val dewPoint: List<Float>,
    @SerialName("wind_speed_10m") val windSpeed: List<Float>,
    @SerialName("wind_direction_10m") val windDirection: List<Float>,
    @SerialName("uv_index") val uvIndex: List<Float>,
    val rain: List<Float>? = null,
    val showers: List<Float>? = null,
    val snowfall: List<Float>? = null,
    @SerialName("weather_code") val weatherCode: List<Int>? = null,
    @SerialName("relative_humidity_2m") val humidity: List<Float?>? = null,
    @SerialName("apparent_temperature") val apparentTemperature: List<Float?>? = null,
    @SerialName("wind_gusts_10m") val windGusts: List<Float?>? = null,
    @SerialName("cloud_cover") val cloudCover: List<Float?>? = null,
    @SerialName("freezing_level_height") val freezingLevel: List<Float?>? = null,
    @SerialName("snow_depth") val snowDepth: List<Float?>? = null,
    val visibility: List<Float?>? = null,
)

@Serializable
data class HourlyForecastResponse(
    val hourly: HourlyData,
    val daily: DailyData? = null,
    val timezone: String? = null,
    @SerialName("hourly_units") val hourlyUnits: Map<String, String> = emptyMap(),
)

@Serializable
data class DailyData(
    val time: List<String>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Float>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Float>,
    @SerialName("precipitation_sum") val precipitationSum: List<Float>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Float>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("uv_index_max") val uvIndexMax: List<Float?>? = null,
    @SerialName("daylight_duration") val daylightDuration: List<Float?>? = null,
    @SerialName("precipitation_hours") val precipitationHours: List<Float?>? = null,
)

@Serializable
data class HourlyPoint(
    val forecastTime: String,
    val hourLabel: String,
    val temperature: Float,
    val precipProb: Float,
    val precipInches: Float,
    val dewPoint: Float,
    val windSpeed: Float,
    val windDirectionDeg: Float,
    val uvIndex: Float,
    val rainInches: Float? = null,
    val snowCentimeters: Float? = null,
    val weatherCode: Int? = null,
    val humidity: Float? = null,
    val feelsLike: Float? = null,
    val windGusts: Float? = null,
    val cloudCover: Float? = null,
    val freezingLevelMeters: Float? = null,
    val snowDepthMeters: Float? = null,
    val visibilityMeters: Float? = null,
)

data class DailyForecast(
    val date: String,
    val temperatureMax: Float,
    val temperatureMin: Float,
    val precipitationSum: Float,
    val precipitationProbabilityMax: Float,
    val sunrise: String,
    val sunset: String,
    val weatherCode: Int,
    val uvIndexMax: Float? = null,
    val daylightSeconds: Float? = null,
    val precipitationHours: Float? = null,
)

data class ForecastSnapshot(
    val hourly: List<HourlyPoint>,
    val daily: List<DailyForecast>,
    val timezone: String,
    val fetchedAt: Long,
    val isCached: Boolean,
)

@Serializable
data class AirQualityResponse(
    val hourly: AirQualityHourlyData,
    val timezone: String? = null,
)

@Serializable
data class AirQualityHourlyData(
    val time: List<String>,
    @SerialName("us_aqi") val usAqi: List<Float>,
    @SerialName("pm2_5") val pm25: List<Float>? = null,
)

data class AirQualityPoint(
    val forecastTime: String,
    val hourLabel: String,
    val usAqi: Float,
    val pm25: Float?,
)

data class AirQualitySnapshot(
    val points: List<AirQualityPoint>,
    val timezone: String,
    val fetchedAt: Long,
    val isCached: Boolean,
)

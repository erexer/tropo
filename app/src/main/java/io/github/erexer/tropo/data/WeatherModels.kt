package io.github.erexer.tropo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val hourly: HourlyData
)

@Serializable
data class HourlyData(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Float>,
    @SerialName("precipitation_probability") val precipProbability: List<Float>,
    @SerialName("precipitation") val precipitation: List<Float>,
    @SerialName("dew_point_2m") val dewPoint: List<Float>,
    @SerialName("wind_speed_10m") val windSpeed: List<Float>,
    @SerialName("wind_direction_10m") val windDirection: List<Float>,
    @SerialName("uv_index") val uvIndex: List<Float>
)

@Serializable
data class HourlyPoint(
    val hourLabel: String,
    val temperature: Float,
    val precipProb: Float,
    val precipInches: Float,
    val dewPoint: Float,
    val windSpeed: Float,
    val windDirectionDeg: Float,
    val uvIndex: Float
)
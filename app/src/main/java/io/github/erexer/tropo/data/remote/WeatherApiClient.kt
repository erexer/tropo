package io.github.erexer.tropo.data.remote

import io.github.erexer.tropo.data.HourlyForecastResponse
import io.github.erexer.tropo.data.remote.model.WeatherResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class WeatherApiClient(
    private val client: HttpClient,
) {
    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
    ): WeatherResponse =
        client
            .get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("current", CURRENT_VARIABLES)
                parameter("temperature_unit", "celsius")
                parameter("wind_speed_unit", "kmh")
            }.body()

    suspend fun fetchHourlyForecast(
        latitude: Double,
        longitude: Double,
        forecastDays: Int = DEFAULT_FORECAST_DAYS,
    ): HourlyForecastResponse =
        client
            .get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("hourly", HOURLY_VARIABLES)
                parameter("daily", DAILY_VARIABLES)
                parameter("temperature_unit", "fahrenheit")
                parameter("wind_speed_unit", "mph")
                parameter("precipitation_unit", "inch")
                parameter("timezone", "auto")
                parameter("forecast_days", forecastDays)
            }.body()

    private companion object {
        const val DEFAULT_FORECAST_DAYS = 3
        const val CURRENT_VARIABLES =
            "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code," +
                "wind_speed_10m,wind_direction_10m,is_day,wind_gusts_10m,cloud_cover"
        const val HOURLY_VARIABLES =
            "temperature_2m,precipitation_probability,precipitation,dew_point_2m," +
                "wind_speed_10m,wind_direction_10m,uv_index,rain,showers,snowfall,weather_code,relative_humidity_2m," +
                "apparent_temperature,wind_gusts_10m,cloud_cover,freezing_level_height,snow_depth,visibility"
        const val DAILY_VARIABLES =
            "temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,sunrise,sunset," +
                "weather_code,uv_index_max,daylight_duration,precipitation_hours"
    }
}

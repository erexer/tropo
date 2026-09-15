package io.github.erexer.tropo.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getHourlyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourlyVars: String = "temperature_2m,precipitation_probability,precipitation,dew_point_2m,wind_speed_10m,wind_direction_10m,uv_index",
        @Query("temperature_unit") tempUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windUnit: String = "mph",
        @Query("precipitation_unit") precipUnit: String = "inch",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 2
    ): OpenMeteoResponse

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun create(): WeatherApiService {
            return Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(WeatherApiService::class.java)
        }
    }
}
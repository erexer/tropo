package io.github.erexer.tropo.data.remote

import io.github.erexer.tropo.data.AirQualityResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class AirQualityApiClient(
    private val client: HttpClient,
) {
    suspend fun fetch(
        latitude: Double,
        longitude: Double,
    ): AirQualityResponse =
        client
            .get("https://air-quality-api.open-meteo.com/v1/air-quality") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("hourly", HOURLY_VARIABLES)
                parameter("timezone", "auto")
                parameter("forecast_days", FORECAST_DAYS)
            }.body()

    private companion object {
        const val FORECAST_DAYS = 3
        const val HOURLY_VARIABLES = "us_aqi,pm2_5"
    }
}

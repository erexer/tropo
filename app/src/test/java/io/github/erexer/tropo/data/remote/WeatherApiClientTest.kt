package io.github.erexer.tropo.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherApiClientTest {
    @Test
    fun fetchWeatherReturnsParsedResponse() =
        runBlocking {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = """{"current":{"temperature_2m":18.5,"relative_humidity_2m":60,"apparent_temperature":18.0,"wind_speed_10m":12.0,"weather_code":1,"is_day":1,"time":"2026-09-16T12:00"}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }

            val apiClient = WeatherApiClient(client)
            val response = apiClient.fetchWeather(48.85, 2.35)

            assertEquals(18.5, response.currentWeather?.temperature ?: 0.0, 0.01)
            assertEquals(1, response.currentWeather?.weathercode)
            assertEquals(60, response.currentWeather?.humidity)
        }

    @Test
    fun fetchHourlyForecastRequestsExpectedUnitsAndParsesValues() =
        runBlocking {
            var requestedTemperatureUnit: String? = null
            var requestedWindSpeedUnit: String? = null
            var requestedForecastDays: String? = null
            val mockEngine =
                MockEngine { request ->
                    requestedTemperatureUnit = request.url.parameters["temperature_unit"]
                    requestedWindSpeedUnit = request.url.parameters["wind_speed_unit"]
                    requestedForecastDays = request.url.parameters["forecast_days"]
                    respond(
                        content = """{"timezone":"America/Los_Angeles","hourly":{"time":["2026-10-01T12:00"],"temperature_2m":[68.0],"precipitation_probability":[20],"precipitation":[0.1],"dew_point_2m":[55.0],"wind_speed_10m":[5.0],"wind_direction_10m":[180],"uv_index":[4.0]},"daily":{"time":["2026-10-01"],"temperature_2m_max":[72.0],"temperature_2m_min":[55.0],"precipitation_sum":[0.2],"precipitation_probability_max":[40],"sunrise":["2026-10-01T06:30"],"sunset":["2026-10-01T18:10"],"weather_code":[2]}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }

            val response = WeatherApiClient(client).fetchHourlyForecast(47.6, -122.3, forecastDays = 10)

            assertEquals("fahrenheit", requestedTemperatureUnit)
            assertEquals("mph", requestedWindSpeedUnit)
            assertEquals("10", requestedForecastDays)
            assertTrue(requestedTemperatureUnit != null)
            assertEquals(68f, response.hourly.temperature.single(), 0.01f)
            assertEquals("America/Los_Angeles", response.timezone)
            assertEquals(72f, response.daily?.temperatureMax?.single())
            client.close()
        }
}

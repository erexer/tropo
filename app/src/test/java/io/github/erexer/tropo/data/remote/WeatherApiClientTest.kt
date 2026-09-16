package com.tropo.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeatherApiClientTest {

    @Test
    fun `getForecast parses valid JSON response correctly`() = runTest {
        val mockJsonResponse = """
            {
              "latitude": 47.6062,
              "longitude": -122.3321,
              "current_weather": {
                "temperature": 18.5,
                "windspeed": 12.3,
                "winddirection": 180,
                "weathercode": 61,
                "is_day": 1,
                "time": "2026-09-16T12:00"
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/v1/forecast", request.url.encodedPath)
            respond(
                content = mockJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val apiClient = WeatherApiClient(httpClient)
        val result = apiClient.getForecast(47.6062, -122.3321)

        assertNotNull(result)
        assertEquals(18.5, result.currentWeather.temperature, 0.01)
        assertEquals(61, result.currentWeather.weatherCode)
    }
}
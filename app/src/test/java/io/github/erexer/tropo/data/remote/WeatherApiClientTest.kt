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
import org.junit.Test

class WeatherApiClientTest {

    @Test
    fun fetchWeatherReturnsParsedResponse() = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"current_weather":{"temperature":18.5,"windspeed":12.0,"weathercode":1,"time":"2026-09-16T12:00"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val apiClient = WeatherApiClient(client)
        val response = apiClient.fetchWeather(48.85, 2.35)

        assertEquals(18.5, response.currentWeather?.temperature ?: 0.0, 0.01)
        assertEquals(1, response.currentWeather?.weathercode)
    }
}
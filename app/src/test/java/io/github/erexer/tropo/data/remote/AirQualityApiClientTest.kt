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

class AirQualityApiClientTest {
    @Test
    fun fetchRequestsUsAqiAndParsesTimezone() =
        runBlocking {
            var requestedVariables: String? = null
            val client =
                HttpClient(
                    MockEngine { request ->
                        requestedVariables = request.url.parameters["hourly"]
                        respond(
                            content = """{"timezone":"America/New_York","hourly":{"time":["2026-10-05T12:00"],"us_aqi":[42],"pm2_5":[8.5]}}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }

            try {
                val response = AirQualityApiClient(client).fetch(42.36, -71.06)

                assertEquals("us_aqi,pm2_5", requestedVariables)
                assertEquals("America/New_York", response.timezone)
                assertEquals(42f, response.hourly.usAqi.single())
                assertEquals(8.5f, response.hourly.pm25?.single())
            } finally {
                client.close()
            }
        }
}

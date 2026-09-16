package io.github.erexer.tropo.data.remote

import io.github.erexer.tropo.data.remote.model.GeocodingResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class GeocodingApi(private val client: HttpClient) {
    suspend fun searchLocation(query: String): GeocodingResponse {
        return client.get("https://geocoding-api.open-meteo.com/v1/search") {
            parameter("name", query)
            parameter("count", 10)
            parameter("language", "en")
            parameter("format", "json")
        }.body()
    }
}
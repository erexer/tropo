package com.tropo.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    @SerialName("results") val results: List<GeocodingLocation>? = null
)

@Serializable
data class GeocodingLocation(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val admin1: String? = null
)

class GeocodingApiClient(private val httpClient: HttpClient) {

    suspend fun searchLocations(query: String, count: Int = 10): List<GeocodingLocation> {
        if (query.isBlank()) return emptyList()
        return try {
            val response: GeocodingResponse = httpClient.get("https://geocoding-api.open-meteo.com/v1/search") {
                parameter("name", query)
                parameter("count", count)
                parameter("language", "en")
                parameter("format", "json")
            }.body()
            response.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
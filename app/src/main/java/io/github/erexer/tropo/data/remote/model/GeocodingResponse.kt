package io.github.erexer.tropo.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val id: Long,
    val name: String,
    val country: String = "",
    val latitude: Double,
    val longitude: Double
)
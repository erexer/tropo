package io.github.erexer.tropo.data

object AirQualityCategory {
    fun label(usAqi: Float): String =
        when {
            usAqi <= 50f -> "Good"
            usAqi <= 100f -> "Moderate"
            usAqi <= 150f -> "Unhealthy for sensitive groups"
            usAqi <= 200f -> "Unhealthy"
            usAqi <= 300f -> "Very unhealthy"
            else -> "Hazardous"
        }
}

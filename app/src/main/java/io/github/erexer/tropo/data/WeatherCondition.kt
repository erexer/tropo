package io.github.erexer.tropo.data

object WeatherCondition {
    private const val LIGHT_FREEZING_DRIZZLE = 56
    private const val DENSE_FREEZING_DRIZZLE = 57

    enum class Light { DAY, NIGHT }

    enum class Kind { CLEAR, CLOUDY, OVERCAST, FOG, RAIN, SNOW, STORM, UNKNOWN }

    fun kind(code: Int): Kind =
        when (label(code)) {
            "Clear" -> Kind.CLEAR
            "Partly cloudy" -> Kind.CLOUDY
            "Overcast" -> Kind.OVERCAST
            "Fog" -> Kind.FOG
            "Drizzle", "Freezing drizzle", "Rain", "Freezing rain" -> Kind.RAIN
            "Snow" -> Kind.SNOW
            "Thunderstorm" -> Kind.STORM
            else -> Kind.UNKNOWN
        }

    fun label(code: Int): String =
        when (code) {
            0 -> "Clear"
            1, 2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            LIGHT_FREEZING_DRIZZLE, DENSE_FREEZING_DRIZZLE -> "Freezing drizzle"
            61, 63, 65, 80, 81, 82 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75, 77, 85, 86 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown"
        }
}

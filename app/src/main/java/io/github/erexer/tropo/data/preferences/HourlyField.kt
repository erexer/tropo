package io.github.erexer.tropo.data.preferences

enum class HourlyField(
    val label: String,
) {
    TEMPERATURE("Temperature"),
    PRECIPITATION_AMOUNT("Precipitation amount"),
    PRECIPITATION_CHANCE("Precipitation chance"),
    WIND("Wind speed and direction"),
    HUMIDITY("Humidity"),
    DEW_POINT("Dew point"),
    UV("UV index"),
    AQI("Air quality"),
    FEELS_LIKE("Feels like"),
    WIND_GUSTS("Wind gusts"),
    CLOUD_COVER("Cloud cover"),
    FREEZING_LEVEL("Freezing level"),
    SNOW_DEPTH("Snow depth"),
    VISIBILITY("Visibility"),
    ;

    companion object {
        val defaults: Set<HourlyField> = setOf(TEMPERATURE, PRECIPITATION_AMOUNT, PRECIPITATION_CHANCE, WIND)

        fun decode(names: Set<String>?): Set<HourlyField> {
            if (names == null) {
                return defaults
            }
            return entries.filter { it.name in names }.toSet()
        }
    }
}

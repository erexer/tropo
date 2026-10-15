package io.github.erexer.tropo.data

enum class PrecipitationType {
    NONE,
    RAIN,
    SNOW,
    MIXED,
    FREEZING,
    UNKNOWN,
    ;

    companion object {
        private val snowCodes = setOf(71, 73, 75, 77, 85, 86)

        @Suppress("MagicNumber")
        private val freezingCodes = setOf(56, 57, 66, 67)
        private val rainCodes = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)

        fun of(point: HourlyPoint): PrecipitationType {
            if (point.precipInches <= 0f) {
                return NONE
            }
            val rain = (point.rainInches ?: 0f) > 0f
            val snow = (point.snowCentimeters ?: 0f) > 0f
            return when {
                point.weatherCode in freezingCodes -> FREEZING
                rain && snow -> MIXED
                snow -> SNOW
                rain -> RAIN
                point.weatherCode in snowCodes -> SNOW
                point.weatherCode in rainCodes -> RAIN
                else -> UNKNOWN
            }
        }

        fun expected(point: HourlyPoint): PrecipitationType {
            val measured = of(point)
            if (measured != NONE && measured != UNKNOWN) return measured
            return fromCode(point.weatherCode)
        }

        fun fromCode(code: Int?): PrecipitationType =
            when (code) {
                in freezingCodes -> FREEZING
                in snowCodes -> SNOW
                in rainCodes -> RAIN
                else -> UNKNOWN
            }
    }
}

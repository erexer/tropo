package io.github.erexer.tropo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PrecipitationTypeTest {
    private val point = HourlyPoint("2026-10-06T12:00", "12", 20f, 80f, 0.1f, 15f, 5f, 0f, 0f)

    @Test
    fun classifiesMeasuredComponentsWithoutTemperatureGuessing() {
        assertEquals(PrecipitationType.RAIN, PrecipitationType.of(point.copy(rainInches = 0.1f)))
        assertEquals(PrecipitationType.SNOW, PrecipitationType.of(point.copy(snowCentimeters = 1f)))
        assertEquals(PrecipitationType.MIXED, PrecipitationType.of(point.copy(rainInches = 0.1f, snowCentimeters = 1f)))
        assertEquals(PrecipitationType.UNKNOWN, PrecipitationType.of(point))
        assertEquals(PrecipitationType.NONE, PrecipitationType.of(point.copy(precipInches = 0f)))
    }

    @Test
    fun fallsBackToExplicitWeatherCode() {
        assertEquals(PrecipitationType.SNOW, PrecipitationType.of(point.copy(weatherCode = 73)))
        assertEquals(PrecipitationType.RAIN, PrecipitationType.of(point.copy(weatherCode = 61)))
        assertEquals(
            PrecipitationType.FREEZING,
            PrecipitationType.of(point.copy(weatherCode = 66, rainInches = 0.1f)),
        )
        assertEquals(
            PrecipitationType.SNOW,
            PrecipitationType.expected(point.copy(weatherCode = 73, precipInches = 0f)),
        )
    }
}

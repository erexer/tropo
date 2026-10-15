package io.github.erexer.tropo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureConverterTest {
    @Test
    fun convertsTemperaturesBetweenUnits() {
        assertEquals(20f, TemperatureConverter.fahrenheitToCelsius(68f), 0.01f)
        assertEquals(32.0, TemperatureConverter.celsiusToFahrenheit(0.0), 0.01)
        assertEquals(16.09f, WindSpeedConverter.milesPerHourToKilometersPerHour(10f), 0.01f)
        assertEquals(10.0, WindSpeedConverter.kilometersPerHourToMilesPerHour(16.0934), 0.01)
    }

    @Test
    fun mapsAqiValuesToUsCategories() {
        assertEquals("Good", AirQualityCategory.label(35f))
        assertEquals("Moderate", AirQualityCategory.label(75f))
        assertEquals("Unhealthy for sensitive groups", AirQualityCategory.label(125f))
        assertEquals("Hazardous", AirQualityCategory.label(325f))
    }
}

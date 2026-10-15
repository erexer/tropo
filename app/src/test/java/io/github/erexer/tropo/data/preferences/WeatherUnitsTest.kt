package io.github.erexer.tropo.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherUnitsTest {
    @Test
    fun mixedUnitsAreIndependent() {
        val units = WeatherUnits(TemperatureUnit.FAHRENHEIT, WindUnit.MS, RainUnit.MM)
        assertEquals(68.0, units.currentTemperature(20.0), 0.001)
        assertEquals(68f, units.forecastTemperature(68f), 0.001f)
        assertEquals(10.0, units.currentWind(36.0), 0.001)
        assertEquals(4.4704f, units.forecastWind(10f), 0.001f)
        assertEquals(25.4f, units.precipitation(1f), 0.001f)
        assertEquals(5.08f, units.snowfall(5.08f), 0.001f)
        assertEquals("cm", units.snowfallLabel)
    }

    @Test
    fun allCombinationsKeepMeaning() {
        val combinations =
            TemperatureUnit.entries.flatMap { temperature ->
                WindUnit.entries.flatMap { wind ->
                    RainUnit.entries.map { rain -> WeatherUnits(temperature, wind, rain) }
                }
            }
        combinations.forEach { units ->
            val expectedTemperature = if (units.temperature == TemperatureUnit.CELSIUS) 20f else 68f
            val expectedWind =
                when (units.wind) {
                    WindUnit.KMH -> 16.09344f
                    WindUnit.MPH -> 10f
                    WindUnit.MS -> 4.4704f
                }
            assertEquals(expectedTemperature, units.forecastTemperature(68f), 0.001f)
            assertEquals(expectedWind, units.forecastWind(10f), 0.001f)
            assertEquals(if (units.rain == RainUnit.MM) 25.4f else 1f, units.precipitation(1f), 0.001f)
        }
    }
}

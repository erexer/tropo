package io.github.erexer.tropo.data.preferences

import io.github.erexer.tropo.data.TemperatureConverter
import java.util.Locale
import kotlin.math.roundToInt

enum class TemperatureUnit(
    val label: String,
) {
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
}

enum class WindUnit(
    val label: String,
) {
    KMH("km/h"),
    MPH("mph"),
    MS("m/s"),
}

enum class RainUnit(
    val label: String,
) {
    MM("mm"),
    INCH("in"),
}

data class WeatherUnits(
    val temperature: TemperatureUnit = TemperatureUnit.CELSIUS,
    val wind: WindUnit = WindUnit.KMH,
    val rain: RainUnit = RainUnit.MM,
) {
    fun currentTemperature(celsius: Double): Double =
        when (temperature) {
            TemperatureUnit.CELSIUS -> celsius
            TemperatureUnit.FAHRENHEIT -> TemperatureConverter.celsiusToFahrenheit(celsius)
        }

    fun forecastTemperature(fahrenheit: Float): Float =
        when (temperature) {
            TemperatureUnit.CELSIUS -> TemperatureConverter.fahrenheitToCelsius(fahrenheit)
            TemperatureUnit.FAHRENHEIT -> fahrenheit
        }

    fun currentWind(kmh: Double): Double =
        when (wind) {
            WindUnit.KMH -> kmh
            WindUnit.MPH -> kmh / KILOMETERS_PER_MILE
            WindUnit.MS -> kmh / KMH_PER_METER_SECOND
        }

    fun forecastWind(mph: Float): Float =
        when (wind) {
            WindUnit.KMH -> mph * KILOMETERS_PER_MILE.toFloat()
            WindUnit.MPH -> mph
            WindUnit.MS -> mph * METERS_SECOND_PER_MPH
        }

    fun forecastWindText(mph: Float): String = windText(forecastWind(mph).toDouble())

    fun currentWindText(kmh: Double): String = windText(currentWind(kmh))

    private fun windText(value: Double): String {
        if (!value.isFinite()) {
            return "–"
        }
        val number =
            if (wind == WindUnit.MS) {
                "%.1f".format(Locale.getDefault(), value)
            } else {
                value.roundToInt().toString()
            }
        return "$number ${wind.label}"
    }

    fun precipitation(inches: Float): Float =
        when (rain) {
            RainUnit.MM -> inches * MILLIMETERS_PER_INCH
            RainUnit.INCH -> inches
        }

    fun snowfall(centimeters: Float): Float =
        when (rain) {
            RainUnit.MM -> centimeters
            RainUnit.INCH -> centimeters / CENTIMETERS_PER_INCH
        }

    val snowfallLabel: String get() = if (rain == RainUnit.MM) "cm" else "in"

    private companion object {
        const val KILOMETERS_PER_MILE = 1.609344
        const val KMH_PER_METER_SECOND = 3.6
        const val METERS_SECOND_PER_MPH = 0.44704f
        const val MILLIMETERS_PER_INCH = 25.4f
        const val CENTIMETERS_PER_INCH = 2.54f
    }
}

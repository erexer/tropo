package io.github.erexer.tropo.data.preferences

import java.util.Locale
import kotlin.math.roundToInt

// Lengths follow the rain unit: mm selects metric, inches selects imperial
private const val METERS_PER_KILOMETER = 1000f
private const val METERS_PER_MILE = 1609.344f
private const val METERS_PER_FOOT = 0.3048f
private const val METERS_PER_INCH = 0.0254f
private const val CENTIMETERS_PER_METER = 100f
private const val ELEVATION_ROUNDING = 10
private const val DECIMAL_LIMIT = 10f

private val WeatherUnits.isMetricLength: Boolean get() = rain == RainUnit.MM

fun WeatherUnits.visibilityText(meters: Float): String =
    if (isMetricLength) {
        decimalBelowTen(meters / METERS_PER_KILOMETER) + " km"
    } else {
        decimalBelowTen(meters / METERS_PER_MILE) + " mi"
    }

fun WeatherUnits.elevationText(meters: Float): String {
    val value = if (isMetricLength) meters else meters / METERS_PER_FOOT
    val rounded = (value / ELEVATION_ROUNDING).roundToInt() * ELEVATION_ROUNDING
    return "%,d %s".format(Locale.getDefault(), rounded, if (isMetricLength) "m" else "ft")
}

fun WeatherUnits.snowDepthText(meters: Float): String =
    if (isMetricLength) {
        "${(meters * CENTIMETERS_PER_METER).roundToInt()} cm"
    } else {
        "%.1f in".format(Locale.getDefault(), meters / METERS_PER_INCH)
    }

private fun decimalBelowTen(value: Float): String =
    if (value < DECIMAL_LIMIT) "%.1f".format(Locale.getDefault(), value) else value.roundToInt().toString()

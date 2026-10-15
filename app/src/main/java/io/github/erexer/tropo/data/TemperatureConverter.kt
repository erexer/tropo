package io.github.erexer.tropo.data

object TemperatureConverter {
    private const val FAHRENHEIT_OFFSET = 32f
    private const val CELSIUS_SCALE_NUMERATOR = 5f
    private const val CELSIUS_SCALE_DENOMINATOR = 9f
    private const val FAHRENHEIT_SCALE_NUMERATOR = 9.0
    private const val FAHRENHEIT_SCALE_DENOMINATOR = 5.0

    fun fahrenheitToCelsius(temperature: Float): Float =
        (temperature - FAHRENHEIT_OFFSET) * CELSIUS_SCALE_NUMERATOR / CELSIUS_SCALE_DENOMINATOR

    fun celsiusToFahrenheit(temperature: Double): Double =
        temperature * FAHRENHEIT_SCALE_NUMERATOR / FAHRENHEIT_SCALE_DENOMINATOR + FAHRENHEIT_OFFSET
}

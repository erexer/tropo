package io.github.erexer.tropo.data

object LengthConverter {
    private const val METERS_PER_FOOT = 0.3048f
    private const val METERS_PER_INCH = 0.0254f
    private const val CENTIMETERS_PER_METER = 100f

    // Open-Meteo switches length units with the request's unit system, so normalize via hourly_units
    fun toMeters(
        value: Float?,
        unit: String?,
    ): Float? =
        value?.takeIf(Float::isFinite)?.let {
            when (unit?.lowercase()) {
                "ft" -> it * METERS_PER_FOOT
                "inch", "in" -> it * METERS_PER_INCH
                "cm" -> it / CENTIMETERS_PER_METER
                else -> it
            }
        }
}

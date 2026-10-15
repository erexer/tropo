package io.github.erexer.tropo.data

object WindSpeedConverter {
    private const val KILOMETERS_PER_MILE = 1.60934f

    fun milesPerHourToKilometersPerHour(speed: Float): Float = speed * KILOMETERS_PER_MILE

    fun kilometersPerHourToMilesPerHour(speed: Double): Double = speed / KILOMETERS_PER_MILE
}

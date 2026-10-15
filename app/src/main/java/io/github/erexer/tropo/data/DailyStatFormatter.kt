package io.github.erexer.tropo.data

import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt

object DailyStatFormatter {
    enum class UvRisk(
        val label: String,
    ) {
        LOW("Low"),
        MODERATE("Moderate"),
        HIGH("High"),
        VERY_HIGH("Very high"),
        EXTREME("Extreme"),
    }

    private const val HOURS_PER_DAY = 24
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
    private const val LOW_MAX = 3f
    private const val MODERATE_MAX = 6f
    private const val HIGH_MAX = 8f
    private const val VERY_HIGH_MAX = 11f

    fun uvRisk(uv: Float): UvRisk =
        when {
            uv < LOW_MAX -> UvRisk.LOW
            uv < MODERATE_MAX -> UvRisk.MODERATE
            uv < HIGH_MAX -> UvRisk.HIGH
            uv < VERY_HIGH_MAX -> UvRisk.VERY_HIGH
            else -> UvRisk.EXTREME
        }

    fun uvCategory(uv: Float): String = uvRisk(uv).label

    fun uvMax(uv: Float): String {
        val high = uv.roundToInt()
        return "UV high of $high (${uvCategory(high.toFloat()).lowercase(Locale.ROOT)})"
    }

    fun uvHigh(
        forecast: DailyForecast,
        points: List<HourlyPoint>,
    ): String? {
        val readings = uvReadings(forecast, points)
        val high = uvValue(forecast, points) ?: return null
        val peak = readings.filterValues { it.roundToInt() == high.roundToInt() }.keys.sorted()
        val window =
            if (readings.size == HOURS_PER_DAY && high.roundToInt() > 0) peakWindows(peak) else ""
        return uvMax(high) + if (window.isEmpty()) "" else " at $window"
    }

    fun uvValue(
        forecast: DailyForecast,
        points: List<HourlyPoint>,
    ): Float? {
        val readings = uvReadings(forecast, points)
        return forecast.uvIndexMax?.takeIf { it.isFinite() && it >= 0f }
            ?: readings.values.maxOrNull()?.takeIf { readings.size == HOURS_PER_DAY }
    }

    private fun uvReadings(
        forecast: DailyForecast,
        points: List<HourlyPoint>,
    ): Map<Int, Float> =
        points
            .mapNotNull { point ->
                val time = runCatching { LocalDateTime.parse(point.forecastTime) }.getOrNull()
                val matches = time?.toLocalDate()?.toString() == forecast.date
                val valid = point.uvIndex.isFinite() && point.uvIndex >= 0f
                if (time != null && matches && valid) {
                    time.hour to point.uvIndex
                } else {
                    null
                }
            }.toMap()

    private fun peakWindows(hours: List<Int>): String {
        val windows = mutableListOf<Pair<Int, Int>>()
        hours.forEach { hour ->
            val previous = windows.lastOrNull()
            if (previous != null && previous.second + 1 == hour) {
                windows[windows.lastIndex] = previous.first to hour
            } else {
                windows += hour to hour
            }
        }
        return windows.joinToString(", ") { (start, end) ->
            if (start == end) "%02d".format(Locale.ROOT, start) else "%02d-%02d".format(Locale.ROOT, start, end)
        }
    }

    fun daylight(seconds: Float): String {
        val totalMinutes = (seconds / SECONDS_PER_MINUTE).roundToInt()
        return "Daylight ${totalMinutes / MINUTES_PER_HOUR} h ${totalMinutes % MINUTES_PER_HOUR} m"
    }

    fun precipitationHours(hours: Float): String {
        val whole = hours == hours.roundToInt().toFloat()
        val value = if (whole) "${hours.roundToInt()}" else "%.1f".format(Locale.getDefault(), hours)
        return "Precip $value h"
    }
}

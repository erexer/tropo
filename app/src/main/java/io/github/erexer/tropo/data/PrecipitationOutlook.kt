package io.github.erexer.tropo.data

import io.github.erexer.tropo.data.preferences.WeatherUnits
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class PrecipitationOutlook(
    val time: LocalDateTime,
    val chance: Int,
    val inches: Float,
    val kind: Kind,
    val isNow: Boolean,
) {
    enum class Kind(
        val noun: String,
        val nowLabel: String,
        val type: PrecipitationType,
    ) {
        RAIN("rain", "Raining now", PrecipitationType.RAIN),
        SNOW("snow", "Snowing now", PrecipitationType.SNOW),
        MIXED("rain/snow", "Rain and snow now", PrecipitationType.MIXED),
        FREEZING("freezing precipitation", "Freezing precipitation now", PrecipitationType.FREEZING),
    }

    fun label(
        units: WeatherUnits,
        now: LocalDateTime,
    ): String {
        val amount = "%.2f %s".format(Locale.getDefault(), units.precipitation(inches), units.rain.label)
        return if (isNow) {
            "${kind.nowLabel} · $amount this hour"
        } else {
            val hour = "%02d".format(Locale.ROOT, time.hour)
            val day =
                when {
                    time.toLocalDate() == now.toLocalDate() -> ""
                    Duration.between(now, time) <= Duration.ofDays(1) -> "tomorrow "
                    else -> time.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())) + " "
                }
            val base = "$chance% chance of ${kind.noun} ${day}at $hour"
            if (inches > 0f) "$base · $amount" else base
        }
    }

    companion object {
        const val MIN_CHANCE = 10f
        const val NONE_THIS_WEEK = "No precipitation expected in the next week"
        private const val WINDOW_DAYS = 7L
        private const val HOURS_PER_DAY = 24L
        private const val MAX_CHANCE = 100f

        fun emptyLabel(
            points: List<HourlyPoint>,
            now: LocalDateTime,
        ): String {
            val currentHour = now.truncatedTo(ChronoUnit.HOURS)
            val knownHours =
                points
                    .filter { it.precipProb in 0f..MAX_CHANCE }
                    .mapNotNull { runCatching { LocalDateTime.parse(it.forecastTime) }.getOrNull() }
                    .toSet()
            val covered = (0..WINDOW_DAYS * HOURS_PER_DAY).all { currentHour.plusHours(it) in knownHours }
            return if (covered) NONE_THIS_WEEK else "No precipitation expected in available forecast"
        }

        fun find(
            points: List<HourlyPoint>,
            now: LocalDateTime,
        ): PrecipitationOutlook? {
            val end = now.plusDays(WINDOW_DAYS)
            val timed =
                points.mapNotNull { point ->
                    runCatching { LocalDateTime.parse(point.forecastTime) }.getOrNull()?.let { it to point }
                }
            val currentHour = now.truncatedTo(ChronoUnit.HOURS)
            timed
                .firstOrNull { (time, point) ->
                    time == currentHour && point.precipProb >= MIN_CHANCE && point.precipInches > 0f
                }?.let { (time, point) -> return of(time, point, isNow = true) }
            return timed
                .firstOrNull { (time, point) ->
                    time.isAfter(now) && !time.isAfter(end) && point.precipProb >= MIN_CHANCE
                }?.let { (time, point) -> of(time, point, isNow = false) }
        }

        private fun of(
            time: LocalDateTime,
            point: HourlyPoint,
            isNow: Boolean,
        ) = PrecipitationOutlook(time, point.precipProb.toInt(), point.precipInches, kindOf(point), isNow)

        private fun kindOf(point: HourlyPoint): Kind =
            when (PrecipitationType.expected(point)) {
                PrecipitationType.SNOW -> Kind.SNOW
                PrecipitationType.MIXED -> Kind.MIXED
                PrecipitationType.RAIN -> Kind.RAIN
                PrecipitationType.FREEZING -> Kind.FREEZING
                PrecipitationType.NONE, PrecipitationType.UNKNOWN ->
                    if (point.weatherCode?.let(WeatherCondition::kind) == WeatherCondition.Kind.SNOW) {
                        Kind.SNOW
                    } else {
                        Kind.RAIN
                    }
            }
    }
}

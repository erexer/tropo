package io.github.erexer.tropo.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object ForecastTimeline {
    fun today(
        days: List<DailyForecast>,
        timezone: String,
        now: Instant,
    ): DailyForecast? {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("UTC"))
        val date = now.atZone(zone).toLocalDate().toString()
        return days.firstOrNull { it.date == date }
    }

    fun isPast(
        time: String,
        timezone: String,
        now: Instant,
    ): Boolean {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("UTC"))
        val hour = now.atZone(zone).toLocalDateTime().truncatedTo(ChronoUnit.HOURS)
        return runCatching { LocalDateTime.parse(time).isBefore(hour) }.getOrDefault(false)
    }

    fun position(
        times: List<LocalDateTime>,
        time: LocalDateTime,
    ): Float {
        if (times.size < 2 || time <= times.first()) {
            return 0f
        }
        val clamped = minOf(time, times.last())
        val index = times.indexOfFirst { it >= clamped }.coerceAtLeast(1)
        val minutes = Duration.between(times[index - 1], times[index]).toMinutes().coerceAtLeast(1)
        return index - 1 + Duration.between(times[index - 1], clamped).toMinutes().toFloat() / minutes
    }

    fun nights(day: DailyForecast): List<Pair<LocalDateTime, LocalDateTime>> {
        val sunrise = runCatching { LocalDateTime.parse(day.sunrise) }.getOrNull()
        val sunset = runCatching { LocalDateTime.parse(day.sunset) }.getOrNull()
        val valid =
            sunrise != null &&
                sunset != null &&
                sunset.isAfter(sunrise) &&
                sunrise.toLocalDate() == sunset.toLocalDate()
        if (!valid) {
            return emptyList()
        }
        val midnight = requireNotNull(sunrise).toLocalDate().atStartOfDay()
        return listOf(midnight to sunrise, requireNotNull(sunset) to midnight.plusDays(1))
    }

    fun currentIndex(
        points: List<HourlyPoint>,
        timezone: String,
        now: Instant,
    ): Int {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("UTC"))
        val localNow = now.atZone(zone).toLocalDateTime().truncatedTo(ChronoUnit.HOURS)
        return points.indices.minByOrNull { index ->
            val time = runCatching { LocalDateTime.parse(points[index].forecastTime) }.getOrNull()
            time?.let { abs(Duration.between(it, localNow).toMinutes()) } ?: Long.MAX_VALUE
        } ?: 0
    }
}

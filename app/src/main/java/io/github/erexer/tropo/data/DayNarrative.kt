package io.github.erexer.tropo.data

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object DayNarrative {
    data class Segment(
        val label: String,
        val weatherCode: Int,
        val start: LocalDateTime,
        val hours: Int,
    )

    private const val MIN_SEGMENT_HOURS = 2
    private const val MAX_SEGMENTS = 3

    // Condition changes from the current hour to midnight; empty when nothing changes
    fun from(
        points: List<HourlyPoint>,
        now: LocalDateTime,
    ): List<Segment> {
        val currentHour = now.truncatedTo(ChronoUnit.HOURS)
        val hours =
            points
                .mapNotNull { point ->
                    val time = runCatching { LocalDateTime.parse(point.forecastTime) }.getOrNull()
                    val code = point.weatherCode
                    val restOfToday =
                        time?.takeIf { !it.isBefore(currentHour) && it.toLocalDate() == now.toLocalDate() }
                    if (restOfToday != null && code != null) {
                        Segment(WeatherCondition.label(code), code, restOfToday, 1)
                    } else {
                        null
                    }
                }.sortedBy { it.start }
        val merged = absorbShort(group(hours))
        if (merged.size < 2) return emptyList()
        val changes =
            merged
                .drop(1)
                .sortedWith(
                    compareByDescending<Segment> {
                        WeatherCondition.kind(it.weatherCode) == WeatherCondition.Kind.STORM
                    }.thenByDescending { isHazard(it) }
                        .thenBy { it.start },
                ).take(MAX_SEGMENTS - 1)
        return (listOf(merged.first()) + changes).sortedBy { it.start }
    }

    private fun group(hours: List<Segment>): List<Segment> =
        hours.fold(mutableListOf()) { segments, hour ->
            val last = segments.lastOrNull()
            if (last != null && last.label == hour.label && last.start.plusHours(last.hours.toLong()) == hour.start) {
                segments[segments.lastIndex] = last.copy(hours = last.hours + hour.hours)
            } else {
                segments += hour
            }
            segments
        }

    private fun absorbShort(segments: List<Segment>): List<Segment> {
        val kept = mutableListOf<Segment>()
        segments.forEach { segment ->
            val previous = kept.lastOrNull()
            if (previous != null && canAbsorb(previous, segment)) {
                kept[kept.lastIndex] = previous.copy(hours = previous.hours + segment.hours)
            } else {
                kept += segment
            }
        }
        return group(kept)
    }

    private fun canAbsorb(
        previous: Segment,
        segment: Segment,
    ): Boolean =
        segment.hours < MIN_SEGMENT_HOURS &&
            !isHazard(segment) &&
            !isHazard(previous) &&
            previous.start.plusHours(previous.hours.toLong()) == segment.start

    private fun isHazard(segment: Segment): Boolean =
        WeatherCondition.kind(segment.weatherCode) == WeatherCondition.Kind.STORM ||
            segment.label == "Freezing rain" ||
            segment.label == "Freezing drizzle"
}

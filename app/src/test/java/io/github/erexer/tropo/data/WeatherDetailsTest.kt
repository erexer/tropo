package io.github.erexer.tropo.data

import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.preferences.RainUnit
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.preferences.elevationText
import io.github.erexer.tropo.data.preferences.snowDepthText
import io.github.erexer.tropo.data.preferences.visibilityText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class WeatherDetailsTest {
    @Test
    fun weatherLabelsHandleClearStormAndUnknownCodes() {
        assertEquals("Clear", WeatherCondition.label(0))
        assertEquals("Thunderstorm", WeatherCondition.label(95))
        assertEquals("Unknown", WeatherCondition.label(999))
    }

    private fun hour(
        time: String,
        code: Int,
    ) = HourlyPoint(time, time.takeLast(5), 50f, 0f, 0f, 40f, 5f, 0f, 0f, weatherCode = code)

    @Test
    fun narrativeListsConditionChangesForRestOfToday() {
        val now = LocalDateTime.parse("2026-10-08T14:20")
        val points =
            listOf(
                hour("2026-10-08T13:00", 0),
                hour("2026-10-08T14:00", 2),
                hour("2026-10-08T15:00", 2),
                hour("2026-10-08T16:00", 0),
                hour("2026-10-08T17:00", 2),
                hour("2026-10-08T18:00", 45),
                hour("2026-10-08T19:00", 45),
                hour("2026-10-08T20:00", 61),
                hour("2026-10-08T21:00", 61),
                hour("2026-10-08T22:00", 3),
                hour("2026-10-08T23:00", 3),
                hour("2026-10-09T00:00", 71),
            )
        val segments = DayNarrative.from(points, now)
        assertEquals(listOf("Partly cloudy", "Fog", "Rain"), segments.map { it.label })
        assertEquals(18, segments[1].start.hour)
    }

    @Test
    fun narrativeEmptyWhenUnchanged() {
        val now = LocalDateTime.parse("2026-10-08T20:00")
        val points = listOf(hour("2026-10-08T20:00", 0), hour("2026-10-08T21:00", 0), hour("2026-10-08T22:00", 1))
        assertTrue(DayNarrative.from(points, now).isEmpty())
    }

    @Test
    fun narrativeTracksMidnight() {
        val before = LocalDateTime.parse("2026-10-08T22:00")
        val points =
            listOf(
                hour("2026-10-08T22:00", 0),
                hour("2026-10-08T23:00", 95),
                hour("2026-10-09T00:00", 0),
                hour("2026-10-09T01:00", 95),
            )
        assertTrue(DayNarrative.from(points, before).all { it.start.toLocalDate() == before.toLocalDate() })
        val midnight = before.plusHours(2)
        val nextDay = DayNarrative.from(points, midnight)
        assertEquals(listOf(0, 1), nextDay.map { it.start.hour })
        assertTrue(nextDay.all { it.start.toLocalDate() == midnight.toLocalDate() })
    }

    @Test
    fun narrativeAbsorbsBriefBenignChangeIntoPreviousSegment() {
        val now = LocalDateTime.parse("2026-10-08T14:20")
        val points =
            listOf(
                hour("2026-10-08T14:00", 0),
                hour("2026-10-08T15:00", 0),
                hour("2026-10-08T16:00", 2),
                hour("2026-10-08T17:00", 45),
                hour("2026-10-08T18:00", 45),
            )
        val segments = DayNarrative.from(points, now)
        assertEquals(3, segments.first().hours)
        assertEquals(listOf("Clear", "Fog"), segments.map { it.label })
    }

    @Test
    fun narrativeKeepsBriefHazardsAndPrioritizesLateStorm() {
        val now = LocalDateTime.parse("2026-10-08T14:20")
        val codes = listOf(0, 0, 2, 2, 45, 45, 95, 0, 0, 0)
        val points =
            codes.mapIndexed { index, code -> hour(now.withMinute(0).plusHours(index.toLong()).toString(), code) }
        val segments = DayNarrative.from(points, now)
        assertEquals(3, segments.size)
        assertEquals("Clear", segments.first().label)
        assertTrue(segments.any { it.weatherCode == 95 && it.start.hour == 20 })
        val freezingRain = DayNarrative.from(points.take(2) + hour("2026-10-08T16:00", 66), now)
        assertTrue(freezingRain.any { it.weatherCode == 66 })
    }

    @Test
    fun overcastIsNotPartlyCloudy() {
        assertEquals("Overcast", WeatherCondition.label(3))
        assertTrue(WeatherCondition.kind(3) != WeatherCondition.kind(2))
    }

    @Test
    fun uvCategoriesFollowWhoScale() {
        assertEquals("Low", DailyStatFormatter.uvCategory(2f))
        assertEquals("Moderate", DailyStatFormatter.uvCategory(3f))
        assertEquals("High", DailyStatFormatter.uvCategory(7f))
        assertEquals("Very high", DailyStatFormatter.uvCategory(10f))
        assertEquals("Extreme", DailyStatFormatter.uvCategory(11f))
        assertEquals("UV high of 7 (high)", DailyStatFormatter.uvMax(6.6f))
    }

    @Test
    fun uvHighIncludesCompleteDayPeakWindows() {
        val day = DailyForecast("2026-10-08", 70f, 50f, 0f, 0f, "", "", 0, uvIndexMax = 4.2f)
        val points =
            (0..23).map { index ->
                hour("2026-10-08T%02d:00".format(index), 0).copy(uvIndex = if (index in 13..15) 4.2f else 1f)
            }
        assertEquals("UV high of 4 (moderate) at 13-15", DailyStatFormatter.uvHigh(day, points))
        assertEquals("UV high of 4 (moderate)", DailyStatFormatter.uvHigh(day, points.drop(1)))
        val separated = points.map { if (it.hourLabel == "14:00") it.copy(uvIndex = 1f) else it }
        assertEquals("UV high of 4 (moderate) at 13, 15", DailyStatFormatter.uvHigh(day, separated))
    }

    @Test
    fun daylightAndPrecipitationHoursFormat() {
        assertEquals("Daylight 11 h 24 m", DailyStatFormatter.daylight(41_040f))
        assertEquals("Precip 3 h", DailyStatFormatter.precipitationHours(3f))
    }

    @Test
    fun lengthsFollowRainUnitSystem() {
        val metric = WeatherUnits(rain = RainUnit.MM)
        val imperial = WeatherUnits(rain = RainUnit.INCH)
        assertEquals("%.1f km".format(Locale.getDefault(), 2.4f), metric.visibilityText(2_400f))
        assertEquals("24 km", metric.visibilityText(24_140f))
        assertEquals("15 mi", imperial.visibilityText(24_140f))
        assertEquals("%,d m".format(Locale.getDefault(), 2_350), metric.elevationText(2_347f))
        assertEquals("%,d ft".format(Locale.getDefault(), 7_700), imperial.elevationText(2_347f))
        assertEquals("25 cm", metric.snowDepthText(0.25f))
        assertEquals("%.1f in".format(Locale.getDefault(), 9.8f), imperial.snowDepthText(0.25f))
    }

    @Test
    fun lengthsNormalizeToMeters() {
        assertEquals(304.8f, LengthConverter.toMeters(1000f, "ft")!!, 0.01f)
        assertEquals(0.254f, LengthConverter.toMeters(10f, "inch")!!, 0.001f)
        assertEquals(500f, LengthConverter.toMeters(500f, "m")!!, 0.01f)
    }

    @Test
    fun smallCitiesIncludeState() {
        val base = LocationEntity(1, "Springfield", "United States", 0.0, 0.0, admin1 = "Illinois")
        assertEquals("Springfield, Illinois", LocationLabel.of(base.copy(population = 114_000)))
        assertEquals("Springfield, Illinois", LocationLabel.of(base))
        assertEquals("Springfield", LocationLabel.of(base.copy(population = 2_000_000)))
        assertEquals("Springfield", LocationLabel.of(base.copy(admin1 = "")))
    }
}

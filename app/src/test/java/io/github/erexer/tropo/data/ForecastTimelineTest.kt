package io.github.erexer.tropo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class ForecastTimelineTest {
    @Test
    fun todaysHighLowUsesLocationDateNotSelectedGraphDay() {
        val days =
            listOf("2026-10-08", "2026-10-09").map { date ->
                DailyForecast(date, 70f, 50f, 0f, 0f, "", "", 0)
            }
        val now = Instant.parse("2026-10-09T01:00:00Z")
        assertEquals("2026-10-08", ForecastTimeline.today(days, "America/Los_Angeles", now)?.date)
    }

    @Test
    fun pastHoursRespectLocationAndCurrentHourStaysCurrent() {
        val now = Instant.parse("2026-11-01T09:59:00Z")
        assertTrue(ForecastTimeline.isPast("2026-11-01T00:00", "America/Los_Angeles", now))
        assertEquals(false, ForecastTimeline.isPast("2026-11-01T01:00", "America/Los_Angeles", now))
        assertEquals(false, ForecastTimeline.isPast("bad", "America/Los_Angeles", now))
    }

    private val midnight = LocalDateTime.parse("2026-10-06T00:00")
    private val times = (0..24).map { midnight.plusHours(it.toLong()) }
    private val day =
        DailyForecast(
            "2026-10-06",
            70f,
            50f,
            0f,
            0f,
            "2026-10-06T06:30",
            "2026-10-06T18:15",
            0,
        )

    @Test
    fun interpolatesSolarBoundariesAndClampsOutsideForecast() {
        assertEquals(6.5f, ForecastTimeline.position(times, midnight.plusMinutes(390)), 0.001f)
        assertEquals(0f, ForecastTimeline.position(times, midnight.minusDays(1)), 0.001f)
        assertEquals(24f, ForecastTimeline.position(times, midnight.plusDays(2)), 0.001f)
        assertEquals(0f, ForecastTimeline.position(listOf(midnight), midnight.plusHours(1)), 0.001f)
    }

    @Test
    fun nightIntervalsUseSunriseAndSunsetNotTwilight() {
        val intervals = ForecastTimeline.nights(day)
        assertEquals(midnight to midnight.plusMinutes(390), intervals.first())
        assertEquals(midnight.plusMinutes(1095) to midnight.plusDays(1), intervals.last())
        assertTrue(ForecastTimeline.nights(day.copy(sunrise = "", sunset = "")).isEmpty())
    }

    @Test
    fun currentHourUsesForecastTimezoneAcrossDateAndDstBoundaries() {
        val points =
            listOf("2026-11-01T00:00", "2026-11-01T01:00", "2026-11-01T02:00").map { time ->
                HourlyPoint(time, "00", 50f, 0f, 0f, 40f, 5f, 0f, 0f)
            }
        assertEquals(
            1,
            ForecastTimeline.currentIndex(points, "America/Los_Angeles", Instant.parse("2026-11-01T09:00:00Z")),
        )
        assertEquals(
            0,
            ForecastTimeline.currentIndex(points, "America/Los_Angeles", Instant.parse("2026-11-01T07:00:00Z")),
        )
    }
}

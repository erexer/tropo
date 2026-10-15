package io.github.erexer.tropo.data

import io.github.erexer.tropo.data.preferences.RainUnit
import io.github.erexer.tropo.data.preferences.WeatherUnits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PrecipitationOutlookTest {
    private val now = LocalDateTime.parse("2026-10-08T14:20")
    private val inches = WeatherUnits(rain = RainUnit.INCH)

    @Suppress("LongParameterList")
    private fun point(
        time: String,
        chance: Float,
        inches: Float = 0f,
        rain: Float? = null,
        snow: Float? = null,
        code: Int? = null,
    ) = HourlyPoint(time, time.takeLast(5), 50f, chance, inches, 40f, 5f, 0f, 0f, rain, snow, code)

    @Test
    fun belowTenPercentIsIgnored() {
        val points = listOf(point("2026-10-08T15:00", 9f), point("2026-10-08T16:00", 5f))
        assertNull(PrecipitationOutlook.find(points, now))
    }

    @Test
    fun outlookTracksHourRollover() {
        val points = listOf(point("2026-10-08T15:00", 60f, 0.1f, rain = 0.1f))
        assertFalse(requireNotNull(PrecipitationOutlook.find(points, now)).isNow)
        val atHour = LocalDateTime.parse("2026-10-08T15:00")
        assertTrue(requireNotNull(PrecipitationOutlook.find(points, atHour)).isNow)
        assertNull(PrecipitationOutlook.find(points, atHour.plusHours(1)))
    }

    @Test
    fun firstUpcomingHourAtTenPercentIsReturned() {
        val points =
            listOf(
                point("2026-10-08T14:00", 5f),
                point("2026-10-08T15:00", 10f, 0.12f, rain = 0.12f),
                point("2026-10-08T16:00", 80f, 0.3f, rain = 0.3f),
            )
        val outlook = PrecipitationOutlook.find(points, now)!!
        assertFalse(outlook.isNow)
        assertEquals(15, outlook.time.hour)
        val amount = "%.2f".format(java.util.Locale.getDefault(), 0.12f)
        assertEquals("10% chance of rain at 15 · $amount in", outlook.label(inches, now))
    }

    @Test
    fun currentHourWithAmountIsRainingNow() {
        val points = listOf(point("2026-10-08T14:00", 60f, 0.05f, rain = 0.05f), point("2026-10-08T15:00", 90f, 0.2f))
        val outlook = PrecipitationOutlook.find(points, now)!!
        assertTrue(outlook.isNow)
        assertTrue(outlook.label(inches, now).startsWith("Raining now · "))
        assertTrue(outlook.label(inches, now).endsWith(" in this hour"))
    }

    @Test
    fun snowAndMixedUseMatchingWords() {
        val snow = PrecipitationOutlook.find(listOf(point("2026-10-08T15:00", 40f, 0.1f, snow = 1f)), now)!!
        assertTrue(snow.label(WeatherUnits(), now).startsWith("40% chance of snow at 15"))
        val mixedPoint = point("2026-10-08T14:00", 40f, 0.1f, rain = 0.05f, snow = 1f)
        val mixed = PrecipitationOutlook.find(listOf(mixedPoint), now)!!
        assertTrue(mixed.label(WeatherUnits(), now).startsWith("Rain and snow now"))
        val snowCode = PrecipitationOutlook.find(listOf(point("2026-10-08T15:00", 20f, code = 73)), now)!!
        assertEquals("20% chance of snow at 15", snowCode.label(WeatherUnits(), now))
    }

    @Test
    fun laterDaysNameTheDay() {
        val tomorrow = PrecipitationOutlook.find(listOf(point("2026-10-09T03:00", 30f)), now)!!
        assertEquals("30% chance of rain tomorrow at 03", tomorrow.label(inches, now))
        val later = PrecipitationOutlook.find(listOf(point("2026-10-10T15:00", 30f)), now)!!
        val weekday =
            later.time.format(
                java.time.format.DateTimeFormatter
                    .ofPattern("EEE", java.util.Locale.getDefault()),
            )
        assertEquals("30% chance of rain $weekday at 15", later.label(inches, now))
    }

    @Test
    fun nothingBeyondOneWeek() {
        assertNull(PrecipitationOutlook.find(listOf(point("2026-10-15T15:00", 80f)), now))
        assertEquals(15, PrecipitationOutlook.find(listOf(point("2026-10-15T14:00", 80f)), now)?.time?.dayOfMonth)
    }

    @Test
    fun amountUsesPreferredUnit() {
        val outlook = PrecipitationOutlook.find(listOf(point("2026-10-08T15:00", 50f, 0.1f, rain = 0.1f)), now)!!
        val label = outlook.label(WeatherUnits(rain = RainUnit.MM), now)
        assertTrue(label, label.endsWith("${"%.2f".format(java.util.Locale.getDefault(), 2.54f)} mm"))
    }

    @Test
    fun dryWeekRequiresCompleteCoverage() {
        val points =
            (0..240).map { offset ->
                point(now.withMinute(0).plusHours(offset.toLong()).toString(), 0f)
            }
        assertEquals(PrecipitationOutlook.NONE_THIS_WEEK, PrecipitationOutlook.emptyLabel(points, now))
        assertEquals(
            "No precipitation expected in available forecast",
            PrecipitationOutlook.emptyLabel(points.take(72), now),
        )
        assertEquals(
            "No precipitation expected in available forecast",
            PrecipitationOutlook.emptyLabel(points.filterIndexed { index, _ -> index != 80 }, now),
        )
        val invalid =
            points.mapIndexed { index, point -> if (index == 80) point.copy(precipProb = Float.NaN) else point }
        assertEquals("No precipitation expected in available forecast", PrecipitationOutlook.emptyLabel(invalid, now))
    }
}

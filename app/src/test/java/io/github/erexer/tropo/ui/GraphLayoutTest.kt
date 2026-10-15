package io.github.erexer.tropo.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphLayoutTest {
    @Test
    fun fractionalPixelSpacingSelectsEveryDrawnHour() {
        val count = 240
        val width = 1687f
        (0 until count).forEach { hour ->
            val horizontal = hour * width / count
            assertEquals("hour $hour", hour, GraphLayout.hourIndex(horizontal, width, count))
        }
    }

    @Test
    fun graphTapsSnapToNearestHourAndClampEdges() {
        assertEquals(10, GraphLayout.hourIndex(104f, 2400f, 240))
        assertEquals(11, GraphLayout.hourIndex(105f, 2400f, 240))
        assertEquals(0, GraphLayout.hourIndex(-5f, 1687f, 240))
        assertEquals(239, GraphLayout.hourIndex(1800f, 1687f, 240))
        assertEquals(0, GraphLayout.hourIndex(0f, 0f, 0))
    }

    @Test
    fun narrowScreensStillShowAtLeastThreeAndThreeQuarterDays() {
        assertEquals(GraphLayout.MIN_VISIBLE_HOURS, GraphLayout.visibleHours(150f, 1f, 240))
    }

    @Test
    fun widerScreensShowMoreHours() {
        assertEquals(160, GraphLayout.visibleHours(400f, 1f, 240))
    }

    @Test
    fun largeTextReducesVisibleHoursButKeepsFloor() {
        assertEquals(106, GraphLayout.visibleHours(400f, 1.5f, 240))
        assertEquals(GraphLayout.MIN_VISIBLE_HOURS, GraphLayout.visibleHours(300f, 2f, 240))
    }

    @Test
    fun shortForecastFillsViewport() {
        assertEquals(48, GraphLayout.visibleHours(400f, 1f, 48))
    }

    @Test
    fun plotHeightTracksTemperatureRange() {
        assertEquals(GraphLayout.MIN_PLOT_HEIGHT_DP, GraphLayout.plotHeightDp(10f, 1f), 0.01f)
        assertEquals(180f, GraphLayout.plotHeightDp(30f, 1f), 0.01f)
        assertEquals(GraphLayout.MAX_PLOT_HEIGHT_DP, GraphLayout.plotHeightDp(60f, 1f), 0.01f)
        assertEquals(270f, GraphLayout.plotHeightDp(30f, 1.5f), 0.01f)
    }

    @Test
    fun niceMaximumUsesReadableSteps() {
        assertEquals(20f, GraphLayout.niceMaximum(18f), 0.01f)
        assertEquals(80f, GraphLayout.niceMaximum(50f), 0.01f)
        assertEquals(1f, GraphLayout.niceMaximum(0.5f), 0.01f)
        assertEquals(10f, GraphLayout.niceMaximum(10f), 0.01f)
    }

    @Test
    fun ticksRunTopToBottom() {
        assertEquals(listOf(100f, 75f, 50f, 25f, 0f), GraphLayout.ticks(0f, 100f))
    }

    @Test
    fun tickLabelsDropTrailingZero() {
        assertEquals("5", GraphLayout.tickLabel(5f))
        assertEquals(String.format(java.util.Locale.getDefault(), "%.1f", 7.5f), GraphLayout.tickLabel(7.5f))
    }
}

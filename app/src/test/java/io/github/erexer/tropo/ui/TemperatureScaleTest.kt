package io.github.erexer.tropo.ui

import io.github.erexer.tropo.data.HourlyPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureScaleTest {
    @Test
    fun scaleIncludesTemperatureAndDewWithPadding() {
        val points =
            listOf(
                HourlyPoint("2026-10-07T00:00", "00", 60f, 0f, 0f, 30f, 5f, 0f, 0f),
                HourlyPoint("2026-10-07T01:00", "01", 70f, 0f, 0f, 35f, 5f, 0f, 0f),
            )
        val scale = TemperatureScale.from(points)
        assertEquals(25f, scale.minimum, 0.001f)
        assertEquals(75f, scale.maximum, 0.001f)
        assertEquals(78f, scale.y(60f, 260f), 0.001f)
        assertEquals(26f, scale.y(70f, 260f), 0.001f)
    }

    @Test
    fun scaleIncludesFeelsLikeOutsideTemperatureRange() {
        val points = listOf(HourlyPoint("2026-10-07T00:00", "00", 60f, 0f, 0f, 30f, 5f, 0f, 0f, feelsLike = 90f))
        val scale = TemperatureScale.from(points)
        assertEquals(95f, scale.maximum, 0.001f)
    }
}

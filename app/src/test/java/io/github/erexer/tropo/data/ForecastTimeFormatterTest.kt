package io.github.erexer.tropo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastTimeFormatterTest {
    @Test
    fun formatsMidnightWithLeadingZero() {
        assertEquals("00:05", ForecastTimeFormatter.format("2026-10-06T00:05"))
    }

    @Test
    fun formatsAfternoonWithoutAmPm() {
        assertEquals("17:45", ForecastTimeFormatter.format("2026-10-06T17:45"))
    }
}

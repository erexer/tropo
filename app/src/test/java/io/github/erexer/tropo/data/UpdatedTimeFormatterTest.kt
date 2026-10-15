package io.github.erexer.tropo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class UpdatedTimeFormatterTest {
    private val millis = ZonedDateTime.of(2026, 10, 8, 21, 5, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

    @Test
    fun usesDeviceZoneWithoutSuffixWhenOffsetsMatch() {
        val text = UpdatedTimeFormatter.format(millis, "America/Los_Angeles", ZoneId.of("America/Los_Angeles"))
        assertEquals("14:05", text)
    }

    @Test
    fun convertsToDeviceZoneAndLabelsItWhenForecastZoneDiffers() {
        val text = UpdatedTimeFormatter.format(millis, "Europe/Paris", ZoneId.of("America/Los_Angeles"))
        assertTrue(text, text.startsWith("14:05 "))
    }
}

package io.github.erexer.tropo.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object UpdatedTimeFormatter {
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun format(
        epochMillis: Long,
        forecastTimezone: String,
        deviceZone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val forecastZone = runCatching { ZoneId.of(forecastTimezone) }.getOrDefault(ZoneId.of("UTC"))
        val sameOffset = deviceZone.rules.getOffset(instant) == forecastZone.rules.getOffset(instant)
        val formatter = if (sameOffset) TIME else DateTimeFormatter.ofPattern("HH:mm z", Locale.getDefault())
        return formatter.withZone(deviceZone).format(instant)
    }
}

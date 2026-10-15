package io.github.erexer.tropo.data

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ForecastTimeFormatter {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun format(value: String): String {
        val time = value.substringAfter('T', value).take(TIME_LENGTH)
        return runCatching { LocalTime.parse(time).format(formatter) }.getOrDefault(time)
    }

    private const val TIME_LENGTH = 5
}

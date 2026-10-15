package io.github.erexer.tropo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.erexer.tropo.data.DailyStatFormatter
import io.github.erexer.tropo.data.PrecipitationType

internal object WeatherValuePalette {
    @Suppress("MagicNumber")
    private val light = listOf(0xFF216927, 0xFF705700, 0xFF9E3F00, 0xFFB52222, 0xFF6039A8).map(::Color)

    @Suppress("MagicNumber")
    private val dark = listOf(0xFF8BD18E, 0xFFFFE066, 0xFFFFB06B, 0xFFFF8A80, 0xFFC3A6F0).map(::Color)

    @Composable
    fun uv(value: Float): Color = uv(value, appearance())

    @Composable
    fun precipitation(type: PrecipitationType): Color = precipitation(type, appearance())

    @Composable
    private fun appearance(): WeatherPalette.Appearance =
        if (WeatherPalette.isDark()) WeatherPalette.Appearance.DARK else WeatherPalette.Appearance.LIGHT

    fun uv(
        value: Float,
        appearance: WeatherPalette.Appearance,
    ): Color {
        if (!value.isFinite() || value < 0f) return Color.Unspecified
        val colors = if (appearance == WeatherPalette.Appearance.DARK) dark else light
        return colors[DailyStatFormatter.uvRisk(value).ordinal]
    }

    @Suppress("MagicNumber")
    fun precipitation(
        type: PrecipitationType,
        appearance: WeatherPalette.Appearance,
    ): Color {
        val colors =
            when (type) {
                PrecipitationType.RAIN -> 0xFF005D80 to 0xFF54C7EA
                PrecipitationType.SNOW -> 0xFF1E6090 to 0xFFA8D9FF
                PrecipitationType.MIXED -> 0xFF6039A8 to 0xFFC3A6F0
                PrecipitationType.FREEZING -> 0xFF705700 to 0xFFFFE066
                PrecipitationType.NONE, PrecipitationType.UNKNOWN -> 0xFF465362 to 0xFFCBD5E1
            }
        return Color(if (appearance == WeatherPalette.Appearance.DARK) colors.second else colors.first)
    }
}

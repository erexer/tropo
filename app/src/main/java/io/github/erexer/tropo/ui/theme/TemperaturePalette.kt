package io.github.erexer.tropo.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object TemperaturePalette {
    internal const val PAST_ALPHA = 0.85f
    private const val COLD_F = 10f
    private const val FREEZING_F = 32f
    private const val COOL_F = 50f
    private const val MILD_F = 65f
    private const val WARM_F = 75f
    private const val HOT_F = 85f
    private const val VERY_HOT_F = 95f
    private val stopsF = listOf(COLD_F, FREEZING_F, COOL_F, MILD_F, WARM_F, HOT_F, VERY_HOT_F)

    // Darker tones for light surfaces, lighter tones for dark surfaces; both keep text contrast >= 4.5:1
    @Suppress("MagicNumber")
    private val light =
        listOf(0xFF174A8B, 0xFF1F5FC9, 0xFF006457, 0xFF216927, 0xFF705700, 0xFF9E3F00, 0xFFB52222).map(::Color)

    @Suppress("MagicNumber")
    private val dark =
        listOf(0xFF93B8E8, 0xFF82B1FF, 0xFF4DD0C4, 0xFF8BD18E, 0xFFFFE066, 0xFFFFB06B, 0xFFFF8A80).map(::Color)

    internal fun gradientStops(
        minimum: Float,
        maximum: Float,
    ): List<Float> = (listOf(maximum) + stopsF.filter { it in minimum..maximum }.reversed() + minimum).distinct()

    fun color(
        fahrenheit: Float,
        isDark: Boolean,
    ): Color {
        val colors = if (isDark) dark else light
        val upper = stopsF.indexOfFirst { it >= fahrenheit }
        return when {
            !fahrenheit.isFinite() || upper == 0 -> colors.first()
            upper < 0 -> colors.last()
            else -> {
                val lower = upper - 1
                val fraction = (fahrenheit - stopsF[lower]) / (stopsF[upper] - stopsF[lower])
                lerp(colors[lower], colors[upper], fraction)
            }
        }
    }
}

package io.github.erexer.tropo.ui

import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

object GraphLayout {
    internal fun hourSpacing(
        width: Float,
        count: Int,
    ): Float = width / count.coerceAtLeast(1)

    internal fun hourIndex(
        horizontal: Float,
        width: Float,
        count: Int,
    ): Int {
        val invalid = !horizontal.isFinite() || !width.isFinite()
        if (count < 1 || width <= 0f || invalid) {
            return 0
        }
        return (horizontal / hourSpacing(width, count)).roundToInt().coerceIn(0, count - 1)
    }

    const val TICK_COUNT = 5
    const val MIN_VISIBLE_HOURS = 90
    const val MIN_HOUR_WIDTH_DP = 2.5f
    const val MIN_PLOT_HEIGHT_DP = 140f
    const val MAX_PLOT_HEIGHT_DP = 220f
    private const val DP_PER_CELSIUS = 6f

    @Suppress("MagicNumber")
    private val NICE_STEPS = listOf(1f, 2f, 2.5f, 5f, 10f)

    fun visibleHours(
        viewportWidthDp: Float,
        fontScale: Float,
        pointCount: Int,
    ): Int {
        val readable = (viewportWidthDp / (MIN_HOUR_WIDTH_DP * fontScale.coerceAtLeast(1f))).toInt()
        return readable.coerceAtLeast(MIN_VISIBLE_HOURS).coerceAtMost(pointCount).coerceAtLeast(1)
    }

    fun plotHeightDp(
        temperatureRangeCelsius: Float,
        fontScale: Float,
    ): Float {
        val height = temperatureRangeCelsius.coerceAtLeast(0f) * DP_PER_CELSIUS
        return height.coerceIn(MIN_PLOT_HEIGHT_DP, MAX_PLOT_HEIGHT_DP) * fontScale.coerceAtLeast(1f)
    }

    fun niceMaximum(value: Float): Float {
        val intervals = TICK_COUNT - 1
        val raw = value.coerceAtLeast(1f) / intervals
        val magnitude = 10f.pow(floor(log10(raw)))
        val step = NICE_STEPS.first { it * magnitude >= raw - EPSILON } * magnitude
        return step * intervals
    }

    // Top to bottom
    fun ticks(
        minimum: Float,
        maximum: Float,
    ): List<Float> {
        val intervals = TICK_COUNT - 1
        return (0..intervals).map { maximum - (maximum - minimum) * it / intervals }
    }

    fun tickLabel(value: Float): String =
        if (value == floor(value)) {
            value.toInt().toString()
        } else {
            "%.1f".format(Locale.getDefault(), value)
        }

    private const val EPSILON = 0.0001f
}

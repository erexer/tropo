package io.github.erexer.tropo.ui

import io.github.erexer.tropo.data.HourlyPoint

data class TemperatureScale(
    val minimum: Float,
    val maximum: Float,
) {
    fun y(
        value: Float,
        height: Float,
    ): Float = height - (value - minimum) / (maximum - minimum).coerceAtLeast(MINIMUM_RANGE) * height

    companion object {
        fun from(points: List<HourlyPoint>): TemperatureScale {
            val values =
                points
                    .flatMap { point ->
                        listOfNotNull(point.temperature, point.dewPoint, point.feelsLike)
                    }.filter(Float::isFinite)
            return TemperatureScale(
                (values.minOrNull() ?: 0f) - PADDING,
                (values.maxOrNull() ?: 0f) + PADDING,
            )
        }

        private const val PADDING = 5f
        private const val MINIMUM_RANGE = 1f
    }
}

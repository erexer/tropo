package io.github.erexer.tropo.data

object WindDirectionFormatter {
    fun compact(degrees: Float): String = describeWindFrom(degrees).replace(" from ", " ")

    private val compassPoints = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    private val flowArrows = arrayOf("↓", "↙", "←", "↖", "↑", "↗", "→", "↘")
    private const val FULL_CIRCLE_DEGREES = 360f
    private const val COMPASS_BIN_SIZE = 45f
    private const val COMPASS_BIN_OFFSET = 22.5f

    fun describeWindFrom(degrees: Float): String {
        val normalized = ((degrees % FULL_CIRCLE_DEGREES + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES)
        val directionIndex = ((normalized + COMPASS_BIN_OFFSET) / COMPASS_BIN_SIZE).toInt() % compassPoints.size
        return "${flowArrows[directionIndex]} from ${compassPoints[directionIndex]}"
    }
}

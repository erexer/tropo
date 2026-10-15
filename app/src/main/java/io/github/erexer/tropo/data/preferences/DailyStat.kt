package io.github.erexer.tropo.data.preferences

enum class DailyStat(
    val label: String,
) {
    UV_MAX("UV high"),
    DAYLIGHT("Daylight duration"),
    PRECIPITATION_HOURS("Precipitation hours"),
    SUN_TIMES("Sun times"),
    ;

    companion object {
        internal const val SCHEMA_VERSION = 2
        val defaults: Set<DailyStat> = entries.toSet()

        fun decode(
            names: Set<String>?,
            version: Int = SCHEMA_VERSION,
        ): Set<DailyStat> {
            if (names == null) {
                return defaults
            }
            val selected = entries.filter { it.name in names }.toSet()
            return if (version < SCHEMA_VERSION) selected + SUN_TIMES else selected
        }
    }
}

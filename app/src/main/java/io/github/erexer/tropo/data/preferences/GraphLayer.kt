package io.github.erexer.tropo.data.preferences

enum class GraphLayer(
    val label: String,
) {
    FEELS_LIKE("Feels like"),
    DEW_POINT("Dew point"),
    WIND("Wind"),
    UV("UV"),
    AQI("AQI"),
    ;

    companion object {
        fun decode(names: Set<String>?): Set<GraphLayer> = entries.filter { it.name in names.orEmpty() }.toSet()
    }
}

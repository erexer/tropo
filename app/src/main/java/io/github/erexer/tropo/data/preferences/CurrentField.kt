package io.github.erexer.tropo.data.preferences

enum class CurrentField(
    val label: String,
) {
    TEMPERATURE("Temperature"),
    CONDITION("Condition"),
    HIGH_LOW("Today's high / low"),
    FEELS_LIKE("Feels like"),
    HUMIDITY("Humidity"),
    WIND("Wind"),
    GUSTS("Wind gusts"),
    CLOUD_COVER("Cloud cover"),
    NARRATIVE("Conditions changing"),
    PRECIPITATION("Precipitation outlook"),
    ;

    companion object {
        val defaults: Set<CurrentField> = entries.toSet()

        fun decode(names: Set<String>?): Set<CurrentField> {
            if (names == null) {
                return defaults
            }
            return entries.filter { it.name in names }.toSet()
        }
    }
}

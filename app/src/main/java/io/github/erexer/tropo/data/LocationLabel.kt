package io.github.erexer.tropo.data

import io.github.erexer.tropo.data.local.LocationEntity

object LocationLabel {
    const val SMALL_CITY_POPULATION = 500_000L

    fun of(location: LocationEntity): String {
        val state = location.admin1?.takeIf { it.isNotBlank() && it != location.name }
        val small = location.population == null || location.population < SMALL_CITY_POPULATION
        return if (state != null && small) "${location.name}, $state" else location.name
    }
}

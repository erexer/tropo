package io.github.erexer.tropo.data.repository

import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.preferences.WeatherUnits

data class LocationWeather(
    val weather: WeatherEntity?,
    val isCached: Boolean,
)

enum class WeatherRefresh { STALE, FORCE, CACHE_ONLY }

data class WidgetSnapshot(
    val location: LocationEntity?,
    val weather: WeatherEntity?,
    val units: WeatherUnits,
)

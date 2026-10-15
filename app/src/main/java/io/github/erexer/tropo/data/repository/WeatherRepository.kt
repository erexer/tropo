package io.github.erexer.tropo.data.repository

import io.github.erexer.tropo.data.AirQualitySnapshot
import io.github.erexer.tropo.data.ForecastSnapshot
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.preferences.AppThemeMode
import io.github.erexer.tropo.data.preferences.CurrentField
import io.github.erexer.tropo.data.preferences.DailyStat
import io.github.erexer.tropo.data.preferences.GraphLayer
import io.github.erexer.tropo.data.preferences.HourlyField
import io.github.erexer.tropo.data.preferences.RainUnit
import io.github.erexer.tropo.data.preferences.TemperatureUnit
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.preferences.WindUnit
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun getCurrentWeather(): WeatherEntity?

    suspend fun currentReading(refresh: WeatherRefresh): LocationWeather

    fun widgetState(): Flow<WidgetSnapshot>

    suspend fun getForecast(forecastDays: Int = 3): Result<ForecastSnapshot>

    suspend fun getAirQuality(): Result<AirQualitySnapshot>

    suspend fun getSelectedLocation(): LocationEntity?

    suspend fun deviceLocation(): Result<LocationEntity>

    suspend fun searchLocations(query: String): List<LocationEntity>

    suspend fun getFavoriteLocations(): List<LocationEntity>

    fun locationWeather(
        locations: List<LocationEntity>,
        refresh: WeatherRefresh,
    ): Flow<Map<Long, LocationWeather>>

    fun locationUpdates(): Flow<Int>

    fun currentFields(): Flow<Set<CurrentField>>

    suspend fun setCurrentFields(fields: Set<CurrentField>)

    fun graphLayers(): Flow<Set<GraphLayer>>

    suspend fun setGraphLayers(layers: Set<GraphLayer>)

    suspend fun selectLocation(location: LocationEntity)

    suspend fun setFavorite(
        location: LocationEntity,
        isFavorite: Boolean,
    )

    suspend fun reorderFavorites(locationIds: List<Long>)

    fun themeMode(): Flow<AppThemeMode>

    suspend fun setThemeMode(themeMode: AppThemeMode)

    fun hourlyFields(): Flow<Set<HourlyField>>

    suspend fun setHourlyFields(fields: Set<HourlyField>)

    fun dailyStats(): Flow<Set<DailyStat>>

    suspend fun setDailyStats(stats: Set<DailyStat>)

    fun units(): Flow<WeatherUnits>

    suspend fun setUnits(units: WeatherUnits)

    suspend fun setTemperature(unit: TemperatureUnit)

    suspend fun setWind(unit: WindUnit)

    suspend fun setRain(unit: RainUnit)
}

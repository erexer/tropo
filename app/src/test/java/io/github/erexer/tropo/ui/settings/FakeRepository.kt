package io.github.erexer.tropo.ui.settings

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
import io.github.erexer.tropo.data.repository.LocationWeather
import io.github.erexer.tropo.data.repository.WeatherRefresh
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.data.repository.WidgetSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeRepository : WeatherRepository {
    override suspend fun deviceLocation(): Result<LocationEntity> = Result.failure(IllegalStateException("Unavailable"))

    override fun locationWeather(
        locations: List<LocationEntity>,
        refresh: WeatherRefresh,
    ): Flow<Map<Long, LocationWeather>> = flowOf(emptyMap())

    override fun locationUpdates(): Flow<Int> = flowOf(0)

    override fun currentFields(): Flow<Set<CurrentField>> = flowOf(CurrentField.defaults)

    override suspend fun setCurrentFields(fields: Set<CurrentField>) = Unit

    override fun graphLayers(): Flow<Set<GraphLayer>> = flowOf(emptySet())

    override suspend fun setGraphLayers(layers: Set<GraphLayer>) = Unit

    override suspend fun getCurrentWeather(): WeatherEntity? = null

    override suspend fun currentReading(refresh: WeatherRefresh): LocationWeather = LocationWeather(null, true)

    override fun widgetState(): Flow<WidgetSnapshot> = flowOf(WidgetSnapshot(null, null, WeatherUnits()))

    override suspend fun getForecast(forecastDays: Int): Result<ForecastSnapshot> {
        val snapshot = ForecastSnapshot(emptyList(), emptyList(), "UTC", 0L, false)
        return Result.success(snapshot)
    }

    override suspend fun getAirQuality(): Result<AirQualitySnapshot> {
        val snapshot = AirQualitySnapshot(emptyList(), "UTC", 0L, false)
        return Result.success(snapshot)
    }

    override suspend fun getSelectedLocation(): LocationEntity? = null

    override suspend fun searchLocations(query: String): List<LocationEntity> = emptyList()

    override suspend fun getFavoriteLocations(): List<LocationEntity> = emptyList()

    override suspend fun selectLocation(location: LocationEntity) = Unit

    override suspend fun setFavorite(
        location: LocationEntity,
        isFavorite: Boolean,
    ) = Unit

    override suspend fun reorderFavorites(locationIds: List<Long>) = Unit

    override fun themeMode(): Flow<AppThemeMode> = flowOf(AppThemeMode.SYSTEM)

    override suspend fun setThemeMode(themeMode: AppThemeMode) = Unit

    override fun hourlyFields(): Flow<Set<HourlyField>> = flowOf(HourlyField.defaults)

    override suspend fun setHourlyFields(fields: Set<HourlyField>) = Unit

    override fun dailyStats(): Flow<Set<DailyStat>> = flowOf(DailyStat.defaults)

    override suspend fun setDailyStats(stats: Set<DailyStat>) = Unit

    override fun units(): Flow<WeatherUnits> = flowOf(WeatherUnits())

    override suspend fun setUnits(units: WeatherUnits) = Unit

    override suspend fun setTemperature(unit: TemperatureUnit) = Unit

    override suspend fun setWind(unit: WindUnit) = Unit

    override suspend fun setRain(unit: RainUnit) = Unit
}

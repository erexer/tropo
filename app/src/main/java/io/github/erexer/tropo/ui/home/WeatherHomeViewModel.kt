package io.github.erexer.tropo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.erexer.tropo.data.AirQualityPoint
import io.github.erexer.tropo.data.DailyForecast
import io.github.erexer.tropo.data.ForecastTimeline
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.preferences.CurrentField
import io.github.erexer.tropo.data.preferences.DailyStat
import io.github.erexer.tropo.data.preferences.GraphLayer
import io.github.erexer.tropo.data.preferences.HourlyField
import io.github.erexer.tropo.data.preferences.TemperatureUnit
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.repository.WeatherRefresh
import io.github.erexer.tropo.data.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val DEFAULT_FORECAST_DAYS = 10
private const val RESUME_TTL_MINUTES = 15L
private val RESUME_TTL = TimeUnit.MINUTES.toMillis(RESUME_TTL_MINUTES)

data class WeatherUiState(
    val location: LocationEntity? = null,
    val weather: WeatherEntity? = null,
    val isWeatherCached: Boolean = false,
    val forecast: List<HourlyPoint> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList(),
    val forecastTimezone: String = "UTC",
    val forecastUpdatedAt: Long? = null,
    val isForecastCached: Boolean = false,
    val airQuality: List<AirQualityPoint> = emptyList(),
    val airQualityUpdatedAt: Long? = null,
    val isAirQualityCached: Boolean = false,
    val airQualityErrorMessage: String? = null,
    val forecastDays: Int = DEFAULT_FORECAST_DAYS,
    val hourlyFields: Set<HourlyField> = HourlyField.defaults,
    val dailyStats: Set<DailyStat> = DailyStat.defaults,
    val currentFields: Set<CurrentField> = CurrentField.defaults,
    val graphLayers: Set<GraphLayer> = emptySet(),
    val graphError: String? = null,
    val hourlyFieldError: String? = null,
    val selectedForecastIndex: Int = 0,
    val nowRevision: Int = 0,
    val isCelsius: Boolean = true,
    val units: WeatherUnits = WeatherUnits(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val forecastErrorMessage: String? = null,
)

class WeatherHomeViewModel(
    private val repository: WeatherRepository,
) : ViewModel() {
    private var loadJob: Job? = null
    private var locationRevision: Int? = null
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentFields().collect { fields ->
                _uiState.value = _uiState.value.copy(currentFields = fields)
            }
        }
        viewModelScope.launch {
            repository.graphLayers().collect { layers -> _uiState.value = _uiState.value.copy(graphLayers = layers) }
        }
        viewModelScope.launch {
            repository.locationUpdates().collect {
                val location = repository.getSelectedLocation()
                if (location?.id == _uiState.value.location?.id) {
                    _uiState.value = _uiState.value.copy(location = location)
                }
            }
        }
        viewModelScope.launch {
            repository.hourlyFields().collect { fields ->
                _uiState.value = _uiState.value.copy(hourlyFields = fields)
            }
        }
        viewModelScope.launch {
            repository.dailyStats().collect { stats ->
                _uiState.value = _uiState.value.copy(dailyStats = stats)
            }
        }
        viewModelScope.launch {
            repository.units().collect { units ->
                _uiState.value =
                    _uiState.value.copy(
                        units = units,
                        isCelsius = units.temperature == TemperatureUnit.CELSIUS,
                    )
            }
        }
    }

    fun loadData() {
        if (loadJob?.isActive == true) {
            return
        }
        loadJob =
            viewModelScope.launch {
                try {
                    val location = repository.getSelectedLocation()
                    val previousState = _uiState.value
                    val sameLocation = previousState.location?.id == location?.id
                    _uiState.value =
                        previousState.copy(
                            location = location,
                            weather = previousState.weather.takeIf { sameLocation },
                            isWeatherCached = sameLocation && previousState.isWeatherCached,
                            forecast = previousState.forecast.takeIf { sameLocation }.orEmpty(),
                            dailyForecast = previousState.dailyForecast.takeIf { sameLocation }.orEmpty(),
                            airQuality = previousState.airQuality.takeIf { sameLocation }.orEmpty(),
                            selectedForecastIndex = previousState.selectedForecastIndex.takeIf { sameLocation } ?: 0,
                            forecastUpdatedAt = previousState.forecastUpdatedAt.takeIf { sameLocation },
                            isForecastCached = sameLocation && previousState.isForecastCached,
                            airQualityUpdatedAt = previousState.airQualityUpdatedAt.takeIf { sameLocation },
                            isAirQualityCached = sameLocation && previousState.isAirQualityCached,
                            isLoading = !sameLocation,
                            isRefreshing = sameLocation,
                            errorMessage = null,
                            forecastErrorMessage = null,
                        )
                    val reading = repository.currentReading(WeatherRefresh.FORCE)
                    val weather = reading.weather
                    // Current-weather fetch may fill in the location's state
                    val detailedLocation =
                        repository.getSelectedLocation()?.takeIf { it.id == location?.id } ?: location
                    val (forecastResult, airQualityResult) =
                        coroutineScope {
                            val forecast = async { repository.getForecast(_uiState.value.forecastDays) }
                            val airQuality = async { repository.getAirQuality() }
                            forecast.await() to airQuality.await()
                        }
                    val forecast = forecastResult.getOrNull()
                    val airQuality = airQualityResult.getOrNull()
                    val activeState = _uiState.value
                    _uiState.value =
                        _uiState.value.copy(
                            location = detailedLocation,
                            weather = weather ?: activeState.weather,
                            isWeatherCached = reading.isCached,
                            forecast = forecast?.hourly ?: activeState.forecast,
                            dailyForecast = forecast?.daily ?: activeState.dailyForecast,
                            forecastTimezone = forecast?.timezone ?: activeState.forecastTimezone,
                            forecastUpdatedAt = forecast?.fetchedAt ?: activeState.forecastUpdatedAt,
                            isForecastCached = forecast?.isCached ?: activeState.isForecastCached,
                            airQuality = airQuality?.points ?: activeState.airQuality,
                            airQualityUpdatedAt = airQuality?.fetchedAt ?: activeState.airQualityUpdatedAt,
                            isAirQualityCached = airQuality?.isCached ?: activeState.isAirQualityCached,
                            airQualityErrorMessage = airQualityResult.exceptionOrNull()?.localizedMessage,
                            selectedForecastIndex =
                                preservedIndex(
                                    forecast?.hourly ?: activeState.forecast,
                                    forecast?.timezone ?: activeState.forecastTimezone,
                                    activeState,
                                ),
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = if (weather == null) "Weather is unavailable. Check your connection and retry." else null,
                            forecastErrorMessage = forecastResult.exceptionOrNull()?.localizedMessage,
                        )
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.localizedMessage ?: "Weather request failed.",
                        )
                }
            }
    }

    fun refreshIfStale(now: Long = System.currentTimeMillis()) {
        val state = _uiState.value
        if (state.location == null || loadJob?.isActive == true) {
            return
        }
        val oldest = minOf(state.weather?.updatedAt ?: 0L, state.forecastUpdatedAt ?: 0L)
        if (now - oldest >= RESUME_TTL) {
            loadData()
        }
    }

    fun reloadLocation() {
        loadJob?.cancel()
        loadJob = null
        loadData()
    }

    fun loadForRevision(revision: Int) {
        if (locationRevision == revision) {
            return
        }
        locationRevision = revision
        reloadLocation()
    }

    fun selectNow() {
        val state = _uiState.value
        selectForecastPoint(ForecastTimeline.currentIndex(state.forecast, state.forecastTimezone, Instant.now()))
        _uiState.value = _uiState.value.copy(nowRevision = state.nowRevision + 1)
    }

    private fun preservedIndex(
        points: List<HourlyPoint>,
        timezone: String,
        state: WeatherUiState,
    ): Int {
        val selectedTime = state.forecast.getOrNull(state.selectedForecastIndex)?.forecastTime
        val index = points.indexOfFirst { it.forecastTime == selectedTime }
        if (index >= 0) {
            return index
        }
        return ForecastTimeline.currentIndex(points, timezone, Instant.now())
    }

    fun selectForecastPoint(index: Int) {
        if (index in _uiState.value.forecast.indices) {
            _uiState.value = _uiState.value.copy(selectedForecastIndex = index)
        }
    }

    fun setHourlyFields(fields: Set<HourlyField>) {
        viewModelScope.launch {
            try {
                repository.setHourlyFields(fields)
                _uiState.value = _uiState.value.copy(hourlyFieldError = null)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(hourlyFieldError = "Could not save hourly fields.")
            }
        }
    }

    fun setGraphLayers(layers: Set<GraphLayer>) {
        viewModelScope.launch {
            try {
                repository.setGraphLayers(layers)
                _uiState.value = _uiState.value.copy(graphError = null)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(graphError = "Could not save graph layers.")
            }
        }
    }

    fun setForecastDays(days: Int) {
        if (days !in SUPPORTED_FORECAST_DAYS || days == _uiState.value.forecastDays) {
            return
        }
        viewModelScope.launch {
            val previousState = _uiState.value
            _uiState.value = previousState.copy(forecastDays = days, isRefreshing = true, forecastErrorMessage = null)
            val result = repository.getForecast(days)
            val snapshot = result.getOrNull()
            _uiState.value =
                _uiState.value.copy(
                    forecastDays = if (snapshot == null) previousState.forecastDays else days,
                    forecast = snapshot?.hourly ?: previousState.forecast,
                    dailyForecast = snapshot?.daily ?: previousState.dailyForecast,
                    forecastTimezone = snapshot?.timezone ?: previousState.forecastTimezone,
                    forecastUpdatedAt = snapshot?.fetchedAt ?: previousState.forecastUpdatedAt,
                    isForecastCached = snapshot?.isCached ?: previousState.isForecastCached,
                    forecastErrorMessage = result.exceptionOrNull()?.localizedMessage,
                    selectedForecastIndex =
                        ForecastTimeline.currentIndex(
                            snapshot?.hourly ?: previousState.forecast,
                            snapshot?.timezone ?: previousState.forecastTimezone,
                            Instant.now(),
                        ),
                    isRefreshing = false,
                )
        }
    }

    private companion object {
        val SUPPORTED_FORECAST_DAYS = setOf(3, 10)
    }
}

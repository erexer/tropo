package io.github.erexer.tropo.ui.locations

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.repository.LocationWeather
import io.github.erexer.tropo.data.repository.WeatherRefresh
import io.github.erexer.tropo.data.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class LocationsViewModel(
    private val repository: WeatherRepository,
) : ViewModel() {
    val units: Flow<WeatherUnits> = repository.units()
    private val query = MutableStateFlow("")
    val searchQuery = query.asStateFlow()
    private val searchFailure = MutableStateFlow<String?>(null)
    val searchError = searchFailure.asStateFlow()
    private val selectionFailure = MutableStateFlow<String?>(null)
    val selectionError = selectionFailure.asStateFlow()
    private val searching = MutableStateFlow(false)
    val isSearching = searching.asStateFlow()
    private val favoriteRevision = MutableStateFlow(0)
    private val selected = MutableStateFlow<LocationEntity?>(null)
    val selectedLocation = selected.asStateFlow()
    private val weather = MutableStateFlow<Map<Long, LocationWeather>>(emptyMap())
    val readings = weather.asStateFlow()
    private val refreshing = MutableStateFlow(false)
    val weatherLoading = refreshing.asStateFlow()
    private val locating = MutableStateFlow(false)
    val usingDeviceLocation = locating.asStateFlow()
    private var weatherJob: Job? = null
    private var deviceJob: Job? = null
    private var weatherRevision = 0

    val favorites =
        combine(favoriteRevision, repository.locationUpdates()) { revision, details -> revision to details }
            .flatMapLatest { flow { emit(repository.getFavoriteLocations()) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SHARING_TIMEOUT), emptyList())

    val searchResults =
        combine(query.debounce(SEARCH_DEBOUNCE), favoriteRevision) { value, _ -> value }
            .mapLatest { value ->
                searchFailure.value = null
                if (value.length < MIN_QUERY_LENGTH) {
                    emptyList()
                } else {
                    searching.value = true
                    try {
                        repository.searchLocations(value)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        searchFailure.value = "Location search failed. Check your connection and retry."
                        emptyList()
                    } finally {
                        searching.value = false
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SHARING_TIMEOUT), emptyList())

    fun loadLocations() {
        viewModelScope.launch { selected.value = repository.getSelectedLocation() }
    }

    fun refreshWeather(refresh: WeatherRefresh) {
        val cities = (favorites.value + listOfNotNull(selected.value)).distinctBy { it.id }
        val revision = ++weatherRevision
        weatherJob?.cancel()
        weatherJob =
            viewModelScope.launch {
                refreshing.value = true
                try {
                    repository.locationWeather(cities, refresh).collect { weather.value = it }
                } finally {
                    if (revision == weatherRevision) refreshing.value = false
                }
            }
    }

    @Suppress("TooGenericExceptionCaught")
    fun useDeviceLocation(onSelected: () -> Unit) {
        if (locating.value) {
            return
        }
        deviceJob =
            viewModelScope.launch {
                locating.value = true
                selectionFailure.value = null
                try {
                    val city = repository.deviceLocation().getOrThrow()
                    repository.selectLocation(city)
                    selected.value = city
                    onSelected()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    selectionFailure.value = error.message ?: "Device location unavailable. Retry."
                } finally {
                    locating.value = false
                }
            }
    }

    fun locationPermissionDenied() {
        selectionFailure.value = "Location permission denied. Enable it in app settings."
    }

    fun cancelLocationRequests() {
        deviceJob?.cancel()
        weatherJob?.cancel()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun selectLocation(
        city: LocationEntity,
        onSelected: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                repository.selectLocation(city)
                query.value = ""
                selectionFailure.value = null
                onSelected()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                selectionFailure.value = "Could not select this location."
            }
        }
    }

    fun toggleFavorite(city: LocationEntity) {
        viewModelScope.launch {
            try {
                repository.setFavorite(city, !city.isFavorite)
                favoriteRevision.value++
                selectionFailure.value = null
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                selectionFailure.value = "Could not update this favorite."
            }
        }
    }

    fun moveFavorite(
        id: Long,
        direction: Int,
    ) {
        val current = favorites.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) {
            return
        }
        val target = (index + direction).coerceIn(0, current.lastIndex)
        if (index == target) {
            return
        }
        val reordered = current.toMutableList().apply { add(target, removeAt(index)) }
        reorderFavorites(reordered.map { it.id })
    }

    fun reorderFavorites(ids: List<Long>) {
        if (ids == favorites.value.map { it.id }) {
            return
        }
        viewModelScope.launch {
            try {
                repository.reorderFavorites(ids)
                favoriteRevision.value++
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                selectionFailure.value = "Could not reorder favorites."
            }
        }
    }
}

@Composable
internal fun locationsModel(repository: WeatherRepository): LocationsViewModel =
    viewModel(factory = viewModelFactory { initializer { LocationsViewModel(repository) } })

private const val MIN_QUERY_LENGTH = 2
private const val SEARCH_DEBOUNCE = 400L
private const val SHARING_TIMEOUT = 5_000L

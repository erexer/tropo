package com.tropo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tropo.data.preferences.AppTheme
import com.tropo.data.preferences.SpeedUnit
import com.tropo.data.preferences.TemperatureUnit
import com.tropo.data.preferences.UserPreferencesRepository
import com.tropo.data.remote.GeocodingApiClient
import com.tropo.data.remote.GeocodingLocation
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val locations: List<GeocodingLocation>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class)
class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val geocodingClient: GeocodingApiClient
) : ViewModel() {

    val userPreferences = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.tropo.data.preferences.UserPreferences()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<SearchUiState> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .mapLatest { query ->
            if (query.length < 2) {
                SearchUiState.Idle
            } else {
                val results = geocodingClient.searchLocations(query)
                if (results.isEmpty()) SearchUiState.Error("No matching locations found.")
                else SearchUiState.Success(results)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState.Idle
        )

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { preferencesRepository.updateTemperatureUnit(unit) }
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        viewModelScope.launch { preferencesRepository.updateSpeedUnit(unit) }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { preferencesRepository.updateAppTheme(theme) }
    }
}
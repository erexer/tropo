package io.github.erexer.tropo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.repository.WeatherRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SettingsViewModel(private val repository: WeatherRepository) : ViewModel() {

    val isCelsius: Flow<Boolean> = repository.isCelsius()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<LocationEntity>> = _searchQuery
        .debounce(400)
        .mapLatest { query ->
            if (query.length < 2) emptyList()
            else repository.searchLocations(query)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun selectLocation(location: LocationEntity) {
        viewModelScope.launch {
            repository.selectLocation(location)
            _searchQuery.value = ""
        }
    }

    fun toggleUnit(isCelsius: Boolean) {
        viewModelScope.launch {
            repository.setCelsius(isCelsius)
        }
    }
}
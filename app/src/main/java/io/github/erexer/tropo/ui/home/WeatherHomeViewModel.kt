package io.github.erexer.tropo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val location: LocationEntity? = null,
    val weather: WeatherEntity? = null,
    val isCelsius: Boolean = true,
    val isLoading: Boolean = true
)

class WeatherHomeViewModel(private val repository: WeatherRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            repository.isCelsius().collect { isCelsius ->
                _uiState.value = _uiState.value.copy(isCelsius = isCelsius)
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val location = repository.getSelectedLocation()
            val weather = repository.getCurrentWeather()
            _uiState.value = _uiState.value.copy(
                location = location,
                weather = weather,
                isLoading = false
            )
        }
    }
}
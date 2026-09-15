package io.github.erexer.tropo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = true,
    val points: List<HourlyPoint> = emptyList(),
    val showUv: Boolean = false,
    val showDewPoint: Boolean = false,
    val showWind: Boolean = false,
    val selectedIndex: Int? = 0,
    val errorMessage: String? = null
)

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        fetchForecast(47.6062, -122.3321) // Seattle default coordinates
    }

    fun fetchForecast(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getHourlyForecast(lat, lon).collect { result ->
                result.fold(
                    onSuccess = { points ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            points = points,
                            selectedIndex = 0
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Network request failed."
                        )
                    }
                )
            }
        }
    }

    fun toggleUv() {
        _uiState.value = _uiState.value.copy(showUv = !_uiState.value.showUv)
    }

    fun toggleDewPoint() {
        _uiState.value = _uiState.value.copy(showDewPoint = !_uiState.value.showDewPoint)
    }

    fun toggleWind() {
        _uiState.value = _uiState.value.copy(showWind = !_uiState.value.showWind)
    }

    fun selectPoint(index: Int) {
        _uiState.value = _uiState.value.copy(selectedIndex = index)
    }

    class Factory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WeatherViewModel(repository) as T
        }
    }
}
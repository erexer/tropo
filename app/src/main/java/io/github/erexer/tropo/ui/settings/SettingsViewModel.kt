package io.github.erexer.tropo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.erexer.tropo.data.preferences.AppThemeMode
import io.github.erexer.tropo.data.preferences.CurrentField
import io.github.erexer.tropo.data.preferences.DailyStat
import io.github.erexer.tropo.data.preferences.HourlyField
import io.github.erexer.tropo.data.preferences.RainUnit
import io.github.erexer.tropo.data.preferences.TemperatureUnit
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.preferences.WindUnit
import io.github.erexer.tropo.data.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: WeatherRepository,
) : ViewModel() {
    val units: Flow<WeatherUnits> = repository.units()
    val hourlyFields: Flow<Set<HourlyField>> = repository.hourlyFields()
    val dailyStats: Flow<Set<DailyStat>> = repository.dailyStats()
    val currentFields: Flow<Set<CurrentField>> = repository.currentFields()
    private val _preferenceError = MutableStateFlow<String?>(null)
    val preferenceError = _preferenceError.asStateFlow()
    val themeMode: Flow<AppThemeMode> = repository.themeMode()

    fun setThemeMode(themeMode: AppThemeMode) {
        savePreference { repository.setThemeMode(themeMode) }
    }

    fun setUnits(units: WeatherUnits) {
        savePreference { repository.setUnits(units) }
    }

    fun setTemperature(unit: TemperatureUnit) {
        savePreference { repository.setTemperature(unit) }
    }

    fun setWind(unit: WindUnit) {
        savePreference { repository.setWind(unit) }
    }

    fun setRain(unit: RainUnit) {
        savePreference { repository.setRain(unit) }
    }

    fun setHourlyFields(fields: Set<HourlyField>) {
        savePreference { repository.setHourlyFields(fields) }
    }

    fun setDailyStats(stats: Set<DailyStat>) {
        savePreference { repository.setDailyStats(stats) }
    }

    fun setCurrentFields(fields: Set<CurrentField>) {
        savePreference { repository.setCurrentFields(fields) }
    }

    private fun savePreference(save: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                save()
                _preferenceError.value = null
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _preferenceError.value = "Could not save preferences."
            }
        }
    }
}

package com.tropo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tropo_user_preferences")

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
enum class SpeedUnit { KMH, MPH, MS }
enum class AppTheme { SYSTEM, LIGHT, DARK, OLED }

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val appTheme: AppTheme = AppTheme.SYSTEM
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferenceKeys {
        val TEMP_UNIT = stringPreferencesKey("temperature_unit")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            val tempUnit = preferences[PreferenceKeys.TEMP_UNIT]?.let {
                runCatching { TemperatureUnit.valueOf(it) }.getOrDefault(TemperatureUnit.CELSIUS)
            } ?: TemperatureUnit.CELSIUS

            val speedUnit = preferences[PreferenceKeys.SPEED_UNIT]?.let {
                runCatching { SpeedUnit.valueOf(it) }.getOrDefault(SpeedUnit.KMH)
            } ?: SpeedUnit.KMH

            val appTheme = preferences[PreferenceKeys.APP_THEME]?.let {
                runCatching { AppTheme.valueOf(it) }.getOrDefault(AppTheme.SYSTEM)
            } ?: AppTheme.SYSTEM

            UserPreferences(tempUnit, speedUnit, appTheme)
        }

    suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.TEMP_UNIT] = unit.name
        }
    }

    suspend fun updateSpeedUnit(unit: SpeedUnit) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.SPEED_UNIT] = unit.name
        }
    }

    suspend fun updateAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.APP_THEME] = theme.name
        }
    }
}
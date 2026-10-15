package io.github.erexer.tropo.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(
    private val context: Context,
) {
    private val useCelsiusKey = booleanPreferencesKey("use_celsius")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val hourlyFieldsKey = stringSetPreferencesKey("hourly_fields")
    private val dailyStatsKey = stringSetPreferencesKey("daily_stats")
    private val dailyStatsVersionKey = intPreferencesKey("daily_stats_version")
    private val temperatureKey = stringPreferencesKey("temperature_unit")
    private val windKey = stringPreferencesKey("wind_unit")
    private val rainKey = stringPreferencesKey("rain_unit")
    private val graphLayersKey = stringSetPreferencesKey("graph_layers")
    private val currentFieldsKey = stringSetPreferencesKey("current_fields")

    val currentFields: Flow<Set<CurrentField>> =
        context.dataStore.data.map { CurrentField.decode(it[currentFieldsKey]) }

    suspend fun setCurrentFields(fields: Set<CurrentField>) {
        context.dataStore.edit { it[currentFieldsKey] = fields.map { field -> field.name }.toSet() }
    }

    val graphLayers: Flow<Set<GraphLayer>> = context.dataStore.data.map { GraphLayer.decode(it[graphLayersKey]) }

    suspend fun setGraphLayers(layers: Set<GraphLayer>) {
        context.dataStore.edit { it[graphLayersKey] = layers.map { layer -> layer.name }.toSet() }
    }

    val units: Flow<WeatherUnits> =
        context.dataStore.data.map { prefs ->
            val metric = prefs[useCelsiusKey] ?: true
            WeatherUnits(
                temperature =
                    TemperatureUnit.entries.firstOrNull { it.name == prefs[temperatureKey] }
                        ?: if (metric) TemperatureUnit.CELSIUS else TemperatureUnit.FAHRENHEIT,
                wind =
                    WindUnit.entries.firstOrNull { it.name == prefs[windKey] }
                        ?: if (metric) WindUnit.KMH else WindUnit.MPH,
                rain =
                    RainUnit.entries.firstOrNull { it.name == prefs[rainKey] }
                        ?: if (metric) RainUnit.MM else RainUnit.INCH,
            )
        }

    val hourlyFields: Flow<Set<HourlyField>> =
        context.dataStore.data.map { prefs ->
            HourlyField.decode(prefs[hourlyFieldsKey])
        }

    val dailyStats: Flow<Set<DailyStat>> =
        context.dataStore.data.map { prefs ->
            DailyStat.decode(prefs[dailyStatsKey], prefs[dailyStatsVersionKey] ?: 0)
        }

    val useCelsius: Flow<Boolean> = units.map { it.temperature == TemperatureUnit.CELSIUS }

    val themeMode: Flow<AppThemeMode> =
        context.dataStore.data.map { prefs ->
            AppThemeMode.entries.firstOrNull { it.name == prefs[themeModeKey] } ?: AppThemeMode.SYSTEM
        }

    suspend fun setUseCelsius(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[useCelsiusKey] = value
            prefs.remove(temperatureKey)
            prefs.remove(windKey)
            prefs.remove(rainKey)
        }
    }

    suspend fun setUnits(units: WeatherUnits) {
        context.dataStore.edit { prefs ->
            prefs[temperatureKey] = units.temperature.name
            prefs[windKey] = units.wind.name
            prefs[rainKey] = units.rain.name
            prefs[useCelsiusKey] = units.temperature == TemperatureUnit.CELSIUS
        }
    }

    suspend fun setTemperature(unit: TemperatureUnit) {
        context.dataStore.edit { it[temperatureKey] = unit.name }
    }

    suspend fun setWind(unit: WindUnit) {
        context.dataStore.edit { it[windKey] = unit.name }
    }

    suspend fun setRain(unit: RainUnit) {
        context.dataStore.edit { it[rainKey] = unit.name }
    }

    suspend fun setThemeMode(value: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = value.name
        }
    }

    suspend fun setHourlyFields(fields: Set<HourlyField>) {
        context.dataStore.edit { prefs ->
            prefs[hourlyFieldsKey] = fields.map { it.name }.toSet()
        }
    }

    suspend fun setDailyStats(stats: Set<DailyStat>) {
        context.dataStore.edit { prefs ->
            prefs[dailyStatsKey] = stats.map { it.name }.toSet()
            prefs[dailyStatsVersionKey] = DailyStat.SCHEMA_VERSION
        }
    }
}

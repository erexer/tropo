package io.github.erexer.tropo.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserPreferencesTest {
    @Test
    fun rapidUnitEditsPreserveOtherChoices() =
        runBlocking {
            val preferences = UserPreferences(ApplicationProvider.getApplicationContext())
            preferences.setUseCelsius(false)
            preferences.setTemperature(TemperatureUnit.CELSIUS)
            assertEquals(WeatherUnits(TemperatureUnit.CELSIUS, WindUnit.MPH, RainUnit.INCH), preferences.units.first())
            coroutineScope {
                launch { preferences.setTemperature(TemperatureUnit.FAHRENHEIT) }
                launch { preferences.setWind(WindUnit.MS) }
                launch { preferences.setRain(RainUnit.MM) }
            }
            assertEquals(WeatherUnits(TemperatureUnit.FAHRENHEIT, WindUnit.MS, RainUnit.MM), preferences.units.first())
            preferences.setUseCelsius(true)
        }

    @Test
    fun currentChoicesPersistIncludingEmptyAndDefaultToAll() =
        runBlocking {
            assertEquals(CurrentField.defaults, CurrentField.decode(null))
            assertEquals(emptySet<CurrentField>(), CurrentField.decode(setOf("obsolete")))
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = UserPreferences(context)
            preferences.setCurrentFields(setOf(CurrentField.HIGH_LOW, CurrentField.WIND))
            val selected = UserPreferences(context).currentFields.first()
            assertEquals(setOf(CurrentField.HIGH_LOW, CurrentField.WIND), selected)
            preferences.setCurrentFields(emptySet())
            assertEquals(emptySet<CurrentField>(), UserPreferences(context).currentFields.first())
            preferences.setCurrentFields(CurrentField.defaults)
        }

    @Test
    fun graphLayersPersistInStableOrder() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = UserPreferences(context)
            val layers = setOf(GraphLayer.WIND, GraphLayer.FEELS_LIKE)
            preferences.setGraphLayers(layers)
            assertEquals(layers, UserPreferences(context).graphLayers.first())
            assertEquals(emptySet<GraphLayer>(), GraphLayer.decode(setOf("obsolete")))
            preferences.setGraphLayers(emptySet())
        }

    @Test
    fun legacyDailyChoicesKeepSunTimesAndNewChoicesCanDisableThem() =
        runBlocking {
            assertEquals(setOf(DailyStat.UV_MAX, DailyStat.SUN_TIMES), DailyStat.decode(setOf("UV_MAX"), 0))
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = UserPreferences(context)
            preferences.setDailyStats(emptySet())
            assertEquals(emptySet<DailyStat>(), UserPreferences(context).dailyStats.first())
            preferences.setDailyStats(DailyStat.defaults)
        }

    @Test
    fun mixedUnitsPreserveLegacy() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = UserPreferences(context)
            preferences.setUseCelsius(false)
            assertEquals(WeatherUnits(TemperatureUnit.FAHRENHEIT, WindUnit.MPH, RainUnit.INCH), preferences.units.first())
            val mixed = WeatherUnits(TemperatureUnit.FAHRENHEIT, WindUnit.MS, RainUnit.MM)
            preferences.setUnits(mixed)
            assertEquals(mixed, UserPreferences(context).units.first())
            preferences.setUnits(mixed.copy(temperature = TemperatureUnit.CELSIUS))
            assertEquals(WindUnit.MS, preferences.units.first().wind)
            assertEquals(RainUnit.MM, preferences.units.first().rain)
            preferences.setUseCelsius(true)
        }

    @Test
    fun hourlyChoicesPersistIncludingNoFields() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = UserPreferences(context)
            val fields = setOf(HourlyField.HUMIDITY, HourlyField.AQI)
            preferences.setHourlyFields(fields)
            assertEquals(fields, UserPreferences(context).hourlyFields.first())
            preferences.setHourlyFields(emptySet())
            assertEquals(emptySet<HourlyField>(), UserPreferences(context).hourlyFields.first())
            preferences.setHourlyFields(HourlyField.defaults)
        }

    @Test
    fun missingChoicesDefaultButUnknownNamesDoNotCreateFields() {
        assertEquals(HourlyField.defaults, HourlyField.decode(null))
        assertEquals(setOf(HourlyField.UV), HourlyField.decode(setOf("UV", "obsolete")))
        assertEquals(emptySet<HourlyField>(), HourlyField.decode(emptySet()))
    }

    @Test
    fun appearancePersistsForNewReadersAndPreservesUnits() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = UserPreferences(context)
            preferences.setUseCelsius(false)
            AppThemeMode.entries.forEach { mode ->
                preferences.setThemeMode(mode)
                val reader = UserPreferences(context)
                assertEquals(mode, reader.themeMode.first())
                assertEquals(false, reader.useCelsius.first())
            }
            preferences.setThemeMode(AppThemeMode.SYSTEM)
            preferences.setUseCelsius(true)
        }
}

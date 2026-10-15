package io.github.erexer.tropo.data.local

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeatherDaoTest {
    private lateinit var database: TropoDatabase
    private lateinit var dao: WeatherDao

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TropoDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = database.weatherDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetWeather() =
        runBlocking {
            val entity = WeatherEntity(1L, 22.5, 0, 50, 10.0, System.currentTimeMillis())
            dao.insertWeather(entity)
            val result = dao.getWeather(1L)
            assertEquals(22.5, result?.temperature ?: 0.0, 0.01)
        }

    @Test
    fun widgetFlowsFollowSelection() =
        runBlocking {
            val first = LocationEntity(1L, "First", "", 1.0, 1.0)
            val second = LocationEntity(2L, "Second", "", 2.0, 2.0)
            dao.selectLocation(first)
            dao.insertWeather(WeatherEntity(1L, 20.0, 0, 50, 5.0, 1L))
            dao.insertWeather(WeatherEntity(2L, 10.0, 0, 50, 5.0, 1L))
            assertEquals(1L, dao.observeSelectedWeather().first()?.locationId)
            val newLocation = async { dao.observeSelectedLocation().first { it?.id == 2L } }
            val newWeather = async { dao.observeSelectedWeather().first { it?.locationId == 2L } }
            dao.selectLocation(second)
            assertEquals(second.copy(isSelected = true), newLocation.await())
            assertEquals(10.0, newWeather.await()?.temperature)
        }

    @Test
    fun cancelledSelectionRollsBack() =
        runBlocking {
            val first = LocationEntity(1L, "First", "", 1.0, 1.0, isSelected = true, isFavorite = true)
            val second = LocationEntity(2L, "Second", "", 2.0, 2.0)
            dao.insertLocations(listOf(first))
            try {
                database.withTransaction {
                    dao.selectLocation(second)
                    throw CancellationException("Interrupted selection")
                }
            } catch (_: CancellationException) {
                assertEquals(first, dao.getSelectedLocation())
                assertEquals(listOf(first), dao.getAllLocations())
            }
        }

    @Test
    fun competingSelectionsKeepOneAndFavorites() =
        runBlocking {
            val first = LocationEntity(1L, "First", "", 1.0, 1.0, isFavorite = true, favoriteOrder = 7)
            val second = LocationEntity(2L, "Second", "", 2.0, 2.0)
            dao.insertLocations(listOf(first))
            coroutineScope {
                launch { dao.selectLocation(first.copy(isFavorite = false, favoriteOrder = 0)) }
                launch { dao.selectLocation(second) }
            }
            assertEquals(1, dao.getAllLocations().count { it.isSelected })
            assertEquals(listOf(first), dao.getFavoriteLocations().map { it.copy(isSelected = false) })
        }

    @Test
    fun hourlyForecastsAreStoredPerLocation() =
        runBlocking {
            val parisForecast =
                HourlyForecastEntity(
                    locationId = 1L,
                    forecastTime = "2026-10-01T12:00",
                    temperature = 68f,
                    precipProb = 20f,
                    precipInches = 0.1f,
                    dewPoint = 55f,
                    windSpeed = 5f,
                    windDirectionDeg = 180f,
                    uvIndex = 4f,
                    fetchedAt = 100L,
                    rainInches = 0.05f,
                    snowCentimeters = 0.5f,
                    weatherCode = 73,
                    humidity = 65f,
                )
            val seattleForecast = parisForecast.copy(locationId = 2L, temperature = 60f)

            dao.replaceHourlyForecast(1L, listOf(parisForecast))
            dao.replaceHourlyForecast(2L, listOf(seattleForecast))

            assertEquals(68f, dao.getHourlyForecast(1L).single().temperature)
            assertEquals(60f, dao.getHourlyForecast(2L).single().temperature)
            assertEquals(0.05f, dao.getHourlyForecast(1L).single().rainInches)
            assertEquals(0.5f, dao.getHourlyForecast(1L).single().snowCentimeters)
            assertEquals(73, dao.getHourlyForecast(1L).single().weatherCode)
            assertEquals(65f, dao.getHourlyForecast(1L).single().humidity)
        }

    @Test
    fun dailyForecastsAndFavoriteOrderingArePersisted() =
        runBlocking {
            val first = LocationEntity(1L, "Boston", "United States", 42.36, -71.06, isFavorite = true)
            val second = LocationEntity(2L, "Seattle", "United States", 47.6, -122.3, isFavorite = true)
            dao.insertLocations(listOf(first, second))
            dao.updateFavoriteOrder(2L, 0)
            dao.updateFavoriteOrder(1L, 1)

            dao.insertDailyForecast(
                listOf(
                    DailyForecastEntity(
                        locationId = 1L,
                        date = "2026-10-05",
                        timezone = "America/New_York",
                        temperatureMax = 72f,
                        temperatureMin = 55f,
                        precipitationSum = 0.2f,
                        precipitationProbabilityMax = 40f,
                        sunrise = "2026-10-05T06:30",
                        sunset = "2026-10-05T18:10",
                        weatherCode = 2,
                        fetchedAt = 100L,
                    ),
                ),
            )
            dao.insertAirQuality(
                listOf(
                    AirQualityEntity(
                        locationId = 1L,
                        forecastTime = "2026-10-05T12:00",
                        timezone = "America/New_York",
                        usAqi = 42f,
                        pm25 = 8.5f,
                        fetchedAt = 100L,
                    ),
                ),
            )

            assertEquals(listOf(2L, 1L), dao.getFavoriteLocations().map { it.id })
            assertEquals("America/New_York", dao.getDailyForecast(1L).single().timezone)
            assertEquals(42f, dao.getAirQuality(1L).single().usAqi)
            dao.updateFavorite(2L, false, 0)
            assertEquals(listOf(1L), dao.getFavoriteLocations().map { it.id })
        }
}

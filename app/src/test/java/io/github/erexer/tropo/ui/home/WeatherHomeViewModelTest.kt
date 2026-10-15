package io.github.erexer.tropo.ui.home

import io.github.erexer.tropo.data.ForecastSnapshot
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.repository.LocationWeather
import io.github.erexer.tropo.data.repository.WeatherRefresh
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.ui.settings.FakeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherHomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshPreservesSelection() =
        runTest(dispatcher) {
            val repository = RefreshRepository()
            val model = WeatherHomeViewModel(repository)
            model.loadData()
            runCurrent()
            model.selectForecastPoint(0)
            val gate = CompletableDeferred<ForecastSnapshot>()
            repository.pending = gate
            model.loadData()
            runCurrent()
            assertTrue(model.uiState.value.isRefreshing)
            model.loadData()
            runCurrent()
            assertEquals(2, repository.requests)
            gate.complete(repository.snapshot)
            runCurrent()
            assertEquals(0, model.uiState.value.selectedForecastIndex)
            assertFalse(model.uiState.value.isRefreshing)
        }

    @Test
    fun refreshFailureKeepsData() =
        runTest(dispatcher) {
            val repository = RefreshRepository()
            val model = WeatherHomeViewModel(repository)
            model.loadData()
            runCurrent()
            model.selectForecastPoint(0)
            repository.failure = IOException("offline")
            model.loadData()
            runCurrent()
            assertEquals(repository.snapshot.hourly, model.uiState.value.forecast)
            assertEquals(0, model.uiState.value.selectedForecastIndex)
            assertEquals("offline", model.uiState.value.forecastErrorMessage)
            assertFalse(model.uiState.value.isRefreshing)
        }

    @Test
    fun locationCancelsOldRequests() =
        runTest(dispatcher) {
            val repository = RefreshRepository()
            val model = WeatherHomeViewModel(repository)
            model.loadData()
            runCurrent()
            repository.pending = CompletableDeferred()
            model.loadData()
            runCurrent()
            repository.location = repository.location.copy(id = 2L, name = "New city")
            val newGate = CompletableDeferred<ForecastSnapshot>()
            repository.pending = newGate
            model.reloadLocation()
            runCurrent()
            assertEquals(
                2L,
                model.uiState.value.location
                    ?.id,
            )
            assertNull(model.uiState.value.weather)
            assertTrue(
                model.uiState.value.forecast
                    .isEmpty(),
            )
            newGate.complete(repository.snapshot)
            runCurrent()
            assertEquals(
                2L,
                model.uiState.value.weather
                    ?.locationId,
            )
        }

    @Test
    fun nowCanRepeatNavigation() =
        runTest(dispatcher) {
            val model = WeatherHomeViewModel(RefreshRepository())
            model.loadData()
            runCurrent()
            model.selectNow()
            model.selectNow()
            assertEquals(2, model.uiState.value.nowRevision)
        }

    @Test
    fun resumeRefreshesOnlyStaleData() =
        runTest(dispatcher) {
            val repository = RefreshRepository()
            val model = WeatherHomeViewModel(repository)
            model.loadData()
            runCurrent()
            val fetchedAt = requireNotNull(model.uiState.value.weather).updatedAt
            model.refreshIfStale(fetchedAt + 60_000L)
            runCurrent()
            assertEquals(1, repository.requests)
            repository.pending = CompletableDeferred()
            model.refreshIfStale(fetchedAt + 900_001L)
            runCurrent()
            model.refreshIfStale(fetchedAt + 900_001L)
            runCurrent()
            assertEquals(2, repository.requests)
            repository.pending?.complete(repository.snapshot)
            runCurrent()
        }

    @Test
    fun cachedCurrentIsIndependentOfForecast() =
        runTest(dispatcher) {
            val repository = RefreshRepository().apply { cachedCurrent = true }
            val model = WeatherHomeViewModel(repository)
            model.loadData()
            runCurrent()
            assertTrue(model.uiState.value.isWeatherCached)
            assertFalse(model.uiState.value.isForecastCached)
        }

    private class RefreshRepository : WeatherRepository by FakeRepository() {
        var cachedCurrent = false
        var location = LocationEntity(1L, "First city", "Country", 0.0, 0.0, true)
        var pending: CompletableDeferred<ForecastSnapshot>? = null
        var failure: Throwable? = null
        var requests = 0
        private val hour = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS)
        val snapshot =
            ForecastSnapshot(
                listOf(
                    HourlyPoint(hour.minusHours(1).toString(), "00", 68f, 0f, 0f, 45f, 5f, 180f, 0f),
                    HourlyPoint(hour.toString(), "01", 66f, 0f, 0f, 45f, 5f, 180f, 0f),
                ),
                emptyList(),
                "UTC",
                System.currentTimeMillis(),
                false,
            )

        override suspend fun getSelectedLocation(): LocationEntity = location

        override suspend fun getCurrentWeather(): WeatherEntity =
            WeatherEntity(
                location.id,
                20.0,
                0,
                50,
                5.0,
                System.currentTimeMillis(),
            )

        override suspend fun currentReading(refresh: WeatherRefresh): LocationWeather =
            LocationWeather(
                getCurrentWeather(),
                cachedCurrent,
            )

        override suspend fun getForecast(forecastDays: Int): Result<ForecastSnapshot> {
            requests++
            failure?.let { return Result.failure(it) }
            return Result.success(pending?.await() ?: snapshot)
        }
    }
}

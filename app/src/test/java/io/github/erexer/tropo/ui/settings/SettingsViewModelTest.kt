package com.tropo.ui.settings

import app.cash.turbine.test
import com.tropo.data.preferences.TemperatureUnit
import com.tropo.data.preferences.UserPreferences
import com.tropo.data.preferences.UserPreferencesRepository
import com.tropo.data.remote.GeocodingApiClient
import com.tropo.data.remote.GeocodingLocation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val preferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val geocodingClient: GeocodingApiClient = mockk()

    private val prefsFlow = MutableStateFlow(UserPreferences())

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { preferencesRepository.userPreferencesFlow } returns prefsFlow

        viewModel = SettingsViewModel(preferencesRepository, geocodingClient)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `userPreferences emits updated values from repository`() = runTest {
        viewModel.userPreferences.test {
            assertEquals(TemperatureUnit.CELSIUS, awaitItem().temperatureUnit)

            prefsFlow.value = UserPreferences(temperatureUnit = TemperatureUnit.FAHRENHEIT)
            assertEquals(TemperatureUnit.FAHRENHEIT, awaitItem().temperatureUnit)
        }
    }

    @Test
    fun `setTemperatureUnit calls repository update`() = runTest {
        viewModel.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesRepository.updateTemperatureUnit(TemperatureUnit.FAHRENHEIT) }
    }

    @Test
    fun `searchResults transitions to Success when valid query typed`() = runTest {
        val mockLocation = GeocodingLocation(
            id = 1L,
            name = "Seattle",
            latitude = 47.6062,
            longitude = -122.3321,
            country = "United States"
        )
        coEvery { geocodingClient.searchLocations("Seattle") } returns listOf(mockLocation)

        viewModel.searchResults.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChanged("Seattle")
            testDispatcher.scheduler.advanceTimeBy(350)

            val state = awaitItem()
            assert(state is SearchUiState.Success)
            assertEquals("Seattle", (state as SearchUiState.Success).locations.first().name)
        }
    }
}
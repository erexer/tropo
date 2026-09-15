package io.github.erexer.tropo.ui

import app.cash.turbine.test
import io.github.erexer.tropo.data.HourlyPoint
import io.github.erexer.tropo.data.WeatherRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: WeatherRepository = mockk()

    private val samplePoints = listOf(
        HourlyPoint(
            hourLabel = "12:00",
            temperature = 72f,
            precipProb = 10f,
            precipInches = 0.0f,
            dewPoint = 55f,
            windSpeed = 8f,
            windDirectionDeg = 180f,
            uvIndex = 5f
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchForecast updates uiState with cached then remote points`() = runTest {
        coEvery { repository.getHourlyForecast(any(), any()) } returns flowOf(
            Result.success(samplePoints)
        )

        val viewModel = WeatherViewModel(repository)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)

            testDispatcher.scheduler.advanceUntilIdle()

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(1, successState.points.size)
            assertEquals(72f, successState.points[0].temperature)
        }
    }

    @Test
    fun `toggleUv updates state correctly`() = runTest {
        coEvery { repository.getHourlyForecast(any(), any()) } returns flowOf(
            Result.success(samplePoints)
        )

        val viewModel = WeatherViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showUv)
        viewModel.toggleUv()
        assertTrue(viewModel.uiState.value.showUv)
    }

    @Test
    fun `repository failure updates error message state`() = runTest {
        val errorMessage = "Network Error"
        coEvery { repository.getHourlyForecast(any(), any()) } returns flowOf(
            Result.failure(RuntimeException(errorMessage))
        )

        val viewModel = WeatherViewModel(repository)

        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(errorMessage, errorState.errorMessage)
        }
    }
}
package io.github.erexer.tropo.ui.locations

import androidx.lifecycle.ViewModelStore
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.preferences.DailyStat
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.ui.locations.LocationsViewModel
import io.github.erexer.tropo.ui.settings.FakeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationsViewModelTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearingLocationOwnerCancelsActivePositionRequest() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val repository =
                object : WeatherRepository by FakeRepository() {
                    var cancelled = false

                    override suspend fun deviceLocation(): Result<LocationEntity> {
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled = true
                        }
                    }
                }
            val store = ViewModelStore()
            try {
                val model = LocationsViewModel(repository)
                store.put("locations", model)
                model.useDeviceLocation { error("Cancelled request navigated") }
                assertTrue(model.usingDeviceLocation.value)
                store.clear()
                runCurrent()
                assertTrue(repository.cancelled)
                assertEquals(false, model.usingDeviceLocation.value)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun deviceFailureAndPermissionDenialPreserveSelection() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repository = DeviceRepository()
            val model = LocationsViewModel(repository)
            model.loadLocations()
            model.locationPermissionDenied()
            model.useDeviceLocation { error("Failure navigated") }
            assertEquals(repository.original, model.selectedLocation.value)
            assertEquals(null, repository.selected)
            repository.result = Result.success(repository.original.copy(id = -1, name = "Device"))
            model.useDeviceLocation {}
            assertEquals(-1L, repository.selected?.id)
            assertEquals(false, repository.selected?.isFavorite)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun queryStateUpdatesOnInput() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val store = ViewModelStore()
        try {
            val viewModel = LocationsViewModel(FakeRepository())
            store.put("locations", viewModel)
            viewModel.onQueryChange("Paris")
            assertEquals("Paris", viewModel.searchQuery.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun reorderFavoritesPersistsNewOrder() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repository = ReorderRepository()
            val viewModel = LocationsViewModel(repository)
            viewModel.reorderFavorites(listOf(3L, 1L, 2L))
            assertEquals(listOf(3L, 1L, 2L), repository.reordered)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class DeviceRepository : WeatherRepository by FakeRepository() {
    val original = LocationEntity(1, "Original", "", 1.0, 1.0, isSelected = true)
    var result: Result<LocationEntity> = Result.failure(IllegalStateException("Position unavailable"))
    var selected: LocationEntity? = null

    override suspend fun getSelectedLocation(): LocationEntity = selected ?: original

    override suspend fun deviceLocation(): Result<LocationEntity> = result

    override suspend fun selectLocation(location: LocationEntity) {
        selected = location
    }
}

private class ReorderRepository : WeatherRepository by FakeRepository() {
    var reordered: List<Long>? = null
    var dailyStats: Set<DailyStat>? = null

    override suspend fun reorderFavorites(locationIds: List<Long>) {
        reordered = locationIds
    }

    override suspend fun setDailyStats(stats: Set<DailyStat>) {
        dailyStats = stats
    }
}

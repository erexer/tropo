package io.github.erexer.tropo.ui.settings

import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.local.WeatherEntity
import io.github.erexer.tropo.data.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeRepository : WeatherRepository {
    override suspend fun getCurrentWeather(): WeatherEntity? = null
    override suspend fun getSelectedLocation(): LocationEntity? = null
    override suspend fun searchLocations(query: String): List<LocationEntity> = emptyList()
    override suspend fun selectLocation(location: LocationEntity) {}
    override fun isCelsius(): Flow<Boolean> = flowOf(true)
    override suspend fun setCelsius(isCelsius: Boolean) {}
}

class SettingsViewModelTest {
    @Test
    fun queryStateUpdatesOnInput() {
        val viewModel = SettingsViewModel(FakeRepository())
        viewModel.onQueryChange("Paris")
        assertEquals("Paris", viewModel.searchQuery.value)
    }
}
package io.github.erexer.tropo.ui.settings

import io.github.erexer.tropo.data.preferences.DailyStat
import io.github.erexer.tropo.data.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun dailyStatsAreSaved() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repository = ReorderRepository()
            SettingsViewModel(repository).setDailyStats(setOf(DailyStat.DAYLIGHT))
            assertEquals(setOf(DailyStat.DAYLIGHT), repository.dailyStats)
        } finally {
            Dispatchers.resetMain()
        }
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

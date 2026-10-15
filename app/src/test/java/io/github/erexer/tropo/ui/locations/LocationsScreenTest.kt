package io.github.erexer.tropo.ui.locations

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntil
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.ui.settings.FakeRepository
import io.github.erexer.tropo.ui.theme.TropoTheme
import io.github.erexer.tropo.data.preferences.AppThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocationsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun clearingQueryAfterFavoriteRefreshDoesNotCrash() {
        val repository = SearchRepository()
        compose.setContent {
            TropoTheme(AppThemeMode.LIGHT) {
                LocationsScreen(repository, onBack = {}, onSelected = {})
            }
        }
        compose.onNodeWithText("Search city").performTextInput("Seattle")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Save Seattle to favorites").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Save Seattle to favorites").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Remove Seattle from favorites").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Locations").assertIsDisplayed()
    }

    private class SearchRepository : WeatherRepository by FakeRepository() {
        private val city = LocationEntity(770_001L, "Seattle", "United States", 47.61, -122.33)
        private val favorites = mutableListOf<LocationEntity>()

        override suspend fun searchLocations(query: String): List<LocationEntity> =
            if (query.equals("Seattle", ignoreCase = true)) {
                listOf(city.copy(isFavorite = favorites.any { it.id == city.id }))
            } else {
                emptyList()
            }

        override suspend fun getFavoriteLocations(): List<LocationEntity> = favorites.toList()

        override suspend fun setFavorite(location: LocationEntity, isFavorite: Boolean) {
            favorites.removeAll { it.id == location.id }
            if (isFavorite) favorites += location.copy(isFavorite = true)
        }
    }
}
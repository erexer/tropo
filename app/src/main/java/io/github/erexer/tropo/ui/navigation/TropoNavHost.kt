package io.github.erexer.tropo.ui.navigation

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.erexer.tropo.di.AppContainer
import io.github.erexer.tropo.ui.home.WeatherHomeScreen
import io.github.erexer.tropo.ui.locations.LocationsScreen
import io.github.erexer.tropo.ui.settings.SettingsScreen

@Composable
fun TropoNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val refreshKey = remember { mutableIntStateOf(0) }

    NavHost(navController = navController, startDestination = "home", modifier = Modifier.systemBarsPadding()) {
        composable("home") {
            WeatherHomeScreen(
                repository = appContainer.weatherRepository,
                refreshKey = refreshKey.intValue,
                onOpenSettings = { navController.navigate("settings") },
                onOpenLocations = { navController.navigate("locations") },
            )
        }
        composable("settings") {
            SettingsScreen(
                repository = appContainer.weatherRepository,
                onBack = { navController.popBackStack() },
            )
        }
        composable("locations") {
            LocationsScreen(
                repository = appContainer.weatherRepository,
                onBack = { navController.popBackStack() },
                onSelected = {
                    refreshKey.intValue++
                    navController.popBackStack()
                },
            )
        }
    }
}

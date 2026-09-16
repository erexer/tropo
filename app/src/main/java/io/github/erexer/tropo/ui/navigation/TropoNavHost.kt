package io.github.erexer.tropo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.erexer.tropo.di.AppContainer
import io.github.erexer.tropo.ui.home.WeatherHomeScreen
import io.github.erexer.tropo.ui.settings.SettingsScreen

@Composable
fun TropoNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            WeatherHomeScreen(
                repository = appContainer.weatherRepository,
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                repository = appContainer.weatherRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
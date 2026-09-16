package io.github.erexer.tropo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.ui.canvas.WeatherParticleCanvas
import io.github.erexer.tropo.ui.theme.LocalTropoColors

@Composable
fun WeatherHomeScreen(repository: WeatherRepository, onOpenSettings: () -> Unit) {
    val viewModel = remember { WeatherHomeViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalTropoColors.current

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        WeatherParticleCanvas()

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.location?.name ?: "Unknown",
                        color = colors.textPrimary,
                        fontSize = 28.sp
                    )
                    Button(onClick = onOpenSettings) {
                        Text("Settings")
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val temp = uiState.weather?.temperature ?: 0.0
                    val displayTemp = if (uiState.isCelsius) temp else (temp * 9 / 5) + 32
                    val unit = if (uiState.isCelsius) "°C" else "°F"

                    Text(
                        text = "${displayTemp.toInt()}$unit",
                        color = colors.textPrimary,
                        fontSize = 72.sp
                    )
                    Text(
                        text = "Wind: ${uiState.weather?.windSpeed ?: 0.0} km/h",
                        color = colors.textSecondary,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
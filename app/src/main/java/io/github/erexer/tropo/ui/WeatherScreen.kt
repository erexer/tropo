package io.github.erexer.tropo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tropo", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.points.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        uiState.errorMessage?.let { err ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = err, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val selected = uiState.selectedIndex?.let { uiState.points.getOrNull(it) }
            selected?.let { pt ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Time: ${pt.hourLabel}", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(text = "${pt.temperature.toInt()}°F", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Rain: ${pt.precipProb.toInt()}% (${pt.precipInches} in)", color = MaterialTheme.colorScheme.primary)
                        Text(text = "Wind: ${pt.windSpeed.toInt()} mph", fontSize = 14.sp)
                        Text(text = "UV Index: ${pt.uvIndex.toInt()}", fontSize = 14.sp)
                        Text(text = "Dew Point: ${pt.dewPoint.toInt()}°F", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            WeatherChart(
                points = uiState.points,
                showUv = uiState.showUv,
                showDewPoint = uiState.showDewPoint,
                showWind = uiState.showWind,
                selectedIndex = uiState.selectedIndex,
                onPointSelected = { idx -> viewModel.selectPoint(idx) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.showUv,
                    onClick = { viewModel.toggleUv() },
                    label = { Text("UV Index") }
                )
                FilterChip(
                    selected = uiState.showDewPoint,
                    onClick = { viewModel.toggleDewPoint() },
                    label = { Text("Dew Point") }
                )
                FilterChip(
                    selected = uiState.showWind,
                    onClick = { viewModel.toggleWind() },
                    label = { Text("Wind") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(uiState.points) { idx, pt ->
                    ElevatedCard(
                        onClick = { viewModel.selectPoint(idx) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (idx == uiState.selectedIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.width(70.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = pt.hourLabel, fontSize = 12.sp)
                            Text(text = "${pt.temperature.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "${pt.precipProb.toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
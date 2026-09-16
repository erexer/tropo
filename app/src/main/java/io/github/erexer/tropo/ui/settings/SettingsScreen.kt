package io.github.erexer.tropo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.erexer.tropo.data.repository.WeatherRepository

@Composable
fun SettingsScreen(repository: WeatherRepository, onBack: () -> Unit) {
    val viewModel = remember { SettingsViewModel(repository) }
    val isCelsius by viewModel.isCelsius.collectAsState(initial = true)
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use Celsius (°C)")
            Switch(checked = isCelsius, onCheckedChange = { viewModel.toggleUnit(it) })
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            label = { Text("Search City") },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(searchResults) { location ->
                Text(
                    text = "${location.name}, ${location.country}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectLocation(location) }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
package io.github.erexer.tropo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.erexer.tropo.data.preferences.AppThemeMode
import io.github.erexer.tropo.data.preferences.CurrentField
import io.github.erexer.tropo.data.preferences.DailyStat
import io.github.erexer.tropo.data.preferences.HourlyField
import io.github.erexer.tropo.data.preferences.RainUnit
import io.github.erexer.tropo.data.preferences.TemperatureUnit
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.preferences.WindUnit
import io.github.erexer.tropo.data.repository.WeatherRepository

@Composable
@Suppress("FunctionNaming", "LongMethod")
fun SettingsScreen(
    repository: WeatherRepository,
    onBack: () -> Unit,
) {
    val viewModel: SettingsViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer { SettingsViewModel(repository) }
                },
        )
    val units by viewModel.units.collectAsState(initial = WeatherUnits())
    val fields by viewModel.hourlyFields.collectAsState(initial = HourlyField.defaults)
    val dailyStats by viewModel.dailyStats.collectAsState(initial = DailyStat.defaults)
    val currentFields by viewModel.currentFields.collectAsState(initial = CurrentField.defaults)
    val themeMode by viewModel.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
    val error by viewModel.preferenceError.collectAsState()
    var showAbout by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    if (showAbout) {
        AboutTropo { showAbout = false }
    }
    if (showReport) {
        IssueReportDialog { showReport = false }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(onClick = { showReport = true }) {
                    Icon(Icons.Default.Email, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Report an issue")
                }
            }
            item {
                TextButton(onClick = { showAbout = true }) { Text("About / Privacy") }
            }
            item {
                UnitChoices("Temperature", TemperatureUnit.entries.map { it.label }, units.temperature.ordinal) {
                    viewModel.setTemperature(TemperatureUnit.entries[it])
                }
            }
            item {
                UnitChoices("Wind speed", WindUnit.entries.map { it.label }, units.wind.ordinal) {
                    viewModel.setWind(WindUnit.entries[it])
                }
            }
            item {
                UnitChoices("Rain / liquid equivalent", RainUnit.entries.map { it.label }, units.rain.ordinal) {
                    viewModel.setRain(RainUnit.entries[it])
                }
            }
            item {
                UnitChoices("Appearance", listOf("System", "Light", "Dark"), themeMode.ordinal) {
                    viewModel.setThemeMode(AppThemeMode.entries[it])
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Current weather",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { viewModel.setCurrentFields(CurrentField.defaults) }) { Text("Reset") }
                }
            }
            CurrentField.entries.forEach { field ->
                item(key = "current_${field.name}") {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().toggleable(
                                value = field in currentFields,
                                role = Role.Checkbox,
                                onValueChange = { checked ->
                                    val selected = if (checked) currentFields + field else currentFields - field
                                    viewModel.setCurrentFields(selected)
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = field in currentFields, onCheckedChange = null)
                        Text(field.label)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily stats", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { viewModel.setDailyStats(DailyStat.defaults) }) { Text("Reset") }
                }
            }
            DailyStat.entries.forEach { stat ->
                item(key = stat.name) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().toggleable(
                                value = stat in dailyStats,
                                role = Role.Checkbox,
                                onValueChange = { checked ->
                                    viewModel.setDailyStats(if (checked) dailyStats + stat else dailyStats - stat)
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = stat in dailyStats, onCheckedChange = null)
                        Text(stat.label)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hourly fields", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { viewModel.setHourlyFields(HourlyField.defaults) }) { Text("Reset") }
                }
            }
            HourlyField.entries.forEach { field ->
                item(key = "hourly_${field.name}") {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().toggleable(
                                value = field in fields,
                                role = Role.Checkbox,
                                onValueChange = { checked ->
                                    viewModel.setHourlyFields(if (checked) fields + field else fields - field)
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = field in fields, onCheckedChange = null)
                        Text(field.label)
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun UnitChoices(
    title: String,
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                label = { Text(label) },
            )
        }
    }
}

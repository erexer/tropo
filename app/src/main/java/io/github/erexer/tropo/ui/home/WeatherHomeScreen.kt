package io.github.erexer.tropo.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.erexer.tropo.data.LocationLabel
import io.github.erexer.tropo.data.UpdatedTimeFormatter
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.ui.common.AttributionLink
import io.github.erexer.tropo.ui.common.CurrentLocationAction
import io.github.erexer.tropo.ui.common.LocationActionStyle
import io.github.erexer.tropo.ui.locations.locationsModel
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming")
fun WeatherHomeScreen(
    repository: WeatherRepository,
    refreshKey: Int,
    onOpenSettings: () -> Unit,
    onOpenLocations: () -> Unit,
) {
    val viewModel = weatherHomeModel(repository)
    val locationModel = locationsModel(repository)
    val locationError by locationModel.selectionError.collectAsState()
    DisposableEffect(locationModel) { onDispose { locationModel.cancelLocationRequests() } }
    val uiState by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val collapse = temperatureCollapse(listState)
    val filters = ForecastFilterState(uiState.graphLayers)
    val clock by produceState(Instant.now()) {
        while (true) {
            delay(CLOCK_TICK_MILLIS - System.currentTimeMillis() % CLOCK_TICK_MILLIS)
            value = Instant.now()
        }
    }

    LaunchedEffect(refreshKey) {
        viewModel.loadForRevision(refreshKey)
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column {
            HomeHeader(uiState, onOpenLocations, onOpenSettings, collapse) {
                CurrentLocationAction(locationModel, viewModel::reloadLocation, LocationActionStyle.ICON)
            }
            locationError?.let {
                Text(it, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
            }
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::loadData,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (uiState.isLoading && uiState.weather == null && uiState.forecast.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = SCREEN_HORIZONTAL_PADDING_DP.dp),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            CurrentConditions(uiState, colors, viewModel::loadData, collapse, clock)
                        }

                        if (uiState.forecast.isNotEmpty()) {
                            item {
                                ForecastSection(
                                    state = uiState,
                                    filters = filters,
                                    clock = clock,
                                    actions =
                                        ForecastActions(
                                            onFiltersChanged = { viewModel.setGraphLayers(it.layers) },
                                            onPointSelected = viewModel::selectForecastPoint,
                                            onHourlyFieldsChanged = viewModel::setHourlyFields,
                                            onNow = viewModel::selectNow,
                                            onRetry = viewModel::loadData,
                                        ),
                                    textColor = colors.onSurface,
                                )
                            }
                        } else if (uiState.forecastErrorMessage != null) {
                            item {
                                ForecastError()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun weatherHomeModel(repository: WeatherRepository): WeatherHomeViewModel =
    viewModel(factory = viewModelFactory { initializer { WeatherHomeViewModel(repository) } })

@Composable
private fun temperatureCollapse(state: LazyListState): Float {
    val threshold = with(LocalDensity.current) { TEMPERATURE_COLLAPSE_DISTANCE_DP.dp.toPx() }
    val progress by remember(state, threshold) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) {
                1f
            } else {
                (state.firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
            }
        }
    }
    return animateFloatAsState(progress, label = "Temperature collapse").value
}

@Composable
@Suppress("FunctionNaming")
private fun HomeHeader(
    state: WeatherUiState,
    onOpenLocations: () -> Unit,
    onOpenSettings: () -> Unit,
    collapse: Float,
    locationAction: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpenLocations, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.LocationOn, "Change location", modifier = Modifier.size(20.dp))
                Text(
                    state.location?.let(LocationLabel::of) ?: "Choose location",
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
            }
            CollapsedConditions(state, collapse)
            locationAction()
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings") }
        }
        val updated =
            state.forecastUpdatedAt?.let {
                UpdatedTimeFormatter.format(it, state.forecastTimezone)
            }
        val status =
            when {
                state.isLoading -> "Loading"
                state.isRefreshing -> "Updating"
                state.isForecastCached -> "Saved forecast · ${updated.orEmpty()}"
                updated != null -> "Forecast updated $updated"
                else -> ""
            }
        val currentStatus =
            state.weather?.let { weather ->
                val time = UpdatedTimeFormatter.format(weather.updatedAt, state.forecastTimezone)
                "${if (state.isWeatherCached) "Saved conditions" else "Conditions updated"} $time"
            }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOfNotNull(status.takeIf { it.isNotEmpty() }, currentStatus).joinToString("\n"),
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            AttributionLink()
        }
    }
}

private const val CLOCK_TICK_MILLIS = 60_000L
internal const val SCREEN_HORIZONTAL_PADDING_DP = 24
private const val TEMPERATURE_COLLAPSE_DISTANCE_DP = 100

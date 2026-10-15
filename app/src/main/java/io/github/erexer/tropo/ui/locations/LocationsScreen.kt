package io.github.erexer.tropo.ui.locations

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.repository.WeatherRefresh
import io.github.erexer.tropo.data.repository.WeatherRepository
import io.github.erexer.tropo.ui.common.AttributionLink
import io.github.erexer.tropo.ui.common.CurrentLocationAction
import io.github.erexer.tropo.ui.common.LocationActionStyle
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod")
fun LocationsScreen(
    repository: WeatherRepository,
    onBack: () -> Unit,
    onSelected: () -> Unit,
) {
    val model = locationsModel(repository)
    val query by model.searchQuery.collectAsState()
    val results by model.searchResults.collectAsState()
    val favorites by model.favorites.collectAsState()
    val searchError by model.searchError.collectAsState()
    val selectionError by model.selectionError.collectAsState()
    val isSearching by model.isSearching.collectAsState()
    val current by model.selectedLocation.collectAsState()
    val readings by model.readings.collectAsState()
    val weatherLoading by model.weatherLoading.collectAsState()
    val units by model.units.collectAsState(initial = WeatherUnits())
    val order = remember { mutableStateListOf<LocationEntity>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val resultIds = results.map { it.id }
    LaunchedEffect(resultIds) {
        if (query.length >= MIN_SEARCH_LENGTH && resultIds.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }
    val haptics = LocalHapticFeedback.current
    val spacingPx = with(LocalDensity.current) { ITEM_SPACING_DP.dp.toPx() }
    LaunchedEffect(Unit) { model.loadLocations() }
    LaunchedEffect(favorites, current) { model.refreshWeather(WeatherRefresh.STALE) }
    DisposableEffect(model) { onDispose { model.cancelLocationRequests() } }
    LaunchedEffect(favorites) {
        if (draggingId == null) {
            order.clear()
            order.addAll(favorites)
        }
    }

    fun finishDrag() {
        draggingId = null
        dragOffset = 0f
        model.reorderFavorites(order.map { it.id })
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Locations", style = MaterialTheme.typography.headlineSmall)
        }
        SearchField(query, isSearching, model::onQueryChange) {
            scope.launch { listState.animateScrollToItem(0) }
        }
        DeviceLocationButton(model, onSelected)
        listOfNotNull(searchError, selectionError).forEach { message ->
            Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }
        PullToRefreshBox(
            isRefreshing = weatherLoading,
            onRefresh = { model.refreshWeather(WeatherRefresh.FORCE) },
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(ITEM_SPACING_DP.dp),
            ) {
                if (query.length >= MIN_SEARCH_LENGTH) {
                    item(key = "search-header") { SectionLabel("Search results") }
                    if (results.isEmpty() && !isSearching && searchError == null) {
                        item(key = "search-empty") {
                            Text("No matches", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(results, key = { "search-${it.id}" }) { location ->
                        val saved = location.isFavorite || order.any { it.id == location.id }
                        LocationCard(
                            location = location,
                            selected = location.id == current?.id,
                            onClick = { model.selectLocation(location, onSelected) },
                            modifier = Modifier.animateItem(),
                            leading = { Icon(Icons.Default.LocationOn, null) },
                            trailing = {
                                FavoriteButton(location.name, saved) {
                                    model.toggleFavorite(location.copy(isFavorite = saved))
                                }
                            },
                        )
                    }
                }
                current?.takeIf { location -> order.none { it.id == location.id } }?.let { location ->
                    item(key = "current-header") { SectionLabel("Current location") }
                    item(key = "current-${location.id}") {
                        val saved = order.any { it.id == location.id }
                        LocationCard(
                            location = location,
                            selected = true,
                            reading = readings[location.id],
                            units = units,
                            onClick = onBack,
                            leading = {
                                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailing = {
                                FavoriteButton(location.name, saved) {
                                    model.toggleFavorite(location.copy(isFavorite = saved))
                                }
                            },
                        )
                    }
                }
                item(key = "favorites-header") { SectionLabel("Favorites") }
                if (order.isEmpty()) {
                    item(key = "favorites-empty") {
                        Text(
                            "Tap the heart on a location to save it here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(order, key = { _, location -> "favorite-${location.id}" }) { index, location ->
                    val key = "favorite-${location.id}"
                    val itemModifier =
                        if (draggingId == location.id) {
                            Modifier.zIndex(1f).graphicsLayer {
                                translationY = dragOffset
                                shadowElevation = DRAG_ELEVATION_DP.dp.toPx()
                            }
                        } else {
                            Modifier.animateItem()
                        }
                    LocationCard(
                        location = location,
                        selected = location.id == current?.id,
                        reading = readings[location.id],
                        units = units,
                        onClick = { model.selectLocation(location, onSelected) },
                        modifier =
                            itemModifier.semantics {
                                customActions =
                                    listOfNotNull(
                                        if (index > 0) {
                                            CustomAccessibilityAction("Move ${location.name} up") {
                                                model.moveFavorite(location.id, -1)
                                                true
                                            }
                                        } else {
                                            null
                                        },
                                        if (index < order.lastIndex) {
                                            CustomAccessibilityAction("Move ${location.name} down") {
                                                model.moveFavorite(location.id, 1)
                                                true
                                            }
                                        } else {
                                            null
                                        },
                                    )
                            },
                        leading = {
                            Icon(
                                Icons.Default.Menu,
                                "Drag to reorder ${location.name}",
                                modifier =
                                    Modifier
                                        .size(DRAG_HANDLE_DP.dp)
                                        .padding(8.dp)
                                        .pointerInput(location.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingId = location.id
                                                    dragOffset = 0f
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragOffset += amount.y
                                                    val itemSize =
                                                        listState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull { it.key == key }
                                                            ?.size
                                                            ?.plus(spacingPx)
                                                    val from = order.indexOfFirst { it.id == location.id }
                                                    val crossed = itemSize != null && abs(dragOffset) > itemSize / 2f
                                                    if (crossed && from >= 0) {
                                                        val to = if (dragOffset > 0) from + 1 else from - 1
                                                        if (to in order.indices) {
                                                            order.add(to, order.removeAt(from))
                                                            dragOffset -= if (to > from) itemSize else -itemSize
                                                        }
                                                    }
                                                },
                                                onDragEnd = ::finishDrag,
                                                onDragCancel = ::finishDrag,
                                            )
                                        },
                            )
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FavoriteButton(location.name, saved = true) { model.toggleFavorite(location) }
                                FavoriteMenu(location.name, index, order.lastIndex) { direction ->
                                    model.moveFavorite(location.id, direction)
                                }
                            }
                        },
                    )
                }
            }
        }
        AttributionLink()
    }
}

@Composable
@Suppress("FunctionNaming")
private fun DeviceLocationButton(
    model: LocationsViewModel,
    onSelected: () -> Unit,
) {
    CurrentLocationAction(model, onSelected, LocationActionStyle.LABEL)
}

@Composable
@Suppress("FunctionNaming")
private fun SearchField(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search city") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            when {
                isSearching -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                query.isNotEmpty() ->
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear search")
                    }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            KeyboardActions(onSearch = {
                onSearch()
                keyboard?.hide()
                focus.clearFocus()
            }),
        shape = RoundedCornerShape(SEARCH_CORNER_DP.dp),
        modifier =
            Modifier.fillMaxWidth().padding(top = 8.dp).onFocusChanged {
                if (it.isFocused && !focused) {
                    onSearch()
                }
                focused = it.isFocused
            },
    )
}

@Composable
@Suppress("FunctionNaming")
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}

private const val MIN_SEARCH_LENGTH = 2
private const val ITEM_SPACING_DP = 8
private const val SEARCH_CORNER_DP = 28
private const val DRAG_HANDLE_DP = 40
private const val DRAG_ELEVATION_DP = 8

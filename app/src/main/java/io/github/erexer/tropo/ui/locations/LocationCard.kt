package io.github.erexer.tropo.ui.locations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erexer.tropo.data.LocationLabel
import io.github.erexer.tropo.data.WeatherCondition
import io.github.erexer.tropo.data.local.LocationEntity
import io.github.erexer.tropo.data.preferences.WeatherUnits
import io.github.erexer.tropo.data.repository.LocationWeather
import io.github.erexer.tropo.ui.common.WeatherIcon
import io.github.erexer.tropo.ui.theme.WeatherPalette
import java.util.Locale
import kotlin.math.abs

@Composable
@Suppress("FunctionNaming", "LongParameterList")
internal fun LocationCard(
    location: LocationEntity,
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    reading: LocationWeather? = null,
    units: WeatherUnits = WeatherUnits(),
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (selected) {
                        Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                    }
                    Text(
                        LocationLabel.of(location),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier.semantics {
                                if (selected) {
                                    contentDescription = "${LocationLabel.of(location)}, selected"
                                }
                            },
                    )
                }
                Text(
                    location.country.ifBlank { coordinates(location) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (reading != null) LocationWeatherSummary(reading, units)
            trailing()
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun LocationWeatherSummary(
    reading: LocationWeather,
    units: WeatherUnits,
) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Column(
        modifier = Modifier.width(68.dp * scale).height(38.dp * scale),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        val weather = reading.weather
        if (weather == null) {
            Text("Unavailable", fontSize = 12.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WeatherIcon(
                    weather.weatherCode,
                    modifier = Modifier.size(18.dp),
                    light =
                        if (weather.isDay) WeatherCondition.Light.DAY else WeatherCondition.Light.NIGHT,
                )
                Text(
                    "${units.currentTemperature(weather.temperature).toInt()}${units.temperature.label}",
                    color = WeatherPalette.temperatureC(weather.temperature),
                    fontSize = 16.sp,
                )
            }
            if (reading.isCached) Text("Saved", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
@Suppress("FunctionNaming")
internal fun FavoriteMenu(
    name: String,
    index: Int,
    lastIndex: Int,
    onMove: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, "More actions for $name")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Move up") },
                leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) },
                enabled = index > 0,
                onClick = {
                    expanded = false
                    onMove(-1)
                },
            )
            DropdownMenuItem(
                text = { Text("Move down") },
                leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                enabled = index < lastIndex,
                onClick = {
                    expanded = false
                    onMove(1)
                },
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
internal fun FavoriteButton(
    name: String,
    saved: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            if (saved) "Remove $name from favorites" else "Save $name to favorites",
            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun coordinates(location: LocationEntity): String {
    val latitude = hemisphere(location.latitude, "N", "S")
    val longitude = hemisphere(location.longitude, "E", "W")
    return "$latitude, $longitude"
}

private fun hemisphere(
    value: Double,
    positive: String,
    negative: String,
): String = "%.2f° %s".format(Locale.getDefault(), abs(value), if (value >= 0) positive else negative)

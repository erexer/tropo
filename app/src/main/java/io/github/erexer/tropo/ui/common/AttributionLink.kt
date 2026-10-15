package io.github.erexer.tropo.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
@Suppress("FunctionNaming")
internal fun AttributionLink(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        TextButton(
            onClick = { failed = !AppLinks.open(context, AppLinks.WEATHER) },
            contentPadding = PaddingValues(4.dp),
        ) {
            Text("Weather data by Open-Meteo", style = MaterialTheme.typography.labelSmall)
        }
        if (failed) {
            Text("No browser available.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

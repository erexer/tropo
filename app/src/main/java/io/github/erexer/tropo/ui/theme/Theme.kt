package io.github.erexer.tropo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    background = DarkBackground,
    surface = SurfaceCard
)

@Composable
fun TropoTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTropoColors provides TropoColors()) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
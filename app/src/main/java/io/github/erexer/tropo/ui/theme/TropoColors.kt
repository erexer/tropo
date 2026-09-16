package io.github.erexer.tropo.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class TropoColors(
    val background: Color = DarkBackground,
    val surface: Color = SurfaceCard,
    val primary: Color = PrimaryBlue,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary
)

val LocalTropoColors = staticCompositionLocalOf { TropoColors() }
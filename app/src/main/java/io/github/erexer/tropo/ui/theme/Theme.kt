package io.github.erexer.tropo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import io.github.erexer.tropo.data.preferences.AppThemeMode

@Suppress("MagicNumber")
private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryBlue,
        onPrimary = Color(0xFF002E69),
        primaryContainer = Color(0xFF004494),
        onPrimaryContainer = Color(0xFFD8E2FF),
        inversePrimary = OceanBlue,
        secondary = Color(0xFFBEC6DC),
        onSecondary = Color(0xFF283141),
        secondaryContainer = Color(0xFF3E4759),
        onSecondaryContainer = Color(0xFFDAE2F9),
        tertiary = Color(0xFF5DD5FC),
        onTertiary = Color(0xFF003543),
        tertiaryContainer = Color(0xFF004D60),
        onTertiaryContainer = Color(0xFFB7EAFF),
        background = DarkBackground,
        onBackground = TextPrimary,
        surface = SurfaceCard,
        onSurface = TextPrimary,
        surfaceVariant = Color(0xFF44474F),
        onSurfaceVariant = TextSecondary,
        surfaceTint = PrimaryBlue,
        inverseSurface = TextPrimary,
        inverseOnSurface = Color(0xFF2E3036),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474F),
        surfaceBright = Color(0xFF37393E),
        surfaceDim = DarkBackground,
        surfaceContainerLowest = Color(0xFF0C0E13),
        surfaceContainerLow = SurfaceCard,
        surfaceContainer = Color(0xFF1F232A),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33353A),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color.Black,
        primaryFixed = Color(0xFFD8E2FF),
        primaryFixedDim = PrimaryBlue,
        onPrimaryFixed = Color(0xFF001A41),
        onPrimaryFixedVariant = Color(0xFF004494),
        secondaryFixed = Color(0xFFDAE2F9),
        secondaryFixedDim = Color(0xFFBEC6DC),
        onSecondaryFixed = Color(0xFF131C2B),
        onSecondaryFixedVariant = Color(0xFF3E4759),
        tertiaryFixed = Color(0xFFB7EAFF),
        tertiaryFixedDim = Color(0xFF5DD5FC),
        onTertiaryFixed = Color(0xFF001F28),
        onTertiaryFixedVariant = Color(0xFF004D60),
    )

@Suppress("MagicNumber")
private val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8E2FF),
        onPrimaryContainer = Color(0xFF001A41),
        inversePrimary = Color(0xFFADC6FF),
        secondary = Color(0xFF565E71),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDAE2F9),
        onSecondaryContainer = Color(0xFF131C2B),
        tertiary = Color(0xFF00677E),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFB7EAFF),
        onTertiaryContainer = Color(0xFF001F28),
        background = LightBackground,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = Color(0xFFE0E2EC),
        onSurfaceVariant = LightTextSecondary,
        surfaceTint = LightPrimary,
        inverseSurface = Color(0xFF2E3036),
        inverseOnSurface = Color(0xFFF0F0F7),
        outline = Color(0xFF74777F),
        outlineVariant = Color(0xFFC4C6D0),
        surfaceBright = LightSurface,
        surfaceDim = Color(0xFFD9DAE0),
        surfaceContainerLowest = LightSurface,
        surfaceContainerLow = Color(0xFFF5F7FD),
        surfaceContainer = Color(0xFFECEEF4),
        surfaceContainerHigh = Color(0xFFE6E8EE),
        surfaceContainerHighest = Color(0xFFE1E2E8),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        scrim = Color.Black,
        primaryFixed = Color(0xFFD8E2FF),
        primaryFixedDim = PrimaryBlue,
        onPrimaryFixed = Color(0xFF001A41),
        onPrimaryFixedVariant = Color(0xFF004494),
        secondaryFixed = Color(0xFFDAE2F9),
        secondaryFixedDim = Color(0xFFBEC6DC),
        onSecondaryFixed = Color(0xFF131C2B),
        onSecondaryFixedVariant = Color(0xFF3E4759),
        tertiaryFixed = Color(0xFFB7EAFF),
        tertiaryFixedDim = Color(0xFF5DD5FC),
        onTertiaryFixed = Color(0xFF001F28),
        onTertiaryFixedVariant = Color(0xFF004D60),
    )

@Composable
@Suppress("FunctionNaming")
fun TropoTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit,
) {
    val isDark =
        when (themeMode) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }
    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                content()
            }
        },
    )
}

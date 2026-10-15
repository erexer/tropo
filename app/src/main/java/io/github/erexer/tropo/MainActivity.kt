package io.github.erexer.tropo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import io.github.erexer.tropo.data.preferences.AppThemeMode
import io.github.erexer.tropo.ui.navigation.TropoNavHost
import io.github.erexer.tropo.ui.theme.TropoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as TropoApplication).container

        setContent {
            val themeMode by appContainer.weatherRepository.themeMode().collectAsState(initial = AppThemeMode.SYSTEM)
            TropoTheme(themeMode) {
                val background = MaterialTheme.colorScheme.background
                val dark = background.luminance() < DARK_LUMINANCE_THRESHOLD
                SideEffect {
                    window.decorView.setBackgroundColor(background.toArgb())
                    val transparent = android.graphics.Color.TRANSPARENT
                    val style =
                        if (dark) {
                            SystemBarStyle.dark(transparent)
                        } else {
                            SystemBarStyle.light(transparent, transparent)
                        }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                TropoNavHost(appContainer = appContainer)
            }
        }
    }
}

private const val DARK_LUMINANCE_THRESHOLD = 0.5f

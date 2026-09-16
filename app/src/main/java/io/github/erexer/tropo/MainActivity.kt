package io.github.erexer.tropo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.erexer.tropo.ui.navigation.TropoNavHost
import io.github.erexer.tropo.ui.theme.TropoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as TropoApplication).container
        
        setContent {
            TropoTheme {
                TropoNavHost(appContainer = appContainer)
            }
        }
    }
}
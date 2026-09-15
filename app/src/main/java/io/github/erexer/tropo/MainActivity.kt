package io.github.erexer.tropo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import io.github.erexer.tropo.data.WeatherApiService
import io.github.erexer.tropo.data.WeatherRepository
import io.github.erexer.tropo.data.local.AppDatabase
import io.github.erexer.tropo.ui.WeatherScreen
import io.github.erexer.tropo.ui.WeatherViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val apiService = WeatherApiService.create()
        val repository = WeatherRepository(apiService, database.weatherDao())

        val viewModel: WeatherViewModel by viewModels {
            WeatherViewModel.Factory(repository)
        }

        setContent {
            MaterialTheme {
                Surface {
                    WeatherScreen(viewModel = viewModel)
                }
            }
        }
    }
}
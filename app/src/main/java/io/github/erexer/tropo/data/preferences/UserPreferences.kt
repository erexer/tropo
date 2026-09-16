package io.github.erexer.tropo.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private val useCelsiusKey = booleanPreferencesKey("use_celsius")

    val useCelsius: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[useCelsiusKey] ?: true
    }

    suspend fun setUseCelsius(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[useCelsiusKey] = value
        }
    }
}
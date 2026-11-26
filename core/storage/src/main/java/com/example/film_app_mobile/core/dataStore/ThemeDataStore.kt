package com.example.film_app_mobile.core.dataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class AppThemeMode {
    LIGHT, DARK
}

private val Context.dataStore by preferencesDataStore("theme_prefs")

class ThemeDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
    }

    val themeFlow: Flow<AppThemeMode> =
        context.dataStore.data.map { prefs ->
            AppThemeMode.valueOf(
                prefs[Keys.THEME] ?: AppThemeMode.DARK.name
            )
        }

    suspend fun saveTheme(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = mode.name
        }
    }
}
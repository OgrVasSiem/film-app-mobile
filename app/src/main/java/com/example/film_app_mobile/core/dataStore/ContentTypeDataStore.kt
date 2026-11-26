package com.example.film_app_mobile.core.dataStore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.film_app_mobile.core.models.ContentType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.map

private val Context.contentPreferences by preferencesDataStore("content_preferences")

class ContentTypeDataStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("content_type")
    val selection =
        context.contentPreferences.data.map { prefs ->
            ContentType.entries.firstOrNull { it.apiKey == prefs[key] } ?: ContentType.FILM
        }

    suspend fun select(type: ContentType) {
        context.contentPreferences.edit { it[key] = type.apiKey }
    }
}

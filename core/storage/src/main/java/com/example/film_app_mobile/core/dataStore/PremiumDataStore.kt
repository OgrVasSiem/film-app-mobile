package com.example.film_app_mobile.core.dataStore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.premiumPreferences by preferencesDataStore("account_entitlements")

@Singleton
class PremiumDataStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun key(server: String, accountId: Long) = booleanPreferencesKey("$server/$accountId")

    fun observe(server: String, accountId: Long) =
        context.premiumPreferences.data.map { it[key(server, accountId)] }.distinctUntilChanged()

    suspend fun save(server: String, accountId: Long, premium: Boolean) {
        context.premiumPreferences.edit { it[key(server, accountId)] = premium }
    }

    suspend fun clear(server: String, accountId: Long) {
        context.premiumPreferences.edit { it.remove(key(server, accountId)) }
    }
}

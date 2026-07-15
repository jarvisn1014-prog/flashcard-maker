package com.nish.flashcards

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// PM Insight: DataStore for settings (API key).
// We store the API key locally on the device — it never goes to a server.
// This is the BYOK (Bring Your Own Key) model in action.

private val Context.dataStore by preferencesDataStore(name = "flashcard_settings")

object SettingsStore {
    private val API_KEY = stringPreferencesKey("api_key")
    private val THEME_MODE = stringPreferencesKey("theme_mode")

    fun getApiKey(context: Context): Flow<String> {
        return context.dataStore.data.map { it[API_KEY] ?: "" }
    }

    suspend fun setApiKey(context: Context, key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    fun getThemeMode(context: Context): Flow<String> {
        return context.dataStore.data.map { it[THEME_MODE] ?: "system" }
    }

    suspend fun setThemeMode(context: Context, mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
}
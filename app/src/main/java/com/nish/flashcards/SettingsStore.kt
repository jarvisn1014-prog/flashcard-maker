package com.nish.flashcards

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// PM Insight: Multi-provider BYOK. User can choose Gemini or Ollama Cloud.
// Each provider has its own API key, stored locally on the device.

private val Context.dataStore by preferencesDataStore(name = "flashcard_settings")

object SettingsStore {
    private val API_KEY = stringPreferencesKey("api_key")
    private val OLLAMA_KEY = stringPreferencesKey("ollama_key")
    private val PROVIDER = stringPreferencesKey("provider")
    private val THEME_MODE = stringPreferencesKey("theme_mode")

    fun getApiKey(context: Context): Flow<String> =
        context.dataStore.data.map { it[API_KEY] ?: "" }

    suspend fun setApiKey(context: Context, key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    fun getOllamaKey(context: Context): Flow<String> =
        context.dataStore.data.map { it[OLLAMA_KEY] ?: "" }

    suspend fun setOllamaKey(context: Context, key: String) {
        context.dataStore.edit { it[OLLAMA_KEY] = key }
    }

    fun getProvider(context: Context): Flow<String> =
        context.dataStore.data.map { it[PROVIDER] ?: "gemini" }

    suspend fun setProvider(context: Context, provider: String) {
        context.dataStore.edit { it[PROVIDER] = provider }
    }

    fun getThemeMode(context: Context): Flow<String> =
        context.dataStore.data.map { it[THEME_MODE] ?: "system" }

    suspend fun setThemeMode(context: Context, mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
}
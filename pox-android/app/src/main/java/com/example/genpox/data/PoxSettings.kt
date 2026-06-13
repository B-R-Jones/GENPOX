package com.example.genpox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pox_settings")

class PoxSettings(private val context: Context) {
    companion object {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val MUTE_SOUND = booleanPreferencesKey("mute_sound")
        val SCAN_RADIUS = floatPreferencesKey("scan_radius")
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY] ?: ""
    }

    val muteSound: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MUTE_SOUND] ?: false
    }

    val scanRadius: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SCAN_RADIUS] ?: 55f
    }

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun setMuteSound(mute: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MUTE_SOUND] = mute
        }
    }

    suspend fun setScanRadius(radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_RADIUS] = radius
        }
    }
}

package com.example.genpox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pox_settings")

class PoxSettings(private val context: Context) {
    companion object {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val MUTE_SOUND = booleanPreferencesKey("mute_sound")
        val SCAN_RADIUS = floatPreferencesKey("scan_radius")
        val TARGET_SEQUENCE = stringPreferencesKey("target_sequence")
        val RAW_STOCK_A = longPreferencesKey("raw_stock_a")
        val RAW_STOCK_G = longPreferencesKey("raw_stock_g")
        val RAW_STOCK_T = longPreferencesKey("raw_stock_t")
        val RAW_STOCK_C = longPreferencesKey("raw_stock_c")
        val BIO_WASTE = longPreferencesKey("bio_waste")
        val SPLICER_SLOTS = stringPreferencesKey("splicer_slots")
        val SPLICER_Q_SCORES = stringPreferencesKey("splicer_q_scores")
        val SPLICER_SIGNATURES = stringPreferencesKey("splicer_signatures")
        val SELECTED_HOST_PLANET = stringPreferencesKey("selected_host_planet")
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

    val targetSequence: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TARGET_SEQUENCE] ?: ""
    }

    val selectedHostPlanet: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_HOST_PLANET] ?: "EARTH"
    }

    val rawStockA: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[RAW_STOCK_A] ?: 10000L
    }

    val rawStockG: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[RAW_STOCK_G] ?: 10000L
    }

    val rawStockT: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[RAW_STOCK_T] ?: 10000L
    }

    val rawStockC: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[RAW_STOCK_C] ?: 10000L
    }

    val bioWaste: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[BIO_WASTE] ?: 0L
    }

    val splicerSlots: Flow<List<String?>> = context.dataStore.data.map { preferences ->
        val json = preferences[SPLICER_SLOTS] ?: ""
        if (json.isEmpty()) {
            List<String?>(8) { null }
        } else {
            try {
                Json.decodeFromString<List<String?>>(json)
            } catch (e: Exception) {
                List<String?>(8) { null }
            }
        }
    }

    val splicerQScores: Flow<List<Double>> = context.dataStore.data.map { preferences ->
        val json = preferences[SPLICER_Q_SCORES] ?: ""
        if (json.isEmpty()) {
            List(8) { 25.0 }
        } else {
            try {
                Json.decodeFromString<List<Double>>(json)
            } catch (e: Exception) {
                List(8) { 25.0 }
            }
        }
    }

    val splicerSignatures: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[SPLICER_SIGNATURES] ?: ""
        if (json.isEmpty()) {
            List(8) { "TAQ" }
        } else {
            try {
                Json.decodeFromString<List<String>>(json)
            } catch (e: Exception) {
                List(8) { "TAQ" }
            }
        }
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

    suspend fun saveTargetSequence(seq: String) {
        context.dataStore.edit { preferences ->
            preferences[TARGET_SEQUENCE] = seq
        }
    }

    suspend fun saveRawStocks(a: Long, g: Long, t: Long, c: Long) {
        context.dataStore.edit { preferences ->
            preferences[RAW_STOCK_A] = a
            preferences[RAW_STOCK_G] = g
            preferences[RAW_STOCK_T] = t
            preferences[RAW_STOCK_C] = c
        }
    }

    suspend fun saveBioWaste(waste: Long) {
        context.dataStore.edit { preferences ->
            preferences[BIO_WASTE] = waste
        }
    }

    suspend fun saveSplicerState(slots: List<String?>, qScores: List<Double>, signatures: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[SPLICER_SLOTS] = Json.encodeToString(slots)
            preferences[SPLICER_Q_SCORES] = Json.encodeToString(qScores)
            preferences[SPLICER_SIGNATURES] = Json.encodeToString(signatures)
        }
    }

    suspend fun saveSelectedHostPlanet(planet: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_HOST_PLANET] = planet
        }
    }
}

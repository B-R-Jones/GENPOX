package com.example.genpox.data.repository

import android.content.Context
import com.example.genpox.data.PoxSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface UserPreferencesRepository {
    val geminiApiKey: Flow<String>
    val muteSound: Flow<Boolean>
    val scanRadius: Flow<Float>
    val targetSequence: Flow<String>
    val rawStockA: Flow<Long>
    val rawStockG: Flow<Long>
    val rawStockT: Flow<Long>
    val rawStockC: Flow<Long>
    val bioWaste: Flow<Long>
    val splicerSlots: Flow<List<String?>>
    val splicerQScores: Flow<List<Double>>
    val splicerSignatures: Flow<List<String>>
    val selectedHostPlanet: Flow<String>

    suspend fun saveGeminiApiKey(apiKey: String)
    suspend fun setMuteSound(mute: Boolean)
    suspend fun setScanRadius(radius: Float)
    suspend fun saveTargetSequence(seq: String)
    suspend fun saveRawStocks(a: Long, g: Long, t: Long, c: Long)
    suspend fun saveBioWaste(waste: Long)
    suspend fun saveSplicerState(slots: List<String?>, qScores: List<Double>, signatures: List<String>)
    suspend fun saveSelectedHostPlanet(planet: String)
}

@Singleton
class DefaultUserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {
    private val settings: PoxSettings by lazy { PoxSettings(context) }

    override val geminiApiKey: Flow<String> get() = settings.geminiApiKey
    override val muteSound: Flow<Boolean> get() = settings.muteSound
    override val scanRadius: Flow<Float> get() = settings.scanRadius
    override val targetSequence: Flow<String> get() = settings.targetSequence
    override val rawStockA: Flow<Long> get() = settings.rawStockA
    override val rawStockG: Flow<Long> get() = settings.rawStockG
    override val rawStockT: Flow<Long> get() = settings.rawStockT
    override val rawStockC: Flow<Long> get() = settings.rawStockC
    override val bioWaste: Flow<Long> get() = settings.bioWaste
    override val splicerSlots: Flow<List<String?>> get() = settings.splicerSlots
    override val splicerQScores: Flow<List<Double>> get() = settings.splicerQScores
    override val splicerSignatures: Flow<List<String>> get() = settings.splicerSignatures
    override val selectedHostPlanet: Flow<String> get() = settings.selectedHostPlanet

    override suspend fun saveGeminiApiKey(apiKey: String) = settings.saveGeminiApiKey(apiKey)
    override suspend fun setMuteSound(mute: Boolean) = settings.setMuteSound(mute)
    override suspend fun setScanRadius(radius: Float) = settings.setScanRadius(radius)
    override suspend fun saveTargetSequence(seq: String) = settings.saveTargetSequence(seq)
    override suspend fun saveRawStocks(a: Long, g: Long, t: Long, c: Long) = settings.saveRawStocks(a, g, t, c)
    override suspend fun saveBioWaste(waste: Long) = settings.saveBioWaste(waste)
    override suspend fun saveSplicerState(slots: List<String?>, qScores: List<Double>, signatures: List<String>) = settings.saveSplicerState(slots, qScores, signatures)
    override suspend fun saveSelectedHostPlanet(planet: String) = settings.saveSelectedHostPlanet(planet)
}

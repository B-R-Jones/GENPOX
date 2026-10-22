package com.example.genpox.util

import com.example.genpox.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeDataRepository : DataRepository {
    val creaturesList = mutableListOf<Creature>()
    val sequencesList = mutableListOf<GeneSequence>()
    val sequencesFlow = MutableStateFlow<List<GeneSequence>>(emptyList())
    val missionsList = mutableListOf<HarvestMission>()

    val apiKeyFlow = MutableStateFlow("")
    val muteFlow = MutableStateFlow(false)
    val radiusFlow = MutableStateFlow(55f)
    val targetSequenceFlow = MutableStateFlow("")

    val stockAFlow = MutableStateFlow(10000L)
    val stockGFlow = MutableStateFlow(10000L)
    val stockTFlow = MutableStateFlow(10000L)
    val stockCFlow = MutableStateFlow(10000L)
    val bioWasteFlow = MutableStateFlow(0L)
    val splicerSlotsFlow = MutableStateFlow<List<String?>>(List(8) { null })
    val splicerQScoresFlow = MutableStateFlow<List<Double>>(List(8) { 25.0 })
    val splicerSignaturesFlow = MutableStateFlow<List<String>>(List(8) { "TAQ" })
    val hostPlanetFlow = MutableStateFlow("EARTH")

    override val allCreatures: Flow<List<Creature>> = flowOf(creaturesList)
    override suspend fun getCreatureById(id: String): Creature? = creaturesList.find { it.id == id }
    override suspend fun insertCreature(creature: Creature) {
        creaturesList.add(creature)
    }
    override suspend fun updateCreature(creature: Creature) {
        val index = creaturesList.indexOfFirst { it.id == creature.id }
        if (index != -1) creaturesList[index] = creature
    }
    override suspend fun deleteCreature(creature: Creature) {
        creaturesList.remove(creature)
    }

    override val allGeneSequences: Flow<List<GeneSequence>> = sequencesFlow
    override suspend fun insertGeneSequence(sequence: GeneSequence) {
        sequencesList.add(sequence)
        sequencesFlow.value = sequencesList.toList()
    }
    override suspend fun insertGeneSequences(sequences: List<GeneSequence>) {
        sequences.forEach { seq ->
            val index = sequencesList.indexOfFirst { it.sequence == seq.sequence }
            if (index != -1) {
                sequencesList[index] = seq
            } else {
                sequencesList.add(seq)
            }
        }
        sequencesFlow.value = sequencesList.toList()
    }
    override suspend fun updateGeneSequence(sequence: GeneSequence) {
        val index = sequencesList.indexOfFirst { it.sequence == sequence.sequence }
        if (index != -1) {
            sequencesList[index] = sequence
            sequencesFlow.value = sequencesList.toList()
        }
    }
    override suspend fun deleteGeneSequence(sequence: GeneSequence) {
        sequencesList.remove(sequence)
        sequencesFlow.value = sequencesList.toList()
    }
    override suspend fun deleteAllCreatures() {
        creaturesList.clear()
    }
    override suspend fun deleteAllGeneSequences() {
        sequencesList.clear()
        sequencesFlow.value = emptyList()
    }
    override suspend fun updateGeneStock(toInsertOrUpdate: List<GeneSequence>, toDelete: List<GeneSequence>) {
        toDelete.forEach { sequencesList.remove(it) }
        toInsertOrUpdate.forEach { item ->
            val index = sequencesList.indexOfFirst { it.sequence == item.sequence }
            if (index != -1) {
                sequencesList[index] = item
            } else {
                sequencesList.add(item)
            }
        }
        sequencesFlow.value = sequencesList.toList()
    }

    override val activeMissions: Flow<List<HarvestMission>> = flowOf(missionsList)
    override val allMissions: Flow<List<HarvestMission>> = flowOf(missionsList)
    override suspend fun insertMission(mission: HarvestMission) {
        missionsList.add(mission)
    }
    override suspend fun updateMission(mission: HarvestMission) {
        val index = missionsList.indexOfFirst { it.id == mission.id }
        if (index != -1) missionsList[index] = mission
    }

    override val geminiApiKey: Flow<String> = apiKeyFlow
    override val muteSound: Flow<Boolean> = muteFlow
    override val scanRadius: Flow<Float> = radiusFlow
    override val targetSequence: Flow<String> = targetSequenceFlow

    override suspend fun saveGeminiApiKey(apiKey: String) {
        apiKeyFlow.value = apiKey
    }
    override suspend fun setMuteSound(mute: Boolean) {
        muteFlow.value = mute
    }
    override suspend fun setScanRadius(radius: Float) {
        radiusFlow.value = radius
    }
    override suspend fun saveTargetSequence(seq: String) {
        targetSequenceFlow.value = seq
    }

    override val rawStockA: Flow<Long> = stockAFlow
    override val rawStockG: Flow<Long> = stockGFlow
    override val rawStockT: Flow<Long> = stockTFlow
    override val rawStockC: Flow<Long> = stockCFlow
    override val bioWaste: Flow<Long> = bioWasteFlow
    override val splicerSlots: Flow<List<String?>> = splicerSlotsFlow
    override val splicerQScores: Flow<List<Double>> = splicerQScoresFlow
    override val splicerSignatures: Flow<List<String>> = splicerSignaturesFlow
    override val selectedHostPlanet: Flow<String> = hostPlanetFlow

    override suspend fun saveRawStocks(a: Long, g: Long, t: Long, c: Long) {
        stockAFlow.value = a
        stockGFlow.value = g
        stockTFlow.value = t
        stockCFlow.value = c
    }

    override suspend fun saveBioWaste(waste: Long) {
        bioWasteFlow.value = waste
    }

    override suspend fun saveSplicerState(slots: List<String?>, qScores: List<Double>, signatures: List<String>) {
        splicerSlotsFlow.value = slots
        splicerQScoresFlow.value = qScores
        splicerSignaturesFlow.value = signatures
    }

    override suspend fun saveSelectedHostPlanet(planet: String) {
        hostPlanetFlow.value = planet
    }

    val reagentsStockFlow = MutableStateFlow<List<ReagentStock>>(emptyList())
    override val allReagentStock: Flow<List<ReagentStock>> = reagentsStockFlow
    override suspend fun insertReagentStock(reagents: List<ReagentStock>) {
        reagentsStockFlow.value = reagents
    }
    override suspend fun decrementReagentStock(id: String) {
        val list = reagentsStockFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val stock = list[idx]
            if (stock.count > 0) {
                list[idx] = stock.copy(count = stock.count - 1)
                reagentsStockFlow.value = list
            }
        }
    }
}

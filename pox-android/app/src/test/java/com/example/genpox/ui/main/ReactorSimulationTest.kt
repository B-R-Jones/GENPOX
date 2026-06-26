package com.example.genpox.ui.main

import com.example.genpox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class ReactorSimulationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // Dedicated self-contained Fake repository for reactor simulation tests
    private class FakeReactorRepository : DataRepository {
        val creaturesList = mutableListOf<Creature>()
        val sequencesList = mutableListOf<GeneSequence>()
        val missionsList = mutableListOf<HarvestMission>()
        
        val apiKeyFlow = MutableStateFlow("")
        val muteFlow = MutableStateFlow(false)
        val radiusFlow = MutableStateFlow(55f)
        val targetSequenceFlow = MutableStateFlow("")
        
        val stockAFlow = MutableStateFlow(10000L)
        val stockGFlow = MutableStateFlow(10000L)
        val stockTFlow = MutableStateFlow(10000L)
        val stockCFlow = MutableStateFlow(10000L)

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

        override val allGeneSequences: Flow<List<GeneSequence>> = flowOf(sequencesList)
        override suspend fun insertGeneSequence(sequence: GeneSequence) {
            sequencesList.add(sequence)
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
        }
        override suspend fun updateGeneSequence(sequence: GeneSequence) {
            val index = sequencesList.indexOfFirst { it.sequence == sequence.sequence }
            if (index != -1) sequencesList[index] = sequence
        }
        override suspend fun deleteGeneSequence(sequence: GeneSequence) {
            sequencesList.remove(sequence)
        }
        override suspend fun deleteAllCreatures() {
            creaturesList.clear()
        }
        override suspend fun deleteAllGeneSequences() {
            sequencesList.clear()
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

        override suspend fun saveRawStocks(a: Long, g: Long, t: Long, c: Long) {
            stockAFlow.value = a
            stockGFlow.value = g
            stockTFlow.value = t
            stockCFlow.value = c
        }

        override suspend fun getCachedRoadCell(cellKey: String): CachedRoadCell? = null
        override suspend fun getAllCachedRoadCells(): List<CachedRoadCell> = emptyList()
        override suspend fun insertCachedRoadCell(cell: CachedRoadCell) {}
        override suspend fun getCachedBuildingCell(cellKey: String): CachedBuildingCell? = null
        override suspend fun getAllCachedBuildingCells(): List<CachedBuildingCell> = emptyList()
        override suspend fun insertCachedBuildingCell(cell: CachedBuildingCell) {}
        override suspend fun clearMapCache() {}
    }

    private lateinit var repository: FakeReactorRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeReactorRepository()
        viewModel = MainViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPolymeraseSwapCosts() = runTest {
        // Set stock levels: 100 of each base, 200 biowaste
        repository.saveRawStocks(100L, 100L, 100L, 100L)
        // Manually set bio-waste in viewModel
        val bioWasteField = viewModel.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel) as MutableStateFlow<Long>
        bioWasteFlow.value = 200L

        assertEquals("Taq", viewModel.activePolymerase.value)

        // Swap to Tth (Costs 25 of all bases, 50 bio-waste - BUT timing is deferred to synthesis start!)
        viewModel.setActivePolymerase("Tth")
        advanceUntilIdle()
        // Selection succeeds but NO bases or waste are deducted yet
        assertEquals("Tth", viewModel.activePolymerase.value)
        assertEquals(100L, viewModel.rawStockA.value)
        assertEquals(100L, viewModel.rawStockG.value)
        assertEquals(100L, viewModel.rawStockT.value)
        assertEquals(100L, viewModel.rawStockC.value)
        assertEquals(200L, bioWasteFlow.value)

        // Set target sequence to AAAAAAAA (8 bases)
        val targetField = viewModel.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAA"

        // Set inlets high to avoid collapse
        viewModel.setInletRatioA(0.90f)
        viewModel.setInletRatioT(0.05f)
        viewModel.setInletRatioG(0.05f)
        viewModel.setInletRatioC(0.05f)

        // Now initiate synthesis - should deduct sequence (8 A) + Tth cost (25 of all bases, 50 bio-waste)
        // Total deduction:
        // A: 8 (sequence) + 25 (Tth) = 33
        // G: 25 (Tth)
        // T: 25 (Tth)
        // C: 25 (Tth)
        // Waste: 50 (Tth)
        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Verify remaining stocks:
        // A: 100 - 33 = 67
        // G: 100 - 25 = 75
        // T: 100 - 25 = 75
        // C: 100 - 25 = 75
        // Waste: 200 - 50 = 150
        assertEquals(67L, viewModel.rawStockA.value)
        assertEquals(75L, viewModel.rawStockG.value)
        assertEquals(75L, viewModel.rawStockT.value)
        assertEquals(75L, viewModel.rawStockC.value)
        assertEquals(150L, viewModel.bioWaste.value)
        // Tth is still engaged because remaining stocks (A=67, G=75, T=75, C=75, Waste=150)
        // are >= Tth requirements (25 of each base, 50 waste)
        assertEquals("Tth", viewModel.activePolymerase.value)

        // Swap to Pfu (timing is deferred, requirements: 50 of each base, 150 waste)
        // Current: A=67, G=75, T=75, C=75, Waste=150. Enough to swap!
        viewModel.setActivePolymerase("Pfu")
        advanceUntilIdle()
        assertEquals("Pfu", viewModel.activePolymerase.value)

        // Run synthesis again with Pfu. Sequence: AAAAAAAA
        // Total deduction:
        // A: 8 (sequence) + 50 (Pfu) = 58
        // G: 50 (Pfu)
        // T: 50 (Pfu)
        // C: 50 (Pfu)
        // Waste: 150 (Pfu)
        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Verify remaining stocks:
        // A: 67 - 58 = 9
        // G: 75 - 50 = 25
        // T: 75 - 50 = 25
        // C: 75 - 50 = 25
        // Waste: 150 - 150 = 0
        assertEquals(9L, viewModel.rawStockA.value)
        assertEquals(25L, viewModel.rawStockG.value)
        assertEquals(25L, viewModel.rawStockT.value)
        assertEquals(25L, viewModel.rawStockC.value)
        assertEquals(0L, viewModel.bioWaste.value)

        // Post-synthesis check: Pfu requirements are 50 of each base and 150 waste.
        // Current: A=9, G=25, T=25, C=25, Waste=0. This is less than Pfu requirements!
        // It must have been reset to Taq!
        assertEquals("Taq", viewModel.activePolymerase.value)

        // Try swapping back to Tth (Requires 25 all, 50 waste - but we have A=9, Waste=0)
        viewModel.setActivePolymerase("Tth")
        advanceUntilIdle()
        // Should reject and remain Taq
        assertEquals("Taq", viewModel.activePolymerase.value)
    }

    @Test
    fun testSaltDependentDenaturation() = runTest {
        // Target: AAAAAAAT (High AT)
        val targetField = viewModel.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAT"

        // Set inlets high to prevent inlet deprivation
        viewModel.setInletRatioA(0.90f)
        viewModel.setInletRatioT(0.10f)
        viewModel.setInletRatioG(0.05f)
        viewModel.setInletRatioC(0.05f)

        // Step A: Low Salt (0.01M), High Temp (70°C). T_denature = 65 + 0.01 * 50 = 65.5°C.
        // 70°C > 65.5°C -> Denaturation Collapse!
        viewModel.setReactorSalt(0.01f)
        viewModel.setReactorTemperature(70.0f)
        viewModel.setActiveChemicalSolute("None")

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertTrue("Reaction should have collapsed at low salt", viewModel.isReactionCollapsed.value)
        assertFalse("Synthesis should be finished", viewModel.isSynthesisActive.value)

        // Step B: High Salt (0.30M), High Temp (70°C). T_denature = 65 + 0.3 * 50 = 80°C.
        // 70°C < 80°C -> Protected!
        viewModel.setReactorSalt(0.30f)
        viewModel.setReactorTemperature(70.0f)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertFalse("Reaction should succeed at high salt", viewModel.isReactionCollapsed.value)
    }

    @Test
    fun testSaltDependentStalling() = runTest {
        // Target: GCGCGCGC (GC rich, triggers fold stalling)
        val targetField = viewModel.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel) as MutableStateFlow<String>
        targetFlow.value = "GCGCGCGC"

        // Set waste high and swap to Pfu to prevent mutations during biophysical checks
        val bioWasteField = viewModel.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel) as MutableStateFlow<Long>
        bioWasteFlow.value = 500L
        viewModel.setActivePolymerase("Pfu")
        advanceUntilIdle()

        // Set inlets high to prevent inlet deprivation
        viewModel.setInletRatioG(0.50f)
        viewModel.setInletRatioC(0.50f)
        viewModel.setInletRatioA(0.05f)
        viewModel.setInletRatioT(0.05f)

        // Step A: Low Salt (0.05M), Temp (35°C). T_stall = 20 + 0.05 * 40 = 22.0°C.
        // 35°C > 22°C -> Safe from stalling!
        viewModel.setReactorSalt(0.05f)
        viewModel.setReactorTemperature(35.0f)
        viewModel.setActiveChemicalSolute("None")

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertFalse("Reaction should succeed at low salt stalling check", viewModel.isReactionCollapsed.value)

        // Step B: High Salt (0.45M), Temp (35°C). T_stall = 20 + 0.45 * 40 = 38.0°C.
        // 35°C < 38°C -> Stalling Collapse!
        viewModel.setReactorSalt(0.45f)
        viewModel.setReactorTemperature(35.0f)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertTrue("Reaction should have collapsed at high salt stalling check", viewModel.isReactionCollapsed.value)
    }

    @Test
    fun testInletStoichiometricFidelity() = runTest {
        // Target: AAAAAAAA (100% A)
        val targetField = viewModel.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAA"

        // Taq polymerase (proofreading disabled)
        viewModel.setActivePolymerase("Taq")

        // Inlets perfectly aligned (A=0.90, others 0.05) -> Q-Score should be high, mutation unlikely
        viewModel.setInletRatioA(0.90f)
        viewModel.setInletRatioT(0.05f)
        viewModel.setInletRatioG(0.05f)
        viewModel.setInletRatioC(0.05f)
        viewModel.setReactorTemperature(16.0f) // aligned with target Tm

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertFalse("Synthesis should succeed", viewModel.isReactionCollapsed.value)
        val qMatched = viewModel.activeSynthesisQScore.value
        assertTrue("Q score should be high for aligned inlets", qMatched > 10.0)

        // Inlets heavily mismatched (A=0.10f, G/T/C higher) -> Q-score penalized
        viewModel.setInletRatioA(0.10f)
        viewModel.setInletRatioG(0.40f)
        viewModel.setInletRatioT(0.40f)
        viewModel.setInletRatioC(0.15f)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        val qMismatched = viewModel.activeSynthesisQScore.value
        assertTrue("Q score should be heavily penalized for mismatched inlets", qMismatched < qMatched)
    }

    @Test
    fun testPfuProofreadingActiveOverride() = runTest {
        // Target: AAAAAAAA
        val targetField = viewModel.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAA"

        // Give plenty of stock/biowaste to allow Pfu swap
        repository.saveRawStocks(1000L, 1000L, 1000L, 1000L)
        val bioWasteField = viewModel.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel) as MutableStateFlow<Long>
        bioWasteFlow.value = 500L

        // Swap to Pfu (Costs 50 base, 150 biowaste)
        viewModel.setActivePolymerase("Pfu")
        advanceUntilIdle()
        assertEquals("Pfu", viewModel.activePolymerase.value)

        // Inlets mismatched (A is 0.10f, others are higher)
        // With Taq, this would guarantee a mutation. With Pfu, proofreading corrects it.
        viewModel.setInletRatioA(0.10f)
        viewModel.setInletRatioG(0.35f)
        viewModel.setInletRatioT(0.35f)
        viewModel.setInletRatioC(0.25f)
        // Set temp to match Pfu target Tm (ideal Tm for AAAAAAAA at default salt 0.05 is 16.0°C)
        viewModel.setReactorTemperature(16.0f)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertFalse(viewModel.isReactionCollapsed.value)
        
        // Assert that the compiled gene block exactly matches target "AAAAAAAA" with no mutations
        val lastCompiled = repository.sequencesList.lastOrNull()
        assertTrue("Compiled sequence must be present", lastCompiled != null)
        assertEquals("AAAAAAAA", lastCompiled?.sequence)
    }
}

package com.example.genpox.ui.main

import com.example.genpox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

        val sequencesFlow = MutableStateFlow<List<GeneSequence>>(emptyList())
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
            if (index != -1) sequencesList[index] = sequence
            sequencesFlow.value = sequencesList.toList()
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

    private lateinit var repository: FakeReactorRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeReactorRepository()
        val synth = com.example.genpox.audio.PoxSynthManager().apply { isMuted = true }
        val locRepo = com.example.genpox.util.FakeLocationRepository()
        val dispatchers = com.example.genpox.util.TestDispatchersProvider()
        val tileStreamManager = com.example.genpox.map.tile.TileStreamManager(
            tileDataProvider = com.example.genpox.map.tile.FakeTileDataProvider(),
            dispatchers = dispatchers,
        )
        viewModel = MainViewModel(
            creatureRepository = repository,
            geneStockRepository = repository,
            missionRepository = repository,
            preferencesRepository = repository,
            synthManager = synth,
            locationRepository = locRepo,
            tileStreamManager = tileStreamManager,
            dispatchers = dispatchers,
        )

        viewModel.setReactorSalt(0.05f)
        viewModel.setReactorTemperature(16.0f)
        resetReactionCollapsedState()
        WaveMath.isTestMode = true
    }

    private fun resetReactionCollapsedState() {
        val collapsedField = viewModel.reactor.javaClass.getDeclaredField("_isReactionCollapsed")
        collapsedField.isAccessible = true
        val collapsedFlow = collapsedField.get(viewModel.reactor) as MutableStateFlow<Boolean>
        collapsedFlow.value = false
    }

    @After
    fun tearDown() {
        viewModel.reactor.stopReactorHeartbeat()
        Dispatchers.resetMain()
        WaveMath.isTestMode = false
    }

    @Test
    fun testPolymeraseSwapCosts() = runTest {
        // Set stock levels: 2000 of each base, 3000 biowaste
        repository.saveRawStocks(2000L, 2000L, 2000L, 2000L)
        // Manually set bio-waste in viewModel
        val bioWasteField = viewModel.reactor.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel.reactor) as MutableStateFlow<Long>
        bioWasteFlow.value = 3000L

        assertEquals("Taq", viewModel.activePolymerase.value)

        // Set target sequence to GGGGAAAA (8 bases, 4 G, 4 A, 0 T, 0 C)
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "GGGGAAAA"

        // Swap to Tth (Costs 100 of all bases + 100 per base count, 0 bio-waste - timing deferred to synthesis start!)
        viewModel.setActivePolymerase("Tth")
        advanceUntilIdle()
        // Selection succeeds but NO bases or waste are deducted yet
        assertEquals("Tth", viewModel.activePolymerase.value)
        assertEquals(2000L, viewModel.rawStockA.value)
        assertEquals(2000L, viewModel.rawStockG.value)
        assertEquals(2000L, viewModel.rawStockT.value)
        assertEquals(2000L, viewModel.rawStockC.value)
        assertEquals(3000L, bioWasteFlow.value)

        // Set temperature and salt (0.15M salt raises AT denaturation threshold to 26.5°C)
        viewModel.setReactorTemperature(25.0f)
        viewModel.setReactorSalt(0.15f)

        // Set inlets to match GGGGAAAA (50% A, 50% G, 0% T, 0% C)
        viewModel.setInletRatioA(0.50f)
        viewModel.setInletRatioG(0.50f)
        viewModel.setInletRatioT(0.00f)
        viewModel.setInletRatioC(0.00f)

        // Now initiate synthesis - should deduct sequence (4 A, 4 G, 0 T, 0 C) + Tth cost (100 base + count * 100, 0 bio-waste)
        // Tth costs: A=500, G=500, T=100, C=100, Waste=0
        // Total deduction:
        // A: 4 (sequence) + 500 (Tth) = 504
        // G: 4 (sequence) + 500 (Tth) = 504
        // T: 0 (sequence) + 100 (Tth) = 100
        // C: 0 (sequence) + 100 (Tth) = 100
        // Waste: 0 (Tth)
        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Verify remaining stocks:
        // A: 2000 - 504 = 1496
        // G: 2000 - 504 = 1496
        // T: 2000 - 100 = 1900
        // C: 2000 - 100 = 1900
        // Waste: 3000 - 0 = 3000
        assertEquals(1496L, viewModel.rawStockA.value)
        assertEquals(1496L, viewModel.rawStockG.value)
        assertEquals(1900L, viewModel.rawStockT.value)
        assertEquals(1900L, viewModel.rawStockC.value)
        assertEquals(3000L, viewModel.bioWaste.value)
        // Tth is still engaged because remaining stocks (A=1496, G=1496, T=1900, C=1900, Waste=3000)
        // are >= Tth requirements (A:500, G:500, T:100, C:100, Waste:0)
        assertEquals("Tth", viewModel.activePolymerase.value)

        // Swap to Pfu (timing is deferred, requirements: 200 all + count * 200 -> A:1000, G:1000, T:200, C:200, 0 waste)
        // Current: A=1496, G=1496, T=1900, C=1900, Waste=3000. Enough to swap!
        viewModel.setActivePolymerase("Pfu")
        advanceUntilIdle()
        assertEquals("Pfu", viewModel.activePolymerase.value)

        // Run synthesis again with Pfu. Sequence: GGGGAAAA
        // Pfu costs: A=1000, G=1000, T=200, C=200, Waste=0
        // Total deduction:
        // A: 4 (sequence) + 1000 (Pfu) = 1004
        // G: 4 (sequence) + 1000 (Pfu) = 1004
        // T: 0 (sequence) + 200 (Pfu) = 200
        // C: 0 (sequence) + 200 (Pfu) = 200
        // Waste: 0 (Pfu)
        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Verify remaining stocks:
        // A: 1496 - 1004 = 492
        // G: 1496 - 1004 = 492
        // T: 1900 - 200 = 1700
        // C: 1900 - 200 = 1700
        // Waste: 3000 - 0 = 3000
        assertEquals(492L, viewModel.rawStockA.value)
        assertEquals(492L, viewModel.rawStockG.value)
        assertEquals(1700L, viewModel.rawStockT.value)
        assertEquals(1700L, viewModel.rawStockC.value)
        assertEquals(3000L, viewModel.bioWaste.value)

        // Post-synthesis check: Pfu requirements for GGGGAAAA are A=1000, G=1000.
        // Current A = 492 < 1000 and G = 492 < 1000.
        // It must have been reset to Taq!
        assertEquals("Taq", viewModel.activePolymerase.value)

        // Try swapping back to Pfu (Requires A=1000, G=1000, but we have A=492, G=492)
        viewModel.setActivePolymerase("Pfu")
        advanceUntilIdle()
        // Should reject and remain Taq
        assertEquals("Taq", viewModel.activePolymerase.value)
    }

    @Test
    fun testTargetBaseSpecificPolymeraseCosts() = runTest {
        // 1. Direct Engine Cost Verification across bases using the exact user example sequence: AATGCGTG
        // Sequence: AATGCGTG -> A=2, C=1, G=3, T=2
        val tthCost = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost("TTH", "AATGCGTG")
        assertEquals(300L, tthCost.costA) // 100 base + 2 * 100
        assertEquals(200L, tthCost.costC) // 100 base + 1 * 100
        assertEquals(400L, tthCost.costG) // 100 base + 3 * 100
        assertEquals(300L, tthCost.costT) // 100 base + 2 * 100
        assertEquals(0L, tthCost.costWaste) // Zero biowaste required

        val pfuCost = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost("PFU", "AATGCGTG")
        assertEquals(600L, pfuCost.costA) // 200 base + 2 * 200
        assertEquals(400L, pfuCost.costC) // 200 base + 1 * 200
        assertEquals(800L, pfuCost.costG) // 200 base + 3 * 200
        assertEquals(600L, pfuCost.costT) // 200 base + 2 * 200
        assertEquals(0L, pfuCost.costWaste) // Zero biowaste required

        val taqCost = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost("TAQ", "AATGCGTG")
        assertEquals(0L, taqCost.costA)
        assertEquals(0L, taqCost.costG)
        assertEquals(0L, taqCost.costT)
        assertEquals(0L, taqCost.costC)
        assertEquals(0L, taqCost.costWaste)

        assertEquals(100L, com.example.genpox.data.ReactorSimulationEngine.calculateSoluteCost("DMSO"))
        assertEquals(100L, com.example.genpox.data.ReactorSimulationEngine.calculateSoluteCost("Netropsin"))
        assertEquals(0L, com.example.genpox.data.ReactorSimulationEngine.calculateSoluteCost("None"))

        // 2. Integration with ViewModel synthesis using sequence AATGCGTG and Tth
        repository.saveRawStocks(5000L, 5000L, 5000L, 5000L)
        val bioWasteField = viewModel.reactor.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel.reactor) as MutableStateFlow<Long>
        bioWasteFlow.value = 2000L

        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "AATGCGTG" // 2 A, 1 C, 3 G, 2 T

        viewModel.setInletRatioA(0.25f)
        viewModel.setInletRatioC(0.25f)
        viewModel.setInletRatioG(0.25f)
        viewModel.setInletRatioT(0.25f)
        viewModel.setReactorTemperature(25.0f)
        viewModel.setReactorSalt(0.15f)

        viewModel.setActivePolymerase("Tth")
        advanceUntilIdle()
        assertEquals("Tth", viewModel.activePolymerase.value)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Deductions:
        // A: 2 (seq) + 300 (poly) = 302 -> 4698
        // C: 1 (seq) + 200 (poly) = 201 -> 4799
        // G: 3 (seq) + 400 (poly) = 403 -> 4597
        // T: 2 (seq) + 300 (poly) = 302 -> 4698
        // Waste: 0 enzyme deduction; synthesis completes without stalling -> 2000L
        assertEquals(4698L, viewModel.rawStockA.value)
        assertEquals(4799L, viewModel.rawStockC.value)
        assertEquals(4597L, viewModel.rawStockG.value)
        assertEquals(4698L, viewModel.rawStockT.value)
        assertEquals(2000L, viewModel.bioWaste.value)
    }

    @Test
    fun testPolymeraseAndSoluteCombinedCostDeduction() = runTest {
        repository.saveRawStocks(5000L, 5000L, 5000L, 5000L)
        val bioWasteField = viewModel.reactor.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel.reactor) as MutableStateFlow<Long>
        bioWasteFlow.value = 2000L

        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "ACGTACGT" // 2 A, 2 C, 2 G, 2 T

        viewModel.setInletRatioA(0.25f)
        viewModel.setInletRatioC(0.25f)
        viewModel.setInletRatioG(0.25f)
        viewModel.setInletRatioT(0.25f)
        viewModel.setReactorTemperature(25.0f)
        viewModel.setReactorSalt(0.15f)

        // Select Tth and DMSO
        viewModel.setActivePolymerase("Tth")
        viewModel.setActiveChemicalSolute("DMSO")
        advanceUntilIdle()
        assertEquals("Tth", viewModel.activePolymerase.value)
        assertEquals("DMSO", viewModel.activeChemicalSolute.value)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Combined deductions for ACGTACGT (2 of each base):
        // Tth poly cost = 100 base + 2 * 100 = 300 each base
        // Solute DMSO = 100 each base
        // Sequence = 2 each base
        // Total deduction per base = 2 + 100 + 300 = 402 -> 5000 - 402 = 4598
        // Waste = 0 deducted -> 2000L
        assertEquals(4598L, viewModel.rawStockA.value)
        assertEquals(4598L, viewModel.rawStockC.value)
        assertEquals(4598L, viewModel.rawStockG.value)
        assertEquals(4598L, viewModel.rawStockT.value)
        assertEquals(2000L, viewModel.bioWaste.value)
    }

    @Test
    fun testChemicalSoluteStockCheckAndDepletionReset() = runTest {
        // 1. Rejection when stock < 100
        repository.saveRawStocks(50L, 50L, 50L, 50L)
        viewModel.setActiveChemicalSolute("Netropsin")
        advanceUntilIdle()
        assertEquals("None", viewModel.activeChemicalSolute.value)

        // 2. Acceptance when stock >= 100
        repository.saveRawStocks(500L, 500L, 500L, 500L)
        viewModel.setActiveChemicalSolute("Netropsin")
        advanceUntilIdle()
        assertEquals("Netropsin", viewModel.activeChemicalSolute.value)

        // 3. Depletion reset: perform synthesis that leaves stocks < 100
        // Target: 8 C's (80% C). Set stocks such that after synthesis, remaining stock < 100
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "CCCCCCCC"

        // Stock: A=100, G=100, T=100, C=108.
        // Netropsin costs 100 each. Seq costs 8 C. Remaining after synthesis: A=0, G=0, T=0, C=0.
        repository.saveRawStocks(100L, 100L, 100L, 108L)
        viewModel.setInletRatioC(0.85f)
        viewModel.setInletRatioA(0.05f)
        viewModel.setInletRatioG(0.05f)
        viewModel.setInletRatioT(0.05f)
        viewModel.setReactorTemperature(35.0f)
        viewModel.setReactorSalt(0.05f)
        viewModel.setActivePolymerase("Taq")

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Stock depleted to 0, so active solute should reset to "None"
        assertEquals("None", viewModel.activeChemicalSolute.value)
    }

    @Test
    fun testPfuCostTargetBaseBonus() = runTest {
        // GCTA -> G=1, C=1, T=1, A=1
        val pfuCostG = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost("PFU", "GCTA")
        assertEquals(400L, pfuCostG.costA) // 200 + 1 * 200
        assertEquals(400L, pfuCostG.costG) // 200 + 1 * 200
        assertEquals(400L, pfuCostG.costT) // 200 + 1 * 200
        assertEquals(400L, pfuCostG.costC) // 200 + 1 * 200
        assertEquals(0L, pfuCostG.costWaste)

        // CGTAA -> C=1, G=1, T=1, A=2
        val pfuCostC = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost("pfu", "CGTAA")
        assertEquals(600L, pfuCostC.costA) // 200 + 2 * 200
        assertEquals(400L, pfuCostC.costG) // 200 + 1 * 200
        assertEquals(400L, pfuCostC.costT) // 200 + 1 * 200
        assertEquals(400L, pfuCostC.costC) // 200 + 1 * 200
        assertEquals(0L, pfuCostC.costWaste)
    }

    @Test
    fun testSaltDependentDenaturation() = runTest {
        // Target: AAAAAAAT (High AT)
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAT"

        // Set inlets high to prevent inlet deprivation
        viewModel.setInletRatioA(0.90f)
        viewModel.setInletRatioT(0.10f)
        viewModel.setInletRatioG(0.05f)
        viewModel.setInletRatioC(0.05f)

        // Step A: Low Salt (0.01M), Temp (25°C). T_denature = 22 + 0.01 * 30 = 22.3°C.
        // 25°C > 22.3°C -> Denaturation Collapse!
        viewModel.setReactorSalt(0.01f)
        viewModel.setReactorTemperature(25.0f)
        viewModel.setActiveChemicalSolute("None")

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertTrue("Reaction should have collapsed at low salt", viewModel.isReactionCollapsed.value)
        assertFalse("Synthesis should be finished", viewModel.isSynthesisActive.value)

        // Step B: High Salt (0.15M), Temp (25°C). T_denature = 22 + 0.15 * 30 = 26.5°C.
        // 25°C < 26.5°C -> Protected!
        viewModel.setReactorSalt(0.15f)
        viewModel.setReactorTemperature(25.0f)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertFalse("Reaction should succeed at high salt", viewModel.isReactionCollapsed.value)
    }

    @Test
    fun testSaltDependentStalling() = runTest {
        // Target: GCGCGCGC (GC rich, triggers fold stalling)
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "GCGCGCGC"

        // Set waste high and swap to Pfu to prevent mutations during biophysical checks
        val bioWasteField = viewModel.reactor.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel.reactor) as MutableStateFlow<Long>
        bioWasteFlow.value = 2000L
        viewModel.setActivePolymerase("Pfu")
        advanceUntilIdle()

        // Set inlets high to prevent inlet deprivation
        viewModel.setInletRatioG(0.50f)
        viewModel.setInletRatioC(0.50f)
        viewModel.setInletRatioA(0.05f)
        viewModel.setInletRatioT(0.05f)

        // Step A: Low Salt (0.05M), Temp (35°C). T_stall = 28 + 0.05 * 30 = 29.5°C.
        // 35°C > 29.5°C -> Safe from stalling!
        viewModel.setReactorSalt(0.05f)
        viewModel.setReactorTemperature(35.0f)
        viewModel.setActiveChemicalSolute("None")

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertFalse("Reaction should succeed at low salt stalling check", viewModel.isReactionCollapsed.value)

        // Step B: High Salt (0.15M), Temp (30°C). T_stall = 28 + 0.15 * 30 = 32.5°C.
        // 30°C < 32.5°C -> Stalling Collapse!
        viewModel.setReactorSalt(0.15f)
        viewModel.setReactorTemperature(30.0f)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertTrue("Reaction should have collapsed at high salt stalling check", viewModel.isReactionCollapsed.value)
    }

    @Test
    fun testInletStoichiometricFidelity() = runTest {
        repository.saveRawStocks(1000L, 1000L, 1000L, 1000L)
        viewModel.setReactorSalt(0.05f)
        viewModel.setActiveChemicalSolute("DMSO")
        resetReactionCollapsedState()
        // Target: AAAAAAAA (100% A)
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAA"

        // Taq polymerase (proofreading disabled)
        viewModel.setActivePolymerase("Taq")

        // Inlets perfectly aligned (A=0.90, others 0.05) -> Q-Score should be high, mutation unlikely
        viewModel.setInletRatioA(0.90f)
        viewModel.setInletRatioT(0.05f)
        viewModel.setInletRatioG(0.05f)
        viewModel.setInletRatioC(0.05f)
        viewModel.setReactorTemperature(20.0f)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        val logs = viewModel.terminalLogs.value.joinToString(" | ")
        assertFalse("Synthesis should succeed: logs=$logs", viewModel.isReactionCollapsed.value)
        val qMatched = viewModel.activeSynthesisQScore.value
        assertTrue("Q score should be high for aligned inlets", qMatched > 10.0)

        // Inlets heavily mismatched (A=0.10f, G/T/C higher) -> Q-score penalized
        resetReactionCollapsedState()
        viewModel.setInletRatioA(0.10f)
        viewModel.setInletRatioG(0.40f)
        viewModel.setInletRatioT(0.40f)
        viewModel.setInletRatioC(0.15f)
        viewModel.setReactorTemperature(20.0f)

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        val qMismatched = viewModel.activeSynthesisQScore.value
        assertTrue("Q score should be heavily penalized for mismatched inlets", qMismatched < qMatched)
    }

    @Test
    fun testPfuProofreadingActiveOverride() = runTest {
        // Target: AAAAAAAA
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAA"

        // Give plenty of stock/biowaste to allow Pfu swap (Pfu requires 2200 A for target AAAAAAAA)
        repository.saveRawStocks(5000L, 5000L, 5000L, 5000L)
        val bioWasteField = viewModel.reactor.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel.reactor) as MutableStateFlow<Long>
        bioWasteFlow.value = 2000L

        // Swap to Pfu (Costs 50 base, 1000 biowaste)
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

    @Test
    fun testInletBlockPrevention() = runTest {
        // Setup target sequence requiring 'A'
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAA"

        // Set Inlet A to 0.05f (absolute minimum / restricted)
        viewModel.setInletRatioA(0.05f)

        // Give plenty of stock
        repository.saveRawStocks(1000L, 1000L, 1000L, 1000L)
        val initialA = viewModel.rawStockA.value

        // Try to initiate
        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Verify synthesis is blocked (not active, no stocks deducted)
        assertFalse(viewModel.isSynthesisActive.value)
        assertEquals(initialA, viewModel.rawStockA.value)
    }

    @Test
    fun testProratedBioWasteRecovery() = runTest {
        // Target: AAAAAAAT (High AT)
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAT"

        // Set inlets > 5% to pass pre-flight block checks
        viewModel.setInletRatioA(0.90f)
        viewModel.setInletRatioT(0.10f)
        viewModel.setInletRatioG(0.00f)
        viewModel.setInletRatioC(0.00f)

        // Give plenty of stock, zero out waste initial
        repository.saveRawStocks(1000L, 1000L, 1000L, 1000L)
        val bioWasteField = viewModel.reactor.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel.reactor) as MutableStateFlow<Long>
        bioWasteFlow.value = 0L

        // Trigger denaturation collapse at step 4: Low Salt (0.01M), Temp (25°C). T_denature = 22.3°C.
        viewModel.setReactorSalt(0.01f)
        viewModel.setReactorTemperature(25.0f)
        viewModel.setActiveChemicalSolute("None")

        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()

        // Verify reaction collapsed and returned exactly 4 bio-waste (collapses at step 4)
        assertTrue(viewModel.isReactionCollapsed.value)
        assertEquals(4L, viewModel.bioWaste.value)
    }

    @Test
    fun testBulkRecycleMatchingGenes() = runTest {
        // Start collection to trigger Lazily started StateFlow
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.geneSequences.collect {}
        }

        // Setup initial vault sequences
        val g1 = GeneSequence(sequence = "AAAAATAT", count = 2, discoveredAt = System.currentTimeMillis(), averageQScore = 30.0)
        val g2 = GeneSequence(sequence = "AAAAGCGC", count = 3, discoveredAt = System.currentTimeMillis(), averageQScore = 32.0)
        val g3 = GeneSequence(sequence = "GCGCGCGC", count = 1, discoveredAt = System.currentTimeMillis(), averageQScore = 35.0)
        repository.insertGeneSequence(g1)
        repository.insertGeneSequence(g2)
        repository.insertGeneSequence(g3)
        advanceUntilIdle()

        // Zero out initial waste
        val bioWasteField = viewModel.reactor.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel.reactor) as MutableStateFlow<Long>
        bioWasteFlow.value = 0L

        // Trigger bulk recycle for prefix "AAAA"
        viewModel.recycleMatchingGenes("AAAA")
        advanceUntilIdle()

        // Verify matches are deleted, non-matches remain
        val allSequences = repository.sequencesList
        org.junit.Assert.assertNull(allSequences.find { it.sequence == "AAAAATAT" })
        org.junit.Assert.assertNull(allSequences.find { it.sequence == "AAAAGCGC" })
        org.junit.Assert.assertNotNull(allSequences.find { it.sequence == "GCGCGCGC" })

        // Verify Bio-Waste yield: (2 + 3) * 2 = 10L
        assertEquals(10L, viewModel.bioWaste.value)
        job.cancel()
    }

    @Test
    fun testEnzymeThermalLimits() = runTest {
        // Target: AAAAAAAA
        val targetField = viewModel.reactor.javaClass.getDeclaredField("_targetSynthesisSequence")
        targetField.isAccessible = true
        val targetFlow = targetField.get(viewModel.reactor) as MutableStateFlow<String>
        targetFlow.value = "AAAAAAAA"

        // Set inputs to prevent other failures
        viewModel.setInletRatioA(0.90f)
        viewModel.setInletRatioT(0.05f)
        viewModel.setInletRatioG(0.05f)
        viewModel.setInletRatioC(0.05f)
        repository.saveRawStocks(5000L, 5000L, 5000L, 5000L)

        // Case 1: Pfu denatures at > 40°C
        val bioWasteField = viewModel.reactor.javaClass.getDeclaredField("_bioWaste")
        bioWasteField.isAccessible = true
        val bioWasteFlow = bioWasteField.get(viewModel.reactor) as MutableStateFlow<Long>
        bioWasteFlow.value = 2000L

        viewModel.setActivePolymerase("Pfu")
        viewModel.setReactorTemperature(41.0f) // > 40°C
        advanceUntilIdle()
        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertTrue("PFU should collapse at >40°C", viewModel.isReactionCollapsed.value)

        // Case 2: Taq denatures at > 45°C
        viewModel.setActivePolymerase("Taq")
        viewModel.setReactorTemperature(46.0f) // > 45°C
        advanceUntilIdle()
        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertTrue("TAQ should collapse at >45°C", viewModel.isReactionCollapsed.value)

        // Case 3: Tth cold inactivates at < 25°C
        viewModel.setActivePolymerase("Tth")
        viewModel.setReactorTemperature(24.0f) // < 25°C
        advanceUntilIdle()
        viewModel.initiateStandardSynthesis()
        advanceUntilIdle()
        assertTrue("TTH should collapse at <25°C", viewModel.isReactionCollapsed.value)
    }

    @Test
    fun testCelestialHostPlanetAnomalousBonding() = runTest {
        // We will test ReactorSimulationEngine.runSynthesis directly.
        // We want to force a condition where step == hostIndex, QScore is high, Temp is eligible,
        // and we have a CONJUNCTION alignment state.
        WaveMath.isTestMode = false

        // Find a timestamp where Mercury and Venus are in CONJUNCTION
        var testTimeMs = 1778982840000L // reference time
        var foundConjunction = false
        for (i in 0 until 5000) { // check up to 5000 days
            if (WaveMath.getPairAlignmentState("MERCURY", "VENUS", testTimeMs) == WaveMath.AlignmentState.CONJUNCTION) {
                foundConjunction = true
                break
            }
            testTimeMs += 3600000L * 24 // +1 day
        }

        assertTrue("Should find a conjunction time", foundConjunction)

        // Set manual offset so that WaveMath.getAdjustedTime() returns this conjunction timestamp
        WaveMath.setManualOffset(testTimeMs - System.currentTimeMillis())

        // Now run synthesis. Mercury is step 1. Target sequence: AAAAAAAA.
        // PFU polymerase is ideal as it has base Q of 35-40 (above 22 threshold).
        // Temp is 16.0f (perfectly matching target Tm for AAAAAAAA at 0.05 salt).
        // Host planet is MERCURY, so hostIndex = 1 (step 1).
        // Under CONJUNCTION: qThreshold = 22.0, anomalousChance = 0.40.
        // We loop up to 50 times (since it's a 40% chance per run) to find a successful bonding.
        var anomalousBonded = false
        var attempts = 0
        while (!anomalousBonded && attempts < 50) {
            attempts++
            val result = ReactorSimulationEngine.runSynthesis(
                target = "AAAAAAAA",
                temp = 16.0f,
                salt = 0.05,
                poly = "PFU",
                solute = "None",
                inletRatioA = 1.0f,
                inletRatioG = 0.0f,
                inletRatioT = 0.0f,
                inletRatioC = 0.0f,
                stepIntervalMs = 0L,
                hostPlanet = "MERCURY",
                onStep = {},
            )
            val firstChar = result.finalStrand[0]
            if (firstChar in listOf('X', 'Z', 'Y', 'W')) {
                anomalousBonded = true
                // Verify that the rest of the strand is standard 'A's
                assertEquals("AAAAAAAA".substring(1), result.finalStrand.substring(1))
                // Verify that step 1 had isMutated = true and a celestial mutation message
                val step1 = result.steps[0]
                assertTrue("Step 1 should be marked mutated", step1.isMutated)
                assertTrue("Step 1 should have celestial message", step1.mutationMessage?.contains("[CELESTIAL]") == true)
            }
        }

        assertTrue("Anomalous base should have successfully bonded after several runs", anomalousBonded)

        // Reset settings
        WaveMath.isTestMode = true
        WaveMath.setManualOffset(0L)
    }
}

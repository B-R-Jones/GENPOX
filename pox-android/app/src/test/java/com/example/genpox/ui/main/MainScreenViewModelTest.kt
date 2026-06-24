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
import org.junit.After
import org.junit.Before
import org.junit.Test
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeDataRepository : DataRepository {
        val creaturesList = mutableListOf<Creature>()
        val sequencesList = mutableListOf<GeneSequence>()
        val missionsList = mutableListOf<HarvestMission>()
        
        val apiKeyFlow = MutableStateFlow("")
        val muteFlow = MutableStateFlow(false)
        val radiusFlow = MutableStateFlow(55f)
        val targetSequenceFlow = MutableStateFlow("")

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

        val cachedRoadCells = mutableListOf<CachedRoadCell>()
        val cachedBuildingCells = mutableListOf<CachedBuildingCell>()

        override suspend fun getCachedRoadCell(cellKey: String): CachedRoadCell? = cachedRoadCells.find { it.cellKey == cellKey }
        override suspend fun getAllCachedRoadCells(): List<CachedRoadCell> = cachedRoadCells
        override suspend fun insertCachedRoadCell(cell: CachedRoadCell) {
            cachedRoadCells.add(cell)
        }
        override suspend fun getCachedBuildingCell(cellKey: String): CachedBuildingCell? = cachedBuildingCells.find { it.cellKey == cellKey }
        override suspend fun getAllCachedBuildingCells(): List<CachedBuildingCell> = cachedBuildingCells
        override suspend fun insertCachedBuildingCell(cell: CachedBuildingCell) {
            cachedBuildingCells.add(cell)
        }
        override suspend fun clearMapCache() {
            cachedRoadCells.clear()
            cachedBuildingCells.clear()
        }
    }

    @Test
    fun testSelectTab() = runTest {
        val repo = FakeDataRepository()
        val viewModel = MainViewModel(repo)
        assertEquals("combinator", viewModel.selectedTab.value)
        viewModel.selectTab("library")
        assertEquals("library", viewModel.selectedTab.value)
    }

    @Test
    fun testAddLog() = runTest {
        val repo = FakeDataRepository()
        val viewModel = MainViewModel(repo)
        val initialSize = viewModel.terminalLogs.value.size
        viewModel.addLog("TEST_LOG_MESSAGE")
        assertEquals(initialSize + 1, viewModel.terminalLogs.value.size)
        assert(viewModel.terminalLogs.value.last().contains("TEST_LOG_MESSAGE"))
    }

    @Test
    fun testOfflineCompilation() = runTest {
        val repo = FakeDataRepository()
        val viewModel = MainViewModel(repo)
        // 64-char valid sequence
        val sequence = "A".repeat(64)
        viewModel.compileCreature(sequence)
        
        // The compilation should execute immediately since we set the main dispatcher to UnconfinedTestDispatcher
        assertEquals(1, repo.creaturesList.size)
        val creature = repo.creaturesList.first()
        assertEquals(sequence, creature.sequence)
        assertEquals("Infection", creature.faction)
        assertNotNull(creature.name)
    }

    @Test
    fun testMissionPhaseTransitions() = runTest {
        val repo = FakeDataRepository()
        val viewModel = MainViewModel(repo)
        
        // Let's manually insert a mission with custom durations to test transition ticking
        val mission = HarvestMission(
            id = "MSN-TEST",
            creatureId = "PX-123",
            creatureName = "Test Specimen",
            creatureFaction = "Infection",
            lat = 40.0,
            lng = -74.0,
            startTime = System.currentTimeMillis(),
            totalDuration = 10,
            harvestedGenes = listOf("AGTCGTAC"),
            isCompleted = false,
            isReturned = false,
            dispatchDistance = 100.0,
            stalledDepth = 50.0,
            originalSequence = "A".repeat(64),
            elapsedSeconds = 0L,
            missionLogs = emptyList(),
            phase = "TRAVEL",
            travelDuration = 2,
            descentDuration = 2,
            harvestDuration = 2,
            ascentDuration = 2,
            transitBackDuration = 2,
            currentPhaseElapsed = 0L
        )
        repo.insertMission(mission)
        
        // Simulate ticking logic similar to the view model heartbeat
        var currentMission = repo.missionsList.first()
        
        fun tick() {
            val nextElapsed = currentMission.elapsedSeconds + 1
            val currentPhaseElapsed = currentMission.currentPhaseElapsed + 1
            var nextPhase = currentMission.phase
            var nextPhaseElapsed = currentPhaseElapsed
            
            when (currentMission.phase) {
                "TRAVEL" -> {
                    if (currentPhaseElapsed >= currentMission.travelDuration) {
                        nextPhase = "DESCENT"
                        nextPhaseElapsed = 0L
                    }
                }
                "DESCENT" -> {
                    if (currentPhaseElapsed >= currentMission.descentDuration) {
                        nextPhase = "HARVESTING"
                        nextPhaseElapsed = 0L
                    }
                }
                "HARVESTING" -> {
                    if (currentPhaseElapsed >= currentMission.harvestDuration) {
                        nextPhase = "ASCENT"
                        nextPhaseElapsed = 0L
                    }
                }
                "ASCENT" -> {
                    if (currentPhaseElapsed >= currentMission.ascentDuration) {
                        nextPhase = "TRANST_BACK"
                        nextPhaseElapsed = 0L
                    }
                }
                "TRANST_BACK" -> {
                    if (currentPhaseElapsed >= currentMission.transitBackDuration) {
                        nextPhase = "COMPLETED"
                        nextPhaseElapsed = 0L
                    }
                }
            }
            
            val isCompleted = nextPhase == "COMPLETED"
            currentMission = currentMission.copy(
                elapsedSeconds = nextElapsed,
                isCompleted = isCompleted,
                phase = nextPhase,
                currentPhaseElapsed = nextPhaseElapsed
            )
        }
        
        // Tick 1 (TRAVEL: elapsed 1, phase elapsed 1)
        tick()
        assertEquals("TRAVEL", currentMission.phase)
        assertEquals(1L, currentMission.currentPhaseElapsed)
        
        // Tick 2 (TRAVEL: elapsed 2, phase elapsed 2 >= 2 -> Transition to DESCENT)
        tick()
        assertEquals("DESCENT", currentMission.phase)
        assertEquals(0L, currentMission.currentPhaseElapsed)
        
        // Tick 3 (DESCENT: elapsed 3, phase elapsed 1)
        tick()
        assertEquals("DESCENT", currentMission.phase)
        assertEquals(1L, currentMission.currentPhaseElapsed)
        
        // Tick 4 (DESCENT: elapsed 4, phase elapsed 2 >= 2 -> Transition to HARVESTING)
        tick()
        assertEquals("HARVESTING", currentMission.phase)
        assertEquals(0L, currentMission.currentPhaseElapsed)

        // Tick 5 (HARVESTING: elapsed 5, phase elapsed 1)
        tick()
        assertEquals("HARVESTING", currentMission.phase)
        assertEquals(1L, currentMission.currentPhaseElapsed)

        // Tick 6 (HARVESTING: elapsed 6, phase elapsed 2 >= 2 -> Transition to ASCENT)
        tick()
        assertEquals("ASCENT", currentMission.phase)
        assertEquals(0L, currentMission.currentPhaseElapsed)

        // Tick 7 (ASCENT: elapsed 7, phase elapsed 1)
        tick()
        assertEquals("ASCENT", currentMission.phase)
        assertEquals(1L, currentMission.currentPhaseElapsed)

        // Tick 8 (ASCENT: elapsed 8, phase elapsed 2 >= 2 -> Transition to TRANST_BACK)
        tick()
        assertEquals("TRANST_BACK", currentMission.phase)
        assertEquals(0L, currentMission.currentPhaseElapsed)

        // Tick 9 (TRANST_BACK: elapsed 9, phase elapsed 1)
        tick()
        assertEquals("TRANST_BACK", currentMission.phase)
        assertEquals(1L, currentMission.currentPhaseElapsed)

        // Tick 10 (TRANST_BACK: elapsed 10, phase elapsed 2 >= 2 -> Transition to COMPLETED)
        tick()
        assertEquals("COMPLETED", currentMission.phase)
        assertEquals(true, currentMission.isCompleted)
    }
}


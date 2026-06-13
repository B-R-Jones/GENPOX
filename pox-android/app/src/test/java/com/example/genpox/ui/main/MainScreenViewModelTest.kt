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
        override suspend fun updateGeneSequence(sequence: GeneSequence) {
            val index = sequencesList.indexOfFirst { it.sequence == sequence.sequence }
            if (index != -1) sequencesList[index] = sequence
        }
        override suspend fun deleteGeneSequence(sequence: GeneSequence) {
            sequencesList.remove(sequence)
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

        override suspend fun saveGeminiApiKey(apiKey: String) {
            apiKeyFlow.value = apiKey
        }
        override suspend fun setMuteSound(mute: Boolean) {
            muteFlow.value = mute
        }
        override suspend fun setScanRadius(radius: Float) {
            radiusFlow.value = radius
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
}

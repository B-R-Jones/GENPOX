package com.example.genpox

import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import com.example.genpox.data.network.HydraPacket
import com.example.genpox.data.network.PoxNetworkInterface
import com.example.genpox.data.network.PoxNetworkManager
import com.example.genpox.ui.main.NetworkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class P2PHackingTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class TestDataRepository : DataRepository {
        val creatures = mutableListOf<Creature>()
        val geneSequences = mutableListOf<GeneSequence>()
        val creaturesFlow = MutableStateFlow<List<Creature>>(emptyList())
        val geneSequencesFlow = MutableStateFlow<List<GeneSequence>>(emptyList())

        var stockA = 1000L
        var stockG = 1000L
        var stockT = 1000L
        var stockC = 1000L

        override val allCreatures: Flow<List<Creature>> = creaturesFlow
        override suspend fun getCreatureById(id: String): Creature? = creatures.find { it.id == id }
        override suspend fun insertCreature(creature: Creature) {
            val idx = creatures.indexOfFirst { it.id == creature.id }
            if (idx != -1) {
                creatures[idx] = creature
            } else {
                creatures.add(creature)
            }
            creaturesFlow.value = creatures.toList()
        }
        override suspend fun updateCreature(creature: Creature) {
            val idx = creatures.indexOfFirst { it.id == creature.id }
            if (idx != -1) {
                creatures[idx] = creature
                creaturesFlow.value = creatures.toList()
            }
        }
        override suspend fun deleteCreature(creature: Creature) {
            creatures.remove(creature)
            creaturesFlow.value = creatures.toList()
        }
        override suspend fun deleteAllCreatures() {
            creatures.clear()
            creaturesFlow.value = emptyList()
        }

        override val allGeneSequences: Flow<List<GeneSequence>> = geneSequencesFlow
        override suspend fun insertGeneSequence(sequence: GeneSequence) {
            geneSequences.add(sequence)
            geneSequencesFlow.value = geneSequences.toList()
        }
        override suspend fun insertGeneSequences(sequences: List<GeneSequence>) {
            geneSequences.addAll(sequences)
            geneSequencesFlow.value = geneSequences.toList()
        }
        override suspend fun updateGeneSequence(sequence: GeneSequence) {
            val idx = geneSequences.indexOfFirst { it.sequence == sequence.sequence }
            if (idx != -1) {
                geneSequences[idx] = sequence
                geneSequencesFlow.value = geneSequences.toList()
            }
        }
        override suspend fun deleteGeneSequence(sequence: GeneSequence) {
            geneSequences.remove(sequence)
            geneSequencesFlow.value = geneSequences.toList()
        }
        override suspend fun deleteAllGeneSequences() {
            geneSequences.clear()
            geneSequencesFlow.value = emptyList()
        }
        override suspend fun updateGeneStock(toInsertOrUpdate: List<GeneSequence>, toDelete: List<GeneSequence>) {}

        override val activeMissions: Flow<List<HarvestMission>> = flowOf(emptyList())
        override val allMissions: Flow<List<HarvestMission>> = flowOf(emptyList())
        override suspend fun insertMission(mission: HarvestMission) {}
        override suspend fun updateMission(mission: HarvestMission) {}

        override val geminiApiKey: Flow<String> = flowOf("")
        override val muteSound: Flow<Boolean> = flowOf(false)
        override val scanRadius: Flow<Float> = flowOf(55f)
        override val targetSequence: Flow<String> = flowOf("")
        override val rawStockA: Flow<Long> get() = flowOf(stockA)
        override val rawStockG: Flow<Long> get() = flowOf(stockG)
        override val rawStockT: Flow<Long> get() = flowOf(stockT)
        override val rawStockC: Flow<Long> get() = flowOf(stockC)
        override val bioWaste: Flow<Long> = flowOf(0L)
        override val splicerSlots: Flow<List<String?>> = flowOf(emptyList())
        override val splicerQScores: Flow<List<Double>> = flowOf(emptyList())
        override val splicerSignatures: Flow<List<String>> = flowOf(emptyList())
        override val selectedHostPlanet: Flow<String> = flowOf("EARTH")

        override suspend fun saveGeminiApiKey(apiKey: String) {}
        override suspend fun setMuteSound(mute: Boolean) {}
        override suspend fun setScanRadius(radius: Float) {}
        override suspend fun saveTargetSequence(seq: String) {}
        override suspend fun saveRawStocks(a: Long, g: Long, t: Long, c: Long) {
            stockA = a
            stockG = g
            stockT = t
            stockC = c
        }
        override suspend fun saveBioWaste(waste: Long) {}
        override suspend fun saveSplicerState(slots: List<String?>, qScores: List<Double>, signatures: List<String>) {}
        override suspend fun saveSelectedHostPlanet(planet: String) {}

        override val allReagentStock: Flow<List<ReagentStock>> = flowOf(emptyList())
        override suspend fun insertReagentStock(reagents: List<ReagentStock>) {}
        override suspend fun decrementReagentStock(id: String) {}
    }

    private class FakeNetworkManager(
        override val activeConnections: StateFlow<Map<String, String>> = MutableStateFlow(emptyMap()),
        private val onPacketSent: (String, HydraPacket) -> Unit,
    ) : PoxNetworkInterface {
        override val isAdvertising: StateFlow<Boolean> = MutableStateFlow(false)
        override val isDiscovering: StateFlow<Boolean> = MutableStateFlow(false)
        override val discoveredPeers: StateFlow<List<PoxNetworkManager.DiscoveredPeer>> = MutableStateFlow(emptyList())
        override val connectionLogs: StateFlow<List<String>> = MutableStateFlow(emptyList())
        override val incomingPackets: SharedFlow<Pair<String, HydraPacket>> = MutableSharedFlow()

        override fun sendPacket(endpointId: String, packet: HydraPacket) {
            onPacketSent(endpointId, packet)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createTestCreature(
        id: String,
        name: String,
        isAutoHacker: Boolean = false,
        isAutoDefender: Boolean = false,
        speed: Int = 50,
        attack: Int = 30,
        defense: Int = 20,
        vitality: Int = 100,
        telomeres: Int = 100,
    ): Creature = Creature(
        id = id,
        sequence = "A".repeat(80),
        name = name,
        faction = "Infection",
        type = "Heavy Hybrid",
        vitality = vitality,
        attack = attack,
        defense = defense,
        speed = speed,
        primaryWeapon = "Jaw",
        lore = "Test lore",
        asciiArt = "...",
        discoveredAt = System.currentTimeMillis(),
        origin = "Local test",
        isAutoHacker = isAutoHacker,
        isAutoDefender = isAutoDefender,
        telomeres = telomeres,
    )

    @Test
    fun testHackingIntrusionFiresCounterResponse() = testScope.runTest {
        val repo = TestDataRepository()
        val synth = PoxSynthManager().apply { isMuted = true }
        val dispatchers = com.example.genpox.util.TestDispatchersProvider()
        val viewModel = NetworkViewModel(
            creatureRepository = repo,
            geneStockRepository = repo,
            preferencesRepository = repo,
            synthManager = synth,
            dispatchers = dispatchers,
        )

        // Set up local defender
        val defender = createTestCreature("def-1", "DefenderSpecimen", isAutoDefender = true)
        repo.insertCreature(defender)

        var sentPacket: HydraPacket? = null
        val netManager = FakeNetworkManager { _, packet ->
            sentPacket = packet
        }
        viewModel.networkManager = netManager

        // Trigger intrusion from remote node
        val intruder = createTestCreature("int-1", "IntruderSpecimen", isAutoHacker = true)
        val intrusionPacket = HydraPacket.HackingIntrusion(hacker = intruder, targetNodeId = "MyNodeId")

        viewModel.receiveIncomingPacket("endpoint-123", "RemoteNode", intrusionPacket)
        testScheduler.advanceUntilIdle()

        // Verify that HackingCounterResponse was sent back
        assertNotNull(sentPacket)
        assertTrue(sentPacket is HydraPacket.HackingCounterResponse)
        val response = sentPacket as HydraPacket.HackingCounterResponse
        assertEquals("MyNodeId", response.targetNodeId)
        assertEquals("DefenderSpecimen", response.defender?.name)
        assertEquals(1, response.vaultAutoDefenders.size)
        assertEquals(50.0, response.baseFirewallComplexity, 0.0)
    }

    @Test
    fun testHackingCounterResponseRunsSimulationAndSendsReport() = testScope.runTest {
        val repo = TestDataRepository()
        val synth = PoxSynthManager().apply { isMuted = true }
        val dispatchers = com.example.genpox.util.TestDispatchersProvider()
        val viewModel = NetworkViewModel(
            creatureRepository = repo,
            geneStockRepository = repo,
            preferencesRepository = repo,
            synthManager = synth,
            dispatchers = dispatchers,
        )

        // Set up local auto-hacker
        val hacker = createTestCreature("hack-1", "HackerSpecimen", isAutoHacker = true, speed = 80, attack = 40)
        repo.insertCreature(hacker)

        var sentPacket: HydraPacket? = null
        val netManager = FakeNetworkManager { _, packet ->
            sentPacket = packet
        }
        viewModel.networkManager = netManager

        // Remote defender response
        val remoteDefender = createTestCreature("def-2", "RemoteDefenderSpecimen", isAutoDefender = true, defense = 10)
        val counterResponse = HydraPacket.HackingCounterResponse(
            targetNodeId = "RemoteNodeId",
            defender = remoteDefender,
            vaultAutoDefenders = listOf(remoteDefender),
            baseFirewallComplexity = 40.0,
        )

        viewModel.receiveIncomingPacket("endpoint-123", "RemoteNode", counterResponse)
        testScheduler.advanceUntilIdle()

        // Verify that HackingResultReport was sent
        assertNotNull(sentPacket)
        assertTrue(sentPacket is HydraPacket.HackingResultReport)
        val report = sentPacket as HydraPacket.HackingResultReport
        assertEquals("RemoteNodeId", report.targetNodeId)

        // Verify that the local hacker telomeres were updated in repository
        val updatedHacker = repo.getCreatureById("hack-1")
        assertNotNull(updatedHacker)
        assertEquals(report.hackerFinalTelomeres, updatedHacker!!.telomeres)
    }

    @Test
    fun testHackingResultReportAppliesDefenderDamage() = testScope.runTest {
        val repo = TestDataRepository()
        val synth = PoxSynthManager().apply { isMuted = true }
        val dispatchers = com.example.genpox.util.TestDispatchersProvider()
        val viewModel = NetworkViewModel(
            creatureRepository = repo,
            geneStockRepository = repo,
            preferencesRepository = repo,
            synthManager = synth,
            dispatchers = dispatchers,
        )

        // Set up local defender
        val defender = createTestCreature("def-1", "MyDefenderSpecimen", isAutoDefender = true, telomeres = 100)
        repo.insertCreature(defender)

        // Receive report that our defender was damaged to 80 telomeres
        val report = HydraPacket.HackingResultReport(
            targetNodeId = "MyNodeId",
            success = false,
            logs = listOf("Combat logged"),
            siphonedPayload = emptyList(),
            hackerFinalTelomeres = 85,
            defenderFinalTelomeres = 80,
        )

        viewModel.receiveIncomingPacket("endpoint-123", "RemoteNode", report)
        testScheduler.advanceUntilIdle()

        // Verify local defender telomeres updated to 80
        val updatedDefender = repo.getCreatureById("def-1")
        assertNotNull(updatedDefender)
        assertEquals(80, updatedDefender!!.telomeres)
    }
}

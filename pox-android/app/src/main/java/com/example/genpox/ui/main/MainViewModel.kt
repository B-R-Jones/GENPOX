package com.example.genpox.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.util.UUID
import kotlin.math.sin

class MainViewModel(private val repository: DataRepository) : ViewModel() {

    val synthManager = PoxSynthManager()

    // State flows from Repository
    val creatures: StateFlow<List<Creature>> = repository.allCreatures
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val geneSequences: StateFlow<List<GeneSequence>> = repository.allGeneSequences
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMissions: StateFlow<List<HarvestMission>> = repository.activeMissions
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val harvestingCreatureIds: StateFlow<Set<String>> = repository.activeMissions
        .map { missions -> missions.filter { !it.isReturned }.map { it.creatureId }.toSet() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val activeMissionCoords: StateFlow<Set<String>> = repository.activeMissions
        .map { missions -> missions.filter { !it.isReturned }.map { "${it.lat},${it.lng}" }.toSet() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allMissions: StateFlow<List<HarvestMission>> = repository.allMissions
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val depletedAnomalyCoords: StateFlow<Set<String>> = repository.allMissions
        .map { missions -> missions.filter { it.isReturned }.map { "${it.lat},${it.lng}" }.toSet() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val geminiApiKey: StateFlow<String> = repository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val muteSound: StateFlow<Boolean> = repository.muteSound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val scanRadius: StateFlow<Float> = repository.scanRadius
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 55f)



    private val _showScanner = MutableStateFlow(false)
    val showScanner: StateFlow<Boolean> = _showScanner.asStateFlow()

    private val _trackedMissionId = MutableStateFlow<String?>(null)
    val trackedMissionId: StateFlow<String?> = _trackedMissionId.asStateFlow()

    fun setTrackedMissionId(id: String?) {
        _trackedMissionId.value = id
    }

    // UI Local State Flow
    private val _selectedTab = MutableStateFlow("combinator")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _activeCreature = MutableStateFlow<Creature?>(null)
    val activeCreature: StateFlow<Creature?> = _activeCreature.asStateFlow()

    private val _creatureCardOpenedFrom = MutableStateFlow<String?>("vault")
    val creatureCardOpenedFrom: StateFlow<String?> = _creatureCardOpenedFrom.asStateFlow()

    private val _defenderCreatureId = MutableStateFlow<String?>(null)
    val defenderCreatureId: StateFlow<String?> = _defenderCreatureId.asStateFlow()

    private val _disintegratedModal = MutableStateFlow<DisintegratedModalData?>(null)
    val disintegratedModal: StateFlow<DisintegratedModalData?> = _disintegratedModal.asStateFlow()

    fun clearDisintegratedModal() {
        _disintegratedModal.value = null
    }

    private val _terminalLogs = MutableStateFlow<List<String>>(listOf("GENPOX COMPILER SYSTEM v2.0 READY."))
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _selectedSplicerCreature = MutableStateFlow<Creature?>(null)
    val selectedSplicerCreature: StateFlow<Creature?> = _selectedSplicerCreature.asStateFlow()

    private val _selectedGeneToAppend = MutableStateFlow<String?>(null)
    val selectedGeneToAppend: StateFlow<String?> = _selectedGeneToAppend.asStateFlow()

    private val _isCompiling = MutableStateFlow(false)
    val isCompiling: StateFlow<Boolean> = _isCompiling.asStateFlow()

    // Bio-Lab Subtab Navigation State
    private val _bioLabSubTab = MutableStateFlow("pox")
    val bioLabSubTab: StateFlow<String> = _bioLabSubTab.asStateFlow()

    // Scanner Subtab Hoisted Navigation State
    private val _scannerSubTab = MutableStateFlow("list")
    val scannerSubTab: StateFlow<String> = _scannerSubTab.asStateFlow()

    fun setScannerSubTab(subTab: String) {
        _scannerSubTab.value = subTab
    }

    // P.O.X. Reactor active state
    private val _poxReactorActive = MutableStateFlow(true)
    val poxReactorActive: StateFlow<Boolean> = _poxReactorActive.asStateFlow()

    // Countdown remaining (seconds) until next synthesis cycle for standard P.O.X. reactor
    private val _poxIdleTime = MutableStateFlow(16)
    val poxIdleTime: StateFlow<Int> = _poxIdleTime.asStateFlow()

    // Countdown remaining (seconds) until next synthesis cycle for Anomaly Engine
    private val _anomalyIdleTime = MutableStateFlow(16)
    val anomalyIdleTime: StateFlow<Int> = _anomalyIdleTime.asStateFlow()

    // Active reactor boost seconds remaining
    private val _boostSecondsLeft = MutableStateFlow(0)
    val boostSecondsLeft: StateFlow<Int> = _boostSecondsLeft.asStateFlow()

    // Anomaly engine unstable fusion state switch
    private val _anomalyEngineActive = MutableStateFlow(false)
    val anomalyEngineActive: StateFlow<Boolean> = _anomalyEngineActive.asStateFlow()

    // Chronological log of past synthesized gene packets
    private val _discoveredPacketsLog = MutableStateFlow<List<GenePacket>>(emptyList())
    val discoveredPacketsLog: StateFlow<List<GenePacket>> = _discoveredPacketsLog.asStateFlow()

    // 8-character string mimicking visual synthesizer readout scrolling
    private val _scrollingGene = MutableStateFlow("--------")
    val scrollingGene: StateFlow<String> = _scrollingGene.asStateFlow()

    // Splicer/Constructor State Flows
    private val _targetSequence = MutableStateFlow("")
    val targetSequence: StateFlow<String> = _targetSequence.asStateFlow()

    private val _splicerSlots = MutableStateFlow<List<String?>>(List<String?>(8) { null })
    val splicerSlots: StateFlow<List<String?>> = _splicerSlots.asStateFlow()

    private val _activeSlotSelection = MutableStateFlow<Int?>(null)
    val activeSlotSelection: StateFlow<Int?> = _activeSlotSelection.asStateFlow()

    private val _slotSequenceFilter = MutableStateFlow("")
    val slotSequenceFilter: StateFlow<String> = _slotSequenceFilter.asStateFlow()

    private val _isReactorFrozen = MutableStateFlow(false)
    val isReactorFrozen: StateFlow<Boolean> = _isReactorFrozen.asStateFlow()

    private val _reactorFreezeTimeLeft = MutableStateFlow(0f)
    val reactorFreezeTimeLeft: StateFlow<Float> = _reactorFreezeTimeLeft.asStateFlow()

    private val _isForcedConstructionActive = MutableStateFlow(false)
    val isForcedConstructionActive: StateFlow<Boolean> = _isForcedConstructionActive.asStateFlow()

    private val _isForcedLoopActive = MutableStateFlow(false)
    val isForcedLoopActive: StateFlow<Boolean> = _isForcedLoopActive.asStateFlow()

    private val _forcedConstructionLogs = MutableStateFlow<List<String>>(emptyList())
    val forcedConstructionLogs: StateFlow<List<String>> = _forcedConstructionLogs.asStateFlow()

    private val _isSplicing = MutableStateFlow(false)
    val isSplicing: StateFlow<Boolean> = _isSplicing.asStateFlow()

    private val _splicingProgress = MutableStateFlow(0)
    val splicingProgress: StateFlow<Int> = _splicingProgress.asStateFlow()

    private val _devForceAnomaly = MutableStateFlow(false)
    val devForceAnomaly: StateFlow<Boolean> = _devForceAnomaly.asStateFlow()

    fun toggleDevForceAnomaly() {
        _devForceAnomaly.value = !_devForceAnomaly.value
        addLog("DEV: GNPX Mode toggled to ${_devForceAnomaly.value}")
        synthManager.playBeep(650f, 0.05f, "sine")
    }

    fun addDevGenes() {
        viewModelScope.launch(Dispatchers.IO) {
            val bases = listOf("A", "G", "T", "C")
            val random = java.util.Random()
            
            val existingMap = geneSequences.value.associateBy { it.sequence }.toMutableMap()
            
            repeat(10000) {
                var seq = ""
                repeat(8) {
                    seq += bases[random.nextInt(4)]
                }
                val existing = existingMap[seq]
                if (existing != null) {
                    existingMap[seq] = existing.copy(count = existing.count + 1)
                } else {
                    existingMap[seq] = GeneSequence(seq, 1, System.currentTimeMillis())
                }
            }
            
            repository.insertGeneSequences(existingMap.values.toList())
            addLog("DEV: Injected 10,000 random genes into biological inventory.")
            synthManager.playSynthesisSuccess()
        }
    }

    // Unified metrics model to compute inventory statistics in a single pass on a background thread
    data class GeneInventoryMetrics(
        val geneSequenceStrings: List<String> = emptyList(),
        val uniqueGenesSize: Int = 0,
        val multiCountGenesSize: Int = 0,
        val anomalousGenesList: List<GeneSequence> = emptyList(),
        val grandTotalStandardNucleotides: Long = 0L,
        val countA: Int = 0,
        val countG: Int = 0,
        val countT: Int = 0,
        val countC: Int = 0
    )

    val inventoryMetrics: StateFlow<GeneInventoryMetrics> = geneSequences
        .map { list ->
            var countA = 0
            var countG = 0
            var countT = 0
            var countC = 0
            var grandTotal = 0L
            var multiCount = 0
            val strings = ArrayList<String>(list.size)
            val anomalousList = ArrayList<GeneSequence>()

            for (i in 0 until list.size) {
                val gene = list[i]
                val seq = gene.sequence
                strings.add(seq)
                if (gene.count > 1) {
                    multiCount++
                }

                val isAnomalous = WaveMath.isAnomalousGene(seq)
                if (isAnomalous) {
                    anomalousList.add(gene)
                } else {
                    if (gene.count > 0) {
                        grandTotal += seq.length
                        for (cIdx in 0 until seq.length) {
                            when (seq[cIdx]) {
                                'A', 'a' -> countA += gene.count
                                'G', 'g' -> countG += gene.count
                                'T', 't' -> countT += gene.count
                                'C', 'c' -> countC += gene.count
                            }
                        }
                    }
                }
            }

            GeneInventoryMetrics(
                geneSequenceStrings = strings,
                uniqueGenesSize = list.size,
                multiCountGenesSize = multiCount,
                anomalousGenesList = anomalousList,
                grandTotalStandardNucleotides = grandTotal,
                countA = countA,
                countG = countG,
                countT = countT,
                countC = countC
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeneInventoryMetrics())

    val grandTotalStandardNucleotides: StateFlow<Long> = inventoryMetrics
        .map { it.grandTotalStandardNucleotides }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val geneSequenceStrings: StateFlow<List<String>> = inventoryMetrics
        .map { it.geneSequenceStrings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueGenesSize: StateFlow<Int> = inventoryMetrics
        .map { it.uniqueGenesSize }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val multiCountGenesSize: StateFlow<Int> = inventoryMetrics
        .map { it.multiCountGenesSize }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val anomalousGenesList: StateFlow<List<GeneSequence>> = inventoryMetrics
        .map { it.anomalousGenesList }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countA: StateFlow<Int> = inventoryMetrics
        .map { it.countA }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countG: StateFlow<Int> = inventoryMetrics
        .map { it.countG }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countT: StateFlow<Int> = inventoryMetrics
        .map { it.countT }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val countC: StateFlow<Int> = inventoryMetrics
        .map { it.countC }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Map location and anomalies
    private val _latitude = MutableStateFlow(37.4220) // Default Palo Alto coordinates
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(-122.0841)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _roads = MutableStateFlow<List<List<Pair<Double, Double>>>>(emptyList())
    val roads: StateFlow<List<List<Pair<Double, Double>>>> = _roads.asStateFlow()

    private val _cachedCells = MutableStateFlow<List<String>>(emptyList())
    val cachedCells: StateFlow<List<String>> = _cachedCells.asStateFlow()

    private val _proceduralHeatwaves = MutableStateFlow<List<String>>(emptyList())

    val heatwaveCells: StateFlow<List<String>> = combine(_cachedCells, _proceduralHeatwaves) { cached, heatwaves ->
        (cached + heatwaves).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _zoomMultiplier = MutableStateFlow(1.0f)
    val zoomMultiplier: StateFlow<Float> = _zoomMultiplier.asStateFlow()

    private var lastCheckedCellKey: String? = null
    private var lastFetchedCellRadius: Int = 1
    private var fetchRoadsJob: kotlinx.coroutines.Job? = null



    private fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dx = (lng2 - lng1) * 111000.0 * kotlin.math.cos(Math.toRadians(lat1))
        val dy = (lat2 - lat1) * 111000.0
        val distMeters = kotlin.math.sqrt(dx * dx + dy * dy)
        return distMeters * 3.28084
    }

    // Anomalies (Procedural nearby points)
    private val _rawAnomalies = MutableStateFlow<List<PoxAnomaly>>(emptyList())
    val anomalies: StateFlow<List<PoxAnomaly>> = combine(
        _rawAnomalies, activeMissions, _latitude, _longitude
    ) { rawList, missions, userLat, userLng ->
        val activeMissionsList = missions.filter { !it.isReturned }
        val pinnedAnomalies = activeMissionsList.map { m ->
            val existing = rawList.find { Math.abs(it.lat - m.lat) < 0.0001 && Math.abs(it.lng - m.lng) < 0.0001 }
            if (existing != null) {
                existing
            } else {
                val animId = "ANM-ACTIVE-${m.creatureId}"
                val animName = "Active Anomaly"
                val dist = calculateDistanceInFeet(userLat, userLng, m.lat, m.lng)
                PoxAnomaly(
                    id = animId,
                    name = animName,
                    lat = m.lat,
                    lng = m.lng,
                    gene = m.harvestedGenes.firstOrNull() ?: "AGTCGTAC",
                    faction = m.creatureFaction,
                    distance = dist,
                    heatZoneDiameter = 500.0 * 3.28084,
                    density = 0.0
                )
            }
        }
        val remaining = rawList.filter { raw -> pinnedAnomalies.none { pinned -> Math.abs(pinned.lat - raw.lat) < 0.0001 && Math.abs(pinned.lng - raw.lng) < 0.0001 } }
        
        val updatedPinned = pinnedAnomalies.map {
            it.copy(distance = calculateDistanceInFeet(userLat, userLng, it.lat, it.lng))
        }
        val updatedRemaining = remaining.map {
            it.copy(distance = calculateDistanceInFeet(userLat, userLng, it.lat, it.lng))
        }
        
        updatedPinned + updatedRemaining
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAnomalyId = MutableStateFlow<String?>(null)
    val selectedAnomalyId: StateFlow<String?> = _selectedAnomalyId.asStateFlow()

    val selectedAnomaly: StateFlow<PoxAnomaly?> = combine(selectedAnomalyId, anomalies) { id, list ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Daily Bounty Sequences
    val dailyBounties = flow {
        emit(generateDailyBounties())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Observe mute settings and sync to synthesizer
        viewModelScope.launch {
            muteSound.collect { mute ->
                synthManager.isMuted = mute
            }
        }
        // Observe and load target sequence
        viewModelScope.launch {
            repository.targetSequence.collect { seq ->
                if (seq.isEmpty()) {
                    val initialSeq = generateRandom64Sequence()
                    repository.saveTargetSequence(initialSeq)
                    _targetSequence.value = initialSeq
                } else {
                    _targetSequence.value = seq
                }
            }
        }
        // Generate anomalies on initialization
        generateNearbyAnomalies(37.4220, -122.0841)

        // Start Periodic Reactor Heartbeat (Tick Engine)
        startReactorHeartbeat()

        // Start scrolling visual effect
        startScrollingGeneTicker()

        // Load initial cached cells from database
        loadCachedCells()

        // Start dynamic heatwave simulation
        startHeatwaveSimulation()
    }

    fun selectTab(tab: String) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            addLog("NAV: Transition to section [${tab.uppercase()}]")
            synthManager.playCombinatorTick()
        }
    }

    fun setSelectedAnomalyId(id: String?) {
        _selectedAnomalyId.value = id
        if (id != null) {
            addLog("LOCKED_TARGET: Coupled target ID $id")
        } else {
            addLog("LOCKED_TARGET: Released lock channels.")
        }
    }

    fun setActiveCreature(creature: Creature?, openedFrom: String = "vault") {
        _activeCreature.value = creature
        _creatureCardOpenedFrom.value = openedFrom
        if (creature != null) {
            synthManager.playCreatureSequenceAudio(creature.sequence)
        }
    }

    fun toggleFavoriteCreature(creature: Creature) {
        viewModelScope.launch {
            val updated = creature.copy(isFavorite = !creature.isFavorite)
            repository.insertCreature(updated)
            _activeCreature.value = updated
            synthManager.playCombinatorTick()
            addLog(if (updated.isFavorite) "FAV: Added \"${creature.name}\" to favorites." else "FAV: Removed \"${creature.name}\" from favorites.")
        }
    }

    fun toggleAutoHackerCreature(creature: Creature) {
        viewModelScope.launch {
            val updated = creature.copy(isAutoHacker = !creature.isAutoHacker)
            repository.insertCreature(updated)
            _activeCreature.value = updated
            synthManager.playBeep(520f, 0.05f, "sine")
            addLog(if (updated.isAutoHacker) "HACK: Designated \"${creature.name}\" as Auto-Hacker." else "HACK: Deactivated Auto-Hacker for \"${creature.name}\".")
        }
    }

    fun toggleDefenderCreature(id: String) {
        _defenderCreatureId.value = if (_defenderCreatureId.value == id) null else id
        synthManager.playBeep(440f, 0.05f, "sine")
        val active = _defenderCreatureId.value == id
        addLog(if (active) "DEF: Designated defender." else "DEF: Cleared defender designation.")
    }

    fun incinerateCreature(creature: Creature, harvestedGene: String) {
        viewModelScope.launch {
            repository.deleteCreature(creature)
            val existing = geneSequences.value.find { it.sequence == harvestedGene }
            if (existing != null) {
                repository.insertGeneSequence(existing.copy(count = existing.count + 1))
            } else {
                repository.insertGeneSequence(GeneSequence(harvestedGene, 1, System.currentTimeMillis()))
            }
            addLog("PURGE: Purged specimen \"${creature.name}\". Segment cached in stock: $harvestedGene")
            synthManager.playBeep(400f, 0.4f, "sawtooth")
            setActiveCreature(null)
        }
    }

    fun decreaseCreatureTelomeres(creatureId: String, amount: Int, reason: String) {
        viewModelScope.launch {
            val creature = repository.getCreatureById(creatureId) ?: return@launch
            val nextT = (creature.telomeres - amount).coerceAtLeast(0)
            if (nextT <= 0) {
                // Creature is completely destroyed!
                val genome = creature.sequence
                val blocks = mutableListOf<String>()
                for (i in 0 until 8) {
                    if (i * 8 + 8 <= genome.length) {
                        blocks.add(genome.substring(i * 8, i * 8 + 8))
                    }
                }
                creature.appendedGenes.forEach { gene ->
                    if (gene.length == 8) {
                        blocks.add(gene)
                    }
                }

                // Return a portion (50%, rounded up) and permanently destroy the rest:
                val returnCount = (blocks.size + 1) / 2
                val shuffled = blocks.shuffled()
                val returnedBlocks = shuffled.take(returnCount)
                val destroyedBlocks = shuffled.drop(returnCount)

                // Add returned blocks to player's sequences pool
                returnedBlocks.forEach { seq ->
                    val upper = seq.uppercase()
                    val existing = geneSequences.value.find { it.sequence == upper }
                    if (existing != null) {
                        repository.insertGeneSequence(existing.copy(count = existing.count + 1))
                    } else {
                        repository.insertGeneSequence(GeneSequence(upper, 1, System.currentTimeMillis()))
                    }
                }

                repository.deleteCreature(creature)
                if (_activeCreature.value?.id == creatureId) {
                    _activeCreature.value = null
                }
                if (_defenderCreatureId.value == creatureId) {
                    _defenderCreatureId.value = null
                }

                _disintegratedModal.value = DisintegratedModalData(
                    name = creature.name,
                    returnedBlocks = returnedBlocks,
                    destroyedBlocks = destroyedBlocks
                )

                addLog("🚨 CHROMOSOMAL FAILURE: \"${creature.name}\" disintegrated! Telomeres reached 0%. Returned ${returnedBlocks.size} gene blocks, lost ${destroyedBlocks.size} permanently!")
                synthManager.playReject()
            } else {
                val updated = creature.copy(telomeres = nextT)
                repository.insertCreature(updated)
                if (_activeCreature.value?.id == creatureId) {
                    _activeCreature.value = updated
                }
                addLog("🧬 CELLULAR INTEGRITY: \"${creature.name}\" telomeres shortened by -$amount% due to $reason. [Current life: $nextT%]")
            }
        }
    }

    fun appendGeneToActiveCreature(creature: Creature, gene: String) {
        viewModelScope.launch {
            val match = geneSequences.value.find { it.sequence == gene && it.count > 0 }
            if (match == null) {
                addLog("ERR: Gene $gene not in inventory.")
                synthManager.playReject()
                return@launch
            }

            val updatedGene = match.copy(count = match.count - 1)
            if (updatedGene.count == 0) {
                repository.deleteGeneSequence(match)
            } else {
                repository.insertGeneSequence(updatedGene)
            }

            val newSeq = creature.sequence + gene
            val newAppended = creature.appendedGenes + gene
            val details = constructProceduralDetails(newSeq)
            
            val updatedCreature = creature.copy(
                sequence = newSeq,
                appendedGenes = newAppended,
                vitality = details.vitality,
                attack = details.attack,
                defense = details.defense,
                speed = details.speed,
                type = details.type,
                faction = details.faction,
                lore = details.lore,
                primaryWeapon = details.primaryWeapon
            )
            repository.insertCreature(updatedCreature)

            addLog("MUTATION: Welded gene $gene to \"${creature.name}\". Stats updated.")
            synthManager.playSynthesisSuccess()
            _activeCreature.value = updatedCreature
        }
    }

    private fun constructProceduralDetails(sequence: String): ProceduralDetails {
        var countA = 0
        var countG = 0
        var countT = 0
        var countC = 0
        for (char in sequence.uppercase()) {
            when (char) {
                'A' -> countA++
                'G' -> countG++
                'T' -> countT++
                'C' -> countC++
            }
        }
        
        var faction = "Infection"
        val maxB = maxOf(countA, countG, countT, countC)
        if (maxB == countA) faction = "Containment"
        else if (maxB == countG) faction = "Infection"
        else if (maxB == countT) faction = "Mech"
        else if (maxB == countC) faction = "Parasite"

        var type = "Parasitic Cyborg Insectoid"
        if (faction == "Infection") type = "Organic Swarm Pathogen"
        if (faction == "Mech") type = "Autonomous Chasis Chitin"
        if (faction == "Containment") type = "Bacterial Nano Containment Grid"

        val vitality = 100 + countA * 5 + (sequence.length / 2)
        val attack = minOf(99, 15 + countG * 3)
        val defense = minOf(99, 15 + countT * 3)
        val speed = minOf(99, 15 + countC * 3)

        val rawWeapons = listOf("Corrosive Spite-Needle", "Vaporizing Plasma-Claw", "Shocking Laser-Stinger", "Bio-Acid Venting-Pod", "Micro-Phage Injector")
        val weaponIndex = (countA + countG + countT + countC) % rawWeapons.size
        val primaryWeapon = rawWeapons[weaponIndex]

        val lore = "Synthesized from custom sub-gene segments. Displays aggressive ${faction.lowercase()}-sector behavior with optimized $primaryWeapon payloads. Integrated weapon clusters operate at $attack-rating."

        return ProceduralDetails(
            faction = faction,
            type = type,
            vitality = vitality,
            attack = attack,
            defense = defense,
            speed = speed,
            primaryWeapon = primaryWeapon,
            lore = lore
        )
    }

    private data class ProceduralDetails(
        val faction: String,
        val type: String,
        val vitality: Int,
        val attack: Int,
        val defense: Int,
        val speed: Int,
        val primaryWeapon: String,
        val lore: String
    )

    fun getUnlockedMoves(sequence: String): List<UnlockedMove> {
        val list = mutableListOf<UnlockedMove>()
        if (sequence.length <= 64) return list

        if (sequence.length >= 72) {
            val gene1 = sequence.substring(64, 72)
            var ga = 0
            var tc = 0
            for (char in gene1.uppercase()) {
                if (char == 'A' || char == 'G') ga++
                else if (char == 'T' || char == 'C') tc++
            }
            if (ga >= tc) {
                list.add(
                    UnlockedMove(
                        type = "healing",
                        name = "BIO-DRAIN REPAIR",
                        description = "Repairs circuitry by siphoning 35 HP from defender."
                    )
                )
            } else {
                list.add(
                    UnlockedMove(
                        type = "evasive",
                        name = "QUANTUM ESCAPE DEVIATION",
                        description = "Evasive shift. Negates next attack damage in this round."
                    )
                )
            }
        }

        if (sequence.length >= 80) {
            val firstType = list.firstOrNull()?.type
            if (firstType == "healing") {
                list.add(
                    UnlockedMove(
                        type = "evasive",
                        name = "ELECTROMAGNETIC SHELL DEFLECT",
                        description = "Hardened shield. Negates defender damage for this round."
                    )
                )
            } else {
                list.add(
                    UnlockedMove(
                        type = "healing",
                        name = "MICRO-PHAGE EXTRACTION",
                        description = "Siphons 35 HP from opponent to mend system hardware."
                    )
                )
            }
        }

        return list
    }

    fun getEmotSoundDetails(sequence: String): EmotSound {
        val chars = sequence.uppercase().replace(Regex("[^AGTC]"), "")
        if (chars.isEmpty()) {
            return EmotSound("none", emptyList(), 0.08, 0.06)
        }
        val numNotes = minOf(8, maxOf(4, chars.length / 8))
        var acount = 0
        var gcount = 0
        var tcount = 0
        var ccount = 0
        for (char in chars) {
            when (char) {
                'A' -> acount++
                'G' -> gcount++
                'T' -> tcount++
                'C' -> ccount++
            }
        }
        val maxCount = maxOf(acount, gcount, tcount, ccount)
        val oscType = when (maxCount) {
            acount -> "sine"
            gcount -> "triangle"
            tcount -> "sawtooth"
            else -> "square"
        }
        val noteDelay = 0.08 + (acount % 3) * 0.02
        val frequencies = mutableListOf<Int>()
        for (i in 0 until numNotes) {
            val start = i * 4
            val end = minOf(chars.length, (i + 1) * 4)
            if (start < chars.length) {
                val charBlock = chars.substring(start, end)
                var baseValue = 0
                for (char in charBlock) {
                    when (char) {
                        'A' -> baseValue += 1
                        'G' -> baseValue += 2
                        'T' -> baseValue += 3
                        'C' -> baseValue += 4
                    }
                }
                val freq = 180 + (baseValue * 30) + (i * 40)
                frequencies.add(freq)
            }
        }
        val noteDuration = 0.06 + (gcount % 4) * 0.03
        val roundedDelay = Math.round(noteDelay * 10000.0) / 10000.0
        val roundedDuration = Math.round(noteDuration * 10000.0) / 10000.0
        return EmotSound(oscType, frequencies, roundedDelay, roundedDuration)
    }

    fun encodeCreatureToBase64(creature: Creature): String {
        val sound = getEmotSoundDetails(creature.sequence)
        val moves = getUnlockedMoves(creature.sequence).map { it.name }
        
        val compact = buildJsonObject {
            put("id", creature.id)
            put("sequence", creature.sequence)
            put("name", creature.name)
            put("faction", creature.faction)
            put("type", creature.type)
            put("vitality", creature.vitality)
            put("attack", creature.attack)
            put("defense", creature.defense)
            put("spd", creature.speed)
            put("weapon", creature.primaryWeapon)
            put("lore", creature.lore)
            put("ascii", creature.asciiArt)
            put("appended", JsonArray(creature.appendedGenes.map { JsonPrimitive(it) }))
            put("moves", JsonArray(moves.map { JsonPrimitive(it) }))
            put("visual", buildJsonObject {
                put("faction", creature.faction)
                put("type", creature.type)
                put("ascii", creature.asciiArt)
            })
            put("sound", buildJsonObject {
                put("oscillator", sound.oscillator)
                put("frequencies", JsonArray(sound.frequencies.map { JsonPrimitive(it) }))
                put("noteDelay", sound.noteDelay)
                put("noteDuration", sound.noteDuration)
            })
            put("telomeres", creature.telomeres)
            put("isFullCoherence", creature.isFullCoherence)
            if (creature.coherenceType != null) {
                put("coherenceType", creature.coherenceType)
            }
        }
        val jsonStr = compact.toString()
        return android.util.Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
    }

    fun setSplicerCreature(creature: Creature?) {
        _selectedSplicerCreature.value = creature
        _selectedGeneToAppend.value = null
        if (creature != null) {
            synthManager.playBeep(660f, 0.05f, "triangle")
        }
    }

    fun selectGeneToAppend(gene: String?) {
        _selectedGeneToAppend.value = gene
        if (gene != null) {
            synthManager.playBeep(880f, 0.04f, "sine")
        }
    }

    fun addLog(log: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _terminalLogs.value = (_terminalLogs.value + "[$timeStr] $log").takeLast(30)
    }

    fun setMute(mute: Boolean) {
        viewModelScope.launch {
            repository.setMuteSound(mute)
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            repository.saveGeminiApiKey(key)
            addLog("SYS: Secure Gemini API Credentials Saved.")
            synthManager.playSynthesisSuccess()
        }
    }

    // Geolocation sync
    fun updateLocation(lat: Double, lng: Double) {
        _latitude.value = lat
        _longitude.value = lng
        generateNearbyAnomalies(lat, lng)
        fetchRoadsIfNeeded(lat, lng)
    }

    fun updateZoom(zoom: Float) {
        if (_zoomMultiplier.value != zoom) {
            _zoomMultiplier.value = zoom
            fetchRoadsIfNeeded(_latitude.value, _longitude.value)
        }
    }

    private fun getCellKey(lat: Double, lng: Double): String {
        val cellX = Math.floor(lat / 0.015).toInt()
        val cellY = Math.floor(lng / 0.015).toInt()
        return "$cellX,$cellY"
    }

    private fun addCachedCellToState(cellKey: String) {
        _cachedCells.value = (_cachedCells.value + cellKey).distinct()
    }

    private fun loadCachedCells() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAllCachedRoadCells()
            _cachedCells.value = list.map { it.cellKey }
        }
    }

    private fun startHeatwaveSimulation() {
        viewModelScope.launch(Dispatchers.Default) {
            val baseCities = listOf(
                Pair(37.4220, -122.0841), // Palo Alto
                Pair(40.7128, -74.0060),  // New York
                Pair(51.5074, -0.1278),   // London
                Pair(35.6762, 139.6503),  // Tokyo
                Pair(-33.8688, 151.2093)  // Sydney
            )
            var ticks = 0L
            while (isActive) {
                val list = mutableListOf<String>()
                baseCities.forEach { (baseLat, baseLng) ->
                    val cellX = Math.floor(baseLat / 0.015).toInt()
                    val cellY = Math.floor(baseLng / 0.015).toInt()
                    
                    list.add("$cellX,$cellY")
                    
                    val offset1X = (kotlin.math.sin(ticks * 0.1 + cellX) * 2).toInt()
                    val offset1Y = (kotlin.math.cos(ticks * 0.1 + cellY) * 2).toInt()
                    list.add("${cellX + offset1X},${cellY + offset1Y}")

                    val offset2X = (kotlin.math.cos(ticks * 0.07 - cellX) * 3).toInt()
                    val offset2Y = (kotlin.math.sin(ticks * 0.07 - cellY) * 3).toInt()
                    list.add("${cellX + offset2X},${cellY + offset2Y}")
                }
                _proceduralHeatwaves.value = list
                ticks++
                delay(3000)
            }
        }
    }

    private fun fetchRoadsIfNeeded(lat: Double, lng: Double) {
        val cellKey = getCellKey(lat, lng)
        val zoom = _zoomMultiplier.value
        val requiredRadius = kotlin.math.ceil((0.009 * zoom) / 0.015).toInt().coerceIn(1, 3)
        if (cellKey != lastCheckedCellKey || requiredRadius != lastFetchedCellRadius) {
            lastCheckedCellKey = cellKey
            lastFetchedCellRadius = requiredRadius
            fetchRoads(lat, lng)
        }
    }

    private fun generateFallbackRoads(lat: Double, lng: Double): List<List<RoadPoint>> {
        val result = mutableListOf<List<RoadPoint>>()
        val latStep = 0.00135
        val lngStep = 0.00135 / kotlin.math.cos(Math.toRadians(lat))
        
        val startLat = Math.floor(lat / latStep) * latStep - 37.0 * latStep
        val startLng = Math.floor(lng / lngStep) * lngStep - 37.0 * lngStep
        
        // Horizontal roads
        for (i in 0..74) {
            val rLat = startLat + i * latStep
            val path = mutableListOf<RoadPoint>()
            for (j in 0..74) {
                val rLng = startLng + j * lngStep
                path.add(RoadPoint(rLat, rLng))
            }
            result.add(path)
        }
        // Vertical roads
        for (j in 0..74) {
            val rLng = startLng + j * lngStep
            val path = mutableListOf<RoadPoint>()
            for (i in 0..74) {
                val rLat = startLat + i * latStep
                path.add(RoadPoint(rLat, rLng))
            }
            result.add(path)
        }
        return result
    }

    private fun fetchRoads(lat: Double, lng: Double) {
        android.util.Log.d("PoxRadar", "fetchRoads called for lat=$lat lng=$lng")
        val oldJob = fetchRoadsJob
        fetchRoadsJob = viewModelScope.launch(Dispatchers.IO) {
            oldJob?.let {
                it.cancel()
                try {
                    it.join()
                } catch (e: Exception) {
                    // Ignore join exceptions
                }
            }
            if (!isActive) return@launch
            
            val cellX = Math.floor(lat / 0.015).toInt()
            val cellY = Math.floor(lng / 0.015).toInt()

            val zoom = _zoomMultiplier.value
            val radius = kotlin.math.ceil((0.009 * zoom) / 0.015).toInt().coerceIn(1, 3)

            val cellsToQuery = mutableListOf<String>()
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    cellsToQuery.add("${cellX + dx},${cellY + dy}")
                }
            }

            val cachedCellsFromDb = mutableListOf<CachedRoadCell>()
            val now = System.currentTimeMillis()
            var allCachedAndValid = true

            for (key in cellsToQuery) {
                val dbCell = repository.getCachedRoadCell(key)
                if (dbCell != null && (now - dbCell.fetchedAt) < 24 * 60 * 60 * 1000L) { // 24 hours
                    cachedCellsFromDb.add(dbCell)
                } else {
                    allCachedAndValid = false
                }
            }

            if (allCachedAndValid) {
                android.util.Log.d("PoxRadar", "Active neighborhood cells valid. Populating max 7x7 map grid from DB cache.")
                addLog("SYS: Loaded map grid from local cache.")
                val maxCellsToLoad = mutableListOf<String>()
                for (dx in -3..3) {
                    for (dy in -3..3) {
                        maxCellsToLoad.add("${cellX + dx},${cellY + dy}")
                    }
                }
                val allRoads = mutableListOf<List<Pair<Double, Double>>>()
                maxCellsToLoad.forEach { key ->
                    val cell = repository.getCachedRoadCell(key)
                    if (cell != null && (now - cell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                        try {
                            val roadsList = Json.decodeFromString<List<List<RoadPoint>>>(cell.roadsJson)
                            roadsList.forEach { road ->
                                allRoads.add(road.map { Pair(it.lat, it.lng) })
                            }
                        } catch (e: Exception) {
                            // Ignore decode exceptions
                        }
                    }
                }
                _roads.value = allRoads
                return@launch
            }

            // At least one cell is missing or expired, fetch bounding box from Overpass
            val minLat = (cellX - radius) * 0.015
            val maxLat = (cellX + radius + 1) * 0.015
            val minLng = (cellY - radius) * 0.015
            val maxLng = (cellY + radius + 1) * 0.015
            
            val endpoints = listOf(
                "https://overpass-api.de/api/interpreter",
                "https://lz4.overpass-api.de/api/interpreter",
                "https://overpass.osm.ch/api/interpreter",
                "https://overpass.openstreetmap.fr/api/interpreter",
                "https://overpass.kumi.systems/api/interpreter"
            )
            
            var success = false
            val query = "[out:json];way($minLat,$minLng,$maxLat,$maxLng)[highway~\"motorway|trunk|primary|secondary|tertiary|unclassified|residential\"];out geom;"
            var fetchedRoads: List<List<RoadPoint>> = emptyList()

            for (baseUrl in endpoints) {
                if (!isActive) return@launch
                try {
                    android.util.Log.d("PoxRadar", "Querying OpenStreetMap from $baseUrl...")
                    addLog("SYS: Querying OpenStreetMap ($baseUrl)...")
                    val url = java.net.URL(baseUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("User-Agent", "GenPoxRadar/1.0 (brent@example.com)")
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connection.setRequestProperty("Accept", "*/*")
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.doOutput = true
                    
                    val postData = "data=" + java.net.URLEncoder.encode(query, "UTF-8")
                    connection.outputStream.use { os ->
                        val input = postData.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                        os.write(input, 0, input.size)
                    }
                    
                    if (!isActive) return@launch
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val text = connection.inputStream.bufferedReader().use { it.readText() }
                        if (!isActive) return@launch
                        
                        fetchedRoads = parseOverpassJson(text)
                        if (fetchedRoads.isNotEmpty()) {
                            android.util.Log.d("PoxRadar", "Loaded ${fetchedRoads.size} road segments from $baseUrl.")
                            addLog("SYS: Loaded ${fetchedRoads.size} road segments from $baseUrl.")
                            success = true
                            break
                        } else {
                            android.util.Log.w("PoxRadar", "Parsed 0 roads from $baseUrl response.")
                        }
                    } else {
                        android.util.Log.w("PoxRadar", "Endpoint $baseUrl returned HTTP $responseCode")
                        addLog("SYS: Endpoint $baseUrl failed: HTTP $responseCode")
                    }
                } catch (e: Exception) {
                    if (!isActive) return@launch
                    android.util.Log.e("PoxRadar", "Endpoint $baseUrl threw exception", e)
                    addLog("SYS: Endpoint $baseUrl error: ${e.message}")
                }
            }
            
            if (!success && isActive) {
                android.util.Log.w("PoxRadar", "All OSM endpoints failed/empty. Generating fallback grid.")
                addLog("SYS: All OpenStreetMap endpoints failed. Generating fallback grid.")
                fetchedRoads = generateFallbackRoads(lat, lng)
            }

            if (isActive) {
                // Distribute fetched roads into the 9 cells
                val cellRoadsMap = mutableMapOf<String, MutableList<List<RoadPoint>>>()
                cellsToQuery.forEach { key ->
                    cellRoadsMap[key] = mutableListOf()
                }

                fetchedRoads.forEach { way ->
                    if (way.size >= 2) {
                        cellsToQuery.forEach { cellKey ->
                            val parts = cellKey.split(",")
                            val cx = parts[0].toInt()
                            val cy = parts[1].toInt()

                            val currentSubPath = mutableListOf<RoadPoint>()
                            way.forEachIndexed { i, pt ->
                                val ptCellX = Math.floor(pt.lat / 0.015).toInt()
                                val ptCellY = Math.floor(pt.lng / 0.015).toInt()

                                val isSelfInCell = (ptCellX == cx && ptCellY == cy)
                                val isPrevInCell = i > 0 && Math.floor(way[i - 1].lat / 0.015).toInt() == cx && Math.floor(way[i - 1].lng / 0.015).toInt() == cy
                                val isNextInCell = i < way.size - 1 && Math.floor(way[i + 1].lat / 0.015).toInt() == cx && Math.floor(way[i + 1].lng / 0.015).toInt() == cy

                                if (isSelfInCell || isPrevInCell || isNextInCell) {
                                    currentSubPath.add(pt)
                                } else {
                                    if (currentSubPath.size >= 2) {
                                        cellRoadsMap[cellKey]?.add(currentSubPath.toList())
                                    }
                                    currentSubPath.clear()
                                }
                            }
                            if (currentSubPath.size >= 2) {
                                cellRoadsMap[cellKey]?.add(currentSubPath.toList())
                            }
                        }
                    }
                }

                // Cache all fetched cells in Room
                for ((key, roadsList) in cellRoadsMap) {
                    val roadsJson = Json.encodeToString(roadsList)
                    val cachedCell = CachedRoadCell(
                        cellKey = key,
                        roadsJson = roadsJson,
                        fetchedAt = now
                    )
                    repository.insertCachedRoadCell(cachedCell)
                    addCachedCellToState(key)
                }

                // Expose all valid cells from the 7x7 neighborhood to the UI flow
                val maxCellsToLoad = mutableListOf<String>()
                for (dx in -3..3) {
                    for (dy in -3..3) {
                        maxCellsToLoad.add("${cellX + dx},${cellY + dy}")
                    }
                }
                val allRoads = mutableListOf<List<Pair<Double, Double>>>()
                maxCellsToLoad.forEach { key ->
                    val cell = repository.getCachedRoadCell(key)
                    if (cell != null && (now - cell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                        try {
                            val roadsList = Json.decodeFromString<List<List<RoadPoint>>>(cell.roadsJson)
                            roadsList.forEach { road ->
                                allRoads.add(road.map { Pair(it.lat, it.lng) })
                            }
                        } catch (e: Exception) {
                            // Ignore decode exceptions
                        }
                    }
                }
                _roads.value = allRoads
            }
        }
    }

    private fun parseOverpassJson(jsonStr: String): List<List<RoadPoint>> {
        val result = mutableListOf<List<RoadPoint>>()
        try {
            val root = org.json.JSONObject(jsonStr)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                val geometry = element.optJSONArray("geometry") ?: continue
                val path = mutableListOf<RoadPoint>()
                for (j in 0 until geometry.length()) {
                    val pt = geometry.getJSONObject(j)
                    val ptLat = pt.getDouble("lat")
                    val ptLon = pt.getDouble("lon")
                    path.add(RoadPoint(ptLat, ptLon))
                }
                if (path.size >= 2) {
                    result.add(path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // Anomaly Scanner trigger
    private fun generateNearbyAnomalies(lat: Double, lng: Double) {
        // Base seed grid scaled from 1000 to 100 to greatly lengthen spatial lifespan of anomalies
        val baseSeed = (lat * 100).toInt() + (lng * 100).toInt()
        val random = java.util.Random(baseSeed.toLong())
        val factions = listOf("Infection", "Mech", "Parasite", "Containment")
        
        val list = mutableListOf<PoxAnomaly>()
        for (i in 0 until 5) {
            var dLat = 0.0
            var dLng = 0.0
            var distMeters = 0.0
            var attempts = 0
            
            while (attempts < 100) {
                // Semi-random angle (0 to 2*PI)
                val angle = random.nextDouble() * 2.0 * Math.PI
                // Spacing within 4km (4000 meters)
                distMeters = 200.0 + random.nextDouble() * 3800.0
                
                dLat = (distMeters * kotlin.math.cos(angle)) / 111000.0
                dLng = (distMeters * kotlin.math.sin(angle)) / (111000.0 * kotlin.math.cos(Math.toRadians(lat)))
                
                // Avoid overlap: allow closer proximity (at least 200m instead of 600m) to create overlapping regions
                val candidateLat = lat + dLat
                val candidateLng = lng + dLng
                val overlaps = list.any { existing ->
                    val dx = (existing.lng - candidateLng) * 111000.0 * kotlin.math.cos(Math.toRadians(lat))
                    val dy = (existing.lat - candidateLat) * 111000.0
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    dist < 200.0
                }
                if (!overlaps) break
                attempts++
            }
            
            val distanceInFeet = distMeters * 3.28084
            
            // Build an anomalous gene
            val gene = WaveMath.generateAnomalousGene(random)
            
            val densityRaw = (random.nextDouble() * 0.66) - 0.33
            val density = Math.round(densityRaw * 100.0) / 100.0
            
            list.add(
                PoxAnomaly(
                    id = "ANM-$i-$baseSeed",
                    name = "Anomaly #${baseSeed % 100 + i}",
                    lat = lat + dLat,
                    lng = lng + dLng,
                    gene = gene,
                    faction = factions[random.nextInt(4)],
                    distance = distanceInFeet,
                    heatZoneDiameter = 500.0 * 3.28084, // 500 meters in feet
                    density = density
                )
            )
        }
        _rawAnomalies.value = list
    }

    // Direct Gemini Client compilation
    fun compileCreature(sequence: String) {
        if (sequence.length != 64 || !sequence.matches(Regex("[AGTCagtc]+"))) {
            addLog("ERR: Telemetry mismatch. Sequence must be 64 bases of AGTC.")
            synthManager.playReject()
            return
        }

        viewModelScope.launch {
            _isCompiling.value = true
            addLog("INI: Accessing Google compilation system...")
            synthManager.playBeep(330f, 0.5f, "sawtooth")

            val apiKey = geminiApiKey.value.trim()

            val matchesTarget = getCoherence(sequence, _targetSequence.value) == "full"
            val creature = if (apiKey.isNotEmpty()) {
                try {
                    compileWithGemini(sequence, apiKey)
                } catch (e: Exception) {
                    addLog("WRN: API Error: ${e.localizedMessage}. Using on-board compiler.")
                    compileDeterministicOffline(sequence)
                }
            } else {
                addLog("SYS: No API Key found. Running offline compiler engine.")
                compileDeterministicOffline(sequence)
            }?.let {
                it.copy(
                    isFullCoherence = matchesTarget,
                    coherenceType = if (matchesTarget) "Natural" else null
                )
            }

            if (creature != null) {
                repository.insertCreature(creature)
                // Consume 8-char blocks if they exist in inventory
                // For simplicity, compilation costs 8 random genes
                addLog("OK: Compiled creature \"${creature.name}\" of Sector [${creature.faction}]")
                synthManager.playSynthesisSuccess()
                setActiveCreature(creature, "combinator")
                _selectedTab.value = "vault"
            } else {
                addLog("ERR: Compilation reactor failure.")
                synthManager.playReject()
            }
            _isCompiling.value = false
        }
    }

    private suspend fun compileWithGemini(sequence: String, apiKey: String): Creature? = withContext(Dispatchers.IO) {
        try {
            val config = generationConfig {
                responseMimeType = "application/json"
            }
            val model = GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = apiKey,
                generationConfig = config,
                systemInstruction = content {
                    text("You are an expert compiler of retro sci-fi cyber-creatures. You output structured bios for bio-mechanical warriors, parasitic battle bugs, and cyber virus creatures in valid JSON.")
                }
            )

            val prompt = """
                Design a bio-mechanical virus/cyborg insectoid monster from this 64-character DNA sequence: "$sequence". Let the content of the DNA sequence (ratio of A, G, T, C) guide its cybernetic characteristics.
                Output a valid JSON containing exactly the following string fields:
                - name: Cool retro battle name, e.g. CyberVex-9, Toxipod, Chitin-Shell
                - faction: One of: 'Infection', 'Mech', 'Parasite', 'Containment'
                - type: Bio-mechanical classification, e.g. Virus Swarm, Neural Parasite, Heavy Metal Shell
                - vitality: Combat Life from 100 to 250 (as a number)
                - attack: Assault force stat from 10 to 99 (as a number)
                - defense: Shield armor stat from 10 to 99 (as a number)
                - speed: Agility velocity stat from 10 to 99 (as a number)
                - primaryWeapon: Bio-weapon name, e.g. Acid Spurt-Needle
                - lore: A brief, flavor-text 2-sentence description detailing its bio-tech evolution.
                - asciiArt: An abstract 5x5 Grid representation using characters '.', 'o', 'x', '#', 'O', '#'
            """.trimIndent()

            val response = model.generateContent(prompt)
            val jsonText = response.text ?: throw Exception("Empty model response")
            val element = Json.parseToJsonElement(jsonText.trim()).jsonObject

            val name = element["name"]?.jsonPrimitive?.content ?: "Specimen-X"
            val faction = element["faction"]?.jsonPrimitive?.content ?: "Infection"
            val type = element["type"]?.jsonPrimitive?.content ?: "Bio-Mutant"
            val vitality = element["vitality"]?.jsonPrimitive?.intOrNull ?: 100
            val attack = element["attack"]?.jsonPrimitive?.intOrNull ?: 50
            val defense = element["defense"]?.jsonPrimitive?.intOrNull ?: 50
            val speed = element["speed"]?.jsonPrimitive?.intOrNull ?: 50
            val weapon = element["primaryWeapon"]?.jsonPrimitive?.content ?: "Claws"
            val lore = element["lore"]?.jsonPrimitive?.content ?: "Telemetry compilation."
            val ascii = element["asciiArt"]?.jsonPrimitive?.content ?: "o.x.o\n.###.\nx.o.x"

            Creature(
                id = UUID.randomUUID().toString(),
                sequence = sequence,
                name = name,
                faction = faction,
                type = type,
                vitality = vitality,
                attack = attack,
                defense = defense,
                speed = speed,
                primaryWeapon = weapon,
                lore = lore,
                asciiArt = ascii,
                discoveredAt = System.currentTimeMillis(),
                origin = "Compiled via AI Engine"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compileDeterministicOffline(sequence: String): Creature {
        val upperSeq = sequence.uppercase()
        var aCount = 0
        var gCount = 0
        var tCount = 0
        var cCount = 0
        for (char in upperSeq) {
            when (char) {
                'A' -> aCount++
                'G' -> gCount++
                'T' -> tCount++
                'C' -> cCount++
            }
        }

        val faction = when (maxOf(aCount, gCount, tCount, cCount)) {
            aCount -> "Infection"
            gCount -> "Mech"
            tCount -> "Parasite"
            else -> "Containment"
        }

        val type = when (faction) {
            "Infection" -> "Acidic Virus Swarm"
            "Mech" -> "Heavy Metal Shell"
            "Parasite" -> "Neural Parasite"
            else -> "Bio-Containment Unit"
        }

        val baseSeed = upperSeq.hashCode()
        val random = java.util.Random(baseSeed.toLong())

        val vitality = 100 + aCount * 5
        val attack = 20 + gCount * 2
        val defense = 20 + cCount * 2
        val speed = 20 + tCount * 2

        val name = when (faction) {
            "Infection" -> "Toxipod-${random.nextInt(99)}"
            "Mech" -> "Chitin-Shell-${random.nextInt(99)}"
            "Parasite" -> "CyberVex-${random.nextInt(99)}"
            else -> "SectorGuard-${random.nextInt(99)}"
        }

        val weapon = when (faction) {
            "Infection" -> "Acid Spurt-Needle"
            "Mech" -> "Titanium Piston Jaw"
            "Parasite" -> "Synapse Siphon"
            else -> "Plasma Shield Mesh"
        }

        val lore = "Deterministic offline specimen. Gene sequence frequencies reveal highly stable $faction bindings, evolving standard operational designation as a $type."

        // Generate abstract 5x5 ASCII art grid
        val grid = StringBuilder()
        for (r in 0 until 5) {
            for (col in 0 until 5) {
                val valRand = random.nextInt(4)
                val char = when (valRand) {
                    0 -> "."
                    1 -> "o"
                    2 -> "x"
                    else -> "#"
                }
                grid.append(char)
            }
            if (r < 4) grid.append("\n")
        }

        return Creature(
            id = UUID.randomUUID().toString(),
            sequence = sequence,
            name = name,
            faction = faction,
            type = type,
            vitality = vitality,
            attack = attack,
            defense = defense,
            speed = speed,
            primaryWeapon = weapon,
            lore = lore,
            asciiArt = grid.toString(),
            discoveredAt = System.currentTimeMillis(),
            origin = "Local Compiled Unit"
        )
    }

    // Appending genes in Splicer
    fun weldGeneToCreature() {
        val creature = _selectedSplicerCreature.value ?: return
        val gene = _selectedGeneToAppend.value ?: return

        viewModelScope.launch {
            // Check if gene is in inventory and has count > 0
            val currentGenes = geneSequences.value
            val match = currentGenes.find { it.sequence == gene && it.count > 0 }
            if (match == null) {
                addLog("ERR: Gene $gene not in inventory.")
                synthManager.playReject()
                return@launch
            }

            // Deduct gene count from inventory
            val updatedGene = match.copy(count = match.count - 1)
            if (updatedGene.count == 0) {
                repository.deleteGeneSequence(match)
            } else {
                repository.insertGeneSequence(updatedGene)
            }

            // Weld gene
            val newAppended = creature.appendedGenes + gene
            // welding restores 10% telomeres, capping at 100
            val restoredTelomeres = creature.telomeres + 10
            val updatedCreature = creature.copy(
                appendedGenes = newAppended,
                telomeres = restoredTelomeres.coerceAtMost(100)
            )
            repository.insertCreature(updatedCreature)

            _selectedSplicerCreature.value = updatedCreature
            _selectedGeneToAppend.value = null
            addLog("WELD: Gene $gene bonded. Telomeres reinforced to ${updatedCreature.telomeres}%.")
            synthManager.playSynthesisSuccess()
        }
    }

    fun getSynodicResonanceMod(faction: String, phaseName: String): Int {
        return when (faction) {
            "Infection" -> {
                if (phaseName == "Waxing Crescent" || phaseName == "Waxing Gibbous") 15
                else if (phaseName == "Waning Crescent" || phaseName == "Waning Gibbous") -10
                else 0
            }
            "Mech" -> {
                if (phaseName == "First Quarter" || phaseName == "Third Quarter") 15
                else if (phaseName == "New Moon" || phaseName == "Full Moon") -10
                else 0
            }
            "Parasite" -> {
                if (phaseName == "Full Moon") 15
                else if (phaseName == "New Moon") -10
                else 0
            }
            "Containment" -> {
                if (phaseName == "New Moon") 15
                else if (phaseName == "Full Moon") -10
                else 0
            }
            else -> 0
        }
    }

    private fun hasCoherenceShield(creature: Creature): Boolean {
        if (WaveMath.getAnomalousBenefits(creature.sequence).any { it.id == "COHERENCE_SHIELD" }) {
            return true
        }
        creature.appendedGenes.forEach { gene ->
            if (WaveMath.isAnomalousGene(gene) && WaveMath.getBenefitForAnomalousGene(gene).id == "COHERENCE_SHIELD") {
                return true
            }
        }
        return false
    }

    // Harvest dispatch from map
    fun dispatchMission(creature: Creature, anomaly: PoxAnomaly) {
        viewModelScope.launch {
            val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
            val phaseFraction = wave.lunarAge / WaveMath.LUNAR_MONTH_DAYS
            val lunarPhaseScale = (1.0 - kotlin.math.cos(phaseFraction * 2.0 * Math.PI)) / 2.0
            val lunarResistanceMod = 0.7 + 0.6 * lunarPhaseScale
            val lunarMutationMod = 0.5 + 1.0 * lunarPhaseScale

            val boundaryRadius = anomaly.getBoundaryRadiusForPlayer(_latitude.value, _longitude.value)
            // Normalized R_base by dividing by 60.0 to align GPS physical feet scale with [10-99] creature stats
            val R_base = boundaryRadius * 0.1
            val R_anom = R_base * lunarResistanceMod

            val resonanceMod = getSynodicResonanceMod(creature.faction, wave.phaseName)
            val effectiveDefense = creature.defense + resonanceMod

            val stalledDepth = (effectiveDefense.toDouble() / R_anom * 100.0).coerceIn(0.0, 100.0)
            val dispatchDistance = boundaryRadius * (1.0 - stalledDepth / 100.0)
            
            // Scaled mutation interval on mobile by dividing by 16
            val mutationInterval = Math.round((480.0 * Math.pow(2.0, -stalledDepth / 25.0)) / lunarMutationMod / 16.0).coerceAtLeast(1L)

            // Speed-based calculations (scaled up by a speedFactor of 1350.0 so speed=1 takes ~16 min at max distance)
            val travelTimeComponent = 32.0
            val travelDistanceComponent = 16.0
            val speedFactor = 1350.0
            val V_travel = (((creature.speed.toDouble() / 50.0) * travelDistanceComponent) / travelTimeComponent) * speedFactor
            
            val travelDistance = maxOf(0.0, anomaly.distance - boundaryRadius)
            val travelDuration = if (V_travel > 0.0) {
                Math.round(travelDistance / V_travel).coerceAtLeast(1L)
            } else {
                32L
            }

            // Calculate precise touchdown coordinate along the vector from player to epicenter
            val t = if (anomaly.distance > 0.0) (dispatchDistance / anomaly.distance).coerceIn(0.0, 1.0) else 0.0
            val landingLat = anomaly.lat + t * (_latitude.value - anomaly.lat)
            val landingLng = anomaly.lng + t * (_longitude.value - anomaly.lng)

            // Compute wave interference density from all overlapping anomalies
            var combinedDensity = 0.0
            _rawAnomalies.value.forEach { anom ->
                val distFromAnom = calculateDistanceInFeet(landingLat, landingLng, anom.lat, anom.lng)
                val boundRad = anom.getBoundaryRadiusForPlayer(landingLat, landingLng)
                if (distFromAnom <= boundRad) {
                    val seed = anom.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
                    val phi = (seed % 360) * (Math.PI / 180.0)
                    val omega = 0.02
                    val alpha = 0.002
                    val waveTerm = kotlin.math.cos(omega * distFromAnom + phi) * kotlin.math.exp(-alpha * distFromAnom)
                    combinedDensity += anom.density * waveTerm
                }
            }

            val descentDistance = boundaryRadius * (stalledDepth / 100.0)
            val densityShift = 0.2 * (lunarPhaseScale - 0.5)
            val effectiveDensity = (combinedDensity + densityShift).coerceIn(-0.33, 0.33)
            
            val hasCoherenceShield = hasCoherenceShield(creature)
            val finalDensity = if (hasCoherenceShield && effectiveDensity > 0.0) 0.0 else effectiveDensity
            val V_descent = V_travel * (1.0 - finalDensity)
            
            val descentDuration = if (V_descent > 0.0) {
                Math.round(descentDistance / V_descent).coerceAtLeast(1L)
            } else {
                32L
            }

            val harvestDuration = 60L // Fixed harvesting duration
            val ascentDuration = descentDuration
            val transitBackDuration = travelDuration
            val calculatedDuration = travelDuration + descentDuration + harvestDuration + ascentDuration + transitBackDuration

            val densityVal = Math.round(combinedDensity * 100.0).toInt()
            val effDensityVal = Math.round(effectiveDensity * 100.0).toInt()
            val missionLogsList = listOf(
                "[LAUNCH] Specimen \"${creature.name}\" dispatched to Anomaly ${anomaly.id}.",
                "[LANDING_COORD] $landingLat,$landingLng",
                "[TELEMETRY] Synodic Resonance: ${if (resonanceMod >= 0) "+" else ""}$resonanceMod DEF (${wave.phaseName}).",
                "[TELEMETRY] Effective Defense: $effectiveDefense | Anomaly Resistance: ${String.format(java.util.Locale.US, "%.1f", R_anom)} (Base: ${R_base.toInt()}, Lunar: ${String.format(java.util.Locale.US, "%.2f", lunarResistanceMod)}x).",
                "[TELEMETRY] Stalled Depth resolved: ${stalledDepth.toInt()}% (${String.format(java.util.Locale.US, "%.1f", dispatchDistance)}ft from epicenter).",
                "[TELEMETRY] Combined Wave Density -> Base: ${if (densityVal >= 0) "+" else ""}$densityVal% | Active Shifted: ${if (effDensityVal >= 0) "+" else ""}$effDensityVal%.",
                "[TELEMETRY] Coherence Shield status: ${if (hasCoherenceShield) "ACTIVE (Drag Imm.)" else "INACTIVE"}.",
                "[TELEMETRY] Durations -> Travel: ${travelDuration}s | Descent: ${descentDuration}s | Harvest: ${harvestDuration}s | Ascent: ${ascentDuration}s | Return: ${transitBackDuration}s.",
                "[TELEMETRY] Mutation Well active. Expected base decay: every ${mutationInterval}s."
            )

            val mission = HarvestMission(
                id = "MSN-" + UUID.randomUUID().toString().take(6),
                creatureId = creature.id,
                creatureName = creature.name,
                creatureFaction = creature.faction,
                lat = anomaly.lat,
                lng = anomaly.lng,
                startTime = System.currentTimeMillis(),
                totalDuration = calculatedDuration,
                harvestedGenes = listOf(anomaly.gene),
                isCompleted = false,
                isReturned = false,
                dispatchDistance = dispatchDistance,
                stalledDepth = stalledDepth,
                originalSequence = creature.sequence,
                elapsedSeconds = 0L,
                missionLogs = missionLogsList,
                phase = "TRAVEL",
                travelDuration = travelDuration,
                descentDuration = descentDuration,
                harvestDuration = harvestDuration,
                ascentDuration = ascentDuration,
                transitBackDuration = transitBackDuration,
                currentPhaseElapsed = 0L
            )
            repository.insertMission(mission)
            addLog("DSP: Dispatched \"${creature.name}\" to harvest spot [${anomaly.gene}]")
            synthManager.playBeep(520f, 0.3f, "triangle")
        }
    }

    fun recallMission(mission: HarvestMission) {
        viewModelScope.launch {
            val landingLog = mission.missionLogs.find { it.startsWith("[LANDING_COORD] ") }
            val (landingLat, landingLng) = if (landingLog != null) {
                val coords = landingLog.substringAfter("[LANDING_COORD] ").split(",")
                coords[0].toDouble() to coords[1].toDouble()
            } else {
                mission.lat to mission.lng
            }

            // Find overlapping adjacent anomaly if any
            val otherAnom = _rawAnomalies.value.find { anom ->
                (Math.abs(anom.lat - mission.lat) > 0.0001 || Math.abs(anom.lng - mission.lng) > 0.0001) &&
                calculateDistanceInFeet(landingLat, landingLng, anom.lat, anom.lng) <= anom.getBoundaryRadiusForPlayer(landingLat, landingLng)
            }

            val primaryGene = mission.harvestedGenes.firstOrNull() ?: "AGTCGTAC"
            var updatedPrimaryGene = primaryGene
            var spilloverSuccess = false
            var hybridGene: String? = null
            val finalLogs = mission.missionLogs.toMutableList()

            if (otherAnom != null) {
                val dA = calculateDistanceInFeet(landingLat, landingLng, mission.lat, mission.lng)
                val dB = calculateDistanceInFeet(landingLat, landingLng, otherAnom.lat, otherAnom.lng)
                
                val baseSpilloverChance = 0.125
                val factor = 1.0 - (dB / (dA + dB)).coerceIn(0.0, 1.0)
                val spilloverChance = baseSpilloverChance * factor
                
                val roll = Math.random()
                finalLogs.add("[TELEMETRY] Adjacent anomaly boundary overlap: ${otherAnom.id} (crosstalk active).")
                if (roll <= spilloverChance) {
                    spilloverSuccess = true
                    val targetGene = primaryGene
                    val overlapGene = otherAnom.gene
                    if (targetGene.length == 8 && overlapGene.length == 8) {
                        hybridGene = targetGene.substring(0, 4) + overlapGene.substring(4, 8)
                        updatedPrimaryGene = hybridGene
                        finalLogs.add("[TELEMETRY] Hybridization success: $hybridGene (Target $targetGene Weld Overlap $overlapGene).")
                    } else {
                        updatedPrimaryGene = overlapGene
                        finalLogs.add("[TELEMETRY] Crosstalk spillover: Harvested adjacent gene $overlapGene.")
                    }
                } else {
                    finalLogs.add("[TELEMETRY] Crosstalk failed. Target signature remains dominant.")
                }
            }

            val bonusGene = WaveMath.generateAnomalousGene()
            val rewardedGenes = listOf(updatedPrimaryGene) + bonusGene
            val updated = mission.copy(isReturned = true, harvestedGenes = rewardedGenes, missionLogs = finalLogs)
            repository.insertMission(updated)

            if (_trackedMissionId.value == mission.id) {
                _trackedMissionId.value = null
            }

            // Add harvested genes to inventory using a batch insert
            val toInsertOrUpdate = mutableListOf<GeneSequence>()
            val currentList = geneSequences.value
            val groupedRewards = rewardedGenes.groupingBy { it }.eachCount()
            
            groupedRewards.forEach { (gene, count) ->
                val existing = currentList.find { it.sequence == gene }
                if (existing != null) {
                    toInsertOrUpdate.add(existing.copy(count = existing.count + count))
                } else {
                    toInsertOrUpdate.add(GeneSequence(gene, count, System.currentTimeMillis()))
                }
            }
            repository.insertGeneSequences(toInsertOrUpdate)

            addLog("RCL: Mission returned. Genes cached: ${rewardedGenes.joinToString()}")
            synthManager.playSynthesisSuccess()
        }
    }

    fun recallAllActiveMissions() {
        viewModelScope.launch {
            try {
                val active = repository.activeMissions.first()
                val ongoing = active.filter { !it.isReturned }
                if (ongoing.isEmpty()) {
                    addLog("DEV: No active harvesters found to recall.")
                    return@launch
                }
                ongoing.forEach { m ->
                    val updated = m.copy(
                        isCompleted = true,
                        phase = "COMPLETED",
                        currentPhaseElapsed = 0L
                    )
                    recallMission(updated)
                }
                addLog("DEV: Recalled ${ongoing.size} active harvesters.")
            } catch (e: Exception) {
                addLog("DEV ERROR: Failed to recall harvesters: ${e.message}")
            }
        }
    }

    fun toggleScanner(show: Boolean) {
        _showScanner.value = show
        synthManager.playCombinatorTick()
    }

    fun processScannedBarcode(data: String) {
        viewModelScope.launch {
            _showScanner.value = false
            addLog("SCAN: Decoded transponder sequence.")
            synthManager.playSynthesisSuccess()

            val creature = decodeCreatureFromBase64(data)
            if (creature != null) {
                val transferredCreature = creature.copy(telomeres = 99)
                repository.insertCreature(transferredCreature)
                addLog("SCAN: Compiled creature \"${creature.name}\" [Telomeres: 99%]")
                setActiveCreature(transferredCreature, "scanner")
                _selectedTab.value = "vault"
            } else if (data.length == 64 && data.matches(Regex("[AGTCagtc]+"))) {
                addLog("SCAN: Scanned raw DNA sequence. Compiling...")
                compileCreature(data)
            } else if (data.length == 8 && data.all { it.uppercaseChar() in "AGTCXZYW?!$%&@#" }) {
                val upper = data.uppercase()
                val existing = geneSequences.value.find { it.sequence == upper }
                if (existing != null) {
                    repository.insertGeneSequence(existing.copy(count = existing.count + 1))
                } else {
                    repository.insertGeneSequence(GeneSequence(upper, 1, System.currentTimeMillis()))
                }
                addLog("SCAN: Registered gene block $upper in inventory.")
            } else {
                addLog("SCAN ERR: Data format invalid or unaligned.")
                synthManager.playReject()
            }
        }
    }

    private fun decodeCreatureFromBase64(base64String: String): Creature? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64String.trim(), android.util.Base64.DEFAULT)
            val jsonStr = String(decodedBytes, Charsets.UTF_8)
            val element = Json.parseToJsonElement(jsonStr).jsonObject
            val id = element["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
            val sequence = element["sequence"]?.jsonPrimitive?.content ?: return null
            val name = element["name"]?.jsonPrimitive?.content ?: return null
            val faction = element["faction"]?.jsonPrimitive?.content ?: "Infection"
            val type = element["type"]?.jsonPrimitive?.content ?: "Unknown Mutant"
            val vitality = element["vitality"]?.jsonPrimitive?.intOrNull ?: 100
            val attack = element["attack"]?.jsonPrimitive?.intOrNull ?: 50
            val defense = element["defense"]?.jsonPrimitive?.intOrNull ?: 50
            val speed = element["spd"]?.jsonPrimitive?.intOrNull ?: element["speed"]?.jsonPrimitive?.intOrNull ?: 50
            val weapon = element["weapon"]?.jsonPrimitive?.content ?: element["primaryWeapon"]?.jsonPrimitive?.content ?: "None"
            val lore = element["lore"]?.jsonPrimitive?.content ?: "Imported specimen."
            val ascii = element["ascii"]?.jsonPrimitive?.content ?: element["asciiArt"]?.jsonPrimitive?.content ?: "o.x.o\n.###.\nx.o.x"
            val appended = element["appended"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val telomeres = element["telomeres"]?.jsonPrimitive?.intOrNull ?: 100
            val isFullCoherence = element["isFullCoherence"]?.jsonPrimitive?.booleanOrNull ?: false
            val coherenceType = element["coherenceType"]?.jsonPrimitive?.content

            Creature(
                id = id,
                sequence = sequence,
                name = name,
                faction = faction,
                type = type,
                vitality = vitality,
                attack = attack,
                defense = defense,
                speed = speed,
                primaryWeapon = weapon,
                lore = lore,
                asciiArt = ascii,
                discoveredAt = System.currentTimeMillis(),
                origin = "Scanned transponder",
                appendedGenes = appended,
                telomeres = telomeres,
                isFullCoherence = isFullCoherence,
                coherenceType = coherenceType
            )
        } catch (e: Exception) {
            null
        }
    }

    // Deterministic 128 daily bounty sequences
    private fun generateDailyBounties(): List<String> {
        val list = mutableListOf<String>()
        val bases = listOf("A", "G", "T", "C")
        val random = java.util.Random(1337) // Stable seed
        for (i in 0 until 128) {
            var seq = ""
            for (j in 0 until 64) {
                seq += bases[random.nextInt(4)]
            }
            list.add(seq)
        }
        return list
    }

    // Bio-Lab Reactor and Anomaly controls
    fun setBioLabSubTab(subTab: String) {
        _bioLabSubTab.value = subTab
        synthManager.playBeep(if (subTab == "pox") 440f else 587f, 0.05f, "sine")
    }

    fun setPoxReactorActive(active: Boolean) {
        if (active) {
            _poxReactorActive.value = true
            _anomalyEngineActive.value = false
            addLog("P.O.X. REACTOR ACTIVE: Standard gene synthesis engaged.")
            synthManager.playBeep(440f, 0.15f, "sine")
        } else {
            _poxReactorActive.value = false
            addLog("P.O.X. Reactor disengaged. Reactor power offline.")
            synthManager.playBeep(350f, 0.15f, "sine")
        }
    }

    fun setAnomalyEngineActive(active: Boolean) {
        if (active) {
            if (grandTotalStandardNucleotides.value < 250000L) {
                addLog("CANNOT ACTIVATE ANOMALY ENGINE: Requires a massive stockpile of minimum 250k total nucleotides.")
                synthManager.playReject()
                return
            }
            _anomalyEngineActive.value = true
            _poxReactorActive.value = false
            addLog("ANOMALY ENGINE ENGAGED! Cosmic gene hunting activated!")
            synthManager.playBeep(120f, 0.6f, "sawtooth")
        } else {
            _anomalyEngineActive.value = false
            addLog("Anomaly Engine disengaged. Reactor power normalized. Standard Bio-Lab Reactor active.")
            synthManager.playBeep(350f, 0.15f, "sine")
        }
    }

    fun triggerManualAcceleration() {
        if (!_poxReactorActive.value) {
            synthManager.playReject()
            return
        }
        val current = _poxIdleTime.value
        if (current > 2) {
            _poxIdleTime.value = current - 2
        } else {
            _poxIdleTime.value = 1
        }
        synthManager.playBeep(880f, 0.05f, "sine")
    }

    fun addBoosterTime(seconds: Int) {
        _boostSecondsLeft.value = (_boostSecondsLeft.value + seconds).coerceAtLeast(0)
        addLog("REACTOR BOOST ENABLED: Cycle acceleration active for $seconds seconds.")
        synthManager.playBeep(520f, 0.2f, "sine")
    }

    fun selectSplicerSlot(idx: Int?) {
        _activeSlotSelection.value = idx
        _slotSequenceFilter.value = ""
        if (idx != null) {
            synthManager.playBeep(440f, 0.05f, "sine")
        }
    }

    fun setSlotSequenceFilter(query: String) {
        _slotSequenceFilter.value = query.uppercase()
    }

    fun assignGeneToSlot(seq: String) {
        val slotIdx = _activeSlotSelection.value ?: return
        viewModelScope.launch {
            val target = _targetSequence.value
            val expected = target.substring(slotIdx * 8, (slotIdx + 1) * 8)
            if (seq != expected) {
                addLog("GENOME MISMATCH: Gene does not match required slot sequence $expected")
                synthManager.playReject()
                return@launch
            }

            val currentGenes = geneSequences.value
            val match = currentGenes.find { it.sequence == seq && it.count > 0 }
            if (match == null) {
                addLog("ERR: Gene $seq not in inventory.")
                synthManager.playReject()
                return@launch
            }

            // Deduct gene count from inventory
            val updatedGene = match.copy(count = match.count - 1)
            if (updatedGene.count <= 0) {
                repository.deleteGeneSequence(match)
            } else {
                repository.insertGeneSequence(updatedGene)
            }

            // Return existing gene to inventory if any
            val existing = _splicerSlots.value[slotIdx]
            if (existing != null) {
                val existingMatch = geneSequences.value.find { it.sequence == existing }
                if (existingMatch != null) {
                    repository.insertGeneSequence(existingMatch.copy(count = existingMatch.count + 1))
                } else {
                    repository.insertGeneSequence(GeneSequence(existing, 1, System.currentTimeMillis()))
                }
            }

            // Update slot
            val updatedSlots = _splicerSlots.value.toMutableList()
            updatedSlots[slotIdx] = seq
            _splicerSlots.value = updatedSlots
            _activeSlotSelection.value = null
            _slotSequenceFilter.value = ""

            synthManager.playBeep(440f, 0.1f, "triangle")
            addLog("Assigned matching gene block to slot #${slotIdx + 1}")

        }
    }

    fun ejectGeneFromSlot(slotIdx: Int) {
        val existing = _splicerSlots.value.getOrNull(slotIdx) ?: return
        viewModelScope.launch {
            synthManager.playBeep(330f, 0.1f, "sawtooth")

            // Return to inventory
            val existingMatch = geneSequences.value.find { it.sequence == existing }
            if (existingMatch != null) {
                repository.insertGeneSequence(existingMatch.copy(count = existingMatch.count + 1))
            } else {
                repository.insertGeneSequence(GeneSequence(existing, 1, System.currentTimeMillis()))
            }

            val updatedSlots = _splicerSlots.value.toMutableList()
            updatedSlots[slotIdx] = null
            _splicerSlots.value = updatedSlots
            addLog("Returned gene segment #${slotIdx + 1} to archive stock")
        }
    }

    fun autofillSplicerSlots() {
        synthManager.playBeep(880f, 0.05f, "sine")
        viewModelScope.launch(Dispatchers.Default) {
            val target = _targetSequence.value
            if (target.length != 64) return@launch

            val updatedSlots = _splicerSlots.value.toMutableList()
            val currentStockMap = geneSequences.value.associateBy { it.sequence }.toMutableMap()
            val modifiedSequences = mutableSetOf<String>()

            var didChanges = false
            var autoFilledCount = 0

            for (i in 0 until 8) {
                if (updatedSlots[i] == null) {
                    val requiredGene = target.substring(i * 8, (i + 1) * 8)
                    val matchedGene = currentStockMap[requiredGene]
                    if (matchedGene != null && matchedGene.count > 0) {
                        val updatedGene = matchedGene.copy(count = matchedGene.count - 1)
                        currentStockMap[requiredGene] = updatedGene
                        updatedSlots[i] = requiredGene
                        modifiedSequences.add(requiredGene)
                        didChanges = true
                        autoFilledCount++
                    }
                }
            }

            if (didChanges) {
                val toUpdate = mutableListOf<GeneSequence>()
                val toDelete = mutableListOf<GeneSequence>()

                for (seq in modifiedSequences) {
                    val originalGene = geneSequences.value.find { it.sequence == seq }
                    val updatedGene = currentStockMap[seq]!!
                    if (originalGene != null) {
                        if (updatedGene.count <= 0) {
                            toDelete.add(originalGene)
                        } else {
                            toUpdate.add(updatedGene)
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    repository.updateGeneStock(toUpdate, toDelete)
                }

                _splicerSlots.value = updatedSlots
                addLog("AUTO GENE: Filled $autoFilledCount matching slots with verified stock segments.")
            } else {
                addLog("AUTO GENE: No matching segments found in stock for any unfilled slots.")
            }
        }
    }

    fun devInjectMissingTargetGenes() {
        if (!_devForceAnomaly.value) return
        val target = _targetSequence.value
        if (target.length != 64) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val basesList = (0 until 8).map { target.substring(it * 8, (it + 1) * 8) }
            val existingList = geneSequences.value.toMutableList()
            var addedAny = false
            
            basesList.forEach { seq ->
                val matchIdx = existingList.indexOfFirst { it.sequence == seq }
                if (matchIdx >= 0) {
                    val match = existingList[matchIdx]
                    if (match.count <= 0) {
                        existingList[matchIdx] = match.copy(count = 1)
                        addedAny = true
                    }
                } else {
                    existingList.add(GeneSequence(seq, 1, System.currentTimeMillis()))
                    addedAny = true
                }
            }
            
            if (addedAny) {
                repository.insertGeneSequences(existingList)
                addLog("DEV: Injected missing target genes into stock archive.")
                synthManager.playSynthesisSuccess()
            } else {
                addLog("DEV: All required target genes already exist in stock.")
                synthManager.playBeep(440f, 0.1f, "sine")
            }
        }
    }

    fun constructSplicedCreature() {
        val slots = _splicerSlots.value
        if (slots.contains(null)) return
        val fullDNASeq = slots.filterNotNull().joinToString("")
        if (fullDNASeq.length != 64) return

        viewModelScope.launch {
            _isSplicing.value = true
            _splicingProgress.value = 0
            synthManager.playBeep(300f, 0.3f, "sawtooth")
            addLog("INITIATING MORPHOGENESIS THERMALS...")

            for (prg in 10..100 step 10) {
                kotlinx.coroutines.delay(200L)
                _splicingProgress.value = prg
                synthManager.playBeep(250f + prg * 5, 0.04f, "sine")
            }

            // Wait 1.5s at 100% progress to appreciate the completed matrix/helix
            kotlinx.coroutines.delay(1500L)

            val spawned = compileDeterministicOffline(fullDNASeq)
            val matchesTarget = getCoherence(fullDNASeq, _targetSequence.value) == "full"
            val idSuffix = UUID.randomUUID().toString().take(6).uppercase()
            val finalCreature = spawned.copy(
                id = "PX-$idSuffix",
                name = spawned.name,
                lore = spawned.lore,
                origin = "Spliced Gene",
                isFullCoherence = matchesTarget,
                coherenceType = if (matchesTarget) "Natural" else null
            )

            repository.insertCreature(finalCreature)
            addLog("SUCCESSFUL ASSEMBLY OF ${finalCreature.name}!")
            synthManager.playSynthesisSuccess()

            _splicerSlots.value = List<String?>(8) { null }
            _isSplicing.value = false
            _splicingProgress.value = 0

            setActiveCreature(finalCreature, "splicer")
            _selectedTab.value = "vault"
        }
    }

    fun setIsForcedLoopActive(active: Boolean) {
        _isForcedLoopActive.value = active
    }

    fun startForcedConstruction(customSequence: String? = null) {
        if (_isSplicing.value || _isForcedConstructionActive.value) return

        val targetSeq = if (customSequence != null && customSequence.length == 64) {
            customSequence
        } else {
            _targetSequence.value
        }

        val isCustom = customSequence != null

        synthManager.playBeep(220f, 0.4f, "sawtooth")
        _isForcedConstructionActive.value = true
        _isReactorFrozen.value = true

        runForcedConstructionCycle(targetSeq, isCustom)
    }

    private var forcedLoopJob: kotlinx.coroutines.Job? = null

    private fun runForcedConstructionCycle(dnaSeq: String, isCustom: Boolean) {
        forcedLoopJob?.cancel()
        forcedLoopJob = viewModelScope.launch(Dispatchers.Default) {
            val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
            _reactorFreezeTimeLeft.value = 8.0f

            var failureChance = 37.5
            var moonStatusLog = "None / Baseline"
            if (wave.isNewMoon) {
                failureChance += 3.75
                moonStatusLog = "New Moon (+3.75% Debuff on construction)"
            } else if (wave.isFullMoon) {
                failureChance -= 3.75
                moonStatusLog = "Full Moon (-3.75% Buff on construction)"
            }

            val isLooping = _isForcedLoopActive.value
            val initialLogs = mutableListOf(
                "[INIT] >> FORCING TARGET SEQUENCE COMPILATION...",
                if (isLooping) {
                    "[STATUS: FREEZE ACTIVE] >> TARGET SEQUENCE UNDERGOING FORCED SEQUENCING."
                } else {
                    "[WARNING] >> G.E.N. NETWORK TAGS ALL FORCED SEQUENCES!"
                },
                if (isLooping) {
                    "[REASON] >> SYSTEMS RUNNING UNDER ABNORMAL STRESS."
                } else {
                    "[WARNING] >> P.O.X. REACTOR LOCKED FOR SEQUENCING."
                },
                "[LUNAR STATUS] >> Phase: ${wave.phaseName} | Effective Debuff Mod: $moonStatusLog"
            ).filterNotNull().toMutableList()

            val currentStock = geneSequences.value.map { it.copy() }.toMutableList()

            initialLogs.add(
                "[CALCULATION] >> Base: 64 | Stock: ${currentStock.sumOf { it.count }} | Fail Chance: ${String.format("%.2f", failureChance)}%"
            )

            _forcedConstructionLogs.value = initialLogs.toList()

            val deductFromStock = { seq: String ->
                val idx = currentStock.indexOfFirst { it.sequence == seq && it.count > 0 }
                if (idx >= 0) {
                    val gene = currentStock[idx]
                    if (gene.count > 1) {
                        currentStock[idx] = gene.copy(count = gene.count - 1)
                    } else {
                        currentStock.removeAt(idx)
                    }
                    true
                } else {
                    false
                }
            }

            val findAndSacrificeGene = { expectedChar: Char, j: Int ->
                val idx = currentStock.indexOfFirst { it.sequence.getOrNull(j) == expectedChar && it.count > 0 }
                if (idx >= 0) {
                    val matchingSeq = currentStock[idx].sequence
                    deductFromStock(matchingSeq)
                    matchingSeq
                } else {
                    null
                }
            }

            data class StepLog(val second: Int, val text: String)
            val stepLogs = mutableListOf<StepLog>()

            val activeSplicerSlots = _splicerSlots.value
            var failedAtSecond = -1

            for (i in 0 until 8) {
                val expectedGene = dnaSeq.substring(i * 8, (i + 1) * 8)
                val isManualMatch = !isCustom && activeSplicerSlots.getOrNull(i) == expectedGene

                // If inventory is empty at the start of slot processing and not manually aligned, abort!
                if (currentStock.sumOf { it.count } == 0 && !isManualMatch && failedAtSecond == -1) {
                    failedAtSecond = i + 1
                    stepLogs.add(StepLog(
                        second = i + 1,
                        text = "Slot #${i + 1} processing using scaffold: VOID SYNTHESIS SCAFFOLD"
                    ))
                    stepLogs.add(StepLog(
                        second = i + 1,
                        text = "  ➔ [FATAL] >> SPLICING PROTOCOL ABORTED: Nucleotide stockpile fully depleted."
                    ))
                    break
                }

                var scaffoldStr = ""
                var scaffoldType = ""

                if (isManualMatch) {
                    scaffoldStr = expectedGene
                    scaffoldType = "PRE-ALIGNED MANUAL GENE"
                } else {
                    val hasMatch = currentStock.any { it.sequence == expectedGene && it.count > 0 }
                    if (hasMatch) {
                        deductFromStock(expectedGene)
                        scaffoldStr = expectedGene
                        scaffoldType = "MATCH STOCK RECRUITED"
                    } else {
                        val anyGene = currentStock.find { it.count > 0 }
                        if (anyGene != null) {
                            deductFromStock(anyGene.sequence)
                            scaffoldStr = anyGene.sequence
                            scaffoldType = "ANY GENE UNSTABLE BASE [${anyGene.sequence.take(4)}...]"
                        } else {
                            scaffoldStr = "--------"
                            scaffoldType = "VOID SYNTHESIS SCAFFOLD"
                        }
                    }
                }

                stepLogs.add(StepLog(
                    second = minOf(7, i + 1),
                    text = "Slot #${i + 1} processing using scaffold: $scaffoldType"
                ))

                for (j in 0 until 8) {
                    val expectedChar = expectedGene.getOrNull(j) ?: 'A'
                    val scaffoldChar = scaffoldStr.getOrNull(j) ?: '-'

                    val roll = java.util.Random().nextDouble() * 100.0
                    if (scaffoldChar == expectedChar && roll >= failureChance) {
                        // Success
                    } else {
                        val sacrificedSeq = findAndSacrificeGene(expectedChar, j)
                        if (sacrificedSeq != null) {
                            stepLogs.add(StepLog(
                                second = minOf(7, i + 1),
                                text = "  ➔ FAILED APPEND (pos ${j + 1}). Sacrificed gene $sacrificedSeq (depleting pool)"
                            ))
                        } else {
                            val backupSacrifice = currentStock.find { it.count > 0 }
                            if (backupSacrifice != null) {
                                deductFromStock(backupSacrifice.sequence)
                                stepLogs.add(StepLog(
                                    second = minOf(7, i + 1),
                                    text = "  ➔ FAILED APPEND (pos ${j + 1}). Sacrificed backup gene ${backupSacrifice.sequence} to guarantee placement"
                                ))
                            } else {
                                // Mismatch append failed and no stock remains to sacrifice
                                if (failedAtSecond == -1) {
                                    failedAtSecond = i + 1
                                    stepLogs.add(StepLog(
                                        second = i + 1,
                                        text = "  ➔ [FATAL] >> SPLICING PROTOCOL ABORTED: Nucleotide stockpile fully depleted."
                                    ))
                                }
                            }
                        }
                        failureChance = maxOf(0.0, failureChance - 3.25)
                    }
                }

                if (failedAtSecond != -1) break

                // If inventory becomes empty after slot processing, fail!
                if (currentStock.sumOf { it.count } == 0 && failedAtSecond == -1) {
                    failedAtSecond = i + 1
                    stepLogs.add(StepLog(
                        second = i + 1,
                        text = "  ➔ [FATAL] >> SPLICING PROTOCOL ABORTED: Nucleotide stockpile fully depleted."
                    ))
                    break
                }
            }

            val maxTenths = if (failedAtSecond != -1) failedAtSecond * 10 else 80

            for (tenths in 1..maxTenths) {
                kotlinx.coroutines.delay(100L)
                _reactorFreezeTimeLeft.value = ((maxTenths - tenths) / 10.0f)

                val lastWholeSec = (tenths - 1) / 10
                val currWholeSec = tenths / 10

                if (currWholeSec > lastWholeSec && currWholeSec <= 8) {
                    val currentLogs = _forcedConstructionLogs.value.toMutableList()
                    currentLogs.add("[SEQUENCE PROGRESS] >> ${((currWholeSec / 8.0) * 100).toInt()}% Complete")
                    val matching = stepLogs.filter { it.second == currWholeSec }
                    for (log in matching) {
                        currentLogs.add("  ${log.text}")
                    }
                    _forcedConstructionLogs.value = currentLogs
                }
            }

            val oldStock = geneSequences.value.associateBy { it.sequence }
            val newStockMap = currentStock.associateBy { it.sequence }

            val toDelete = mutableListOf<GeneSequence>()
            val toUpdate = mutableListOf<GeneSequence>()

            for ((seq, oldGene) in oldStock) {
                val newGene = newStockMap[seq]
                if (newGene == null) {
                    toDelete.add(oldGene)
                } else if (newGene.count != oldGene.count) {
                    toUpdate.add(newGene)
                }
            }

            if (toDelete.isNotEmpty() || toUpdate.isNotEmpty()) {
                repository.updateGeneStock(toUpdate, toDelete)
            }

            _splicerSlots.value = List<String?>(8) { null }

            if (failedAtSecond != -1) {
                _isReactorFrozen.value = false
                _isForcedConstructionActive.value = false
                _isForcedLoopActive.value = false
                _selectedTab.value = "splicer"
                synthManager.playReject()
                addLog("FORCED CONSTRUCTION FAILED: Nucleotide stockpile depleted!")
            } else {
                val spawned = compileDeterministicOffline(dnaSeq)
                val matchesTarget = getCoherence(dnaSeq, _targetSequence.value) == "full"
                val idSuffix = UUID.randomUUID().toString().take(6).uppercase()
                val finalCreature = spawned.copy(
                    id = "PX-$idSuffix",
                    name = "${spawned.name} [FORCED]",
                    lore = "This hybrid genome was forced together in the bio-lab reactor. ${spawned.lore}",
                    origin = "Forced Synthesis",
                    isFullCoherence = matchesTarget,
                    coherenceType = if (matchesTarget) "Forced" else null
                )

                repository.insertCreature(finalCreature)
                synthManager.playSynthesisSuccess()

                if (_isForcedLoopActive.value) {
                    addLog("[FORCED LOOP] Specimen \"${finalCreature.name}\" assembled successfully! Continuing loop iteration...")

                    var nextDna = dnaSeq
                    if (!isCustom) {
                        nextDna = generateRandom64Sequence()
                        repository.saveTargetSequence(nextDna)
                    }

                    kotlinx.coroutines.delay(1000L)
                    if (_isForcedLoopActive.value) {
                        runForcedConstructionCycle(nextDna, isCustom)
                    } else {
                        _isReactorFrozen.value = false
                        _isForcedConstructionActive.value = false
                        addLog("FORCED LOOP ENDED. Reactor is now available for normal sequencing.")
                    }
                } else {
                    _isReactorFrozen.value = false
                    _isForcedConstructionActive.value = false
                    _activeCreature.value = finalCreature
                    _selectedTab.value = "splicer"

                    if (!isCustom) {
                        val nextTarget = generateRandom64Sequence()
                        repository.saveTargetSequence(nextTarget)
                    }

                    addLog("FORCED CONSTRUCTION COMPLETED. Produced sequence: \"${finalCreature.name}\"!")
                }
            }
        }
    }

    fun generateRandom64Sequence(): String {
        val bases = listOf('A', 'G', 'T', 'C')
        val random = java.util.Random()
        return (1..64).map { bases[random.nextInt(4)] }.joinToString("")
    }

    private var heartbeatJob: kotlinx.coroutines.Job? = null
    private var scrollingTickerJob: kotlinx.coroutines.Job? = null

    private fun startReactorHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                
                if (!_isReactorFrozen.value) {
                    // Decrement active booster timers
                    if (_boostSecondsLeft.value > 0) {
                        _boostSecondsLeft.value -= 1
                        if (_boostSecondsLeft.value == 0) {
                            addLog("REACTOR BOOSTER EXPIRED: Splicing cycle returned to 16 seconds standard.")
                        }
                    }
                    
                    val isBoostActive = _boostSecondsLeft.value > 0
                    val resetVal = if (isBoostActive) 8 else 16
                    
                    // 1. Tick P.O.X. Reactor if active (paused for testing)
                    if (_poxReactorActive.value) {
                        val currentPoxIdle = _poxIdleTime.value
                        var nextPoxVal = currentPoxIdle - 1
                        if (isBoostActive && currentPoxIdle > 8) {
                            nextPoxVal = 8
                        }
                        
                        if (nextPoxVal <= 0) {
                            triggerStandardSynthesis()
                            nextPoxVal = resetVal
                        }
                        _poxIdleTime.value = nextPoxVal
                    }
                    
                    // 2. Tick Anomaly Engine if active (paused for testing)
                    if (_anomalyEngineActive.value) {
                        val currentAnomalyIdle = _anomalyIdleTime.value
                        var nextAnomalyVal = currentAnomalyIdle - 1
                        if (isBoostActive && currentAnomalyIdle > 8) {
                            nextAnomalyVal = 8
                        }
                        
                        if (nextAnomalyVal <= 0) {
                            if (grandTotalStandardNucleotides.value >= 250000L) {
                                triggerAnomalousSynthesis()
                            } else {
                                _anomalyEngineActive.value = false
                                addLog("ANOMALY ENGINE SHUT DOWN: Nucleotide reserves fell below minimum 250k threshold.")
                            }
                            nextAnomalyVal = resetVal
                        }
                        _anomalyIdleTime.value = nextAnomalyVal
                    }
                          // 3. Tick active harvest missions in database
                    val activeMissionsList = activeMissions.value
                    if (activeMissionsList.isNotEmpty()) {
                        activeMissionsList.forEach { m ->
                            if (!m.isReturned) {
                                val nextElapsed = m.elapsedSeconds + 1
                                val currentPhaseElapsed = m.currentPhaseElapsed + 1
                                
                                var nextPhase = m.phase
                                var nextPhaseElapsed = currentPhaseElapsed
                                val phaseLogs = mutableListOf<String>()

                                when (m.phase) {
                                    "TRAVEL" -> {
                                        if (currentPhaseElapsed >= m.travelDuration) {
                                            nextPhase = "DESCENT"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[TELEMETRY] Arrived at anomaly boundary. Initiating descent phase into well.")
                                        }
                                    }
                                    "DESCENT" -> {
                                        if (currentPhaseElapsed >= m.descentDuration) {
                                            nextPhase = "HARVESTING"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[TELEMETRY] Stalled depth reached at ${m.stalledDepth.toInt()}%. Initiating extraction protocol.")
                                        }
                                    }
                                    "HARVESTING" -> {
                                        if (currentPhaseElapsed >= m.harvestDuration) {
                                            nextPhase = "ASCENT"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[TELEMETRY] Extraction phase complete. Commencing ascent vector back to boundary.")
                                        }
                                    }
                                    "ASCENT" -> {
                                        if (currentPhaseElapsed >= m.ascentDuration) {
                                            nextPhase = "TRANST_BACK"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[TELEMETRY] Boundary reached. Transitioning to homeward transit trajectory.")
                                        }
                                    }
                                    "TRANST_BACK" -> {
                                        if (currentPhaseElapsed >= m.transitBackDuration) {
                                            nextPhase = "COMPLETED"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[COMPLETE] Specimen returned to base proximity. Sequence payload ready for stockpile.")
                                        }
                                    }
                                }

                                val isCompleted = nextPhase == "COMPLETED"
                                
                                val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
                                val phaseFraction = wave.lunarAge / WaveMath.LUNAR_MONTH_DAYS
                                val lunarPhaseScale = (1.0 - kotlin.math.cos(phaseFraction * 2.0 * Math.PI)) / 2.0
                                val lunarMutationMod = 0.5 + 1.0 * lunarPhaseScale
                                val mutationInterval = Math.round((480.0 * Math.pow(2.0, -m.stalledDepth / 25.0)) / lunarMutationMod / 16.0).coerceAtLeast(1L)
                                
                                val newLogs = m.missionLogs.toMutableList()
                                phaseLogs.forEach { log ->
                                    newLogs.add(log)
                                }
                                
                                // Check for mutation (only allowed while inside the boundary: DESCENT, HARVESTING, ASCENT)
                                if (!isCompleted && (m.phase == "DESCENT" || m.phase == "HARVESTING" || m.phase == "ASCENT") && nextElapsed % mutationInterval == 0L) {
                                    val creature = repository.getCreatureById(m.creatureId)
                                    if (creature != null) {
                                        val bases = listOf("A", "G", "T", "C")
                                        val chars = creature.sequence.toCharArray()
                                        val mutationIndex = (Math.random() * chars.size).toInt()
                                        val oldBase = chars[mutationIndex].toString()
                                        val choices = bases.filter { it != oldBase }
                                        val newBase = choices[(Math.random() * choices.size).toInt()]
                                        chars[mutationIndex] = newBase[0]
                                        val mutatedSequence = String(chars)
                                         
                                        val proc = compileDeterministicOffline(mutatedSequence)
                                        val updatedCreature = creature.copy(
                                            sequence = mutatedSequence,
                                            vitality = proc.vitality,
                                            attack = proc.attack,
                                            defense = proc.defense,
                                            speed = proc.speed,
                                            faction = proc.faction,
                                            type = proc.type,
                                            primaryWeapon = proc.primaryWeapon,
                                            lore = proc.lore,
                                            isMutated = true,
                                            originalSequence = creature.originalSequence ?: m.originalSequence ?: creature.sequence
                                        )
                                        repository.insertCreature(updatedCreature)
                                        newLogs.add("[MUTATION] Pos $mutationIndex: $oldBase -> $newBase. Stats re-calculated.")
                                    }
                                }
                                
                                val updatedMission = m.copy(
                                    elapsedSeconds = nextElapsed,
                                    isCompleted = isCompleted,
                                    missionLogs = newLogs,
                                    phase = nextPhase,
                                    currentPhaseElapsed = nextPhaseElapsed
                                )
                                if (isCompleted) {
                                    recallMission(updatedMission)
                                } else {
                                    repository.insertMission(updatedMission)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startScrollingGeneTicker() {
        scrollingTickerJob?.cancel()
        scrollingTickerJob = viewModelScope.launch(Dispatchers.Default) {
            val bases = listOf("A", "G", "T", "C")
            val rand = java.util.Random()
            while (true) {
                kotlinx.coroutines.delay(110L)
                var temp = ""
                for (i in 0 until 8) {
                    temp += bases[rand.nextInt(bases.size)]
                }
                _scrollingGene.value = temp
            }
        }
    }

    private suspend fun triggerStandardSynthesis() {
        val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
        val batch = mutableListOf<String>()
        val random = java.util.Random()
        for (i in 0 until 8) {
            batch.add(WaveMath.generateWaveGeneBlock(wave, random))
        }

        val currentList = geneSequences.value
        val newGenesList = mutableListOf<String>()
        val toInsertOrUpdate = mutableListOf<GeneSequence>()

        val batchCounts = batch.groupingBy { it }.eachCount()

        batchCounts.forEach { (sequence, count) ->
            val match = currentList.find { it.sequence == sequence }
            if (match != null) {
                toInsertOrUpdate.add(match.copy(count = match.count + count))
            } else {
                newGenesList.add(sequence)
                toInsertOrUpdate.add(GeneSequence(sequence, count, System.currentTimeMillis()))
            }
        }

        withContext(Dispatchers.IO) {
            repository.insertGeneSequences(toInsertOrUpdate)
        }

        // Save batch packet log
        val packet = GenePacket(
            id = "PKT-" + UUID.randomUUID().toString().take(6).uppercase(),
            genes = batch,
            timestamp = System.currentTimeMillis(),
            isAnomalous = false,
            newGenes = newGenesList
        )
        _discoveredPacketsLog.value = (listOf(packet) + _discoveredPacketsLog.value).take(50)

        addLog("REACTOR SYNTHESIS: 8 standard genes successfully generated.")
        synthManager.playCombinatorTick()
    }

    private suspend fun triggerAnomalousSynthesis() {
        val isDevMode = _devForceAnomaly.value
        val isSuccess = if (isDevMode) {
            true
        } else {
            val grandTotal = grandTotalStandardNucleotides.value
            val coupling = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
            val chanceMetrics = WaveMath.getAnomalyEngineSuccessChance(grandTotal, coupling)
            val roll = java.util.Random().nextDouble() * 100.0

            // Consume standard nucleotides
            consumeNucleotides(10000)
            roll <= chanceMetrics.finalChance
        }

        val currentList = geneSequences.value
        val newGenesList = mutableListOf<String>()

        val generated = if (isSuccess) {
            val anomalousGene = WaveMath.generateAnomalousGene()
            val match = currentList.find { it.sequence == anomalousGene }
            if (match != null) {
                repository.insertGeneSequence(match.copy(count = match.count + 1))
            } else {
                newGenesList.add(anomalousGene)
                repository.insertGeneSequence(GeneSequence(anomalousGene, 1, System.currentTimeMillis()))
            }
            anomalousGene
        } else {
            "DECAYED!"
        }

        val packet = GenePacket(
            id = "ANM-" + UUID.randomUUID().toString().take(6).uppercase(),
            genes = listOf(generated),
            timestamp = System.currentTimeMillis(),
            isAnomalous = true,
            newGenes = newGenesList
        )
        _discoveredPacketsLog.value = (listOf(packet) + _discoveredPacketsLog.value).take(50)

        if (isSuccess) {
            if (isDevMode) {
                addLog("[DEV MODE FUSION] Force-triggered successfully (Zero resources consumed). Generated anomalous block $generated.")
            } else {
                addLog("[ANOMALY ENGINE] Unstable Fusion Success! Formed anomalous block $generated.")
            }
            synthManager.playSynthesisSuccess()
        } else {
            addLog("[ANOMALY ENGINE] Fusion decay: 10,000 standard nucleotides decomposed in buffer.")
            synthManager.playBeep(220f, 0.35f, "sawtooth")
        }
    }

    private suspend fun consumeNucleotides(amount: Int) {
        var remaining = Math.ceil(amount.toDouble() / 8.0).toInt()
        val standardGenes = geneSequences.value.filter { !WaveMath.isAnomalousGene(it.sequence) }
        val toUpdate = mutableListOf<GeneSequence>()
        val toDelete = mutableListOf<GeneSequence>()

        for (gene in standardGenes) {
            if (remaining <= 0) break
            val toTake = Math.min(gene.count, remaining)
            remaining -= toTake
            val newCount = gene.count - toTake
            if (newCount <= 0) {
                toDelete.add(gene)
            } else {
                toUpdate.add(gene.copy(count = newCount))
            }
        }

        if (toUpdate.isNotEmpty() || toDelete.isNotEmpty()) {
            repository.updateGeneStock(toUpdate, toDelete)
        }
    }

    fun clearDiscoveredPacketsLog() {
        _discoveredPacketsLog.value = emptyList()
    }

    private fun getCoherence(seq: String, target: String): String {
        if (seq.isEmpty() || target.isEmpty()) return "none"
        val seq64 = seq.take(64).uppercase()
        val target64 = target.take(64).uppercase()
        if (seq64 == target64) return "full"

        var alignedMatches = 0
        for (i in 0 until 8) {
            val start = i * 8
            val end = (i + 1) * 8
            if (start < seq64.length && end <= seq64.length && start < target64.length && end <= target64.length) {
                val seqGene = seq64.substring(start, end)
                val tgtGene = target64.substring(start, end)
                if (seqGene == tgtGene) {
                    alignedMatches++
                }
            }
        }
        if (alignedMatches == 8) return "full"
        return "none"
    }
}

data class GenePacket(
    val id: String,
    val genes: List<String>,
    val timestamp: Long,
    val isAnomalous: Boolean,
    val newGenes: List<String> = emptyList()
)

data class PoxAnomaly(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val gene: String,
    val faction: String,
    val distance: Double,
    val heatZoneDiameter: Double,
    val density: Double = 0.0
) {
    fun getBoundaryRadius(theta: Double): Double {
        val seed = id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
        val r0 = heatZoneDiameter / 2.0
        val epsilon = 0.15 + (seed % 3) * 0.05 // 0.15, 0.20, 0.25
        val k = 3 + (seed % 3)                 // 3, 4, 5 lobes
        val phi = (seed % 360) * (Math.PI / 180.0)
        return r0 * (1.0 + epsilon * kotlin.math.cos(k * theta + phi))
    }

    fun getBoundaryRadiusForPlayer(userLat: Double, userLng: Double): Double {
        val dLat = userLat - lat
        val dLng = (userLng - lng) * kotlin.math.cos(Math.toRadians(userLat))
        val theta = kotlin.math.atan2(dLat, dLng)
        return getBoundaryRadius(theta)
    }
}

class MainViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class UnlockedMove(
    val type: String,
    val name: String,
    val description: String
)

data class EmotSound(
    val oscillator: String,
    val frequencies: List<Int>,
    val noteDelay: Double,
    val noteDuration: Double
)

data class DisintegratedModalData(
    val name: String,
    val returnedBlocks: List<String>,
    val destroyedBlocks: List<String>
)

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
import java.util.Locale
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
        if (id != null) {
            _scannerSubTab.value = "radar"
            _selectedAnomalyId.value = null
            selectTab("scanner")
        }
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

    // Raw nucleotide stocks (in-memory MutableStateFlows backed by repository updates)
    private val _rawStockA = MutableStateFlow(10000L)
    val rawStockA: StateFlow<Long> = _rawStockA.asStateFlow()

    private val _rawStockG = MutableStateFlow(10000L)
    val rawStockG: StateFlow<Long> = _rawStockG.asStateFlow()

    private val _rawStockT = MutableStateFlow(10000L)
    val rawStockT: StateFlow<Long> = _rawStockT.asStateFlow()

    private val _rawStockC = MutableStateFlow(10000L)
    val rawStockC: StateFlow<Long> = _rawStockC.asStateFlow()

    // Biophysics reactor parameters
    private val _reactorTemperature = MutableStateFlow(37.0f)
    val reactorTemperature: StateFlow<Float> = _reactorTemperature.asStateFlow()

    private val _reactorSalt = MutableStateFlow(0.05f)
    val reactorSalt: StateFlow<Float> = _reactorSalt.asStateFlow()

    private val _activePolymerase = MutableStateFlow("Taq")
    val activePolymerase: StateFlow<String> = _activePolymerase.asStateFlow()

    private val _activeChemicalSolute = MutableStateFlow("None")
    val activeChemicalSolute: StateFlow<String> = _activeChemicalSolute.asStateFlow()

    private val _inletRatioA = MutableStateFlow(0.25f)
    val inletRatioA: StateFlow<Float> = _inletRatioA.asStateFlow()

    private val _inletRatioG = MutableStateFlow(0.25f)
    val inletRatioG: StateFlow<Float> = _inletRatioG.asStateFlow()

    private val _inletRatioT = MutableStateFlow(0.25f)
    val inletRatioT: StateFlow<Float> = _inletRatioT.asStateFlow()

    private val _inletRatioC = MutableStateFlow(0.25f)
    val inletRatioC: StateFlow<Float> = _inletRatioC.asStateFlow()

    fun setReactorTemperature(value: Float) {
        _reactorTemperature.value = value
    }

    fun setReactorSalt(value: Float) {
        _reactorSalt.value = value
    }

    fun setActivePolymerase(value: String) {
        val current = _activePolymerase.value
        if (current.equals(value, ignoreCase = true)) return
        
        val valueUpper = value.uppercase()
        val reqBase = when (valueUpper) {
            "TTH" -> 25L
            "PFU" -> 50L
            else -> 0L
        }
        val reqWaste = when (valueUpper) {
            "TTH" -> 50L
            "PFU" -> 150L
            else -> 0L
        }
        
        if (_rawStockA.value < reqBase || _rawStockG.value < reqBase || _rawStockT.value < reqBase || _rawStockC.value < reqBase || _bioWaste.value < reqWaste) {
            addLog("SELECTION ERROR: Insufficient stocks to engage $value ($value requires $reqBase of each base & $reqWaste Bio-Waste).")
            synthManager.playReject()
            return
        }
        
        _activePolymerase.value = value
        
        val costMsg = when (valueUpper) {
            "TAQ" -> "Baseline enzyme selected (Free)."
            "TTH" -> "Tth selected (Cost: 25 of all bases & 50 Bio-Waste deducted upon ignition)."
            "PFU" -> "Pfu selected (Cost: 50 of all bases & 150 Bio-Waste deducted upon ignition)."
            else -> ""
        }
        addLog("ENGAGED $value POLYMERASE: $costMsg")
        synthManager.playBeep(440f, 0.1f, "sine")
    }

    fun setActiveChemicalSolute(value: String) {
        _activeChemicalSolute.value = value
    }

    fun setInletRatioA(value: Float) {
        _inletRatioA.value = value.coerceIn(0.05f, 0.90f)
    }

    fun setInletRatioG(value: Float) {
        _inletRatioG.value = value.coerceIn(0.05f, 0.90f)
    }

    fun setInletRatioT(value: Float) {
        _inletRatioT.value = value.coerceIn(0.05f, 0.90f)
    }

    fun setInletRatioC(value: Float) {
        _inletRatioC.value = value.coerceIn(0.05f, 0.90f)
    }

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

    // Cumulative standard nucleotides consumed during the current active anomaly run
    private val _anomalyConsumedBases = MutableStateFlow(0L)
    val anomalyConsumedBases: StateFlow<Long> = _anomalyConsumedBases.asStateFlow()

    // Chronological log of past synthesized gene packets
    private val _discoveredPacketsLog = MutableStateFlow<List<GenePacket>>(emptyList())
    val discoveredPacketsLog: StateFlow<List<GenePacket>> = _discoveredPacketsLog.asStateFlow()

    // 8-character string mimicking visual synthesizer readout scrolling
    private val _scrollingGene = MutableStateFlow("--------")
    val scrollingGene: StateFlow<String> = _scrollingGene.asStateFlow()

    // Real-time single-gene stepped synthesis states
    private val _activeSynthesisStrand = MutableStateFlow("")
    val activeSynthesisStrand: StateFlow<String> = _activeSynthesisStrand.asStateFlow()

    private val _activeSynthesisStep = MutableStateFlow(0)
    val activeSynthesisStep: StateFlow<Int> = _activeSynthesisStep.asStateFlow()

    private val _activeSynthesisQScore = MutableStateFlow(0.0)
    val activeSynthesisQScore: StateFlow<Double> = _activeSynthesisQScore.asStateFlow()

    private val _activeSynthesisTm = MutableStateFlow(0.0)
    val activeSynthesisTm: StateFlow<Double> = _activeSynthesisTm.asStateFlow()

    private val _activeSynthesisMfe = MutableStateFlow(0.0)
    val activeSynthesisMfe: StateFlow<Double> = _activeSynthesisMfe.asStateFlow()

    private val _targetSynthesisSequence = MutableStateFlow("AAAAAAAA")
    val targetSynthesisSequence: StateFlow<String> = _targetSynthesisSequence.asStateFlow()

    private val _isSynthesisActive = MutableStateFlow(false)
    val isSynthesisActive: StateFlow<Boolean> = _isSynthesisActive.asStateFlow()

    private val _isReactionCollapsed = MutableStateFlow(false)
    val isReactionCollapsed: StateFlow<Boolean> = _isReactionCollapsed.asStateFlow()

    private val _bioWaste = MutableStateFlow(0L)
    val bioWaste: StateFlow<Long> = _bioWaste.asStateFlow()

    private val _isLoopActive = MutableStateFlow(false)
    val isLoopActive: StateFlow<Boolean> = _isLoopActive.asStateFlow()

    fun toggleLoopActive() {
        _isLoopActive.value = !_isLoopActive.value
        synthManager.playBeep(if (_isLoopActive.value) 600f else 400f, 0.1f, "sine")
    }

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

    fun addDevBases() {
        viewModelScope.launch(Dispatchers.IO) {
            val a = _rawStockA.value + 10000L
            val g = _rawStockG.value + 10000L
            val t = _rawStockT.value + 10000L
            val c = _rawStockC.value + 10000L
            
            _rawStockA.value = a
            _rawStockG.value = g
            _rawStockT.value = t
            _rawStockC.value = c
            repository.saveRawStocks(a, g, t, c)
            addLog("DEV: Injected 10,000 units of all raw bases (A, G, T, C).")
            synthManager.playSynthesisSuccess()
        }
    }

    fun clearDevBases() {
        viewModelScope.launch(Dispatchers.IO) {
            _rawStockA.value = 0L
            _rawStockG.value = 0L
            _rawStockT.value = 0L
            _rawStockC.value = 0L
            _bioWaste.value = 0L
            repository.saveRawStocks(0L, 0L, 0L, 0L)
            
            val currentPoly = _activePolymerase.value.uppercase()
            if (currentPoly == "TTH" || currentPoly == "PFU") {
                _activePolymerase.value = "Taq"
                addLog("SYS WARNING: Selected enzyme reset to Taq due to stock depletion.")
            }
            
            addLog("DEV: Raw stockpiles and Bio-Waste cleared to 0.")
            synthManager.playReject()
        }
    }

    fun clearDevGenes() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllGeneSequences()
            addLog("DEV: Cleared all Gene Sequences from database.")
            synthManager.playReject()
        }
    }

    fun clearDevCreatures() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllCreatures()
            addLog("DEV: Cleared all Creature specimens from database.")
            synthManager.playReject()
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

    private val _buildings = MutableStateFlow<List<BuildingStructure>>(emptyList())
    val buildings: StateFlow<List<BuildingStructure>> = _buildings.asStateFlow()

    private val _cachedCells = MutableStateFlow<List<String>>(emptyList())
    val cachedCells: StateFlow<List<String>> = _cachedCells.asStateFlow()

    private val _proceduralHeatwaves = MutableStateFlow<List<String>>(emptyList())

    val heatwaveCells: StateFlow<List<String>> = combine(_cachedCells, _proceduralHeatwaves) { cached, heatwaves ->
        (cached + heatwaves).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _zoomMultiplier = MutableStateFlow(1.0f)
    val zoomMultiplier: StateFlow<Float> = _zoomMultiplier.asStateFlow()

    private val _isProfilerEnabled = MutableStateFlow(false)
    val isProfilerEnabled: StateFlow<Boolean> = _isProfilerEnabled.asStateFlow()

    val profilerState = com.example.genpox.ui.components.MapProfilerState()

    fun toggleProfiler() {
        _isProfilerEnabled.value = !_isProfilerEnabled.value
        if (_isProfilerEnabled.value) {
            synthManager.playBeep(520f, 0.1f, "square")
        } else {
            synthManager.playBeep(330f, 0.1f, "sine")
        }
    }

    private var lastCheckedCellKey: String? = null
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
        // Observe database/repository stocks to initialize and keep in-sync
        viewModelScope.launch {
            repository.rawStockA.collect { value ->
                _rawStockA.value = value
            }
        }
        viewModelScope.launch {
            repository.rawStockG.collect { value ->
                _rawStockG.value = value
            }
        }
        viewModelScope.launch {
            repository.rawStockT.collect { value ->
                _rawStockT.value = value
            }
        }
        viewModelScope.launch {
            repository.rawStockC.collect { value ->
                _rawStockC.value = value
            }
        }

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

            val currentStock = geneSequences.value
            val blocks = (0 until newSeq.length step 8).map { newSeq.substring(it, it + 8) }
            val qScoresList = blocks.map { seq ->
                currentStock.find { it.sequence == seq }?.averageQScore ?: 25.0
            }
            val avgQ = qScoresList.average()
            val scaledTelomeres = (avgQ * 2.5).toInt().coerceIn(10, 100)
            val scaledVitality = (details.vitality * (0.5 + 0.5 * (avgQ / 40.0))).toInt()
            
            val updatedCreature = creature.copy(
                sequence = newSeq,
                appendedGenes = newAppended,
                vitality = scaledVitality,
                telomeres = scaledTelomeres,
                attack = details.attack,
                defense = details.defense,
                speed = details.speed,
                type = details.type,
                faction = details.faction,
                lore = details.lore,
                primaryWeapon = details.primaryWeapon
            )
            repository.insertCreature(updatedCreature)

            addLog("MUTATION: Welded gene $gene to \"${creature.name}\". Stats updated (Q-score scaling applied).")
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
        _terminalLogs.update { current ->
            (current + "[$timeStr] $log").takeLast(100)
        }
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
        addLog("SYS: Terminal cache cleared.")
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

    fun refreshMap() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearMapCache()
            fetchRoads(_latitude.value, _longitude.value)
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
        if (cellKey != lastCheckedCellKey) {
            lastCheckedCellKey = cellKey
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

    private fun generateFallbackBuildings(lat: Double, lng: Double): List<List<RoadPoint>> {
        val result = mutableListOf<List<RoadPoint>>()
        val random = java.util.Random((lat.hashCode() + lng.hashCode()).toLong())
        val latStep = 0.00135
        val lngStep = 0.00135 / kotlin.math.cos(Math.toRadians(lat))
        
        val startLat = Math.floor(lat / latStep) * latStep - 15.0 * latStep
        val startLng = Math.floor(lng / lngStep) * lngStep - 15.0 * lngStep
        
        for (i in 0..30 step 2) {
            for (j in 0..30 step 2) {
                if (random.nextFloat() < 0.4f) { // 40% chance of building at intersection cell
                    val bLat = startLat + i * latStep + 0.0003
                    val bLng = startLng + j * lngStep + 0.0003
                    
                    val w = 0.0002
                    val h = 0.0002 / kotlin.math.cos(Math.toRadians(lat))
                    
                    val polygon = listOf(
                        RoadPoint(bLat, bLng),
                        RoadPoint(bLat + w, bLng),
                        RoadPoint(bLat + w, bLng + h),
                        RoadPoint(bLat, bLng + h),
                        RoadPoint(bLat, bLng) // close polygon
                    )
                    result.add(polygon)
                }
            }
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
            val radius = 3

            val cellsToQuery = mutableListOf<String>()
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    cellsToQuery.add("${cellX + dx},${cellY + dy}")
                }
            }

            val cachedRoadCells = mutableListOf<CachedRoadCell>()
            val cachedBuildingCells = mutableListOf<CachedBuildingCell>()
            val now = System.currentTimeMillis()
            var allCachedAndValid = true

            for (key in cellsToQuery) {
                val dbRoadCell = repository.getCachedRoadCell(key)
                val dbBuildingCell = repository.getCachedBuildingCell(key)
                if (dbRoadCell != null && dbBuildingCell != null && 
                    (now - dbRoadCell.fetchedAt) < 24 * 60 * 60 * 1000L && 
                    (now - dbBuildingCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                    cachedRoadCells.add(dbRoadCell)
                    cachedBuildingCells.add(dbBuildingCell)
                } else {
                    allCachedAndValid = false
                }
            }

            if (allCachedAndValid) {
                android.util.Log.d("PoxRadar", "Active neighborhood cells valid. Populating map from DB cache.")
                addLog("SYS: Loaded map grid from local cache.")
                val maxCellsToLoad = mutableListOf<String>()
                for (dx in -radius..radius) {
                    for (dy in -radius..radius) {
                        maxCellsToLoad.add("${cellX + dx},${cellY + dy}")
                    }
                }
                val allRoads = mutableListOf<List<Pair<Double, Double>>>()
                val allBuildings = mutableListOf<BuildingStructure>()
                maxCellsToLoad.forEach { key ->
                    val roadCell = repository.getCachedRoadCell(key)
                    val buildingCell = repository.getCachedBuildingCell(key)
                    if (roadCell != null && (now - roadCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                        try {
                            val roadsList = Json.decodeFromString<List<List<RoadPoint>>>(roadCell.roadsJson)
                            roadsList.forEach { road ->
                                allRoads.add(road.map { Pair(it.lat, it.lng) })
                            }
                        } catch (e: Exception) {}
                    }
                    if (buildingCell != null && (now - buildingCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                        try {
                            val buildingsList = Json.decodeFromString<List<BuildingStructure>>(buildingCell.buildingsJson)
                            allBuildings.addAll(buildingsList)
                        } catch (e: Exception) {}
                    }
                }
                _roads.value = allRoads.distinct()
                if (allRoads.isNotEmpty() && allBuildings.isEmpty()) {
                    android.util.Log.d("PoxRadar", "DB cache valid but buildings are empty. Generating fallback buildings.")
                    val fallback = generateFallbackBuildings(lat, lng)
                    val fallbackStructures = fallback.map { BuildingStructure(points = it, isFallback = true) }
                    _buildings.value = fallbackStructures.distinct()
                } else {
                    _buildings.value = allBuildings.distinct()
                }
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
            // Always query both roads and buildings to ensure the local cache is fully populated for zooming
            val query = "[out:json];(way($minLat,$minLng,$maxLat,$maxLng)[highway~\"motorway|trunk|primary|secondary|tertiary|unclassified|residential\"];way($minLat,$minLng,$maxLat,$maxLng)[building];);out geom;"
            var fetchedRoads: List<List<RoadPoint>> = emptyList()
            var fetchedBuildings: List<BuildingStructure> = emptyList()
            var femaBuildings: List<BuildingStructure> = emptyList()
            var usgsBuildings: List<BuildingStructure> = emptyList()

            // 1. Query FEMA USA Structures REST API first as the primary building footprints source
            if (isActive) {
                try {
                    android.util.Log.d("PoxRadar", "Querying FEMA USA Structures as primary...")
                    addLog("SYS: Querying FEMA USA Structures (Primary)...")
                    val femaUrl = "https://services2.arcgis.com/FiaPA4ga0iQKduv3/arcgis/rest/services/USA_Structures_View/FeatureServer/0/query" +
                            "?geometry=$minLng,$minLat,$maxLng,$maxLat" +
                            "&geometryType=esriGeometryEnvelope" +
                            "&spatialRel=esriSpatialRelIntersects" +
                            "&inSR=4326" +
                            "&outSR=4326" +
                            "&returnGeometry=true" +
                            "&outFields=OBJECTID" +
                            "&f=json"
                    
                    val url = java.net.URL(femaUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "GenPoxRadar/1.0 (brent@example.com)")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val text = connection.inputStream.bufferedReader().use { it.readText() }
                        femaBuildings = parseArcgisJson(text)
                        android.util.Log.d("PoxRadar", "FEMA primary returned ${femaBuildings.size} buildings.")
                        addLog("SYS: Loaded ${femaBuildings.size} buildings from FEMA.")
                    } else {
                        android.util.Log.w("PoxRadar", "FEMA endpoint returned HTTP $responseCode")
                        addLog("SYS: FEMA query failed: HTTP $responseCode")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PoxRadar", "FEMA query failed", e)
                    addLog("SYS: FEMA query error: ${e.message}")
                }
            }

            // 1b. Query USGS National Structures REST API as the secondary building footprints source
            if (isActive) {
                try {
                    android.util.Log.d("PoxRadar", "Querying USGS National Structures...")
                    addLog("SYS: Querying USGS Structures (Secondary)...")
                    val usgsUrl = "https://carto.nationalmap.gov/arcgis/rest/services/structures/FeatureServer/0/query" +
                            "?geometry=$minLng,$minLat,$maxLng,$maxLat" +
                            "&geometryType=esriGeometryEnvelope" +
                            "&spatialRel=esriSpatialRelIntersects" +
                            "&inSR=4326" +
                            "&outSR=4326" +
                            "&returnGeometry=true" +
                            "&outFields=OBJECTID" +
                            "&f=json"
                    
                    val url = java.net.URL(usgsUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "GenPoxRadar/1.0 (brent@example.com)")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val text = connection.inputStream.bufferedReader().use { it.readText() }
                        usgsBuildings = parseArcgisJson(text)
                        android.util.Log.d("PoxRadar", "USGS returned ${usgsBuildings.size} buildings.")
                        addLog("SYS: Loaded ${usgsBuildings.size} buildings from USGS.")
                    } else {
                        android.util.Log.w("PoxRadar", "USGS endpoint returned HTTP $responseCode")
                        addLog("SYS: USGS query failed: HTTP $responseCode")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PoxRadar", "USGS query failed", e)
                    addLog("SYS: USGS query error: ${e.message}")
                }
            }

            // 2. Query OSM for Roads and secondary Buildings
            var osmBuildings: List<BuildingStructure> = emptyList()

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
                        
                        fetchedRoads = parseOverpassJson(text, "highway")
                        val rawBuildings = parseOverpassJson(text, "building")
                        osmBuildings = rawBuildings.map { BuildingStructure(points = it, isFallback = false) }
                        
                        if (fetchedRoads.isNotEmpty() || osmBuildings.isNotEmpty()) {
                            android.util.Log.d("PoxRadar", "Loaded map segments from $baseUrl.")
                            addLog("SYS: Loaded map data from $baseUrl.")
                            success = true
                            break
                        } else {
                            android.util.Log.w("PoxRadar", "Parsed 0 map features from $baseUrl response.")
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

            // 3. Method C: Merge FEMA (primary), USGS (secondary), and OSM (tertiary) buildings (avoiding duplicates)
            // 3. Method C: Merge FEMA (primary), USGS (secondary), and OSM (tertiary) buildings (avoiding duplicates)
            if (isActive) {
                fun getBBox(building: BuildingStructure): BuildingBBox {
                    var minL = Double.MAX_VALUE
                    var maxL = -Double.MAX_VALUE
                    var minLn = Double.MAX_VALUE
                    var maxLn = -Double.MAX_VALUE
                    building.points.forEach { pt ->
                        if (pt.lat < minL) minL = pt.lat
                        if (pt.lat > maxL) maxL = pt.lat
                        if (pt.lng < minLn) minLn = pt.lng
                        if (pt.lng > maxLn) maxLn = pt.lng
                    }
                    return BuildingBBox(minL, maxL, minLn, maxLn)
                }

                val mergedBuildings = femaBuildings.toMutableList()
                val mergedBoxes = mutableListOf<BuildingBBox>()

                femaBuildings.forEach { b ->
                    val box = getBBox(b)
                    val dLat = box.maxLat - box.minLat
                    val dLng = box.maxLng - box.minLng
                    if (dLat in 0.0..0.005 && dLng in 0.0..0.005) {
                        mergedBoxes.add(box)
                    } else {
                        android.util.Log.w("PoxRadar", "FEMA building filtered from merge check (too large/corrupt): dLat=$dLat, dLng=$dLng")
                    }
                }

                // Add USGS buildings if they don't overlap with already merged buildings
                usgsBuildings.forEach { usgs ->
                    val usgsBox = getBBox(usgs)
                    val dLat = usgsBox.maxLat - usgsBox.minLat
                    val dLng = usgsBox.maxLng - usgsBox.minLng
                    val intersects = mergedBoxes.any { box -> usgsBox.intersects(box) }
                    if (!intersects) {
                        mergedBuildings.add(usgs)
                        if (dLat in 0.0..0.005 && dLng in 0.0..0.005) {
                            mergedBoxes.add(usgsBox)
                        } else {
                            android.util.Log.w("PoxRadar", "USGS building filtered from merge check (too large/corrupt): dLat=$dLat, dLng=$dLng")
                        }
                    }
                }

                // Add OSM buildings if they don't overlap with already merged buildings
                var loggedIntersections = 0
                if (success && osmBuildings.isNotEmpty()) {
                    osmBuildings.forEach { osm ->
                        val osmBox = getBBox(osm)
                        val dLat = osmBox.maxLat - osmBox.minLat
                        val dLng = osmBox.maxLng - osmBox.minLng
                        val intersectingBox = mergedBoxes.find { box -> osmBox.intersects(box) }
                        if (intersectingBox == null) {
                            mergedBuildings.add(osm)
                            if (dLat in 0.0..0.005 && dLng in 0.0..0.005) {
                                mergedBoxes.add(osmBox)
                            }
                        } else {
                            if (loggedIntersections < 5) {
                                android.util.Log.d("PoxRadar", "Intersection Found! OSM Box: minLat=${osmBox.minLat}, maxLat=${osmBox.maxLat}, minLng=${osmBox.minLng}, maxLng=${osmBox.maxLng} overlapped with FEMA Box: minLat=${intersectingBox.minLat}, maxLat=${intersectingBox.maxLat}, minLng=${intersectingBox.minLng}, maxLng=${intersectingBox.maxLng}")
                                loggedIntersections++
                            }
                        }
                    }
                }
                fetchedBuildings = mergedBuildings
                android.util.Log.d("PoxRadar", "Sequential Merge: FEMA count=${femaBuildings.size}, USGS count=${usgsBuildings.size}, OSM count=${osmBuildings.size}, Merged total=${fetchedBuildings.size}")
            }
            
            if (!success && isActive) {
                android.util.Log.w("PoxRadar", "All OSM endpoints failed/empty. Generating fallback grid.")
                addLog("SYS: All OpenStreetMap endpoints failed. Generating fallback grid.")
                fetchedRoads = generateFallbackRoads(lat, lng)
                if (fetchedBuildings.isEmpty()) {
                    fetchedBuildings = generateFallbackBuildings(lat, lng).map { BuildingStructure(points = it, isFallback = true) }
                }
            }

            if (success && fetchedBuildings.isEmpty() && isActive) {
                android.util.Log.d("PoxRadar", "OSM, FEMA, and USGS returned 0 buildings. Generating fallback structures.")
                fetchedBuildings = generateFallbackBuildings(lat, lng).map { BuildingStructure(points = it, isFallback = true) }
            }

            if (isActive) {
                // Distribute fetched roads into the cells
                val cellRoadsMap = mutableMapOf<String, MutableList<List<RoadPoint>>>()
                val cellBuildingsMap = mutableMapOf<String, MutableList<BuildingStructure>>()
                cellsToQuery.forEach { key ->
                    cellRoadsMap[key] = mutableListOf()
                    cellBuildingsMap[key] = mutableListOf()
                }

                // Caching whole roads in cells they touch to prevent segment cutoffs at boundary edges
                fetchedRoads.forEach { way ->
                    if (way.size >= 2) {
                        val cellsForThisRoad = mutableSetOf<String>()
                        way.forEach { pt ->
                            val cx = Math.floor(pt.lat / 0.015).toInt()
                            val cy = Math.floor(pt.lng / 0.015).toInt()
                            val key = "$cx,$cy"
                            if (cellsToQuery.contains(key)) {
                                cellsForThisRoad.add(key)
                            }
                        }
                        cellsForThisRoad.forEach { key ->
                            cellRoadsMap[key]?.add(way)
                        }
                    }
                }

                fetchedBuildings.forEach { way ->
                    if (way.points.size >= 2) {
                        val cellsForThisBuilding = mutableSetOf<String>()
                        way.points.forEach { pt ->
                            val cx = Math.floor(pt.lat / 0.015).toInt()
                            val cy = Math.floor(pt.lng / 0.015).toInt()
                            val key = "$cx,$cy"
                            if (cellsToQuery.contains(key)) {
                                cellsForThisBuilding.add(key)
                            }
                        }
                        cellsForThisBuilding.forEach { key ->
                            cellBuildingsMap[key]?.add(way)
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

                for ((key, buildingsList) in cellBuildingsMap) {
                    val buildingsJson = Json.encodeToString(buildingsList)
                    val cachedCell = CachedBuildingCell(
                        cellKey = key,
                        buildingsJson = buildingsJson,
                        fetchedAt = now
                    )
                    repository.insertCachedBuildingCell(cachedCell)
                }

                // Expose all valid cells from the neighborhood to the UI flow
                val maxCellsToLoad = mutableListOf<String>()
                for (dx in -radius..radius) {
                    for (dy in -radius..radius) {
                        maxCellsToLoad.add("${cellX + dx},${cellY + dy}")
                    }
                }
                val allRoads = mutableListOf<List<Pair<Double, Double>>>()
                val allBuildings = mutableListOf<BuildingStructure>()
                maxCellsToLoad.forEach { key ->
                    val roadCell = repository.getCachedRoadCell(key)
                    val buildingCell = repository.getCachedBuildingCell(key)
                    if (roadCell != null && (now - roadCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                        try {
                            val roadsList = Json.decodeFromString<List<List<RoadPoint>>>(roadCell.roadsJson)
                            roadsList.forEach { road ->
                                allRoads.add(road.map { Pair(it.lat, it.lng) })
                            }
                        } catch (e: Exception) {}
                    }
                    if (buildingCell != null && (now - buildingCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                        try {
                            val buildingsList = Json.decodeFromString<List<BuildingStructure>>(buildingCell.buildingsJson)
                            allBuildings.addAll(buildingsList)
                        } catch (e: Exception) {}
                    }
                }
                _roads.value = allRoads.distinct()
                _buildings.value = allBuildings.distinct()
            }
        }
    }

    private fun parseOverpassJson(jsonStr: String, type: String): List<List<RoadPoint>> {
        val result = mutableListOf<List<RoadPoint>>()
        try {
            val root = org.json.JSONObject(jsonStr)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                val tags = element.optJSONObject("tags")
                val isBuilding = tags?.has("building") == true
                val isHighway = tags?.has("highway") == true
                
                val matches = if (type == "building") isBuilding else isHighway
                if (!matches) continue
                
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

    private fun parseArcgisJson(jsonStr: String): List<BuildingStructure> {
        val result = mutableListOf<BuildingStructure>()
        try {
            val root = org.json.JSONObject(jsonStr)
            val features = root.optJSONArray("features") ?: return emptyList()
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val geometry = feature.optJSONObject("geometry") ?: continue
                val rings = geometry.optJSONArray("rings") ?: continue
                if (rings.length() > 0) {
                    val ring = rings.getJSONArray(0)
                    val points = mutableListOf<RoadPoint>()
                    for (j in 0 until ring.length()) {
                        val pt = ring.getJSONArray(j)
                        val lng = pt.getDouble(0)
                        val lat = pt.getDouble(1)
                        points.add(RoadPoint(lat, lng))
                    }
                    if (points.size >= 2) {
                        result.add(BuildingStructure(points = points, isFallback = false))
                    }
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

            // Speed-based calculations (scaled up by a speedFactor of 1350.0 so speed=1 takes ~16 min at max distance)
            val travelTimeComponent = 32.0
            val travelDistanceComponent = 16.0
            val speedFactor = 1350.0
            val V_travel_base = (((creature.speed.toDouble() / 50.0) * travelDistanceComponent) / travelTimeComponent) * speedFactor
            
            val V_travel = V_travel_base
            
            val travelDistance = maxOf(0.0, anomaly.distance - boundaryRadius)
            val travelDuration = if (V_travel > 0.0) {
                Math.round(travelDistance / V_travel).coerceAtLeast(1L)
            } else {
                32L
            }

            val V_descent = V_travel_base * (1.0 - 2.0 * finalDensity).coerceAtLeast(0.1) * 0.024
            
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
            setTrackedMissionId(mission.id)
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
            _anomalyConsumedBases.value = 0L // Reset cumulative run counter on start
            addLog("ANOMALY ENGINE ENGAGED! Cosmic gene hunting activated!")
            synthManager.playBeep(120f, 0.6f, "sawtooth")
        } else {
            _anomalyEngineActive.value = false
            _anomalyConsumedBases.value = 0L // Clear cumulative run counter
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

            val currentStock = geneSequences.value
            val qScoresList = slots.filterNotNull().map { seq ->
                currentStock.find { it.sequence == seq }?.averageQScore ?: 25.0
            }
            val avgQ = qScoresList.average()
            val scaledTelomeres = (avgQ * 2.5).toInt().coerceIn(10, 100)

            val spawned = compileDeterministicOffline(fullDNASeq)
            val scaledVitality = (spawned.vitality * (0.5 + 0.5 * (avgQ / 40.0))).toInt()
            
            val matchesTarget = getCoherence(fullDNASeq, _targetSequence.value) == "full"
            val idSuffix = UUID.randomUUID().toString().take(6).uppercase()
            val finalCreature = spawned.copy(
                id = "PX-$idSuffix",
                name = spawned.name,
                lore = spawned.lore,
                origin = "Spliced Gene",
                isFullCoherence = matchesTarget,
                coherenceType = if (matchesTarget) "Natural" else null,
                telomeres = scaledTelomeres,
                vitality = scaledVitality
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

            var tempRawA = rawStockA.value
            var tempRawG = rawStockG.value
            var tempRawT = rawStockT.value
            var tempRawC = rawStockC.value

            data class StepLog(val second: Int, val text: String)
            val stepLogs = mutableListOf<StepLog>()

            val activeSplicerSlots = _splicerSlots.value
            var failedAtSecond = -1

            for (i in 0 until 8) {
                val expectedGene = dnaSeq.substring(i * 8, (i + 1) * 8)
                val isManualMatch = !isCustom && activeSplicerSlots.getOrNull(i) == expectedGene

                // Scaffold checks (100% success or unstable base/void)
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

                // Apply MFE fold stalling penalty
                val mfe = BiophysicsEngine.calculateMinimumFreeEnergy(expectedGene)
                val isFoldStalled = mfe <= -5.0
                val slotFailureChance = failureChance + (if (isFoldStalled) 10.0 else 0.0)
                if (isFoldStalled) {
                    stepLogs.add(StepLog(
                        second = minOf(7, i + 1),
                        text = "  ➔ [WARNING] >> Fold Stalling Penalty active (+10.0% failure chance)"
                    ))
                }

                for (j in 0 until 8) {
                    val expectedChar = expectedGene.getOrNull(j) ?: 'A'
                    val scaffoldChar = scaffoldStr.getOrNull(j) ?: '-'

                    val roll = java.util.Random().nextDouble() * 100.0
                    if (scaffoldChar == expectedChar && roll >= slotFailureChance) {
                        // Success
                    } else {
                        // Failed append (Sacrifice required)
                        var sacrificeSuccess = false
                        var logText = ""
                        
                        // 1. Try Targeted Sacrifice
                        var available = false
                        when (expectedChar) {
                            'A' -> if (tempRawA > 0) { tempRawA--; available = true }
                            'G' -> if (tempRawG > 0) { tempRawG--; available = true }
                            'T' -> if (tempRawT > 0) { tempRawT--; available = true }
                            'C' -> if (tempRawC > 0) { tempRawC--; available = true }
                        }
                        
                        if (available) {
                            sacrificeSuccess = true
                            logText = "  ➔ FAILED APPEND (pos ${j + 1}). Sacrificed raw $expectedChar base (A:$tempRawA, G:$tempRawG, T:$tempRawT, C:$tempRawC)"
                        } else {
                            // 2. Try Transmutation Sacrifice (requires 2 other bases)
                            val candidatesForDeduction = mutableListOf<Char>()
                            for (rep in 1..2) {
                                when {
                                    tempRawA > 0 -> { tempRawA--; candidatesForDeduction.add('A') }
                                    tempRawG > 0 -> { tempRawG--; candidatesForDeduction.add('G') }
                                    tempRawT > 0 -> { tempRawT--; candidatesForDeduction.add('T') }
                                    tempRawC > 0 -> { tempRawC--; candidatesForDeduction.add('C') }
                                }
                            }
                            
                            if (candidatesForDeduction.size == 2) {
                                sacrificeSuccess = true
                                logText = "  ➔ FAILED APPEND (pos ${j + 1}). Transmuted expected $expectedChar by sacrificing: ${candidatesForDeduction.joinToString(", ")}"
                            }
                        }
                        
                        if (sacrificeSuccess) {
                            stepLogs.add(StepLog(
                                second = minOf(7, i + 1),
                                text = logText
                            ))
                        } else {
                            // Depleted raw bases stockpile!
                            if (failedAtSecond == -1) {
                                failedAtSecond = i + 1
                                stepLogs.add(StepLog(
                                    second = i + 1,
                                    text = "  ➔ [FATAL] >> SPLICING PROTOCOL ABORTED: Raw nucleotide stockpile fully depleted."
                                ))
                            }
                        }
                        failureChance = maxOf(0.0, failureChance - 3.25)
                    }
                }

                if (failedAtSecond != -1) break
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

            if (failedAtSecond != -1) {
                _isReactorFrozen.value = false
                _isForcedConstructionActive.value = false
                _isForcedLoopActive.value = false
                _selectedTab.value = "splicer"
                synthManager.playReject()
                addLog("FORCED CONSTRUCTION FAILED: Raw nucleotide stockpile depleted!")
            } else {
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
                
                // Save updated raw stocks on success
                repository.saveRawStocks(tempRawA, tempRawG, tempRawT, tempRawC)

                val blocks = (0 until 8).map { dnaSeq.substring(it * 8, (it + 1) * 8) }
                val currentStock = geneSequences.value
                val qScoresList = blocks.map { seq ->
                    currentStock.find { it.sequence == seq }?.averageQScore ?: 25.0
                }
                val avgQ = qScoresList.average()
                val scaledTelomeres = (avgQ * 2.5).toInt().coerceIn(10, 100)

                val spawned = compileDeterministicOffline(dnaSeq)
                val scaledVitality = (spawned.vitality * (0.5 + 0.5 * (avgQ / 40.0))).toInt()
                val matchesTarget = getCoherence(dnaSeq, _targetSequence.value) == "full"
                val idSuffix = UUID.randomUUID().toString().take(6).uppercase()
                
                // Calculate CAI
                val cai = BiophysicsEngine.calculateCodonAdaptationIndex(dnaSeq, spawned.faction)
                
                val finalCreature = spawned.copy(
                    id = "PX-$idSuffix",
                    name = "${spawned.name} [FORCED]",
                    lore = "This hybrid genome was forced together in the bio-lab reactor. ${spawned.lore}",
                    origin = "Forced Synthesis",
                    isFullCoherence = matchesTarget,
                    coherenceType = if (matchesTarget) "Forced" else null,
                    codonAdaptationIndex = cai,
                    telomeres = scaledTelomeres,
                    vitality = scaledVitality
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
    private var ticksSinceLastSave = 0
 
    private fun startReactorHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.Default) {
            var siphonTick = 0
            while (true) {
                kotlinx.coroutines.delay(1000L)
                
                siphonTick++
                if (siphonTick >= 5) {
                    siphonTick = 0
                    // 0. Passive siphoning accumulation
                    val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
                    var incA = 1L
                    var incG = 1L
                    var incT = 1L
                    var incC = 1L
                    
                    if (!wave.isSuppressed) {
                        val p = wave.primary
                        val s = wave.secondary
                        val pm = wave.primaryMultiplier
                        val sm = wave.secondaryMultiplier
                        
                        if (p == "A") incA = Math.round(1.0 * pm)
                        if (p == "G") incG = Math.round(1.0 * pm)
                        if (p == "T") incT = Math.round(1.0 * pm)
                        if (p == "C") incC = Math.round(1.0 * pm)
                        
                        if (s == "A") incA = Math.round(1.0 * sm)
                        if (s == "G") incG = Math.round(1.0 * sm)
                        if (s == "T") incT = Math.round(1.0 * sm)
                        if (s == "C") incC = Math.round(1.0 * sm)
                    }
                    
                    val nextA = _rawStockA.value + incA
                    val nextG = _rawStockG.value + incG
                    val nextT = _rawStockT.value + incT
                    val nextC = _rawStockC.value + incC
                    _rawStockA.value = nextA
                    _rawStockG.value = nextG
                    _rawStockT.value = nextT
                    _rawStockC.value = nextC
                    
                    ticksSinceLastSave++
                    if (ticksSinceLastSave >= 10) {
                        ticksSinceLastSave = 0
                        repository.saveRawStocks(nextA, nextG, nextT, nextC)
                    }
                }
                
                
                if (!_isReactorFrozen.value) {
                    // Decrement active booster timers
                    if (_boostSecondsLeft.value > 0) {
                        _boostSecondsLeft.value -= 1
                        if (_boostSecondsLeft.value == 0) {
                            addLog("REACTOR BOOSTER EXPIRED: Splicing cycle returned to standard duration.")
                        }
                    }
                    
                    // 2. Tick Anomaly Engine if active (paused for testing)
                    if (_anomalyEngineActive.value) {
                        val poly = _activePolymerase.value.uppercase()
                        val baseCycle = when (poly) {
                            "TAQ" -> 8
                            "PFU" -> 32
                            "TTH" -> 16
                            else -> 16
                        }
                        val isBoostActive = _boostSecondsLeft.value > 0
                        val resetVal = if (isBoostActive) baseCycle / 2 else baseCycle

                        val currentAnomalyIdle = _anomalyIdleTime.value
                        var nextAnomalyVal = currentAnomalyIdle - 1
                        if (isBoostActive && currentAnomalyIdle > (baseCycle / 2)) {
                            nextAnomalyVal = baseCycle / 2
                        }
                        
                        if (nextAnomalyVal <= 0) {
                            if (rawStockA.value >= 2500 && rawStockG.value >= 2500 && 
                                rawStockT.value >= 2500 && rawStockC.value >= 2500) {
                                triggerAnomalousSynthesis()
                            } else {
                                _anomalyEngineActive.value = false
                                _anomalyConsumedBases.value = 0L
                                addLog("ANOMALY ENGINE SHUT DOWN: Out of raw nucleotides (requires 2,500 of each base) to sustain fusion.")
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
                                            phaseLogs.add("[TELEMETRY] Arrived at boundary. Initiating descent.")
                                        }
                                    }
                                    "DESCENT" -> {
                                        if (currentPhaseElapsed >= m.descentDuration) {
                                            nextPhase = "HARVESTING"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[TELEMETRY] Depth reached. Initiating extraction.")
                                        }
                                    }
                                    "HARVESTING" -> {
                                        if (currentPhaseElapsed >= m.harvestDuration) {
                                            nextPhase = "ASCENT"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[TELEMETRY] Extraction complete. Commencing ascent.")
                                        }
                                    }
                                    "ASCENT" -> {
                                        if (currentPhaseElapsed >= m.ascentDuration) {
                                            nextPhase = "TRANST_BACK"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[TELEMETRY] Boundary reached. Transitioning to transit.")
                                        }
                                    }
                                    "TRANST_BACK" -> {
                                        if (currentPhaseElapsed >= m.transitBackDuration) {
                                            nextPhase = "COMPLETED"
                                            nextPhaseElapsed = 0L
                                            phaseLogs.add("[COMPLETE] Payload returned.")
                                        }
                                    }
                                }

                                val isCompleted = nextPhase == "COMPLETED"
                                
                                val waveConfig = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
                                val phaseFraction = waveConfig.lunarAge / WaveMath.LUNAR_MONTH_DAYS
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

    private fun generateBiophysicalGeneBlock(
        wave: WaveMath.WaveConfig?,
        inletA: Float, inletG: Float, inletT: Float, inletC: Float,
        random: java.util.Random = java.util.Random()
    ): String {
        val bases = listOf("A", "G", "T", "C")
        val b1 = wave?.primary ?: "A"
        val b2 = wave?.secondary ?: "G"
        val m1 = wave?.primaryMultiplier ?: 1.0
        val m2 = wave?.secondaryMultiplier ?: 1.0
        val isSuppressed = wave?.isSuppressed ?: true

        var res = ""
        for (i in 0 until 8) {
            val prevChar = if (i > 0) res[i - 1].toString() else ""
            val weights = mutableMapOf("A" to 1.0, "G" to 1.0, "T" to 1.0, "C" to 1.0)
            
            if (!isSuppressed) {
                if (prevChar == b1) {
                    weights[b2] = m2
                } else {
                    weights[b1] = m1
                }
            }

            // Multiply by inlet ratio biases
            weights["A"] = (weights["A"] ?: 1.0) * inletA.toDouble()
            weights["G"] = (weights["G"] ?: 1.0) * inletG.toDouble()
            weights["T"] = (weights["T"] ?: 1.0) * inletT.toDouble()
            weights["C"] = (weights["C"] ?: 1.0) * inletC.toDouble()

            val sum = weights.values.sum()
            val r = random.nextDouble() * sum
            var acc = 0.0
            var selected = "A"
            for ((base, w) in weights) {
                acc += w
                if (r < acc) {
                    selected = base
                    break
                }
            }
            res += selected
        }
        return res
    }

    fun decomposeGeneBlock(sequence: String, countToDecompose: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val match = geneSequences.value.find { it.sequence == sequence } ?: return@launch
            val actualCount = Math.min(match.count, countToDecompose)
            if (actualCount <= 0) return@launch
            
            val newCount = match.count - actualCount
            if (newCount <= 0) {
                repository.deleteGeneSequence(match)
            } else {
                repository.updateGeneSequence(match.copy(count = newCount))
            }
            
            var localA = 0L
            var localG = 0L
            var localT = 0L
            var localC = 0L
            
            sequence.forEach { char ->
                when (char) {
                    'A' -> localA++
                    'G' -> localG++
                    'T' -> localT++
                    'C' -> localC++
                    else -> {
                        // Transmute anomalous characters into random standard bases
                        val roll = (Math.random() * 4).toInt()
                        when (roll) {
                            0 -> localA++
                            1 -> localG++
                            2 -> localT++
                            3 -> localC++
                        }
                    }
                }
            }
            
            val yieldA = localA * actualCount
            val yieldG = localG * actualCount
            val yieldT = localT * actualCount
            val yieldC = localC * actualCount
            
            val newA = _rawStockA.value + yieldA
            val newG = _rawStockG.value + yieldG
            val newT = _rawStockT.value + yieldT
            val newC = _rawStockC.value + yieldC
            _rawStockA.value = newA
            _rawStockG.value = newG
            _rawStockT.value = newT
            _rawStockC.value = newC
            repository.saveRawStocks(newA, newG, newT, newC)
            
            addLog("RECYCLED: Deconstructed $actualCount blocks of $sequence into raw bases: +$yieldA A, +$yieldG G, +$yieldT T, +$yieldC C.")
            synthManager.playCombinatorTick()
        }
    }

    fun cycleTargetBase(index: Int) {
        if (index !in 0..7) return
        val current = _targetSynthesisSequence.value
        val bases = listOf('A', 'G', 'T', 'C')
        val charArray = current.toCharArray()
        val currChar = charArray[index]
        val nextIdx = (bases.indexOf(currChar) + 1) % bases.size
        charArray[index] = bases[nextIdx]
        _targetSynthesisSequence.value = String(charArray)
        synthManager.playCombinatorTick()
    }

    fun initiateStandardSynthesis() {
        if (_isSynthesisActive.value) return
        
        viewModelScope.launch {
            val target = _targetSynthesisSequence.value
            val random = java.util.Random()
            val temp = _reactorTemperature.value
            val salt = _reactorSalt.value.toDouble()
            val poly = _activePolymerase.value
            val solute = _activeChemicalSolute.value
            
            // Count bases in target sequence
            var reqA = 0L
            var reqG = 0L
            var reqT = 0L
            var reqC = 0L
            target.forEach { char ->
                when (char) {
                    'A' -> reqA++
                    'G' -> reqG++
                    'T' -> reqT++
                    'C' -> reqC++
                }
            }
            
            // Add cofactor costs if active
            var soluteCost = 0L
            var actualSolute = solute
            if (actualSolute == "DMSO" || actualSolute == "Netropsin") {
                soluteCost = 100L
            }
            
            // Enzyme swap costs determined at start
            val enzymeBaseCost = when (poly.uppercase()) {
                "TTH" -> 25L
                "PFU" -> 50L
                else -> 0L
            }
            val enzymeWasteCost = when (poly.uppercase()) {
                "TTH" -> 50L
                "PFU" -> 150L
                else -> 0L
            }
            
            val totalReqA = reqA + soluteCost + enzymeBaseCost
            val totalReqG = reqG + soluteCost + enzymeBaseCost
            val totalReqT = reqT + soluteCost + enzymeBaseCost
            val totalReqC = reqC + soluteCost + enzymeBaseCost
            val currentWaste = _bioWaste.value
            
            val currentA = _rawStockA.value
            val currentG = _rawStockG.value
            val currentT = _rawStockT.value
            val currentC = _rawStockC.value
            
            if (currentA < totalReqA || currentG < totalReqG || currentT < totalReqT || currentC < totalReqC || currentWaste < enzymeWasteCost) {
                addLog("SYNTHESIS RUNTIME ERROR: Insufficient stocks for sequence, solutes, or enzyme swap cost ($poly requires $enzymeBaseCost bases & $enzymeWasteCost waste).")
                _isLoopActive.value = false
                synthManager.playReject()
                return@launch
            }
            
            // Calculate Stoichiometric Deviation and Q-score Penalty
            val targetLen = target.length.toDouble()
            val ratioA = target.count { it == 'A' }.toDouble() / targetLen
            val ratioG = target.count { it == 'G' }.toDouble() / targetLen
            val ratioT = target.count { it == 'T' }.toDouble() / targetLen
            val ratioC = target.count { it == 'C' }.toDouble() / targetLen

            val sumInlets = _inletRatioA.value + _inletRatioG.value + _inletRatioT.value + _inletRatioC.value
            val normI_A = if (sumInlets > 0) _inletRatioA.value / sumInlets else 0.25f
            val normI_G = if (sumInlets > 0) _inletRatioG.value / sumInlets else 0.25f
            val normI_T = if (sumInlets > 0) _inletRatioT.value / sumInlets else 0.25f
            val normI_C = if (sumInlets > 0) _inletRatioC.value / sumInlets else 0.25f

            val deviation = Math.abs(normI_A - ratioA) +
                            Math.abs(normI_G - ratioG) +
                            Math.abs(normI_T - ratioT) +
                            Math.abs(normI_C - ratioC)
            val penalty = (deviation * 15.0).coerceAtMost(15.0)

            // Lock reactor and deduct stocks
            _isSynthesisActive.value = true
            _isReactionCollapsed.value = false
            _activeSynthesisStep.value = 0
            _activeSynthesisStrand.value = ""
            _activeSynthesisQScore.value = 0.0
            
            val afterA = currentA - totalReqA
            val afterG = currentG - totalReqG
            val afterT = currentT - totalReqT
            val afterC = currentC - totalReqC
            val afterWaste = currentWaste - enzymeWasteCost
            
            _rawStockA.value = afterA
            _rawStockG.value = afterG
            _rawStockT.value = afterT
            _rawStockC.value = afterC
            _bioWaste.value = afterWaste
            repository.saveRawStocks(afterA, afterG, afterT, afterC)
            
            if (soluteCost > 0) {
                addLog("SOLUTE INJECTION: Consumed 100 units of each base for active chemical solute buffer ($actualSolute).")
            }
            if (enzymeBaseCost > 0 || enzymeWasteCost > 0) {
                addLog("ENZYME CHARGE: Consumed $enzymeBaseCost of all bases & $enzymeWasteCost Bio-Waste for $poly.")
            }
            
            addLog("REACTOR: Initializing stepped transcription of target sequence \"$target\"...")
            
            val baseCycle = when (poly.uppercase()) {
                "TAQ" -> 8
                "PFU" -> 32
                "TTH" -> 16
                else -> 16
            }
            val isBoostActive = _boostSecondsLeft.value > 0
            val cycleDuration = if (isBoostActive) baseCycle / 2 else baseCycle
            
            // Calculate interval per step in ms (minimum 100ms per step)
            val stepIntervalMs = ((cycleDuration * 1000L) / 8).coerceAtLeast(100L)
            
            var collapsed = false
            var finalStrand = ""
            
            for (step in 1..8) {
                kotlinx.coroutines.delay(stepIntervalMs)
                
                val expectedChar = target[step - 1]
                var incorporatedChar = expectedChar
                
                // Crosstalk Mutation Roll (proofreading bypassed if using PFU)
                if (!poly.equals("PFU", ignoreCase = true)) {
                    val inletRatio = when (expectedChar) {
                        'A' -> normI_A
                        'G' -> normI_G
                        'T' -> normI_T
                        'C' -> normI_C
                        else -> 1.0f
                    }
                    val roll = random.nextDouble()
                    if (roll > inletRatio) {
                        // Mutation triggered! Select a random base weighted by other inlets
                        val options = listOf('A', 'G', 'T', 'C').filter { it != expectedChar }
                        val otherWeights = options.map { o ->
                            when (o) {
                                'A' -> normI_A.toDouble()
                                'G' -> normI_G.toDouble()
                                'T' -> normI_T.toDouble()
                                'C' -> normI_C.toDouble()
                                else -> 0.0
                            }
                        }
                        val sumOthers = otherWeights.sum()
                        incorporatedChar = if (sumOthers > 0.0) {
                            val rOthers = random.nextDouble() * sumOthers
                            var accOthers = 0.0
                            var sel = options.first()
                            for (idx in options.indices) {
                                accOthers += otherWeights[idx]
                                if (rOthers < accOthers) {
                                    sel = options[idx]
                                    break
                                }
                            }
                            sel
                        } else {
                            options[random.nextInt(options.size)]
                        }
                        
                        addLog("CROSSTALK INTRUSION: Mismatched inlet ratios caused mutation at step $step! Incorporated $incorporatedChar instead of $expectedChar.")
                    }
                }
                
                finalStrand += incorporatedChar
                _activeSynthesisStrand.value = finalStrand
                _activeSynthesisStep.value = step
                
                // Biophysics calculations
                val tm = BiophysicsEngine.calculateMeltingTemperature(finalStrand, salt)
                val mfe = BiophysicsEngine.calculateMinimumFreeEnergy(finalStrand)
                _activeSynthesisTm.value = tm
                _activeSynthesisMfe.value = mfe
                
                // Phred Q Score tracking
                val baseQ = BiophysicsEngine.calculatePhredQScore(poly, random)
                val targetTmVal = BiophysicsEngine.calculateMeltingTemperature(target, salt)
                val diff = temp.toDouble() - targetTmVal
                val sigma = when (poly.uppercase()) {
                    "TAQ" -> 15.0
                    "PFU" -> 5.0
                    "TTH" -> 25.0
                    else -> 15.0
                }
                val eta = Math.exp(-(diff * diff) / (2.0 * sigma * sigma))
                
                val baseStepQ = (baseQ * eta - penalty).coerceIn(5.0, 40.0)
                val prevQ = _activeSynthesisQScore.value
                val newQ = if (step == 1) baseStepQ else (prevQ * (step - 1) + baseStepQ) / step
                _activeSynthesisQScore.value = newQ.coerceIn(5.0, 40.0)
                
                // Biophysical failure checks
                var failureTriggered = false
                var collapseReason = ""
                
                // Inlet flow deprivation check
                val inletVal = when (expectedChar) {
                    'A' -> _inletRatioA.value
                    'G' -> _inletRatioG.value
                    'T' -> _inletRatioT.value
                    'C' -> _inletRatioC.value
                    else -> 0f
                }
                if (inletVal <= 0.05f) {
                    failureTriggered = true
                    collapseReason = "inlet deprivation ($expectedChar flow restricted to ${String.format(java.util.Locale.US, "%.0f%%", inletVal * 100)})"
                }
                
                // GC Hairpin Stalling (Salt-dependent)
                val stallThreshold = 20.0f + (salt.toFloat() * 40.0f)
                val targetMfe = BiophysicsEngine.calculateMinimumFreeEnergy(target)
                if (!failureTriggered && finalStrand.length >= 4 && temp < stallThreshold && targetMfe <= -5.0 && actualSolute != "DMSO") {
                    failureTriggered = true
                    collapseReason = "stalling (GC hairpin secondary fold stabilized by salt at ${String.format(java.util.Locale.US, "%.1f", stallThreshold)}°C)"
                }
                
                // AT Denaturation (Salt-dependent)
                val denatureThreshold = 65.0f + (salt.toFloat() * 50.0f)
                val targetGcPercent = target.count { it == 'G' || it == 'C' }.toDouble() / target.length.toDouble()
                if (!failureTriggered && finalStrand.length >= 4 && temp > denatureThreshold && targetGcPercent < 0.40 && actualSolute != "Netropsin") {
                    failureTriggered = true
                    collapseReason = "denaturation (AT bonds denatured due to low salt stabilization at ${String.format(java.util.Locale.US, "%.1f", denatureThreshold)}°C)"
                }
                
                if (failureTriggered) {
                    collapsed = true
                    _isReactionCollapsed.value = true
                    _bioWaste.value += 8
                    addLog("CRITICAL COLLAPSE: Polymerase halted at step $step due to unstabilized $collapseReason. 8 raw bases converted to bio-waste.")
                    synthManager.playReject()
                    break
                }
            }
            
            if (!collapsed) {
                // Success: persist sequence to database
                val qScore = _activeSynthesisQScore.value
                val currentList = geneSequences.value
                val newGenesList = mutableListOf<String>()
                val toInsertOrUpdate = mutableListOf<GeneSequence>()
                
                val match = currentList.find { it.sequence == finalStrand }
                if (match != null) {
                    val newQ = (match.averageQScore * match.count + qScore) / (match.count + 1)
                    toInsertOrUpdate.add(match.copy(
                        count = match.count + 1,
                        averageQScore = newQ,
                        meltingTemp = _activeSynthesisTm.value,
                        minimumFreeEnergy = _activeSynthesisMfe.value
                    ))
                } else {
                    newGenesList.add(finalStrand)
                    toInsertOrUpdate.add(GeneSequence(
                        sequence = finalStrand,
                        count = 1,
                        discoveredAt = System.currentTimeMillis(),
                        averageQScore = qScore,
                        meltingTemp = _activeSynthesisTm.value,
                        minimumFreeEnergy = _activeSynthesisMfe.value
                    ))
                }
                
                repository.insertGeneSequences(toInsertOrUpdate)
                
                val packet = GenePacket(
                    id = "PKT-" + UUID.randomUUID().toString().take(6).uppercase(),
                    genes = listOf(finalStrand),
                    timestamp = System.currentTimeMillis(),
                    isAnomalous = false,
                    newGenes = newGenesList
                )
                _discoveredPacketsLog.update { current ->
                    (listOf(packet) + current).take(50)
                }
                
                addLog("REACTOR SYNTHESIS: Compiled gene block $finalStrand (Q-Score: ${String.format(java.util.Locale.US, "%.1f", qScore)}).")
                synthManager.playSynthesisSuccess()
            }
            
            // Unlock reactor
            _isSynthesisActive.value = false

            // Check remaining resources for current enzyme
            val remA = _rawStockA.value
            val remG = _rawStockG.value
            val remT = _rawStockT.value
            val remC = _rawStockC.value
            val remWaste = _bioWaste.value
            val currentEnz = _activePolymerase.value.uppercase()
            
            val costBase = when (currentEnz) {
                "TTH" -> 25L
                "PFU" -> 50L
                else -> 0L
            }
            val costWaste = when (currentEnz) {
                "TTH" -> 50L
                "PFU" -> 150L
                else -> 0L
            }
            
            if (remA < costBase || remG < costBase || remT < costBase || remC < costBase || remWaste < costWaste) {
                _activePolymerase.value = "Taq"
                addLog("SYS WARNING: Selected enzyme reset to Taq due to stock depletion.")
                synthManager.playReject()
            }
            
            // Loop synthesis handling
            if (_isLoopActive.value) {
                val nextEnz = _activePolymerase.value.uppercase()
                val nextEnzBaseCost = when (nextEnz) {
                    "TTH" -> 25L
                    "PFU" -> 50L
                    else -> 0L
                }
                val nextEnzWasteCost = when (nextEnz) {
                    "TTH" -> 50L
                    "PFU" -> 150L
                    else -> 0L
                }
                
                var nextReqA = 0L
                var nextReqG = 0L
                var nextReqT = 0L
                var nextReqC = 0L
                target.forEach { char ->
                    when (char) {
                        'A' -> nextReqA++
                        'G' -> nextReqG++
                        'T' -> nextReqT++
                        'C' -> nextReqC++
                    }
                }
                
                var nextSoluteCost = 0L
                val nextSolute = _activeChemicalSolute.value
                if (nextSolute == "DMSO" || nextSolute == "Netropsin") {
                    nextSoluteCost = 100L
                }
                
                val nextTotalA = nextReqA + nextSoluteCost + nextEnzBaseCost
                val nextTotalG = nextReqG + nextSoluteCost + nextEnzBaseCost
                val nextTotalT = nextReqT + nextSoluteCost + nextEnzBaseCost
                val nextTotalC = nextReqC + nextSoluteCost + nextEnzBaseCost
                
                val nextRemA = _rawStockA.value
                val nextRemG = _rawStockG.value
                val nextRemT = _rawStockT.value
                val nextRemC = _rawStockC.value
                val nextRemWaste = _bioWaste.value
                
                if (nextRemA >= nextTotalA && nextRemG >= nextTotalG && nextRemT >= nextTotalT && nextRemC >= nextTotalC && nextRemWaste >= nextEnzWasteCost) {
                    kotlinx.coroutines.delay(500L)
                    initiateStandardSynthesis()
                } else {
                    _isLoopActive.value = false
                    addLog("SYS: Synthesis loop halted due to insufficient feedstock/biowaste.")
                    synthManager.playReject()
                }
            }
        }
    }

    private suspend fun triggerAnomalousSynthesis() {
        val isDevMode = _devForceAnomaly.value
        var rollChance = 0.0
        val isSuccess = if (isDevMode) {
            true
        } else {
             // Consume 2,500 of each raw base from stocks (total 10,000 nucleotides)
             val currentA = (_rawStockA.value - 2500L).coerceAtLeast(0L)
             val currentG = (_rawStockG.value - 2500L).coerceAtLeast(0L)
             val currentT = (_rawStockT.value - 2500L).coerceAtLeast(0L)
             val currentC = (_rawStockC.value - 2500L).coerceAtLeast(0L)
             _rawStockA.value = currentA
             _rawStockG.value = currentG
             _rawStockT.value = currentT
             _rawStockC.value = currentC
             repository.saveRawStocks(currentA, currentG, currentT, currentC)
            
             _anomalyConsumedBases.value += 10000L // Increment the consumed bases in this run

             val currentEnz = _activePolymerase.value.uppercase()
             val costBase = when (currentEnz) {
                 "TTH" -> 25L
                 "PFU" -> 50L
                 else -> 0L
             }
             val costWaste = when (currentEnz) {
                 "TTH" -> 50L
                 "PFU" -> 150L
                 else -> 0L
             }
             if (currentA < costBase || currentG < costBase || currentT < costBase || currentC < costBase || _bioWaste.value < costWaste) {
                 _activePolymerase.value = "Taq"
                 addLog("SYS WARNING: Selected enzyme reset to Taq due to stock depletion from anomalous synthesis.")
             }

            val coupling = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
            // Calculate success chance based on cumulative bases consumed during this active run
            val chanceMetrics = WaveMath.getAnomalyEngineSuccessChance(_anomalyConsumedBases.value, coupling)
            rollChance = chanceMetrics.finalChance
            val roll = java.util.Random().nextDouble() * 100.0

            roll <= chanceMetrics.finalChance
        }

        val currentList = geneSequences.value
        val newGenesList = mutableListOf<String>()

        val generated = if (isSuccess) {
            val anomalousGene = WaveMath.generateAnomalousGene()
            
            // Calculate Tm, MFE, and Phred Q-score for the anomalous gene
            val tm = BiophysicsEngine.calculateMeltingTemperature(anomalousGene, _reactorSalt.value.toDouble())
            val mfe = BiophysicsEngine.calculateMinimumFreeEnergy(anomalousGene)
            val q = 40.0 // Anomalous fusion achieves perfect base call accuracy
            
            val match = currentList.find { it.sequence == anomalousGene }
            if (match != null) {
                repository.insertGeneSequence(match.copy(
                    count = match.count + 1,
                    averageQScore = (match.averageQScore * match.count + q) / (match.count + 1),
                    meltingTemp = tm,
                    minimumFreeEnergy = mfe
                ))
            } else {
                newGenesList.add(anomalousGene)
                repository.insertGeneSequence(GeneSequence(
                    sequence = anomalousGene, 
                    count = 1, 
                    discoveredAt = System.currentTimeMillis(),
                    averageQScore = q,
                    meltingTemp = tm,
                    minimumFreeEnergy = mfe
                ))
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
        _discoveredPacketsLog.update { current ->
            (listOf(packet) + current).take(50)
        }

        if (isSuccess) {
            if (isDevMode) {
                addLog("[DEV MODE FUSION] Force-triggered successfully (Zero resources consumed). Generated anomalous block $generated.")
            } else {
                addLog("[ANOMALY ENGINE] Unstable Fusion Success! Formed anomalous block $generated after consuming ${_anomalyConsumedBases.value} bases.")
            }
            synthManager.playSynthesisSuccess()
            _anomalyConsumedBases.value = 0L // Reset cumulative run counter on success

            // Auto-shutoff if remaining bases are below 250k activation threshold (active run is finished)
            val rawTotal = rawStockA.value + rawStockG.value + rawStockT.value + rawStockC.value
            if (rawTotal < 250000L) {
                _anomalyEngineActive.value = false
                addLog("[ANOMALY ENGINE] Engine disengaged: Success achieved. Nucleotide reserves are below 250k threshold for starting a new run.")
            }
        } else {
            val formattedChance = String.format(Locale.US, "%.2f%%", rollChance)
            addLog("[ANOMALY ENGINE] Fusion decay: 10,000 standard raw nucleotides decomposed in buffer (Success Chance: $formattedChance, Total Consumed: ${_anomalyConsumedBases.value} bases).")
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

data class BuildingBBox(val minLat: Double, val maxLat: Double, val minLng: Double, val maxLng: Double) {
    fun intersects(other: BuildingBBox): Boolean {
        return minLat <= other.maxLat && maxLat >= other.minLat &&
               minLng <= other.maxLng && maxLng >= other.minLng
    }
}

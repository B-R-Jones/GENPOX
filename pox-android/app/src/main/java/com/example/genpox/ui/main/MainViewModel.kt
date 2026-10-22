package com.example.genpox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.genpox.MainTab
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import com.example.genpox.data.location.LocationRepository
import com.example.genpox.data.network.PoxNetworkManager
import com.example.genpox.data.repository.*
import com.example.genpox.ui.components.MapProfilerState
import com.example.genpox.util.DispatchersProvider
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val creatureRepository: CreatureRepository,
    private val geneStockRepository: GeneStockRepository,
    private val missionRepository: MissionRepository,
    private val preferencesRepository: UserPreferencesRepository,
    val synthManager: PoxSynthManager,
    private val locationRepository: LocationRepository,
    val tileStreamManager: com.example.genpox.map.tile.TileStreamManager,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {

    // Feature ViewModels Instantiation
    val reactor = ReactorViewModel(geneStockRepository, preferencesRepository, creatureRepository, synthManager, dispatchers)
    val splicer = SplicerViewModel(geneStockRepository, preferencesRepository, creatureRepository, synthManager, dispatchers)
    val network = NetworkViewModel(creatureRepository, geneStockRepository, preferencesRepository, synthManager, dispatchers)
    val scanner = ScannerViewModel(
        missionRepository = missionRepository,
        creatureRepository = creatureRepository,
        preferencesRepository = preferencesRepository,
        geneStockRepository = geneStockRepository,
        synthManager = synthManager,
        locationRepository = locationRepository,
        tileStreamManager = tileStreamManager,
        dispatchers = dispatchers,
    )

    // Backing flows for unit test reflection access
    val _bioWaste get() = reactor._bioWaste
    val _targetSynthesisSequence get() = reactor._targetSynthesisSequence

    // Navigation and tab states
    private val _selectedTab = MutableStateFlow<MainTab>(MainTab.Combinator)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _activeCreature = MutableStateFlow<Creature?>(null)
    val activeCreature: StateFlow<Creature?> = _activeCreature.asStateFlow()

    private val _creatureCardOpenedFrom = MutableStateFlow<String?>("vault")
    val creatureCardOpenedFrom: StateFlow<String?> = _creatureCardOpenedFrom.asStateFlow()

    private val _defenderCreatureId = MutableStateFlow<String?>(null)
    val defenderCreatureId: StateFlow<String?> = _defenderCreatureId.asStateFlow()

    private val _disintegratedModal = MutableStateFlow<DisintegratedModalData?>(null)
    val disintegratedModal: StateFlow<DisintegratedModalData?> = _disintegratedModal.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(listOf("GENPOX COMPILER SYSTEM v2.0 READY."))
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _isLogWindowOpen = MutableStateFlow(false)
    val isLogWindowOpen: StateFlow<Boolean> = _isLogWindowOpen.asStateFlow()

    fun openLogWindow() {
        _isLogWindowOpen.value = true
    }

    fun closeLogWindow() {
        _isLogWindowOpen.value = false
    }

    fun toggleLogWindow() {
        _isLogWindowOpen.value = !_isLogWindowOpen.value
    }

    private val _logFlashTrigger = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val logFlashTrigger: SharedFlow<String> = _logFlashTrigger.asSharedFlow()
    private val lastMessageFlashTime = mutableMapOf<String, Long>()

    private val _isCompiling = MutableStateFlow(false)
    val isCompiling: StateFlow<Boolean> = _isCompiling.asStateFlow()

    private val _showScanner = MutableStateFlow(false)
    val showScanner: StateFlow<Boolean> = _showScanner.asStateFlow()

    private val _vaultSubTab = MutableStateFlow("creatures")
    val vaultSubTab: StateFlow<String> = _vaultSubTab.asStateFlow()

    private val _splicerSubTab = MutableStateFlow("splicer")
    val splicerSubTab: StateFlow<String> = _splicerSubTab.asStateFlow()

    private val _networkSubTab = MutableStateFlow("hydranet")
    val networkSubTab: StateFlow<String> = _networkSubTab.asStateFlow()

    private val _settingsSubTab = MutableStateFlow("general")
    val settingsSubTab: StateFlow<String> = _settingsSubTab.asStateFlow()

    // Bio-Lab Subtab Navigation State
    private val _bioLabSubTab = MutableStateFlow("pox")
    val bioLabSubTab: StateFlow<String> = _bioLabSubTab.asStateFlow()

    fun setBioLabSubTab(subTab: String) {
        _bioLabSubTab.value = subTab
    }

    // Local Node Name
    val localNodeName = "AGENT-NODE-417"

    // Profiler States
    private val _isProfilerEnabled = MutableStateFlow(false)
    val isProfilerEnabled: StateFlow<Boolean> = _isProfilerEnabled.asStateFlow()
    val profilerState = MapProfilerState()

    fun toggleProfiler() {
        _isProfilerEnabled.value = !_isProfilerEnabled.value
        addLog("PROFILER: Frame rate stats overlay set to: ${if (_isProfilerEnabled.value) "ACTIVE" else "OFFLINE"}")
    }

    // Global CRT Phosphor Monitor custom parameters
    private val _crtScanlineOpacity = MutableStateFlow(com.example.genpox.theme.CyberTheme.crtScanlineOpacity)
    val crtScanlineOpacity: StateFlow<Float> = _crtScanlineOpacity.asStateFlow()

    private val _crtGrilleOpacity = MutableStateFlow(com.example.genpox.theme.CyberTheme.crtGrilleOpacity)
    val crtGrilleOpacity: StateFlow<Float> = _crtGrilleOpacity.asStateFlow()

    private val _crtBloomIntensity = MutableStateFlow(com.example.genpox.theme.CyberTheme.crtBloomIntensity)
    val crtBloomIntensity: StateFlow<Float> = _crtBloomIntensity.asStateFlow()

    private val _crtCurvatureBezel = MutableStateFlow(com.example.genpox.theme.CyberTheme.crtCurvatureBezel)
    val crtCurvatureBezel: StateFlow<Float> = _crtCurvatureBezel.asStateFlow()

    private val _crtGlitchIntensity = MutableStateFlow(com.example.genpox.theme.CyberTheme.crtGlitchIntensity)
    val crtGlitchIntensity: StateFlow<Float> = _crtGlitchIntensity.asStateFlow()

    fun setCrtScanlineOpacity(value: Float) {
        _crtScanlineOpacity.value = value
        com.example.genpox.theme.CyberTheme.update { copy(crtScanlineOpacity = value) }
    }
    fun setCrtGrilleOpacity(value: Float) {
        _crtGrilleOpacity.value = value
        com.example.genpox.theme.CyberTheme.update { copy(crtGrilleOpacity = value) }
    }
    fun setCrtBloomIntensity(value: Float) {
        _crtBloomIntensity.value = value
        com.example.genpox.theme.CyberTheme.update { copy(crtBloomIntensity = value) }
    }
    fun setCrtCurvatureBezel(value: Float) {
        _crtCurvatureBezel.value = value
        com.example.genpox.theme.CyberTheme.update { copy(crtCurvatureBezel = value) }
    }
    fun setCrtGlitchIntensity(value: Float) {
        _crtGlitchIntensity.value = value
        com.example.genpox.theme.CyberTheme.update { copy(crtGlitchIntensity = value) }
    }

    fun syncPhosphorSettingsFromTheme() {
        _crtScanlineOpacity.value = com.example.genpox.theme.CyberTheme.crtScanlineOpacity
        _crtGrilleOpacity.value = com.example.genpox.theme.CyberTheme.crtGrilleOpacity
        _crtBloomIntensity.value = com.example.genpox.theme.CyberTheme.crtBloomIntensity
        _crtCurvatureBezel.value = com.example.genpox.theme.CyberTheme.crtCurvatureBezel
        _crtGlitchIntensity.value = com.example.genpox.theme.CyberTheme.crtGlitchIntensity
    }

    // Daily Bounty Sequences
    val dailyBounties: StateFlow<List<String>> = flow {
        emit(generateDailyBounties())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun generateDailyBounties(): List<String> {
        val list = mutableListOf<String>()
        val bases = listOf("A", "G", "T", "C")
        val random = java.util.Random(1337)
        for (i in 0 until 128) {
            var seq = ""
            for (j in 0 until 64) {
                seq += bases[random.nextInt(4)]
            }
            list.add(seq)
        }
        return list
    }

    // Gemini API keys and mute settings collected from DB
    val geminiApiKey: StateFlow<String> = preferencesRepository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val muteSound: StateFlow<Boolean> = preferencesRepository.muteSound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Observe database/repository stocks to initialize and keep in-sync
    val creatures: StateFlow<List<Creature>> = creatureRepository.allCreatures
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val geneSequences: StateFlow<List<GeneSequence>> = geneStockRepository.allGeneSequences
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryMetrics: StateFlow<DnaMetricsCalculator.GeneInventoryMetrics> = geneSequences.map { list ->
        DnaMetricsCalculator.calculateInventoryMetrics(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DnaMetricsCalculator.GeneInventoryMetrics())

    val grandTotalStandardNucleotides: StateFlow<Long> = inventoryMetrics
        .map { it.grandTotalStandardNucleotides }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _networkManager = MutableStateFlow<PoxNetworkManager?>(null)
    var networkManager: PoxNetworkManager?
        get() = _networkManager.value
        set(value) {
            _networkManager.value = value
            network.networkManager = value
        }

    // REACTOR VIEWMODEL DELEGATES
    val poxReactorActive get() = reactor.poxReactorActive
    val reactorTemperature get() = reactor.reactorTemperature
    val reactorSalt: StateFlow<Float> get() = reactor.reactorSalt
    val activePolymerase get() = reactor.activePolymerase
    val activeChemicalSolute get() = reactor.activeChemicalSolute
    val inletRatioA get() = reactor.inletRatioA
    val inletRatioG get() = reactor.inletRatioG
    val inletRatioT get() = reactor.inletRatioT
    val inletRatioC get() = reactor.inletRatioC
    val rawStockA get() = reactor.rawStockA
    val rawStockG get() = reactor.rawStockG
    val rawStockT get() = reactor.rawStockT
    val rawStockC get() = reactor.rawStockC
    val bioWaste get() = reactor.bioWaste
    val poxIdleTime get() = reactor.poxIdleTime
    val anomalyIdleTime get() = reactor.anomalyIdleTime
    val boostSecondsLeft get() = reactor.boostSecondsLeft
    val anomalyEngineActive get() = reactor.anomalyEngineActive
    val anomalyConsumedBases get() = reactor.anomalyConsumedBases
    val discoveredPacketsLog get() = reactor.discoveredPacketsLog
    val scrollingGene get() = reactor.scrollingGene
    val activeSynthesisStrand get() = reactor.activeSynthesisStrand
    val activeSynthesisStep get() = reactor.activeSynthesisStep
    val activeSynthesisQScore get() = reactor.activeSynthesisQScore
    val activeSynthesisTm get() = reactor.activeSynthesisTm
    val activeSynthesisMfe get() = reactor.activeSynthesisMfe
    val targetSynthesisSequence get() = reactor.targetSynthesisSequence
    val isSynthesisActive get() = reactor.isSynthesisActive
    val isReactionCollapsed get() = reactor.isReactionCollapsed
    val isLoopActive get() = reactor.isLoopActive
    val selectedReagent get() = reactor.selectedReagent
    val reagents get() = reactor.reagents
    val devForceAnomaly get() = reactor.devForceAnomaly
    val baseFonts get() = reactor.baseFonts
    val nearestFontDistance get() = reactor.nearestFontDistance
    val activeSiphonedFont get() = reactor.activeSiphonedFont
    val siphonedRate get() = reactor.siphonedRate
    val celestialWells get() = reactor.celestialWells
    val selectedHostPlanet get() = reactor.selectedHostPlanet

    // Orrery Time Controls
    private val _orreryTimeScaleIndex = MutableStateFlow(0)
    val orreryTimeScaleIndex: StateFlow<Int> = _orreryTimeScaleIndex.asStateFlow()

    fun setOrreryTimeScaleIndex(index: Int) {
        _orreryTimeScaleIndex.value = index
        val scale = when (index) {
            0 -> 1.0
            1 -> 10.0
            2 -> 100.0
            3 -> 1000.0
            4 -> 10000.0
            else -> 1.0
        }
        WaveMath.setScale(scale)
        addLog("ORRERY: Timescale set to ${scale.toInt()}x")
    }

    fun rewindOrreryTime() {
        viewModelScope.launch {
            val startOffset = WaveMath.manualOffsetMs
            if (startOffset == 0L && WaveMath.timeScale == 1.0) {
                synthManager.playBeep(300f, 0.05f, "sine")
                addLog("ORRERY: Chronological alignment already at real-time zero")
                return@launch
            }

            addLog("ORRERY: Initiating chronological rewind sequence...")
            _orreryTimeScaleIndex.value = 0
            WaveMath.timeScale = 1.0

            val durationMs = 1500L
            val stepDelay = 16L
            val steps = durationMs / stepDelay

            for (i in 1..steps) {
                val progress = i.toDouble() / steps.toDouble()
                val easeOutProgress = 1.0 - Math.pow(1.0 - progress, 2.0)
                val currentOffset = startOffset - (startOffset * easeOutProgress).toLong()
                WaveMath.manualOffsetMs = currentOffset

                if (i % 8L == 0L) {
                    val freq = 180f + (1.0f - easeOutProgress.toFloat()) * 420f
                    synthManager.playBeep(freq, 0.04f, "sine")
                }

                kotlinx.coroutines.delay(stepDelay)
            }

            WaveMath.manualOffsetMs = 0L
            addLog("ORRERY: Temporal alignment restored to standard epoch.")
            synthManager.playBeep(220f, 0.15f, "sine")
        }
    }

    fun setReactorTemperature(value: Float) = reactor.setReactorTemperature(value)
    fun setReactorSalt(value: Float) = reactor.setReactorSalt(value)
    fun setActivePolymerase(value: String) = reactor.setActivePolymerase(value)
    fun setActiveChemicalSolute(value: String) = reactor.setActiveChemicalSolute(value)
    fun setInletRatioA(value: Float) = reactor.setInletRatioA(value)
    fun setInletRatioG(value: Float) = reactor.setInletRatioG(value)
    fun setInletRatioT(value: Float) = reactor.setInletRatioT(value)
    fun setInletRatioC(value: Float) = reactor.setInletRatioC(value)
    fun setReactorDialIndex(index: Int) = reactor.setReactorDialIndex(index)
    fun toggleLoopActive() = reactor.toggleLoopActive()
    fun changeHostPlanet(planet: String) = reactor.changeHostPlanet(planet)
    fun selectReagent(reagentId: String?) = reactor.selectReagent(reagentId)
    fun toggleDevForceAnomaly() = reactor.toggleDevForceAnomaly()
    fun addDevBases() = reactor.addDevBases()
    fun clearDevBases() = reactor.clearDevBases()
    fun clearDevGenes() = reactor.clearDevGenes()
    fun clearDevCreatures() = reactor.clearDevCreatures()
    fun cycleTargetBase(index: Int) = reactor.cycleTargetBase(index)
    fun setTargetSynthesisSequence(sequence: String) = reactor.setTargetSynthesisSequence(sequence)
    fun recycleMatchingGenes(prefix: String) = reactor.recycleMatchingGenes(prefix)
    fun decomposeGeneBlock(sequence: String, countToDecompose: Int) = reactor.decomposeGeneBlock(sequence, countToDecompose)
    fun initiateStandardSynthesis() = reactor.initiateStandardSynthesis()
    fun setPoxReactorActive(active: Boolean) = reactor.setPoxReactorActive(active)
    fun setAnomalyEngineActive(active: Boolean) = reactor.setAnomalyEngineActive(active)
    fun triggerManualAcceleration() = reactor.triggerManualAcceleration()
    fun addBoosterTime(seconds: Int) = reactor.addBoosterTime(seconds)
    fun clearDiscoveredPacketsLog() = reactor.clearDiscoveredPacketsLog()

    // SPLICER VIEWMODEL DELEGATES
    val targetSequence get() = splicer.targetSequence
    val splicerSlots get() = splicer.splicerSlots
    val splicerQScores get() = splicer.splicerQScores
    val splicerSignatures get() = splicer.splicerSignatures
    val activeSlotSelection get() = splicer.activeSlotSelection
    val slotSequenceFilter get() = splicer.slotSequenceFilter
    val isReactorFrozen get() = splicer.isReactorFrozen
    val reactorFreezeTimeLeft get() = splicer.reactorFreezeTimeLeft
    val isForcedConstructionActive get() = splicer.isForcedConstructionActive
    val isForcedLoopActive get() = splicer.isForcedLoopActive
    val forcedConstructionLogs get() = splicer.forcedConstructionLogs
    val isSplicing get() = splicer.isSplicing
    val splicingProgress get() = splicer.splicingProgress
    val sequencedBasesCount get() = splicer.sequencedBasesCount
    val newlySplicedCreature get() = splicer.newlySplicedCreature
    val isCustomScaffoldMode get() = splicer.isCustomScaffoldMode
    val selectedSplicerCreature get() = splicer.selectedSplicerCreature
    val selectedGeneToAppend get() = splicer.selectedGeneToAppend

    fun selectSplicerSlot(idx: Int?) = splicer.selectSplicerSlot(idx)
    fun setCustomScaffoldMode(active: Boolean) = splicer.setCustomScaffoldMode(active)
    fun setSlotSequenceFilter(query: String) = splicer.setSlotSequenceFilter(query)
    fun assignGeneToSlot(seq: String) = splicer.assignGeneToSlot(seq)
    fun ejectGeneFromSlot(slotIdx: Int) = splicer.ejectGeneFromSlot(slotIdx)
    fun autofillSplicerSlots() = splicer.autofillSplicerSlots()
    fun devInjectMissingTargetGenes() = splicer.devInjectMissingTargetGenes()
    fun setSplicerCreature(creature: Creature?) = splicer.setSplicerCreature(creature)
    fun selectGeneToAppend(gene: String?) = splicer.selectGeneToAppend(gene)
    fun weldGeneToCreature() = splicer.weldGeneToCreature()
    fun constructSplicedCreature() = splicer.constructSplicedCreature(selectedReagent.value)
    fun completeSplicingAndViewInVault() = splicer.completeSplicingAndViewInVault()
    fun completeSplicingAndReturnToSplicer() = splicer.completeSplicingAndReturnToSplicer()
    fun setIsForcedLoopActive(active: Boolean) = splicer.setIsForcedLoopActive(active)
    fun startForcedConstruction(customSequence: String? = null) = splicer.startForcedConstruction(customSequence, selectedReagent.value)

    // NETWORK VIEWMODEL DELEGATES
    val inboxMessages get() = network.inboxMessages
    val selectedInboxMessageId get() = network.selectedInboxMessageId
    val registeredPeers get() = network.registeredPeers
    val isNetworkSimulationActive get() = network.isNetworkSimulationActive
    val composerRecipientId get() = network.composerRecipientId
    val composerRecipientName get() = network.composerRecipientName
    val composerRecipientIsConnected get() = network.composerRecipientIsConnected
    val composerMessageText get() = network.composerMessageText
    val composerAttachedCreature get() = network.composerAttachedCreature
    val composerAttachedGene get() = network.composerAttachedGene
    val composerTransferResources get() = network.composerTransferResources

    fun selectInboxMessage(id: String?) = network.selectInboxMessage(id)
    fun toggleNetworkSimulation(enabled: Boolean) = network.toggleNetworkSimulation(enabled)
    fun registerPeerNode(name: String, sector: String) = network.registerPeerNode(name, sector)
    fun markMessageAsRead(messageId: String) = network.markMessageAsRead(messageId)
    fun claimMessageAttachment(messageId: String) = network.claimMessageAttachment(messageId)
    fun transmitP2PMessage(endpointId: String, text: String, creature: Creature?, gene: GeneSequence?, transferResources: Boolean) = network.transmitP2PMessage(endpointId, text, creature, gene, transferResources)
    fun setComposerRecipient(id: String?, name: String?, isConnected: Boolean) = network.setComposerRecipient(id, name, isConnected)
    fun setComposerMessageText(text: String) = network.setComposerMessageText(text)
    fun setComposerAttachedCreature(creature: Creature?) = network.setComposerAttachedCreature(creature)
    fun setComposerAttachedGene(gene: GeneSequence?) = network.setComposerAttachedGene(gene)
    fun setComposerTransferResources(active: Boolean) = network.setComposerTransferResources(active)
    fun clearComposer() = network.clearComposer()

    // SCANNER VIEWMODEL DELEGATES
    val activeMissions: StateFlow<List<HarvestMission>> get() = scanner.activeMissions
    val harvestingCreatureIds: StateFlow<Set<String>> get() = scanner.harvestingCreatureIds
    val activeMissionCoords: StateFlow<Set<String>> get() = scanner.activeMissionCoords
    val allMissions: StateFlow<List<HarvestMission>> get() = scanner.allMissions
    val depletedAnomalyCoords: StateFlow<Set<String>> get() = scanner.depletedAnomalyCoords
    val scanRadius: StateFlow<Float> get() = scanner.scanRadius
    val trackedMissionId: StateFlow<String?> get() = scanner.trackedMissionId
    val selectedAnomalyId: StateFlow<String?> get() = scanner.selectedAnomalyId
    val scannerSubTab: StateFlow<String> get() = scanner.scannerSubTab
    val listSubTab: StateFlow<String> get() = scanner.listSubTab
    val showAnomaliesOnMap: StateFlow<Boolean> get() = scanner.showAnomaliesOnMap
    val showFontsOnMap: StateFlow<Boolean> get() = scanner.showFontsOnMap
    val showWellsOnMap: StateFlow<Boolean> get() = scanner.showWellsOnMap

    fun toggleAnomaliesOnMap() = scanner.toggleAnomaliesOnMap()
    fun toggleFontsOnMap() = scanner.toggleFontsOnMap()
    fun toggleWellsOnMap() = scanner.toggleWellsOnMap()
    val latitude: StateFlow<Double> get() = scanner.latitude
    val longitude: StateFlow<Double> get() = scanner.longitude
    val zoomMultiplier: StateFlow<Float> get() = scanner.zoomMultiplier

    val proceduralHeatwaves: StateFlow<List<String>> get() = scanner.proceduralHeatwaves
    val rawAnomalies: StateFlow<List<PoxAnomaly>> get() = scanner.rawAnomalies
    val anomalies: StateFlow<List<PoxAnomaly>> get() = scanner.anomalies
    val selectedAnomaly: StateFlow<PoxAnomaly?> get() = scanner.selectedAnomaly
    val heatwaveCells: StateFlow<List<String>> get() = scanner.proceduralHeatwaves

    fun setTrackedMissionId(id: String?) {
        scanner.setTrackedMissionId(id)
        if (id != null) {
            _selectedTab.value = MainTab.Scanner
        }
    }
    fun setSelectedAnomalyId(id: String?) = scanner.setSelectedAnomalyId(id)
    fun setScannerSubTab(subTab: String) = scanner.setScannerSubTab(subTab)
    fun setListSubTab(subTab: String) = scanner.setListSubTab(subTab)
    fun updateLocation(lat: Double, lng: Double) {
        reactor.updateLocation(lat, lng)
        scanner.updateLocation(lat, lng)
    }
    fun updateZoom(zoom: Float) = scanner.updateZoom(zoom)
    fun updateCameraPan(panWorldX: Float, panWorldY: Float) = scanner.updateCameraPan(panWorldX, panWorldY)
    fun refreshMap() = scanner.refreshMap()
    fun dispatchMission(creature: Creature, anomaly: PoxAnomaly) = scanner.dispatchMission(creature, anomaly)
    fun recallMission(mission: HarvestMission) = scanner.recallMission(mission)
    fun recallAllActiveMissions() = scanner.recallAllActiveMissions()
    fun getSynodicResonanceMod(faction: String, phaseName: String): Int = scanner.getSynodicResonanceMod(faction, phaseName)

    // Distance computation delegate helper
    fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = GeoMath.calculateHaversineDistanceInFeet(lat1, lng1, lat2, lng2)

    // Vault, Splicer, Network, Settings Tab setters
    fun setVaultSubTab(subTab: String) {
        _vaultSubTab.value = subTab
        synthManager.playBeep(if (subTab == "creatures") 440f else 587f, 0.05f, "sine")
    }

    fun setSplicerSubTab(subTab: String) {
        _splicerSubTab.value = subTab
        synthManager.playBeep(if (subTab == "splicer") 440f else 587f, 0.05f, "sine")
    }

    fun setNetworkSubTab(subTab: String) {
        _networkSubTab.value = subTab
    }

    fun setSettingsSubTab(subTab: String) {
        _settingsSubTab.value = subTab
    }

    fun clearDisintegratedModal() {
        _disintegratedModal.value = null
    }

    init {
        // Tie logs together
        reactor.onLog = { addLog(it) }
        splicer.onLog = { addLog(it) }
        splicer.onSetActiveCreature = { creature, openedFrom -> setActiveCreature(creature, openedFrom) }
        splicer.onSelectTab = { selectTabByRoute(it) }
        network.onLog = { addLog(it) }
        scanner.onLog = { addLog(it) }

        splicer.devForceAnomalyState = reactor.devForceAnomaly
        splicer.onResetSelectedReagent = { reactor.selectReagent(null) }

        // Collect incoming P2P packets
        viewModelScope.launch {
            _networkManager.collectLatest { nm ->
                nm?.incomingPackets?.collect { pair ->
                    val endpointId = pair.first
                    val packet = pair.second
                    val peerName = nm.activeConnections.value[endpointId] ?: "Unknown Peer"
                    network.receiveIncomingPacket(endpointId, peerName, packet)

                    val now = System.currentTimeMillis()
                    val lastFlash = lastMessageFlashTime[peerName] ?: 0L
                    if (now - lastFlash >= 5 * 60 * 1000L) {
                        lastMessageFlashTime[peerName] = now
                        _logFlashTrigger.emit(peerName)
                        addLog("message received from $peerName")
                    }
                }
            }
        }

        // Auto-switch to search tab if no creatures exist on first load
        var hasAutoDirectedToSearch = false
        viewModelScope.launch {
            creatures.collect { list ->
                if (!hasAutoDirectedToSearch && list.isEmpty()) {
                    _vaultSubTab.value = "search"
                    hasAutoDirectedToSearch = true
                }
            }
        }

        // Synchronize mute sound changes to synthesis manager
        viewModelScope.launch {
            muteSound.collect { mute ->
                synthManager.isMuted = mute
            }
        }
    }

    fun selectTab(tab: MainTab) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            addLog("NAV: Transition to section [${tab.route.uppercase()}]")
            synthManager.playCombinatorTick()

            if (tab is MainTab.Splicer) {
                if (splicer.isCustomScaffoldMode.value) {
                    splicer.selectSplicerSlot(null)
                }
                if (_vaultSubTab.value == "search") {
                    _vaultSubTab.value = "creatures"
                    _creatureCardOpenedFrom.value = "vault"
                }
            }
        }
    }

    @JvmName("selectTabByRouteString")
    fun selectTab(route: String) {
        selectTab(MainTab.fromRoute(route))
    }

    fun selectTabByRoute(route: String) {
        selectTab(MainTab.fromRoute(route))
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
            creatureRepository.insertCreature(updated)
            _activeCreature.value = updated
            synthManager.playCombinatorTick()
            addLog(if (updated.isFavorite) "FAV: Added \"${creature.name}\" to favorites." else "FAV: Removed \"${creature.name}\" from favorites.")
        }
    }

    fun toggleAutoHackerCreature(creature: Creature) {
        viewModelScope.launch {
            val updated = creature.copy(isAutoHacker = !creature.isAutoHacker)
            creatureRepository.insertCreature(updated)
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
            creatureRepository.deleteCreature(creature)
            val tm = GeneticsEngine.calculateMeltingTemperature(harvestedGene, reactorSalt.value.toDouble())
            val mfe = GeneticsEngine.calculateMinimumFreeEnergy(harvestedGene)
            val qScore = (creature.telomeres / 2.5).coerceIn(10.0, 45.0)
            geneStockRepository.insertGeneSequence(
                GeneSequence(
                    sequence = harvestedGene,
                    count = 1,
                    discoveredAt = System.currentTimeMillis(),
                    averageQScore = qScore,
                    meltingTemp = tm,
                    minimumFreeEnergy = mfe,
                ),
            )
            addLog("PURGE: Purged specimen \"${creature.name}\". Segment cached in stock: $harvestedGene")
            synthManager.playBeep(400f, 0.4f, "sawtooth")
            setActiveCreature(null)
        }
    }

    fun decreaseCreatureTelomeres(creatureId: String, amount: Int, reason: String) {
        viewModelScope.launch {
            val creature = creatureRepository.getCreatureById(creatureId) ?: return@launch
            val nextT = (creature.telomeres - amount).coerceAtLeast(0)
            if (nextT <= 0) {
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

                val returnCount = (blocks.size + 1) / 2
                val shuffled = blocks.shuffled()
                val returnedBlocks = shuffled.take(returnCount)
                val destroyedBlocks = shuffled.drop(returnCount)

                returnedBlocks.forEach { seq ->
                    val upper = seq.uppercase()
                    val tm = GeneticsEngine.calculateMeltingTemperature(upper, reactorSalt.value.toDouble())
                    val mfe = GeneticsEngine.calculateMinimumFreeEnergy(upper)
                    val qScore = (creature.telomeres / 2.5).coerceIn(10.0, 45.0)
                    geneStockRepository.insertGeneSequence(
                        GeneSequence(
                            sequence = upper,
                            count = 1,
                            discoveredAt = System.currentTimeMillis(),
                            averageQScore = qScore,
                            meltingTemp = tm,
                            minimumFreeEnergy = mfe,
                        ),
                    )
                }

                creatureRepository.deleteCreature(creature)
                if (_activeCreature.value?.id == creatureId) {
                    _activeCreature.value = null
                }
                if (_defenderCreatureId.value == creatureId) {
                    _defenderCreatureId.value = null
                }

                _disintegratedModal.value = DisintegratedModalData(
                    name = creature.name,
                    returnedBlocks = returnedBlocks,
                    destroyedBlocks = destroyedBlocks,
                )

                addLog("CHROMOSOMAL FAILURE: \"${creature.name}\" disintegrated! Telomeres reached 0%. Returned ${returnedBlocks.size} gene blocks, lost ${destroyedBlocks.size} permanently!")
                synthManager.playReject()
            } else {
                val updated = creature.copy(telomeres = nextT)
                creatureRepository.insertCreature(updated)
                if (_activeCreature.value?.id == creatureId) {
                    _activeCreature.value = updated
                }
                addLog("CELLULAR INTEGRITY: \"${creature.name}\" telomeres shortened by -$amount% due to $reason. [Current life: $nextT%]")
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

            val weldedGeneQ = match.averageQScore
            geneStockRepository.deleteGeneSequence(match)

            val newSeq = creature.sequence + gene
            val newAppended = creature.appendedGenes + gene
            val details = constructProceduralDetails(newSeq)

            val prevAvgQ = creature.telomeres / 2.5
            val prevBlocks = creature.sequence.length / 8
            val avgQ = (prevAvgQ * prevBlocks + weldedGeneQ) / (prevBlocks + 1)

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
                primaryWeapon = details.primaryWeapon,
            )
            creatureRepository.insertCreature(updatedCreature)

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
        if (maxB == countA) {
            faction = "Containment"
        } else if (maxB == countG) {
            faction = "Infection"
        } else if (maxB == countT) {
            faction = "Mech"
        } else if (maxB == countC) {
            faction = "Parasite"
        }

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
            lore = lore,
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
        val lore: String,
    )

    fun getUnlockedMoves(sequence: String): List<UnlockedMove> {
        val list = mutableListOf<UnlockedMove>()
        if (sequence.length <= 64) return list

        if (sequence.length >= 72) {
            val gene1 = sequence.substring(64, 72)
            var ga = 0
            var tc = 0
            for (char in gene1.uppercase()) {
                if (char == 'A' || char == 'G') {
                    ga++
                } else if (char == 'T' || char == 'C') {
                    tc++
                }
            }
            if (ga >= tc) {
                list.add(
                    UnlockedMove(
                        type = "healing",
                        name = "BIO-DRAIN REPAIR",
                        description = "Repairs circuitry by siphoning 35 HP from defender.",
                    ),
                )
            } else {
                list.add(
                    UnlockedMove(
                        type = "evasive",
                        name = "QUANTUM ESCAPE DEVIATION",
                        description = "Evasive shift. Negates next attack damage in this round.",
                    ),
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
                        description = "Hardened shield. Negates defender damage for this round.",
                    ),
                )
            } else {
                list.add(
                    UnlockedMove(
                        type = "healing",
                        name = "MICRO-PHAGE EXTRACTION",
                        description = "Siphons 35 HP from opponent to mend system hardware.",
                    ),
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
            put(
                "visual",
                buildJsonObject {
                    put("faction", creature.faction)
                    put("type", creature.type)
                    put("ascii", creature.asciiArt)
                },
            )
            put(
                "sound",
                buildJsonObject {
                    put("oscillator", sound.oscillator)
                    put("frequencies", JsonArray(sound.frequencies.map { JsonPrimitive(it) }))
                    put("noteDelay", sound.noteDelay)
                    put("noteDuration", sound.noteDuration)
                },
            )
            put("telomeres", creature.telomeres)
            put("isFullCoherence", creature.isFullCoherence)
            if (creature.coherenceType != null) {
                put("coherenceType", creature.coherenceType)
            }
        }
        val jsonStr = compact.toString()
        return android.util.Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
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
            preferencesRepository.setMuteSound(mute)
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.saveGeminiApiKey(key)
            addLog("SYS: Secure Gemini API Credentials Saved.")
            synthManager.playSynthesisSuccess()
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
                creatureRepository.insertCreature(transferredCreature)
                addLog("SCAN: Compiled creature \"${creature.name}\" [Telomeres: 99%]")
                setActiveCreature(transferredCreature, "scanner")
                _selectedTab.value = MainTab.Vault
            } else if (data.length == 64 && data.matches(Regex("[AGTCagtc]+"))) {
                addLog("SCAN: Scanned raw DNA sequence. Compiling...")
                compileCreature(data)
            } else if (data.length == 8 && data.all { it.uppercaseChar() in "AGTCXZYW?!$%&@#" }) {
                val upper = data.uppercase()
                val tm = GeneticsEngine.calculateMeltingTemperature(upper, reactorSalt.value.toDouble())
                val mfe = GeneticsEngine.calculateMinimumFreeEnergy(upper)
                geneStockRepository.insertGeneSequence(
                    GeneSequence(
                        sequence = upper,
                        count = 1,
                        discoveredAt = System.currentTimeMillis(),
                        averageQScore = 30.0,
                        meltingTemp = tm,
                        minimumFreeEnergy = mfe,
                    ),
                )
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
                coherenceType = coherenceType,
            )
        } catch (e: Exception) {
            null
        }
    }

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
            val matchesTarget = getCoherence(sequence, targetSequence.value) == "full"
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
                    coherenceType = if (matchesTarget) "Natural" else null,
                )
            }

            if (creature != null) {
                creatureRepository.insertCreature(creature)
                addLog("OK: Compiled creature \"${creature.name}\" of Sector [${creature.faction}]")
                synthManager.playSynthesisSuccess()
                setActiveCreature(creature, "combinator")
                _selectedTab.value = MainTab.Vault
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
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = config,
                systemInstruction = content {
                    text("You are an expert compiler of retro sci-fi cyber-creatures. You output structured bios for bio-mechanical warriors, parasitic battle bugs, and cyber virus creatures in valid JSON.")
                },
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
                origin = "Compiled via AI Engine",
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compileDeterministicOffline(sequence: String, reagent: String? = null): Creature {
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

        var aCountFaction = aCount
        var gCountFaction = gCount
        var tCountFaction = tCount
        var cCountFaction = cCount

        when (reagent) {
            "REAGENT_ALIGN_CONTAINMENT" -> cCountFaction += 15
            "REAGENT_ALIGN_INFECTION" -> aCountFaction += 15
            "REAGENT_ALIGN_MECH" -> gCountFaction += 15
            "REAGENT_ALIGN_PARASITE" -> tCountFaction += 15
        }

        val faction = when (maxOf(aCountFaction, gCountFaction, tCountFaction, cCountFaction)) {
            aCountFaction -> "Infection"
            gCountFaction -> "Mech"
            tCountFaction -> "Parasite"
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
            origin = "Local Compiled Unit",
        )
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

class MainViewModelFactory(
    private val repository: DataRepository,
    private val synthManager: PoxSynthManager,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val dispatchers = com.example.genpox.util.DefaultDispatchersProvider()
            val locationDataSource = com.example.genpox.data.location.DefaultLocationDataSource(repository as Any as android.content.Context)
            val locationRepository = com.example.genpox.data.location.DefaultLocationRepository(locationDataSource)
            val tileDataProvider = com.example.genpox.map.tile.VectorTileRepository(repository as Any as android.content.Context, dispatchers)
            val tileStreamManager = com.example.genpox.map.tile.TileStreamManager(tileDataProvider, dispatchers)
            return MainViewModel(
                creatureRepository = repository,
                geneStockRepository = repository,
                missionRepository = repository,
                preferencesRepository = repository,
                synthManager = synthManager,
                locationRepository = locationRepository,
                tileStreamManager = tileStreamManager,
                dispatchers = dispatchers,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class GenePacket(
    val id: String,
    val genes: List<String>,
    val timestamp: Long,
    val isAnomalous: Boolean,
    val newGenes: List<String> = emptyList(),
)

data class UnlockedMove(
    val type: String,
    val name: String,
    val description: String,
)

data class EmotSound(
    val oscillator: String,
    val frequencies: List<Int>,
    val noteDelay: Double,
    val noteDuration: Double,
)

data class DisintegratedModalData(
    val name: String,
    val returnedBlocks: List<String>,
    val destroyedBlocks: List<String>,
)

data class BuildingBBox(val minLat: Double, val maxLat: Double, val minLng: Double, val maxLng: Double) {
    fun intersects(other: BuildingBBox): Boolean = minLat <= other.maxLat &&
        maxLat >= other.minLat &&
        minLng <= other.maxLng &&
        maxLng >= other.minLng
}

data class PoxBaseFont(
    val id: String,
    val lat: Double,
    val lng: Double,
    val baseType: Char, // 'A', 'G', 'T', 'C'
    val spawnTime: Long,
    val durationMs: Long = 90000L, // 90 seconds
    val maxRadiusFeet: Double = 500.0,
    val isInverted: Boolean = false,
) {
    fun isExpired(now: Long): Boolean = now - spawnTime >= durationMs

    fun getGatherAmount(distanceFeet: Double): Long {
        if (distanceFeet > maxRadiusFeet) return 0L
        val maxGather = 40.0
        val ratio = 1.0 - (distanceFeet / maxRadiusFeet)
        return Math.round(maxGather * Math.pow(ratio, 4.0)).coerceAtLeast(0L)
    }
}

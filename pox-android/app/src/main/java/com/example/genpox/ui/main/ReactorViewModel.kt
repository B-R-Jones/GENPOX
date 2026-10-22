package com.example.genpox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import com.example.genpox.data.repository.*
import com.example.genpox.util.DispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReactorViewModel @Inject constructor(
    private val geneStockRepository: GeneStockRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val creatureRepository: CreatureRepository,
    private val synthManager: PoxSynthManager,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    val _bioWaste = MutableStateFlow(0L)
    val _targetSynthesisSequence = MutableStateFlow("GCAATAGC")

    // Log callback to post back to MainViewModel
    var onLog: ((String) -> Unit)? = null

    private fun addLog(message: String) {
        onLog?.invoke(message)
    }

    // State Variables
    private val _poxReactorActive = MutableStateFlow(true)
    val poxReactorActive: StateFlow<Boolean> = _poxReactorActive.asStateFlow()

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

    private val _rawStockA = MutableStateFlow(10000L)
    val rawStockA: StateFlow<Long> = _rawStockA.asStateFlow()

    private val _rawStockG = MutableStateFlow(10000L)
    val rawStockG: StateFlow<Long> = _rawStockG.asStateFlow()

    private val _rawStockT = MutableStateFlow(10000L)
    val rawStockT: StateFlow<Long> = _rawStockT.asStateFlow()

    private val _rawStockC = MutableStateFlow(10000L)
    val rawStockC: StateFlow<Long> = _rawStockC.asStateFlow()

    val bioWaste: StateFlow<Long> = _bioWaste.asStateFlow()

    private val _poxIdleTime = MutableStateFlow(12)
    val poxIdleTime: StateFlow<Int> = _poxIdleTime.asStateFlow()

    private val _anomalyIdleTime = MutableStateFlow(60)
    val anomalyIdleTime: StateFlow<Int> = _anomalyIdleTime.asStateFlow()

    private val _boostSecondsLeft = MutableStateFlow(0)
    val boostSecondsLeft: StateFlow<Int> = _boostSecondsLeft.asStateFlow()

    private val _anomalyEngineActive = MutableStateFlow(false)
    val anomalyEngineActive: StateFlow<Boolean> = _anomalyEngineActive.asStateFlow()

    private val _anomalyConsumedBases = MutableStateFlow(0L)
    val anomalyConsumedBases: StateFlow<Long> = _anomalyConsumedBases.asStateFlow()

    private val _discoveredPacketsLog = MutableStateFlow<List<GenePacket>>(emptyList())
    val discoveredPacketsLog: StateFlow<List<GenePacket>> = _discoveredPacketsLog.asStateFlow()

    private val _scrollingGene = MutableStateFlow("AGTCGTAC")
    val scrollingGene: StateFlow<String> = _scrollingGene.asStateFlow()

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

    val targetSynthesisSequence: StateFlow<String> = _targetSynthesisSequence.asStateFlow()

    private val _isSynthesisActive = MutableStateFlow(false)
    val isSynthesisActive: StateFlow<Boolean> = _isSynthesisActive.asStateFlow()

    private val _isReactionCollapsed = MutableStateFlow(false)
    val isReactionCollapsed: StateFlow<Boolean> = _isReactionCollapsed.asStateFlow()

    private val _isLoopActive = MutableStateFlow(false)
    val isLoopActive: StateFlow<Boolean> = _isLoopActive.asStateFlow()

    private val _selectedReagent = MutableStateFlow<String?>(null)
    val selectedReagent: StateFlow<String?> = _selectedReagent.asStateFlow()

    private val _reagents = MutableStateFlow<List<ReagentStock>>(emptyList())
    val reagents: StateFlow<List<ReagentStock>> = _reagents.asStateFlow()

    private val _devForceAnomaly = MutableStateFlow(false)
    val devForceAnomaly: StateFlow<Boolean> = _devForceAnomaly.asStateFlow()

    // Base Font fields for GPS siphoning
    private val _baseFonts = MutableStateFlow<List<PoxBaseFont>>(emptyList())
    val baseFonts: StateFlow<List<PoxBaseFont>> = _baseFonts.asStateFlow()

    private val _nearestFontDistance = MutableStateFlow<Double?>(null)
    val nearestFontDistance: StateFlow<Double?> = _nearestFontDistance.asStateFlow()

    private val _activeSiphonedFont = MutableStateFlow<PoxBaseFont?>(null)
    val activeSiphonedFont: StateFlow<PoxBaseFont?> = _activeSiphonedFont.asStateFlow()

    private val _siphonedRate = MutableStateFlow(0L)
    val siphonedRate: StateFlow<Long> = _siphonedRate.asStateFlow()

    private val _celestialWells = MutableStateFlow<List<CelestialWell>>(emptyList())
    val celestialWells: StateFlow<List<CelestialWell>> = _celestialWells.asStateFlow()

    // Location fields updated from MainViewModel
    private val _latitude = MutableStateFlow(37.4220)
    private val _longitude = MutableStateFlow(-122.0841)

    // Gene sequences from repository (held locally for standard/anomalous checking)
    private val geneSequences = geneStockRepository.allGeneSequences.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList(),
    )

    val selectedHostPlanet = preferencesRepository.selectedHostPlanet.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "EARTH",
    )

    fun changeHostPlanet(planet: String) {
        viewModelScope.launch {
            preferencesRepository.saveSelectedHostPlanet(planet)
        }
    }

    private var heartbeatJob: Job? = null
    private var scrollingTickerJob: Job? = null
    private var ticksSinceLastSave = 0

    init {
        // Collect DB resources
        viewModelScope.launch {
            preferencesRepository.rawStockA.collect { _rawStockA.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.rawStockG.collect { _rawStockG.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.rawStockT.collect { _rawStockT.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.rawStockC.collect { _rawStockC.value = it }
        }
        viewModelScope.launch {
            preferencesRepository.bioWaste.collect { _bioWaste.value = it }
        }
        viewModelScope.launch {
            geneStockRepository.allReagentStock.collect { list ->
                if (list.isNotEmpty()) {
                    _reagents.value = list
                }
            }
        }

        // Initialize wells
        val startLat = _latitude.value
        val startLng = _longitude.value
        val ghDLat = 300.0 / 364000.0
        val ghDLng = -200.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))
        val magDLat = -500.0 / 364000.0
        val magDLng = 400.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))
        val gravDLat = 400.0 / 364000.0
        val gravDLng = 600.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))

        _celestialWells.value = listOf(
            CelestialWell("WELL-GH", "Venusian Greenhouse Anomaly", startLat + ghDLat, startLng + ghDLng, 800.0, "GREENHOUSE", 1.0),
            CelestialWell("WELL-MAG", "Jovian Magnetospheric Anomaly", startLat + magDLat, startLng + magDLng, 1000.0, "MAGNETOSPHERIC", 1.0),
            CelestialWell("WELL-GRAV", "Asteroid Gravitational Anomaly", startLat + gravDLat, startLng + gravDLng, 600.0, "GRAVITATIONAL", 1.0),
        )

        if (!WaveMath.isTestMode) {
            startReactorHeartbeat()
            startScrollingGeneTicker()
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        _latitude.value = lat
        _longitude.value = lng
    }

    fun setReactorTemperature(value: Float) {
        _reactorTemperature.value = value
    }

    fun setReactorSalt(value: Float) {
        _reactorSalt.value = value
    }

    fun setActivePolymerase(value: String) {
        val polyCost = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost(value, _targetSynthesisSequence.value)

        val currentA = _rawStockA.value
        val currentG = _rawStockG.value
        val currentT = _rawStockT.value
        val currentC = _rawStockC.value
        val currentWaste = _bioWaste.value

        if (currentA < polyCost.costA || currentG < polyCost.costG || currentT < polyCost.costT || currentC < polyCost.costC) {
            addLog("ERR: Insufficient resources to program $value (Requires ${polyCost.costA}A/${polyCost.costG}G/${polyCost.costT}T/${polyCost.costC}C).")
            synthManager.playReject()
            return
        }

        _activePolymerase.value = value
        addLog("REACTOR: Selected polymerase engine: $value")
        synthManager.playCombinatorTick()
    }

    fun setActiveChemicalSolute(value: String) {
        val soluteCost = com.example.genpox.data.ReactorSimulationEngine.calculateSoluteCost(value)
        val currentA = _rawStockA.value
        val currentG = _rawStockG.value
        val currentT = _rawStockT.value
        val currentC = _rawStockC.value

        if (currentA < soluteCost || currentG < soluteCost || currentT < soluteCost || currentC < soluteCost) {
            addLog("ERR: Insufficient resources to program $value (Requires $soluteCost of all bases A/G/T/C).")
            synthManager.playReject()
            return
        }

        _activeChemicalSolute.value = value
        addLog("REACTOR: Selected chemical solute buffer: $value")
        synthManager.playCombinatorTick()
    }

    fun setInletRatioA(value: Float) {
        _inletRatioA.value = value
    }
    fun setInletRatioG(value: Float) {
        _inletRatioG.value = value
    }
    fun setInletRatioT(value: Float) {
        _inletRatioT.value = value
    }
    fun setInletRatioC(value: Float) {
        _inletRatioC.value = value
    }

    fun setReactorDialIndex(index: Int) {
        synthManager.playCombinatorTick()
    }

    fun toggleLoopActive() {
        _isLoopActive.value = !_isLoopActive.value
        synthManager.playCombinatorTick()
        addLog("REACTOR: Auto-loop synthesis ${if (_isLoopActive.value) "ENGAGED" else "DISENGAGED"}.")
    }

    fun selectReagent(reagentId: String?) {
        _selectedReagent.value = reagentId
        if (reagentId != null) {
            synthManager.playBeep(520f, 0.05f, "sine")
        }
    }

    fun toggleDevForceAnomaly() {
        _devForceAnomaly.value = !_devForceAnomaly.value
        addLog("DEV: Force Anomalous compilation set to ${_devForceAnomaly.value}")
        synthManager.playBeep(600f, 0.1f, "sine")
    }

    fun addDevBases() {
        viewModelScope.launch {
            val a = _rawStockA.value + 10000L
            val g = _rawStockG.value + 10000L
            val t = _rawStockT.value + 10000L
            val c = _rawStockC.value + 10000L
            _rawStockA.value = a
            _rawStockG.value = g
            _rawStockT.value = t
            _rawStockC.value = c
            preferencesRepository.saveRawStocks(a, g, t, c)
            addLog("DEV: Injected +10k of all raw bases.")
        }
    }

    fun clearDevBases() {
        viewModelScope.launch {
            _rawStockA.value = 0L
            _rawStockG.value = 0L
            _rawStockT.value = 0L
            _rawStockC.value = 0L
            preferencesRepository.saveRawStocks(0, 0, 0, 0)
            addLog("DEV: Cleared all raw bases stocks.")
        }
    }

    fun clearDevGenes() {
        viewModelScope.launch(Dispatchers.IO) {
            geneStockRepository.deleteAllGeneSequences()
            addLog("DEV: Cleared local genetic archive.")
        }
    }

    fun clearDevCreatures() {
        viewModelScope.launch(Dispatchers.IO) {
            creatureRepository.deleteAllCreatures()
            addLog("DEV: Cleared active creature hangar.")
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

    fun setTargetSynthesisSequence(sequence: String) {
        if (sequence.length == 8) {
            _targetSynthesisSequence.value = sequence.uppercase()
            addLog("SYS: Set Bio-Lab synthesis target sequence to ${sequence.uppercase()}")
            synthManager.playCombinatorTick()
        }
    }

    fun recycleMatchingGenes(prefix: String) {
        if (prefix.isEmpty()) return
        viewModelScope.launch {
            val matches = geneSequences.value.filter { it.sequence.startsWith(prefix, ignoreCase = true) }
            if (matches.isEmpty()) return@launch

            var totalCount = 0L
            matches.forEach { gene ->
                totalCount += gene.count
                geneStockRepository.deleteGeneSequence(gene)
            }

            val wasteYield = totalCount * 2
            val newWaste = _bioWaste.value + wasteYield
            _bioWaste.value = newWaste
            preferencesRepository.saveBioWaste(newWaste)

            addLog("VAULT RECYCLING: Bulk deconstructed $totalCount matching gene blocks. Recovered +$wasteYield Bio-Waste.")
            synthManager.playSynthesisSuccess()
        }
    }

    fun decomposeGeneBlock(sequence: String, countToDecompose: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val matches = geneSequences.value.filter { it.sequence == sequence }
            val totalAvailable = matches.sumOf { it.count }
            val actualCount = Math.min(totalAvailable, countToDecompose)
            if (actualCount <= 0) return@launch

            var remainingToDecompose = actualCount
            for (gene in matches) {
                if (remainingToDecompose <= 0) break
                val takeFromThis = Math.min(gene.count, remainingToDecompose)
                val nextCount = gene.count - takeFromThis
                if (nextCount == 0) {
                    geneStockRepository.deleteGeneSequence(gene)
                } else {
                    geneStockRepository.insertGeneSequence(gene.copy(count = nextCount))
                }
                remainingToDecompose -= takeFromThis
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
            val wasteYield = actualCount * 2L

            val newA = _rawStockA.value + yieldA
            val newG = _rawStockG.value + yieldG
            val newT = _rawStockT.value + yieldT
            val newC = _rawStockC.value + yieldC
            val newWaste = _bioWaste.value + wasteYield

            _rawStockA.value = newA
            _rawStockG.value = newG
            _rawStockT.value = newT
            _rawStockC.value = newC
            _bioWaste.value = newWaste

            preferencesRepository.saveRawStocks(newA, newG, newT, newC)
            preferencesRepository.saveBioWaste(newWaste)

            addLog("RECYCLED: Deconstructed $actualCount blocks of $sequence into raw bases: +$yieldA A, +$yieldG G, +$yieldT T, +$yieldC C, and +$wasteYield Bio-Waste.")
            synthManager.playCombinatorTick()
        }
    }

    fun initiateStandardSynthesis() {
        if (_isSynthesisActive.value) return

        viewModelScope.launch {
            val target = _targetSynthesisSequence.value
            val temp = _reactorTemperature.value
            val salt = _reactorSalt.value.toDouble()
            val poly = _activePolymerase.value
            val solute = _activeChemicalSolute.value

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

            val hasInletAError = reqA > 0 && _inletRatioA.value <= 0.05f
            val hasInletGError = reqG > 0 && _inletRatioG.value <= 0.05f
            val hasInletTError = reqT > 0 && _inletRatioT.value <= 0.05f
            val hasInletCError = reqC > 0 && _inletRatioC.value <= 0.05f

            if (hasInletAError || hasInletGError || hasInletTError || hasInletCError) {
                addLog("SYNTHESIS BLOCKED: Feedstock inlets for required bases must be open to at least 5% to charge chamber.")
                synthManager.playReject()
                return@launch
            }

            val soluteCost = com.example.genpox.data.ReactorSimulationEngine.calculateSoluteCost(solute)
            val polyCost = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost(poly, target)

            val totalReqA = reqA + soluteCost + polyCost.costA
            val totalReqG = reqG + soluteCost + polyCost.costG
            val totalReqT = reqT + soluteCost + polyCost.costT
            val totalReqC = reqC + soluteCost + polyCost.costC
            val currentWaste = _bioWaste.value

            val currentA = _rawStockA.value
            val currentG = _rawStockG.value
            val currentT = _rawStockT.value
            val currentC = _rawStockC.value

            if (currentA < totalReqA || currentG < totalReqG || currentT < totalReqT || currentC < totalReqC) {
                addLog("SYNTHESIS RUNTIME ERROR: Insufficient stocks for sequence, solutes, or enzyme cost ($poly requires ${polyCost.costA}A/${polyCost.costG}G/${polyCost.costT}T/${polyCost.costC}C).")
                _isLoopActive.value = false
                synthManager.playReject()
                return@launch
            }

            _isSynthesisActive.value = true
            _isReactionCollapsed.value = false
            _activeSynthesisStep.value = 0
            _activeSynthesisStrand.value = ""
            _activeSynthesisQScore.value = 0.0

            val afterA = currentA - totalReqA
            val afterG = currentG - totalReqG
            val afterT = currentT - totalReqT
            val afterC = currentC - totalReqC

            _rawStockA.value = afterA
            _rawStockG.value = afterG
            _rawStockT.value = afterT
            _rawStockC.value = afterC
            preferencesRepository.saveRawStocks(afterA, afterG, afterT, afterC)

            if (soluteCost > 0) {
                addLog("SOLUTE INJECTION: Consumed $soluteCost units of each base for active chemical solute buffer ($solute).")
            }
            if (polyCost.costA > 0 || polyCost.costG > 0 || polyCost.costT > 0 || polyCost.costC > 0) {
                addLog("ENZYME CHARGE: Consumed ${polyCost.costA}A/${polyCost.costG}G/${polyCost.costT}T/${polyCost.costC}C for $poly.")
            }

            addLog("REACTOR: Initializing stepped transcription of target sequence \"$target\"...")

            val baseCycle = when (poly.uppercase()) {
                "TAQ" -> 8
                "PFU" -> 32
                "TTH" -> 12
                else -> 12
            }
            val isBoostActive = _boostSecondsLeft.value > 0
            val cycleDuration = if (isBoostActive) baseCycle / 2 else baseCycle

            val saltSpeedMultiplier = 1.0 + (salt - 0.05) * 2.0
            val adjustedCycleDuration = cycleDuration / saltSpeedMultiplier
            val stepIntervalMs = ((adjustedCycleDuration * 1000L) / 8).toLong().coerceAtLeast(100L)

            val simResult = ReactorSimulationEngine.runSynthesis(
                target = target,
                temp = temp,
                salt = salt,
                poly = poly,
                solute = solute,
                inletRatioA = _inletRatioA.value,
                inletRatioG = _inletRatioG.value,
                inletRatioT = _inletRatioT.value,
                inletRatioC = _inletRatioC.value,
                stepIntervalMs = stepIntervalMs,
                hostPlanet = selectedHostPlanet.value,
                onStep = { stepRes ->
                    _activeSynthesisStrand.value = stepRes.accumulatedStrand
                    _activeSynthesisStep.value = stepRes.step
                    _activeSynthesisTm.value = stepRes.tm
                    _activeSynthesisMfe.value = stepRes.mfe
                    _activeSynthesisQScore.value = stepRes.qScore

                    if (stepRes.isMutated && stepRes.mutationMessage != null) {
                        addLog(stepRes.mutationMessage)
                    }

                    if (stepRes.isCollapsed) {
                        _isReactionCollapsed.value = true
                        val newWaste = _bioWaste.value + stepRes.step
                        _bioWaste.value = newWaste
                        viewModelScope.launch {
                            preferencesRepository.saveBioWaste(newWaste)
                        }
                        addLog("CRITICAL COLLAPSE: Halted at step ${stepRes.step} due to ${stepRes.collapseReason}. converted to bio-waste.")
                        synthManager.playReject()
                    }
                },
            )

            if (!simResult.isCollapsed) {
                val qScore = simResult.qScore
                val currentList = geneSequences.value
                val newGenesList = mutableListOf<String>()
                val isNewSequence = currentList.none { it.sequence == simResult.finalStrand }
                if (isNewSequence) {
                    newGenesList.add(simResult.finalStrand)
                }

                geneStockRepository.insertGeneSequence(
                    GeneSequence(
                        sequence = simResult.finalStrand,
                        count = 1,
                        discoveredAt = System.currentTimeMillis(),
                        averageQScore = qScore,
                        meltingTemp = simResult.tm,
                        minimumFreeEnergy = simResult.mfe,
                        polymeraseSignature = poly.uppercase(),
                    ),
                )

                val packet = GenePacket(
                    id = "PKT-" + UUID.randomUUID().toString().take(6).uppercase(),
                    genes = listOf(simResult.finalStrand),
                    timestamp = System.currentTimeMillis(),
                    isAnomalous = false,
                    newGenes = newGenesList,
                )
                _discoveredPacketsLog.update { current ->
                    (listOf(packet) + current).take(50)
                }

                addLog("REACTOR SYNTHESIS: Compiled gene block ${simResult.finalStrand} (Q-Score: ${String.format(Locale.US, "%.1f", qScore)}).")
                synthManager.playSynthesisSuccess()
            }

            _isSynthesisActive.value = false

            // Check remaining resources for current enzyme
            val remA = _rawStockA.value
            val remG = _rawStockG.value
            val remT = _rawStockT.value
            val remC = _rawStockC.value
            val remWaste = _bioWaste.value
            val currentEnz = _activePolymerase.value
            val currentEnzCost = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost(currentEnz, target)

            if (remA < currentEnzCost.costA || remG < currentEnzCost.costG || remT < currentEnzCost.costT || remC < currentEnzCost.costC) {
                _activePolymerase.value = "Taq"
                addLog("SYS WARNING: Selected enzyme reset to Taq due to stock depletion.")
                synthManager.playReject()
            }

            val currentSolute = _activeChemicalSolute.value
            val currentSoluteCost = com.example.genpox.data.ReactorSimulationEngine.calculateSoluteCost(currentSolute)
            if (remA < currentSoluteCost || remG < currentSoluteCost || remT < currentSoluteCost || remC < currentSoluteCost) {
                _activeChemicalSolute.value = "None"
                addLog("SYS WARNING: Selected solute reset to None due to stock depletion.")
            }

            // Loop synthesis handling
            if (_isLoopActive.value) {
                val nextEnz = _activePolymerase.value
                val nextPolyCost = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost(nextEnz, target)

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

                val nextSoluteCost = com.example.genpox.data.ReactorSimulationEngine.calculateSoluteCost(_activeChemicalSolute.value)

                val nextTotalA = nextReqA + nextSoluteCost + nextPolyCost.costA
                val nextTotalG = nextReqG + nextSoluteCost + nextPolyCost.costG
                val nextTotalT = nextReqT + nextSoluteCost + nextPolyCost.costT
                val nextTotalC = nextReqC + nextSoluteCost + nextPolyCost.costC

                val nextRemA = _rawStockA.value
                val nextRemG = _rawStockG.value
                val nextRemT = _rawStockT.value
                val nextRemC = _rawStockC.value

                if (nextRemA >= nextTotalA && nextRemG >= nextTotalG && nextRemT >= nextTotalT && nextRemC >= nextTotalC) {
                    delay(500L)
                    initiateStandardSynthesis()
                } else {
                    _isLoopActive.value = false
                    addLog("SYS: Synthesis loop halted due to insufficient feedstock.")
                    synthManager.playReject()
                }
            }
        }
    }

    fun stopReactorHeartbeat() {
        heartbeatJob?.cancel()
    }

    fun stopScrollingGeneTicker() {
        scrollingTickerJob?.cancel()
    }

    fun stopAllTickers() {
        heartbeatJob?.cancel()
        scrollingTickerJob?.cancel()
    }

    public override fun onCleared() {
        super.onCleared()
        stopAllTickers()
    }

    private fun startReactorHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.Default) {
            var siphonTick = 0
            var siphonLogTicks = 0
            while (true) {
                delay(1000L)

                val now = System.currentTimeMillis()
                var currentFonts = _baseFonts.value.filter { !it.isExpired(now) }
                val pLat = _latitude.value
                val pLng = _longitude.value

                if (currentFonts.size < 3) {
                    val updatedFonts = currentFonts.toMutableList()
                    val random = java.util.Random()
                    while (updatedFonts.size < 5) {
                        val distFeet = 150.0 + random.nextDouble() * 650.0
                        val angle = random.nextDouble() * 2.0 * Math.PI
                        val dLat = (distFeet * Math.cos(angle)) / 364000.0
                        val dLng = (distFeet * Math.sin(angle)) / (364000.0 * Math.cos(Math.toRadians(pLat)))

                        val fontId = "FNT-" + UUID.randomUUID().toString().take(6).uppercase()
                        val bases = listOf('A', 'G', 'T', 'C')
                        val fontBase = bases[random.nextInt(4)]
                        val lifetime = 60000L + random.nextInt(60).toLong() * 1000L
                        val isInvertedFont = random.nextDouble() < 0.15

                        updatedFonts.add(PoxBaseFont(fontId, pLat + dLat, pLng + dLng, fontBase, now, lifetime, isInverted = isInvertedFont))
                    }
                    currentFonts = updatedFonts
                    _baseFonts.value = currentFonts
                } else {
                    _baseFonts.value = currentFonts
                }

                var nearestDist: Double? = null
                var activeSiphonFont: PoxBaseFont? = null
                var gatherRate = 0L

                for (font in currentFonts) {
                    val dist = calculateDistanceInFeet(pLat, pLng, font.lat, font.lng)
                    val currentNearest = nearestDist
                    if (currentNearest == null || dist < currentNearest) {
                        nearestDist = dist
                    }
                    if (dist <= font.maxRadiusFeet) {
                        activeSiphonFont = font
                        gatherRate = font.getGatherAmount(dist)
                    }
                }

                _nearestFontDistance.value = nearestDist
                _activeSiphonedFont.value = activeSiphonFont
                _siphonedRate.value = gatherRate

                val currentSiphonFont = activeSiphonFont
                if (gatherRate > 0 && currentSiphonFont != null) {
                    val base = currentSiphonFont.baseType
                    siphonLogTicks++
                    if (currentSiphonFont.isInverted) {
                        when (base) {
                            'A' -> _rawStockA.value = (_rawStockA.value - gatherRate).coerceAtLeast(0L)
                            'G' -> _rawStockG.value = (_rawStockG.value - gatherRate).coerceAtLeast(0L)
                            'T' -> _rawStockT.value = (_rawStockT.value - gatherRate).coerceAtLeast(0L)
                            'C' -> _rawStockC.value = (_rawStockC.value - gatherRate).coerceAtLeast(0L)
                        }
                        preferencesRepository.saveRawStocks(_rawStockA.value, _rawStockG.value, _rawStockT.value, _rawStockC.value)

                        val wasteAdded = gatherRate / 6
                        if (wasteAdded > 0) {
                            _bioWaste.value += wasteAdded
                            preferencesRepository.saveBioWaste(_bioWaste.value)
                        }
                        if (siphonLogTicks >= 3) {
                            siphonLogTicks = 0
                            val baseName = when (currentSiphonFont.baseType) {
                                'A' -> "Adenine"
                                'G' -> "Guanine"
                                'T' -> "Thymine"
                                'C' -> "Cytosine"
                                else -> "Unknown"
                            }
                            addLog("[VOID] Gravitational void siphoned $gatherRate $baseName units, transmuting into +$wasteAdded Bio-Waste.")
                        }
                    } else {
                        when (base) {
                            'A' -> _rawStockA.value += gatherRate
                            'G' -> _rawStockG.value += gatherRate
                            'T' -> _rawStockT.value += gatherRate
                            'C' -> _rawStockC.value += gatherRate
                        }
                        preferencesRepository.saveRawStocks(_rawStockA.value, _rawStockG.value, _rawStockT.value, _rawStockC.value)
                        if (siphonLogTicks >= 3) {
                            siphonLogTicks = 0
                            val baseName = when (currentSiphonFont.baseType) {
                                'A' -> "Adenine"
                                'G' -> "Guanine"
                                'T' -> "Thymine"
                                'C' -> "Cytosine"
                                else -> "Unknown"
                            }
                            addLog("[SIPHON] Siphoned +$gatherRate $baseName units from Font.")
                        }
                    }
                } else {
                    siphonLogTicks = 0
                }

                siphonTick++
                if (siphonTick >= 5) {
                    siphonTick = 0
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
                        preferencesRepository.saveRawStocks(nextA, nextG, nextT, nextC)
                    }
                }

                // Decrement booster timer
                if (_boostSecondsLeft.value > 0) {
                    _boostSecondsLeft.value -= 1
                    if (_boostSecondsLeft.value == 0) {
                        addLog("REACTOR BOOSTER EXPIRED: Splicing cycle returned to standard duration.")
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
                delay(110L)
                var temp = ""
                for (i in 0 until 8) {
                    temp += bases[rand.nextInt(bases.size)]
                }
                _scrollingGene.value = temp
            }
        }
    }

    fun setBioLabSubTab(subTab: String) {
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
            val rawTotal = _rawStockA.value + _rawStockG.value + _rawStockT.value + _rawStockC.value
            if (rawTotal < 250000L) {
                addLog("CANNOT ACTIVATE ANOMALY ENGINE: Requires a massive stockpile of minimum 250k total nucleotides.")
                synthManager.playReject()
                return
            }
            _anomalyEngineActive.value = true
            _poxReactorActive.value = false
            _anomalyConsumedBases.value = 0L
            addLog("ANOMALY ENGINE ENGAGED! Cosmic gene hunting activated!")
            synthManager.playBeep(120f, 0.6f, "sawtooth")
        } else {
            _anomalyEngineActive.value = false
            _anomalyConsumedBases.value = 0L
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

    fun clearDiscoveredPacketsLog() {
        _discoveredPacketsLog.value = emptyList()
    }

    private fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = GeoMath.calculateHaversineDistanceInFeet(lat1, lng1, lat2, lng2)

    fun triggerAnomalousSynthesisStub() {
        viewModelScope.launch {
            triggerAnomalousSynthesis()
        }
    }

    private suspend fun triggerAnomalousSynthesis() {
        val isDevMode = _devForceAnomaly.value
        var rollChance = 0.0
        val isSuccess = if (isDevMode) {
            true
        } else {
            val currentA = (_rawStockA.value - 2500L).coerceAtLeast(0L)
            val currentG = (_rawStockG.value - 2500L).coerceAtLeast(0L)
            val currentT = (_rawStockT.value - 2500L).coerceAtLeast(0L)
            val currentC = (_rawStockC.value - 2500L).coerceAtLeast(0L)
            _rawStockA.value = currentA
            _rawStockG.value = currentG
            _rawStockT.value = currentT
            _rawStockC.value = currentC
            preferencesRepository.saveRawStocks(currentA, currentG, currentT, currentC)

            _anomalyConsumedBases.value += 10000L

            val currentEnz = _activePolymerase.value
            val polyCost = com.example.genpox.data.ReactorSimulationEngine.calculatePolymeraseCost(currentEnz, _targetSynthesisSequence.value)
            if (currentA < polyCost.costA || currentG < polyCost.costG || currentT < polyCost.costT || currentC < polyCost.costC) {
                _activePolymerase.value = "Taq"
                addLog("SYS WARNING: Selected enzyme reset to Taq due to stock depletion from anomalous synthesis.")
            }

            val currentSolute = _activeChemicalSolute.value
            val soluteCost = com.example.genpox.data.ReactorSimulationEngine.calculateSoluteCost(currentSolute)
            if (currentA < soluteCost || currentG < soluteCost || currentT < soluteCost || currentC < soluteCost) {
                _activeChemicalSolute.value = "None"
                addLog("SYS WARNING: Selected solute reset to None due to stock depletion from anomalous synthesis.")
            }

            val coupling = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
            val chanceMetrics = WaveMath.getAnomalyEngineSuccessChance(_anomalyConsumedBases.value, coupling)
            rollChance = chanceMetrics.finalChance
            val roll = java.util.Random().nextDouble() * 100.0

            roll <= chanceMetrics.finalChance
        }

        val currentList = geneSequences.value
        val newGenesList = mutableListOf<String>()

        val generated = if (isSuccess) {
            val anomalousGene = WaveMath.generateAnomalousGene()
            val tm = GeneticsEngine.calculateMeltingTemperature(anomalousGene, _reactorSalt.value.toDouble())
            val mfe = GeneticsEngine.calculateMinimumFreeEnergy(anomalousGene)
            val q = 40.0

            val isNewSequence = currentList.none { it.sequence == anomalousGene }
            if (isNewSequence) {
                newGenesList.add(anomalousGene)
            }
            geneStockRepository.insertGeneSequence(
                GeneSequence(
                    sequence = anomalousGene,
                    count = 1,
                    discoveredAt = System.currentTimeMillis(),
                    averageQScore = q,
                    meltingTemp = tm,
                    minimumFreeEnergy = mfe,
                    polymeraseSignature = _activePolymerase.value.uppercase(),
                ),
            )
            anomalousGene
        } else {
            "DECAYED!"
        }

        val packet = GenePacket(
            id = "ANM-" + UUID.randomUUID().toString().take(6).uppercase(),
            genes = listOf(generated),
            timestamp = System.currentTimeMillis(),
            isAnomalous = true,
            newGenes = newGenesList,
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
            _anomalyConsumedBases.value = 0L

            val rawTotal = _rawStockA.value + _rawStockG.value + _rawStockT.value + _rawStockC.value
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
}

package com.example.genpox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SplicerViewModel(
    private val repository: DataRepository,
    private val synthManager: PoxSynthManager,
    val reactorSalt: StateFlow<Float>
) : ViewModel() {

    var onLog: ((String) -> Unit)? = null
    var onSetActiveCreature: ((Creature?, String) -> Unit)? = null
    var onSelectTab: ((String) -> Unit)? = null

    private fun addLog(message: String) {
        onLog?.invoke(message)
    }

    // State Variables
    private val _targetSequence = MutableStateFlow("GCAATAGC")
    val targetSequence: StateFlow<String> = _targetSequence.asStateFlow()

    private val _splicerSlots = MutableStateFlow<List<String?>>(List(8) { null })
    val splicerSlots: StateFlow<List<String?>> = _splicerSlots.asStateFlow()

    private val _splicerQScores = MutableStateFlow<List<Double>>(List(8) { 25.0 })
    val splicerQScores: StateFlow<List<Double>> = _splicerQScores.asStateFlow()

    private val _splicerSignatures = MutableStateFlow<List<String>>(List(8) { "TAQ" })
    val splicerSignatures: StateFlow<List<String>> = _splicerSignatures.asStateFlow()

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

    private val _sequencedBasesCount = MutableStateFlow(0)
    val sequencedBasesCount: StateFlow<Int> = _sequencedBasesCount.asStateFlow()

    private val _newlySplicedCreature = MutableStateFlow<Creature?>(null)
    val newlySplicedCreature: StateFlow<Creature?> = _newlySplicedCreature.asStateFlow()

    private val _isCustomScaffoldMode = MutableStateFlow(false)
    val isCustomScaffoldMode: StateFlow<Boolean> = _isCustomScaffoldMode.asStateFlow()

    private val _selectedSplicerCreature = MutableStateFlow<Creature?>(null)
    val selectedSplicerCreature: StateFlow<Creature?> = _selectedSplicerCreature.asStateFlow()

    private val _selectedGeneToAppend = MutableStateFlow<String?>(null)
    val selectedGeneToAppend: StateFlow<String?> = _selectedGeneToAppend.asStateFlow()

    // Local caches collected from DB
    val geneSequences = repository.allGeneSequences.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    private val _rawStockA = MutableStateFlow(10000L)
    private val _rawStockG = MutableStateFlow(10000L)
    private val _rawStockT = MutableStateFlow(10000L)
    private val _rawStockC = MutableStateFlow(10000L)
    private val _bioWaste = MutableStateFlow(0L)
    private val _reactorSalt = MutableStateFlow(0.05f)

    // Developer mode flag reference
    var devForceAnomalyState: StateFlow<Boolean> = MutableStateFlow(false)

    var onResetSelectedReagent: (() -> Unit)? = null

    init {
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
        viewModelScope.launch {
            repository.splicerSlots.collect { _splicerSlots.value = it }
        }
        viewModelScope.launch {
            repository.splicerQScores.collect { _splicerQScores.value = it }
        }
        viewModelScope.launch {
            repository.splicerSignatures.collect { _splicerSignatures.value = it }
        }
        viewModelScope.launch {
            repository.rawStockA.collect { _rawStockA.value = it }
        }
        viewModelScope.launch {
            repository.rawStockG.collect { _rawStockG.value = it }
        }
        viewModelScope.launch {
            repository.rawStockT.collect { _rawStockT.value = it }
        }
        viewModelScope.launch {
            repository.rawStockC.collect { _rawStockC.value = it }
        }
        viewModelScope.launch {
            repository.bioWaste.collect { _bioWaste.value = it }
        }
        viewModelScope.launch {
            reactorSalt.collect { _reactorSalt.value = it }
        }
    }

    fun selectSplicerSlot(idx: Int?) {
        _activeSlotSelection.value = idx
        _slotSequenceFilter.value = ""
        if (idx != null) {
            synthManager.playBeep(440f, 0.05f, "sine")
        }
    }

    fun setCustomScaffoldMode(active: Boolean) {
        _isCustomScaffoldMode.value = active
        viewModelScope.launch {
            for (i in 0 until 8) {
                val existing = _splicerSlots.value.getOrNull(i)
                if (existing != null) {
                    val qScore = _splicerQScores.value.getOrNull(i) ?: 25.0
                    val tm = GeneticsEngine.calculateMeltingTemperature(existing, _reactorSalt.value.toDouble())
                    val mfe = GeneticsEngine.calculateMinimumFreeEnergy(existing)
                    repository.insertGeneSequence(
                        GeneSequence(
                            sequence = existing,
                            count = 1,
                            discoveredAt = System.currentTimeMillis(),
                            averageQScore = qScore,
                            meltingTemp = tm,
                            minimumFreeEnergy = mfe
                        )
                    )
                }
            }
            val clearedSlots = List<String?>(8) { null }
            val clearedQScores = List(8) { 25.0 }
            val clearedSignatures = List(8) { "TAQ" }
            _splicerSlots.value = clearedSlots
            _splicerQScores.value = clearedQScores
            _splicerSignatures.value = clearedSignatures
            repository.saveSplicerState(clearedSlots, clearedQScores, clearedSignatures)
            addLog("SPLICER: Custom Scaffold Mode ${if (active) "ENGAGED" else "DISENGAGED"}. Splicer slots reset.")
        }
    }

    fun setSlotSequenceFilter(query: String) {
        _slotSequenceFilter.value = query.uppercase()
    }

    fun assignGeneToSlot(seq: String) {
        val slotIdx = _activeSlotSelection.value ?: return
        viewModelScope.launch {
            if (!_isCustomScaffoldMode.value) {
                val target = _targetSequence.value
                val expected = target.substring(slotIdx * 8, (slotIdx + 1) * 8)
                if (seq != expected) {
                    addLog("GENOME MISMATCH: Gene does not match required slot sequence $expected")
                    synthManager.playReject()
                    return@launch
                }
            }

            val currentGenes = geneSequences.value
            val match = currentGenes.find { it.sequence == seq && it.count > 0 }
            if (match == null) {
                addLog("ERR: Gene $seq not in inventory.")
                synthManager.playReject()
                return@launch
            }

            repository.deleteGeneSequence(match)

            val existing = _splicerSlots.value[slotIdx]
            if (existing != null) {
                val qScore = _splicerQScores.value[slotIdx]
                val tm = GeneticsEngine.calculateMeltingTemperature(existing, _reactorSalt.value.toDouble())
                val mfe = GeneticsEngine.calculateMinimumFreeEnergy(existing)
                val sig = _splicerSignatures.value.getOrNull(slotIdx) ?: "TAQ"
                repository.insertGeneSequence(
                    GeneSequence(
                        sequence = existing,
                        count = 1,
                        discoveredAt = System.currentTimeMillis(),
                        averageQScore = qScore,
                        meltingTemp = tm,
                        minimumFreeEnergy = mfe,
                        polymeraseSignature = sig
                    )
                )
            }

            val updatedSlots = _splicerSlots.value.toMutableList()
            updatedSlots[slotIdx] = seq
            _splicerSlots.value = updatedSlots

            val updatedQScores = _splicerQScores.value.toMutableList()
            updatedQScores[slotIdx] = match.averageQScore
            _splicerQScores.value = updatedQScores

            val updatedSignatures = _splicerSignatures.value.toMutableList()
            updatedSignatures[slotIdx] = match.polymeraseSignature
            _splicerSignatures.value = updatedSignatures

            repository.saveSplicerState(updatedSlots, updatedQScores, updatedSignatures)

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

            val qScore = _splicerQScores.value.getOrNull(slotIdx) ?: 25.0
            val tm = GeneticsEngine.calculateMeltingTemperature(existing, _reactorSalt.value.toDouble())
            val mfe = GeneticsEngine.calculateMinimumFreeEnergy(existing)
            val signature = _splicerSignatures.value.getOrNull(slotIdx) ?: "TAQ"
            repository.insertGeneSequence(
                GeneSequence(
                    sequence = existing,
                    count = 1,
                    discoveredAt = System.currentTimeMillis(),
                    averageQScore = qScore,
                    meltingTemp = tm,
                    minimumFreeEnergy = mfe,
                    polymeraseSignature = signature
                )
            )

            val updatedSlots = _splicerSlots.value.toMutableList()
            updatedSlots[slotIdx] = null
            _splicerSlots.value = updatedSlots

            val updatedQScores = _splicerQScores.value.toMutableList()
            updatedQScores[slotIdx] = 25.0
            _splicerQScores.value = updatedQScores

            val updatedSignatures = _splicerSignatures.value.toMutableList()
            updatedSignatures[slotIdx] = "TAQ"
            _splicerSignatures.value = updatedSignatures

            repository.saveSplicerState(updatedSlots, updatedQScores, updatedSignatures)

            addLog("Returned gene segment #${slotIdx + 1} to archive stock")
        }
    }

    fun autofillSplicerSlots() {
        synthManager.playBeep(880f, 0.05f, "sine")
        viewModelScope.launch(Dispatchers.Default) {
            val target = _targetSequence.value
            if (target.length != 64) return@launch

            val updatedSlots = _splicerSlots.value.toMutableList()
            val updatedQScores = _splicerQScores.value.toMutableList()
            val updatedSignatures = _splicerSignatures.value.toMutableList()
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
                        updatedQScores[i] = matchedGene.averageQScore
                        updatedSignatures[i] = matchedGene.polymeraseSignature
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
                _splicerQScores.value = updatedQScores
                _splicerSignatures.value = updatedSignatures
                repository.saveSplicerState(updatedSlots, updatedQScores, updatedSignatures)
                addLog("AUTO GENE: Filled $autoFilledCount matching slots with verified stock segments.")
            } else {
                addLog("AUTO GENE: No matching segments found in stock for any unfilled slots.")
            }
        }
    }

    fun devInjectMissingTargetGenes() {
        if (!devForceAnomalyState.value) return
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
                    val tm = GeneticsEngine.calculateMeltingTemperature(seq, _reactorSalt.value.toDouble())
                    val mfe = GeneticsEngine.calculateMinimumFreeEnergy(seq)
                    existingList.add(
                        GeneSequence(
                            sequence = seq,
                            count = 1,
                            discoveredAt = System.currentTimeMillis(),
                            averageQScore = 30.0,
                            meltingTemp = tm,
                            minimumFreeEnergy = mfe
                        )
                    )
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

    fun weldGeneToCreature() {
        val creature = _selectedSplicerCreature.value ?: return
        val gene = _selectedGeneToAppend.value ?: return

        viewModelScope.launch {
            val currentGenes = geneSequences.value
            val match = currentGenes.find { it.sequence == gene && it.count > 0 }
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

            val newAppended = creature.appendedGenes + gene
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

    fun constructSplicedCreature(selectedReagent: String?) {
        val slots = _splicerSlots.value
        if (slots.contains(null)) return
        val fullDNASeq = slots.filterNotNull().joinToString("")
        if (fullDNASeq.length != 64) return

        viewModelScope.launch {
            _isSplicing.value = true
            _splicingProgress.value = 0
            _sequencedBasesCount.value = 0

            synthManager.playBeep(300f, 0.3f, "sawtooth")
            addLog("INITIATING MORPHOGENESIS THERMALS...")

            var modifierLore = ""
            var telomereBonus = 0
            var forceFullTelomeres = false
            var vitalityMod = 1.0
            var attackMod = 1.0
            var defenseMod = 1.0
            var speedMod = 1.0
            var isAutoHackerVal = false
            var isAutoDefenderVal = false
            var flatAttackReduction = 0

            when (selectedReagent) {
                "REAGENT_LIGASE" -> {
                    defenseMod = 1.20
                    forceFullTelomeres = true
                    modifierLore = "Assembled with T4 DNA Ligase high-fidelity joining enzymes."
                }
                "REAGENT_BETAINE" -> {
                    telomereBonus = -10
                    modifierLore = "Stabilized with Betaine minor groove solvation buffer."
                }
                "REAGENT_FORMAMIDE" -> {
                    telomereBonus = -10
                    modifierLore = "Secondary structure hairpins resolved with Formamide denaturant."
                }
                "REAGENT_TU" -> {
                    telomereBonus = 15
                    modifierLore = "Telomeres overcharged via Telomerase Activator (TU-115)."
                }
                "REAGENT_STAT_VITALITY" -> {
                    vitalityMod = 1.25
                    val aCount = fullDNASeq.count { it == 'A' }
                    val penalty = (aCount / 3.0) / 100.0
                    attackMod = 1.0 - penalty
                    defenseMod = 1.0 - penalty
                    speedMod = 1.0 - penalty
                    modifierLore = "Vitality prioritized via Adenine H-Bond Recruiter."
                }
                "REAGENT_STAT_ATTACK" -> {
                    attackMod = 1.25
                    val gCount = fullDNASeq.count { it == 'G' }
                    val penalty = (gCount / 3.0) / 100.0
                    vitalityMod = 1.0 - penalty
                    defenseMod = 1.0 - penalty
                    speedMod = 1.0 - penalty
                    modifierLore = "Attack potential maximized via Guanine Charge Exciter."
                }
                "REAGENT_STAT_DEFENSE" -> {
                    defenseMod = 1.25
                    val tCount = fullDNASeq.count { it == 'T' }
                    val penalty = (tCount / 3.0) / 100.0
                    vitalityMod = 1.0 - penalty
                    attackMod = 1.0 - penalty
                    speedMod = 1.0 - penalty
                    modifierLore = "Defense structure reinforced via Thymine Lattice Scaffold."
                }
                "REAGENT_STAT_SPEED" -> {
                    speedMod = 1.25
                    val cCount = fullDNASeq.count { it == 'C' }
                    val penalty = (cCount / 3.0) / 100.0
                    vitalityMod = 1.0 - penalty
                    attackMod = 1.0 - penalty
                    defenseMod = 1.0 - penalty
                    modifierLore = "Reaction speed polarized via Cytosine Spin Polarizer."
                }
                "REAGENT_ALIGN_CONTAINMENT" -> {
                    modifierLore = "Containment sector phenotype guided via alignment catalysts."
                }
                "REAGENT_ALIGN_INFECTION" -> {
                    modifierLore = "Infection sector phenotype guided via alignment catalysts."
                }
                "REAGENT_ALIGN_MECH" -> {
                    modifierLore = "Mech sector phenotype guided via alignment catalysts."
                }
                "REAGENT_ALIGN_PARASITE" -> {
                    modifierLore = "Parasite sector phenotype guided via alignment catalysts."
                }
                "REAGENT_TACTICAL_HACK" -> {
                    isAutoHackerVal = true
                    speedMod = 0.95
                    modifierLore = "Firewall intrusion protocols augmented via Beta-Gel Polymer."
                }
                "REAGENT_TACTICAL_DEFEND" -> {
                    isAutoDefenderVal = true
                    flatAttackReduction = 10
                    modifierLore = "Autonomous node-defense shielding coated with Silver-Nitrate Catalyst."
                }
                "REAGENT_TACTICAL_TEMP" -> {
                    modifierLore = "Thermal overclocking protocols active via Betaine Hydrate."
                }
                "REAGENT_VISUAL_GLOW" -> {
                    modifierLore = "Bioluminescent phosphorus markers visible on chassis profile."
                }
                "REAGENT_VISUAL_MATRIX" -> {
                    modifierLore = "Holographic matrix scan-line overlay injected."
                }
                "REAGENT_VISUAL_SHIMMER" -> {
                    modifierLore = "Chameleon structural shimmer enabled."
                }
            }

            val avgQ = _splicerQScores.value.average()
            val scaledTelomeres = if (forceFullTelomeres) 100 else ((avgQ * 2.5).toInt() + telomereBonus).coerceIn(10, 115)

            val spawned = compileDeterministicOffline(fullDNASeq, selectedReagent)
            val scaledVitality = (spawned.vitality * (0.5 + 0.5 * (avgQ / 40.0)) * vitalityMod).toInt()
            val scaledAttack = (spawned.attack * attackMod).toInt() - flatAttackReduction
            val scaledDefense = (spawned.defense * defenseMod).toInt()
            val scaledSpeed = (spawned.speed * speedMod).toInt()

            val sigs = _splicerSignatures.value
            val tthCount = sigs.count { it == "TTH" }
            val pfuCount = sigs.count { it == "PFU" }
            val taqCount = sigs.count { it == "TAQ" }
            val heatRes = tthCount * 12.5
            val radRes = pfuCount * 12.5
            val kineticRes = taqCount * 12.5

            val matchesTarget = getCoherence(fullDNASeq, _targetSequence.value) == "full"
            val idSuffix = UUID.randomUUID().toString().take(6).uppercase()
            val finalCreature = spawned.copy(
                id = "PX-$idSuffix",
                name = spawned.name,
                lore = spawned.lore + if (modifierLore.isNotEmpty()) "\n" + modifierLore else "",
                origin = if (selectedReagent != null) "Catalyzed Splicing" else "Spliced Gene",
                isFullCoherence = matchesTarget,
                coherenceType = if (matchesTarget) "Natural" else null,
                telomeres = scaledTelomeres,
                vitality = scaledVitality,
                attack = scaledAttack.coerceAtLeast(5),
                defense = scaledDefense.coerceAtLeast(5),
                speed = scaledSpeed.coerceAtLeast(5),
                isAutoHacker = isAutoHackerVal,
                isAutoDefender = isAutoDefenderVal,
                heatResistance = heatRes,
                radResistance = radRes,
                kineticResistance = kineticRes
            )

            repository.insertCreature(finalCreature)
            selectedReagent?.let { repository.decrementReagentStock(it) }
            _newlySplicedCreature.value = finalCreature

            for (k in 1..64) {
                delay(500L)
                _sequencedBasesCount.value = k
                _splicingProgress.value = (k * 100) / 64

                val baseChar = fullDNASeq.getOrNull(k - 1) ?: 'A'
                val pitchMod = k * 3.5f
                val freq = when (baseChar) {
                    'A' -> 320f + pitchMod
                    'G' -> 380f + pitchMod
                    'T' -> 440f + pitchMod
                    'C' -> 500f + pitchMod
                    else -> 220f + pitchMod
                }
                val oscType = when (baseChar) {
                    'A' -> "sine"
                    'G' -> "triangle"
                    'T' -> "sawtooth"
                    'C' -> "square"
                    else -> "sine"
                }
                synthManager.playBeep(freq, 0.08f, oscType)
            }

            addLog("SUCCESSFUL ASSEMBLY OF ${finalCreature.name}!")
            synthManager.playSynthesisSuccess()

            if (matchesTarget) {
                val nextTarget = generateRandom64Sequence()
                repository.saveTargetSequence(nextTarget)
                addLog("GEN-NET: Target sequence fully compiled! Generating new Synodic Sequence matrix.")
            }
        }
    }

    fun completeSplicingAndViewInVault() {
        val finalCreature = _newlySplicedCreature.value ?: return
        viewModelScope.launch {
            resetSplicerState()
            onSetActiveCreature?.invoke(finalCreature, "splicer")
            onSelectTab?.invoke("vault")
        }
    }

    fun completeSplicingAndReturnToSplicer() {
        viewModelScope.launch {
            resetSplicerState()
        }
    }

    private suspend fun resetSplicerState() {
        val clearedSlots = List<String?>(8) { null }
        val clearedQScores = List(8) { 25.0 }
        val clearedSignatures = List(8) { "TAQ" }
        _splicerSlots.value = clearedSlots
        _splicerQScores.value = clearedQScores
        _splicerSignatures.value = clearedSignatures
        repository.saveSplicerState(clearedSlots, clearedQScores, clearedSignatures)
        _isSplicing.value = false
        _splicingProgress.value = 0
        _sequencedBasesCount.value = 0
        _newlySplicedCreature.value = null
        onResetSelectedReagent?.invoke()
    }

    fun setIsForcedLoopActive(active: Boolean) {
        _isForcedLoopActive.value = active
    }

    fun startForcedConstruction(customSequence: String? = null, selectedReagent: String?) {
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

        runForcedConstructionCycle(targetSeq, isCustom, selectedReagent)
    }

    private var forcedLoopJob: Job? = null

    private fun runForcedConstructionCycle(dnaSeq: String, isCustom: Boolean, reagent: String?) {
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
            initialLogs.add("[CALCULATION] >> Base: 64 | Stock: ${currentStock.sumOf { it.count }} | Fail Chance: ${String.format("%.2f", failureChance)}%")
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

            var tempRawA = _rawStockA.value
            var tempRawG = _rawStockG.value
            var tempRawT = _rawStockT.value
            var tempRawC = _rawStockC.value

            data class StepLog(val second: Int, val text: String)
            val stepLogs = mutableListOf<StepLog>()

            val activeSplicerSlots = _splicerSlots.value
            val activeSplicerQScores = _splicerQScores.value
            var failedAtSecond = -1
            val recruitedQScores = mutableListOf<Double>()

            for (i in 0 until 8) {
                val expectedGene = dnaSeq.substring(i * 8, (i + 1) * 8)
                val isManualMatch = !isCustom && activeSplicerSlots.getOrNull(i) == expectedGene

                var scaffoldStr = ""
                var scaffoldType = ""
                var qScore = 25.0

                if (isManualMatch) {
                    scaffoldStr = expectedGene
                    scaffoldType = "PRE-ALIGNED MANUAL GENE"
                    qScore = activeSplicerQScores.getOrNull(i) ?: 25.0
                } else {
                    val matchedGene = currentStock.find { it.sequence == expectedGene && it.count > 0 }
                    if (matchedGene != null) {
                        deductFromStock(expectedGene)
                        scaffoldStr = expectedGene
                        scaffoldType = "MATCH STOCK RECRUITED"
                        qScore = matchedGene.averageQScore
                    } else {
                        val anyGene = currentStock.find { it.count > 0 }
                        if (anyGene != null) {
                            deductFromStock(anyGene.sequence)
                            scaffoldStr = anyGene.sequence
                            scaffoldType = "ANY GENE UNSTABLE BASE [${anyGene.sequence.take(4)}...]"
                            qScore = anyGene.averageQScore
                        } else {
                            scaffoldStr = "--------"
                            scaffoldType = "VOID SYNTHESIS SCAFFOLD"
                            qScore = 25.0
                        }
                    }
                }
                recruitedQScores.add(qScore)

                stepLogs.add(StepLog(minOf(7, i + 1), "Slot #${i + 1} processing using scaffold: $scaffoldType"))

                val mfe = GeneticsEngine.calculateMinimumFreeEnergy(expectedGene)
                val isFoldStalled = mfe <= -5.0
                val slotFailureChance = failureChance + (if (isFoldStalled) 10.0 else 0.0)
                if (isFoldStalled) {
                    stepLogs.add(StepLog(minOf(7, i + 1), "  ➔ [WARNING] >> Fold Stalling Penalty active (+10.0% failure chance)"))
                }

                for (j in 0 until 8) {
                    val expectedChar = expectedGene.getOrNull(j) ?: 'A'
                    val scaffoldChar = scaffoldStr.getOrNull(j) ?: '-'

                    val roll = java.util.Random().nextDouble() * 100.0
                    var charFailureChance = slotFailureChance
                    if (reagent == "REAGENT_BETAINE" && (expectedChar == 'A' || expectedChar == 'T')) {
                        charFailureChance = 0.0
                    }
                    if (reagent == "REAGENT_FORMAMIDE" && (expectedChar == 'G' || expectedChar == 'C')) {
                        charFailureChance = 0.0
                    }

                    if (scaffoldChar == expectedChar && roll >= charFailureChance) {
                        // Success
                    } else {
                        var sacrificeSuccess = false
                        var logText = ""

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
                            stepLogs.add(StepLog(minOf(7, i + 1), logText))
                        } else {
                            if (failedAtSecond == -1) {
                                failedAtSecond = i + 1
                                stepLogs.add(StepLog(i + 1, "  ➔ [FATAL] >> SPLICING PROTOCOL ABORTED: Raw nucleotide stockpile fully depleted."))
                            }
                        }
                        failureChance = maxOf(0.0, failureChance - 3.25)
                    }
                }

                if (failedAtSecond != -1) break
            }

            val maxTenths = if (failedAtSecond != -1) failedAtSecond * 10 else 80

            for (tenths in 1..maxTenths) {
                delay(100L)
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
                onSelectTab?.invoke("splicer")
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

                val clearedSlots = List<String?>(8) { null }
                val clearedQScores = List(8) { 25.0 }
                val clearedSignatures = List(8) { "TAQ" }
                _splicerSlots.value = clearedSlots
                _splicerQScores.value = clearedQScores
                _splicerSignatures.value = clearedSignatures
                repository.saveSplicerState(clearedSlots, clearedQScores, clearedSignatures)

                repository.saveRawStocks(tempRawA, tempRawG, tempRawT, tempRawC)

                val avgQ = recruitedQScores.average()
                val scaledTelomeres = (avgQ * 2.5).toInt().coerceIn(10, 100)

                val spawned = compileDeterministicOffline(dnaSeq, reagent)
                val scaledVitality = (spawned.vitality * (0.5 + 0.5 * (avgQ / 40.0))).toInt()
                val matchesTarget = getCoherence(dnaSeq, _targetSequence.value) == "full"
                val idSuffix = UUID.randomUUID().toString().take(6).uppercase()

                val cai = GeneticsEngine.calculateCodonAdaptationIndex(dnaSeq, spawned.faction)

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

                    delay(1000L)
                    if (_isForcedLoopActive.value) {
                        runForcedConstructionCycle(nextDna, isCustom, reagent)
                    } else {
                        _isReactorFrozen.value = false
                        _isForcedConstructionActive.value = false
                        addLog("FORCED LOOP ENDED. Reactor is now available for normal sequencing.")
                    }
                } else {
                    _isReactorFrozen.value = false
                    _isForcedConstructionActive.value = false
                    onSetActiveCreature?.invoke(finalCreature, "splicer")
                    onSelectTab?.invoke("splicer")

                    if (!isCustom) {
                        val nextTarget = generateRandom64Sequence()
                        repository.saveTargetSequence(nextTarget)
                    }

                    addLog("FORCED CONSTRUCTION COMPLETED. Produced sequence: \"${finalCreature.name}\"!")
                }
            }
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
            origin = "Local Compiled Unit"
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

    fun generateRandom64Sequence(): String {
        val bases = listOf('A', 'G', 'T', 'C')
        val random = java.util.Random()
        return (1..64).map { bases[random.nextInt(4)] }.joinToString("")
    }
}

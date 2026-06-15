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
import kotlinx.serialization.json.*
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

    val geminiApiKey: StateFlow<String> = repository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val muteSound: StateFlow<Boolean> = repository.muteSound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val scanRadius: StateFlow<Float> = repository.scanRadius
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 55f)

    private val _showScanner = MutableStateFlow(false)
    val showScanner: StateFlow<Boolean> = _showScanner.asStateFlow()

    // UI Local State Flow
    private val _selectedTab = MutableStateFlow("combinator")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _activeCreature = MutableStateFlow<Creature?>(null)
    val activeCreature: StateFlow<Creature?> = _activeCreature.asStateFlow()

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

    // Countdown remaining (seconds) until next synthesis cycle
    private val _idleTime = MutableStateFlow(16)
    val idleTime: StateFlow<Int> = _idleTime.asStateFlow()

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

    // Grand total nucleotide stockpile derived dynamically from standard genes in inventory (unique only)
    val grandTotalStandardNucleotides: StateFlow<Long> = geneSequences
        .map { list ->
            var total = 0L
            list.forEach { gene ->
                if (!WaveMath.isAnomalousGene(gene.sequence) && gene.count > 0) {
                    total += gene.sequence.length
                }
            }
            total
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Map location and anomalies
    private val _latitude = MutableStateFlow(37.4220) // Default Palo Alto coordinates
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(-122.0841)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    // Anomalies (Procedural nearby points)
    private val _anomalies = MutableStateFlow<List<PoxAnomaly>>(emptyList())
    val anomalies: StateFlow<List<PoxAnomaly>> = _anomalies.asStateFlow()

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
    }

    fun selectTab(tab: String) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            addLog("NAV: Transition to section [${tab.uppercase()}]")
            synthManager.playCombinatorTick()
        }
    }

    fun setActiveCreature(creature: Creature?) {
        _activeCreature.value = creature
        if (creature != null) {
            synthManager.playCreatureSequenceAudio(creature.sequence)
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

    fun addLog(log: String) {
        val time = System.currentTimeMillis() % 1000000
        _terminalLogs.value = (_terminalLogs.value + "[$time] $log").takeLast(30)
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
    }

    // Anomaly Scanner trigger
    private fun generateNearbyAnomalies(lat: Double, lng: Double) {
        val baseSeed = (lat * 1000).toInt() + (lng * 1000).toInt()
        val random = java.util.Random(baseSeed.toLong())
        val factions = listOf("Infection", "Mech", "Parasite", "Containment")
        val bases = listOf("A", "G", "T", "C")
        
        val list = mutableListOf<PoxAnomaly>()
        for (i in 0 until 5) {
            val dLat = (random.nextDouble() - 0.5) * 0.002
            val dLng = (random.nextDouble() - 0.5) * 0.002
            
            // Build an 8-character random gene
            var gene = ""
            for (j in 0 until 8) {
                gene += bases[random.nextInt(4)]
            }
            
            list.add(
                PoxAnomaly(
                    id = "ANM-$i-$baseSeed",
                    name = "Anomaly #${baseSeed % 100 + i}",
                    lat = lat + dLat,
                    lng = lng + dLng,
                    gene = gene,
                    faction = factions[random.nextInt(4)],
                    distance = (random.nextDouble() * 150) + 15,
                    heatZoneDiameter = (random.nextInt(30) + 20).toDouble()
                )
            )
        }
        _anomalies.value = list
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
            }

            if (creature != null) {
                repository.insertCreature(creature)
                // Consume 8-char blocks if they exist in inventory
                // For simplicity, compilation costs 8 random genes
                addLog("OK: Compiled creature \"${creature.name}\" of Sector [${creature.faction}]")
                synthManager.playSynthesisSuccess()
                _activeCreature.value = creature
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
            val restoredTelomeres = (creature.telomeres ?: 100) + 10
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

    // Harvest dispatch from map
    fun dispatchMission(creature: Creature, anomaly: PoxAnomaly) {
        viewModelScope.launch {
            val mission = HarvestMission(
                id = "MSN-" + UUID.randomUUID().toString().take(6),
                creatureId = creature.id,
                creatureName = creature.name,
                creatureFaction = creature.faction,
                lat = anomaly.lat,
                lng = anomaly.lng,
                startTime = System.currentTimeMillis(),
                totalDuration = 25L, // 25 seconds for instant-gratification mobile scanning
                harvestedGenes = listOf(anomaly.gene)
            )
            repository.insertMission(mission)
            addLog("DSP: Dispatched \"${creature.name}\" to harvest spot [${anomaly.gene}]")
            synthManager.playBeep(520f, 0.3f, "triangle")
        }
    }

    fun recallMission(mission: HarvestMission) {
        viewModelScope.launch {
            val updated = mission.copy(isReturned = true)
            repository.insertMission(updated)

            // Add harvested genes to inventory
            mission.harvestedGenes.forEach { gene ->
                val existing = geneSequences.value.find { it.sequence == gene }
                if (existing != null) {
                    repository.insertGeneSequence(existing.copy(count = existing.count + 1))
                } else {
                    repository.insertGeneSequence(GeneSequence(gene, 1, System.currentTimeMillis()))
                }
            }

            addLog("RCL: Mission returned. Genes cached: ${mission.harvestedGenes.joinToString()}")
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
                repository.insertCreature(creature)
                addLog("SCAN: Compiled creature \"${creature.name}\"")
                _activeCreature.value = creature
                _selectedTab.value = "vault"
            } else if (data.length == 64 && data.matches(Regex("[AGTCagtc]+"))) {
                addLog("SCAN: Scanned raw DNA sequence. Compiling...")
                compileCreature(data)
            } else if (data.length == 8 && data.matches(Regex("[AGTCagtc]+"))) {
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
                telomeres = telomeres
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

    fun setAnomalyEngineActive(active: Boolean) {
        if (active) {
            if (grandTotalStandardNucleotides.value < 250000L) {
                addLog("CANNOT ACTIVATE ANOMALY ENGINE: Requires a massive stockpile of minimum 250k total nucleotides.")
                synthManager.playReject()
                return
            }
            _anomalyEngineActive.value = true
            addLog("ANOMALY ENGINE ENGAGED! Cosmic gene hunting activated!")
            synthManager.playBeep(120f, 0.6f, "sawtooth")
        } else {
            _anomalyEngineActive.value = false
            addLog("Anomaly Engine disengaged. Reactor power normalized. Standard Bio-Lab Reactor active.")
            synthManager.playBeep(350f, 0.15f, "sine")
        }
    }

    fun triggerManualAcceleration() {
        val current = _idleTime.value
        if (current > 2) {
            _idleTime.value = current - 2
        } else {
            _idleTime.value = 1
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

            val spawned = compileDeterministicOffline(fullDNASeq)
            val idSuffix = UUID.randomUUID().toString().take(6).uppercase()
            val finalCreature = spawned.copy(
                id = "PX-$idSuffix",
                name = spawned.name,
                lore = spawned.lore,
                origin = "Spliced Gene"
            )

            repository.insertCreature(finalCreature)
            addLog("SUCCESSFUL ASSEMBLY OF ${finalCreature.name}!")
            synthManager.playSynthesisSuccess()

            _splicerSlots.value = List<String?>(8) { null }
            _isSplicing.value = false
            _splicingProgress.value = 0

            _activeCreature.value = finalCreature
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
                val idSuffix = UUID.randomUUID().toString().take(6).uppercase()
                val finalCreature = spawned.copy(
                    id = "PX-$idSuffix",
                    name = "${spawned.name} [FORCED]",
                    lore = "This hybrid genome was forced together in the bio-lab reactor. ${spawned.lore}",
                    origin = "Forced Synthesis"
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
                    
                    // Decrement countdown
                    val currentIdle = _idleTime.value
                    var nextVal = currentIdle - 1
                    if (isBoostActive && currentIdle > 8) {
                        nextVal = 8
                    }
                    
                    if (nextVal <= 0) {
                        if (_anomalyEngineActive.value && grandTotalStandardNucleotides.value >= 250000L) {
                            triggerAnomalousSynthesis()
                        } else {
                            if (_anomalyEngineActive.value) {
                                _anomalyEngineActive.value = false
                                addLog("ANOMALY ENGINE SHUT DOWN: Nucleotide reserves fell below minimum 250k threshold.")
                            }
                            triggerStandardSynthesis()
                        }
                        nextVal = resetVal
                    }
                    
                    _idleTime.value = nextVal
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

        // Save each gene to database
        batch.forEach { sequence ->
            val match = currentList.find { it.sequence == sequence }
            if (match != null) {
                repository.insertGeneSequence(match.copy(count = match.count + 1))
            } else {
                newGenesList.add(sequence)
                repository.insertGeneSequence(GeneSequence(sequence, 1, System.currentTimeMillis()))
            }
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
        for (gene in standardGenes) {
            if (remaining <= 0) break
            val toTake = Math.min(gene.count, remaining)
            remaining -= toTake
            val newCount = gene.count - toTake
            if (newCount <= 0) {
                repository.deleteGeneSequence(gene)
            } else {
                repository.insertGeneSequence(gene.copy(count = newCount))
            }
        }
    }

    fun clearDiscoveredPacketsLog() {
        _discoveredPacketsLog.value = emptyList()
        addLog("GENE ARCHIVE: Cleared all packet records.")
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
    val heatZoneDiameter: Double
)

class MainViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

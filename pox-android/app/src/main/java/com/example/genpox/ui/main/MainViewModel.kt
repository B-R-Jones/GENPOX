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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val geneSequences: StateFlow<List<GeneSequence>> = repository.allGeneSequences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMissions: StateFlow<List<HarvestMission>> = repository.activeMissions
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
                _selectedTab.value = "library"
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
                _selectedTab.value = "library"
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

    private var heartbeatJob: kotlinx.coroutines.Job? = null
    private var scrollingTickerJob: kotlinx.coroutines.Job? = null

    private fun startReactorHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                
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

        // Save each gene to database
        batch.forEach { sequence ->
            val currentList = geneSequences.value
            val match = currentList.find { it.sequence == sequence }
            if (match != null) {
                repository.insertGeneSequence(match.copy(count = match.count + 1))
            } else {
                repository.insertGeneSequence(GeneSequence(sequence, 1, System.currentTimeMillis()))
            }
        }

        // Save batch packet log
        val packet = GenePacket(
            id = "PKT-" + UUID.randomUUID().toString().take(6).uppercase(),
            genes = batch,
            timestamp = System.currentTimeMillis(),
            isAnomalous = false
        )
        _discoveredPacketsLog.value = (listOf(packet) + _discoveredPacketsLog.value).take(50)

        addLog("REACTOR SYNTHESIS: 8 standard genes successfully generated.")
        synthManager.playCombinatorTick()
    }

    private suspend fun triggerAnomalousSynthesis() {
        val grandTotal = grandTotalStandardNucleotides.value
        val coupling = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
        val chanceMetrics = WaveMath.getAnomalyEngineSuccessChance(grandTotal, coupling)
        val roll = java.util.Random().nextDouble() * 100.0

        // Consume standard nucleotides
        consumeNucleotides(10000)

        val isSuccess = roll <= chanceMetrics.finalChance
        val generated = if (isSuccess) {
            val anomalousGene = WaveMath.generateAnomalousGene()
            val currentList = geneSequences.value
            val match = currentList.find { it.sequence == anomalousGene }
            if (match != null) {
                repository.insertGeneSequence(match.copy(count = match.count + 1))
            } else {
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
            isAnomalous = true
        )
        _discoveredPacketsLog.value = (listOf(packet) + _discoveredPacketsLog.value).take(50)

        if (isSuccess) {
            addLog("[ANOMALY ENGINE] Unstable Fusion Success! Formed anomalous block $generated.")
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
}

data class GenePacket(
    val id: String,
    val genes: List<String>,
    val timestamp: Long,
    val isAnomalous: Boolean
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

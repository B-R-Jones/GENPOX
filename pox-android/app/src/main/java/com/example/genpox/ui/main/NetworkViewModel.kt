package com.example.genpox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import com.example.genpox.data.network.HydraPacket
import com.example.genpox.data.repository.*
import com.example.genpox.util.DispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val creatureRepository: CreatureRepository,
    private val geneStockRepository: GeneStockRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val synthManager: PoxSynthManager,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {
    val reactorSalt: StateFlow<Float> = MutableStateFlow(0.05f)

    var onLog: ((String) -> Unit)? = null

    private fun addLog(message: String) {
        onLog?.invoke(message)
    }

    private var connectionsJob: kotlinx.coroutines.Job? = null

    var networkManager: com.example.genpox.data.network.PoxNetworkInterface? = null
        set(value) {
            field = value
            if (value != null) {
                observeConnections(value)
            } else {
                connectionsJob?.cancel()
            }
        }

    private fun observeConnections(nm: com.example.genpox.data.network.PoxNetworkInterface) {
        connectionsJob?.cancel()
        connectionsJob = viewModelScope.launch {
            var previousConnections = emptyMap<String, String>()
            nm.activeConnections.collect { currentConnections ->
                val newlyAdded = currentConnections.keys - previousConnections.keys
                previousConnections = currentConnections

                if (newlyAdded.isNotEmpty()) {
                    newlyAdded.forEach { endpointId ->
                        triggerPassiveHackIfPossible(nm, endpointId)
                    }
                }
            }
        }
    }

    private fun triggerPassiveHackIfPossible(nm: com.example.genpox.data.network.PoxNetworkInterface, endpointId: String) {
        viewModelScope.launch {
            val autoHacker = creatureRepository.allCreatures.first().find { it.isAutoHacker && it.telomeres > 0 }
            if (autoHacker != null) {
                val targetNodeId = nm.activeConnections.value[endpointId] ?: "Remote Node"
                val intrusionPacket = HydraPacket.HackingIntrusion(
                    hacker = autoHacker,
                    targetNodeId = targetNodeId,
                )
                nm.sendPacket(endpointId, intrusionPacket)
                addLog("HACK: Designated Auto-Hacker \"${autoHacker.name}\" initiated passive intrusion on $targetNodeId.")
            }
        }
    }

    // State Variables
    private val _inboxMessages = MutableStateFlow<List<MailMessage>>(emptyList())
    val inboxMessages: StateFlow<List<MailMessage>> = _inboxMessages.asStateFlow()

    private val _selectedInboxMessageId = MutableStateFlow<String?>(null)
    val selectedInboxMessageId: StateFlow<String?> = _selectedInboxMessageId.asStateFlow()

    private val _registeredPeers = MutableStateFlow<List<PeerNode>>(emptyList())
    val registeredPeers: StateFlow<List<PeerNode>> = _registeredPeers.asStateFlow()

    private val _isNetworkSimulationActive = MutableStateFlow(false)
    val isNetworkSimulationActive: StateFlow<Boolean> = _isNetworkSimulationActive.asStateFlow()

    private val _composerRecipientId = MutableStateFlow<String?>(null)
    val composerRecipientId: StateFlow<String?> = _composerRecipientId.asStateFlow()

    private val _composerRecipientName = MutableStateFlow<String?>(null)
    val composerRecipientName: StateFlow<String?> = _composerRecipientName.asStateFlow()

    private val _composerRecipientIsConnected = MutableStateFlow(false)
    val composerRecipientIsConnected: StateFlow<Boolean> = _composerRecipientIsConnected.asStateFlow()

    private val _composerMessageText = MutableStateFlow("")
    val composerMessageText: StateFlow<String> = _composerMessageText.asStateFlow()

    private val _composerAttachedCreature = MutableStateFlow<Creature?>(null)
    val composerAttachedCreature: StateFlow<Creature?> = _composerAttachedCreature.asStateFlow()

    private val _composerAttachedGene = MutableStateFlow<GeneSequence?>(null)
    val composerAttachedGene: StateFlow<GeneSequence?> = _composerAttachedGene.asStateFlow()

    private val _composerTransferResources = MutableStateFlow(false)
    val composerTransferResources: StateFlow<Boolean> = _composerTransferResources.asStateFlow()

    // Local stocks variables
    private val _rawStockA = MutableStateFlow(10000L)
    private val _rawStockG = MutableStateFlow(10000L)
    private val _rawStockT = MutableStateFlow(10000L)
    private val _rawStockC = MutableStateFlow(10000L)
    private val _bioWaste = MutableStateFlow(0L)
    private val _reactorSalt = MutableStateFlow(0.05f)

    init {
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
            reactorSalt.collect { _reactorSalt.value = it }
        }
    }

    fun selectInboxMessage(id: String?) {
        _selectedInboxMessageId.value = id
        if (id != null) {
            val list = _inboxMessages.value.map { m ->
                if (m.id == id) m.copy(isRead = true) else m
            }
            _inboxMessages.value = list
            synthManager.playCombinatorTick()
        }
    }

    fun toggleNetworkSimulation(enabled: Boolean) {
        _isNetworkSimulationActive.value = enabled
        synthManager.playCombinatorTick()
        addLog("NET: Simulated proximity transceiver set to ${if (enabled) "ACTIVE" else "STANDBY"}")
    }

    fun registerPeerNode(name: String, sector: String) {
        val nextId = "peer_${_registeredPeers.value.size + 1}"
        val newPeer = PeerNode(nextId, name, sector, "---", "ONLINE")
        _registeredPeers.value = _registeredPeers.value + newPeer
        addLog("NET: Registered remote transponder terminal [${newPeer.name}].")
        synthManager.playSynthesisSuccess()
    }

    fun markMessageAsRead(messageId: String) {
        val list = _inboxMessages.value.map { m ->
            if (m.id == messageId) m.copy(isRead = true) else m
        }
        _inboxMessages.value = list
    }

    fun claimMessageAttachment(messageId: String) {
        val msg = _inboxMessages.value.find { it.id == messageId } ?: return
        if (msg.isClaimed) return

        viewModelScope.launch {
            if (msg.attachedCreatureDna != null && msg.attachedCreatureName != null) {
                val newCreature = Creature(
                    id = UUID.randomUUID().toString(),
                    sequence = msg.attachedCreatureDna,
                    name = msg.attachedCreatureName,
                    faction = msg.attachedCreatureFaction ?: "Natural",
                    type = "Spliced Hybrid",
                    vitality = (80..180).random(),
                    attack = (10..40).random(),
                    defense = (10..40).random(),
                    speed = (10..40).random(),
                    primaryWeapon = "Organic Spikes",
                    lore = "Received via peer network transmission from ${msg.from}.",
                    asciiArt = " [P2P CODON LINK] ",
                    discoveredAt = System.currentTimeMillis(),
                    origin = msg.from,
                )
                creatureRepository.insertCreature(newCreature)
            }

            if (msg.attachedGeneSequence != null) {
                val tm = GeneticsEngine.calculateMeltingTemperature(msg.attachedGeneSequence, _reactorSalt.value.toDouble())
                val mfe = GeneticsEngine.calculateMinimumFreeEnergy(msg.attachedGeneSequence)
                val newSeq = GeneSequence(
                    sequence = msg.attachedGeneSequence,
                    count = 1,
                    discoveredAt = System.currentTimeMillis(),
                    averageQScore = 30.0,
                    meltingTemp = tm,
                    minimumFreeEnergy = mfe,
                )
                geneStockRepository.insertGeneSequence(newSeq)
            }

            if (msg.transferGenes > 0 || msg.transferWaste > 0) {
                val newA = _rawStockA.value + msg.transferGenes
                val newG = _rawStockG.value + msg.transferGenes
                val newT = _rawStockT.value + msg.transferGenes
                val newC = _rawStockC.value + msg.transferGenes
                _rawStockA.value = newA
                _rawStockG.value = newG
                _rawStockT.value = newT
                _rawStockC.value = newC
                preferencesRepository.saveRawStocks(newA, newG, newT, newC)

                val newWaste = _bioWaste.value + msg.transferWaste
                _bioWaste.value = newWaste
                preferencesRepository.saveBioWaste(newWaste)
            }

            val updated = _inboxMessages.value.map { m ->
                if (m.id == messageId) m.copy(isClaimed = true, isRead = true) else m
            }
            _inboxMessages.value = updated
            synthManager.playSynthesisSuccess()
        }
    }

    fun receiveIncomingPacket(endpointId: String, peerName: String, packet: HydraPacket) {
        when (packet) {
            is HydraPacket.HackingIntrusion -> {
                viewModelScope.launch {
                    val allCreatures = creatureRepository.allCreatures.first()
                    val autoDefenders = allCreatures.filter { it.isAutoDefender && it.telomeres > 0 }
                    val primaryDefender = autoDefenders.firstOrNull()

                    addLog("SECURITY: Hacking intrusion detected from $peerName using hacker spec \"${packet.hacker.name}\". Defender assigned: ${primaryDefender?.name ?: "NONE"}.")

                    val responsePacket = HydraPacket.HackingCounterResponse(
                        targetNodeId = packet.targetNodeId,
                        defender = primaryDefender,
                        vaultAutoDefenders = autoDefenders,
                        baseFirewallComplexity = 50.0,
                    )
                    networkManager?.sendPacket(endpointId, responsePacket)
                }
            }
            is HydraPacket.HackingCounterResponse -> {
                viewModelScope.launch {
                    val autoHacker = creatureRepository.allCreatures.first().find { it.isAutoHacker && it.telomeres > 0 }
                    if (autoHacker == null) {
                        addLog("HACK ERR: Active Auto-Hacker not found for counter response.")
                        return@launch
                    }

                    val waveConfig = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
                    val waveCouplingVal = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
                    val hackingResult = ConflictEngine.simulateCreatureVsPlayerNode(
                        hacker = autoHacker,
                        targetNodeId = packet.targetNodeId,
                        defender = packet.defender,
                        vaultAutoDefenders = packet.vaultAutoDefenders,
                        baseFirewallComplexity = packet.baseFirewallComplexity,
                        phaseName = waveConfig.phaseName,
                        waveCoupling = waveCouplingVal,
                        seed = System.currentTimeMillis(),
                    )

                    val updatedHacker = autoHacker.copy(telomeres = hackingResult.finalHackerTelomeres)
                    creatureRepository.insertCreature(updatedHacker)

                    if (hackingResult.success && hackingResult.siphonedPayload.isNotEmpty()) {
                        hackingResult.siphonedPayload.forEach { gene ->
                            val tm = GeneticsEngine.calculateMeltingTemperature(gene, _reactorSalt.value.toDouble())
                            val mfe = GeneticsEngine.calculateMinimumFreeEnergy(gene)
                            val newSeq = GeneSequence(
                                sequence = gene,
                                count = 1,
                                discoveredAt = System.currentTimeMillis(),
                                averageQScore = 35.0,
                                meltingTemp = tm,
                                minimumFreeEnergy = mfe,
                            )
                            geneStockRepository.insertGeneSequence(newSeq)
                            addLog("HACK: Siphoned gene sequence $gene from target $peerName and saved to database.")
                        }
                    }

                    val defenderVal = packet.defender
                    val defenderFinalTelomeres = if (defenderVal != null) {
                        val combat = hackingResult.directCombatResult
                        if (combat != null) {
                            val maxHp = combat.logs.find { it.contains("Defender effective stats: HP ") }
                                ?.substringAfter("HP ")?.substringBefore(",")?.toIntOrNull() ?: defenderVal.telomeres
                            if (combat.finalDefenderHp <= 0) {
                                maxOf(0, defenderVal.telomeres - 20)
                            } else if (combat.finalDefenderHp < maxHp) {
                                maxOf(0, defenderVal.telomeres - 10)
                            } else {
                                defenderVal.telomeres
                            }
                        } else {
                            defenderVal.telomeres
                        }
                    } else {
                        null
                    }

                    val reportPacket = HydraPacket.HackingResultReport(
                        targetNodeId = packet.targetNodeId,
                        success = hackingResult.success,
                        logs = hackingResult.logs,
                        siphonedPayload = hackingResult.siphonedPayload,
                        hackerFinalTelomeres = hackingResult.finalHackerTelomeres,
                        defenderFinalTelomeres = defenderFinalTelomeres,
                    )
                    networkManager?.sendPacket(endpointId, reportPacket)

                    addLog("HACK RESULT: Hacking on $peerName was a ${if (hackingResult.success) "SUCCESS" else "FAILURE"}. Final Hacker Telomeres: ${hackingResult.finalHackerTelomeres}%.")
                    hackingResult.logs.forEach { addLog("  $it") }
                }
            }
            is HydraPacket.HackingResultReport -> {
                viewModelScope.launch {
                    addLog("SECURITY REPORT: Node security breach assessment on ${packet.targetNodeId} by $peerName.")
                    addLog("  Result: ${if (packet.success) "BREACHED" else "CONTAINED"}")
                    packet.logs.forEach { addLog("  $it") }

                    if (packet.defenderFinalTelomeres != null) {
                        val defender = creatureRepository.allCreatures.first().find { it.isAutoDefender && it.telomeres > 0 }
                        if (defender != null) {
                            val updatedDefender = defender.copy(telomeres = packet.defenderFinalTelomeres)
                            creatureRepository.insertCreature(updatedDefender)
                            addLog("SECURITY: Defender \"${defender.name}\" telomeres updated to ${packet.defenderFinalTelomeres}% after combat.")
                            if (packet.defenderFinalTelomeres <= 0) {
                                addLog("FATAL: Defender \"${defender.name}\" disintegrated due to total telomere decay during security defense.")
                            }
                        }
                    }
                }
            }
            is HydraPacket.NetworkMessage -> {
                val text = packet.messageText
                val subject = when {
                    packet.attachedCreatureDna != null -> "CREATURE SECURED TRANSACTION"
                    packet.attachedGeneSequence != null -> "GENE SEQUENCE TRANSFERRED"
                    packet.transferGenes > 0 -> "RESOURCES DISPATCH RECEIVED"
                    else -> "PEER UPLINK MESSAGE"
                }

                val bodyBuilder = java.lang.StringBuilder()
                bodyBuilder.append("PAYLOAD MESSAGE:\n\"$text\"\n\n")
                if (packet.attachedCreatureDna != null) {
                    bodyBuilder.append("ATTACHED HYBRID: ${packet.attachedCreatureName} [FACTION: ${packet.attachedCreatureFaction}]\n")
                }
                if (packet.attachedGeneSequence != null) {
                    bodyBuilder.append("ATTACHED CODON BLOCK: ${packet.attachedGeneSequence}\n")
                }
                if (packet.transferGenes > 0 || packet.transferWaste > 0) {
                    bodyBuilder.append("ATTACHED RESOURCES:\n")
                    if (packet.transferGenes > 0) bodyBuilder.append("  +${packet.transferGenes} Stock of all Bases (A, G, T, C)\n")
                    if (packet.transferWaste > 0) bodyBuilder.append("  +${packet.transferWaste} Bio-Waste stockpile units\n")
                }

                val newMsg = MailMessage(
                    id = "msg_recv_${System.currentTimeMillis()}",
                    from = peerName,
                    subject = subject,
                    timestamp = "Just now",
                    body = bodyBuilder.toString(),
                    isRead = false,
                    attachedCreatureDna = packet.attachedCreatureDna,
                    attachedCreatureName = packet.attachedCreatureName,
                    attachedCreatureFaction = packet.attachedCreatureFaction,
                    attachedGeneSequence = packet.attachedGeneSequence,
                    transferGenes = packet.transferGenes,
                    transferWaste = packet.transferWaste,
                )

                _inboxMessages.value = listOf(newMsg) + _inboxMessages.value
                synthManager.playSynthesisSuccess()
            }
            else -> {}
        }
    }

    fun transmitP2PMessage(
        endpointId: String,
        text: String,
        creature: Creature?,
        gene: GeneSequence?,
        transferResources: Boolean,
    ) {
        val nm = networkManager ?: return

        viewModelScope.launch {
            var transGenes = 0
            var transWaste = 0

            if (transferResources) {
                if (_rawStockA.value >= 10 &&
                    _rawStockG.value >= 10 &&
                    _rawStockT.value >= 10 &&
                    _rawStockC.value >= 10 &&
                    _bioWaste.value >= 25
                ) {
                    val newA = _rawStockA.value - 10
                    val newG = _rawStockG.value - 10
                    val newT = _rawStockT.value - 10
                    val newC = _rawStockC.value - 10
                    _rawStockA.value = newA
                    _rawStockG.value = newG
                    _rawStockT.value = newT
                    _rawStockC.value = newC
                    preferencesRepository.saveRawStocks(newA, newG, newT, newC)

                    val newWaste = _bioWaste.value - 25
                    _bioWaste.value = newWaste
                    preferencesRepository.saveBioWaste(newWaste)

                    transGenes = 10
                    transWaste = 25
                }
            }

            val packet = HydraPacket.NetworkMessage(
                senderName = "AGENT-NODE",
                messageText = text.take(64),
                attachedCreatureDna = creature?.sequence,
                attachedCreatureName = creature?.name,
                attachedCreatureFaction = creature?.faction,
                attachedGeneSequence = gene?.sequence,
                transferGenes = transGenes,
                transferWaste = transWaste,
            )

            if (gene != null) {
                geneStockRepository.deleteGeneSequence(gene)
            }

            nm.sendPacket(endpointId, packet)
            synthManager.playBeep(880f, 0.08f, "square")
        }
    }

    fun setComposerRecipient(id: String?, name: String?, isConnected: Boolean) {
        _composerRecipientId.value = id
        _composerRecipientName.value = name
        _composerRecipientIsConnected.value = isConnected
    }

    fun setComposerMessageText(text: String) {
        _composerMessageText.value = text
    }

    fun setComposerAttachedCreature(creature: Creature?) {
        _composerAttachedCreature.value = creature
    }

    fun setComposerAttachedGene(gene: GeneSequence?) {
        _composerAttachedGene.value = gene
    }

    fun setComposerTransferResources(active: Boolean) {
        _composerTransferResources.value = active
    }

    fun clearComposer() {
        _composerRecipientId.value = null
        _composerRecipientName.value = null
        _composerRecipientIsConnected.value = false
        _composerMessageText.value = ""
        _composerAttachedCreature.value = null
        _composerAttachedGene.value = null
        _composerTransferResources.value = false
    }
}

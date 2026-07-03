package com.example.genpox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import com.example.genpox.data.network.PoxNetworkManager
import com.example.genpox.data.network.HydraPacket
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class NetworkViewModel(
    private val repository: DataRepository,
    private val synthManager: PoxSynthManager,
    val reactorSalt: StateFlow<Float>
) : ViewModel() {

    var onLog: ((String) -> Unit)? = null

    private fun addLog(message: String) {
        onLog?.invoke(message)
    }

    var networkManager: PoxNetworkManager? = null

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
                    origin = msg.from
                )
                repository.insertCreature(newCreature)
            }

            if (msg.attachedGeneSequence != null) {
                val tm = BiophysicsEngine.calculateMeltingTemperature(msg.attachedGeneSequence, _reactorSalt.value.toDouble())
                val mfe = BiophysicsEngine.calculateMinimumFreeEnergy(msg.attachedGeneSequence)
                val newSeq = GeneSequence(
                    sequence = msg.attachedGeneSequence,
                    count = 1,
                    discoveredAt = System.currentTimeMillis(),
                    averageQScore = 30.0,
                    meltingTemp = tm,
                    minimumFreeEnergy = mfe
                )
                repository.insertGeneSequence(newSeq)
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
                repository.saveRawStocks(newA, newG, newT, newC)

                val newWaste = _bioWaste.value + msg.transferWaste
                _bioWaste.value = newWaste
                repository.saveBioWaste(newWaste)
            }

            val updated = _inboxMessages.value.map { m ->
                if (m.id == messageId) m.copy(isClaimed = true, isRead = true) else m
            }
            _inboxMessages.value = updated
            synthManager.playSynthesisSuccess()
        }
    }

    fun receiveIncomingPacket(peerName: String, packet: HydraPacket) {
        if (packet is HydraPacket.NetworkMessage) {
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
                transferWaste = packet.transferWaste
            )

            _inboxMessages.value = listOf(newMsg) + _inboxMessages.value
            synthManager.playSynthesisSuccess()
        }
    }

    fun transmitP2PMessage(
        endpointId: String,
        text: String,
        creature: Creature?,
        gene: GeneSequence?,
        transferResources: Boolean
    ) {
        val nm = networkManager ?: return

        viewModelScope.launch {
            var transGenes = 0
            var transWaste = 0

            if (transferResources) {
                if (_rawStockA.value >= 10 && _rawStockG.value >= 10 &&
                    _rawStockT.value >= 10 && _rawStockC.value >= 10 &&
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
                    repository.saveRawStocks(newA, newG, newT, newC)

                    val newWaste = _bioWaste.value - 25
                    _bioWaste.value = newWaste
                    repository.saveBioWaste(newWaste)

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
                transferWaste = transWaste
            )

            if (gene != null) {
                repository.deleteGeneSequence(gene)
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

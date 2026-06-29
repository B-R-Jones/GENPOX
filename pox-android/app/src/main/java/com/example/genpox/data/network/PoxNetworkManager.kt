package com.example.genpox.data.network

import android.content.Context
import android.util.Log
import com.example.genpox.audio.PoxSynthManager
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
sealed class HydraPacket {
    @Serializable
    data class Beacon(
        val nodeName: String,
        val sector: String,
        val appVersion: Int,
        val publicKey: String
    ) : HydraPacket()

    @Serializable
    data class Ping(val timestamp: Long) : HydraPacket()

    @Serializable
    data class SpecimenOffer(
        val transactionId: String,
        val creatureDna: String,
        val creatureName: String,
        val faction: String,
        val signature: String
    ) : HydraPacket()

    @Serializable
    data class SpecimenResponse(
        val transactionId: String,
        val accept: Boolean
    ) : HydraPacket()

    @Serializable
    @SerialName("message")
    data class NetworkMessage(
        val senderName: String,
        val messageText: String,
        val attachedCreatureDna: String? = null,
        val attachedCreatureName: String? = null,
        val attachedCreatureFaction: String? = null,
        val attachedGeneSequence: String? = null,
        val transferGenes: Int = 0,
        val transferWaste: Int = 0
    ) : HydraPacket()
}

class PoxNetworkManager(
    private val context: Context,
    private val synthManager: PoxSynthManager
) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.example.genpox.HYDRA_NET"

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var isSimulationActive = false
        private set

    fun toggleSimulation(active: Boolean) {
        isSimulationActive = active
        log("SIMULATOR: Loopback daemon state set to: ${if (active) "ACTIVE" else "OFFLINE"}")
        if (!active) {
            val currentConnections = _activeConnections.value.toMutableMap()
            val mockIds = currentConnections.keys.filter { it.startsWith("MOCK-") }
            mockIds.forEach { id ->
                simulateDisconnected(id)
            }
            _discoveredPeers.value = _discoveredPeers.value.filterNot { it.endpointId.startsWith("MOCK-") }
        }
    }

    private fun simulateConnectionInitiated(endpointId: String, name: String) {
        log("HANDSHAKE REQ: Connection handshake initiated from $name")
        pendingConnectionNames[endpointId] = name
        resetInactivityTimeout()
        mainScope.launch {
            delay(800)
            simulateConnectionResult(endpointId, ConnectionsStatusCodes.STATUS_OK)
        }
    }

    private fun simulateConnectionResult(endpointId: String, statusCode: Int) {
        if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
            val name = pendingConnectionNames.remove(endpointId) ?: "Secured Node Link"
            log("LINK SECURED: Connection fully established with $name ($endpointId)")
            val currentConnections = _activeConnections.value.toMutableMap()
            currentConnections[endpointId] = name
            _activeConnections.value = currentConnections
            synthManager.playSynthesisSuccess()
            resetInactivityTimeout()
        } else {
            pendingConnectionNames.remove(endpointId)
            log("LINK REFUSED: Peer rejected channel handshake.")
            synthManager.playReject()
            resetInactivityTimeout()
        }
    }

    private fun simulateDisconnected(endpointId: String) {
        log("LINK BROKEN: Disconnected from peer endpoint $endpointId")
        val currentConnections = _activeConnections.value.toMutableMap()
        currentConnections.remove(endpointId)
        _activeConnections.value = currentConnections
        synthManager.playBeep(220f, 0.2f, "sawtooth")
        resetInactivityTimeout()
    }

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers

    private val _activeConnections = MutableStateFlow<Map<String, String>>(emptyMap()) // EndpointId -> PeerName
    val activeConnections: StateFlow<Map<String, String>> = _activeConnections

    private val _incomingPackets = MutableSharedFlow<Pair<String, HydraPacket>>(extraBufferCapacity = 16)
    val incomingPackets: SharedFlow<Pair<String, HydraPacket>> = _incomingPackets.asSharedFlow()

    private val _connectionLogs = MutableStateFlow<List<String>>(emptyList())
    val connectionLogs: StateFlow<List<String>> = _connectionLogs

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering

    data class DiscoveredPeer(
        val endpointId: String,
        val name: String,
        val info: String
    )

    private fun log(message: String) {
        val currentLogs = _connectionLogs.value.toMutableList()
        currentLogs.add("[$serviceId] $message")
        _connectionLogs.value = currentLogs.takeLast(100)
    }

    private var inactivityTimeoutJob: Job? = null

    private fun resetInactivityTimeout() {
        inactivityTimeoutJob?.cancel()
        if (_isAdvertising.value || _isDiscovering.value) {
            if (_activeConnections.value.isNotEmpty()) {
                return
            }
            inactivityTimeoutJob = mainScope.launch {
                delay(120_000)
                log("TIMEOUT: Radio shutdown. No active connections or beacon activity for 120s.")
                stopAllEndpoints()
            }
        }
    }

    fun startAdvertising(localPlayerName: String) {
        if (isSimulationActive) {
            _isAdvertising.value = true
            log("SYS: Beacon active (Simulated). Broadcasting Node ID: $localPlayerName")
            synthManager.playBeep(880f, 0.1f, "square")
            resetInactivityTimeout()
            return
        }

        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startAdvertising(
            localPlayerName,
            serviceId,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            _isAdvertising.value = true
            log("SYS: Beacon active. Broadcasting Node ID: $localPlayerName")
            synthManager.playBeep(880f, 0.1f, "square")
            resetInactivityTimeout()
        }.addOnFailureListener { e ->
            _isAdvertising.value = false
            log("ERR: Advertising failure. Code: ${e.message}")
        }
    }

    fun startDiscovery() {
        if (isSimulationActive) {
            _isDiscovering.value = true
            log("SYS: Scan loop activated (Simulated). Searching for peer beacons...")
            synthManager.playBeep(520f, 0.08f, "sine")
            resetInactivityTimeout()
            mainScope.launch {
                delay(1500)
                if (_isDiscovering.value && isSimulationActive) {
                    val mockPeers = listOf(
                        DiscoveredPeer("MOCK-PEER-1", "MOCK-PEER-ALPHA", "P2P Segment"),
                        DiscoveredPeer("MOCK-PEER-2", "MOCK-PEER-BETA", "P2P Segment")
                    )
                    val currentPeers = _discoveredPeers.value.toMutableList()
                    mockPeers.forEach { peer ->
                        if (currentPeers.none { it.endpointId == peer.endpointId }) {
                            currentPeers.add(peer)
                        }
                    }
                    _discoveredPeers.value = currentPeers
                    synthManager.playBeep(784f, 0.05f, "sine")
                    log("NODE FOUND: [MOCK-PEER-ALPHA] (ID: MOCK-PEER-1)")
                    log("NODE FOUND: [MOCK-PEER-BETA] (ID: MOCK-PEER-2)")
                    resetInactivityTimeout()
                }
            }
            return
        }

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            _isDiscovering.value = true
            log("SYS: Scan loop activated. Searching for peer beacons...")
            synthManager.playBeep(520f, 0.08f, "sine")
            resetInactivityTimeout()
        }.addOnFailureListener { e ->
            _isDiscovering.value = false
            log("ERR: Scan activation failed: ${e.message}")
        }
    }

    fun stopAllEndpoints() {
        inactivityTimeoutJob?.cancel()
        if (isSimulationActive) {
            _isAdvertising.value = false
            _isDiscovering.value = false
            val currentConnections = _activeConnections.value.toMutableMap()
            val mockIds = currentConnections.keys.toList()
            mockIds.forEach { id ->
                simulateDisconnected(id)
            }
            _discoveredPeers.value = emptyList()
            _activeConnections.value = emptyMap()
            log("SYS: All network interfaces shutdown (Simulated). Radio off.")
            synthManager.playBeep(330f, 0.15f, "triangle")
            return
        }

        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _isAdvertising.value = false
        _isDiscovering.value = false
        _discoveredPeers.value = emptyList()
        _activeConnections.value = emptyMap()
        log("SYS: All network interfaces shutdown. Radio off.")
        synthManager.playBeep(330f, 0.15f, "triangle")
    }


    fun requestConnection(endpointId: String, localPlayerName: String) {
        if (isSimulationActive && endpointId.startsWith("MOCK-")) {
            log("DIAL: Requesting simulated link connection to endpoint $endpointId...")
            val name = if (endpointId == "MOCK-PEER-1") "MOCK-PEER-ALPHA" else "MOCK-PEER-BETA"
            mainScope.launch {
                delay(1000)
                simulateConnectionInitiated(endpointId, name)
            }
            return
        }

        connectionsClient.requestConnection(
            localPlayerName,
            endpointId,
            connectionLifecycleCallback
        ).addOnFailureListener { e ->
            log("ERR: Direct dial failure to endpoint: ${e.message}")
        }
    }

    fun sendPacket(endpointId: String, packet: HydraPacket) {
        resetInactivityTimeout()
        if (isSimulationActive && endpointId.startsWith("MOCK-")) {
            log("PACKET SENT: Transmitted package to $endpointId")
            if (packet is HydraPacket.Ping) {
                mainScope.launch {
                    delay(60)
                    val returnPacket = HydraPacket.Ping(packet.timestamp)
                    val jsonStr = Json.encodeToString<HydraPacket>(returnPacket)
                    log("PACKET RECV: Decrypting package of ${jsonStr.toByteArray().size} bytes")
                    handleIncomingPacket(endpointId, jsonStr)
                    val rtt = System.currentTimeMillis() - packet.timestamp
                    log("PING: Loopback echo resolved successfully in ${rtt}ms.")
                }
            } else if (packet is HydraPacket.NetworkMessage) {
                mainScope.launch {
                    delay(2000)
                    val mockReply = HydraPacket.NetworkMessage(
                        senderName = "MOCK-PEER-ALPHA",
                        messageText = "ECHO RESOLVED. SENSOR MATRIX SHOWS OPTIMAL LINK COHERENCE.",
                        attachedGeneSequence = "GATTACA",
                        transferGenes = 10,
                        transferWaste = 25
                    )
                    val jsonStr = Json.encodeToString<HydraPacket>(mockReply)
                    log("PACKET RECV: Decrypting simulated response packet")
                    handleIncomingPacket(endpointId, jsonStr)
                }
            }
            return
        }

        try {
            val jsonStr = Json.encodeToString(packet)
            val bytes = jsonStr.toByteArray()
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
                .addOnSuccessListener {
                    log("PACKET SENT: Transmitted package to $endpointId")
                }
                .addOnFailureListener { e ->
                    log("ERR: Transmission failure: ${e.message}")
                }
        } catch (e: Exception) {
            log("ERR: Serialization failure: ${e.message}")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            log("NODE FOUND: [${info.endpointName}] (ID: $endpointId)")
            val currentPeers = _discoveredPeers.value.toMutableList()
            if (currentPeers.none { it.endpointId == endpointId }) {
                currentPeers.add(DiscoveredPeer(endpointId, info.endpointName, "P2P Segment"))
                _discoveredPeers.value = currentPeers
            }
            synthManager.playBeep(784f, 0.05f, "sine")
            resetInactivityTimeout()
        }

        override fun onEndpointLost(endpointId: String) {
            log("NODE LOST: (ID: $endpointId)")
            _discoveredPeers.value = _discoveredPeers.value.filterNot { it.endpointId == endpointId }
            resetInactivityTimeout()
        }
    }

    private val pendingConnectionNames = mutableMapOf<String, String>()

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            log("HANDSHAKE REQ: Connection handshake initiated from ${info.endpointName}")
            pendingConnectionNames[endpointId] = info.endpointName
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            resetInactivityTimeout()
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    val name = pendingConnectionNames.remove(endpointId) ?: "Secured Node Link"
                    log("LINK SECURED: Connection fully established with $name ($endpointId)")
                    val currentConnections = _activeConnections.value.toMutableMap()
                    currentConnections[endpointId] = name
                    _activeConnections.value = currentConnections
                    synthManager.playSynthesisSuccess()
                    resetInactivityTimeout()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    pendingConnectionNames.remove(endpointId)
                    log("LINK REFUSED: Peer rejected channel handshake.")
                    synthManager.playReject()
                    resetInactivityTimeout()
                }
                else -> {
                    pendingConnectionNames.remove(endpointId)
                    log("ERR: Connection handshake broke. Code: ${result.status.statusCode}")
                    resetInactivityTimeout()
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            log("LINK BROKEN: Disconnected from peer endpoint $endpointId")
            val currentConnections = _activeConnections.value.toMutableMap()
            val name = currentConnections.remove(endpointId) ?: "endpoint"
            _activeConnections.value = currentConnections
            synthManager.playBeep(220f, 0.2f, "sawtooth")
            resetInactivityTimeout()
        }
    }


    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes() ?: return
                val jsonStr = String(data)
                log("PACKET RECV: Decrypting package of ${data.size} bytes")
                resetInactivityTimeout()
                handleIncomingPacket(endpointId, jsonStr)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun handleIncomingPacket(endpointId: String, jsonStr: String) {
        try {
            log("INCOMING DATA DECRYPTED: $jsonStr")
            val packet = Json.decodeFromString<HydraPacket>(jsonStr)
            mainScope.launch {
                _incomingPackets.emit(Pair(endpointId, packet))
            }
        } catch (e: Exception) {
            log("ERR: Cryptographic verification mismatch. Corrupt packet discarded. ${e.message}")
        }
    }
}

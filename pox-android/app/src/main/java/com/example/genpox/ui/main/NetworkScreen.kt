package com.example.genpox.ui.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import com.example.genpox.theme.*
import com.example.genpox.ui.components.DualPaneConsoleFrame
import com.example.genpox.ui.components.PoxTabFrame
import com.example.genpox.ui.components.PoxSubTab
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import com.example.genpox.data.network.PoxNetworkManager
import com.example.genpox.data.network.HydraPacket
import com.example.genpox.data.MailMessage
import com.example.genpox.data.Creature
import com.example.genpox.data.GeneSequence
import com.example.genpox.data.PeerNode
import androidx.compose.ui.platform.LocalContext


// ==========================================
// HOLOGRAPHIC CANVAS ICON RENDERERS
// ==========================================

@Composable
fun HoloGearIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = 1.5.dp.toPx()
        
        // Outer ring
        drawCircle(color, radius = w * 0.26f, center = Offset(cx, cy), style = Stroke(width = strokeW))
        
        // Inner hub hole
        drawCircle(color, radius = w * 0.12f, center = Offset(cx, cy), style = Stroke(width = strokeW))
        
        // Gear teeth (8 teeth)
        for (i in 0 until 8) {
            val angleRad = Math.toRadians((i * 45).toDouble())
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()
            
            val pStart = Offset(cx + w * 0.26f * cosA, cy + h * 0.26f * sinA)
            val pEnd = Offset(cx + w * 0.40f * cosA, cy + h * 0.40f * sinA)
            drawLine(color, pStart, pEnd, strokeWidth = strokeW * 1.5f)
        }
    }
}

@Composable
fun HoloMailIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()
        
        val paddingX = w * 0.15f
        val paddingY = h * 0.26f
        val left = paddingX
        val right = w - paddingX
        val top = paddingY
        val bottom = h - paddingY
        
        // Envelope outline
        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            style = Stroke(width = strokeW)
        )
        
        // Envelope flap fold line
        val cx = w / 2f
        val cy = h * 0.54f // slightly below center
        drawLine(color, Offset(left, top), Offset(cx, cy), strokeWidth = strokeW)
        drawLine(color, Offset(right, top), Offset(cx, cy), strokeWidth = strokeW)
    }
}

@Composable
fun HoloFriendsIcon(
    color: Color,
    modifier: Modifier = Modifier,
    buttonBackgroundColor: Color = Color(0xFF0F172A)
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()
        
        // Left background silhouette (smaller)
        val c1x = w * 0.35f
        val c1y = h * 0.40f
        val r1 = w * 0.11f
        // Head
        drawCircle(color, radius = r1, center = Offset(c1x, c1y), style = Stroke(width = strokeW * 0.8f))
        // Shoulders
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(c1x - r1 * 1.4f, c1y + r1 * 0.7f),
            size = Size(r1 * 2.8f, r1 * 2f),
            style = Stroke(width = strokeW * 0.8f)
        )
        
        // Right foreground silhouette (larger, overlapping)
        val c2x = w * 0.65f
        val c2y = h * 0.45f
        val r2 = w * 0.13f
        
        // Mask behind the foreground silhouette to avoid overlapping background lines
        drawCircle(buttonBackgroundColor, radius = r2 + 2.dp.toPx(), center = Offset(c2x, c2y))
        drawRect(buttonBackgroundColor, topLeft = Offset(c2x - r2 * 2f, c2y + r2 * 0.5f), size = Size(r2 * 4f, r2 * 2.5f))
        
        // Head
        drawCircle(color, radius = r2, center = Offset(c2x, c2y), style = Stroke(width = strokeW))
        // Shoulders
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(c2x - r2 * 1.7f, c2y + r2 * 0.7f),
            size = Size(r2 * 3.4f, r2 * 2.2f),
            style = Stroke(width = strokeW)
        )
    }
}

@Composable
fun HoloCautionIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = 1.5.dp.toPx()
        
        val path = Path().apply {
            moveTo(cx, cy - h * 0.38f)
            lineTo(cx + w * 0.42f, cy + h * 0.35f)
            lineTo(cx - w * 0.42f, cy + h * 0.35f)
            close()
        }
        
        drawPath(path, color = color, style = Stroke(width = strokeW))
        
        drawLine(
            color = color,
            start = Offset(cx, cy - h * 0.12f),
            end = Offset(cx, cy + h * 0.10f),
            strokeWidth = strokeW * 1.2f
        )
        
        drawCircle(
            color = color,
            radius = strokeW * 0.8f,
            center = Offset(cx, cy + h * 0.22f)
        )
    }
}

// ==========================================
// MAIN NETWORK SCREEN VIEW
// ==========================================

@Composable
fun NetworkView(viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val activeSubView by viewModel.networkSubTab.collectAsState() // "hydranet", "diagnostics", "inbox", "friends"

    val context = LocalContext.current
    val networkManager = viewModel.networkManager

    // Collect network manager flows if it is initialized
    val isAdvertising by networkManager?.isAdvertising?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val isDiscovering by networkManager?.isDiscovering?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val discoveredPeers by networkManager?.discoveredPeers?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val activeConnections by networkManager?.activeConnections?.collectAsState(initial = emptyMap()) ?: remember { mutableStateOf(emptyMap()) }
    val networkLogs by networkManager?.connectionLogs?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    // Fallback/Mock logs if networkManager is not available
    val diagnosticsLogs = remember {
        mutableStateListOf(
            "SYS: INIT NET INTERFACE DECK ON PORT 5005...",
            "SYS: BINDING TO DETERMINISTIC UDP CHANNELS...",
            "SYS: RESOLVING SATELLITE EPHEMERIS FOR G.E.N.P.O.X. NETWORK...",
            "SYS: PORT BINDING COMPLETE. STANDBY MODE ACTIVE."
        )
    }
    val logsListState = rememberLazyListState()

    fun addDiagnosticLog(message: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        diagnosticsLogs.add("[$timeStr] $message")
        coroutineScope.launch {
            if (diagnosticsLogs.isNotEmpty()) {
                logsListState.animateScrollToItem(diagnosticsLogs.size - 1)
            }
        }
    }

    // Permissions requesting launcher
    val permissionsToRequest = remember {
        val list = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            list.add(android.Manifest.permission.BLUETOOTH_SCAN)
            list.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        list.toTypedArray()
    }
    
    var hasPermissions by remember {
        mutableStateOf(
            permissionsToRequest.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            addDiagnosticLog("SYS: Radio/P2P permissions granted.")
        } else {
            addDiagnosticLog("SYS: Radio/P2P permissions denied.")
        }
    }

    val inboxMessages by viewModel.inboxMessages.collectAsState()
    val hasUnread = inboxMessages.any { !it.isRead }

    val selectedInboxMessageId by viewModel.selectedInboxMessageId.collectAsState()
    val selectedInboxMessage = remember(selectedInboxMessageId, inboxMessages) {
        inboxMessages.find { it.id == selectedInboxMessageId }
    }

    val peerNodes by viewModel.registeredPeers.collectAsState()

    val composerRecipientId by viewModel.composerRecipientId.collectAsState()
    val composerRecipientName by viewModel.composerRecipientName.collectAsState()
    val composerRecipientIsConnected by viewModel.composerRecipientIsConnected.collectAsState()

    val showComposerForPeer = remember(composerRecipientId, composerRecipientName, composerRecipientIsConnected) {
        if (composerRecipientId != null && composerRecipientName != null) {
            LivePeerDisplay(composerRecipientId!!, composerRecipientName!!, composerRecipientIsConnected)
        } else {
            null
        }
    }

    val subTabs = remember(hasUnread, activeSubView) {
        listOf(
            PoxSubTab("hydranet", "HYDRA", icon = { iconColor -> HoloCautionIcon(iconColor) }),
            PoxSubTab("diagnostics", "GEAR", icon = { iconColor -> HoloGearIcon(iconColor) }),
            PoxSubTab("inbox", "MAIL", bounce = hasUnread, icon = { iconColor -> HoloMailIcon(iconColor) }),
            PoxSubTab("friends", "PEERS", icon = { iconColor ->
                HoloFriendsIcon(
                    color = iconColor,
                    buttonBackgroundColor = if (activeSubView == "friends") CyberGreen else Color(0xFF0F172A)
                )
            }),
            PoxSubTab("logs", "LOGS", icon = { iconColor ->
                Box(
                    modifier = Modifier.size(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ">_",
                        color = iconColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            })
        )
    }

    val activeConnectionsCount = if (networkManager != null) activeConnections.size else peerNodes.count { it.status != "OFFLINE" }
    val activeStatusText = if (networkManager != null) {
        if (isAdvertising || isDiscovering) "ACTIVE SCAN" else "STANDBY"
    } else {
        "ONLINE"
    }
    val activeStatusColor = if (networkManager != null) {
        if (isAdvertising || isDiscovering) Color(0xFF22D3EE) else CyberGreen
    } else {
        CyberGreen
    }

    val isComposerActive = showComposerForPeer != null

    PoxTabFrame(
        flavorTitle = "G.E.N. P.O.X. HYDRA-NET V0.7",
        statusText = if (showComposerForPeer != null) "CLOSE" else if (selectedInboxMessage != null) "CLOSE" else activeStatusText,
        statusColor = if (showComposerForPeer != null) CyberRed else if (selectedInboxMessage != null) CyberRed else activeStatusColor,
        onStatusClick = if (showComposerForPeer != null) { {
            viewModel.synthManager.playBeep(440f, 0.05f, "sine")
            viewModel.clearComposer()
        } } else if (selectedInboxMessage != null) { {
            viewModel.synthManager.playBeep(480f, 0.04f, "sine")
            viewModel.selectInboxMessage(null)
        } } else null,
        headerTitle = if (showComposerForPeer != null) "SECURE UPLINK MESSAGE COMPILER" else if (selectedInboxMessage != null) "DECRYPTED MESSAGE MODULE" else "MULTI-NODE COMMUNICATIONS",
        descriptionText = if (showComposerForPeer != null) "RECIPIENT: ${showComposerForPeer.name} [${showComposerForPeer.endpointId}]" else if (selectedInboxMessage != null) "DECRYPTING TRANSMISSION FROM: ${selectedInboxMessage.from}" else "Manager your node contacts and communications here.",

        subTabs = subTabs,
        activeSubTab = activeSubView,
        onSubTabClick = { id, tag ->
            if (showComposerForPeer != null) {
                viewModel.clearComposer()
            }
            viewModel.selectInboxMessage(null)
            viewModel.setNetworkSubTab(id)
            addDiagnosticLog("NAV: Holo-deck switch to sub-screen [${tag}]")
        },
        viewModel = viewModel,
        isScrollable = false
    ) {
        if (showComposerForPeer != null) {
            MessageCompilerContent(
                viewModel = viewModel,
                peer = showComposerForPeer,
                onAddLog = { addDiagnosticLog(it) },
                onClose = { viewModel.clearComposer() }
            )
        } else if (selectedInboxMessage != null) {
            MessageReaderContent(
                viewModel = viewModel,
                message = selectedInboxMessage,
                onAddLog = { addDiagnosticLog(it) },
                onClose = { viewModel.selectInboxMessage(null) }
            )
        } else {
            when (activeSubView) {
                "hydranet" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "HYDRA-NET CONNECTION SECURED. SYSTEMS NOMINAL.",
                                color = CyberGreen.copy(alpha = 0.6f),
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ACTIVE LINKS IN CLUSTER: $activeConnectionsCount",
                                color = Color.White,
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                "diagnostics" -> {
                    DiagnosticsContent(
                        viewModel = viewModel,
                        onAddLog = { addDiagnosticLog(it) }
                    )
                }
                "logs" -> {
                    TerminalLogsContent(
                        viewModel = viewModel,
                        diagnosticsLogs = if (networkManager != null) networkLogs else diagnosticsLogs,
                        logsListState = logsListState,
                        onClearLogs = {
                            if (networkManager != null) {
                                networkManager.clearLogs()
                            } else {
                                diagnosticsLogs.clear()
                            }
                        }
                    )
                }
                "inbox" -> {
                    InboxContent(
                        viewModel = viewModel,
                        messages = inboxMessages,
                        onAddLog = { addDiagnosticLog(it) }
                    )
                }
                "friends" -> {
                    FriendsListContent(
                        viewModel = viewModel,
                        peers = peerNodes,
                        onAddLog = { addDiagnosticLog(it) },
                        hasPermissions = hasPermissions,
                        onRequestPermissions = {
                            permissionLauncher.launch(permissionsToRequest)
                        },
                        showComposerForPeer = showComposerForPeer,
                        onShowComposerChange = { peer ->
                            if (peer != null) {
                                viewModel.setComposerRecipient(peer.endpointId, peer.name, peer.isConnected)
                            } else {
                                viewModel.clearComposer()
                            }
                        }
                    )
                }
            }
        }
    }
}


// ==========================================
// SUB-VIEW: DIAGNOSTICS
// ==========================================

@Composable
private fun DiagnosticsContent(
    viewModel: MainViewModel,
    onAddLog: (String) -> Unit
) {
    val userLat by viewModel.latitude.collectAsState()
    val userLng by viewModel.longitude.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()

    val sectorIp = remember(userLat, userLng) {
        val octet2 = ((userLat.absoluteValue * 100).toInt() % 240) + 10
        val octet3 = ((userLng.absoluteValue * 100).toInt() % 254) + 1
        "10.$octet2.$octet3.100"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DualPaneConsoleFrame(
            theme = "green",
            flavorTitle = "G.E.N. P.O.X. NETWORK NODE CONSOLE",
            statusText = "STANDBY",
            statusColor = CyberGreen,
            primaryTitle = "NETWORK INTERFACES STATUS",
            primaryContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Real-time communication channels configuration and alignment metadata.",
                        color = CyberGreenDim,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Default
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Pair("SECTOR IP ADDRESS", sectorIp),
                            Pair("NTP SYNC STATUS", "ONLINE (GPS/UTC)"),
                            Pair("SATELLITE DOWNLINK", "STANDBY - FREQ LOCK OK"),
                            Pair("LOCAL SECTOR DENSITY", "${anomalies.size} ANOMALIES"),
                            Pair("DOWNLINK BITRATE", "0 bps"),
                            Pair("UPLINK COMPILER STATUS", "READY")
                        ).forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = CyberGreenDim,
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp
                                )
                                Text(
                                    text = value,
                                    color = if (value.contains("ONLINE") || value.contains("READY") || value.contains("OK")) CyberGreen else Color.White,
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            secondaryTitle = "SYSTEM DIAGNOSTIC CONTROLS",
            secondaryContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Radio Signal Emulation Toggle Card
                    val isSimActive by viewModel.isNetworkSimulationActive.collectAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isSimActive) CyberCyan else CyberBorder, RoundedCornerShape(4.dp))
                            .background(CyberPanel)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "RADIO SIGNAL EMULATION",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Default
                                )
                                Text(
                                    text = if (isSimActive) "SIMULATOR ACTIVE: LOOPBACK MODE" else "SIMULATOR OFFLINE: NORMAL RADAR MODE",
                                    color = if (isSimActive) CyberCyan else Color.Gray,
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.synthManager.playBeep(640f, 0.05f, "sine")
                                    viewModel.toggleNetworkSimulation(!isSimActive)
                                    if (!isSimActive) {
                                        onAddLog("SIMULATOR: Software loopback radio online. Mock endpoints configured.")
                                    } else {
                                        onAddLog("SIMULATOR: Loopback daemon shut down. Physical radio online.")
                                    }
                                },
                                modifier = Modifier
                                    .height(26.dp)
                                    .width(90.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSimActive) Color.Red else CyberCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (isSimActive) "DISABLE" else "ENABLE",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(640f, 0.05f, "sine")
                                val nm = viewModel.networkManager
                                if (nm != null) {
                                    val active = nm.activeConnections.value
                                    if (active.isNotEmpty()) {
                                        val dest = active.keys.first()
                                        nm.sendPacket(dest, HydraPacket.Ping(System.currentTimeMillis()))
                                        onAddLog("PING: Dispatched request to secure endpoint $dest.")
                                    } else {
                                        onAddLog("PING: No active secure connections. Activating scans...")
                                        nm.startDiscovery()
                                    }
                                } else {
                                    onAddLog("PING: BROADCAST UDP DISPATCH TO $sectorIp...")
                                    val latency = (20..80).random()
                                    onAddLog("PING: GATEWAY RESPONDED (RTT: ${latency}ms)")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "PING GATEWAY",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }
 
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(450f, 0.05f, "sine")
                                val nm = viewModel.networkManager
                                if (nm != null) {
                                    nm.stopAllEndpoints()
                                }
                                onAddLog("SYS: LOG FLUSH DEPLOYED. ALL P2P ENDPOINTS RESET.")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Yellow
                            ),
                            border = BorderStroke(1.dp, Color.Yellow),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "✕ FLUSH CACHE",
                                color = Color.Yellow,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }
                    }

                }
            }
        )
    }
}

@Composable
private fun TerminalLogsContent(
    viewModel: MainViewModel,
    diagnosticsLogs: List<String>,
    logsListState: LazyListState,
    onClearLogs: () -> Unit
) {
    // Auto-scroll to the bottom when new logs arrive
    LaunchedEffect(diagnosticsLogs.size) {
        if (diagnosticsLogs.isNotEmpty()) {
            logsListState.animateScrollToItem(diagnosticsLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE TERMINAL LOGS",
                color = CyberGreen,
                style = Typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            // Clear logs button
            Button(
                onClick = {
                    viewModel.synthManager.playReject()
                    onClearLogs()
                },
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Red
                ),
                border = BorderStroke(1.dp, Color.Red),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = "CLEAR CACHE",
                    color = Color.Red,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp)
        ) {
            if (diagnosticsLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TERMINAL LOG IS EMPTY.",
                        color = CyberGreen.copy(alpha = 0.5f),
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    state = logsListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(diagnosticsLogs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("CRITICAL") || log.contains("ERROR")) Color.Red 
                                    else if (log.contains("MUTATION") || log.contains("CROSSTALK")) Color(0xFFF97316) 
                                    else if (log.contains("SUCCESS") || log.contains("ENGAGED")) CyberGreen
                                    else CyberGreen.copy(alpha = 0.6f),
                            style = Typography.bodySmall,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-VIEW: INBOX
// ==========================================

@Composable
private fun InboxContent(
    viewModel: MainViewModel,
    messages: List<MailMessage>,
    onAddLog: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Inbox Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INBOX DATA DECK",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            Text(
                text = "ONLINE - PACKETS: ${messages.size}",
                color = CyberGreenDim,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }

        Text(
            text = "INCOMING FREQUENCY ENCODED DATA TRANSFERS & BROADCAST ALERTS:",
            color = CyberGreenDim,
            fontSize = 8.sp,
            fontFamily = FontFamily.Default,
            lineHeight = 10.sp
        )

        messages.forEach { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(CyberPanel)
                    .clickable {
                        viewModel.synthManager.playBeep(580f, 0.04f, "sine")
                        if (!msg.isRead) {
                            viewModel.markMessageAsRead(msg.id)
                            onAddLog("INBOX: Read message from ${msg.from}")
                        }
                        viewModel.selectInboxMessage(msg.id)
                    }
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg.from,
                            color = if (msg.isRead) CyberGreenDim else CyberGreen,
                            style = Typography.bodySmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = msg.timestamp,
                            color = Color.Gray,
                            style = Typography.bodySmall,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = msg.subject,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "CLICK TO DECRYPT AND READ",
                        color = CyberGreenDim.copy(alpha = 0.6f),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                }
            }
        }
    }
}

private data class LivePeerDisplay(
    val endpointId: String,
    val name: String,
    val isConnected: Boolean
)

@Composable
private fun FriendsListContent(
    viewModel: MainViewModel,
    peers: List<PeerNode>,
    onAddLog: (String) -> Unit,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    showComposerForPeer: LivePeerDisplay?,
    onShowComposerChange: (LivePeerDisplay?) -> Unit
) {
    var showRegisterForm by remember { mutableStateOf(false) }
    var peerNameInput by remember { mutableStateOf("") }
    var peerSectorInput by remember { mutableStateOf("") }


    val nm = viewModel.networkManager
    val isAdvertising by nm?.isAdvertising?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val isDiscovering by nm?.isDiscovering?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val discoveredPeers by nm?.discoveredPeers?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val activeConnections by nm?.activeConnections?.collectAsState(initial = emptyMap()) ?: remember { mutableStateOf(emptyMap()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Local Node Information Display (Hierarchical Globally Unique IP Geocoder)
        val userLat by viewModel.latitude.collectAsState()
        val userLng by viewModel.longitude.collectAsState()
        val sectorIp = remember(userLat, userLng) {
            val latVal = userLat.coerceIn(-90.0, 90.0)
            val lngVal = userLng.coerceIn(-180.0, 180.0)
            val latIndex = (((latVal + 90.0) / 180.0) * 240.0).toInt().coerceIn(0, 239) + 10
            val lngIndex = (((lngVal + 180.0) / 360.0) * 254.0).toInt().coerceIn(0, 253) + 1
            val latFraction = (((latVal + 90.0) / 180.0) * 240.0) % 1.0
            val lngFraction = (((lngVal + 180.0) / 360.0) * 254.0) % 1.0
            val subLat = (latFraction * 16.0).toInt().coerceIn(0, 15)
            val subLng = (lngFraction * 16.0).toInt().coerceIn(0, 15)
            val octet4 = ((subLat * 16) + subLng + 1).coerceIn(1, 254)
            "10.$latIndex.$lngIndex.$octet4"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR LOCAL NODE ID: ${viewModel.localNodeName}",
                    color = CyberCyan,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SECTOR IP: $sectorIp",
                    color = CyberGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        // Permission Warning or Radio Controls
        if (!hasPermissions) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Yellow, RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "WARNING: LOCAL RADIO HARDWARE SYSTEM OFFLINE",
                        color = Color.Yellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "GENPOX requires location and Bluetooth/Nearby devices permissions to establish peer ad-hoc connection channels.",
                        color = Color.Yellow.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Default
                    )
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(520f, 0.05f, "sine")
                            onRequestPermissions()
                        },
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "AUTHORIZE P2P PERMISSIONS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
            }
        } else if (nm != null) {
            // Radio Controls Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(CyberPanel)
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AD-HOC TRANSCEIVER INTERFACE",
                        color = CyberGreenDim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STATE: ",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        val statusLabel = when {
                            isAdvertising && isDiscovering -> "ADV + DISCOVER"
                            isAdvertising -> "BROADCAST BEACON ACTIVE"
                            isDiscovering -> "SCANNING PEER SECTORS"
                            else -> "STANDBY"
                        }
                        Text(
                            text = statusLabel,
                            color = if (isAdvertising || isDiscovering) Color(0xFF22D3EE) else CyberGreenDim,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(520f, 0.04f, "sine")
                                if (isDiscovering) {
                                    nm.stopAllEndpoints()
                                } else {
                                    nm.startDiscovery()
                                }
                            },
                            modifier = Modifier.weight(1f).height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDiscovering) Color.Red else CyberGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (isDiscovering) "STOP SCAN" else "START SCAN",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(600f, 0.04f, "sine")
                                if (isAdvertising) {
                                    nm.stopAllEndpoints()
                                } else {
                                    nm.startAdvertising(viewModel.localNodeName)
                                }
                            },
                            modifier = Modifier.weight(1f).height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdvertising) Color.Red else CyberGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (isAdvertising) "STOP BEACON" else "START BEACON",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }
                        if (isAdvertising || isDiscovering) {
                            Button(
                                onClick = {
                                    nm.stopAllEndpoints()
                                },
                                modifier = Modifier.weight(0.8f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.Yellow
                                ),
                                border = BorderStroke(1.dp, Color.Yellow),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "OFF",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Connected & Discovered Peers Section
        val allLivePeers = remember(discoveredPeers, activeConnections) {
            val list = mutableListOf<LivePeerDisplay>()
            activeConnections.forEach { (endpointId, name) ->
                list.add(LivePeerDisplay(endpointId, name, isConnected = true))
            }
            discoveredPeers.forEach { peer ->
                if (!activeConnections.containsKey(peer.endpointId)) {
                    list.add(LivePeerDisplay(peer.endpointId, peer.name, isConnected = false))
                }
            }
            list
        }

        if (nm != null && allLivePeers.isNotEmpty()) {
            Text(
                text = "ACTIVE HYDRA-NET NODE CONNECTIONS:",
                color = Color(0xFF22D3EE),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            allLivePeers.forEach { peer ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (peer.isConnected) CyberGreen else Color(0xFF22D3EE), RoundedCornerShape(4.dp))
                        .background(CyberPanel)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = peer.name,
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                            Text(
                                text = "CHANNEL ID: ${peer.endpointId}",
                                color = Color.Gray,
                                style = Typography.bodySmall,
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (peer.isConnected) CyberGreen else Color(0xFF22D3EE),
                                            RoundedCornerShape(2.5.dp)
                                        )
                                )
                                Text(
                                    text = if (peer.isConnected) "SECURED NODE LINK" else "DISCOVERED BEACON",
                                    color = Color.LightGray,
                                    style = Typography.bodySmall,
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (peer.isConnected) {
                                Button(
                                    onClick = {
                                        viewModel.synthManager.playBeep(720f, 0.04f, "triangle")
                                        nm.sendPacket(peer.endpointId, HydraPacket.Ping(System.currentTimeMillis()))
                                        onAddLog("PING: Sent direct ping transaction package to ${peer.name}.")
                                    },
                                    modifier = Modifier
                                        .height(26.dp)
                                        .width(70.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberGreen,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "PING",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Default
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.synthManager.playBeep(640f, 0.05f, "sine")
                                        onShowComposerChange(peer)
                                    },
                                    modifier = Modifier
                                        .height(26.dp)
                                        .width(70.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "MESSAGE",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Default
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.synthManager.playBeep(720f, 0.04f, "triangle")
                                        onAddLog("DIAL: Requesting channel connection to ${peer.name}...")
                                        nm.requestConnection(peer.endpointId, "AGENT-NODE")
                                    },
                                    modifier = Modifier
                                        .height(26.dp)
                                        .width(90.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF22D3EE),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "CONNECT",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Default
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        // Registry Directory Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REGISTERED OFFLINE DIRECTORY",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            Text(
                text = "TOTAL: ${peers.size}",
                color = CyberGreenDim,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }

        // peer lists
        peers.forEach { peer ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(CyberPanel)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = peer.name,
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default
                        )
                        Text(
                            text = "SECTOR: ${peer.sector}",
                            color = CyberGreenDim,
                            style = Typography.bodySmall,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(
                                        when (peer.status) {
                                            "ONLINE" -> CyberGreen
                                            "STANDBY" -> Color(0xFFFBBF24)
                                            else -> Color.Red
                                        },
                                        RoundedCornerShape(2.5.dp)
                                    )
                            )
                            Text(
                                text = "${peer.status} | LATENCY: ${peer.latency}",
                                color = Color.Gray,
                                style = Typography.bodySmall,
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (peer.status != "OFFLINE") {
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(720f, 0.04f, "triangle")
                                val ping = if (peer.status == "ONLINE") (30..90).random() else (110..210).random()
                                onAddLog("PEER_PING: Dispatched request to ${peer.name}. Latency response resolved: ${ping}ms.")
                            },
                            modifier = Modifier
                                .height(26.dp)
                                .width(82.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = CyberGreen
                            ),
                            border = BorderStroke(1.dp, CyberGreen),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "PING LINK",
                                color = CyberGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Toggle add peer form
        if (!showRegisterForm) {
            Button(
                onClick = {
                    viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                    showRegisterForm = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "REGISTER NEW PEER NODE",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                )
            }
        } else {
            // Expansion Form Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberGreen, RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ESTABLISH DIRECT DIAL LINK CHANNEL",
                        color = CyberGreenDim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )

                    OutlinedTextField(
                        value = peerNameInput,
                        onValueChange = { peerNameInput = it },
                        label = { Text("PEER CODE NAME", color = CyberGreenDim, fontSize = 8.sp, fontFamily = FontFamily.Default) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberGreen,
                            unfocusedTextColor = CyberGreen,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    OutlinedTextField(
                        value = peerSectorInput,
                        onValueChange = { peerSectorInput = it },
                        label = { Text("REGISTRY SECTOR", color = CyberGreenDim, fontSize = 8.sp, fontFamily = FontFamily.Default) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberGreen,
                            unfocusedTextColor = CyberGreen,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (peerNameInput.isNotBlank()) {
                                    val name = peerNameInput.uppercase().trim()
                                    val sector = if (peerSectorInput.isBlank()) "Default Grid Sector" else peerSectorInput.trim()
                                    viewModel.registerPeerNode(name, sector)
                                    onAddLog("PEER_REGISTRY: Established dynamic channel to $name in $sector.")
                                    viewModel.synthManager.playSynthesisSuccess()
                                    
                                    // Reset fields
                                    peerNameInput = ""
                                    peerSectorInput = ""
                                    showRegisterForm = false
                                } else {
                                    viewModel.synthManager.playReject()
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "ESTABLISH UPLINK",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(450f, 0.05f, "sine")
                                showRegisterForm = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberRed,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "CANCEL",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun MessageCompilerContent(
    viewModel: MainViewModel,
    peer: LivePeerDisplay,
    onAddLog: (String) -> Unit,
    onClose: () -> Unit
) {
    val msgText by viewModel.composerMessageText.collectAsState()
    val attachedCreature by viewModel.composerAttachedCreature.collectAsState()
    val attachedGene by viewModel.composerAttachedGene.collectAsState()
    val transferResources by viewModel.composerTransferResources.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Attachment status indicators
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGreen.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(8.dp)
        ) {
            Text(
                text = "ACTIVE ATTACHMENTS",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SPECIMEN: ${attachedCreature?.name ?: "NONE"}",
                color = if (attachedCreature != null) CyberGreen else Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "GENE SEQUENCE: ${attachedGene?.sequence ?: "NONE"}",
                color = if (attachedGene != null) CyberGreen else Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "QUANTUM SHIPMENT 10 BASES 25 WASTE: ${if (transferResources) "ACTIVE" else "NONE"}",
                color = if (transferResources) CyberGreen else Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Attachment Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                    viewModel.setActiveCreature(null, "message_creature")
                    viewModel.setVaultSubTab("creatures")
                    viewModel.selectTab("vault")
                },
                modifier = Modifier.weight(1f).height(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "ATTACH SPECIMEN",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                )
            }

            Button(
                onClick = {
                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                    viewModel.setActiveCreature(null, "message_gene")
                    viewModel.setVaultSubTab("search")
                    viewModel.selectTab("vault")
                },
                modifier = Modifier.weight(1f).height(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "ATTACH GENE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                )
            }
        }

        val rawStockA by viewModel.rawStockA.collectAsState()
        val rawStockG by viewModel.rawStockG.collectAsState()
        val rawStockT by viewModel.rawStockT.collectAsState()
        val rawStockC by viewModel.rawStockC.collectAsState()
        val bioWaste by viewModel.bioWaste.collectAsState()
        val hasEnoughResources = rawStockA >= 10 && rawStockG >= 10 && rawStockT >= 10 && rawStockC >= 10 && bioWaste >= 25

        Button(
            onClick = {
                viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                if (hasEnoughResources) {
                    viewModel.setComposerTransferResources(!transferResources)
                } else {
                    viewModel.synthManager.playReject()
                }
            },
            modifier = Modifier.fillMaxWidth().height(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (transferResources) CyberGreen else Color(0xFF2E2E2E),
                contentColor = if (transferResources) Color.Black else Color.White
            ),
            border = BorderStroke(1.dp, if (transferResources) CyberGreen else CyberGreen.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (hasEnoughResources) "ATTACH 10 BASES 25 WASTE" else "INSUFFICIENT STOCK FOR RESOURCE ATTACHMENT",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Message text field
        OutlinedTextField(
            value = msgText,
            onValueChange = {
                if (it.length <= 64) {
                    viewModel.setComposerMessageText(it)
                }
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            placeholder = {
                Text(
                    text = "ENTER SECURE TRANSMISSION LOG MAX 64 CHARACTERS",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberCyan.copy(alpha = 0.35f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        Text(
            text = "${msgText.length}/64",
            color = if (msgText.length >= 64) Color.Red else CyberCyan.copy(alpha = 0.8f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(4.dp))

        val canTransmit = msgText.isNotBlank() || attachedCreature != null || attachedGene != null || transferResources
        Button(
            onClick = {
                viewModel.transmitP2PMessage(
                    endpointId = peer.endpointId,
                    text = msgText,
                    creature = attachedCreature,
                    gene = attachedGene,
                    transferResources = transferResources
                )
                onAddLog("TX: Dispatched payload block containing message: \"$msgText\" to ${peer.name}.")
                onClose()
            },
            enabled = canTransmit,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canTransmit) CyberCyan else Color(0xFF2E2E2E),
                contentColor = if (canTransmit) Color.Black else Color.Gray
            ),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "TRANSMIT PROTOCOL",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (canTransmit) Color.Black else Color.Gray,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@Composable
private fun MessageReaderContent(
    viewModel: MainViewModel,
    message: MailMessage,
    onAddLog: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Message Metadata Panel
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FROM: ${message.from}",
                    color = CyberGreen,
                    style = Typography.bodySmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "DATE: ${message.timestamp}",
                    color = Color.Gray,
                    style = Typography.bodySmall,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "SUBJECT: ${message.subject}",
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }

        // Message Body Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            Text(
                text = message.body,
                color = CyberGreen.copy(alpha = 0.9f),
                style = Typography.bodySmall,
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                lineHeight = 13.sp
            )
        }

        // Cargo/Attachment Section
        val hasAttachment = message.attachedCreatureDna != null || message.attachedGeneSequence != null || message.transferGenes > 0 || message.transferWaste > 0
        if (hasAttachment) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (message.isClaimed) Color.Gray else CyberCyan, RoundedCornerShape(4.dp))
                    .background(CyberPanel)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = "INCOMING CARGO TRANSFERS:",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (message.attachedCreatureDna != null) {
                            Text(
                                text = "• SPECIMEN: ${message.attachedCreatureName} [${message.attachedCreatureFaction}]",
                                color = CyberCyan,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (message.attachedGeneSequence != null) {
                            Text(
                                text = "• CODON BLOCK: ${message.attachedGeneSequence}",
                                color = CyberCyan,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (message.transferGenes > 0 || message.transferWaste > 0) {
                            Text(
                                text = "• RESOURCES: +10 All Bases / +25 Bio-Waste",
                                color = CyberCyan,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (message.isClaimed) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EXTRACTED / SECURED",
                                color = Color.Gray,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(880f, 0.05f, "sine")
                                viewModel.claimMessageAttachment(message.id)
                                onAddLog("CARGO: Decrypted and claimed transfer cargo from ${message.from}.")
                            },
                            modifier = Modifier
                                .height(26.dp)
                                .width(115.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "SECURE TRANSFERS",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Back Button
        Button(
            onClick = {
                viewModel.synthManager.playBeep(480f, 0.04f, "sine")
                onClose()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberGreen.copy(alpha = 0.15f),
                contentColor = CyberGreen
            ),
            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "RETURN TO INBOX MODULE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }
    }
}






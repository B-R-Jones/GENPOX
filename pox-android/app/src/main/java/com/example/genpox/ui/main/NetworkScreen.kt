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
import com.example.genpox.theme.*
import com.example.genpox.ui.components.DualPaneConsoleFrame
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

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
fun HoloFriendsIcon(color: Color, modifier: Modifier = Modifier) {
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
        drawCircle(Color.Black, radius = r2 + 2.dp.toPx(), center = Offset(c2x, c2y))
        drawRect(Color.Black, topLeft = Offset(c2x - r2 * 2f, c2y + r2 * 0.5f), size = Size(r2 * 4f, r2 * 2.5f))
        
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

// ==========================================
// DATA CLASS DEFINITIONS
// ==========================================

private data class MailMessage(
    val id: String,
    val from: String,
    val subject: String,
    val timestamp: String,
    val body: String,
    var isRead: Boolean = false
)

private data class PeerNode(
    val id: String,
    val name: String,
    val sector: String,
    val latency: String,
    val status: String
)

// ==========================================
// MAIN NETWORK SCREEN VIEW
// ==========================================

@Composable
fun NetworkView(viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var activeSubView by remember { mutableStateOf("diagnostics") } // "diagnostics", "inbox", "friends"

    // Unified logs cached in viewmodel logs or local console
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

    // Inbox data state
    val inboxMessages = remember {
        mutableStateListOf(
            MailMessage(
                "msg_1",
                "CONTAINMENT NODE #04",
                "ATMOSPHERIC DENSITY ALARM",
                "2 hours ago",
                "ALERT: Synodic drift is generating a +0.31 density anomaly crosstalk near epicenter. Dispatch engines must equip COHERENCE_SHIELD constructs to bypass velocity degradation protocols immediately."
            ),
            MailMessage(
                "msg_2",
                "REACTOR CORE AGENT",
                "CODON BLOCK COMPILE LOG",
                "6 hours ago",
                "TRANSMISSION: Batch 109 compiled successfully. 8 genetic telemetry sequences bound and cached to biological stockpiles. Synchronized signature hash aligns with deterministic calendar seed."
            ),
            MailMessage(
                "msg_3",
                "ANONYMOUS SPLICER",
                "ENCRYPTED SIGNAL RECEPT",
                "Yesterday",
                "Snoop data intercepted from infection sector well: AGTCGTAC. Sequence successfully routed to player inventory registers. Decode via Bio-Lab Step-Search node compilation."
            ),
            MailMessage(
                "msg_4",
                "SYSTEM UPDATE",
                "NTP CHANNELS RE-ALIGNED",
                "2 days ago",
                "NTP time servers successfully re-aligned with cosmic clock cycles. Harmonic coupling metrics reset to baseline 80.0% parameters."
            )
        )
    }

    // Friends list data state
    val peerNodes = remember {
        mutableStateListOf(
            PeerNode("peer_1", "SPECIMEN-HUNTER-X", "Palo Alto Sector", "42ms", "ONLINE"),
            PeerNode("peer_2", "CODON_SLICER_99", "Grid Sector 7", "120ms", "STANDBY"),
            PeerNode("peer_3", "NUCLEOTIDE_DECK_42", "Unknown Orbit", "---", "OFFLINE")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CONTENT AREA (depends on subView state)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 68.dp) // Leave clean buffer for floating buttons
        ) {
            when (activeSubView) {
                "diagnostics" -> {
                    DiagnosticsContent(
                        viewModel = viewModel,
                        diagnosticsLogs = diagnosticsLogs,
                        logsListState = logsListState,
                        onAddLog = { addDiagnosticLog(it) }
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
                        onAddLog = { addDiagnosticLog(it) }
                    )
                }
            }
        }

        // Pinned Holo-Nav Deck (Floating bottom-right buttons)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(
                Pair("diagnostics", "GEAR"),
                Pair("inbox", "MAIL"),
                Pair("friends", "PEERS")
            ).forEach { (subView, tag) ->
                val isActive = activeSubView == subView
                val borderColor = if (isActive) CyberGreen else CyberGreenDim.copy(alpha = 0.5f)
                val glowColor = if (isActive) CyberGreen.copy(alpha = 0.2f) else Color.Transparent
                val backColor = if (isActive) CyberGreen else Color(0xFF0F172A).copy(alpha = 0.85f)
                val iconColor = if (isActive) Color.Black else CyberGreen

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .cyberglass(
                            borderColor = borderColor,
                            glowColor = glowColor,
                            backgroundColor = backColor
                        )
                        .clickable {
                            if (activeSubView != subView) {
                                viewModel.synthManager.playCombinatorTick()
                                activeSubView = subView
                                addDiagnosticLog("NAV: Holo-deck switch to sub-screen [${tag}]")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (subView) {
                        "diagnostics" -> HoloGearIcon(iconColor)
                        "inbox" -> HoloMailIcon(iconColor)
                        "friends" -> HoloFriendsIcon(iconColor)
                    }
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
    diagnosticsLogs: List<String>,
    logsListState: LazyListState,
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
            secondaryTitle = "TERMINAL DIAGNOSTICS LOG",
            secondaryContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "System log stream monitoring local node network socket transactions.",
                        color = CyberGreenDim,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Default
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color.Black)
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        LazyColumn(
                            state = logsListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(diagnosticsLogs) { log ->
                                Text(
                                    text = log,
                                    color = if (log.contains("SYS")) CyberGreenDim else if (log.contains("PING")) Color(0xFF22D3EE) else CyberGreen,
                                    style = Typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 7.5.sp,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(640f, 0.05f, "sine")
                                onAddLog("PING: BROADCAST UDP DISPATCH TO $sectorIp...")
                                val latency = (20..80).random()
                                onAddLog("PING: GATEWAY RESPONDED (RTT: ${latency}ms)")
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
                                onAddLog("SYS: LOG FLUSH DEPLOYED.")
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

// ==========================================
// SUB-VIEW: INBOX
// ==========================================

@Composable
private fun InboxContent(
    viewModel: MainViewModel,
    messages: List<MailMessage>,
    onAddLog: (String) -> Unit
) {
    var expandedMessageId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp),
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
            val isExpanded = expandedMessageId == msg.id
            val cardBorderColor = if (isExpanded) CyberGreen else CyberBorder

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(4.dp))
                    .background(CyberPanel)
                    .clickable {
                        viewModel.synthManager.playBeep(if (isExpanded) 480f else 580f, 0.04f, "sine")
                        expandedMessageId = if (isExpanded) null else msg.id
                        if (!msg.isRead) {
                            msg.isRead = true
                            onAddLog("INBOX: Read message from ${msg.from}")
                        }
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

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = msg.body,
                                color = CyberGreen.copy(alpha = 0.9f),
                                style = Typography.bodySmall,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Default,
                                lineHeight = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "CLICK TO COLLAPSE PACKET",
                            color = CyberGreenDim,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
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
}

// ==========================================
// SUB-VIEW: FRIENDS LIST
// ==========================================

@Composable
private fun FriendsListContent(
    viewModel: MainViewModel,
    peers: MutableList<PeerNode>,
    onAddLog: (String) -> Unit
) {
    var showRegisterForm by remember { mutableStateOf(false) }
    var peerNameInput by remember { mutableStateOf("") }
    var peerSectorInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PEER DIRECTORY",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            Text(
                text = "ACTIVE LINKS: ${peers.count { it.status != "OFFLINE" }}",
                color = CyberGreenDim,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }

        Text(
            text = "HOLOGRAPHIC PEER NODE LINKAGES DETECTED IN LOCAL REGISTRY:",
            color = CyberGreenDim,
            fontSize = 8.sp,
            fontFamily = FontFamily.Default,
            lineHeight = 10.sp
        )

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
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black
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
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black
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
                                    peers.add(PeerNode("peer_${peers.size + 1}", name, sector, "---", "ONLINE"))
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
                                containerColor = Color.Transparent,
                                contentColor = Color.Yellow
                            ),
                            border = BorderStroke(1.dp, Color.Yellow),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "✕ CANCEL",
                                color = Color.Yellow,
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

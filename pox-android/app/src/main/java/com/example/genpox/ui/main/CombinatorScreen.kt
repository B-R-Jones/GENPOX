package com.example.genpox.ui.main

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.Creature
import com.example.genpox.data.GeneSequence
import com.example.genpox.data.HarvestMission
import com.example.genpox.data.WaveMath
import com.example.genpox.data.BuildingStructure
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.absoluteValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.SystemFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Immutable


// ==========================================
// BATCH PACKET LOG VIEW
// ==========================================
@Composable
fun BatchPacketLogView(
    viewModel: MainViewModel,
    onSelectGene: (String) -> Unit,
    onClose: () -> Unit
) {
    val discoveredPacketsLog by viewModel.discoveredPacketsLog.collectAsState()
    
    var packetLogQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header / Title bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GENE SYNTHESIS LOG",
                color = Color(0xFF00FF41).copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }

        // Info Bar
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(borderColor = Color(0xFF00FF41), backgroundColor = Color.Transparent)
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF00FF41).copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SELECT ANY GENE BLOCK TO LOAD DETAILED ANALYSIS",
                color = Color(0xFF00FF41),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
        }

        // Logs List
        val filteredPackets = remember(discoveredPacketsLog, packetLogQuery) {
            discoveredPacketsLog.filter { packet ->
                packetLogQuery.isEmpty() || packet.genes.any { it.contains(packetLogQuery.uppercase()) }
            }
        }

        if (filteredPackets.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO SYNTHESIS PACKETS RECORDED IN THIS SECTOR.",
                    color = Color(0xFF00FF41).copy(alpha = 0.5f),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredPackets) { pIdx, packet ->
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(packet.timestamp))
                    val uniqueCount = packet.newGenes.size
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberglass(
                                borderColor = Color(0xFF00FF41).copy(alpha = 0.25f),
                                backgroundColor = Color.Black.copy(alpha = 0.4f)
                            )
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Packet Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#",
                                        style = Typography.labelSmall,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FF41)
                                    )
                                    Text(
                                        text = "${discoveredPacketsLog.size - pIdx}",
                                        style = Typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FF41)
                                    )
                                    Text(
                                        text = " PACKET SPLICED",
                                        style = Typography.labelSmall,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FF41)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$uniqueCount",
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF22D3EE)
                                        )
                                        Text(
                                            text = " NEW GENES",
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF22D3EE)
                                        )
                                    }
                                    Text(
                                        text = timeStr,
                                        style = Typography.labelSmall,
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Genes display
                            if (packet.isAnomalous) {
                                val anomalousGene = packet.genes.firstOrNull() ?: "DECAYED!"
                                val isDecayed = anomalousGene == "DECAYED!"
                                val isNew = packet.newGenes.contains(anomalousGene)
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isNew) {
                                                Modifier.cyberglass(
                                                    borderColor = Color(0xFFA855F7).copy(alpha = alpha),
                                                    backgroundColor = Color.Transparent
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable(enabled = !isDecayed) {
                                            onSelectGene(anomalousGene)
                                            viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                        }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = anomalousGene,
                                            style = Typography.bodyMedium,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDecayed) Color.Red else Color(0xFFA855F7).copy(alpha = if (isNew) alpha else 1f)
                                        )
                                        if (!isDecayed) {
                                            Text(
                                                text = "SECURED [DIAGNOSE]",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Default,
                                                fontSize = 7.5.sp,
                                                color = Color(0xFFD8B4FE)
                                            )
                                        } else {
                                            Text(
                                                text = "DECOMPOSED",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Default,
                                                fontSize = 7.5.sp,
                                                color = Color.Red
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (row in 0 until 2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (col in 0 until 4) {
                                                val idx = row * 4 + col
                                                val gene = packet.genes.getOrNull(idx)
                                                if (gene != null) {
                                                    val isNew = packet.newGenes.contains(gene)
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .then(
                                                                if (isNew) {
                                                                    Modifier.cyberglass(
                                                                        borderColor = Color(0xFF22D3EE).copy(alpha = alpha),
                                                                        backgroundColor = Color.Transparent
                                                                    )
                                                                } else {
                                                                    Modifier
                                                                }
                                                            )
                                                            .clickable {
                                                                onSelectGene(gene)
                                                                viewModel.synthManager.playBeep(330f, 0.04f, "sine")
                                                            }
                                                            .padding(vertical = 12.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = gene,
                                                            style = Typography.labelSmall,
                                                            fontSize = 7.5.sp,
                                                            color = if (isNew) Color(0xFF22D3EE).copy(alpha = alpha) else Color(0xFF00FF41),
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF00FF41).copy(alpha = alpha))
                )
                Text(
                    text = "STANDARD GENE SEARCH ACTIVE",
                    color = Color(0xFF00FF41).copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "SECURE CONNECTION: AIS-DEV-ENV",
                color = Color(0xFF005511),
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// TERMINAL LOG SUB-VIEW
// ==========================================
@Composable
fun TerminalLogSubView(
    viewModel: MainViewModel,
    activeBorder: Color
) {
    val logs by viewModel.terminalLogs.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to the bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
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
                color = Color(0xFF00FF41),
                style = Typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            // Clear logs button
            PoxButton(
                text = "CLEAR CACHE",
                onClick = { viewModel.clearTerminalLogs() },
                buttonType = PoxButtonType.RED_MUTED,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.REJECT_BEEP,
                viewModel = viewModel
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, activeBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TERMINAL LOG IS EMPTY.",
                        color = Color(0xFF00FF41).copy(alpha = 0.5f),
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("CRITICAL") || log.contains("ERROR")) Color.Red 
                                    else if (log.contains("MUTATION") || log.contains("CROSSTALK")) Color(0xFFF97316) 
                                    else if (log.contains("SUCCESS") || log.contains("ENGAGED")) Color(0xFF00FF41)
                                    else Color(0xFF00FF41).copy(alpha = 0.6f),
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
// 1. COMBINATOR VIEW (Modularized & Slimmed)
// ==========================================
@Composable
fun CombinatorView(viewModel: MainViewModel) {
    val bioLabSubTab by viewModel.bioLabSubTab.collectAsState()
    val poxReactorActive by viewModel.poxReactorActive.collectAsState()
    val anomalyEngineActive by viewModel.anomalyEngineActive.collectAsState()
    val discoveredPacketsLog by viewModel.discoveredPacketsLog.collectAsState()
    val grandTotalStandardNucleotides by viewModel.grandTotalStandardNucleotides.collectAsState()
    val geneSequences by viewModel.geneSequences.collectAsState()
    val devForceAnomaly by viewModel.devForceAnomaly.collectAsState()
    val inventoryMetrics by viewModel.inventoryMetrics.collectAsState()
    
    val geneSequenceStrings = remember(geneSequences) { geneSequences.map { it.sequence } }
    
    // Last Main Subtab state to remember REACTOR context
    var lastMainSubTab by remember { mutableStateOf("pox") }
    LaunchedEffect(bioLabSubTab) {
        lastMainSubTab = "pox"
    }

    // Local details popups state
    var selectedPacketByGene by remember { mutableStateOf<String?>(null) }
    var selectedAnomalousGene by remember { mutableStateOf<String?>(null) }

    // Wave config calculations
    val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
    
    // UI colors and themes based on subtab
    val activeColor = CyberGreen
    val activeColorDim = CyberGreenDim
    val activeBorder = CyberBorder
    val activePanel = CyberPanel

    val subTabs = remember {
        listOf(
            PoxSubTab("parameters", "PARAMETERS", icon = { iconColor ->
                WireframeReactorParametersIcon(color = iconColor, modifier = Modifier.size(24.dp))
            }),
            PoxSubTab("pox", "REACTOR", icon = { iconColor ->
                WireframeDna(color = iconColor, modifier = Modifier.size(24.dp))
            }),
            PoxSubTab("terminal", "TERMINAL", icon = { iconColor ->
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ">_",
                        color = iconColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            })
        )
    }

    PoxTabFrame(
        flavorTitle = if (bioLabSubTab == "parameters") {
            "Tide Pool Reactor : Reactor Specifications & Parameters"
        } else {
            "G.E.N. P.O.X. TIDE POOL REACTOR V2.4"
        },
        statusText = if (poxReactorActive) "SYSTEMS ON" else "SYSTEMS OFF",
        statusColor = if (poxReactorActive) CyberGreen else Color.Red,
        onStatusClick = {
            viewModel.setPoxReactorActive(!poxReactorActive)
        },
        headerTitle = if (bioLabSubTab == "parameters") {
            "P.O.X. Reactor Parameters"
        } else {
            "SINGLE-NODE CYBERNETIC SYNTHESIZER"
        },
        descriptionText = if (bioLabSubTab == "parameters") {
            "Manage your reactor to improve synthesis accuracy and gene quality."
        } else {
            "Program your reactor, confirm ideal sequence coherence, and initiate synthesis."
        },
        borderColor = activeBorder,
        backgroundColor = activePanel,
        isScrollable = (bioLabSubTab == "pox"),
        subTabs = subTabs,
        activeSubTab = bioLabSubTab,
        onSubTabClick = { id, tag ->
            viewModel.synthManager.playBeep(600f, 0.08f, "sine")
            viewModel.setBioLabSubTab(id)
        },
        viewModel = viewModel
    ) {
        when (bioLabSubTab) {
            "pox" -> {
                ReactorDashboardView(
                    viewModel = viewModel,
                    activeBorder = activeBorder,
                    activePanel = activePanel
                )
            }
            "parameters" -> {
                ReactorParametersView(
                    viewModel = viewModel,
                    activeBorder = activeBorder,
                    activePanel = activePanel
                )
            }
            "terminal" -> {
                TerminalLogSubView(
                    viewModel = viewModel,
                    activeBorder = activeBorder
                )
            }
            else -> {
                ReactorDashboardView(
                    viewModel = viewModel,
                    activeBorder = activeBorder,
                    activePanel = activePanel
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        // Progress and scope panels
        ReactorProgressPanel(viewModel = viewModel, lastMainSubTab = lastMainSubTab, activeColor = activeColor)

    }

    // Diagnostic popups
    selectedPacketByGene?.let { gene ->
        GeneDetailsPopup(viewModel, gene, activeColor, activePanel, onClose = { selectedPacketByGene = null })
    }
    selectedAnomalousGene?.let { gene ->
        VaultGeneDetailsPopup(viewModel, gene, onClose = { selectedAnomalousGene = null })
    }
}

@Composable
fun ReactorProgressPanel(
    viewModel: MainViewModel,
    lastMainSubTab: String,
    activeColor: Color
) {
    val boostSecondsLeft by viewModel.boostSecondsLeft.collectAsState()
    val poxReactorActive by viewModel.poxReactorActive.collectAsState()
    val anomalyEngineActive by viewModel.anomalyEngineActive.collectAsState()
    val activeStep by viewModel.activeSynthesisStep.collectAsState()
    
    val isSynthesisActive by viewModel.isSynthesisActive.collectAsState()
    val targetSeq by viewModel.targetSynthesisSequence.collectAsState()
    val rawStockA by viewModel.rawStockA.collectAsState()
    val rawStockG by viewModel.rawStockG.collectAsState()
    val rawStockT by viewModel.rawStockT.collectAsState()
    val rawStockC by viewModel.rawStockC.collectAsState()
    val activeSolute by viewModel.activeChemicalSolute.collectAsState()

    val hasEnoughStock = remember(targetSeq, rawStockA, rawStockG, rawStockT, rawStockC, activeSolute) {
        var reqA = 0L
        var reqG = 0L
        var reqT = 0L
        var reqC = 0L
        targetSeq.forEach { char ->
            when (char) {
                'A' -> reqA++
                'G' -> reqG++
                'T' -> reqT++
                'C' -> reqC++
            }
        }
        val soluteCost = if (activeSolute == "DMSO" || activeSolute == "Netropsin") 100L else 0L
        rawStockA >= (reqA + soluteCost) &&
        rawStockG >= (reqG + soluteCost) &&
        rawStockT >= (reqT + soluteCost) &&
        rawStockC >= (reqC + soluteCost)
    }

    // Timer text and booster status
    ReactorTimerStatus(
        statusProvider = {
            if (lastMainSubTab == "anomaly") {
                if (anomalyEngineActive) "ANOMALOUS CONSOLIDATION: ACTIVE" else "ANOMALOUS CONSOLIDATION: IDLE"
            } else {
                if (!poxReactorActive) {
                    "GENE ARRAY REACTOR: OFFLINE"
                } else if (isSynthesisActive) {
                    "SYNTHESIZING: STEP $activeStep/8"
                } else if (hasEnoughStock) {
                    ""
                } else {
                    "INSUFFICIENT STOCK"
                }
            }
        },
        colorProvider = {
            if (lastMainSubTab == "anomaly") {
                if (anomalyEngineActive) Color(0xFFA855F7) else Color.Gray
            } else {
                if (!poxReactorActive) {
                    Color.Red
                } else if (isSynthesisActive) {
                    CyberGreen
                } else if (hasEnoughStock) {
                    CyberGreen
                } else {
                    Color.Red
                }
            }
        },
        boostSecondsLeftProvider = { if (lastMainSubTab == "pox") boostSecondsLeft else 0 },
        activeColor = CyberGreen
    )
}

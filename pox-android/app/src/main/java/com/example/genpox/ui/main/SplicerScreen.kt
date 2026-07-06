package com.example.genpox.ui.main

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.runtime.withFrameNanos


@Composable
fun GeneticResonanceScope(
    progress: Float,
    isActive: Boolean,
    isAnomaly: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scope_oscillation")
    val wavePhaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    val ambientNoiseState = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "noise"
    )

    // Pre-allocate a remembered Path to prevent garbage collection frame-drop stutters
    val scopePath = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        
        // Defer animated state reads to draw phase
        val wavePhase = wavePhaseState.value
        val ambientNoise = ambientNoiseState.value
        
        val gridStroke = 0.5.dp.toPx()
        val gridColor = activeColor.copy(alpha = 0.1f)
        
        drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = gridStroke)
        drawLine(gridColor, Offset(0f, cy), Offset(w, cy), strokeWidth = gridStroke * 1.5f)
        drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = gridStroke)
        
        val numVerticalLines = 8
        for (v in 1..numVerticalLines) {
            val vx = w * (v.toFloat() / (numVerticalLines + 1))
            drawLine(gridColor, Offset(vx, 0f), Offset(vx, h), strokeWidth = gridStroke)
        }
        
        if (isActive) {
            scopePath.reset()
            val segments = 50
            val amp = h * 0.35f
            val freq = 3.5f * Math.PI.toFloat() / w
            
            for (x in 0..segments) {
                val px = w * (x.toFloat() / segments)
                val angle = px * freq - wavePhase
                var sinVal = kotlin.math.sin(angle.toDouble()).toFloat()
                
                // Secondary wave: phase multiplier is integer 2 to wrap cleanly with wavePhase
                val angle2 = px * freq * 2f - wavePhase * 2f
                sinVal += 0.25f * kotlin.math.sin(angle2.toDouble()).toFloat()
                
                if (isAnomaly) {
                    val angle3 = angle * 10f
                    sinVal += 0.12f * ambientNoise * kotlin.math.sin(angle3.toDouble()).toFloat()
                }
                
                val edgeEnvelope = kotlin.math.sin((x.toFloat() / segments * Math.PI.toFloat()).toDouble()).toFloat()
                val finalY = cy + sinVal * amp * edgeEnvelope
                
                if (x == 0) {
                    scopePath.moveTo(px, finalY)
                } else {
                    scopePath.lineTo(px, finalY)
                }
            }
            
            drawPath(
                path = scopePath,
                color = activeColor,
                style = Stroke(width = 1.8.dp.toPx())
            )
            
            drawPath(
                path = scopePath,
                color = activeColor.copy(alpha = 0.2f),
                style = Stroke(width = 4.dp.toPx())
            )
        } else {
            scopePath.reset()
            val segments = 30
            for (x in 0..segments) {
                val px = w * (x.toFloat() / segments)
                val jitter = if (isAnomaly) ambientNoise * 1.2.dp.toPx() else 0f
                if (x == 0) {
                    scopePath.moveTo(px, cy + jitter)
                } else {
                    scopePath.lineTo(px, cy + jitter)
                }
            }
            drawPath(
                path = scopePath,
                color = Color.Red.copy(alpha = 0.5f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

// ==========================================
// 2. SPLICER VIEW
// ==========================================
@Composable
fun SplicerView(viewModel: MainViewModel) {
    val targetSequence by viewModel.targetSequence.collectAsState()
    val splicerSlots by viewModel.splicerSlots.collectAsState()
    val activeSlotSelection by viewModel.activeSlotSelection.collectAsState()
    val slotSequenceFilter by viewModel.slotSequenceFilter.collectAsState()
    val isReactorFrozen by viewModel.isReactorFrozen.collectAsState()
    val reactorFreezeTimeLeft by viewModel.reactorFreezeTimeLeft.collectAsState()
    val isForcedConstructionActive by viewModel.isForcedConstructionActive.collectAsState()
    val isForcedLoopActive by viewModel.isForcedLoopActive.collectAsState()
    val forcedConstructionLogs by viewModel.forcedConstructionLogs.collectAsState()
    val isSplicing by viewModel.isSplicing.collectAsState()
    val splicingProgress by viewModel.splicingProgress.collectAsState()
    val sequencedBasesCount by viewModel.sequencedBasesCount.collectAsState()
    val newlySplicedCreature by viewModel.newlySplicedCreature.collectAsState()
    val inventoryGenes by viewModel.geneSequences.collectAsState()
    val splicerSubTab by viewModel.splicerSubTab.collectAsState()
    
    val selectedReagent by viewModel.selectedReagent.collectAsState()

    val reagentColor = selectedReagent?.let { rId ->
        when (rId) {
            "REAGENT_LIGASE" -> Color(0xFF00FF66)
            "REAGENT_BETAINE" -> Color(0xFFFBBF24)
            "REAGENT_FORMAMIDE" -> Color(0xFFA855F7)
            "REAGENT_TU" -> Color(0xFFEF4444)
            "REAGENT_STAT_VITALITY" -> Color(0xFF38BDF8)
            "REAGENT_STAT_ATTACK" -> Color(0xFF34D399)
            "REAGENT_STAT_DEFENSE" -> Color(0xFFFB7185)
            "REAGENT_STAT_SPEED" -> Color(0xFFFBBF24)
            "REAGENT_ALIGN_CONTAINMENT" -> Color(0xFF38BDF8)
            "REAGENT_ALIGN_INFECTION" -> Color(0xFF34D399)
            "REAGENT_ALIGN_MECH" -> Color(0xFFFB7185)
            "REAGENT_ALIGN_PARASITE" -> Color(0xFFFBBF24)
            "REAGENT_TACTICAL_HACK" -> Color(0xFF84CC16)
            "REAGENT_TACTICAL_DEFEND" -> Color(0xFFCBD5E1)
            "REAGENT_TACTICAL_TEMP" -> Color(0xFFEC4899)
            "REAGENT_VISUAL_GLOW" -> Color(0xFF00FF66)
            "REAGENT_VISUAL_MATRIX" -> Color(0xFF22D3EE)
            "REAGENT_VISUAL_SHIMMER" -> {
                val transition = rememberInfiniteTransition(label = "shimmer_transition")
                val hue by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shimmer_hue"
                )
                Color.hsv(hue, 0.8f, 0.9f)
            }
            else -> CyberTheme.green
        }
    } ?: CyberTheme.green

    val activeColor = reagentColor
    val activeBorder = activeColor.copy(alpha = 0.35f)
    val activePanel = CyberTheme.greenPanel
    var selectedGeneDetails by remember { mutableStateOf<String?>(null) }
    var onEjectActiveGene by remember { mutableStateOf<(() -> Unit)?>(null) }

    val subTabs = remember {
        listOf(
            PoxSubTab("splicer", "SPLICER", icon = { iconColor ->
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

    Box(modifier = Modifier.fillMaxSize()) {
        PoxTabFrame(
            flavorTitle = if (splicerSubTab == "splicer") {
                if (isForcedConstructionActive) "P.O.X. REACTOR ACTIVE (OVERRIDE)" else "G.E.N. P.O.X. SPLICING ASSEMBLY CHAMBER V1.7"
            } else {
                "P.O.X. REACTOR TELEMETRY OVERRIDES"
            },
            statusText = if (isForcedConstructionActive || isSplicing) "ACTIVE" else "READY",
            statusColor = if (isForcedConstructionActive || isSplicing) Color.Red else CyberGreen,
            headerTitle = if (splicerSubTab == "splicer") {
                if (isForcedConstructionActive) "FORCED SEQUENCING ACTIVE" else "SINGLE-NODE SEQUENCING"
            } else {
                "FORCED SEQUENCING LOG TERMINAL"
            },
            descriptionText = if (splicerSubTab == "splicer") {
                "Fill all slots with stockpiled genes to assemble the target genome."
            } else {
                "Observe reactor telemetry logs and system overrides."
            },
            borderColor = activeBorder,
            backgroundColor = activePanel,
            isScrollable = false,
            subTabs = subTabs,
            activeSubTab = splicerSubTab,
            onSubTabClick = { id, tag ->
                viewModel.setSplicerSubTab(id)
            },
            viewModel = viewModel,
            useDynamicReagentColor = true,
            drawSubTabs = false
        ) {
            when (splicerSubTab) {
                "splicer" -> {
                    SplicerLeftPanel(
                        viewModel = viewModel,
                        targetSequence = targetSequence,
                        splicerSlots = splicerSlots,
                        activeSlotSelection = activeSlotSelection,
                        isReactorFrozen = isReactorFrozen,
                        reactorFreezeTimeLeft = reactorFreezeTimeLeft,
                        isForcedConstructionActive = isForcedConstructionActive,
                        isForcedLoopActive = isForcedLoopActive,
                        forcedConstructionLogs = forcedConstructionLogs,
                        isSplicing = isSplicing,
                        splicingProgress = splicingProgress,
                        sequencedBasesCount = sequencedBasesCount,
                        newlySplicedCreature = newlySplicedCreature,
                        onShowGeneDetails = { gene, eject ->
                            selectedGeneDetails = gene
                            onEjectActiveGene = eject
                        },
                        activeColor = activeColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "terminal" -> {
                    SplicerTerminalLogView(
                        viewModel = viewModel,
                        forcedConstructionLogs = forcedConstructionLogs,
                        isForcedLoopActive = isForcedLoopActive,
                        activeBorder = activeBorder
                    )
                }
            }
        }

        val isCustomScaffoldMode by viewModel.isCustomScaffoldMode.collectAsState()

        val mainRow = subTabs.map { tab ->
            val isActive = splicerSubTab == tab.id
            HoloNavButton(
                id = tab.id,
                borderColor = if (isActive) activeColor else activeColor.copy(alpha = 0.24f),
                glowColor = if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent,
                backgroundColor = if (isActive) activeColor else Color(0xFF0F172A).copy(alpha = 0.85f),
                enabled = true,
                onClick = {
                    if (splicerSubTab != tab.id) {
                        viewModel.synthManager.playCombinatorTick()
                        viewModel.setSplicerSubTab(tab.id)
                    }
                },
                bounce = tab.bounce,
                content = { iconColor ->
                    tab.icon(iconColor)
                }
            )
        }

        val splicerSecondaryTabs = listOf(
            PoxSubTab("targeted", "TARGETED") { iconColor ->
                WireframeTargetedIcon(color = iconColor, modifier = Modifier.size(24.dp))
            },
            PoxSubTab("custom", "CUSTOM") { iconColor ->
                WireframeCustomIcon(color = iconColor, modifier = Modifier.size(24.dp))
            }
        )

        val secondaryRow = splicerSecondaryTabs.map { tab ->
            val isModeActive = if (tab.id == "custom") isCustomScaffoldMode else !isCustomScaffoldMode
            val tabColor = if (tab.id == "custom") Color(0xFF22D3EE) else activeColor
            HoloNavButton(
                id = tab.id,
                borderColor = if (isModeActive) tabColor else tabColor.copy(alpha = 0.24f),
                glowColor = if (isModeActive) tabColor.copy(alpha = 0.15f) else Color.Transparent,
                backgroundColor = if (isModeActive) tabColor else Color(0xFF0F172A).copy(alpha = 0.85f),
                enabled = true,
                onClick = {
                    val targetMode = tab.id == "custom"
                    if (isCustomScaffoldMode != targetMode) {
                        viewModel.synthManager.playCombinatorTick()
                        viewModel.setCustomScaffoldMode(targetMode)
                    }
                },
                bounce = tab.bounce,
                content = { iconColor ->
                    tab.icon(iconColor)
                }
            )
        }

        val rows = if (splicerSubTab == "splicer") {
            listOf(mainRow, secondaryRow)
        } else {
            listOf(mainRow)
        }

        val activeModeId = if (isCustomScaffoldMode) "custom" else "targeted"

        PoxUnifiedHoloNav(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 12.dp),
            rows = rows,
            activeButtonIds = setOf(splicerSubTab, activeModeId),
            allowStacking = true,
            viewModel = viewModel
        )

        // Slotting UI Floating Panel (holographic popup overlay)
        if (activeSlotSelection != null && !isForcedConstructionActive && !isSplicing) {
            // Translucent dim backdrop to focus user attention
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.selectSplicerSlot(null) }
            )

            // Centered overlay card
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.9f)
                    .clickable(enabled = false) {} // block clicks to background
                    .border(1.dp, CyberGreen, RoundedCornerShape(4.dp))
                    .background(Color(0xFF060B07), RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                SplicerRightPanel(
                    viewModel = viewModel,
                    activeSlotSelection = activeSlotSelection,
                    targetSequence = targetSequence,
                    slotSequenceFilter = slotSequenceFilter,
                    inventoryGenes = inventoryGenes,
                    isWide = false
                )
            }
        }

        // Gene details popup overlay
        selectedGeneDetails?.let { gene ->
            GeneDetailsPopup(
                viewModel = viewModel,
                gene = gene,
                activeColor = activeColor,
                activePanel = activePanel,
                onEject = onEjectActiveGene,
                onClose = {
                    selectedGeneDetails = null
                    onEjectActiveGene = null
                }
            )
        }
    }
}

@Composable
fun SplicerLeftPanel(
    viewModel: MainViewModel,
    targetSequence: String,
    splicerSlots: List<String?>,
    activeSlotSelection: Int?,
    isReactorFrozen: Boolean,
    reactorFreezeTimeLeft: Float,
    isForcedConstructionActive: Boolean,
    isForcedLoopActive: Boolean,
    forcedConstructionLogs: List<String>,
    isSplicing: Boolean,
    splicingProgress: Int,
    sequencedBasesCount: Int,
    newlySplicedCreature: Creature?,
    onShowGeneDetails: (String, (() -> Unit)?) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val activeColorDim = activeColor.copy(alpha = 0.5f)
    val getBaseColor = { base: Char ->
        when (base) {
            'A' -> Color(0xFF38BDF8) // Sky Blue
            'G' -> Color(0xFF34D399) // Emerald Green
            'T' -> Color(0xFFFB7185) // Rose/Red
            'C' -> Color(0xFFFBBF24) // Amber/Yellow
            else -> Color.Gray
        }
    }
    val selectedReagent by viewModel.selectedReagent.collectAsState()
    val reagents by viewModel.reagents.collectAsState()
    val splicerQScores by viewModel.splicerQScores.collectAsState()
    val isCustomScaffoldMode by viewModel.isCustomScaffoldMode.collectAsState()
    var showReagentDrawer by remember { mutableStateOf(false) }
    val infoAlpha by animateFloatAsState(
        targetValue = if (showReagentDrawer) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "info_sectors_fade"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "hex_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val allFull = splicerSlots.none { it == null }
    var currentSpin by remember { mutableStateOf(0f) }
    LaunchedEffect(allFull) {
        if (allFull) {
            var lastTime = 0L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (lastTime != 0L) {
                        val deltaSeconds = (frameTimeNanos - lastTime) / 1_000_000_000f
                        currentSpin += 1.25f * deltaSeconds
                    }
                    lastTime = frameTimeNanos
                }
            }
        } else {
            val remainder = currentSpin % (2f * Math.PI.toFloat())
            val target = currentSpin - remainder
            animate(
                initialValue = currentSpin,
                targetValue = target,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ) { value, _ ->
                currentSpin = value
            }
        }
    }

    if (isForcedConstructionActive) {
        // Emergency Forced Compile Terminal Screen
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {

                // CRT scrolling terminal logs container
                val displayedLogs = remember { mutableStateListOf<String>() }
                LaunchedEffect(forcedConstructionLogs) {
                    if (forcedConstructionLogs.isEmpty()) {
                        displayedLogs.clear()
                    } else {
                        val isNewRun = displayedLogs.size > forcedConstructionLogs.size ||
                                (displayedLogs.isNotEmpty() && forcedConstructionLogs.firstOrNull() != displayedLogs.firstOrNull())
                        val startIdx = if (isNewRun) {
                            displayedLogs.clear()
                            0
                        } else {
                            displayedLogs.size
                        }
                        for (i in startIdx until forcedConstructionLogs.size) {
                            delay(150L)
                            displayedLogs.add(forcedConstructionLogs[i])
                        }
                    }
                }

                val listState = rememberLazyListState()
                LaunchedEffect(displayedLogs.size) {
                    if (displayedLogs.isNotEmpty()) {
                        listState.animateScrollToItem(displayedLogs.size - 1)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF050505))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                        .padding(6.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayedLogs) { log ->
                        val isAlert = log.contains("FAILED") || log.contains("WARNING") || log.contains("Failed") || log.contains("Sacrificed")
                        Text(
                            text = log,
                            color = if (isAlert) Color(0xFFFCA5A5) else Color(0xFF34D399),
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!isForcedLoopActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x20EF4444))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "P.O.X. REACTOR FROZEN: ${reactorFreezeTimeLeft}s REMAINING",
                            color = Color(0xFFFCA5A5),
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (isForcedLoopActive) {
                    Button(
                        onClick = {
                            viewModel.synthManager.playReject()
                            viewModel.setIsForcedLoopActive(false)
                        },
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberRed,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "EXIT AUTO-SYNTHESIS LOOP CASCADE",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
            }
        }
    } else if (isSplicing) {
        val density = LocalDensity.current
        val densityVal = density.density
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header / Title (Minimalist)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (sequencedBasesCount < 64) "CYBERGENETIC SEQUENCING IN PROGRESS" else "CYBERGENETIC SEQUENCING COMPLETE",
                    color = CyberGreen,
                    style = Typography.bodyMedium,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Center area with Circular Splicing Matrix and Creature Wireframe in center
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val widthDp = with(density) { constraints.maxWidth.toDp() }.value
                val heightDp = with(density) { constraints.maxHeight.toDp() }.value

                val centerX = widthDp / 2f
                val centerY = heightDp / 2f
                val radius = minOf(widthDp, heightDp) * 0.35f
                val rPx = radius * densityVal

                // Render CreatureWireframeView at the center (sized to fit inside radius * 0.8f)
                val wireframeDna = newlySplicedCreature?.sequence?.let { seq ->
                    seq.take(sequencedBasesCount) + "A".repeat(64 - sequencedBasesCount)
                } ?: "A".repeat(64)
                val wireframeFaction = newlySplicedCreature?.faction ?: "Infection"

                // We make the wireframe size slightly smaller than the genome circle radius
                val wireframeSizeDp = (radius * 1.5f).dp
                
                // Animate alpha for faction glow: start at 0.15 and go to 1.0 when sequenced
                val wireframeAlpha = 0.2f + 0.8f * (sequencedBasesCount / 64f)
                
                Box(
                    modifier = Modifier
                        .size(wireframeSizeDp)
                        .graphicsLayer(alpha = wireframeAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    CreatureWireframeView(
                        dna = wireframeDna,
                        faction = wireframeFaction,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Now draw the Circular Genome Map around it
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // Draw outer/inner circular borders of the track
                    drawCircle(
                        color = activeColorDim.copy(alpha = 0.2f),
                        radius = rPx * 0.86f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = activeColorDim.copy(alpha = 0.2f),
                        radius = rPx * 1.04f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Draw 8 Arc segments (each with 8 bases)
                    val currentSpinDeg = currentSpin * 180f / Math.PI.toFloat()
                    val fullDnaSeq = newlySplicedCreature?.sequence ?: "A".repeat(64)
                    for (i in 0 until 8) {
                        val startAngle = (i * 45 - 90 + 2f) + currentSpinDeg
                        val sweepAngle = 41f

                        // Draw the 8 individual color-coded base cells
                        for (b in 0 until 8) {
                            val baseIdx = i * 8 + b
                            val baseChar = fullDnaSeq.getOrNull(baseIdx) ?: 'A'
                            val isBaseSequenced = baseIdx < sequencedBasesCount
                            val baseColor = if (isBaseSequenced) getBaseColor(baseChar) else Color.DarkGray
                            val baseAlpha = if (isBaseSequenced) 0.80f else 0.10f

                            val baseStartAngle = startAngle + b * 5.125f
                            val baseSweepAngle = 4.6f

                            drawArc(
                                color = baseColor.copy(alpha = baseAlpha),
                                startAngle = baseStartAngle,
                                sweepAngle = baseSweepAngle,
                                useCenter = false,
                                topLeft = Offset(cx - rPx * 0.95f, cy - rPx * 0.95f),
                                size = Size(rPx * 1.9f, rPx * 1.9f),
                                style = Stroke(width = rPx * 0.18f)
                            )
                        }

                        // Outer segment boundary outline
                        val sequencedBasesInSlot = (sequencedBasesCount - i * 8).coerceIn(0, 8)
                        val outlineColor = if (sequencedBasesInSlot == 8) activeColor else activeColorDim.copy(alpha = 0.2f)
                        drawArc(
                            color = outlineColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(cx - rPx * 1.04f, cy - rPx * 1.04f),
                            size = Size(rPx * 2.08f, rPx * 2.08f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress text and/or buttons
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (sequencedBasesCount < 64) "SEQUENCING P.O.X. CYBERGENOME: $splicingProgress%" else "P.O.X. CYBERGENOME SEQUENCED",
                    color = if (sequencedBasesCount < 64) activeColor else CyberGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "[ $sequencedBasesCount / 64 NUCLEOTIDES VERIFIED ]",
                    color = if (sequencedBasesCount < 64) activeColorDim else CyberGreenDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal
                )

                if (sequencedBasesCount == 64) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(880f, 0.05f, "sine")
                                viewModel.completeSplicingAndViewInVault()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "VIEW IN GEN-VAULT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }
                        
                        Button(
                            onClick = {
                                viewModel.synthManager.playCombinatorTick()
                                viewModel.completeSplicingAndReturnToSplicer()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.LightGray
                            ),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "RETURN TO SPLICER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Standard Re-sequencing compact circular genome matrix panel
        val density = LocalDensity.current
        val geneTextPaint = remember(density) {
            android.graphics.Paint().apply {
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                textSize = with(density) { 9.dp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
        }
        val labelPaint = remember(density) {
            android.graphics.Paint().apply {
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                textSize = with(density) { 7.dp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                color = activeColorDim.copy(alpha = 0.5f).toArgb()
                isAntiAlias = true
            }
        }
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "segment_pulse"
        )

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main workspace with Circular Splicing Matrix
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val densityVal = density.density
                val widthDp = with(density) { constraints.maxWidth.toDp() }.value
                val heightDp = with(density) { constraints.maxHeight.toDp() }.value

                val centerX = widthDp / 2f
                val centerY = heightDp / 2f
                val radius = minOf(widthDp, heightDp) * 0.35f

                // 1. Background spokes and rotating DNA wireframe
                // Added polar coordinate hit testing to make the genome track segments directly clickable!
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentSpin, splicerSlots, targetSequence) {
                            detectTapGestures { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val cx = w / 2f
                                val cy = h / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                val rPx = radius * densityVal

                                // Detect if click is inside the track (with a generous touch target padding)
                                if (distance >= rPx * 0.76f && distance <= rPx * 1.14f) {
                                    val tapAngleRad = kotlin.math.atan2(dy, dx)
                                    var tapAngleDeg = Math.toDegrees(tapAngleRad.toDouble()).toFloat()
                                    val currentSpinDeg = currentSpin * 180f / Math.PI.toFloat()

                                    // Adjust for spin rotation
                                    var adjustedAngle = tapAngleDeg - currentSpinDeg
                                    var normalizedAngle = (adjustedAngle + 90f) % 360f
                                    if (normalizedAngle < 0f) {
                                        normalizedAngle += 360f
                                    }

                                    // 8 slots, each 45 degrees
                                    val tappedSlotIdx = (normalizedAngle / 45f).toInt() % 8
                                    val slotGene = splicerSlots.getOrNull(tappedSlotIdx)
                                    if (slotGene != null) {
                                        onShowGeneDetails(slotGene) {
                                            viewModel.ejectGeneFromSlot(tappedSlotIdx)
                                        }
                                    } else {
                                        viewModel.selectSplicerSlot(tappedSlotIdx)
                                        viewModel.setActiveCreature(null, "splicer_slot")
                                        viewModel.setVaultSubTab("search")
                                        viewModel.selectTab("vault")
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val rPx = radius * densityVal

                    // Draw outer/inner circular borders of the track
                    drawCircle(
                        color = activeColorDim.copy(alpha = 0.3f),
                        radius = rPx * 0.86f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = activeColorDim.copy(alpha = 0.3f),
                        radius = rPx * 1.04f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Central core glowing outline
                    drawCircle(
                        color = activeColorDim.copy(alpha = 0.5f),
                        radius = rPx * 0.4f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Draw active Bio-Resonance Radial Scope (Option 2)
                    val sweepAngleRad = wavePhase // rotates from 0 to 2*PI
                    val numSweepTailLines = 6
                    for (t in 0 until numSweepTailLines) {
                        val tailAngleRad = sweepAngleRad - t * 0.08f // trailing angles
                        val tailAlpha = (1f - t.toFloat() / numSweepTailLines) * 0.25f
                        val tx = cx + (rPx * 0.4f) * cos(tailAngleRad)
                        val ty = cy + (rPx * 0.4f) * sin(tailAngleRad)
                        drawLine(
                            color = activeColorDim.copy(alpha = tailAlpha),
                            start = Offset(cx, cy),
                            end = Offset(tx, ty),
                            strokeWidth = if (t == 0) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }

                    // Rotating sonar sweep cursor dot
                    val cursorX = cx + (rPx * 0.4f) * cos(sweepAngleRad)
                    val cursorY = cy + (rPx * 0.4f) * sin(sweepAngleRad)
                    drawCircle(
                        color = activeColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(cursorX, cursorY)
                    )

                    // Draw the circular Bio-Resonance Oscilloscope wave (radial frequency spikes)
                    // We scan 360 degrees in steps of 4 degrees (90 total spokes)
                    val currentSpinDeg = currentSpin * 180f / Math.PI.toFloat()
                    for (a in 0 until 360 step 4) {
                        // Map the screen-space angle back to the rotating slot index
                        var normalizedA = (a + 90f - currentSpinDeg) % 360f
                        if (normalizedA < 0f) normalizedA += 360f
                        val slotIdx = (normalizedA / 45f).toInt().coerceIn(0, 7)
                        val slotGene = splicerSlots.getOrNull(slotIdx)
                        val requiredSegment = targetSequence.substring(slotIdx * 8, (slotIdx + 1) * 8)
                        val isAnom = slotGene?.let { WaveMath.isAnomalousGene(it) } ?: false
                        val isCoherent = slotGene == requiredSegment

                        val waveColor = when {
                            slotGene == null -> activeColorDim.copy(alpha = 0.4f)
                            isAnom -> Color(0xFFA855F7).copy(alpha = 0.8f)
                            isCoherent -> activeColor.copy(alpha = 0.8f)
                            else -> Color.Red.copy(alpha = 0.8f)
                        }

                        val angleRad = a * Math.PI / 180.0

                        // Calculate dynamic wave height based on slot state
                        var h = when {
                            slotGene == null -> {
                                val ripple = sin(a * 2f * Math.PI.toFloat() / 180f + wavePhase * 1.5f)
                                1.5.dp.toPx() + 1.dp.toPx() * ripple
                            }
                            isAnom -> {
                                val ripple = sin(a * 40f * Math.PI.toFloat() / 180f + wavePhase * 25f)
                                val noise = sin(a * 123f).absoluteValue
                                6.dp.toPx() + 14.dp.toPx() * ripple * noise
                            }
                            isCoherent -> {
                                val ripple = sin(a * 6f * Math.PI.toFloat() / 180f + wavePhase * 3f)
                                7.dp.toPx() + 5.dp.toPx() * ripple
                            }
                            else -> {
                                // Mismatch
                                val ripple = sin(a * 25f * Math.PI.toFloat() / 180f + wavePhase * 12f)
                                val noise = sin(a * 77f).absoluteValue
                                5.dp.toPx() + 7.dp.toPx() * ripple * noise
                            }
                        }

                        // Calculate radar sweep excitation effect (brightening & height boost)
                        val sweepAngleDeg = (sweepAngleRad * 180.0 / Math.PI) % 360.0
                        var diffDeg = (sweepAngleDeg - a) % 360.0
                        if (diffDeg < 0) diffDeg += 360.0

                        val isExcited = diffDeg <= 20.0
                        if (isExcited) {
                            val excitationFactor = 1f + ((20.0 - diffDeg) / 20.0).toFloat() * 1.1f
                            h *= excitationFactor
                        }

                        val sx = cx + (rPx * 0.42f) * cos(angleRad).toFloat()
                        val sy = cy + (rPx * 0.42f) * sin(angleRad).toFloat()
                        val ex = cx + (rPx * 0.42f + h) * cos(angleRad).toFloat()
                        val ey = cy + (rPx * 0.42f + h) * sin(angleRad).toFloat()

                        drawLine(
                            color = waveColor,
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Draw white glowing peak highlights for excited states
                        if (isExcited) {
                            val glowAlpha = ((20.0 - diffDeg) / 20.0).toFloat() * 0.7f
                            drawLine(
                                color = Color.White.copy(alpha = glowAlpha),
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }

                    // 8 Arc segments (sliced into 8 bases), segment dividers, outer ticks and curved text
                    for (i in 0 until 8) {
                        val slotGene = splicerSlots.getOrNull(i)
                        val requiredSegment = targetSequence.substring(i * 8, (i + 1) * 8)
                        val isAnom = slotGene?.let { WaveMath.isAnomalousGene(it) } ?: false
                        val isCoherent = slotGene == requiredSegment

                        val statusColor = when {
                            slotGene == null -> CyberBorder.copy(alpha = 0.12f)
                            isAnom -> Color(0xFFA855F7)
                            isCustomScaffoldMode -> Color(0xFF22D3EE)
                            isCoherent -> activeColor
                            else -> Color.Red
                        }

                        // Calculate arc angles in degrees
                        val currentSpinDeg = currentSpin * 180f / Math.PI.toFloat()
                        val startAngle = (i * 45 - 90 + 2f) + currentSpinDeg
                        val sweepAngle = 41f

                        // Draw pulsing background under the selected segment
                        if (activeSlotSelection == i) {
                            drawArc(
                                color = statusColor.copy(alpha = pulseAlpha * 0.25f),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(cx - rPx * 0.95f, cy - rPx * 0.95f),
                                size = Size(rPx * 1.9f, rPx * 1.9f),
                                style = Stroke(width = rPx * 0.18f)
                            )
                        }

                        // Draw the 8 individual color-coded base cells
                        val segmentGeneString = slotGene ?: requiredSegment
                        for (b in 0 until 8) {
                            val baseChar = segmentGeneString.getOrNull(b) ?: '-'
                            val baseColor = getBaseColor(baseChar)
                            
                            val baseStartAngle = startAngle + b * 5.125f
                            val baseSweepAngle = 4.6f // leaves a 0.525 degree gap between bases
                            
                            drawArc(
                                color = baseColor.copy(alpha = if (slotGene != null) 0.6f else 0.15f),
                                startAngle = baseStartAngle,
                                sweepAngle = baseSweepAngle,
                                useCenter = false,
                                topLeft = Offset(cx - rPx * 0.95f, cy - rPx * 0.95f),
                                size = Size(rPx * 1.9f, rPx * 1.9f),
                                style = Stroke(width = rPx * 0.18f)
                            )
                        }

                        // Draw outer boundary highlight arc
                        drawArc(
                            color = if (slotGene == null) statusColor else statusColor.copy(alpha = 0.8f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(cx - rPx * 1.04f, cy - rPx * 1.04f),
                            size = Size(rPx * 2.08f, rPx * 2.08f),
                            style = Stroke(width = if (activeSlotSelection == i) 3.5.dp.toPx() else 1.5.dp.toPx())
                        )

                        // Draw inner boundary highlight arc for selected slot
                        if (activeSlotSelection == i) {
                            drawArc(
                                color = statusColor.copy(alpha = 0.8f),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(cx - rPx * 0.86f, cy - rPx * 0.86f),
                                size = Size(rPx * 1.72f, rPx * 1.72f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Draw segment division spoke (radial divider) at slot boundaries
                        val divisionAngleRad = ((i * 45 - 90) * Math.PI / 180.0) + currentSpin
                        val dsx = cx + (rPx * 0.86f) * cos(divisionAngleRad).toFloat()
                        val dsy = cy + (rPx * 0.86f) * sin(divisionAngleRad).toFloat()
                        val dex = cx + (rPx * 1.04f) * cos(divisionAngleRad).toFloat()
                        val dey = cy + (rPx * 1.04f) * sin(divisionAngleRad).toFloat()
                        drawLine(
                            color = activeColorDim.copy(alpha = 0.25f),
                            start = Offset(dsx, dsy),
                            end = Offset(dex, dey),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Draw outer coordinate tick marks
                        val tsx = cx + (rPx * 1.04f) * cos(divisionAngleRad).toFloat()
                        val tsy = cy + (rPx * 1.04f) * sin(divisionAngleRad).toFloat()
                        val tex = cx + (rPx * 1.10f) * cos(divisionAngleRad).toFloat()
                        val tey = cy + (rPx * 1.10f) * sin(divisionAngleRad).toFloat()
                        drawLine(
                            color = activeColorDim.copy(alpha = 0.4f),
                            start = Offset(tsx, tsy),
                            end = Offset(tex, tey),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Draw outer base-pair coordinate label (0, 8, 16 ... 56 bp)
                        val lx = cx + (rPx * 1.20f) * cos(divisionAngleRad).toFloat()
                        val ly = cy + (rPx * 1.20f) * sin(divisionAngleRad).toFloat() + with(density) { 3.dp.toPx() }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "${i * 8} bp",
                                lx,
                                ly,
                                labelPaint
                            )
                        }

                        // Draw gene sequence text along the arc segment path (clockwise direction)
                        val textRadius = rPx * 0.95f
                        val androidPath = android.graphics.Path()
                        val oval = android.graphics.RectF(cx - textRadius, cy - textRadius, cx + textRadius, cy + textRadius)
                        androidPath.addArc(oval, startAngle, sweepAngle)

                        val isTargetAnom = WaveMath.isAnomalousGene(requiredSegment)
                        val textPaintColor = when {
                            slotGene == null -> {
                                if (isCustomScaffoldMode) Color(0xFF22D3EE).copy(alpha = 0.3f)
                                else if (isTargetAnom) Color(0xFFA855F7).copy(alpha = 0.22f)
                                else activeColorDim.copy(alpha = 0.22f)
                            }
                            isAnom -> Color(0xFFC084FC)
                            isCustomScaffoldMode -> Color(0xFF22D3EE)
                            isCoherent -> activeColor
                            else -> Color.Red
                        }
                        geneTextPaint.color = textPaintColor.toArgb()

                        val textToDraw = slotGene ?: if (isCustomScaffoldMode) "[ ANY ]" else requiredSegment
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawTextOnPath(
                                textToDraw,
                                androidPath,
                                0f, // hOffset - centered automatically by Paint.Align.CENTER
                                with(density) { 3.dp.toPx() }, // vOffset
                                geneTextPaint
                            )
                        }
                    }
                }

                // 2. Central coherence status telemetry readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val coherentCount = if (isCustomScaffoldMode) {
                        splicerSlots.count { it != null }
                    } else {
                        (0 until 8).count { i ->
                            splicerSlots.getOrNull(i) == targetSequence.substring(i * 8, (i + 1) * 8)
                        }
                    }
                    val coherencePct = (coherentCount * 100) / 8

                    Text(
                        text = if (isCustomScaffoldMode) "ASSEMBLY" else "COHERENCE",
                        color = if (isCustomScaffoldMode) Color(0xFF22D3EE).copy(alpha = 0.7f) else activeColorDim,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$coherencePct%",
                        color = if (isCustomScaffoldMode) {
                            if (coherencePct == 100) Color(0xFF22D3EE) else Color(0xFF22D3EE).copy(alpha = 0.5f)
                        } else {
                            if (coherencePct == 100) activeColor else if (coherencePct > 0) activeColorDim else Color.Gray
                        },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // 3. Corner stat and reagent attribute readouts when fully coherent
                val coherentCountVal = (0 until 8).count { i ->
                    splicerSlots.getOrNull(i) == targetSequence.substring(i * 8, (i + 1) * 8)
                }
                val coherencePctVal = (coherentCountVal * 100) / 8
                val isFullyCoherent = if (isCustomScaffoldMode) {
                    splicerSlots.none { it == null }
                } else {
                    coherencePctVal == 100
                }

                if (isFullyCoherent) {
                    val fullDNASeq = splicerSlots.filterNotNull().joinToString("")
                    if (fullDNASeq.length == 64) {
                        val aCount = fullDNASeq.count { it == 'A' }
                        val gCount = fullDNASeq.count { it == 'G' }
                        val tCount = fullDNASeq.count { it == 'T' }
                        val cCount = fullDNASeq.count { it == 'C' }

                        var vitalityMod = 1.0
                        var attackMod = 1.0
                        var defenseMod = 1.0
                        var speedMod = 1.0
                        var flatAttackReduction = 0
                        
                        var vitModText = ""
                        var atkModText = ""
                        var defModText = ""
                        var spdModText = ""
                        var reagentEffectLines = listOf("NO REAGENT EFFECT")

                        when (selectedReagent) {
                            "REAGENT_LIGASE" -> {
                                defenseMod = 1.20
                                defModText = "(+20% DEF)"
                                reagentEffectLines = listOf("+20% DEFENSE", "100% TELOMERES")
                            }
                            "REAGENT_BETAINE" -> {
                                reagentEffectLines = listOf("-10 TELOMERES", "0% A/T FAIL RATE")
                            }
                            "REAGENT_FORMAMIDE" -> {
                                reagentEffectLines = listOf("-10 TELOMERES", "0% G/C FAIL RATE")
                            }
                            "REAGENT_TU" -> {
                                reagentEffectLines = listOf("+15 TELOMERES")
                            }
                            "REAGENT_STAT_VITALITY" -> {
                                vitalityMod = 1.25
                                vitModText = "(+25% VIT)"
                                val penaltyVal = aCount / 3.0
                                attackMod = 1.0 - (penaltyVal / 100.0)
                                defenseMod = 1.0 - (penaltyVal / 100.0)
                                speedMod = 1.0 - (penaltyVal / 100.0)
                                val penaltyPct = "%.1f".format(penaltyVal)
                                atkModText = "(-$penaltyPct% ATK)"
                                defModText = "(-$penaltyPct% DEF)"
                                spdModText = "(-$penaltyPct% SPD)"
                                reagentEffectLines = listOf("+25% VITALITY", "-$penaltyPct% ATK/DEF/SPD")
                            }
                            "REAGENT_STAT_ATTACK" -> {
                                attackMod = 1.25
                                atkModText = "(+25% ATK)"
                                val penaltyVal = gCount / 3.0
                                vitalityMod = 1.0 - (penaltyVal / 100.0)
                                defenseMod = 1.0 - (penaltyVal / 100.0)
                                speedMod = 1.0 - (penaltyVal / 100.0)
                                val penaltyPct = "%.1f".format(penaltyVal)
                                vitModText = "(-$penaltyPct% VIT)"
                                defModText = "(-$penaltyPct% DEF)"
                                spdModText = "(-$penaltyPct% SPD)"
                                reagentEffectLines = listOf("+25% ATTACK", "-$penaltyPct% VIT/DEF/SPD")
                            }
                            "REAGENT_STAT_DEFENSE" -> {
                                defenseMod = 1.25
                                defModText = "(+25% DEF)"
                                val penaltyVal = tCount / 3.0
                                vitalityMod = 1.0 - (penaltyVal / 100.0)
                                attackMod = 1.0 - (penaltyVal / 100.0)
                                speedMod = 1.0 - (penaltyVal / 100.0)
                                val penaltyPct = "%.1f".format(penaltyVal)
                                vitModText = "(-$penaltyPct% VIT)"
                                atkModText = "(-$penaltyPct% ATK)"
                                spdModText = "(-$penaltyPct% SPD)"
                                reagentEffectLines = listOf("+25% DEFENSE", "-$penaltyPct% VIT/ATK/SPD")
                            }
                            "REAGENT_STAT_SPEED" -> {
                                speedMod = 1.25
                                spdModText = "(+25% SPD)"
                                val penaltyVal = cCount / 3.0
                                vitalityMod = 1.0 - (penaltyVal / 100.0)
                                attackMod = 1.0 - (penaltyVal / 100.0)
                                defenseMod = 1.0 - (penaltyVal / 100.0)
                                val penaltyPct = "%.1f".format(penaltyVal)
                                vitModText = "(-$penaltyPct% VIT)"
                                atkModText = "(-$penaltyPct% ATK)"
                                defModText = "(-$penaltyPct% DEF)"
                                reagentEffectLines = listOf("+25% SPEED", "-$penaltyPct% VIT/ATK/DEF")
                            }
                            "REAGENT_TACTICAL_HACK" -> {
                                speedMod = 0.95
                                spdModText = "(-5% SPD)"
                                reagentEffectLines = listOf("-5% SPEED", "AUTO-HACK INTRUSION")
                            }
                            "REAGENT_TACTICAL_DEFEND" -> {
                                flatAttackReduction = 10
                                atkModText = "(-10 ATK)"
                                reagentEffectLines = listOf("-10 ATTACK", "AUTO-NODE DEFENDER")
                            }
                            "REAGENT_ALIGN_CONTAINMENT" -> {
                                reagentEffectLines = listOf("+15 CONTAINMENT WEIGHT")
                            }
                            "REAGENT_ALIGN_INFECTION" -> {
                                reagentEffectLines = listOf("+15 INFECTION WEIGHT")
                            }
                            "REAGENT_ALIGN_MECH" -> {
                                reagentEffectLines = listOf("+15 MECH WEIGHT")
                            }
                            "REAGENT_ALIGN_PARASITE" -> {
                                reagentEffectLines = listOf("+15 PARASITE WEIGHT")
                            }
                            "REAGENT_VISUAL_GLOW" -> {
                                reagentEffectLines = listOf("BIOLUMINESCENT CHASSIS")
                            }
                            "REAGENT_VISUAL_MATRIX" -> {
                                reagentEffectLines = listOf("MATRIX SCANLINE FILTER")
                            }
                            "REAGENT_VISUAL_SHIMMER" -> {
                                reagentEffectLines = listOf("CHROMATOPHORE SHIMMER")
                            }
                        }

                        val baseVit = 100 + aCount * 5
                        val baseAtk = 20 + gCount * 2
                        val baseDef = 20 + cCount * 2
                        val baseSpd = 20 + tCount * 2

                        val minVit = (baseVit * 0.625 * vitalityMod).toInt()
                        val maxVit = (baseVit * 1.0 * vitalityMod).toInt()

                        val minAtk = ((baseAtk * attackMod).toInt() - flatAttackReduction - 3).coerceAtLeast(5)
                        val maxAtk = ((baseAtk * attackMod).toInt() - flatAttackReduction + 3).coerceAtLeast(5)

                        val minDef = ((baseDef * defenseMod).toInt() - 3).coerceAtLeast(5)
                        val maxDef = ((baseDef * defenseMod).toInt() + 3).coerceAtLeast(5)

                        val minSpd = ((baseSpd * speedMod).toInt() - 3).coerceAtLeast(5)
                        val maxSpd = ((baseSpd * speedMod).toInt() + 3).coerceAtLeast(5)

                        val avgQ = if (splicerQScores.isNotEmpty()) splicerQScores.average() else 25.0
                        val avgQStr = "%.1f".format(avgQ)
                        
                        val forceFullTelomeres = (selectedReagent == "REAGENT_LIGASE")
                        val telomereBonus = when (selectedReagent) {
                            "REAGENT_BETAINE", "REAGENT_FORMAMIDE" -> -10
                            "REAGENT_TU" -> 15
                            else -> 0
                        }
                        val scaledTelomeres = if (forceFullTelomeres) 100 else ((avgQ * 2.5).toInt() + telomereBonus).coerceIn(10, 115)

                        var aCountFaction = aCount
                        var gCountFaction = gCount
                        var tCountFaction = tCount
                        var cCountFaction = cCount

                        when (selectedReagent) {
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

                        val hasReagent = selectedReagent != null
                        val reagentHeaderColor = if (hasReagent) activeColor else activeColorDim.copy(alpha = 0.5f)

                        // Top Rows: Sector 1 (Top-Left) & Sector 2 (Top-Right)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .graphicsLayer(alpha = infoAlpha)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Sector 1: Top-Left (All Stats, line by line)
                            Column {
                                Text(
                                    text = "ESTIMATED ATTRIBUTES",
                                    color = activeColorDim,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "VIT: $minVit-$maxVit HP ",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (vitModText.isNotEmpty()) {
                                        Text(
                                            text = vitModText,
                                            color = if (vitModText.contains("+")) activeColor else Color.Red,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ATK: $minAtk-$maxAtk ATK ",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (atkModText.isNotEmpty()) {
                                        Text(
                                            text = atkModText,
                                            color = if (atkModText.contains("+")) activeColor else Color.Red,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DEF: $minDef-$maxDef DEF ",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (defModText.isNotEmpty()) {
                                        Text(
                                            text = defModText,
                                            color = if (defModText.contains("+")) activeColor else Color.Red,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "SPD: $minSpd-$maxSpd SPD ",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (spdModText.isNotEmpty()) {
                                        Text(
                                            text = spdModText,
                                            color = if (spdModText.contains("+")) activeColor else Color.Red,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Sector 2: Top-Right (Splicing Telemetry)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "SPLICING TELEMETRY",
                                    color = activeColorDim,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "AVG Q-SCORE: $avgQStr",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "TELOMERES: $scaledTelomeres%",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Bottom Rows: Sector 4 (Bottom-Left) & Sector 3 (Bottom-Right)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .graphicsLayer(alpha = infoAlpha)
                                .padding(horizontal = 12.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Sector 4: Bottom-Left (Sequenced creature type/faction)
                            Column {
                                Text(
                                    text = "COMPILATION TARGET",
                                    color = activeColorDim,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "TYPE: ${type.uppercase()}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "SECTOR: ${faction.uppercase()}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Sector 3: Bottom-Right (Reagent effect, unambiguous, dynamic undimming)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CATALYST MODIFICATION",
                                    color = reagentHeaderColor,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                reagentEffectLines.forEach { line ->
                                    Text(
                                        text = line,
                                        color = if (!hasReagent) {
                                            Color.Gray
                                        } else {
                                            if (line.contains("-") || line.contains("FAIL") || line.contains("PENALTY")) {
                                                Color.Red.copy(alpha = 0.8f)
                                            } else {
                                                activeColor
                                            }
                                        },
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // REAGENT INJECTION PORT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .border(BorderStroke(1.dp, activeColor.copy(alpha = 0.4f)), RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { 
                        viewModel.synthManager.playCombinatorTick()
                        showReagentDrawer = !showReagentDrawer 
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeReagentObj = reagents.find { it.id == selectedReagent }
                    Text(
                        text = "REAGENT: ${activeReagentObj?.name?.uppercase() ?: "NO ACTIVE REAGENT"}",
                        color = if (selectedReagent != null) activeColor else Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedReagent != null) "[SLOTTED]" else "[EMPTY INJECTION PORT]",
                        color = if (selectedReagent != null) activeColor else Color.DarkGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // SLIDING REAGENT DRAWER
            if (showReagentDrawer) {
                var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT SPLICING CATALYST AGENT",
                            color = activeColor,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(CyberRed, RoundedCornerShape(4.dp))
                                .clickable { 
                                    viewModel.synthManager.playCombinatorTick()
                                    showReagentDrawer = false 
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CLOSE",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val categories = listOf("ALL", "ASSEMBLY", "STAT", "FACTION", "TACTICAL", "AESTHETIC")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(categories) { cat ->
                            val isFilterSelected = (cat == "ALL" && selectedCategoryFilter == null) || (selectedCategoryFilter == cat)
                            val btnColor = if (isFilterSelected) activeColor else Color.Gray
                            Box(
                                modifier = Modifier
                                    .border(BorderStroke(1.dp, btnColor.copy(alpha = if (isFilterSelected) 0.8f else 0.4f)), RoundedCornerShape(2.dp))
                                    .background(if (isFilterSelected) activeColor.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.synthManager.playCombinatorTick()
                                        selectedCategoryFilter = if (cat == "ALL") null else cat
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = btnColor,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val filteredReagents = reagents.filter {
                        selectedCategoryFilter == null || it.type.uppercase() == selectedCategoryFilter
                    }
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .fillMaxHeight()
                                    .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)), RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable {
                                        viewModel.synthManager.playCombinatorTick()
                                        viewModel.selectReagent(null)
                                        showReagentDrawer = false
                                    }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "EJECT / NONE",
                                    color = Color.Gray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        items(filteredReagents) { reagentObj ->
                            val isSelected = selectedReagent == reagentObj.id
                            val cardColor = if (isSelected) activeColor else Color.Gray
                            val hasStock = reagentObj.count > 0
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .fillMaxHeight()
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) activeColor else Color.Gray.copy(alpha = 0.4f)),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(if (isSelected) activeColor.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.5f))
                                    .clickable {
                                        if (hasStock) {
                                            viewModel.selectReagent(reagentObj.id)
                                            showReagentDrawer = false
                                        } else {
                                            viewModel.synthManager.playReject()
                                        }
                                    }
                                    .padding(6.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = reagentObj.name.uppercase(),
                                        color = if (hasStock) cardColor else Color.Gray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = reagentObj.type.uppercase(),
                                            color = Color.Gray,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "STOCK: ${reagentObj.count}",
                                            color = if (hasStock) cardColor else Color.Red,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))            // Synthesizer Actions and Forced Emergency Overrides
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasEmpty = splicerSlots.contains(null)
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(880f, 0.1f, "sine")
                            viewModel.constructSplicedCreature()
                        },
                        enabled = !hasEmpty,
                        modifier = Modifier.weight(1f).height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!hasEmpty) activeColor else Color.Transparent,
                            contentColor = if (!hasEmpty) Color.Black else Color.Gray
                        ),
                        border = if (!hasEmpty) null else BorderStroke(1.dp, activeColorDim),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "CONSTRUCT SPECIMEN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.synthManager.playCombinatorTick()
                            viewModel.autofillSplicerSlots()
                        },
                        modifier = Modifier.width(38.dp).height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = activeColorDim
                        ),
                        border = BorderStroke(1.dp, activeColorDim),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        HoloDnaAutoSlotIcon(color = activeColorDim)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val forcedEnabled = !isForcedConstructionActive && !isSplicing
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                            viewModel.startForcedConstruction()
                        },
                        enabled = forcedEnabled,
                        modifier = Modifier.weight(1f).height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (forcedEnabled) CyberOrange else Color(0xFF2E2E2E),
                            contentColor = if (forcedEnabled) Color.Black else Color.Gray
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "FORCED CONSTRUCTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.synthManager.playCombinatorTick()
                            viewModel.setIsForcedLoopActive(!isForcedLoopActive)
                        },
                        modifier = Modifier.width(38.dp).height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isForcedLoopActive) CyberOrange else Color(0xFF2E2E2E),
                            contentColor = if (isForcedLoopActive) Color.Black else CyberOrange
                        ),
                        border = BorderStroke(1.dp, if (isForcedLoopActive) CyberOrange else CyberOrange.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "⟲",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default
                        )
                    }
                }

                val devForceAnomaly by viewModel.devForceAnomaly.collectAsState()
                if (devForceAnomaly) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                            viewModel.devInjectMissingTargetGenes()
                        },
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFBBF24),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "DEV: INJECT MISSING GENES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplicerSlotCell(
    idx: Int,
    slot: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEject: () -> Unit
) {
    val isAnom = slot?.let { WaveMath.isAnomalousGene(it) } ?: false
    val borderStroke = when {
        isSelected -> BorderStroke(1.5.dp, if (isAnom) Color(0xFFA855F7) else CyberGreen)
        slot != null -> BorderStroke(1.dp, if (isAnom) Color(0xFF701A75) else CyberGreenDim)
        else -> BorderStroke(1.dp, CyberBorder)
    }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(borderStroke, RoundedCornerShape(4.dp))
            .background(
                if (isSelected) {
                    if (isAnom) Color(0x30A855F7) else Color(0x2000FF41)
                } else {
                    if (isAnom) Color(0x15A855F7) else Color(0xFF080808)
                }
            )
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text = "#${idx + 1}",
            color = if (isAnom) Color(0xFFC084FC) else CyberGreenDim,
            style = Typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )

        if (slot != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = slot.take(4),
                    color = if (isAnom) Color(0xFFD8B4FE) else CyberGreen,
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = slot.substring(4),
                    color = if (isAnom) Color(0xFFA855F7) else CyberGreenDim,
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onEject() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = Color.Red,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "----",
                color = Color(0xFF1B2B1B),
                style = Typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun SplicerRightPanel(
    viewModel: MainViewModel,
    activeSlotSelection: Int?,
    targetSequence: String,
    slotSequenceFilter: String,
    inventoryGenes: List<GeneSequence>,
    isWide: Boolean
) {
    if (activeSlotSelection == null) return

    val expected = targetSequence.substring(activeSlotSelection * 8, (activeSlotSelection + 1) * 8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ASSIGN GENE BLOCK TO SLOT #${activeSlotSelection + 1}",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { viewModel.selectSplicerSlot(null) },
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberRed,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = "CLOSE",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Expected gene info block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            Text(
                text = "REQUIRED SEGMENT FOR SLOT #${activeSlotSelection + 1}",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = expected,
                color = Color(0xFF22D3EE),
                style = Typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        val matchingGene = inventoryGenes.find { it.sequence == expected && it.count > 0 }

        if (matchingGene != null) {
            // Stock panel matching Required Segment block style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberGreen.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                    .border(1.dp, CyberGreenDim, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE STOCK",
                        color = CyberGreenDim,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MUTATE READY",
                        color = Color(0xFF34D399),
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Default,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = matchingGene.sequence,
                        color = CyberGreen,
                        style = Typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "x${matchingGene.count}",
                        color = CyberGreen,
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = { viewModel.assignGeneToSlot(matchingGene.sequence) },
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "LOAD GENE BLOCK",
                    color = Color.Black,
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        } else {
            // No stock panel matching Required Segment block style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x15EF4444), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Text(
                    text = "AVAILABLE STOCK",
                    color = Color(0xFFEF4444).copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "NO COMPATIBLE SEGMENT FOUND",
                    color = Color.Red,
                    style = Typography.bodyMedium,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = { viewModel.setTargetSynthesisSequence(expected) },
            modifier = Modifier.fillMaxWidth().height(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFF22D3EE)
            ),
            border = BorderStroke(1.dp, Color(0xFF22D3EE)),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "TARGET IN BIO-LAB",
                color = Color(0xFF22D3EE),
                style = Typography.bodySmall,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 8.5.sp
            )
        }
    }
}

@Composable
fun SplicerTestView(viewModel: MainViewModel) {
    val targetSequence by viewModel.targetSequence.collectAsState()
    val splicerSlots by viewModel.splicerSlots.collectAsState()

    val targetBlocks = remember(targetSequence) {
        (0 until 8).map { i ->
            if (i * 8 + 8 <= targetSequence.length) targetSequence.substring(i * 8, i * 8 + 8) else "--------"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DualPaneConsoleFrame(
            theme = "purple",
            flavorTitle = "[ G.E.N. P.O.X. SPLICER (T) TEST INTERFACE ]",
            statusText = "CALIBRATION ONLINE",
            statusColor = Color(0xFFA855F7),
            primaryTitle = "Genetic Splicing Matrix (T)",
            primaryContent = {
                Text(
                    text = "Observe the 8 slots representing raw blocks cached to compile the target sequence.",
                    color = Color(0xFFA855F7).copy(alpha = 0.8f),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until 8) {
                        val slotGene = splicerSlots.getOrNull(i)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SLOT #${i + 1}",
                                color = Color.Gray,
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                            Text(
                                text = slotGene ?: "--------",
                                color = if (slotGene != null) CyberGreen else Color.DarkGray,
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            secondaryTitle = "Target Alignment Buffer (T)",
            secondaryContent = {
                Text(
                    text = "Required target sequence broken down into 8-character blocks.",
                    color = Color(0xFFA855F7).copy(alpha = 0.8f),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    targetBlocks.chunked(4).forEach { rowBlocks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowBlocks.forEach { block ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = block,
                                        color = Color(0xFFA855F7),
                                        style = Typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SplicerTerminalLogView(
    viewModel: MainViewModel,
    forcedConstructionLogs: List<String>,
    isForcedLoopActive: Boolean,
    activeBorder: Color
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {

            val listState = rememberLazyListState()
            LaunchedEffect(forcedConstructionLogs.size) {
                if (forcedConstructionLogs.isNotEmpty()) {
                    listState.animateScrollToItem(forcedConstructionLogs.size - 1)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF050505))
                    .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                    .padding(6.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(forcedConstructionLogs) { log ->
                    val isAlert = log.contains("FAILED") || log.contains("WARNING") || log.contains("Failed") || log.contains("Sacrificed")
                    Text(
                        text = log,
                        color = if (isAlert) Color(0xFFFCA5A5) else Color(0xFF34D399),
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isForcedLoopActive) {
            Button(
                onClick = {
                    viewModel.synthManager.playReject()
                    viewModel.setIsForcedLoopActive(false)
                },
                modifier = Modifier.fillMaxWidth().height(34.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Red
                ),
                border = BorderStroke(1.dp, Color.Red),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "✕ EXIT AUTO-SYNTHESIS LOOP CASCADE",
                    color = Color.Red,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                )
            }
        }
    }
}

@Composable
fun BaseHexagon(
    char: Char,
    isPlaceholder: Boolean,
    sizeDp: Dp,
    modifier: Modifier = Modifier
) {
    val color = when (char) {
        'A' -> Color(0xFF38BDF8) // Cyan
        'G' -> Color(0xFF34D399) // Green
        'T' -> Color(0xFFFB7185) // Rose/Red
        'C' -> Color(0xFFFBBF24) // Yellow
        else -> Color.Gray
    }
    val hexColor = if (isPlaceholder) Color.DarkGray.copy(alpha = 0.4f) else color

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val hexRadius = w / 2f
            val center = Offset(w / 2f, h / 2f)
            val path = Path().apply {
                for (i in 0..5) {
                    val angleRad = i * Math.PI / 3f - Math.PI / 6f
                    val x = center.x + hexRadius * cos(angleRad).toFloat()
                    val y = center.y + hexRadius * sin(angleRad).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            if (!isPlaceholder) {
                drawPath(path, color = hexColor.copy(alpha = 0.15f), style = Fill)
                drawPath(path, color = hexColor, style = Stroke(width = 1.2.dp.toPx()))
            } else {
                drawPath(path, color = hexColor, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)))
            }
        }
        if (char != '-' && char != '.') {
            Text(
                text = char.toString(),
                color = if (isPlaceholder) Color.Gray else hexColor,
                fontSize = if (sizeDp < 20.dp) 7.sp else if (sizeDp > 26.dp) 11.sp else 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


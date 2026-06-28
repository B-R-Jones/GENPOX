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
    val inventoryGenes by viewModel.geneSequences.collectAsState()
    val splicerSubTab by viewModel.splicerSubTab.collectAsState()
    val activeColor = CyberTheme.red
    val activeBorder = CyberTheme.redBorder
    val activePanel = CyberTheme.redPanel

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
            flavorTitle = "G.E.N. P.O.X. SPLICING ASSEMBLY CHAMBER V1.7",
            statusText = if (isForcedConstructionActive || isSplicing) "ACTIVE" else "READY",
            statusColor = if (isForcedConstructionActive || isSplicing) Color.Red else CyberGreen,
            headerTitle = "MOLECULAR SPLICING SEQUENCER",
            descriptionText = "Splice DNA fragments together to assemble your target species or override system thresholds using forced compilation.",
            borderColor = activeBorder,
            backgroundColor = activePanel,
            isScrollable = false,
            subTabs = subTabs,
            activeSubTab = splicerSubTab,
            onSubTabClick = { id, tag ->
                viewModel.setSplicerSubTab(id)
            },
            viewModel = viewModel
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
                    .cyberglass(
                        borderColor = CyberGreen,
                        glowColor = CyberGreen.copy(alpha = 0.15f),
                        backgroundColor = Color(0xFF060B07).copy(alpha = 0.95f)
                    )
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
    modifier: Modifier = Modifier
) {
    if (isForcedConstructionActive) {
        // Emergency Forced Compile Terminal Screen
        Column(
            modifier = modifier
                .cyberglass(borderColor = Color(0xFFEF4444), backgroundColor = Color.Black)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FORCED SEQUENCING ACTIVE",
                        color = Color(0xFFEF4444),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "P.O.X. REACTOR ACTIVE (OVERRIDE)",
                    color = Color.White,
                    style = Typography.bodyMedium,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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
                    PoxButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "✕ EXIT AUTO-SYNTHESIS LOOP CASCADE",
                        onClick = { viewModel.setIsForcedLoopActive(false) },
                        buttonType = PoxButtonType.RED_DANGER,
                        buttonSize = PoxButtonSize.STANDARD,
                        sound = PoxButtonSound.REJECT_BEEP,
                        viewModel = viewModel
                    )
                }
            }
        }
    } else if (isSplicing) {
        // Splicing Morphogenesis Screen
        Column(
            modifier = modifier
                .cyberglass(borderColor = CyberBorder, backgroundColor = CyberPanel)
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            val backbonePath1 = remember { Path() }
            val backbonePath2 = remember { Path() }

            // Custom rotating Canvas double helix DNA animation
            Canvas(modifier = Modifier.size(130.dp).rotate(rotation)) {
                val w = size.width
                val h = size.height
                val amplitude = 36f

                // Re-evaluate paths smoothly using 60 step segments
                backbonePath1.reset()
                backbonePath2.reset()
                val smoothSteps = 60
                for (i in 0..smoothSteps) {
                    val t = i.toFloat() / smoothSteps
                    val y = t * h
                    val angle = t * 2 * Math.PI.toFloat()
                    val x1 = w / 2 + amplitude * sin(angle)
                    val x2 = w / 2 - amplitude * sin(angle)
                    if (i == 0) {
                        backbonePath1.moveTo(x1, y)
                        backbonePath2.moveTo(x2, y)
                    } else {
                        backbonePath1.lineTo(x1, y)
                        backbonePath2.lineTo(x2, y)
                    }
                }
                drawPath(backbonePath1, color = Color(0xFFF97316), style = Stroke(width = 2.dp.toPx()))
                drawPath(backbonePath2, color = Color(0xFFF97316), style = Stroke(width = 2.dp.toPx()))

                // Draw 4 base-pair connecting rungs (cyan) with node circles
                val rungs = 4
                for (i in 0..rungs) {
                    val t = i.toFloat() / rungs
                    val y = t * h
                    val angle = t * 2 * Math.PI.toFloat()
                    val x1 = w / 2 + amplitude * sin(angle)
                    val x2 = w / 2 - amplitude * sin(angle)

                    val slotIdx = if (i == rungs) rungs - 1 else i
                    val segmentProgressThreshold = (slotIdx + 1) * 25f
                    val isSegmentLoaded = splicingProgress >= segmentProgressThreshold

                    val targetSeg = if (slotIdx * 16 + 16 <= targetSequence.length) {
                        targetSequence.substring(slotIdx * 16, slotIdx * 16 + 16)
                    } else {
                        "--------"
                    }
                    val isAnom = WaveMath.isAnomalousGene(targetSeg)

                    val segmentColor = if (isAnom) {
                        Color(0xFFA855F7)
                    } else {
                        Color(0xFF22D3EE)
                    }

                    val colorNode1 = if (isSegmentLoaded) {
                        if (sin(angle) > 0) segmentColor else Color(0xFFA855F7)
                    } else {
                        Color.DarkGray
                    }

                    val colorNode2 = if (isSegmentLoaded) {
                        if (sin(angle) > 0) Color(0xFFA855F7) else segmentColor
                    } else {
                        Color.DarkGray
                    }

                    val lineColor = if (isSegmentLoaded) {
                        segmentColor.copy(alpha = 0.8f)
                    } else {
                        Color.DarkGray.copy(alpha = 0.3f)
                    }

                    drawLine(
                        color = lineColor,
                        start = Offset(x1, y),
                        end = Offset(x2, y),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(color = colorNode1, radius = 5.dp.toPx(), center = Offset(x1, y))
                    drawCircle(color = colorNode2, radius = 5.dp.toPx(), center = Offset(x2, y))
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "P.O.X. REACTOR ENGAGED",
                color = CyberGreen,
                style = Typography.bodyMedium,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sequencing P.O.X. genome using synthesized genes...",
                color = CyberGreenDim,
                style = Typography.bodySmall,
                fontFamily = FontFamily.Default,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Synthesis Assembly Matrix
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SYNTHESIS ASSEMBLY MATRIX",
                    color = CyberGreenDim,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                for (i in 0 until 8) {
                    val segmentProgressThreshold = (i + 1) * 12.5f
                    val isSegmentLoaded = splicingProgress >= segmentProgressThreshold
                    val targetSeg = if (i * 8 + 8 <= targetSequence.length) {
                        targetSequence.substring(i * 8, i * 8 + 8)
                    } else {
                        "--------"
                    }
                    val isAnom = WaveMath.isAnomalousGene(targetSeg)
                    val color = if (isAnom) {
                        Color(0xFFA855F7)
                    } else {
                        when (i % 4) {
                            0 -> CyberGreen
                            1 -> Color(0xFFFBBF24)
                            2 -> Color(0xFF60A5FA)
                            else -> Color(0xFFC084FC)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSegmentLoaded) color.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .background(if (isSegmentLoaded) color.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SLOT #${i + 1}",
                            color = if (isSegmentLoaded) color.copy(alpha = 0.8f) else Color.Gray,
                            style = Typography.bodySmall,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (isSegmentLoaded) {
                            Text(
                                text = targetSeg,
                                color = color,
                                style = Typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        } else {
                            Text(
                                text = "SPLICING SEQUENCE...",
                                color = Color.DarkGray,
                                style = Typography.bodySmall,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Default
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "SPLICING PACKETS BUFFER: $splicingProgress%",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        // Standard Re-sequencing grid layout panel
        Column(
            modifier = modifier
                .cyberglass(borderColor = CyberBorder, backgroundColor = CyberPanel)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "G.E.N. P.O.X. E-MERGE SEQUENCER V1.7",
                        color = CyberGreenDim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = "SYSTEMS ON",
                        color = CyberGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SINGLE-NODE SEQUENCING",
                        color = Color.White,
                        style = Typography.bodyMedium,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Fill all slots with stockpiled genes to assemble the target genome.",
                    color = CyberGreen.copy(alpha = 0.8f),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 10.sp
                )

                // 8 Slots grid (arranged as 4 cols, 2 rows)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (idx in 0..3) {
                            Box(modifier = Modifier.weight(1f)) {
                                SplicerSlotCell(
                                    idx = idx,
                                    slot = splicerSlots[idx],
                                    isSelected = activeSlotSelection == idx,
                                    onClick = { viewModel.selectSplicerSlot(idx) },
                                    onEject = { viewModel.ejectGeneFromSlot(idx) }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (idx in 4..7) {
                            Box(modifier = Modifier.weight(1f)) {
                                SplicerSlotCell(
                                    idx = idx,
                                    slot = splicerSlots[idx],
                                    isSelected = activeSlotSelection == idx,
                                    onClick = { viewModel.selectSplicerSlot(idx) },
                                    onEject = { viewModel.ejectGeneFromSlot(idx) }
                                )
                            }
                        }
                    }
                }

                // Target genome required sequence
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF050C06))
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REQUIRED TARGET SEQUENCE",
                            color = CyberGreenDim,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "64-CHAR GENOME GOAL",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // Renders 8 blocks of 8 bases in a centered 2x4 grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (col in 0 until 4) {
                                    val i = row * 4 + col
                                    val segment = targetSequence.substring(i * 8, (i + 1) * 8)
                                    val isAnom = WaveMath.isAnomalousGene(segment)
                                    val color = if (isAnom) {
                                        Color(0xFFA855F7)
                                    } else {
                                        when (i % 4) {
                                            0 -> CyberGreen
                                            1 -> Color(0xFFFBBF24)
                                            2 -> Color(0xFF60A5FA)
                                            else -> Color(0xFFC084FC)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isAnom) Color(0x20A855F7) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = if (isAnom) 1.dp else 0.dp,
                                                color = if (isAnom) Color(0x30A855F7) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = segment,
                                            color = color,
                                            style = Typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Current spliced specimen DNA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF050C06))
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "CURRENT SPLICED SEQUENCE",
                        color = CyberGreenDim,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (col in 0 until 4) {
                                    val i = row * 4 + col
                                    val slotGene = splicerSlots.getOrNull(i)
                                    val segment = slotGene ?: "--------"
                                    val isAnom = if (slotGene != null) {
                                        val targetSegment = targetSequence.substring(i * 8, (i + 1) * 8)
                                        WaveMath.isAnomalousGene(targetSegment)
                                    } else false
                                    
                                    val color = if (slotGene == null) {
                                        Color.Gray
                                    } else if (isAnom) {
                                        Color(0xFFA855F7)
                                    } else {
                                        when (i % 4) {
                                            0 -> CyberGreen
                                            1 -> Color(0xFFFBBF24)
                                            2 -> Color(0xFF60A5FA)
                                            else -> Color(0xFFC084FC)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isAnom) Color(0x20A855F7) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = if (isAnom) 1.dp else 0.dp,
                                                color = if (isAnom) Color(0x30A855F7) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = segment,
                                            color = color,
                                            style = Typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            textAlign = TextAlign.Center
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PoxButton(
                        modifier = Modifier.weight(1f),
                        text = "AUTO-FILL SLOTS",
                        onClick = { viewModel.autofillSplicerSlots() },
                        buttonType = PoxButtonType.GREEN_MUTED,
                        buttonSize = PoxButtonSize.STANDARD,
                        sound = PoxButtonSound.COMBINATOR_TICK,
                        viewModel = viewModel
                    )
                    val hasEmpty = splicerSlots.contains(null)
                    PoxButton(
                        modifier = Modifier.weight(1f),
                        text = "CONSTRUCT SPECIMEN",
                        onClick = { viewModel.constructSplicedCreature() },
                        enabled = !hasEmpty,
                        buttonType = PoxButtonType.GREEN_PHOSPHOR,
                        buttonSize = PoxButtonSize.STANDARD,
                        sound = PoxButtonSound.SUCCESS_CHIME,
                        viewModel = viewModel
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PoxButton(
                        modifier = Modifier.weight(1f),
                        text = "FORCED CONSTRUCTION",
                        onClick = { viewModel.startForcedConstruction() },
                        enabled = !isForcedConstructionActive && !isSplicing,
                        buttonType = PoxButtonType.RED_DANGER,
                        buttonSize = PoxButtonSize.STANDARD,
                        sound = PoxButtonSound.BEEP_HIGH,
                        viewModel = viewModel
                    )
                    PoxButton(
                        modifier = Modifier.width(38.dp),
                        text = "⟲",
                        onClick = { viewModel.setIsForcedLoopActive(!isForcedLoopActive) },
                        buttonType = if (isForcedLoopActive) PoxButtonType.RED_DANGER else PoxButtonType.RED_MUTED,
                        buttonSize = PoxButtonSize.STANDARD,
                        sound = PoxButtonSound.COMBINATOR_TICK,
                        viewModel = viewModel
                    )
                }

                val devForceAnomaly by viewModel.devForceAnomaly.collectAsState()
                if (devForceAnomaly) {
                    Spacer(modifier = Modifier.height(4.dp))
                    PoxButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "DEV: INJECT MISSING GENES",
                        onClick = { viewModel.devInjectMissingTargetGenes() },
                        buttonType = PoxButtonType.YELLOW_WARNING,
                        buttonSize = PoxButtonSize.STANDARD,
                        sound = PoxButtonSound.BEEP_DEFAULT,
                        viewModel = viewModel
                    )
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
        slot != null -> BorderStroke(1.dp, if (isAnom) Color(0xFF701A75) else CyberGreenDim.copy(alpha = 0.5f))
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
            Box(
                modifier = Modifier
                    .cyberglass(borderColor = Color.Red, backgroundColor = Color.Transparent)
                    .clickable { viewModel.selectSplicerSlot(null) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕ CLOSE",
                    color = Color.Red,
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
                    .border(1.dp, CyberGreenDim.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .cyberglass(borderColor = CyberGreen, backgroundColor = CyberGreen)
                    .clickable { viewModel.assignGeneToSlot(matchingGene.sequence) },
                contentAlignment = Alignment.Center
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
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(4.dp))
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .cyberglass(borderColor = Color(0xFF22D3EE), backgroundColor = Color(0x1522D3EE))
                .clickable { viewModel.setTargetSynthesisSequence(expected) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TARGET IN BIO-LAB",
                color = Color(0xFF22D3EE),
                style = Typography.bodySmall,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
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
                        .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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
                        .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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
                                        .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.2f), RoundedCornerShape(2.dp))
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FORCED SEQUENCING LOG TERMINAL",
                    color = Color(0xFFEF4444),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "P.O.X. REACTOR TELEMETRY OVERRIDES",
                color = Color.White,
                style = Typography.bodyMedium,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                    .border(1.dp, activeBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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
            PoxButton(
                modifier = Modifier.fillMaxWidth(),
                text = "✕ EXIT AUTO-SYNTHESIS LOOP CASCADE",
                onClick = { viewModel.setIsForcedLoopActive(false) },
                buttonType = PoxButtonType.RED_DANGER,
                buttonSize = PoxButtonSize.STANDARD,
                sound = PoxButtonSound.REJECT_BEEP,
                viewModel = viewModel
            )
        }
    }
}


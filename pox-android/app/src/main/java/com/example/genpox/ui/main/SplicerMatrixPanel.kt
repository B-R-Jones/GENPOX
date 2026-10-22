package com.example.genpox.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.data.Creature
import com.example.genpox.data.GraphicsMath
import com.example.genpox.theme.CyberGreen
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.theme.CyberOrange
import com.example.genpox.theme.CyberRed
import com.example.genpox.theme.Typography
import com.example.genpox.ui.components.CreatureWireframeView
import com.example.genpox.ui.components.HolographicRenderEngine
import kotlinx.coroutines.delay

@Composable
fun SplicerMatrixPanel(
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
    modifier: Modifier = Modifier,
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
    val selectedReagent by viewModel.selectedReagent.collectAsStateWithLifecycle()
    val reagents by viewModel.reagents.collectAsStateWithLifecycle()
    val splicerQScores by viewModel.splicerQScores.collectAsStateWithLifecycle()
    val isCustomScaffoldMode by viewModel.isCustomScaffoldMode.collectAsStateWithLifecycle()
    var showReagentDrawer by remember { mutableStateOf(false) }
    val infoAlpha by animateFloatAsState(
        targetValue = if (showReagentDrawer) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "info_sectors_fade",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "hex_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
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
                animationSpec = spring(stiffness = Spring.StiffnessLow),
            ) { value, _ ->
                currentSpin = value
            }
        }
    }

    if (isForcedConstructionActive) {
        // Emergency Forced Compile Terminal Screen
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween,
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(displayedLogs) { log ->
                        val isAlert = log.contains("FAILED") || log.contains("WARNING") || log.contains("Failed") || log.contains("Sacrificed")
                        Text(
                            text = log,
                            color = if (isAlert) Color(0xFFFCA5A5) else Color(0xFF34D399),
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Red),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "P.O.X. REACTOR FROZEN: ${reactorFreezeTimeLeft}s REMAINING",
                            color = Color(0xFFFCA5A5),
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
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
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "EXIT AUTO-SYNTHESIS LOOP CASCADE",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header / Title (Minimalist)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (sequencedBasesCount < 64) "CYBERGENETIC SEQUENCING IN PROGRESS" else "CYBERGENETIC SEQUENCING COMPLETE",
                    color = CyberGreen,
                    style = Typography.bodyMedium,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            // Center area with Circular Splicing Matrix and Creature Wireframe in center
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val widthDp = with(density) { constraints.maxWidth.toDp() }.value
                val heightDp = with(density) { constraints.maxHeight.toDp() }.value

                val centerX = widthDp / 2f
                val centerY = heightDp / 2f
                val radius = minOf(widthDp, heightDp) * 0.35f
                val rPx = radius * densityVal

                val wireframeDna = newlySplicedCreature?.sequence?.let { seq ->
                    seq.take(sequencedBasesCount) + "A".repeat(64 - sequencedBasesCount)
                } ?: "A".repeat(64)
                val wireframeFaction = newlySplicedCreature?.faction ?: "Infection"
                val wireframeSizeDp = (radius * 1.5f).dp
                val wireframeAlpha = 0.2f + 0.8f * (sequencedBasesCount / 64f)

                Box(
                    modifier = Modifier
                        .size(wireframeSizeDp)
                        .graphicsLayer(alpha = wireframeAlpha),
                    contentAlignment = Alignment.Center,
                ) {
                    CreatureWireframeView(
                        dna = wireframeDna,
                        faction = wireframeFaction,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Now draw the Circular Genome Map around it using HolographicRenderEngine
                Canvas(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    HolographicRenderEngine.run {
                        drawSplicingProgressGenomeSlots(
                            cx = cx,
                            cy = cy,
                            rPx = rPx,
                            sequencedBasesCount = sequencedBasesCount,
                            newlySplicedCreature = newlySplicedCreature,
                            currentSpin = currentSpin,
                            activeColor = activeColor,
                            activeColorDim = activeColorDim,
                            density = density,
                            getBaseColor = getBaseColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress text and/or buttons
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (sequencedBasesCount < 64) "SEQUENCING P.O.X. CYBERGENOME: $splicingProgress%" else "P.O.X. CYBERGENOME SEQUENCED",
                    color = if (sequencedBasesCount < 64) activeColor else CyberGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "[ $sequencedBasesCount / 64 NUCLEOTIDES VERIFIED ]",
                    color = if (sequencedBasesCount < 64) activeColorDim else CyberGreenDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                )

                if (sequencedBasesCount == 64) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(880f, 0.05f, "sine")
                                viewModel.completeSplicingAndViewInVault()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeColor,
                                contentColor = Color.Black,
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = "VIEW IN GEN-VAULT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default,
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
                                contentColor = Color.LightGray,
                            ),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = "RETURN TO SPLICER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default,
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
                repeatMode = RepeatMode.Reverse,
            ),
            label = "segment_pulse",
        )

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Main workspace with Circular Splicing Matrix
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
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
                        .pointerInput(currentSpin, splicerSlots, targetSequence, isCustomScaffoldMode) {
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
                                    val tappedSlotIdx = GraphicsMath.calculateTappedSlotIndex(dx, dy, currentSpin)
                                    val slotGene = splicerSlots.getOrNull(tappedSlotIdx)
                                    if (slotGene != null) {
                                        onShowGeneDetails(slotGene) {
                                            viewModel.ejectGeneFromSlot(tappedSlotIdx)
                                        }
                                    } else {
                                        viewModel.selectSplicerSlot(tappedSlotIdx)
                                        if (isCustomScaffoldMode) {
                                            viewModel.setActiveCreature(null, "splicer_slot")
                                            viewModel.setVaultSubTab("search")
                                            viewModel.selectTab("vault")
                                        }
                                    }
                                }
                            }
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val rPx = radius * densityVal

                    HolographicRenderEngine.run {
                        drawConcentricGridCircles(
                            cx = cx,
                            cy = cy,
                            rPx = rPx,
                            color = activeColorDim,
                            density = density,
                        )

                        drawRadarSweep(
                            cx = cx,
                            cy = cy,
                            rPx = rPx,
                            sweepAngleRad = wavePhase,
                            activeColor = activeColor,
                            activeColorDim = activeColorDim,
                            density = density,
                        )

                        drawOscilloscopeWave(
                            cx = cx,
                            cy = cy,
                            rPx = rPx,
                            splicerSlots = splicerSlots,
                            targetSequence = targetSequence,
                            currentSpin = currentSpin,
                            wavePhase = wavePhase,
                            activeColor = activeColor,
                            activeColorDim = activeColorDim,
                            density = density,
                        )

                        drawCircularGenomeSlots(
                            cx = cx,
                            cy = cy,
                            rPx = rPx,
                            splicerSlots = splicerSlots,
                            targetSequence = targetSequence,
                            activeSlotSelection = activeSlotSelection,
                            currentSpin = currentSpin,
                            pulseAlpha = pulseAlpha,
                            isCustomScaffoldMode = isCustomScaffoldMode,
                            activeColor = activeColor,
                            activeColorDim = activeColorDim,
                            density = density,
                            labelPaint = labelPaint,
                            geneTextPaint = geneTextPaint,
                            getBaseColor = getBaseColor,
                        )
                    }
                }

                // 2. Central coherence status telemetry readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
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
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "$coherencePct%",
                        color = if (isCustomScaffoldMode) {
                            if (coherencePct == 100) Color(0xFF22D3EE) else Color(0xFF22D3EE).copy(alpha = 0.5f)
                        } else {
                            if (coherencePct == 100) {
                                activeColor
                            } else if (coherencePct > 0) {
                                activeColorDim
                            } else {
                                Color.Gray
                            }
                        },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
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
                                val penaltyVal = cCount / 3.0
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
                                val penaltyVal = tCount / 3.0
                                vitalityMod = 1.0 - (penaltyVal / 100.0)
                                attackMod = 1.0 - (penaltyVal / 100.0)
                                defenseMod = 1.0 - (penaltyVal / 100.0)
                                val penaltyPct = "%.1f".format(penaltyVal)
                                vitModText = "(-$penaltyPct% VIT)"
                                atkModText = "(-$penaltyPct% ATK)"
                                defModText = "(-$penaltyPct% DEF)"
                                reagentEffectLines = listOf("+25% SPEED", "-$penaltyPct% VIT/ATK/DEF")
                            }
                        }

                        // Top-left/top-right summary stats overlays
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .graphicsLayer(alpha = infoAlpha),
                        ) {
                            // Left Overlay: Stats
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(4.dp),
                            ) {
                                Text("SPECIMEN SPECS", color = activeColor, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row {
                                    Text("VIT: ${(100 * vitalityMod).toInt()}% ", color = CyberGreen, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    if (vitModText.isNotEmpty()) Text(vitModText, color = if (vitalityMod > 1.0) CyberGreen else Color.Red, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row {
                                    Text("ATK: ${(100 * attackMod).toInt()}% ", color = CyberGreen, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    if (atkModText.isNotEmpty()) Text(atkModText, color = if (attackMod > 1.0) CyberGreen else Color.Red, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row {
                                    Text("DEF: ${(100 * defenseMod).toInt()}% ", color = CyberGreen, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    if (defModText.isNotEmpty()) Text(defModText, color = if (defenseMod > 1.0) CyberGreen else Color.Red, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row {
                                    Text("SPD: ${(100 * speedMod).toInt()}% ", color = CyberGreen, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    if (spdModText.isNotEmpty()) Text(spdModText, color = if (speedMod > 1.0) CyberGreen else Color.Red, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Right Overlay: Active Reagent Details
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.End,
                            ) {
                                Text("REAGENT MODIFIER", color = activeColor, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(2.dp))
                                val reagentDisplayName = when (selectedReagent) {
                                    "REAGENT_LIGASE" -> "DNA LIGASE"
                                    "REAGENT_BETAINE" -> "BETAINE CO-SOLVENT"
                                    "REAGENT_FORMAMIDE" -> "FORMAMIDE DENATURANT"
                                    "REAGENT_TU" -> "T4 POLYNUCLEOTIDE KINASE"
                                    "REAGENT_STAT_VITALITY" -> "VITALITY ADJUVANT"
                                    "REAGENT_STAT_ATTACK" -> "ATTACK ADJUVANT"
                                    "REAGENT_STAT_DEFENSE" -> "DEFENSE ADJUVANT"
                                    "REAGENT_STAT_SPEED" -> "SPEED ADJUVANT"
                                    else -> "NONE"
                                }
                                Text(reagentDisplayName, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(2.dp))
                                reagentEffectLines.forEach { line ->
                                    val isBonus = line.startsWith("+") || line.contains("0%")
                                    Text(line, color = if (isBonus) CyberGreen else Color.Red.copy(alpha = 0.8f), fontSize = 6.5.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Synthesizer Actions and Forced Overrides
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                            contentColor = if (!hasEmpty) Color.Black else Color.Gray,
                        ),
                        border = if (!hasEmpty) null else BorderStroke(1.dp, activeColorDim),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "CONSTRUCT SPECIMEN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
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
                            contentColor = activeColorDim,
                        ),
                        border = BorderStroke(1.dp, activeColorDim),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        HoloDnaAutoSlotIcon(color = activeColorDim)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                            contentColor = if (forcedEnabled) Color.Black else Color.Gray,
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "FORCED CONSTRUCTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
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
                            contentColor = if (isForcedLoopActive) Color.Black else CyberOrange,
                        ),
                        border = BorderStroke(1.dp, if (isForcedLoopActive) CyberOrange else CyberOrange.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "⟲",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }

                val devForceAnomaly by viewModel.devForceAnomaly.collectAsStateWithLifecycle()
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
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "DEV: INJECT MISSING GENES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }
            }
        }
    }
}

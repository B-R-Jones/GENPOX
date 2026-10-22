package com.example.genpox.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.theme.*
import com.example.genpox.ui.components.DualPaneConsoleFrame
import com.example.genpox.ui.components.HoloNavButton
import com.example.genpox.ui.components.PoxSubTab
import com.example.genpox.ui.components.PoxTabFrame
import com.example.genpox.ui.components.PoxUnifiedHoloNav

@Composable
fun GeneticResonanceScope(
    progress: Float,
    isActive: Boolean,
    isAnomaly: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scope_oscillation")
    val wavePhaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val ambientNoiseState = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "noise",
    )

    val scopePath = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cy = h / 2f

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
                style = Stroke(width = 1.8.dp.toPx()),
            )

            drawPath(
                path = scopePath,
                color = activeColor.copy(alpha = 0.2f),
                style = Stroke(width = 4.dp.toPx()),
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
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
fun SplicerView(viewModel: MainViewModel) {
    val targetSequence by viewModel.targetSequence.collectAsStateWithLifecycle()
    val splicerSlots by viewModel.splicerSlots.collectAsStateWithLifecycle()
    val activeSlotSelection by viewModel.activeSlotSelection.collectAsStateWithLifecycle()
    val slotSequenceFilter by viewModel.slotSequenceFilter.collectAsStateWithLifecycle()
    val isReactorFrozen by viewModel.isReactorFrozen.collectAsStateWithLifecycle()
    val reactorFreezeTimeLeft by viewModel.reactorFreezeTimeLeft.collectAsStateWithLifecycle()
    val isForcedConstructionActive by viewModel.isForcedConstructionActive.collectAsStateWithLifecycle()
    val isForcedLoopActive by viewModel.isForcedLoopActive.collectAsStateWithLifecycle()
    val forcedConstructionLogs by viewModel.forcedConstructionLogs.collectAsStateWithLifecycle()
    val isSplicing by viewModel.isSplicing.collectAsStateWithLifecycle()
    val splicingProgress by viewModel.splicingProgress.collectAsStateWithLifecycle()
    val sequencedBasesCount by viewModel.sequencedBasesCount.collectAsStateWithLifecycle()
    val newlySplicedCreature by viewModel.newlySplicedCreature.collectAsStateWithLifecycle()
    val inventoryGenes by viewModel.geneSequences.collectAsStateWithLifecycle()
    val splicerSubTab by viewModel.splicerSubTab.collectAsStateWithLifecycle()

    val selectedReagent by viewModel.selectedReagent.collectAsStateWithLifecycle()

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
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "shimmer_hue",
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
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ">_",
                        color = iconColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }),
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
            drawSubTabs = false,
        ) {
            when (splicerSubTab) {
                "splicer" -> {
                    SplicerMatrixPanel(
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                "terminal" -> {
                    SplicerTerminalLogView(
                        viewModel = viewModel,
                        forcedConstructionLogs = forcedConstructionLogs,
                        isForcedLoopActive = isForcedLoopActive,
                        activeBorder = activeBorder,
                    )
                }
            }
        }

        val isCustomScaffoldMode by viewModel.isCustomScaffoldMode.collectAsStateWithLifecycle()

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
                },
            )
        }

        val splicerSecondaryTabs = listOf(
            PoxSubTab("targeted", "TARGETED") { iconColor ->
                WireframeTargetedIcon(color = iconColor, modifier = Modifier.size(24.dp))
            },
            PoxSubTab("custom", "CUSTOM") { iconColor ->
                WireframeCustomIcon(color = iconColor, modifier = Modifier.size(24.dp))
            },
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
                },
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
            viewModel = viewModel,
        )

        // Slotting UI Floating Panel (holographic popup overlay)
        if (activeSlotSelection != null && !isCustomScaffoldMode && !isForcedConstructionActive && !isSplicing) {
            // Translucent dim backdrop to focus user attention
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.selectSplicerSlot(null) },
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
                    .padding(16.dp),
            ) {
                SplicerSlotPanel(
                    viewModel = viewModel,
                    activeSlotSelection = activeSlotSelection,
                    targetSequence = targetSequence,
                    slotSequenceFilter = slotSequenceFilter,
                    inventoryGenes = inventoryGenes,
                    isWide = false,
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
                },
            )
        }
    }
}

@Composable
fun SplicerTestView(viewModel: MainViewModel) {
    val targetSequence by viewModel.targetSequence.collectAsStateWithLifecycle()
    val splicerSlots by viewModel.splicerSlots.collectAsStateWithLifecycle()

    val targetBlocks = remember(targetSequence) {
        (0 until 8).map { i ->
            if (i * 8 + 8 <= targetSequence.length) targetSequence.substring(i * 8, i * 8 + 8) else "--------"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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
                    fontFamily = FontFamily.Default,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (i in 0 until 8) {
                        val slotGene = splicerSlots.getOrNull(i)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "SLOT #${i + 1}",
                                color = Color.Gray,
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                            )
                            Text(
                                text = slotGene ?: "--------",
                                color = if (slotGene != null) CyberGreen else Color.DarkGray,
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
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
                    fontFamily = FontFamily.Default,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    targetBlocks.chunked(4).forEach { rowBlocks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            rowBlocks.forEach { block ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = block,
                                        color = Color(0xFFA855F7),
                                        style = Typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

package com.example.genpox.ui.main

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.pager.*
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
import com.example.genpox.data.GeneticsEngine
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

private fun getStandardBaseForIndex(index: Int): Char {
    return when (index) {
        0, 1 -> 'A'
        2, 3 -> 'G'
        4, 5 -> 'T'
        6, 7 -> 'C'
        else -> 'A'
    }
}

private fun getStandardCounterpart(seq: String): String {
    val anomalousChars = "XZYW?!$%&@#"
    return seq.mapIndexed { index, char ->
        if (char in anomalousChars) {
            getStandardBaseForIndex(index)
        } else {
            char
        }
    }.joinToString("")
}

private fun matchesPrefix(gene: String, prefix: String): Boolean {
    if (gene.length < prefix.length) return false
    for (i in prefix.indices) {
        val pChar = prefix[i]
        val gChar = gene[i]
        if (pChar in "XZYW?!$%&@#") {
            if (gChar != pChar) return false
        } else {
            val gStandard = getStandardBaseForIndex(i)
            val gCharStandard = if (gChar in "XZYW?!$%&@#") gStandard else gChar
            if (gCharStandard != pChar) return false
        }
    }
    return true
}

private fun getAnomalousOptionsForCouple(couple: String, activeStep: Int, anomalousWindowIndex: Int): List<String> {
    if (anomalousWindowIndex / 2 != activeStep) return listOf(couple)
    val anomPos = anomalousWindowIndex % 2
    val anomalousBases = listOf("X", "Z", "Y", "W", "?", "!", "$", "%", "&", "@", "#")
    val c1 = couple[0].toString()
    val c2 = couple[1].toString()
    
    val list = mutableListOf(couple)
    if (anomPos == 0) {
        for (b in anomalousBases) {
            list.add(b + c2)
        }
    } else {
        for (b in anomalousBases) {
            list.add(c1 + b)
        }
    }
    return list
}

private fun calculateAnomalyDepth(prefix: String, couple: String, activeStep: Int, allInventoryGenes: List<String>): Int {
    val fullPrefix = prefix + couple
    val matchingGenes = allInventoryGenes.filter { matchesPrefix(it, fullPrefix) }
    
    var maxDepth = 0
    val anomalousChars = "XZYW?!$%&@#"
    for (gene in matchingGenes) {
        val startIndex = (activeStep + 1) * 2
        for (i in startIndex until 8) {
            if (i < gene.length && gene[i] in anomalousChars) {
                val stepOfAnom = i / 2
                val depth = stepOfAnom - activeStep
                if (depth > maxDepth) {
                    maxDepth = depth
                }
            }
        }
    }
    return maxDepth
}

// ==========================================
// STEP SEARCH VIEW
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StepSearchView(
    viewModel: MainViewModel,
    activeColor: Color,
    activeColorDim: Color,
    activeBorder: Color,
    activePanel: Color,
    isAnomaly: Boolean,
    onSelectGene: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDeconstructGene: ((GeneSequence) -> Unit)? = null
) {
    val geneSequences by viewModel.geneSequences.collectAsState()
    val openedFromState = viewModel.creatureCardOpenedFrom.collectAsState()
    val isCustomScaffoldModeState = viewModel.isCustomScaffoldMode.collectAsState()
    
    val selectedHostPlanet by viewModel.selectedHostPlanet.collectAsState()
    val hostIndex = when (selectedHostPlanet.uppercase()) {
        "MERCURY" -> 1
        "VENUS" -> 2
        "EARTH" -> 3
        "MARS" -> 4
        "JUPITER" -> 5
        "SATURN" -> 6
        "URANUS" -> 7
        "NEPTUNE" -> 8
        else -> 3
    }
    val anomalousWindowIndex = hostIndex - 1
    
    var stepSearchPrefix by remember { mutableStateOf("") }
    var viewStepSearchMatchesOnly by remember { mutableStateOf(false) }
    var activeSubSearchCouple by remember { mutableStateOf<String?>(null) }

    val matches = remember(geneSequences, stepSearchPrefix) {
        geneSequences.filter { matchesPrefix(it.sequence, stepSearchPrefix) }
    }
    val matchCount = remember(matches) { matches.sumOf { it.count } }

    val stepSize = if (isAnomaly) 1 else 2
    val maxSteps = if (isAnomaly) 8 else 4
    val activeStep = stepSearchPrefix.length / stepSize
    val stepLabels = if (isAnomaly) {
        listOf("1bp", "2bp", "3bp", "4bp", "5bp", "6bp", "7bp", "8bp")
    } else {
        listOf("1-2bp", "3-4bp", "5-6bp", "7-8bp")
    }

    val uniqueGenesSize = remember(geneSequences) { geneSequences.map { it.sequence }.distinct().size }
    val multiCountGenesSize = remember(geneSequences) {
        geneSequences.groupBy { it.sequence }.count { it.value.size > 1 }
    }

    val isDone = activeStep == maxSteps
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (isDone) Modifier.verticalScroll(scrollState) else Modifier),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Text(
            text = "MOLECULAR STEP-SEARCH DIRECTORY",
            color = activeColorDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
            modifier = Modifier.fillMaxWidth()
        )

        // Registry Counts Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeBorder, RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UNIQUE GENE IDS",
                    color = activeColorDim,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 9.sp
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⬢ ", color = activeColor, fontSize = 14.sp)
                    Text(
                        text = "$uniqueGenesSize",
                        color = Color.White,
                        style = Typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(35.dp)
                    .background(activeBorder)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "MULTI-COUNT GENE IDS",
                    color = activeColorDim,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 9.sp
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⬢ ", color = activeColor, fontSize = 14.sp)
                    Text(
                        text = "$multiCountGenesSize",
                        color = Color.White,
                        style = Typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Progress indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stepLabels.forEachIndexed { idx, stepLabel ->
                val isCompleted = activeStep > idx
                val isActive = activeStep == idx
                val isPrefixFilled = if (isAnomaly) {
                    stepSearchPrefix.length > idx
                } else {
                    stepSearchPrefix.length >= (idx + 1) * 2
                }
                
                val displayText = if (isAnomaly) {
                    if (stepSearchPrefix.length > idx) {
                        stepSearchPrefix[idx].toString()
                    } else {
                        stepLabel
                    }
                } else {
                    if (stepSearchPrefix.length >= (idx + 1) * 2) {
                        stepSearchPrefix.substring(idx * 2, (idx + 1) * 2)
                    } else {
                        stepLabel
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (isActive) activeColor else if (isCompleted) activeColorDim else Color.DarkGray,
                            RoundedCornerShape(4.dp)
                        )
                        .background(
                            if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        style = Typography.labelSmall,
                        fontFamily = if (isPrefixFilled) FontFamily.Monospace else FontFamily.Default,
                        color = if (isPrefixFilled) Color.White else if (isActive || isCompleted) activeColor else Color.Gray,
                        fontWeight = if (isActive || isPrefixFilled) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isAnomaly) 10.sp else 12.sp
                    )
                }
            }
        }

        // Query & Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeBorder, RoundedCornerShape(4.dp)).background(activePanel, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: UNDO
                val undoEnabled = activeStep > 0
                Button(
                    onClick = {
                        viewModel.synthManager.playCombinatorTick()
                        stepSearchPrefix = stepSearchPrefix.dropLast(stepSize)
                        viewStepSearchMatchesOnly = false
                    },
                    enabled = undoEnabled,
                    modifier = Modifier.weight(1f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (undoEnabled) Color(0xFFFBBF24) else Color.Transparent,
                        contentColor = if (undoEnabled) Color.Black else Color.Gray
                    ),
                    border = if (undoEnabled) null else BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "↶ UNDO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                }

                // Button 2: RESET
                val resetEnabled = stepSearchPrefix.isNotEmpty()
                Button(
                    onClick = {
                        viewModel.synthManager.playReject()
                        stepSearchPrefix = ""
                        viewStepSearchMatchesOnly = false
                    },
                    enabled = resetEnabled,
                    modifier = Modifier.weight(1f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resetEnabled) Color(0xFFFBBF24) else Color.Transparent,
                        contentColor = if (resetEnabled) Color.Black else Color.Gray
                    ),
                    border = if (resetEnabled) null else BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "✕ RESET",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                }

                // Button 3: RECYCLE
                val recycleEnabled = stepSearchPrefix.isNotEmpty() && matchCount > 0
                val wasteYield = matchCount * 8
                Button(
                    onClick = {
                        viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                        viewModel.recycleMatchingGenes(stepSearchPrefix)
                        stepSearchPrefix = ""
                        viewStepSearchMatchesOnly = false
                    },
                    enabled = recycleEnabled,
                    modifier = Modifier.weight(1f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (recycleEnabled) Color(0xFFA855F7) else Color.Transparent,
                        contentColor = if (recycleEnabled) Color.Black else Color.Gray
                    ),
                    border = if (recycleEnabled) null else BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "♺ RECYCLE (+$wasteYield W)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                }
            }
        }

        // Main display area: Grid or list of matches
        val allInventoryGenes = remember(geneSequences) { geneSequences.map { it.sequence }.distinct() }

        if (isDone) {
            // Detailed Inline Analysis Card
            val gene = stepSearchPrefix
            val isAnom = WaveMath.isAnomalousGene(gene)
            val benefit = if (isAnom) WaveMath.getBenefitForAnomalousGene(gene) else null

            // Biophysical parameters
            val copies = remember(gene, geneSequences) {
                geneSequences.filter { it.sequence == gene }
            }
            val count = copies.size

            val pageCount = if (count > 0) count else 1
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
            val currentPage = pagerState.currentPage

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        (if (isAnom) Color(0xFFA855F7) else activeColor).copy(alpha = 0.35f),
                        RoundedCornerShape(4.dp)
                    )
                    .background(
                        if (isAnom) Color(0xFF1E0B36) else activePanel,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAnom) {
                        Text(
                            text = "DECRYPTED ANOMALY MOLECULE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = CyberTheme.purpleDim
                        )
                    } else {
                        Text(
                            text = "MOLECULAR ANALYSIS SUMMARY",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = activeColorDim
                        )
                    }
                }

                Text(
                    text = "SEQUENCE: $gene",
                    style = Typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val pageCopy = copies.getOrNull(page)
                    val tm = pageCopy?.meltingTemp ?: GeneticsEngine.calculateMeltingTemperature(gene, viewModel.reactorSalt.value.toDouble())
                    val mfe = pageCopy?.minimumFreeEnergy ?: GeneticsEngine.calculateMinimumFreeEnergy(gene)
                    val qScore = pageCopy?.averageQScore ?: 30.0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, (if (isAnom) Color(0xFFD8B4FE) else activeColor).copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "THERMODYNAMIC Tm: " + String.format(Locale.US, "%.2f°C", tm),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MIN FREE ENERGY: " + String.format(Locale.US, "%.2f kcal/mol", mfe),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AVERAGE Q-SCORE: " + String.format(Locale.US, "%.1f PHRED", qScore),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IN STOCK: ",
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnom) CyberTheme.purpleDim else activeColorDim
                    )
                    Text(
                        text = if (count > 0) "(${currentPage + 1}/$count)" else "(0/0)",
                        style = Typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                    )
                }

                if (isAnom && benefit != null) {
                    Text(
                        text = "TACTICAL PERK: ${benefit.name.uppercase()}",
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Default,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = benefit.description.uppercase(),
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        color = Color.LightGray,
                        lineHeight = 12.sp
                    )
                } else {
                    Text(
                        text = "STANDARD STABLE SEQUENCE. IDEAL FOR CREATURE TELOMERE REINFORCEMENT OPERATIONS.",
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        color = Color.LightGray,
                        lineHeight = 12.sp
                    )
                }

                val currentCopy = copies.getOrNull(currentPage)
                val currentTm = currentCopy?.meltingTemp ?: GeneticsEngine.calculateMeltingTemperature(gene, viewModel.reactorSalt.value.toDouble())
                val currentMfe = currentCopy?.minimumFreeEnergy ?: GeneticsEngine.calculateMinimumFreeEnergy(gene)
                val currentQScore = currentCopy?.averageQScore ?: 30.0

                val resolvedGeneSeq = currentCopy ?: GeneSequence(
                    sequence = gene,
                    count = 1,
                    discoveredAt = System.currentTimeMillis(),
                    averageQScore = currentQScore,
                    meltingTemp = currentTm,
                    minimumFreeEnergy = currentMfe
                )

                val openedFrom = openedFromState.value
                val isCustomScaffoldMode = isCustomScaffoldModeState.value

                if (openedFrom == "message_gene") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                            viewModel.setComposerAttachedGene(resolvedGeneSeq)
                            viewModel.setActiveCreature(null, "vault")
                            viewModel.selectTab("network")
                        },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "ATTACH GENE BLOCK",
                            color = Color.Black,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (openedFrom == "splicer_slot") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                            viewModel.assignGeneToSlot(resolvedGeneSeq.sequence)
                            viewModel.setActiveCreature(null, "vault")
                            if (isCustomScaffoldMode) {
                                viewModel.selectTab("splicer")
                            }
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "LOAD INTO SPLICER",
                            color = Color.Black,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (openedFrom != "message_gene" && openedFrom != "splicer_slot" && count > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                            onDeconstructGene?.invoke(resolvedGeneSeq)
                        },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberRed,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "DECONSTRUCT GENE BLOCK",
                            color = Color.Black,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (viewStepSearchMatchesOnly) {
            // Matches View
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESOLVED STOCK MATCHES:",
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        color = activeColorDim
                    )
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                            viewStepSearchMatchesOnly = false
                        },
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Yellow
                        ),
                        border = BorderStroke(1.dp, Color.Yellow),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "✕ BACK",
                            color = Color.Yellow,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                val matches = remember(allInventoryGenes, stepSearchPrefix) {
                    allInventoryGenes.filter { matchesPrefix(it, stepSearchPrefix) }
                }
                
                if (matches.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO MATCHING STANDARD OR ANOMALOUS GENES FOUND.",
                            color = activeColorDim,
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(matches) { matchSeq ->
                            val count = geneSequences.find { it.sequence == matchSeq }?.count ?: 0
                            val isAnom = WaveMath.isAnomalousGene(matchSeq)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isAnom) Color(0xFFA855F7) else activeBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        if (isAnom) Color(0xFF1E0B36) else activePanel,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        onSelectGene(matchSeq)
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = matchSeq,
                                        style = Typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                                    )
                                    Text(
                                        text = if (isAnom) "ANOMALY SECTOR" else "STANDARD GENE",
                                        style = Typography.labelSmall,
                                        fontFamily = FontFamily.Default,
                                        color = if (isAnom) Color(0xFFA855F7) else activeColorDim
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "QTY: ",
                                        style = Typography.bodyMedium,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                                    )
                                    Text(
                                        text = "x$count",
                                        style = Typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Grid View (Select Couple or Base)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (activeSubSearchCouple != null) "SELECT ANOMALOUS PAIR COMPATIBLE WITH INVENTORY:" else if (isAnomaly) "SELECT ANOMALY BASE TO FILTER:" else "SELECT SEQUENCE COUPLE TO FILTER:",
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    color = if (activeSubSearchCouple != null) Color(0xFFA855F7) else activeColorDim,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (activeSubSearchCouple != null) {
                    val targetCouple = activeSubSearchCouple!!
                    val subOptions = getAnomalousOptionsForCouple(targetCouple, activeStep, anomalousWindowIndex)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TARGET: $targetCouple (STEP ${activeStep + 1})",
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD8B4FE)
                        )
                        Text(
                            text = "✕ CANCEL",
                            color = Color.Yellow,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                activeSubSearchCouple = null
                                viewModel.synthManager.playReject()
                            }
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(subOptions) { option ->
                            val tentative = stepSearchPrefix + option
                            val isFinalStep = tentative.length == 8
                            val matchCount = allInventoryGenes.count { matchesPrefix(it, tentative) }
                            val displayCount = if (isFinalStep) {
                                geneSequences.filter { matchesPrefix(it.sequence, tentative) }.sumOf { it.count }
                            } else {
                                matchCount
                            }
                            val displayLabel = if (isFinalStep) " STOCK" else " TYPES"
                            val isEnabled = matchCount > 0
                            val isOptionAnomalous = WaveMath.isAnomalousGene(option)
                            
                            Box(
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isEnabled) {
                                            if (isOptionAnomalous) Color(0xFFA855F7) else activeColor
                                        } else Color.DarkGray,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        if (isEnabled) {
                                            if (isOptionAnomalous) Color(0xFF1E0B36) else activePanel
                                        } else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable(enabled = isEnabled) {
                                        stepSearchPrefix = tentative
                                        activeSubSearchCouple = null
                                        viewModel.synthManager.playSynthesisSuccess()
                                    }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = option,
                                        style = Typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnabled) {
                                            if (isOptionAnomalous) Color(0xFFD8B4FE) else Color.White
                                        } else Color.Gray.copy(alpha = 0.3f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (isEnabled) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "x",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Default,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOptionAnomalous) Color(0xFFD8B4FE) else activeColor
                                            )
                                            Text(
                                                text = "$displayCount",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOptionAnomalous) Color(0xFFD8B4FE) else activeColor
                                            )
                                            Text(
                                                text = displayLabel,
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Default,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOptionAnomalous) Color(0xFFD8B4FE) else activeColor
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "-",
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (isAnomaly) {
                    val options = listOf("X", "Z", "Y", "W", "?", "!", "$", "%", "&", "@", "#")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(options) { base ->
                            val tentative = stepSearchPrefix + base
                            val isFinalStep = tentative.length == 8
                            val matchCount = allInventoryGenes.count { matchesPrefix(it, tentative) }
                            val displayCount = if (isFinalStep) {
                                geneSequences.filter { matchesPrefix(it.sequence, tentative) }.sumOf { it.count }
                            } else {
                                matchCount
                            }
                            val displayLabel = if (isFinalStep) " STOCK" else " TYPES"
                            val isEnabled = matchCount > 0
                            
                            Box(
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isEnabled) activeColor else Color.DarkGray,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        if (isEnabled) activePanel else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable(enabled = isEnabled) {
                                        stepSearchPrefix = tentative
                                        viewModel.synthManager.playCombinatorTick()
                                    }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = base,
                                        style = Typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnabled) Color.White else Color.Gray.copy(alpha = 0.3f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (isEnabled) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "x",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Default,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeColor
                                            )
                                            Text(
                                                text = "$displayCount",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeColor
                                            )
                                            Text(
                                                text = displayLabel,
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Default,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeColor
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "-",
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val couples = listOf("AA", "AC", "AG", "AT", "CA", "CC", "CG", "CT", "GA", "GC", "GG", "GT", "TA", "TC", "TG", "TT")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(couples) { couple ->
                            val tentative = stepSearchPrefix + couple
                            val isFinalStep = tentative.length == 8
                            val matchCount = allInventoryGenes.count { matchesPrefix(it, tentative) }
                            val displayCount = if (isFinalStep) {
                                geneSequences.filter { matchesPrefix(it.sequence, tentative) }.sumOf { it.count }
                            } else {
                                matchCount
                            }
                            val displayLabel = if (isFinalStep) " STOCK" else " TYPES"
                            val isEnabled = matchCount > 0
                            
                            val hasAnomWindow = anomalousWindowIndex / 2 == activeStep
                            val anomPos = if (hasAnomWindow) anomalousWindowIndex % 2 else -1
                            val depthDots = calculateAnomalyDepth(stepSearchPrefix, couple, activeStep, allInventoryGenes)

                            val annotatedText = buildAnnotatedString {
                                val char1 = couple[0].toString()
                                val char2 = couple[1].toString()
                                if (anomPos == 0) {
                                    withStyle(style = SpanStyle(color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)) {
                                        append(char1)
                                    }
                                    withStyle(style = SpanStyle(color = if (isEnabled) Color.White else Color.Gray.copy(alpha = 0.3f))) {
                                        append(char2)
                                    }
                                } else if (anomPos == 1) {
                                    withStyle(style = SpanStyle(color = if (isEnabled) Color.White else Color.Gray.copy(alpha = 0.3f))) {
                                        append(char1)
                                    }
                                    withStyle(style = SpanStyle(color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)) {
                                        append(char2)
                                    }
                                } else {
                                    withStyle(style = SpanStyle(color = if (isEnabled) Color.White else Color.Gray.copy(alpha = 0.3f))) {
                                        append(couple)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isEnabled) activeColor else Color.DarkGray,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        if (isEnabled) activePanel else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .combinedClickable(
                                        enabled = isEnabled,
                                        onClick = {
                                            stepSearchPrefix = tentative
                                            viewModel.synthManager.playCombinatorTick()
                                        },
                                        onLongClick = {
                                            if (hasAnomWindow) {
                                                activeSubSearchCouple = couple
                                                viewModel.synthManager.playBeep(520f, 0.1f, "sine")
                                            }
                                        }
                                    )
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = annotatedText,
                                            style = Typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (isEnabled) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "x",
                                                    style = Typography.labelSmall,
                                                    fontFamily = FontFamily.Default,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activeColor
                                                )
                                                Text(
                                                    text = "$displayCount",
                                                    style = Typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activeColor
                                                )
                                                Text(
                                                    text = displayLabel,
                                                    style = Typography.labelSmall,
                                                    fontFamily = FontFamily.Default,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activeColor
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "-",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                    
                                    if (isEnabled && depthDots > 0) {
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 2.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            repeat(depthDots) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.dp)
                                                        .background(Color(0xFFA855F7), shape = RoundedCornerShape(1.5.dp))
                                                )
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

        // Intermediate matches shortcut button
        if (!isDone && stepSearchPrefix.isNotEmpty() && !viewStepSearchMatchesOnly) {
            Button(
                onClick = {
                    viewStepSearchMatchesOnly = true
                    viewModel.synthManager.playCombinatorTick()
                },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = activeColor
                ),
                border = BorderStroke(1.dp, activeColor),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "VIEW INTERMEDIATE MATCHES",
                    color = activeColor,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

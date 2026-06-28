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
import com.example.genpox.data.BiophysicsEngine
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
// STEP SEARCH VIEW
// ==========================================
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
    
    var stepSearchPrefix by remember { mutableStateOf("") }
    var viewStepSearchMatchesOnly by remember { mutableStateOf(false) }

    val matches = remember(geneSequences, stepSearchPrefix) {
        geneSequences.filter { it.sequence.startsWith(stepSearchPrefix, ignoreCase = true) }
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

    val uniqueGenesSize = remember(geneSequences) { geneSequences.size }
    val multiCountGenesSize = remember(geneSequences) { geneSequences.count { it.count > 1 } }

    Column(
        modifier = modifier.fillMaxSize(),
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
                .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
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
                        .cyberglass(
                            borderColor = if (isActive) activeColor else if (isCompleted) activeColorDim.copy(alpha = 0.5f) else Color.DarkGray,
                            backgroundColor = if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent
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
                .cyberglass(borderColor = activeBorder, backgroundColor = activePanel)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: UNDO
                val undoEnabled = activeStep > 0
                PoxButton(
                    modifier = Modifier.weight(1f),
                    text = "↶ UNDO",
                    onClick = {
                        stepSearchPrefix = stepSearchPrefix.dropLast(stepSize)
                        viewStepSearchMatchesOnly = false
                    },
                    enabled = undoEnabled,
                    buttonType = PoxButtonType.YELLOW_WARNING,
                    buttonSize = PoxButtonSize.STANDARD,
                    sound = PoxButtonSound.COMBINATOR_TICK,
                    viewModel = viewModel
                )

                // Button 2: RESET
                val resetEnabled = stepSearchPrefix.isNotEmpty()
                PoxButton(
                    modifier = Modifier.weight(1f),
                    text = "✕ RESET",
                    onClick = {
                        stepSearchPrefix = ""
                        viewStepSearchMatchesOnly = false
                    },
                    enabled = resetEnabled,
                    buttonType = PoxButtonType.YELLOW_WARNING,
                    buttonSize = PoxButtonSize.STANDARD,
                    sound = PoxButtonSound.REJECT_BEEP,
                    viewModel = viewModel
                )

                // Button 3: RECYCLE
                val recycleEnabled = stepSearchPrefix.isNotEmpty() && matchCount > 0
                val wasteYield = matchCount * 8
                PoxButton(
                    modifier = Modifier.weight(1f),
                    text = "♺ RECYCLE (+$wasteYield W)",
                    onClick = {
                        viewModel.recycleMatchingGenes(stepSearchPrefix)
                        stepSearchPrefix = ""
                        viewStepSearchMatchesOnly = false
                    },
                    enabled = recycleEnabled,
                    buttonType = PoxButtonType.PURPLE_ANOMALY,
                    buttonSize = PoxButtonSize.STANDARD,
                    sound = PoxButtonSound.BEEP_DEFAULT,
                    viewModel = viewModel
                )
            }
        }

        // Main display area: Grid or list of matches
        val allInventoryGenes = remember(geneSequences) { geneSequences.map { it.sequence } }
        val isDone = activeStep == maxSteps

        if (isDone) {
            // Detailed Inline Analysis Card
            val gene = stepSearchPrefix
            val isAnom = WaveMath.isAnomalousGene(gene)
            val benefit = if (isAnom) WaveMath.getBenefitForAnomalousGene(gene) else null

            // Biophysical parameters
            val geneSequence = remember(gene, geneSequences) {
                geneSequences.find { it.sequence == gene }
            }
            val tm = remember(gene, geneSequence, viewModel.reactorSalt) {
                geneSequence?.meltingTemp ?: BiophysicsEngine.calculateMeltingTemperature(gene, viewModel.reactorSalt.value.toDouble())
            }
            val mfe = remember(gene, geneSequence) {
                geneSequence?.minimumFreeEnergy ?: BiophysicsEngine.calculateMinimumFreeEnergy(gene)
            }
            val qScore = remember(gene, geneSequence) {
                geneSequence?.averageQScore ?: 30.0
            }
            val count = geneSequence?.count ?: 0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberglass(
                        borderColor = if (isAnom) Color(0xFFA855F7) else activeBorder,
                        backgroundColor = if (isAnom) Color(0xFF1E0B36) else activePanel
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAnom) {
                        WireframeGalaxy(
                            color = Color(0xFFD8B4FE),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "DECRYPTED ANOMALY MOLECULE",
                            style = Typography.titleSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD8B4FE)
                        )
                    } else {
                        WireframeDna(
                            color = activeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "MOLECULAR ANALYSIS SUMMARY",
                            style = Typography.titleSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, (if (isAnom) Color(0xFFD8B4FE) else activeColor).copy(alpha = 0.3f), RoundedCornerShape(2.dp))
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
                        text = "ACCURATE Q-SCORE: " + String.format(Locale.US, "%.1f (Phred)", qScore),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
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

                if (geneSequence != null && count > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberglass(
                                borderColor = Color(0xFFEF4444),
                                backgroundColor = Color(0xFF7F1D1D).copy(alpha = 0.15f)
                            )
                            .clickable {
                                viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                                onDeconstructGene?.invoke(geneSequence)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♺ DECONSTRUCT GENE BLOCK",
                            color = Color(0xFFFCA5A5),
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
                    Box(
                        modifier = Modifier
                            .cyberglass(borderColor = Color.Yellow, backgroundColor = Color.Transparent)
                            .clickable {
                                viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                viewStepSearchMatchesOnly = false
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
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
                    allInventoryGenes.filter { it.startsWith(stepSearchPrefix) }
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
                                    .cyberglass(
                                        borderColor = if (isAnom) Color(0xFFA855F7).copy(alpha = 0.5f) else activeBorder,
                                        backgroundColor = if (isAnom) Color(0xFF1E0B36) else activePanel
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
                    text = if (isAnomaly) "SELECT ANOMALY BASE TO FILTER:" else "SELECT SEQUENCE COUPLE TO FILTER:",
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    color = activeColorDim,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (isAnomaly) {
                    val options = listOf("X", "Z", "Y", "W", "?", "!", "$", "%", "&", "@", "#")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(options) { base ->
                            val tentative = stepSearchPrefix + base
                            val matchCount = allInventoryGenes.count { it.startsWith(tentative) }
                            val isEnabled = matchCount > 0
                            
                            Box(
                                modifier = Modifier
                                    .cyberglass(
                                        borderColor = if (isEnabled) activeColor else Color.DarkGray.copy(alpha = 0.2f),
                                        backgroundColor = if (isEnabled) activePanel else Color.Transparent
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
                                                text = "$matchCount",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeColor
                                            )
                                            Text(
                                                text = " TYPES",
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
                            val matchCount = allInventoryGenes.count { it.startsWith(tentative) }
                            val isEnabled = matchCount > 0
                            
                            Box(
                                modifier = Modifier
                                    .cyberglass(
                                        borderColor = if (isEnabled) activeColor else Color.DarkGray.copy(alpha = 0.2f),
                                        backgroundColor = if (isEnabled) activePanel else Color.Transparent
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
                                        text = couple,
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
                                                text = "$matchCount",
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeColor
                                            )
                                            Text(
                                                text = " TYPES",
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
                }
            }
        }

        // Intermediate matches shortcut button
        if (!isDone && stepSearchPrefix.isNotEmpty() && !viewStepSearchMatchesOnly) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .cyberglass(
                        borderColor = activeColorDim.copy(alpha = 0.5f),
                        backgroundColor = activeColorDim.copy(alpha = 0.15f)
                    )
                    .clickable {
                        viewStepSearchMatchesOnly = true
                        viewModel.synthManager.playCombinatorTick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VIEW INTERMEDIATE MATCHES",
                    style = Typography.labelSmall,
                    color = activeColor,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

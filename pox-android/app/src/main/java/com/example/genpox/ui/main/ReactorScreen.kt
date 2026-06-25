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
import kotlinx.coroutines.flow.StateFlow
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
fun HolographicHelixReactor(
    activeColor: Color,
    isActive: Boolean,
    isAnomaly: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "helix_rotation")
    val rotationPhaseState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 4000 else 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    val pulseScaleState = transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glitchProgressState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glitch"
    )
    
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        
        val rotationPhase = rotationPhaseState.value
        val pulseScale = pulseScaleState.value
        val glitchProgress = glitchProgressState.value
        
        val isGlitching = isAnomaly && (glitchProgress < 0.15f)
        val numStrands = 14
        val helixRadius = w * 0.25f * pulseScale
        val lineStroke = 1.2.dp.toPx()
        val glitchCenterY = glitchProgress * h
        
        for (i in 0 until numStrands) {
            val t = i.toFloat() / (numStrands - 1)
            val cyPos = h * 0.12f + t * h * 0.76f
            
            val nodeAngle = t * 3.5f * Math.PI.toFloat() + rotationPhase
            
            val x1 = cx + helixRadius * kotlin.math.cos(nodeAngle.toDouble()).toFloat()
            val z1 = helixRadius * kotlin.math.sin(nodeAngle.toDouble()).toFloat()
            
            val x2 = cx + helixRadius * kotlin.math.cos((nodeAngle + Math.PI.toFloat()).toDouble()).toFloat()
            val z2 = helixRadius * kotlin.math.sin((nodeAngle + Math.PI.toFloat()).toDouble()).toFloat()
            
            var gx1 = x1
            var gx2 = x2
            if (isGlitching && kotlin.math.abs(cyPos - glitchCenterY) < 35.dp.toPx()) {
                val offsetVal = (kotlin.math.sin(cyPos * 0.1f) * 10.dp.toPx()).toFloat()
                gx1 += offsetVal
                gx2 -= offsetVal
            }
            
            val alpha1 = ((z1 + helixRadius) / (2f * helixRadius) * 0.6f + 0.4f).coerceIn(0.1f, 1.0f)
            val alpha2 = ((z2 + helixRadius) / (2f * helixRadius) * 0.6f + 0.4f).coerceIn(0.1f, 1.0f)
            
            val radius1 = ((z1 + helixRadius) / (2f * helixRadius) * 2.5.dp.toPx() + 1.8.dp.toPx())
            val radius2 = ((z2 + helixRadius) / (2f * helixRadius) * 2.5.dp.toPx() + 1.8.dp.toPx())
            
            val barAlpha = ((alpha1 + alpha2) / 2f * 0.4f).coerceIn(0.1f, 0.7f)
            drawLine(
                color = activeColor.copy(alpha = barAlpha),
                start = Offset(gx1, cyPos),
                end = Offset(gx2, cyPos),
                strokeWidth = lineStroke * 0.7f
            )
            
            drawCircle(
                color = activeColor.copy(alpha = alpha1),
                radius = radius1,
                center = Offset(gx1, cyPos)
            )
            
            drawCircle(
                color = (if (isAnomaly) Color(0xFFC084FC) else activeColor).copy(alpha = alpha2),
                radius = radius2,
                center = Offset(gx2, cyPos)
            )
        }
    }
}

@Composable
fun ReactorVisualChamber(
    metrics: CanvasMetrics,
    geneSequenceStrings: List<String>,
    activeColor: Color,
    isActive: Boolean,
    isAnomaly: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .cyberglass(borderColor = activeColor, glowColor = activeColor.copy(alpha = 0.15f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            NodeCrystalCanvas(
                metrics = metrics,
                inventoryStrings = geneSequenceStrings,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(0.8f)
                .background(activeColor.copy(alpha = 0.3f))
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            HolographicHelixReactor(
                activeColor = activeColor,
                isActive = isActive,
                isAnomaly = isAnomaly,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun TranscriptionDecoderTicker(
    scrollingGeneFlow: StateFlow<String>,
    poxReactorActiveProvider: () -> Boolean,
    anomalyEngineActiveProvider: () -> Boolean,
    isAnomaly: Boolean,
    activeColor: Color,
    activeColorDim: Color,
    modifier: Modifier = Modifier
) {
    val scrollingGene by scrollingGeneFlow.collectAsState()
    val displayText = remember(scrollingGene, isAnomaly) {
        if (isAnomaly) {
            val active = anomalyEngineActiveProvider()
            if (!active) {
                "--------"
            } else {
                val syms = "XZYW?!$%&@#"
                scrollingGene.mapIndexed { i, char ->
                    val idx = (char.code + i) % syms.length
                    syms[idx]
                }.joinToString("")
            }
        } else {
            val active = poxReactorActiveProvider()
            if (!active) {
                "--------"
            } else {
                scrollingGene
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .cyberglass(borderColor = activeColor.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TRANSCRIPTION DECODER:",
            color = activeColorDim,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default
        )
        Text(
            text = displayText,
            color = activeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ResonanceScopeChamber(
    progressProvider: () -> Float,
    isActive: Boolean,
    isAnomaly: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .cyberglass(borderColor = activeColor, glowColor = activeColor.copy(alpha = 0.15f))
            .padding(2.dp)
    ) {
        val progress = progressProvider()
        GeneticResonanceScope(
            progress = progress,
            isActive = isActive,
            isAnomaly = isAnomaly,
            activeColor = activeColor,
            modifier = Modifier.fillMaxSize()
        )
        
        Text(
            text = String.format(Locale.US, "RESONANCE: %d%%", (progress * 100).toInt()),
            color = activeColor.copy(alpha = 0.7f),
            style = Typography.labelSmall,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        )
    }
}

@Composable
fun ReactorTimerStatus(
    statusProvider: () -> String,
    colorProvider: () -> Color,
    boostSecondsLeftProvider: () -> Int,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = statusProvider(),
            color = colorProvider(),
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            style = Typography.bodySmall
        )

        val boostSecondsLeft = boostSecondsLeftProvider()
        if (boostSecondsLeft > 0) {
            Text(
                text = String.format(Locale.US, "REACTOR BOOST ACTIVE: %02d:%02d REMAINING", boostSecondsLeft / 60, boostSecondsLeft % 60),
                color = activeColor,
                fontWeight = FontWeight.Bold,
                style = Typography.labelSmall,
                fontFamily = FontFamily.Default,
                modifier = Modifier
                    .background(activeColor.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                    .border(1.dp, activeColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ==========================================
// REACTOR DASHBOARD VIEW
// ==========================================
@Composable
fun ReactorDashboardView(
    viewModel: MainViewModel,
    activeBorder: Color,
    activePanel: Color
) {
    val geneSequences by viewModel.geneSequences.collectAsState()
    val devForceAnomaly by viewModel.devForceAnomaly.collectAsState()
    
    val uniqueGenesSize = remember(geneSequences) { geneSequences.size }
    val multiCountGenesSize = remember(geneSequences) { geneSequences.count { it.count > 1 } }
    
    val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
    val todayWave = wave
    val tomorrowWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 86400000L)
    val dayAfterWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 172800000L)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Counts section
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
                    color = CyberGreenDim,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 9.sp
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⬢ ", color = CyberGreen, fontSize = 14.sp)
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
                    .background(CyberBorder)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "MULTI-COUNT GENE IDS",
                    color = CyberGreenDim,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 9.sp
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⬢ ", color = CyberGreen, fontSize = 14.sp)
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

        // Today's base-pair wave card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(
                    borderColor = if (todayWave.isSuppressed) Color(0xFF990000) else CyberGreen,
                    backgroundColor = if (todayWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.6f)
                )
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "⚡",
                        color = if (todayWave.isSuppressed) Color.Red else Color(0xFFFFB300),
                        fontSize = 14.sp
                    )
                    Column {
                        Text(
                            text = "TODAY'S BASE-PAIR WAVE",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (todayWave.isSuppressed) "DORMANT (CONGESTED DECAY)" else "ACTIVE: ${todayWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!todayWave.isSuppressed) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${todayWave.primary} ➔ ${todayWave.secondary}",
                            color = CyberGreen,
                            style = Typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "1.12x & 1.62x BOOST",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "NULL",
                        color = Color.Red,
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF330000), RoundedCornerShape(2.dp))
                            .border(1.dp, Color.Red, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Forecast Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tomorrow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .cyberglass(
                        borderColor = if (tomorrowWave.isSuppressed) Color(0xFF990000) else Color.DarkGray,
                        backgroundColor = if (tomorrowWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f)
                    )
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOMORROW BASE-PAIR",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (tomorrowWave.isSuppressed) "DORMANT" else "${tomorrowWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!tomorrowWave.isSuppressed) {
                        Text(
                            text = "${tomorrowWave.primary}➔${tomorrowWave.secondary}",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Day After Tomorrow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .cyberglass(
                        borderColor = if (dayAfterWave.isSuppressed) Color(0xFF990000) else Color.DarkGray,
                        backgroundColor = if (dayAfterWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f)
                    )
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAY AFTER TOMORROW",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (dayAfterWave.isSuppressed) "DORMANT" else "${dayAfterWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!dayAfterWave.isSuppressed) {
                        Text(
                            text = "${dayAfterWave.primary}➔${dayAfterWave.secondary}",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        if (devForceAnomaly) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.addDevGenes() },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x30F97316),
                    contentColor = Color(0xFFF97316)
                ),
                border = BorderStroke(1.dp, Color(0xFFF97316)),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "DEV: INJECT 10K GENES",
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ReactorParametersView(
    viewModel: MainViewModel,
    activeBorder: Color,
    activePanel: Color
) {
    val rawStockA by viewModel.rawStockA.collectAsState()
    val rawStockG by viewModel.rawStockG.collectAsState()
    val rawStockT by viewModel.rawStockT.collectAsState()
    val rawStockC by viewModel.rawStockC.collectAsState()
    val tempState by viewModel.reactorTemperature.collectAsState()
    val saltState by viewModel.reactorSalt.collectAsState()
    val activePolyState by viewModel.activePolymerase.collectAsState()
    val activeSoluteState by viewModel.activeChemicalSolute.collectAsState()
    val inletRatioAState by viewModel.inletRatioA.collectAsState()
    val inletRatioGState by viewModel.inletRatioG.collectAsState()
    val inletRatioTState by viewModel.inletRatioT.collectAsState()
    val inletRatioCState by viewModel.inletRatioC.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. RAW NUCLEOTIDE STOCKPILE FEEDSTOCK
        Text(
            text = "RAW FEEDSTOCK RESERVES",
            color = CyberGreenDim,
            style = Typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val stockItems = listOf(
                Triple("A", rawStockA, Color(0xFF38BDF8)),
                Triple("G", rawStockG, Color(0xFF34D399)),
                Triple("T", rawStockT, Color(0xFFFB7185)),
                Triple("C", rawStockC, Color(0xFFFBBF24))
            )
            stockItems.forEach { (label, flowVal, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .cyberglass(borderColor = color.copy(alpha = 0.6f), backgroundColor = Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = label, color = color, fontWeight = FontWeight.Bold, style = Typography.labelSmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%,d", flowVal),
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // 2. THERMODYNAMICS & ENV CONTROLS (Sliders)
        Text(
            text = "BIOPHYSICAL CHAMBER CONTEXT",
            color = CyberGreenDim,
            style = Typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Temperature Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHAMBER TEMPERATURE",
                            color = Color.White,
                            style = Typography.bodySmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f°C", tempState),
                            color = if (tempState > 75f) Color(0xFFEF4444) else if (tempState < 30f) Color(0xFF38BDF8) else CyberGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Slider(
                        value = tempState,
                        onValueChange = { viewModel.setReactorTemperature(it) },
                        valueRange = 15f..95f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberGreen,
                            activeTrackColor = CyberGreen,
                            inactiveTrackColor = CyberBorder.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }

                // Salt Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SALT CONCENTRATION [Na+]",
                            color = Color.White,
                            style = Typography.bodySmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "%.3f M", saltState),
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Slider(
                        value = saltState,
                        onValueChange = { viewModel.setReactorSalt(it) },
                        valueRange = 0.01f..0.50f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberGreen,
                            activeTrackColor = CyberGreen,
                            inactiveTrackColor = CyberBorder.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }

        // 3. POLYMERASE & SOLUTE COFACTOR SELECTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Polymerase
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "POLYMERASE ENZYME",
                    color = CyberGreenDim,
                    style = Typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val polymerases = listOf("Taq", "Pfu", "Tth")
                    polymerases.forEach { poly ->
                        val isSelected = activePolyState.equals(poly, ignoreCase = true)
                        Button(
                            onClick = { viewModel.setActivePolymerase(poly) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CyberGreen.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (isSelected) CyberGreen else Color.Gray
                            ),
                            border = BorderStroke(1.dp, if (isSelected) CyberGreen else CyberBorder.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = poly.uppercase(),
                                style = Typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chemical Solute
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CHEMICAL SOLUTE COFACTOR",
                    color = CyberGreenDim,
                    style = Typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val solutes = listOf("None", "DMSO", "Netropsin")
                    solutes.forEach { solute ->
                        val isSelected = activeSoluteState.equals(solute, ignoreCase = true)
                        Button(
                            onClick = { viewModel.setActiveChemicalSolute(solute) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CyberGreen.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (isSelected) CyberGreen else Color.Gray
                            ),
                            border = BorderStroke(1.dp, if (isSelected) CyberGreen else CyberBorder.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = solute.uppercase(),
                                style = Typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. INLET RATIO WEIGHTS
        Text(
            text = "4-CHANNEL FEEDSTOCK INLET BIAS",
            color = CyberGreenDim,
            style = Typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val channels = listOf(
                    Triple("INLET A", inletRatioAState, { v: Float -> viewModel.setInletRatioA(v) }),
                    Triple("INLET G", inletRatioGState, { v: Float -> viewModel.setInletRatioG(v) }),
                    Triple("INLET T", inletRatioTState, { v: Float -> viewModel.setInletRatioT(v) }),
                    Triple("INLET C", inletRatioCState, { v: Float -> viewModel.setInletRatioC(v) })
                )
                channels.forEach { (label, value, onSet) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 8.sp,
                            modifier = Modifier.width(60.dp)
                        )
                        Slider(
                            value = value,
                            onValueChange = onSet,
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberGreen,
                                activeTrackColor = CyberGreen,
                                inactiveTrackColor = CyberBorder.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", value),
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            modifier = Modifier
                                .width(30.dp)
                                .padding(start = 4.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BioLabTestView(viewModel: MainViewModel) {
    val scrollingGene by viewModel.scrollingGene.collectAsState()
    val poxIdleTime by viewModel.poxIdleTime.collectAsState()
    val discoveredPacketsLog by viewModel.discoveredPacketsLog.collectAsState()
    val poxReactorActive by viewModel.poxReactorActive.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DualPaneConsoleFrame(
            theme = "green",
            flavorTitle = "[ G.E.N. P.O.X. BIO-LAB (T) TEST INTERFACE ]",
            statusText = if (poxReactorActive) "SYSTEMS ON" else "SYSTEMS OFF",
            statusColor = if (poxReactorActive) CyberGreen else Color.Red,
            primaryTitle = "Primary Node Combinator (T)",
            primaryContent = {
                Text(
                    text = "Observe live single-node cybernetic synthesis values and ticks below.",
                    color = CyberGreen.copy(alpha = 0.8f),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scrollingGene,
                        color = CyberGreen,
                        style = Typography.titleLarge,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REACTOR STATE: ${if (poxReactorActive) "ACTIVE" else "IDLE"}",
                        color = CyberGreenDim,
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "NEXT CYCLE IN: ${poxIdleTime}S",
                        color = CyberGreenDim,
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            secondaryTitle = "Discovered Gene Packets Log (T)",
            secondaryContent = {
                Text(
                    text = "Live telemetry transcription feed from the active synthesis processor.",
                    color = CyberGreen.copy(alpha = 0.8f),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (discoveredPacketsLog.isEmpty()) {
                        Text(
                            text = "LOG EMPTY: NO ACTIVE PACKETS CACHED",
                            color = Color.Gray,
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    } else {
                        discoveredPacketsLog.takeLast(3).reversed().forEach { packet ->
                            val typeStr = if (packet.isAnomalous) "ANOMALOUS FUSION" else "GENE SYNTHESIS"
                            Text(
                                text = "• [${packet.genes.size} GENES] $typeStr SUCCESS",
                                color = if (packet.isAnomalous) Color(0xFFA855F7) else CyberGreen,
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        )
    }
}

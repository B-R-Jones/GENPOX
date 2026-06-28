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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import com.example.genpox.data.BiophysicsEngine
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

@Composable
fun BiophysicalResonanceGraph(
    tempDeviance: Float, // Temp - Tm
    transcriptionSpeed: Float, // Frequency modifier
    isAnomaly: Boolean,
    soluteActive: String,
    activeColor: Color,
    isStallThreat: Boolean,
    isDenatureThreat: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "graph_oscillation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulseTransition = rememberInfiniteTransition(label = "boundary_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        
        // Draw background grid lines
        val gridColor = activeColor.copy(alpha = 0.08f)
        val gridStroke = 0.5.dp.toPx()
        
        drawLine(gridColor, Offset(0f, h * 0.2f), Offset(w, h * 0.2f), strokeWidth = gridStroke)
        drawLine(gridColor, Offset(0f, cy), Offset(w, cy), strokeWidth = gridStroke * 1.5f)
        drawLine(gridColor, Offset(0f, h * 0.8f), Offset(w, h * 0.8f), strokeWidth = gridStroke)
        
        val numVerticalLines = 8
        for (v in 1..numVerticalLines) {
            val vx = w * (v.toFloat() / (numVerticalLines + 1))
            drawLine(gridColor, Offset(vx, 0f), Offset(vx, h), strokeWidth = gridStroke)
        }

        // Draw Boundary Danger Zones (Dashed lines / shaded areas)
        val upperBoundaryY = h * 0.2f
        val lowerBoundaryY = h * 0.8f
        
        val upperAlpha = if (isDenatureThreat) pulseAlpha else 0.3f
        val upperStroke = if (isDenatureThreat) 2.5.dp.toPx() else 1.dp.toPx()
        drawLine(
            color = Color(0xFFEF4444).copy(alpha = upperAlpha),
            start = Offset(0f, upperBoundaryY),
            end = Offset(w, upperBoundaryY),
            strokeWidth = upperStroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        val lowerAlpha = if (isStallThreat) pulseAlpha else 0.3f
        val lowerStroke = if (isStallThreat) 2.5.dp.toPx() else 1.dp.toPx()
        drawLine(
            color = Color(0xFF22D3EE).copy(alpha = lowerAlpha),
            start = Offset(0f, lowerBoundaryY),
            end = Offset(w, lowerBoundaryY),
            strokeWidth = lowerStroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        val segments = 100
        val isPhaseLocked = tempDeviance.absoluteValue <= 2.0f && !isStallThreat && !isDenatureThreat

        if (isPhaseLocked) {
            // Merge into a single CyberGreen wave!
            val path = Path()
            val maxAmp = h * 0.25f
            for (i in 0..segments) {
                val fraction = i.toFloat() / segments
                val px = w * fraction
                val envelope = sin(fraction * Math.PI).toFloat()
                
                // Frequency is locked
                val angle = fraction * 4f * Math.PI.toFloat() - phase * transcriptionSpeed
                val waveVal = sin(angle.toDouble()).toFloat() + 0.2f * sin((angle * 2.1f).toDouble()).toFloat()
                
                val py = cy + waveVal * maxAmp * envelope
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(
                path = path,
                color = CyberGreen,
                style = Stroke(width = 2.5.dp.toPx())
            )
        } else {
            // Draw Target Wave (static blue/purple sine wave)
            val targetPath = Path()
            val targetAmp = h * 0.2f
            for (i in 0..segments) {
                val fraction = i.toFloat() / segments
                val px = w * fraction
                val envelope = sin(fraction * Math.PI).toFloat()
                
                val angle = fraction * 4f * Math.PI.toFloat() - phase * 0.5f // constant target speed
                val waveVal = sin(angle.toDouble()).toFloat()
                
                val py = cy + waveVal * targetAmp * envelope
                if (i == 0) targetPath.moveTo(px, py) else targetPath.lineTo(px, py)
            }
            drawPath(
                path = targetPath,
                color = Color(0xFF818CF8), // Indigo / Soft Purple-Blue
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
            )

            // Draw Environmental Wave (dynamic yellow/orange/red/cyan wave shifting phase and amp)
            val envPath = Path()
            val devianceAbs = tempDeviance.absoluteValue
            val maxAmp = (h * 0.3f).coerceAtMost(8.dp.toPx() + devianceAbs * 0.8f)
            val phaseOffset = tempDeviance * 0.1f
            
            val envColor = when {
                soluteActive.uppercase() == "DMSO" || soluteActive.uppercase() == "NETROPSIN" -> Color(0xFFA855F7) // Purple solute
                tempDeviance > 15f -> Color(0xFFEF4444) // Excess Heat
                tempDeviance < -15f -> Color(0xFF22D3EE) // Excess Cold
                else -> Color(0xFFFBBF24) // Yellow (unaligned but warm)
            }

            for (i in 0..segments) {
                val fraction = i.toFloat() / segments
                val px = w * fraction
                val envelope = sin(fraction * Math.PI).toFloat()
                
                val angle = fraction * 4f * Math.PI.toFloat() - phase * transcriptionSpeed + phaseOffset
                var waveVal = sin(angle.toDouble()).toFloat()
                
                // Add jitter noise if temperature is high
                if (devianceAbs > 12f) {
                    val noiseAngle = fraction * 25f * Math.PI.toFloat() + phase * 4f
                    waveVal += 0.1f * sin(noiseAngle.toDouble()).toFloat() * (devianceAbs / 30f).coerceAtMost(1.2f)
                }

                val py = cy + waveVal * maxAmp * envelope
                if (i == 0) envPath.moveTo(px, py) else envPath.lineTo(px, py)
            }
            drawPath(
                path = envPath,
                color = envColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun NucleotideDockingArray(
    activeStrand: String,
    activeStep: Int,
    isCollapsed: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "docking_array_ripple")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val points1 = ArrayList<Offset>()
        val points2 = ArrayList<Offset>()
        
        val numNodes = 8
        for (i in 0 until numNodes) {
            val x = w * 0.08f + (w * 0.84f) * (i.toFloat() / (numNodes - 1))
            val angle = i * 1.0f + phase
            val y1 = h / 2f + sin(angle) * (h * 0.22f)
            val y2 = h / 2f - sin(angle) * (h * 0.22f)
            points1.add(Offset(x, y1))
            points2.add(Offset(x, y2))
        }
        
        // Draw connecting rungs (base-pair bonds)
        for (i in 0 until numNodes) {
            val alpha = if (isCollapsed) 0.15f else 0.25f
            val rungColor = if (isCollapsed) Color(0xFFEF4444).copy(alpha = alpha) else CyberGreenDim.copy(alpha = alpha)
            drawLine(
                color = rungColor,
                start = points1[i],
                end = points2[i],
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
        }
        
        // Draw the backbones
        val path1 = Path()
        val path2 = Path()
        path1.moveTo(points1[0].x, points1[0].y)
        path2.moveTo(points2[0].x, points2[0].y)
        for (i in 1 until numNodes) {
            path1.lineTo(points1[i].x, points1[i].y)
            path2.lineTo(points2[i].x, points2[i].y)
        }
        
        val backboneColor = if (isCollapsed) Color(0xFFEF4444) else CyberGreen.copy(alpha = 0.5f)
        val backboneStroke = if (isCollapsed) 2.5.dp.toPx() else 1.5.dp.toPx()
        val backboneEffect = if (isCollapsed) {
            PathEffect.dashPathEffect(floatArrayOf(15f, 10f, 5f, 10f), phase * 10f)
        } else {
            null
        }
        
        drawPath(
            path = path1,
            color = backboneColor,
            style = Stroke(width = backboneStroke, pathEffect = backboneEffect)
        )
        drawPath(
            path = path2,
            color = backboneColor.copy(alpha = if (isCollapsed) 1.0f else 0.3f),
            style = Stroke(width = backboneStroke * 0.7f, pathEffect = backboneEffect)
        )
        
        // Draw hexagons along points1 (primary strand)
        val hexRadius = (w * 0.045f).coerceAtMost(22.dp.toPx()).coerceAtLeast(12.dp.toPx())
        for (i in 0 until numNodes) {
            val center = points1[i]
            val isFilled = i < activeStrand.length
            val char = if (isFilled) activeStrand[i] else null
            
            val hexColor = when (char) {
                'A' -> Color(0xFF38BDF8) // Cyan
                'G' -> Color(0xFF34D399) // Green
                'T' -> Color(0xFFFB7185) // Rose/Red
                'C' -> Color(0xFFFBBF24) // Yellow
                else -> if (isCollapsed) Color(0xFFEF4444).copy(alpha = 0.4f) else CyberGreen.copy(alpha = 0.3f)
            }
            
            val hexPath = Path()
            for (j in 0..5) {
                val angleRad = j * Math.PI / 3f
                val hx = center.x + hexRadius * cos(angleRad).toFloat()
                val hy = center.y + hexRadius * sin(angleRad).toFloat()
                if (j == 0) hexPath.moveTo(hx, hy) else hexPath.lineTo(hx, hy)
            }
            hexPath.close()
            
            if (isFilled && char != null) {
                drawPath(
                    path = hexPath,
                    color = hexColor.copy(alpha = 0.15f),
                    style = Fill
                )
                drawPath(
                    path = hexPath,
                    color = hexColor,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                val textLayoutResult = textMeasurer.measure(
                    text = char.toString(),
                    style = TextStyle(
                        color = hexColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        center.x - textLayoutResult.size.width / 2f,
                        center.y - textLayoutResult.size.height / 2f
                    )
                )
                
                val barWidth = hexRadius * 1.2f
                val barHeight = 2.5.dp.toPx()
                val barTop = center.y + hexRadius + 3.dp.toPx()
                val barColor = if (isCollapsed && i == activeStrand.length - 1) Color.Red else CyberGreen
                
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(center.x - barWidth / 2f, barTop),
                    size = Size(barWidth, barHeight)
                )
                drawRect(
                    color = barColor,
                    topLeft = Offset(center.x - barWidth / 2f, barTop),
                    size = Size(barWidth * 0.8f, barHeight)
                )
            } else {
                drawPath(
                    path = hexPath,
                    color = hexColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = if (isCollapsed) PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f) else null
                    )
                )
                
                val textLayoutResult = textMeasurer.measure(
                    text = (i + 1).toString(),
                    style = TextStyle(
                        color = hexColor.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        center.x - textLayoutResult.size.width / 2f,
                        center.y - textLayoutResult.size.height / 2f
                    )
                )
            }
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

    val activeStrand by viewModel.activeSynthesisStrand.collectAsState()
    val activeStep by viewModel.activeSynthesisStep.collectAsState()
    val activeQ by viewModel.activeSynthesisQScore.collectAsState()
    val activeTm by viewModel.activeSynthesisTm.collectAsState()
    val activeMfe by viewModel.activeSynthesisMfe.collectAsState()
    val currentTemp by viewModel.reactorTemperature.collectAsState()
    val currentSalt by viewModel.reactorSalt.collectAsState()
    val activePoly by viewModel.activePolymerase.collectAsState()
    val activeSolute by viewModel.activeChemicalSolute.collectAsState()
    val poxReactorActive by viewModel.poxReactorActive.collectAsState()

    val targetSeq by viewModel.targetSynthesisSequence.collectAsState()
    val isSynthesisActive by viewModel.isSynthesisActive.collectAsState()
    val isReactionCollapsed by viewModel.isReactionCollapsed.collectAsState()
    val bioWaste by viewModel.bioWaste.collectAsState()
    val isLoopActive by viewModel.isLoopActive.collectAsState()

    val targetTm = remember(targetSeq, currentSalt) {
        BiophysicsEngine.calculateMeltingTemperature(targetSeq, currentSalt.toDouble())
    }

    val tempDeviance = currentTemp - targetTm.toFloat()
    val safeMinTemp = remember(currentSalt) { 28.0f + (currentSalt * 30.0f) }
    val safeMaxTemp = remember(currentSalt) { 22.0f + (currentSalt * 30.0f) }

    val speedFactor = remember(activePoly) {
        when (activePoly.uppercase()) {
            "TAQ" -> 2.0f
            "TTH" -> 1.0f
            "PFU" -> 0.5f
            else -> 1.0f
        }
    }
    val targetMfe = remember(targetSeq) {
        BiophysicsEngine.calculateMinimumFreeEnergy(targetSeq)
    }

    val isStallThreat = remember(targetSeq, currentTemp, safeMinTemp, targetMfe, activeSolute) {
        targetSeq.length >= 4 && currentTemp < safeMinTemp && targetMfe <= -5.0 && activeSolute != "DMSO"
    }
    val gcPercent = remember(targetSeq) {
        if (targetSeq.isEmpty()) 0.0 else targetSeq.count { it == 'G' || it == 'C' }.toDouble() / targetSeq.length.toDouble()
    }
    val isDenatureThreat = remember(targetSeq, currentTemp, safeMaxTemp, gcPercent, activeSolute) {
        targetSeq.length >= 4 && currentTemp > safeMaxTemp && gcPercent < 0.40 && activeSolute != "Netropsin"
    }

    val safeRangeText = remember(targetSeq, targetMfe, gcPercent, activeSolute, safeMinTemp, safeMaxTemp) {
        val hasStall = targetSeq.length >= 4 && targetMfe <= -5.0 && activeSolute != "DMSO"
        val hasDenature = targetSeq.length >= 4 && gcPercent < 0.40 && activeSolute != "Netropsin"
        when {
            hasStall && hasDenature -> "NONE"
            hasStall -> "${safeMinTemp.toInt()}°C - 50°C"
            hasDenature -> "15°C - ${safeMaxTemp.toInt()}°C"
            else -> "15°C - 50°C"
        }
    }

    val waveColor = remember(tempDeviance, activeSolute) {
        when {
            activeSolute.uppercase() == "DMSO" || activeSolute.uppercase() == "NETROPSIN" -> Color(0xFFA855F7)
            tempDeviance > 15f -> Color(0xFFEF4444)
            tempDeviance < -15f -> Color(0xFF22D3EE)
            else -> CyberGreen
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // LIVE STEPPED SYNTHESIS TELEMETRY PANEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE TRANSCRIPTION TELEMETRY",
                        color = CyberGreenDim,
                        style = Typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = when {
                            isReactionCollapsed -> "STATUS: COLLAPSED"
                            isSynthesisActive -> "STATUS: TRANSCRIBING"
                            else -> "STATUS: STANDBY"
                        },
                        color = when {
                            isReactionCollapsed -> Color.Red
                            isSynthesisActive -> CyberGreen
                            else -> Color.Gray
                        },
                        style = Typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Transcription monitor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Growing sequence readout
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.dp, activeBorder.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeStrand.padEnd(8, '-'),
                            color = if (isReactionCollapsed) Color.Red else if (activeQ <= 5.0 && activeStrand.isNotEmpty()) Color.Red else CyberGreen,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    // Pre-flight bio-analysis details readout: One row, three columns
                    Column(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.dp, activeBorder.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PRE-FLIGHT BIO-ANALYSIS",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Current Temp
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f°C", currentTemp),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "CURR TEMP",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Default
                                )
                            }
                            
                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(activeBorder.copy(alpha = 0.3f))
                            )

                            // Column 2: Ideal Temp
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f°C", targetTm),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "IDEAL TEMP",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Default
                                )
                            }
                            
                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(activeBorder.copy(alpha = 0.3f))
                            )

                            // Column 3: Safe Temp Range
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text(
                                    text = safeRangeText,
                                    color = if (isStallThreat || isDenatureThreat) Color(0xFFEF4444) else CyberGreen,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "SAFE RANGE",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Default
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(activeBorder.copy(alpha = 0.3f))
                            )

                            // Column 4: Temp Deviance
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%+.1f°C", tempDeviance),
                                    color = if (kotlin.math.abs(tempDeviance) > 15f) Color(0xFFEF4444) else CyberGreen,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "DEVIANCE",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }
                }

                // Waveguide visualizer + Docking Array combined in a row!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Waveguide (normalized graph)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .cyberglass(borderColor = activeBorder.copy(alpha = 0.5f), backgroundColor = Color.Black.copy(alpha = 0.3f))
                            .padding(2.dp)
                    ) {
                        BiophysicalResonanceGraph(
                            tempDeviance = tempDeviance,
                            transcriptionSpeed = speedFactor,
                            isAnomaly = false,
                            soluteActive = activeSolute,
                            activeColor = CyberGreen,
                            isStallThreat = isStallThreat,
                            isDenatureThreat = isDenatureThreat,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Docking Array (hexagons)
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .cyberglass(borderColor = activeBorder.copy(alpha = 0.5f), backgroundColor = Color.Black.copy(alpha = 0.3f))
                    ) {
                        NucleotideDockingArray(
                            activeStrand = activeStrand,
                            activeStep = activeStep,
                            isCollapsed = isReactionCollapsed,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Compact Feedstock & Bio-Waste Stockpile Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val rawStockA by viewModel.rawStockA.collectAsState()
            val rawStockG by viewModel.rawStockG.collectAsState()
            val rawStockT by viewModel.rawStockT.collectAsState()
            val rawStockC by viewModel.rawStockC.collectAsState()

            val stocks = listOf(
                Triple("A", rawStockA, Color(0xFF38BDF8)),
                Triple("G", rawStockG, Color(0xFF34D399)),
                Triple("T", rawStockT, Color(0xFFFB7185)),
                Triple("C", rawStockC, Color(0xFFFBBF24)),
                Triple("WASTE", bioWaste, Color(0xFFA855F7))
            )
            stocks.forEach { (label, amt, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .cyberglass(borderColor = color.copy(alpha = 0.4f), backgroundColor = Color.Black.copy(alpha = 0.3f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = label, color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(text = String.format(Locale.US, "%d", amt), color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Target Dial Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "PROGRAM TARGET SEQUENCE (TAP DIALS TO CYCLE)",
                color = CyberGreenDim,
                style = Typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..7) {
                    val char = targetSeq[i]
                    val baseColor = when (char) {
                        'A' -> Color(0xFF38BDF8) // Cyan
                        'G' -> Color(0xFF34D399) // Green
                        'T' -> Color(0xFFFB7185) // Rose
                        'C' -> Color(0xFFFBBF24) // Yellow
                        else -> Color.Gray
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSynthesisActive) Color.DarkGray.copy(alpha = 0.2f) else baseColor.copy(alpha = 0.1f))
                            .border(
                                width = 1.dp,
                                color = if (isSynthesisActive) Color.Gray.copy(alpha = 0.3f) else baseColor.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable(enabled = !isSynthesisActive) {
                                viewModel.cycleTargetBase(i)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            color = if (isSynthesisActive) Color.Gray else baseColor,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Pre-flight checks and Initiate button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            val rawStockA by viewModel.rawStockA.collectAsState()
            val rawStockG by viewModel.rawStockG.collectAsState()
            val rawStockT by viewModel.rawStockT.collectAsState()
            val rawStockC by viewModel.rawStockC.collectAsState()

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

            val buttonEnabled = poxReactorActive && !isSynthesisActive && hasEnoughStock

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PoxButton(
                    modifier = Modifier.weight(1f),
                    text = if (isSynthesisActive) "SYNTHESIZING..." else "INITIATE SYNTHESIS",
                    onClick = {
                        viewModel.initiateStandardSynthesis()
                    },
                    enabled = buttonEnabled,
                    buttonType = PoxButtonType.GREEN_PHOSPHOR,
                    buttonSize = PoxButtonSize.STANDARD,
                    sound = PoxButtonSound.SUCCESS_CHIME,
                    viewModel = viewModel
                )

                PoxButton(
                    modifier = Modifier.width(38.dp),
                    text = "⟲",
                    onClick = {
                        viewModel.toggleLoopActive()
                    },
                    buttonType = if (isLoopActive) PoxButtonType.GREEN_PHOSPHOR else PoxButtonType.GREEN_MUTED,
                    buttonSize = PoxButtonSize.STANDARD,
                    sound = PoxButtonSound.COMBINATOR_TICK,
                    viewModel = viewModel
                )
            }
        }



        if (devForceAnomaly) {
            Spacer(modifier = Modifier.height(10.dp))
            PoxButton(
                modifier = Modifier.fillMaxWidth(),
                text = "DEV: INJECT 10K BASES & WASTE",
                onClick = { viewModel.addDevBases() },
                buttonType = PoxButtonType.YELLOW_WARNING,
                buttonSize = PoxButtonSize.STANDARD,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun VerticalSpinnableDial(
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val channels = listOf("A", "G", "T", "C")
    var dragAccumulator by remember { mutableStateOf(0f) }
    var hasCycledInThisGesture by remember { mutableStateOf(false) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnIndexChange by rememberUpdatedState(onIndexChange)

    Box(
        modifier = modifier
            .width(50.dp)
            .height(80.dp)
            .cyberglass(borderColor = activeColor, backgroundColor = Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulator = 0f
                        hasCycledInThisGesture = false
                    },
                    onDragEnd = {
                        dragAccumulator = 0f
                        hasCycledInThisGesture = false
                    },
                    onDragCancel = {
                        dragAccumulator = 0f
                        hasCycledInThisGesture = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!hasCycledInThisGesture) {
                            dragAccumulator += dragAmount.y
                            if (dragAccumulator > 40f) {
                                val newIndex = (currentSelectedIndex - 1 + 4) % 4
                                currentOnIndexChange(newIndex)
                                hasCycledInThisGesture = true
                                dragAccumulator = 0f
                            } else if (dragAccumulator < -40f) {
                                val newIndex = (currentSelectedIndex + 1) % 4
                                currentOnIndexChange(newIndex)
                                hasCycledInThisGesture = true
                                dragAccumulator = 0f
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val prevIndex = (selectedIndex - 1 + 4) % 4
            Text(
                text = channels[prevIndex],
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = channels[selectedIndex],
                color = activeColor,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val nextIndex = (selectedIndex + 1) % 4
            Text(
                text = channels[nextIndex],
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
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
    val bioWaste by viewModel.bioWaste.collectAsState()
    val tempState by viewModel.reactorTemperature.collectAsState()
    val saltState by viewModel.reactorSalt.collectAsState()
    val activePolyState by viewModel.activePolymerase.collectAsState()
    val activeSoluteState by viewModel.activeChemicalSolute.collectAsState()
    val inletRatioAState by viewModel.inletRatioA.collectAsState()
    val inletRatioGState by viewModel.inletRatioG.collectAsState()
    val inletRatioTState by viewModel.inletRatioT.collectAsState()
    val inletRatioCState by viewModel.inletRatioC.collectAsState()

    var activeDialIndex by remember { mutableStateOf(0) } // 0=A, 1=G, 2=T, 3=C

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
                Triple("C", rawStockC, Color(0xFFFBBF24)),
                Triple("WASTE", bioWaste, Color(0xFFA855F7))
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
                    PoxSlider(
                        value = tempState,
                        onValueChange = { viewModel.setReactorTemperature(it) },
                        valueRange = 15f..50f,
                        activeColor = CyberGreen,
                        inactiveColor = CyberBorder
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
                    PoxSlider(
                        value = saltState,
                        onValueChange = { viewModel.setReactorSalt(it) },
                        valueRange = 0.01f..0.15f,
                        activeColor = CyberGreen,
                        inactiveColor = CyberBorder
                    )
                }
            }
        }

        // 3. POLYMERASE ENZYME SELECTOR
        Column {
            Text(
                text = "POLYMERASE ENZYME",
                color = CyberGreenDim,
                style = Typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val polymerases = listOf("Taq", "Tth", "Pfu")
                polymerases.forEach { poly ->
                    val isSelected = activePolyState.equals(poly, ignoreCase = true)
                    val btnText = when (poly.uppercase()) {
                        "TAQ" -> "TAQ (FREE)"
                        "TTH" -> "TTH (25/ALL + 50w)"
                        "PFU" -> "PFU (50/ALL + 150w)"
                        else -> poly.uppercase()
                    }
                    PoxButton(
                        modifier = Modifier.weight(1f),
                        text = btnText,
                        onClick = { viewModel.setActivePolymerase(poly) },
                        buttonType = if (isSelected) PoxButtonType.GREEN_PHOSPHOR else PoxButtonType.GREEN_MUTED,
                        buttonSize = PoxButtonSize.COMPACT,
                        sound = PoxButtonSound.COMBINATOR_TICK,
                        viewModel = viewModel
                    )
                }
            }
        }

        // 4. CHEMICAL SOLUTE COFACTOR SELECTOR
        Column {
            Text(
                text = "CHEMICAL SOLUTE COFACTOR",
                color = CyberGreenDim,
                style = Typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberglass(borderColor = activeBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val solutes = listOf("None", "DMSO", "Netropsin")
                solutes.forEach { solute ->
                    val isSelected = activeSoluteState.equals(solute, ignoreCase = true)
                    PoxButton(
                        modifier = Modifier.weight(1f),
                        text = solute.uppercase(),
                        onClick = { viewModel.setActiveChemicalSolute(solute) },
                        buttonType = if (isSelected) PoxButtonType.GREEN_PHOSPHOR else PoxButtonType.GREEN_MUTED,
                        buttonSize = PoxButtonSize.COMPACT,
                        sound = PoxButtonSound.COMBINATOR_TICK,
                        viewModel = viewModel
                    )
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
            val activeBaseColor = remember(activeDialIndex) {
                when (activeDialIndex) {
                    0 -> Color(0xFF38BDF8) // A: Cyan
                    1 -> Color(0xFF34D399) // G: Green
                    2 -> Color(0xFFFB7185) // T: Rose
                    else -> Color(0xFFFBBF24) // C: Yellow
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dial selector on the left
                VerticalSpinnableDial(
                    selectedIndex = activeDialIndex,
                    onIndexChange = {
                        activeDialIndex = it
                    },
                    activeColor = activeBaseColor,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                // Dynamic slider controls on the right
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    val label = when (activeDialIndex) {
                        0 -> "INLET A"
                        1 -> "INLET G"
                        2 -> "INLET T"
                        else -> "INLET C"
                    }
                    val value = when (activeDialIndex) {
                        0 -> inletRatioAState
                        1 -> inletRatioGState
                        2 -> inletRatioTState
                        else -> inletRatioCState
                    }
                    val onValueChange: (Float) -> Unit = { v ->
                        when (activeDialIndex) {
                            0 -> viewModel.setInletRatioA(v)
                            1 -> viewModel.setInletRatioG(v)
                            2 -> viewModel.setInletRatioT(v)
                            else -> viewModel.setInletRatioC(v)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$label RATIO BIAS",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 8.5.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", value),
                            color = activeBaseColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    PoxSlider(
                        value = value,
                        onValueChange = onValueChange,
                        valueRange = 0.05f..0.90f,
                        activeColor = activeBaseColor,
                        inactiveColor = activeBaseColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SWIPE DIAL VERTICALLY TO CYCLE CHANNELS",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal
                    )
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

package com.example.genpox.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.genpox.data.GeneticsEngine
import com.example.genpox.data.WaveMath
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TranscriptionDecoderTicker(
    scrollingGeneFlow: StateFlow<String>,
    poxReactorActiveProvider: () -> Boolean,
    anomalyEngineActiveProvider: () -> Boolean,
    isAnomaly: Boolean,
    activeColor: Color,
    activeColorDim: Color,
    modifier: Modifier = Modifier,
) {
    val scrollingGene by scrollingGeneFlow.collectAsStateWithLifecycle()
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
            .border(1.dp, activeColor, RoundedCornerShape(4.dp)).background(Color.Transparent, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "TRANSCRIPTION DECODER:",
            color = activeColorDim,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
        )
        Text(
            text = displayText,
            color = activeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
fun ResonanceScopeChamber(
    progressProvider: () -> Float,
    isActive: Boolean,
    isAnomaly: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(1.dp, activeColor, RoundedCornerShape(4.dp)).background(Color.Transparent, RoundedCornerShape(4.dp))
            .padding(2.dp),
    ) {
        val progress = progressProvider()
        GeneticResonanceScope(
            progress = progress,
            isActive = isActive,
            isAnomaly = isAnomaly,
            activeColor = activeColor,
            modifier = Modifier.fillMaxSize(),
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
                .padding(4.dp),
        )
    }
}

@Composable
fun ReactorTimerStatus(
    statusProvider: () -> String,
    colorProvider: () -> Color,
    boostSecondsLeftProvider: () -> Int,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = statusProvider(),
            color = colorProvider(),
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            style = Typography.bodySmall,
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
                    .border(1.dp, activeColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
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
    activeStrand: String,
    isCollapsed: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "graph_oscillation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val pulseTransition = rememberInfiniteTransition(label = "boundary_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    val textMeasurer = rememberTextMeasurer()

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
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
        )

        val lowerAlpha = if (isStallThreat) pulseAlpha else 0.3f
        val lowerStroke = if (isStallThreat) 2.5.dp.toPx() else 1.dp.toPx()
        drawLine(
            color = Color(0xFF22D3EE).copy(alpha = lowerAlpha),
            start = Offset(0f, lowerBoundaryY),
            end = Offset(w, lowerBoundaryY),
            strokeWidth = lowerStroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
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
                style = Stroke(width = 2.5.dp.toPx()),
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
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)),
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
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        // Overlay the 8 gene sequence hexagons following the active wave y-value
        val numNodes = 8
        val hexRadius = (w * 0.03f).coerceAtMost(18.dp.toPx()).coerceAtLeast(11.dp.toPx())
        val getWaveY = { frac: Float ->
            val envelope = sin(frac * Math.PI).toFloat()
            if (isPhaseLocked) {
                val maxAmp = h * 0.25f
                val angle = frac * 4f * Math.PI.toFloat() - phase * transcriptionSpeed
                val waveVal = sin(angle.toDouble()).toFloat() + 0.2f * sin((angle * 2.1f).toDouble()).toFloat()
                cy + waveVal * maxAmp * envelope
            } else {
                val devianceAbs = tempDeviance.absoluteValue
                val maxAmp = (h * 0.3f).coerceAtMost(8.dp.toPx() + devianceAbs * 0.8f)
                val phaseOffset = tempDeviance * 0.1f

                val angle = frac * 4f * Math.PI.toFloat() - phase * transcriptionSpeed + phaseOffset
                var waveVal = sin(angle.toDouble()).toFloat()

                if (devianceAbs > 12f) {
                    val noiseAngle = frac * 25f * Math.PI.toFloat() + phase * 4f
                    waveVal += 0.1f * sin(noiseAngle.toDouble()).toFloat() * (devianceAbs / 30f).coerceAtMost(1.2f)
                }
                cy + waveVal * maxAmp * envelope
            }
        }
        val points = ArrayList<Offset>()
        for (i in 0 until numNodes) {
            val fraction = 0.08f + 0.84f * (i.toFloat() / (numNodes - 1))
            val x = w * fraction
            val y = getWaveY(fraction)
            points.add(Offset(x, y))
        }

        for (i in 0 until numNodes) {
            val center = points[i]
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

            // Draw dark background inside hexagon to stand out from the grid/waves
            drawPath(
                path = hexPath,
                color = Color.Black.copy(alpha = 0.75f),
                style = Fill,
            )

            if (isFilled && char != null) {
                drawPath(
                    path = hexPath,
                    color = hexColor.copy(alpha = 0.15f),
                    style = Fill,
                )
                drawPath(
                    path = hexPath,
                    color = hexColor,
                    style = Stroke(width = 2.dp.toPx()),
                )

                val textLayoutResult = textMeasurer.measure(
                    text = char.toString(),
                    style = TextStyle(
                        color = hexColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        center.x - textLayoutResult.size.width / 2f,
                        center.y - textLayoutResult.size.height / 2f,
                    ),
                )

                val barWidth = hexRadius * 1.2f
                val barHeight = 2.5.dp.toPx()
                val barTop = center.y + hexRadius + 3.dp.toPx()
                val barColor = if (isCollapsed && i == activeStrand.length - 1) Color.Red else CyberGreen

                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(center.x - barWidth / 2f, barTop),
                    size = Size(barWidth, barHeight),
                )
                drawRect(
                    color = barColor,
                    topLeft = Offset(center.x - barWidth / 2f, barTop),
                    size = Size(barWidth * 0.8f, barHeight),
                )
            } else {
                drawPath(
                    path = hexPath,
                    color = hexColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = if (isCollapsed) PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f) else null,
                    ),
                )

                val textLayoutResult = textMeasurer.measure(
                    text = (i + 1).toString(),
                    style = TextStyle(
                        color = hexColor.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        center.x - textLayoutResult.size.width / 2f,
                        center.y - textLayoutResult.size.height / 2f,
                    ),
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
    activePanel: Color,
) {
    val geneSequences by viewModel.geneSequences.collectAsStateWithLifecycle()
    val devForceAnomaly by viewModel.devForceAnomaly.collectAsStateWithLifecycle()

    val uniqueGenesSize = remember(geneSequences) { geneSequences.size }
    val multiCountGenesSize = remember(geneSequences) { geneSequences.count { it.count > 1 } }

    val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
    val todayWave = wave
    val tomorrowWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 86400000L)
    val dayAfterWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 172800000L)

    val activeStrand by viewModel.activeSynthesisStrand.collectAsStateWithLifecycle()
    val activeStep by viewModel.activeSynthesisStep.collectAsStateWithLifecycle()
    val activeQ by viewModel.activeSynthesisQScore.collectAsStateWithLifecycle()
    val activeTm by viewModel.activeSynthesisTm.collectAsStateWithLifecycle()
    val activeMfe by viewModel.activeSynthesisMfe.collectAsStateWithLifecycle()
    val currentTemp by viewModel.reactorTemperature.collectAsStateWithLifecycle()
    val currentSalt by viewModel.reactorSalt.collectAsStateWithLifecycle()
    val activePoly by viewModel.activePolymerase.collectAsStateWithLifecycle()
    val activeSolute by viewModel.activeChemicalSolute.collectAsStateWithLifecycle()
    val poxReactorActive by viewModel.poxReactorActive.collectAsStateWithLifecycle()

    val targetSeq by viewModel.targetSynthesisSequence.collectAsStateWithLifecycle()
    val isSynthesisActive by viewModel.isSynthesisActive.collectAsStateWithLifecycle()
    val isReactionCollapsed by viewModel.isReactionCollapsed.collectAsStateWithLifecycle()
    val bioWaste by viewModel.bioWaste.collectAsStateWithLifecycle()
    val isLoopActive by viewModel.isLoopActive.collectAsStateWithLifecycle()

    val targetTm = remember(targetSeq, currentSalt) {
        GeneticsEngine.calculateMeltingTemperature(targetSeq, currentSalt.toDouble())
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
        GeneticsEngine.calculateMinimumFreeEnergy(targetSeq)
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
        // PRE-FLIGHT BIO-ANALYSIS PANEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "PRE-FLIGHT BIO-ANALYSIS",
                    color = CyberGreenDim,
                    style = Typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Column 1: Current Temp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f°C", currentTemp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "CURR TEMP",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Default,
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(activeBorder.copy(alpha = 0.3f)),
                    )

                    // Column 2: Ideal Temp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f°C", targetTm),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "IDEAL TEMP",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Default,
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(activeBorder.copy(alpha = 0.3f)),
                    )

                    // Column 3: Safe Temp Range
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.3f),
                    ) {
                        Text(
                            text = safeRangeText,
                            color = if (isStallThreat || isDenatureThreat) Color(0xFFEF4444) else CyberGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "SAFE RANGE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Default,
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(activeBorder.copy(alpha = 0.3f)),
                    )

                    // Column 4: Temp Deviance
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = String.format(Locale.US, "%+.1f°C", tempDeviance),
                            color = if (kotlin.math.abs(tempDeviance) > 15f) Color(0xFFEF4444) else CyberGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "DEVIANCE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }
            }
        }

        // BIOPHYSICAL RESONANCE GRAPH PANEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(4.dp),
        ) {
            BiophysicalResonanceGraph(
                tempDeviance = tempDeviance,
                transcriptionSpeed = speedFactor,
                isAnomaly = false,
                soluteActive = activeSolute,
                activeColor = CyberGreen,
                isStallThreat = isStallThreat,
                isDenatureThreat = isDenatureThreat,
                activeStrand = activeStrand,
                isCollapsed = isReactionCollapsed,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Compact Feedstock & Bio-Waste Stockpile Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val rawStockA by viewModel.rawStockA.collectAsStateWithLifecycle()
            val rawStockG by viewModel.rawStockG.collectAsStateWithLifecycle()
            val rawStockT by viewModel.rawStockT.collectAsStateWithLifecycle()
            val rawStockC by viewModel.rawStockC.collectAsStateWithLifecycle()

            val stocks = listOf(
                Triple("A", rawStockA, Color(0xFF38BDF8)),
                Triple("G", rawStockG, Color(0xFF34D399)),
                Triple("T", rawStockT, Color(0xFFFB7185)),
                Triple("C", rawStockC, Color(0xFFFBBF24)),
                Triple("WASTE", bioWaste, Color(0xFFA855F7)),
            )
            stocks.forEach { (label, amt, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, color, RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
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
                .border(1.dp, activeBorder, RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "PROGRAM TARGET SEQUENCE (TAP DIALS TO CYCLE)",
                color = CyberGreenDim,
                style = Typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
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
                                color = if (isSynthesisActive) Color.Gray else baseColor,
                                shape = RoundedCornerShape(4.dp),
                            )
                            .clickable(enabled = !isSynthesisActive) {
                                viewModel.cycleTargetBase(i)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = char.toString(),
                            color = if (isSynthesisActive) Color.Gray else baseColor,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Pre-flight checks and Initiate button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeBorder, RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(8.dp),
        ) {
            val rawStockA by viewModel.rawStockA.collectAsStateWithLifecycle()
            val rawStockG by viewModel.rawStockG.collectAsStateWithLifecycle()
            val rawStockT by viewModel.rawStockT.collectAsStateWithLifecycle()
            val rawStockC by viewModel.rawStockC.collectAsStateWithLifecycle()

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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        viewModel.synthManager.playBeep(880f, 0.1f, "sine")
                        viewModel.initiateStandardSynthesis()
                    },
                    enabled = buttonEnabled,
                    modifier = Modifier.weight(1f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (buttonEnabled) CyberGreen else Color.Transparent,
                        contentColor = if (buttonEnabled) Color.Black else Color.Gray,
                    ),
                    border = if (buttonEnabled) null else BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = if (isSynthesisActive) "SYNTHESIZING..." else "INITIATE SYNTHESIS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                    )
                }

                Button(
                    onClick = {
                        viewModel.synthManager.playCombinatorTick()
                        viewModel.toggleLoopActive()
                    },
                    modifier = Modifier.width(38.dp).height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoopActive) CyberGreen else Color.Transparent,
                        contentColor = if (isLoopActive) Color.Black else CyberGreen,
                    ),
                    border = BorderStroke(1.dp, CyberGreen),
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
        }

        if (devForceAnomaly) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                    viewModel.addDevBases()
                },
                modifier = Modifier.fillMaxWidth().height(34.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFBBF24),
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = "DEV: INJECT 10K BASES & WASTE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                )
            }
        }
    }
}

fun adjustInletRatios(changedIndex: Int, newValue: Float, viewModel: MainViewModel) {
    val minVal = 0.05f
    val maxVal = 0.85f

    val currentValues = floatArrayOf(
        viewModel.inletRatioA.value,
        viewModel.inletRatioG.value,
        viewModel.inletRatioT.value,
        viewModel.inletRatioC.value,
    )
    val oldValue = currentValues[changedIndex]

    if (newValue == oldValue) return

    if (newValue < oldValue) {
        val clampedVal = newValue.coerceIn(minVal, maxVal)
        when (changedIndex) {
            0 -> viewModel.setInletRatioA(clampedVal)
            1 -> viewModel.setInletRatioG(clampedVal)
            2 -> viewModel.setInletRatioT(clampedVal)
            3 -> viewModel.setInletRatioC(clampedVal)
        }
    } else {
        val sum = currentValues.sum()
        val bank = (1.0f - sum).coerceAtLeast(0f)
        val maxAllowed = oldValue + bank
        val clampedVal = newValue.coerceIn(oldValue, maxAllowed.coerceAtMost(maxVal))
        when (changedIndex) {
            0 -> viewModel.setInletRatioA(clampedVal)
            1 -> viewModel.setInletRatioG(clampedVal)
            2 -> viewModel.setInletRatioT(clampedVal)
            3 -> viewModel.setInletRatioC(clampedVal)
        }
    }
}

@Composable
fun BioSilo(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rangeMin = range.start
    val rangeMax = range.endInclusive
    val fraction = ((value - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)

    var componentHeight by remember { mutableStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .width(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.5.dp, color.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .onGloballyPositioned { coordinates ->
                    componentHeight = coordinates.size.height
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val initialY = down.position.y
                        if (componentHeight > 0) {
                            val newFraction = 1f - (initialY / componentHeight.toFloat())
                            val newValue = rangeMin + newFraction.coerceIn(0f, 1f) * (rangeMax - rangeMin)
                            onValueChange(newValue)
                        }

                        val dragPointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == dragPointerId }
                            if (change == null || !change.pressed) break

                            val currentY = change.position.y
                            if (componentHeight > 0) {
                                val newFraction = 1f - (currentY / componentHeight.toFloat())
                                val newValue = rangeMin + newFraction.coerceIn(0f, 1f) * (rangeMax - rangeMin)
                                onValueChange(newValue)
                                change.consume()
                            }
                        }
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.85f),
                                color.copy(alpha = 0.35f),
                            ),
                        ),
                    )
                    .align(Alignment.BottomCenter),
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val numTicks = 6
                val tickColor = color.copy(alpha = 0.25f)
                val tickWidth = size.width * 0.25f
                for (t in 1 until numTicks) {
                    val ty = size.height * (t.toFloat() / numTicks)
                    drawLine(tickColor, Offset(2.dp.toPx(), ty), Offset(2.dp.toPx() + tickWidth, ty), strokeWidth = 1.dp.toPx())
                    drawLine(tickColor, Offset(size.width - 2.dp.toPx() - tickWidth, ty), Offset(size.width - 2.dp.toPx(), ty), strokeWidth = 1.dp.toPx())
                }
            }
        }
    }
}

@Composable
fun BiophysicalTempSlider(
    value: Float,
    targetTm: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rangeMin = 15f
    val rangeMax = 50f
    val currentFraction = ((value - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)

    val fTm = ((targetTm - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
    val fIdealStart = ((targetTm - 2f - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
    val fIdealEnd = ((targetTm + 2f - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
    val fCold = ((targetTm - 15f - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
    val fHot = ((targetTm + 15f - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)

    var componentWidth by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
            .onGloballyPositioned { coordinates ->
                componentWidth = coordinates.size.width
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (componentWidth > 0) {
                        val newFraction = (down.position.x / componentWidth.toFloat()).coerceIn(0f, 1f)
                        val newValue = rangeMin + newFraction * (rangeMax - rangeMin)
                        onValueChange(newValue)
                    }

                    val dragPointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == dragPointerId }
                        if (change == null || !change.pressed) break

                        if (componentWidth > 0) {
                            val newFraction = (change.position.x / componentWidth.toFloat()).coerceIn(0f, 1f)
                            val newValue = rangeMin + newFraction * (rangeMax - rangeMin)
                            onValueChange(newValue)
                            change.consume()
                        }
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val gradientBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF22D3EE),
                    fCold to Color(0xFF22D3EE),
                    fIdealStart to Color(0xFF34D399),
                    fIdealEnd to Color(0xFF34D399),
                    fHot to Color(0xFFEF4444),
                    1.0f to Color(0xFFEF4444),
                ),
                start = Offset(0f, 0f),
                end = Offset(w, 0f),
            )

            drawRect(
                brush = gradientBrush,
                topLeft = Offset(0f, 0f),
                size = size,
            )

            val numTicks = 20
            val tickColor = Color.Black.copy(alpha = 0.25f)
            val tickHeight = h * 0.25f
            for (t in 1 until numTicks) {
                val tx = w * (t.toFloat() / numTicks)
                drawLine(tickColor, Offset(tx, 0f), Offset(tx, tickHeight), strokeWidth = 1.dp.toPx())
                drawLine(tickColor, Offset(tx, h - tickHeight), Offset(tx, h), strokeWidth = 1.dp.toPx())
            }

            val indicatorX = w * currentFraction
            drawLine(
                color = Color.White,
                start = Offset(indicatorX, 0f),
                end = Offset(indicatorX, h),
                strokeWidth = 2.dp.toPx(),
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(indicatorX, h / 2f),
            )
        }
    }
}

@Composable
fun ReactorParametersView(
    viewModel: MainViewModel,
    activeBorder: Color,
    activePanel: Color,
) {
    val tempState by viewModel.reactorTemperature.collectAsStateWithLifecycle()
    val saltState by viewModel.reactorSalt.collectAsStateWithLifecycle()
    val activePolyState by viewModel.activePolymerase.collectAsStateWithLifecycle()
    val activeSoluteState by viewModel.activeChemicalSolute.collectAsStateWithLifecycle()
    val inletRatioAState by viewModel.inletRatioA.collectAsStateWithLifecycle()
    val inletRatioGState by viewModel.inletRatioG.collectAsStateWithLifecycle()
    val inletRatioTState by viewModel.inletRatioT.collectAsStateWithLifecycle()
    val inletRatioCState by viewModel.inletRatioC.collectAsStateWithLifecycle()

    val targetSeq by viewModel.targetSynthesisSequence.collectAsStateWithLifecycle()
    val targetTm = remember(targetSeq, saltState) {
        GeneticsEngine.calculateMeltingTemperature(targetSeq, saltState.toDouble()).toFloat()
    }
    val tempDeviance = tempState - targetTm

    val bankState = (1.0f - (inletRatioAState + inletRatioGState + inletRatioTState + inletRatioCState)).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. FEEDSTOCK INLET BIAS & SALT SILOS (At the top where raw feedstock was)
        Text(
            text = "FEEDSTOCK INLET BIAS & SALT SILOS",
            color = CyberGreenDim,
            style = Typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(1.dp, activeBorder, RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Silo A
                BioSilo(
                    label = "A",
                    value = inletRatioAState,
                    range = 0.05f..0.85f,
                    color = Color(0xFF38BDF8),
                    onValueChange = { newValue ->
                        adjustInletRatios(0, newValue, viewModel)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )

                // Silo G
                BioSilo(
                    label = "G",
                    value = inletRatioGState,
                    range = 0.05f..0.85f,
                    color = Color(0xFF34D399),
                    onValueChange = { newValue ->
                        adjustInletRatios(1, newValue, viewModel)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )

                // Silo T
                BioSilo(
                    label = "T",
                    value = inletRatioTState,
                    range = 0.05f..0.85f,
                    color = Color(0xFFFB7185),
                    onValueChange = { newValue ->
                        adjustInletRatios(2, newValue, viewModel)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )

                // Silo C
                BioSilo(
                    label = "C",
                    value = inletRatioCState,
                    range = 0.05f..0.85f,
                    color = Color(0xFFFBBF24),
                    onValueChange = { newValue ->
                        adjustInletRatios(3, newValue, viewModel)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )

                // Silo SALT
                BioSilo(
                    label = "SALT",
                    value = saltState,
                    range = 0.01f..0.15f,
                    color = Color(0xFFF8FAFC), // cyber white / phosphor white
                    onValueChange = { newValue ->
                        viewModel.setReactorSalt(newValue)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        // 2. THERMODYNAMICS & ENV CONTROLS (Chamber Temperature only, no Salt slider)
        Text(
            text = "BIOPHYSICAL CHAMBER CONTEXT",
            color = CyberGreenDim,
            style = Typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeBorder, RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CHAMBER TEMPERATURE",
                        color = Color.White,
                        style = Typography.bodySmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    val devianceAbs = kotlin.math.abs(tempDeviance)
                    val displayColor = when {
                        devianceAbs > 15f -> if (tempDeviance > 0) Color(0xFFEF4444) else Color(0xFF22D3EE)
                        devianceAbs <= 2.0f -> CyberGreen
                        else -> Color(0xFFFBBF24)
                    }
                    Text(
                        text = String.format(Locale.US, "%.1f°C (IDEAL: %.1f°C)", tempState, targetTm),
                        color = displayColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    )
                }
                BiophysicalTempSlider(
                    value = tempState,
                    targetTm = targetTm,
                    onValueChange = { viewModel.setReactorTemperature(it) },
                )
            }
        }

        // 3. POLYMERASE ENZYME SELECTOR
        Column {
            Text(
                text = "POLYMERASE ENZYME",
                color = CyberGreenDim,
                style = Typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, activeBorder, RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val polymerases = listOf("Taq", "Tth", "Pfu")
                polymerases.forEach { poly ->
                    val isSelected = activePolyState.equals(poly, ignoreCase = true)
                    val btnText = when (poly.uppercase()) {
                        "TAQ" -> "TAQ (FREE)"
                        "TTH" -> "TTH (100+100x)"
                        "PFU" -> "PFU (200+200x)"
                        else -> poly.uppercase()
                    }
                    Button(
                        onClick = {
                            viewModel.synthManager.playCombinatorTick()
                            viewModel.setActivePolymerase(poly)
                        },
                        modifier = Modifier.weight(1f).height(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) CyberGreen else Color.Transparent,
                            contentColor = if (isSelected) Color.Black else CyberGreen,
                        ),
                        border = BorderStroke(1.dp, CyberGreen),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = btnText,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }
            }
        }

        // 4. CHEMICAL SOLUTE COFACTOR SELECTOR
        Column {
            Text(
                text = "CHEMICAL SOLUTE COFACTOR",
                color = CyberGreenDim,
                style = Typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, activeBorder, RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val solutes = listOf("None", "DMSO", "Netropsin")
                solutes.forEach { solute ->
                    val isSelected = activeSoluteState.equals(solute, ignoreCase = true)
                    val soluteText = when (solute.uppercase()) {
                        "NONE" -> "NONE (0)"
                        "DMSO" -> "DMSO (100 ALL)"
                        "NETROPSIN" -> "NETROPSIN (100 ALL)"
                        else -> solute.uppercase()
                    }
                    Button(
                        onClick = {
                            viewModel.synthManager.playCombinatorTick()
                            viewModel.setActiveChemicalSolute(solute)
                        },
                        modifier = Modifier.weight(1f).height(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) CyberGreen else Color.Transparent,
                            contentColor = if (isSelected) Color.Black else CyberGreen,
                        ),
                        border = BorderStroke(1.dp, CyberGreen),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = soluteText,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }
            }
        }

        // 5. CYBER-ORRERY CALIBRATION
        val selectedHostPlanet by viewModel.selectedHostPlanet.collectAsStateWithLifecycle()

        val alignmentState = remember(selectedHostPlanet, WaveMath.manualOffsetMs) {
            val (p1, p2) = when (selectedHostPlanet.uppercase()) {
                "MERCURY", "VENUS" -> Pair("MERCURY", "VENUS")
                "EARTH", "MARS" -> Pair("EARTH", "MARS")
                "JUPITER", "SATURN" -> Pair("JUPITER", "SATURN")
                "URANUS", "NEPTUNE" -> Pair("URANUS", "NEPTUNE")
                else -> Pair("EARTH", "MARS")
            }
            WaveMath.getPairAlignmentState(p1, p2, System.currentTimeMillis() + WaveMath.manualOffsetMs)
        }

        val alignmentColor = when (alignmentState) {
            WaveMath.AlignmentState.CONJUNCTION -> CyberGreen
            WaveMath.AlignmentState.OPPOSITION -> Color(0xFFEF4444)
            WaveMath.AlignmentState.QUADRATURE -> Color(0xFFFBBF24)
            WaveMath.AlignmentState.NEUTRAL -> Color(0xFF38BDF8)
        }

        Column {
            Text(
                text = "CYBER-ORRERY CALIBRATION",
                color = CyberGreenDim,
                style = Typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "HOST PLANET: ${selectedHostPlanet.uppercase()}",
                        color = Color.White,
                        style = Typography.bodySmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "ALIGNMENT:",
                            color = CyberGreenDim,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = alignmentState.name.uppercase(),
                            color = alignmentColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BioLabTestView(viewModel: MainViewModel) {
    val scrollingGene by viewModel.scrollingGene.collectAsStateWithLifecycle()
    val poxIdleTime by viewModel.poxIdleTime.collectAsStateWithLifecycle()
    val discoveredPacketsLog by viewModel.discoveredPacketsLog.collectAsStateWithLifecycle()
    val poxReactorActive by viewModel.poxReactorActive.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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
                    fontFamily = FontFamily.Default,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = scrollingGene,
                        color = CyberGreen,
                        style = Typography.titleLarge,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "REACTOR STATE: ${if (poxReactorActive) "ACTIVE" else "IDLE"}",
                        color = CyberGreenDim,
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "NEXT CYCLE IN: ${poxIdleTime}S",
                        color = CyberGreenDim,
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
            secondaryTitle = "Discovered Gene Packets Log (T)",
            secondaryContent = {
                Text(
                    text = "Live telemetry transcription feed from the active synthesis processor.",
                    color = CyberGreen.copy(alpha = 0.8f),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (discoveredPacketsLog.isEmpty()) {
                        Text(
                            text = "LOG EMPTY: NO ACTIVE PACKETS CACHED",
                            color = Color.Gray,
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                        )
                    } else {
                        discoveredPacketsLog.takeLast(3).reversed().forEach { packet ->
                            val typeStr = if (packet.isAnomalous) "ANOMALOUS FUSION" else "GENE SYNTHESIS"
                            Text(
                                text = "• [${packet.genes.size} GENES] $typeStr SUCCESS",
                                color = if (packet.isAnomalous) Color(0xFFA855F7) else CyberGreen,
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            },
        )
    }
}

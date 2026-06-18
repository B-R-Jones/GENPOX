package com.example.genpox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import java.util.*
import kotlin.math.*

data class CanvasMetrics(
    val subnodeDepthG: Float,
    val subnodeDepthA: Float,
    val subnodeDepthT: Float,
    val subnodeDepthC: Float,
    val totalNodeDepth: Float,
    val subnodeWidthG: Float,
    val subnodeWidthA: Float,
    val subnodeWidthT: Float,
    val subnodeWidthC: Float,
    val totalNodeWidth: Float,
    val colorR: Int,
    val colorG: Int,
    val colorB: Int
)

@Composable
fun NodeCrystalCanvas(
    metrics: CanvasMetrics,
    inventoryStrings: List<String>,
    modifier: Modifier = Modifier
) {
    val wavePath = remember { Path() }
    val clipPath = remember { Path() }

    val dailyResonancePair = remember {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dateSeed = year * 10000 + month * 100 + day
        val dinucleotides = listOf(
            "GG", "GA", "GT", "GC",
            "AG", "AA", "AT", "AC",
            "TG", "TA", "TT", "TC",
            "CG", "CA", "CT", "CC"
        )
        dinucleotides[(dateSeed % 16).toInt()]
    }

    val resonanceIntensity = remember(inventoryStrings, dailyResonancePair) {
        var pairCount = 0
        inventoryStrings.forEach { seq ->
            for (i in 0 until seq.length - 1) {
                if (seq.substring(i, i + 2) == dailyResonancePair) {
                    pairCount++
                }
            }
        }
        min(1.0f, pairCount / 8.0f)
    }

    val lunarPhase = remember {
        val currentTimeSeconds = System.currentTimeMillis() / 1000.0
        val knownNewMoon = 1704974220.0 // Known New Moon epoch
        val synodicMonth = 2551443.0 // 29.53059 days in seconds
        val diff = currentTimeSeconds - knownNewMoon
        val rawPhase = (diff / synodicMonth) % 1.0
        val phaseFraction = if (rawPhase < 0) rawPhase + 1.0 else rawPhase
        0.5 - abs(phaseFraction - 0.5) // 0.0 (New Moon) to 0.5 (Full Moon)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "crystal_anim")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF030A04).copy(alpha = 0.8f))
            .border(1.dp, Color(0xFF08220C), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2

            // Baseline radius R structural modification from Lunar Phase
            val lunarBaselineMod = 1.0f + lunarPhase.toFloat() * 0.15f // full moon expands radius up to 15%
            val R = (min(size.width, size.height) / 2f) * 0.65f * lunarBaselineMod

            val subWidthG = metrics.subnodeWidthG
            val subWidthA = metrics.subnodeWidthA
            val subWidthT = metrics.subnodeWidthT
            val subWidthC = metrics.subnodeWidthC
            val depthG = metrics.subnodeDepthG
            val depthA = metrics.subnodeDepthA
            val depthT = metrics.subnodeDepthT
            val depthC = metrics.subnodeDepthC
            val totDepth = metrics.totalNodeDepth

            // Skewness/Variance for wave amplitude
            val widths = listOf(subWidthG, subWidthA, subWidthT, subWidthC)
            val avgWidth = widths.average().toFloat()
            val variance = widths.map { (it - avgWidth).pow(2) }.average().toFloat()
            val skewness = sqrt(variance)

            val waveAmplitude = 0.5f + skewness * 12.0f
            val waveFrequency = 0.05f + totDepth * 0.08f

            // Vertex points
            val ptG = Offset(cx, cy - R * subWidthG)
            val ptT = Offset(cx + R * subWidthT, cy)
            val ptC = Offset(cx, cy + R * subWidthC)
            val ptA = Offset(cx - R * subWidthA, cy)

            // Draw Background grid
            val steps = listOf(0.25f, 0.5f, 0.75f, 1.0f)
            val gridColor = Color(0xFF00FF41).copy(alpha = 0.05f)
            steps.forEach { step ->
                // Avoid path object allocations per frame by drawing line segments
                drawLine(gridColor, Offset(cx, cy - R * step), Offset(cx + R * step, cy), strokeWidth = 1f)
                drawLine(gridColor, Offset(cx + R * step, cy), Offset(cx, cy + R * step), strokeWidth = 1f)
                drawLine(gridColor, Offset(cx, cy + R * step), Offset(cx - R * step, cy), strokeWidth = 1f)
                drawLine(gridColor, Offset(cx - R * step, cy), Offset(cx, cy - R * step), strokeWidth = 1f)

                drawCircle(
                    color = Color(0xFF00FF41).copy(alpha = 0.05f),
                    radius = R * step,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 0.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f), 0f)
                    )
                )
            }

            // Radar axes
            drawLine(Color(0xFF00FF41).copy(alpha = 0.09f), Offset(cx, cy - R), Offset(cx, cy + R), strokeWidth = 1f)
            drawLine(Color(0xFF00FF41).copy(alpha = 0.09f), Offset(cx - R, cy), Offset(cx + R, cy), strokeWidth = 1f)

            // Corona under full moon
            if (lunarPhase >= 0.35) {
                val coronaIntensity = ((lunarPhase - 0.35) / 0.15).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = coronaIntensity * 0.45f),
                            Color.White.copy(alpha = coronaIntensity * 0.25f),
                            Color(0xFFEF4444).copy(alpha = coronaIntensity * 0.12f),
                            Color(0xFFEAB308).copy(alpha = coronaIntensity * 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = R * 1.5f
                    ),
                    radius = R * 1.5f,
                    center = Offset(cx, cy)
                )
            }

            // Synthesizing Glitch Spike checking
            val colorsList = listOf('G', 'T', 'C', 'A')
            val cycleDur = 4500L
            val tCycle = (animTime * 1000 % cycleDur) / cycleDur.toFloat()
            val activeIdx = (tCycle * 4).toInt() % 4
            val activeChar = colorsList[activeIdx]
            val isCrossesResonance = dailyResonancePair.contains(activeChar)
            val glitchMultiplier = if (isCrossesResonance) (1.0f + resonanceIntensity * 0.15f) else 1.0f

            // Inner Fills and Geometric Harmonics
            val numHarmonics = 2
            val alphaG = depthG * (if (lunarPhase < 0.1) 0f else 0.62f) + (if (depthG > 0) 0.08f else 0f)
            val alphaT = depthT * (if (lunarPhase < 0.1) 0f else 0.62f) + (if (depthT > 0) 0.08f else 0f)
            val alphaC = depthC * (if (lunarPhase < 0.1) 0f else 0.62f) + (if (depthC > 0) 0.08f else 0f)
            val alphaA = depthA * (if (lunarPhase < 0.1) 0f else 0.62f) + (if (depthA > 0) 0.08f else 0f)

            // Function to draw chaotic wave edge
            fun drawWaveEdge(
                pStart: Offset,
                pEnd: Offset,
                colStart: Color,
                colEnd: Color,
                scale: Float
            ) {
                val dx = pEnd.x - pStart.x
                val dy = pEnd.y - pStart.y
                val len = hypot(dx, dy)
                val angle = atan2(dy, dx)
                val segments = 12
                val amp = waveAmplitude * scale
                val freq = waveFrequency
                val brush = Brush.linearGradient(
                    colors = listOf(colStart, colEnd),
                    start = pStart,
                    end = pEnd
                )

                wavePath.reset()
                wavePath.moveTo(pStart.x, pStart.y)
                for (i in 1..segments) {
                    val t = i.toFloat() / segments
                    val px = pStart.x + dx * t
                    val py = pStart.y + dy * t

                    val envelope = sin(t * Math.PI.toFloat())
                    val waveOffset = amp * sin(t * len * freq + scale * 10f) * envelope

                    val perpX = -sin(angle) * waveOffset
                    val perpY = cos(angle) * waveOffset

                    wavePath.lineTo(px + perpX, py + perpY)
                }
                drawPath(
                    wavePath,
                    brush = brush,
                    style = Stroke(
                        width = (0.7f + (totDepth / 4f) * 0.8f) * (scale + 0.1f) * glitchMultiplier
                    )
                )
            }

            fun drawShellLayer(scale: Float, alphaMod: Float) {
                val hG = Offset(cx, cy - R * subWidthG * scale)
                val hT = Offset(cx + R * subWidthT * scale, cy)
                val hC = Offset(cx, cy + R * subWidthC * scale)
                val hA = Offset(cx - R * subWidthA * scale, cy)

                // Draw solid inner clip gradient if not New Moon
                if (lunarPhase >= 0.05) {
                    clipPath.reset()
                    clipPath.moveTo(hG.x, hG.y)
                    clipPath.lineTo(hT.x, hT.y)
                    clipPath.lineTo(hC.x, hC.y)
                    clipPath.lineTo(hA.x, hA.y)
                    clipPath.close()
                    
                    clipPath(clipPath) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFEF4444).copy(alpha = alphaG * alphaMod * 0.42f), Color.Transparent),
                                center = hG,
                                radius = R * 1.3f
                            ),
                            radius = R * 1.3f,
                            center = hG
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF3B82F6).copy(alpha = alphaT * alphaMod * 0.42f), Color.Transparent),
                                center = hT,
                                radius = R * 1.3f
                            ),
                            radius = R * 1.3f,
                            center = hT
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFEAB308).copy(alpha = alphaC * alphaMod * 0.42f), Color.Transparent),
                                center = hC,
                                radius = R * 1.3f
                            ),
                            radius = R * 1.3f,
                            center = hC
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF22C55E).copy(alpha = alphaA * alphaMod * 0.42f), Color.Transparent),
                                center = hA,
                                radius = R * 1.3f
                            ),
                            radius = R * 1.3f,
                            center = hA
                        )
                    }
                }

                // Draw outlines (wavy for outer shell, straight vector outlines for inner harmonics)
                if (scale == 1.0f) {
                    drawWaveEdge(hG, hT, Color(0xFFEF4444).copy(alpha = alphaG * alphaMod + 0.1f), Color(0xFF3B82F6).copy(alpha = alphaT * alphaMod + 0.1f), scale)
                    drawWaveEdge(hT, hC, Color(0xFF3B82F6).copy(alpha = alphaT * alphaMod + 0.1f), Color(0xFFEAB308).copy(alpha = alphaC * alphaMod + 0.1f), scale)
                    drawWaveEdge(hC, hA, Color(0xFFEAB308).copy(alpha = alphaC * alphaMod + 0.1f), Color(0xFF22C55E).copy(alpha = alphaA * alphaMod + 0.1f), scale)
                    drawWaveEdge(hA, hG, Color(0xFF22C55E).copy(alpha = alphaA * alphaMod + 0.1f), Color(0xFFEF4444).copy(alpha = alphaG * alphaMod + 0.1f), scale)
                } else {
                    val strokeW = (0.7f + (totDepth / 4f) * 0.8f) * (scale + 0.1f) * glitchMultiplier
                    drawLine(
                        brush = Brush.linearGradient(colors = listOf(Color(0xFFEF4444).copy(alpha = alphaG * alphaMod + 0.1f), Color(0xFF3B82F6).copy(alpha = alphaT * alphaMod + 0.1f)), start = hG, end = hT),
                        start = hG,
                        end = hT,
                        strokeWidth = strokeW
                    )
                    drawLine(
                        brush = Brush.linearGradient(colors = listOf(Color(0xFF3B82F6).copy(alpha = alphaT * alphaMod + 0.1f), Color(0xFFEAB308).copy(alpha = alphaC * alphaMod + 0.1f)), start = hT, end = hC),
                        start = hT,
                        end = hC,
                        strokeWidth = strokeW
                    )
                    drawLine(
                        brush = Brush.linearGradient(colors = listOf(Color(0xFFEAB308).copy(alpha = alphaC * alphaMod + 0.1f), Color(0xFF22C55E).copy(alpha = alphaA * alphaMod + 0.1f)), start = hC, end = hA),
                        start = hC,
                        end = hA,
                        strokeWidth = strokeW
                    )
                    drawLine(
                        brush = Brush.linearGradient(colors = listOf(Color(0xFF22C55E).copy(alpha = alphaA * alphaMod + 0.1f), Color(0xFFEF4444).copy(alpha = alphaG * alphaMod + 0.1f)), start = hA, end = hG),
                        start = hA,
                        end = hG,
                        strokeWidth = strokeW
                    )
                }
            }



            // Draw outer shell (scale = 1.0f)
            drawShellLayer(1.0f, 1.0f)

            // Draw nested harmonics (expanding pulsing echoes)
            for (k in 1..numHarmonics) {
                val tPhase = (animTime * 0.00008f - k * 0.3f) % 1f
                val progress = if (tPhase < 0f) tPhase + 1f else tPhase
                val hScale = 0.15f + progress * 0.83f
                val alphaFade = sin(progress * Math.PI.toFloat()) * (0.65f - k * 0.1f)
                if (alphaFade > 0f) {
                    drawShellLayer(hScale, alphaFade)
                }
            }

            // Draw vertex glowing dots
            val dotMarkers = listOf(
                Pair(ptG, Color(0xFFEF4444)),
                Pair(ptT, Color(0xFF3B82F6)),
                Pair(ptC, Color(0xFFEAB308)),
                Pair(ptA, Color(0xFF22C55E))
            )

            dotMarkers.forEachIndexed { idx, pair ->
                val size = 3.5f + when (idx) {
                    0 -> alphaG
                    1 -> alphaT
                    2 -> alphaC
                    else -> alphaA
                } * 2.0f
                drawCircle(
                    color = pair.second,
                    radius = size,
                    center = pair.first
                )
            }

            // Center anchor dot
            drawCircle(
                color = Color(0xFF00FF41).copy(alpha = 0.55f),
                radius = 1.5f,
                center = Offset(cx, cy)
            )
        }
    }
}

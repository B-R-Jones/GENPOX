package com.example.genpox.ui.components

import android.view.Choreographer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

class MapProfilerState {
    var fps by mutableStateOf(0f)
    var avgFrameTimeMs by mutableStateOf(0f)
    var jankRate by mutableStateOf(0f)
    
    var staticRecomps = 0
    var dynamicRecomps = 0
    var hudRecomps = 0
    
    var staticRecompRate by mutableStateOf(0)
    var dynamicRecompRate by mutableStateOf(0)
    var hudRecompRate by mutableStateOf(0)
    
    var drawPathCount by mutableStateOf(0)
    var drawLineCount by mutableStateOf(0)
    var drawCircleCount by mutableStateOf(0)
    var drawTextCount by mutableStateOf(0)
    
    val frameTimeHistory = mutableStateListOf<Float>()
    
    fun updateFrameTime(timeMs: Float) {
        frameTimeHistory.add(timeMs)
        if (frameTimeHistory.size > 80) {
            frameTimeHistory.removeAt(0)
        }
    }
    
    fun resetFrameDrawCounters() {
        drawPathCount = 0
        drawLineCount = 0
        drawCircleCount = 0
        drawTextCount = 0
    }
}

class ProfilingDrawScope(
    private val delegate: DrawScope,
    private val state: MapProfilerState
) : DrawScope by delegate {

    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: androidx.compose.ui.graphics.StrokeCap,
        pathEffect: androidx.compose.ui.graphics.PathEffect?,
        alpha: Float,
        colorFilter: androidx.compose.ui.graphics.ColorFilter?,
        blendMode: androidx.compose.ui.graphics.BlendMode
    ) {
        state.drawLineCount++
        delegate.drawLine(color, start, end, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    }

    override fun drawPath(
        path: Path,
        color: Color,
        alpha: Float,
        style: DrawStyle,
        colorFilter: androidx.compose.ui.graphics.ColorFilter?,
        blendMode: androidx.compose.ui.graphics.BlendMode
    ) {
        state.drawPathCount++
        delegate.drawPath(path, color, alpha, style, colorFilter, blendMode)
    }

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: androidx.compose.ui.graphics.ColorFilter?,
        blendMode: androidx.compose.ui.graphics.BlendMode
    ) {
        state.drawCircleCount++
        delegate.drawCircle(color, radius, center, alpha, style, colorFilter, blendMode)
    }

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: androidx.compose.ui.graphics.ColorFilter?,
        blendMode: androidx.compose.ui.graphics.BlendMode
    ) {
        state.drawCircleCount++
        delegate.drawArc(color, startAngle, sweepAngle, useCenter, topLeft, size, alpha, style, colorFilter, blendMode)
    }
}

@Composable
fun FrameTimeMonitor(
    state: MapProfilerState,
    enabled: Boolean
) {
    if (!enabled) return

    LaunchedEffect(enabled) {
        var lastFrameTimeNanos = 0L
        while (isActive) {
            androidx.compose.runtime.withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos > 0) {
                    val frameTimeMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
                    state.updateFrameTime(frameTimeMs)
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    LaunchedEffect(enabled) {
        var logTicks = 0
        while (isActive) {
            delay(500)
            val history = state.frameTimeHistory.toList()
            val activeHistory = history.filter { it < 100f }
            if (activeHistory.isNotEmpty()) {
                val avg = activeHistory.average().toFloat()
                state.avgFrameTimeMs = avg
                state.fps = (1000f / avg).coerceIn(0f, 120f)
                
                val janks = activeHistory.count { it > 17f }
                state.jankRate = (janks.toFloat() / activeHistory.size) * 100f
            } else if (history.isNotEmpty()) {
                val avg = history.average().toFloat()
                state.avgFrameTimeMs = avg
                state.fps = (1000f / avg).coerceIn(0f, 120f)
                state.jankRate = 0f
            }
            
            state.staticRecompRate = state.staticRecomps * 2
            state.dynamicRecompRate = state.dynamicRecomps * 2
            state.hudRecompRate = state.hudRecomps * 2
            
            state.staticRecomps = 0
            state.dynamicRecomps = 0
            state.hudRecomps = 0
            
            logTicks++
            if (logTicks >= 4) {
                logTicks = 0
                android.util.Log.d(
                    "MapProfiler",
                    String.format(
                        java.util.Locale.US,
                        "STATS | FPS: %.1f Hz | Frame: %.1f ms | Jank: %.1f%% | " +
                        "Recomps [Static: %d, Dynamic: %d, HUD: %d] | " +
                        "DrawCalls [Lines: %d, Paths: %d, Circles: %d, Texts: %d]",
                        state.fps, state.avgFrameTimeMs, state.jankRate,
                        state.staticRecompRate, state.dynamicRecompRate, state.hudRecompRate,
                        state.drawLineCount, state.drawPathCount, state.drawCircleCount, state.drawTextCount
                    )
                )
            }
        }
    }
}

@Composable
fun ProfilerHUDOverlay(
    state: MapProfilerState,
    modifier: Modifier = Modifier
) {
    val greenGlow = Color(0xFF00FF66)
    val paneBg = Color.Black.copy(alpha = 0.85f)
    val borderCol = Color(0xFF00FF66).copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .width(280.dp)
            .padding(12.dp)
            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
            .background(paneBg, RoundedCornerShape(4.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "G.E.N.P.O.X. RENDER PROFILER",
                color = greenGlow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "FPS: %.1f Hz", state.fps),
                    color = greenGlow,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(java.util.Locale.US, "Frame: %.1f ms", state.avgFrameTimeMs),
                    color = greenGlow,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(java.util.Locale.US, "Jank: %.1f%%", state.jankRate),
                    color = if (state.jankRate > 5f) Color.Red else greenGlow,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = "RECOMPOSITION RATE (r/s)",
                color = greenGlow.copy(alpha = 0.7f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Static: ${state.staticRecompRate}", color = greenGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Dynamic: ${state.dynamicRecompRate}", color = greenGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(text = "HUD: ${state.hudRecompRate}", color = greenGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = "GPU DRAW CALLS / FRAME",
                color = greenGlow.copy(alpha = 0.7f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Lines: ${state.drawLineCount}", color = greenGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Paths: ${state.drawPathCount}", color = greenGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Circs: ${state.drawCircleCount}", color = greenGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Texts: ${state.drawTextCount}", color = greenGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .border(0.5.dp, greenGlow.copy(alpha = 0.2f))
            ) {
                val w = size.width
                val h = size.height
                
                val thresholdY = h - (16.6f / 33.3f) * h
                drawLine(
                    color = Color.Red.copy(alpha = 0.35f),
                    start = Offset(0f, thresholdY),
                    end = Offset(w, thresholdY),
                    strokeWidth = 0.5f
                )
                
                val history = state.frameTimeHistory.toList()
                if (history.size > 1) {
                    val maxVal = 33.3f
                    val stepX = w / 80f
                    val path = Path()
                    
                    val firstY = h - (history[0].coerceAtMost(maxVal) / maxVal) * h
                    path.moveTo(0f, firstY)
                    
                    for (i in 1 until history.size) {
                        val valY = h - (history[i].coerceAtMost(maxVal) / maxVal) * h
                        path.lineTo(i * stepX, valY)
                    }
                    
                    drawPath(
                        path = path,
                        color = greenGlow,
                        style = Stroke(width = 1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilingCanvas(
    modifier: Modifier,
    isProfilerEnabled: Boolean,
    profilerState: MapProfilerState,
    onDraw: DrawScope.() -> Unit
) {
    Canvas(modifier = modifier) {
        if (isProfilerEnabled) {
            val profilingScope = ProfilingDrawScope(this, profilerState)
            profilingScope.onDraw()
        } else {
            onDraw()
        }
    }
}

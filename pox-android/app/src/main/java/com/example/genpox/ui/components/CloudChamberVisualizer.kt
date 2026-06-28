package com.example.genpox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.PoxBaseFont
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

data class IonisationStreak(
    val id: String,
    val points: List<Offset>,
    val color: Color,
    val spawnTime: Long,
    val lifetimeMs: Long
)

@Composable
fun CloudChamberVisualizer(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val nearestDistance by viewModel.nearestFontDistance.collectAsState()
    val activeSiphonFont by viewModel.activeSiphonedFont.collectAsState()
    
    val streaks = remember { mutableStateListOf<IonisationStreak>() }
    val random = remember { Random() }
    
    LaunchedEffect(viewModel) {
        var lastTime = System.currentTimeMillis()
        var timeSinceLastSpawn = 0.0
        
        while (isActive) {
            val now = System.currentTimeMillis()
            val dt = (now - lastTime) / 1000.0
            lastTime = now
            
            streaks.removeAll { now - it.spawnTime >= it.lifetimeMs }
            
            val d = nearestDistance ?: 9999.0
            val lambda = if (d <= 500.0) {
                0.4 + 5.6 * Math.pow(1.0 - (d / 500.0), 2.0)
            } else {
                0.4
            }
            
            val spawnInterval = 1.0 / lambda
            timeSinceLastSpawn += dt
            
            if (timeSinceLastSpawn >= spawnInterval) {
                timeSinceLastSpawn = 0.0
                
                val color = if (activeSiphonFont != null) {
                    when (activeSiphonFont?.baseType) {
                        'A' -> Color(0xFF38BDF8)
                        'G' -> Color(0xFF34D399)
                        'T' -> Color(0xFFFB7185)
                        'C' -> Color(0xFFFBBF24)
                        else -> Color(0xFF00993C).copy(alpha = 0.15f)
                    }
                } else {
                    Color(0xFF00993C).copy(alpha = 0.08f)
                }
                
                // Determine random starting position (either on edges or inside)
                val edge = random.nextInt(4)
                var startX = 0f
                var startY = 0f
                var angle = 0.0
                
                when (edge) {
                    0 -> { // Top edge
                        startX = random.nextFloat()
                        startY = -0.05f
                        angle = Math.PI * 0.25 + random.nextFloat() * Math.PI * 0.5
                    }
                    1 -> { // Bottom edge
                        startX = random.nextFloat()
                        startY = 1.05f
                        angle = -Math.PI * 0.75 + random.nextFloat() * Math.PI * 0.5
                    }
                    2 -> { // Left edge
                        startX = -0.05f
                        startY = random.nextFloat()
                        angle = -Math.PI * 0.25 + random.nextFloat() * Math.PI * 0.5
                    }
                    3 -> { // Right edge
                        startX = 1.05f
                        startY = random.nextFloat()
                        angle = Math.PI * 0.75 + random.nextFloat() * Math.PI * 0.5
                    }
                }
                
                val speed = 0.3f + random.nextFloat() * 0.5f
                val curve = -2.0f + random.nextFloat() * 4.0f
                
                val pathPoints = mutableListOf<Offset>()
                var cx = startX
                var cy = startY
                val numSegments = 12 + random.nextInt(8)
                val step = 0.03f
                
                for (s in 0 until numSegments) {
                    pathPoints.add(Offset(cx, cy))
                    cx += (speed * cos(angle)).toFloat() * step
                    cy += (speed * sin(angle)).toFloat() * step
                    angle += curve * step
                }
                
                val lifetime = 600L + random.nextInt(400).toLong()
                val streakId = java.util.UUID.randomUUID().toString()
                
                streaks.add(
                    IonisationStreak(
                        id = streakId,
                        points = pathPoints,
                        color = color,
                        spawnTime = now,
                        lifetimeMs = lifetime
                    )
                )
            }
            
            kotlinx.coroutines.delay(16)
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        streaks.forEach { streak ->
            if (streak.points.size >= 2) {
                val now = System.currentTimeMillis()
                val age = now - streak.spawnTime
                val lifeFraction = age.toFloat() / streak.lifetimeMs
                val alpha = (1.0f - lifeFraction).coerceIn(0f, 1f)
                
                val path = Path()
                val firstPt = streak.points[0]
                path.moveTo(firstPt.x * w, firstPt.y * h)
                
                for (i in 1 until streak.points.size) {
                    val pt = streak.points[i]
                    path.lineTo(pt.x * w, pt.y * h)
                }
                
                val drawColor = if (activeSiphonFont != null) {
                    streak.color.copy(alpha = alpha * 0.38f)
                } else {
                    streak.color.copy(alpha = alpha * 0.12f)
                }
                
                drawPath(
                    path = path,
                    color = drawColor,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = null
                    )
                )
            }
        }
    }
}

package com.example.genpox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

class CreatureGeometry(val dna: String) {
    val vertices = mutableListOf<Vertex3D>()
    val edges = mutableListOf<Pair<Int, Int>>()
    
    val innerVertices = mutableListOf<Vertex3D>()
    val innerEdges = mutableListOf<Pair<Int, Int>>()
    
    var baseRadius = 1.0
    var breatheAmp = 0.1
    var breatheFreq = 2
    var twistRate = 0.0
    
    init {
        generate()
    }
    
    private fun generate() {
        val genes = dna.chunked(8)
        if (genes.size < 8) return
        
        val g1 = genes[0]
        val g2 = genes[1]
        val g3 = genes[2]
        val g4 = genes[3]
        val g5 = genes[4]
        val g6 = genes[5]
        val g7 = genes[6]
        val g8 = genes[7]
        
        // --- Gene 1: Core Shape ---
        val g1Sum = g1.sumOf { it.code }
        val shapeType = g1Sum % 5
        baseRadius = 60.0 * (0.8 + (g1Sum % 5) * 0.08) // Base model radius around 48-67
        
        val coreVertices = mutableListOf<Vertex3D>()
        val coreEdges = mutableListOf<Pair<Int, Int>>()
        
        when (shapeType) {
            0 -> { // Tetrahedron
                coreVertices.add(Vertex3D(1.0, 1.0, 1.0))
                coreVertices.add(Vertex3D(-1.0, -1.0, 1.0))
                coreVertices.add(Vertex3D(-1.0, 1.0, -1.0))
                coreVertices.add(Vertex3D(1.0, -1.0, -1.0))
                
                coreEdges.add(Pair(0, 1))
                coreEdges.add(Pair(0, 2))
                coreEdges.add(Pair(0, 3))
                coreEdges.add(Pair(1, 2))
                coreEdges.add(Pair(1, 3))
                coreEdges.add(Pair(2, 3))
            }
            1 -> { // Cube
                for (x in listOf(-1.0, 1.0)) {
                    for (y in listOf(-1.0, 1.0)) {
                        for (z in listOf(-1.0, 1.0)) {
                            coreVertices.add(Vertex3D(x, y, z))
                        }
                    }
                }
                // Connect cube edges
                for (i in 0 until 8) {
                    for (j in i + 1 until 8) {
                        var diffCount = 0
                        if (coreVertices[i].x != coreVertices[j].x) diffCount++
                        if (coreVertices[i].y != coreVertices[j].y) diffCount++
                        if (coreVertices[i].z != coreVertices[j].z) diffCount++
                        if (diffCount == 1) {
                            coreEdges.add(Pair(i, j))
                        }
                    }
                }
            }
            2 -> { // Octahedron
                coreVertices.add(Vertex3D(1.0, 0.0, 0.0))
                coreVertices.add(Vertex3D(-1.0, 0.0, 0.0))
                coreVertices.add(Vertex3D(0.0, 1.0, 0.0))
                coreVertices.add(Vertex3D(0.0, -1.0, 0.0))
                coreVertices.add(Vertex3D(0.0, 0.0, 1.0))
                coreVertices.add(Vertex3D(0.0, 0.0, -1.0))
                
                for (i in 0 until 6) {
                    for (j in i + 1 until 6) {
                        if ((i == 0 && j == 1) || (i == 2 && j == 3) || (i == 4 && j == 5)) continue
                        coreEdges.add(Pair(i, j))
                    }
                }
            }
            3 -> { // Icosahedron
                val phi = (1.0 + sqrt(5.0)) / 2.0
                coreVertices.add(Vertex3D(-1.0, phi, 0.0))
                coreVertices.add(Vertex3D(1.0, phi, 0.0))
                coreVertices.add(Vertex3D(-1.0, -phi, 0.0))
                coreVertices.add(Vertex3D(1.0, -phi, 0.0))
                
                coreVertices.add(Vertex3D(0.0, -1.0, phi))
                coreVertices.add(Vertex3D(0.0, 1.0, phi))
                coreVertices.add(Vertex3D(0.0, -1.0, -phi))
                coreVertices.add(Vertex3D(0.0, 1.0, -phi))
                
                coreVertices.add(Vertex3D(phi, 0.0, -1.0))
                coreVertices.add(Vertex3D(phi, 0.0, 1.0))
                coreVertices.add(Vertex3D(-phi, 0.0, -1.0))
                coreVertices.add(Vertex3D(-phi, 0.0, 1.0))
                
                // Normalize vertices
                for (v in coreVertices) {
                    val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
                    v.x /= len
                    v.y /= len
                    v.z /= len
                }
                // Connect if distance matches
                for (i in 0 until 12) {
                    for (j in i + 1 until 12) {
                        val dx = coreVertices[i].x - coreVertices[j].x
                        val dy = coreVertices[i].y - coreVertices[j].y
                        val dz = coreVertices[i].z - coreVertices[j].z
                        val dist = sqrt(dx*dx + dy*dy + dz*dz)
                        if (dist < 1.1) {
                            coreEdges.add(Pair(i, j))
                        }
                    }
                }
            }
            else -> { // Dodecahedron
                val phi = (1.0 + sqrt(5.0)) / 2.0
                for (x in listOf(-1.0, 1.0)) {
                    for (y in listOf(-1.0, 1.0)) {
                        for (z in listOf(-1.0, 1.0)) {
                            coreVertices.add(Vertex3D(x, y, z))
                        }
                    }
                }
                val invPhi = 1.0 / phi
                coreVertices.add(Vertex3D(0.0, invPhi, phi))
                coreVertices.add(Vertex3D(0.0, -invPhi, phi))
                coreVertices.add(Vertex3D(0.0, invPhi, -phi))
                coreVertices.add(Vertex3D(0.0, -invPhi, -phi))
                
                coreVertices.add(Vertex3D(invPhi, phi, 0.0))
                coreVertices.add(Vertex3D(-invPhi, phi, 0.0))
                coreVertices.add(Vertex3D(invPhi, -phi, 0.0))
                coreVertices.add(Vertex3D(-invPhi, -phi, 0.0))
                
                coreVertices.add(Vertex3D(phi, 0.0, invPhi))
                coreVertices.add(Vertex3D(-phi, 0.0, invPhi))
                coreVertices.add(Vertex3D(phi, 0.0, -invPhi))
                coreVertices.add(Vertex3D(-phi, 0.0, -invPhi))
                
                // Normalize
                for (v in coreVertices) {
                    val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
                    v.x /= len
                    v.y /= len
                    v.z /= len
                }
                // Connect
                for (i in 0 until 20) {
                    for (j in i + 1 until 20) {
                        val dx = coreVertices[i].x - coreVertices[j].x
                        val dy = coreVertices[i].y - coreVertices[j].y
                        val dz = coreVertices[i].z - coreVertices[j].z
                        val dist = sqrt(dx*dx + dy*dy + dz*dz)
                        if (dist < 0.77) {
                            coreEdges.add(Pair(i, j))
                        }
                    }
                }
            }
        }
        
        // Scale to base size
        for (v in coreVertices) {
            v.x *= baseRadius
            v.y *= baseRadius
            v.z *= baseRadius
        }
        
        // --- Gene 2: Cephalic (Head) Distortion ---
        val g2Sum = g2.sumOf { it.code }
        val headDisplace = 0.7 + (g2Sum % 8) * 0.12 // 0.7 to 1.66
        for (v in coreVertices) {
            if (v.y > baseRadius * 0.2) {
                v.x *= headDisplace
                v.y *= headDisplace
                v.z *= headDisplace
            }
        }
        
        vertices.addAll(coreVertices)
        edges.addAll(coreEdges)
        
        // --- Gene 3: Limb Extrusions ---
        val g3Sum = g3.sumOf { it.code }
        val limbLength = baseRadius * (0.35 + (g3Sum % 6) * 0.12)
        val numLimbPairs = 1 + (g3Sum % 3)
        
        val lateralIndices = coreVertices.indices
            .filter { abs(coreVertices[it].x) > baseRadius * 0.4 }
            .sortedByDescending { abs(coreVertices[it].x) }
            
        for (k in 0 until min(numLimbPairs * 2, lateralIndices.size)) {
            val baseIdx = lateralIndices[k]
            val baseV = coreVertices[baseIdx]
            val signX = if (baseV.x >= 0.0) 1.0 else -1.0
            
            // Joint 1
            val j1 = Vertex3D(
                baseV.x + signX * limbLength * 0.6,
                baseV.y - limbLength * 0.2,
                baseV.z
            )
            vertices.add(j1)
            val j1Idx = vertices.size - 1
            edges.add(Pair(baseIdx, j1Idx))
            
            // Joint 2 (extremity)
            val j2 = Vertex3D(
                j1.x + signX * limbLength * 0.4,
                j1.y - limbLength * 0.6,
                j1.z
            )
            vertices.add(j2)
            val j2Idx = vertices.size - 1
            edges.add(Pair(j1Idx, j2Idx))
        }
        
        // --- Gene 4: Armor Plating (Outer concentric cage) ---
        val g4Sum = g4.sumOf { it.code }
        val hasArmor = g4Sum % 2 == 0
        val armorScale = 1.15 + (g4Sum % 4) * 0.04
        if (hasArmor) {
            val coreCount = coreVertices.size
            val armorStartIndex = vertices.size
            
            for (i in 0 until coreCount) {
                val cv = coreVertices[i]
                vertices.add(Vertex3D(cv.x * armorScale, cv.y * armorScale, cv.z * armorScale))
            }
            for (edge in coreEdges) {
                edges.add(Pair(edge.first + armorStartIndex, edge.second + armorStartIndex))
            }
            for (i in 0 until min(4, coreCount)) {
                edges.add(Pair(i, i + armorStartIndex))
            }
        }
        
        // --- Gene 5: Core Energy Reactor ---
        val g5Sum = g5.sumOf { it.code }
        val innerRadius = baseRadius * (0.22 + (g5Sum % 4) * 0.05)
        innerVertices.add(Vertex3D(innerRadius, 0.0, 0.0))
        innerVertices.add(Vertex3D(-innerRadius, 0.0, 0.0))
        innerVertices.add(Vertex3D(0.0, innerRadius, 0.0))
        innerVertices.add(Vertex3D(0.0, -innerRadius, 0.0))
        innerVertices.add(Vertex3D(0.0, 0.0, innerRadius))
        innerVertices.add(Vertex3D(0.0, 0.0, -innerRadius))
        for (i in 0 until 6) {
            for (j in i + 1 until 6) {
                if ((i == 0 && j == 1) || (i == 2 && j == 3) || (i == 4 && j == 5)) continue
                innerEdges.add(Pair(i, j))
            }
        }
        
        // --- Gene 6: Breathing Undulation Config ---
        val g6Sum = g6.sumOf { it.code }
        breatheAmp = 0.04 + (g6Sum % 5) * 0.03
        breatheFreq = 1 + (g6Sum % 3)
        
        // --- Gene 7: Helical Twist Config ---
        val g7Sum = g7.sumOf { it.code }
        twistRate = ((g7Sum % 8) - 4) * 0.003
    }
}

data class Vertex3D(var x: Double, var y: Double, var z: Double)

@Composable
fun CreatureWireframeView(
    dna: String,
    faction: String,
    modifier: Modifier = Modifier
) {
    val geometry = remember(dna) { CreatureGeometry(dna) }

    // Infinite transitions for smooth rotation and breathing cycles
    val infiniteTransition = rememberInfiniteTransition(label = "creature_spin")
    
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val breathingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath"
    )

    val staticTrigger by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "static"
    )

    // Holographic faction glow colors
    val factionColor = when (faction) {
        "Infection" -> Color(0xFFEF4444)
        "Mech" -> Color(0xFF3B82F6)
        "Parasite" -> Color(0xFFA855F7)
        else -> Color(0xFF00FF41)
    }

    val frontColor = factionColor
    val backColor = factionColor.copy(alpha = 0.22f)
    val innerCoreColor = Color(0xFFFBBF24) // Gold glowing energy core

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val minDimension = min(width, height)
        val densityVal = density
        
        // Scale model to match frame size
        val visualScale = (minDimension / 180f).coerceIn(0.25f, 2.5f)

        val tilt = Math.toRadians(18.0) // constant viewing tilt angle
        val cosT = cos(tilt)
        val sinT = sin(tilt)

        val cosS = cos(spinAngle.toDouble())
        val sinS = sin(spinAngle.toDouble())

        val breathScale = 1.0 + geometry.breatheAmp * sin(breathingPhase.toDouble() * geometry.breatheFreq)
        val finalScale = visualScale

        if (finalScale < 0.3f) {
            // LOD Tier 3: Draw a single glowing contact dot
            drawCircle(
                color = factionColor,
                radius = 4f * densityVal,
                center = Offset(centerX, centerY)
            )
        } else {
            val projectedPoints = mutableListOf<Offset>()
            val depthZ = mutableListOf<Double>()

            // 1. Project primary creature vertices (Body, limbs, armor)
            for (v in geometry.vertices) {
                // Apply breathing scale
                val bx = v.x * breathScale * finalScale
                val by = v.y * breathScale * finalScale
                val bz = v.z * breathScale * finalScale

                // Apply helical twist
                val twist = v.y * geometry.twistRate
                val cosTw = cos(twist)
                val sinTw = sin(twist)
                val tx = bx * cosTw - bz * sinTw
                val tz = bx * sinTw + bz * cosTw
                val ty = by

                // Apply global spin (rotation around Y-axis)
                val rx = tx * cosS - tz * sinS
                val rz = tx * sinS + tz * cosS
                val ry = ty

                // Apply global tilt (rotation around X-axis)
                val finalX = rx
                val finalY = ry * cosT - rz * sinT
                val finalZ = ry * sinT + rz * cosT

                projectedPoints.add(Offset((centerX + finalX).toFloat(), (centerY - finalY).toFloat()))
                depthZ.add(finalZ)
            }

            val maxRadius = (geometry.baseRadius * breathScale * finalScale * 1.4).coerceAtLeast(10.0)

            // 2. Draw outer model drop shadows (LOD Tier 1: finalScale > 0.8f)
            if (finalScale > 0.8f) {
                for (edge in geometry.edges) {
                    if (edge.first < projectedPoints.size && edge.second < projectedPoints.size) {
                        val z1 = depthZ[edge.first]
                        val z2 = depthZ[edge.second]
                        val avgZ = (z1 + z2) / 2.0
                        
                        val depthPct = ((avgZ / maxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                        val alpha = (0.18f + 0.82f * depthPct).toFloat()
                        val stroke = (0.8f + 1.2f * depthPct).toFloat() * densityVal

                        drawLine(
                            color = Color.Black.copy(alpha = alpha * 0.8f),
                            start = projectedPoints[edge.first] + Offset(1.5f * densityVal, 1.5f * densityVal),
                            end = projectedPoints[edge.second] + Offset(1.5f * densityVal, 1.5f * densityVal),
                            strokeWidth = stroke
                        )
                    }
                }
            }

            // 3. Draw outer model main lines
            for (edge in geometry.edges) {
                if (edge.first < projectedPoints.size && edge.second < projectedPoints.size) {
                    val z1 = depthZ[edge.first]
                    val z2 = depthZ[edge.second]
                    val avgZ = (z1 + z2) / 2.0
                    
                    val depthPct = ((avgZ / maxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                    val alpha = (0.18f + 0.82f * depthPct).toFloat()
                    val stroke = (0.8f + 1.2f * depthPct).toFloat() * densityVal

                    drawLine(
                        color = factionColor.copy(alpha = alpha),
                        start = projectedPoints[edge.first],
                        end = projectedPoints[edge.second],
                        strokeWidth = stroke
                    )
                }
            }

            // 4. Project and draw inner core energy reactor (LOD Tier 1: finalScale > 0.8f)
            if (finalScale > 0.8f) {
                val coreSpinAngle = spinAngle * 3.5 // spins faster
                val cosCs = cos(coreSpinAngle.toDouble())
                val sinCs = sin(coreSpinAngle.toDouble())

                val projectedInner = mutableListOf<Offset>()
                val depthInnerZ = mutableListOf<Double>()

                for (v in geometry.innerVertices) {
                    val vx = v.x * finalScale
                    val vy = v.y * finalScale
                    val vz = v.z * finalScale

                    // Spin core around both Y and Z axis for a gyroscope visual
                    val rx = vx * cosCs - vz * sinCs
                    val rz = vx * sinCs + vz * cosCs
                    val ry = vy

                    val finalX = rx
                    val finalY = ry * cosT - rz * sinT
                    val finalZ = ry * sinT + rz * cosT

                    projectedInner.add(Offset((centerX + finalX).toFloat(), (centerY - finalY).toFloat()))
                    depthInnerZ.add(finalZ)
                }

                val innerMaxRadius = (geometry.innerVertices.firstOrNull()?.x ?: 10.0) * finalScale * 1.2

                // Inner core shadows
                for (edge in geometry.innerEdges) {
                    if (edge.first < projectedInner.size && edge.second < projectedInner.size) {
                        val z1 = depthInnerZ[edge.first]
                        val z2 = depthInnerZ[edge.second]
                        val avgZ = (z1 + z2) / 2.0
                        
                        val depthPct = ((avgZ / innerMaxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                        val alpha = (0.2f + 0.8f * depthPct).toFloat()
                        val stroke = (0.6f + 0.8f * depthPct).toFloat() * densityVal

                        drawLine(
                            color = Color.Black.copy(alpha = alpha * 0.8f),
                            start = projectedInner[edge.first] + Offset(1.5f * densityVal, 1.5f * densityVal),
                            end = projectedInner[edge.second] + Offset(1.5f * densityVal, 1.5f * densityVal),
                            strokeWidth = stroke
                        )
                    }
                }

                // Inner core lines
                for (edge in geometry.innerEdges) {
                    if (edge.first < projectedInner.size && edge.second < projectedInner.size) {
                        val z1 = depthInnerZ[edge.first]
                        val z2 = depthInnerZ[edge.second]
                        val avgZ = (z1 + z2) / 2.0
                        
                        val depthPct = ((avgZ / innerMaxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                        val alpha = (0.2f + 0.8f * depthPct).toFloat()
                        val stroke = (0.6f + 0.8f * depthPct).toFloat() * densityVal

                        drawLine(
                            color = innerCoreColor.copy(alpha = alpha),
                            start = projectedInner[edge.first],
                            end = projectedInner[edge.second],
                            strokeWidth = stroke
                        )
                    }
                }
            }
        }

        // 4. Draw Scanner Static / Horizontal scanlines
        val scanlineSpacing = 5f * densityVal
        var yVal = 0f
        while (yVal < height) {
            drawLine(
                color = factionColor.copy(alpha = 0.04f),
                start = Offset(0f, yVal),
                end = Offset(width, yVal),
                strokeWidth = 1f
            )
            yVal += scanlineSpacing
        }

        // Horizontal sweep scanner bar
        val beamY = (staticTrigger / 100f) * height
        drawLine(
            color = factionColor.copy(alpha = 0.12f),
            start = Offset(0f, beamY),
            end = Offset(width, beamY),
            strokeWidth = 3f * densityVal
        )

        // Random popping horizontal static lines (flicker)
        val triggerInt = staticTrigger.toInt()
        if (triggerInt % 13 == 0) {
            val randomY = (sin(staticTrigger.toDouble()) * 0.5 + 0.5) * height
            drawLine(
                color = factionColor.copy(alpha = 0.22f),
                start = Offset(0f, randomY.toFloat()),
                end = Offset(width, randomY.toFloat()),
                strokeWidth = 1f * densityVal
            )
        }

        // Random glowing static noise dots
        val dotCount = 6
        for (d in 0 until dotCount) {
            val dotX = (sin((triggerInt + d * 23).toDouble()) * 0.5 + 0.5) * width
            val dotY = (cos((triggerInt + d * 29).toDouble()) * 0.5 + 0.5) * height
            drawCircle(
                color = factionColor.copy(alpha = 0.25f),
                radius = 1f * densityVal,
                center = Offset(dotX.toFloat(), dotY.toFloat())
            )
        }
    }
}

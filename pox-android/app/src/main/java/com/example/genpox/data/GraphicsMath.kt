package com.example.genpox.data

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.abs

object GraphicsMath {

    // --- Simple Coordinate Representation for Non-UI Layer ---
    data class Point2D(val x: Float, val y: Float)
    data class Point3D(val x: Double, val y: Double, val z: Double)

    // ==========================================
    // 1. NODE CRYSTAL CANVAS MATH
    // ==========================================
    
    fun calculateLunarBaselineMod(lunarPhase: Double): Float {
        return 1.0f + lunarPhase.toFloat() * 0.15f
    }

    fun calculateSkewness(widths: List<Float>): Float {
        if (widths.isEmpty()) return 0f
        val avg = widths.average().toFloat()
        val variance = widths.map { (it - avg).pow(2) }.average().toFloat()
        return sqrt(variance)
    }

    fun calculateWaveAmplitude(skewness: Float): Float {
        return 0.5f + skewness * 12.0f
    }

    fun calculateWaveFrequency(totalDepth: Float): Float {
        return 0.05f + totalDepth * 0.08f
    }

    fun calculateCrystalVertices(
        cx: Float,
        cy: Float,
        r: Float,
        subWidthG: Float,
        subWidthT: Float,
        subWidthC: Float,
        subWidthA: Float
    ): List<Point2D> {
        return listOf(
            Point2D(cx, cy - r * subWidthG),   // ptG
            Point2D(cx + r * subWidthT, cy),   // ptT
            Point2D(cx, cy + r * subWidthC),   // ptC
            Point2D(cx - r * subWidthA, cy)    // ptA
        )
    }

    // ==========================================
    // 2. HOLOGRAPHIC HELIX REACTOR MATH
    // ==========================================

    data class HelixStrandMetrics(
        val x1: Float,
        val y: Float,
        val alpha1: Float,
        val radius1Dp: Float,
        val x2: Float,
        val alpha2: Float,
        val radius2Dp: Float,
        val barAlpha: Float
    )

    fun calculateHelixStrand(
        index: Int,
        numStrands: Int,
        w: Float,
        h: Float,
        rotationPhase: Float,
        pulseScale: Float,
        isGlitching: Boolean,
        glitchProgress: Float,
        isAnomaly: Boolean,
        glitchThresholdPx: Float,
        glitchOffsetPx: Float
    ): HelixStrandMetrics {
        val cx = w / 2f
        val t = index.toFloat() / (numStrands - 1).coerceAtLeast(1)
        val cyPos = h * 0.12f + t * h * 0.76f

        val nodeAngle = t * 3.5f * Math.PI.toFloat() + rotationPhase
        val helixRadius = w * 0.25f * pulseScale

        val x1 = cx + helixRadius * cos(nodeAngle.toDouble()).toFloat()
        val z1 = helixRadius * sin(nodeAngle.toDouble()).toFloat()

        val x2 = cx + helixRadius * cos((nodeAngle + Math.PI.toFloat()).toDouble()).toFloat()
        val z2 = helixRadius * sin((nodeAngle + Math.PI.toFloat()).toDouble()).toFloat()

        var gx1 = x1
        var gx2 = x2
        
        val glitchCenterY = glitchProgress * h
        if (isGlitching && abs(cyPos - glitchCenterY) < glitchThresholdPx) {
            val offsetVal = (sin(cyPos * 0.1f) * glitchOffsetPx)
            gx1 += offsetVal
            gx2 -= offsetVal
        }

        val alpha1 = ((z1 + helixRadius) / (2f * helixRadius) * 0.6f + 0.4f).coerceIn(0.1f, 1.0f)
        val alpha2 = ((z2 + helixRadius) / (2f * helixRadius) * 0.6f + 0.4f).coerceIn(0.1f, 1.0f)

        val radius1Dp = ((z1 + helixRadius) / (2f * helixRadius) * 2.5f + 1.8f)
        val radius2Dp = ((z2 + helixRadius) / (2f * helixRadius) * 2.5f + 1.8f)

        val barAlpha = ((alpha1 + alpha2) / 2f * 0.4f).coerceIn(0.1f, 0.7f)

        return HelixStrandMetrics(
            x1 = gx1,
            y = cyPos,
            alpha1 = alpha1,
            radius1Dp = radius1Dp,
            x2 = gx2,
            alpha2 = alpha2,
            radius2Dp = radius2Dp,
            barAlpha = barAlpha
        )
    }

    // ==========================================
    // 3. 3D CRYSTAL CORE BLUEPRINT MATH
    // ==========================================

    fun generate3DBlueprintVertices(crystalRadius: Double, rotationAngle: Double): List<Point3D> {
        val rotationVal = rotationAngle * 1.5
        return listOf(
            Point3D(0.0, crystalRadius, 0.0), // 0: Top
            Point3D(0.0, -crystalRadius, 0.0), // 1: Bottom
            Point3D(crystalRadius * cos(rotationVal), 0.0, crystalRadius * sin(rotationVal)), // 2
            Point3D(crystalRadius * cos(rotationVal + Math.PI/2), 0.0, crystalRadius * sin(rotationVal + Math.PI/2)), // 3
            Point3D(crystalRadius * cos(rotationVal + Math.PI), 0.0, crystalRadius * sin(rotationVal + Math.PI)), // 4
            Point3D(crystalRadius * cos(rotationVal + 3*Math.PI/2), 0.0, crystalRadius * sin(rotationVal + 3*Math.PI/2)) // 5
        )
    }

    fun project3DBlueprintPoint(
        pt: Point3D,
        tiltVal: Double,
        centerX: Double,
        centerY: Double
    ): Point2D {
        val yt = pt.y * cos(tiltVal) - pt.z * sin(tiltVal)
        return Point2D((centerX + pt.x).toFloat(), (centerY - yt).toFloat())
    }

    // ==========================================
    // 4. CIRCULAR BIO-RESONANCE OSCILLOSCOPE MATH
    // ==========================================

    fun calculateOscilloscopeHeightDp(
        angleDeg: Int,
        slotGene: String?,
        targetSegment: String,
        wavePhase: Float
    ): Float {
        val isAnom = slotGene?.let { WaveMath.isAnomalousGene(it) } ?: false
        val isCoherent = slotGene == targetSegment

        return when {
            slotGene == null -> {
                val ripple = sin(angleDeg * 2f * Math.PI.toFloat() / 180f + wavePhase * 1.5f)
                1.5f + 1.0f * ripple
            }
            isAnom -> {
                val ripple = sin(angleDeg * 40f * Math.PI.toFloat() / 180f + wavePhase * 25f)
                val noise = abs(sin(angleDeg * 123f))
                6.0f + 14.0f * ripple * noise
            }
            isCoherent -> {
                val ripple = sin(angleDeg * 6f * Math.PI.toFloat() / 180f + wavePhase * 3f)
                7.0f + 5.0f * ripple
            }
            else -> {
                // Mismatch
                val ripple = sin(angleDeg * 25f * Math.PI.toFloat() / 180f + wavePhase * 12f)
                val noise = abs(sin(angleDeg * 77f))
                5.0f + 7.0f * ripple * noise
            }
        }
    }

    // ==========================================
    // 5. ORRERY CELESTIAL AND DOME PROJECTION MATH
    // ==========================================

    data class OrreryPlanetCoordinates(
        val lambda: Double,
        val declination: Double,
        val hourAngle: Double
    )

    fun calculateOrreryCoordinates(
        planetName: String,
        timeMs: Long,
        userLng: Double,
        xE: Double,
        yE: Double
    ): OrreryPlanetCoordinates {
        val (x, y) = WaveMath.getHeliocentricPosition(planetName, timeMs)
        val lambda = if (planetName.uppercase() == "EARTH") {
            kotlin.math.atan2(-yE, -xE) // Sun Proxy
        } else {
            kotlin.math.atan2(y - yE, x - xE)
        }
        val declination = kotlin.math.asin(0.3978 * sin(lambda))
        val phiRot = (2.0 * Math.PI * ((timeMs % 86400000L).toDouble() / 86400000.0 + 0.5)) % (2.0 * Math.PI)
        val lambdaSun = kotlin.math.atan2(-yE, -xE)
        val lngOffsetRad = Math.toRadians(userLng)
        val hourAngle = ((phiRot + lngOffsetRad + lambdaSun - lambda) % (2.0 * Math.PI) + 2.0 * Math.PI) % (2.0 * Math.PI)
        
        return OrreryPlanetCoordinates(lambda, declination, hourAngle)
    }

    fun calculateHourAngleSeparation(ha1: Double, ha2: Double): Double {
        var diff = kotlin.math.abs(ha1 - ha2)
        if (diff > Math.PI) {
            diff = 2.0 * Math.PI - diff
        }
        return Math.toDegrees(diff)
    }

    data class OrreryProjectionMetrics(
        val centerY: Float,
        val projectedRadius: Float,
        val dx: Float,
        val dy: Float,
        val isBelowHorizon: Boolean
    )

    fun projectOrreryPlanet(
        cx: Float,
        cy: Float,
        maxRadius: Float,
        radiusScale: Float,
        latRad: Double,
        declination: Double,
        hourAngle: Double
    ): OrreryProjectionMetrics {
        val r = maxRadius * radiusScale
        val cyShift = cy - r * sin(latRad).toFloat() * sin(declination).toFloat()
        val rp = (r * cos(latRad).toFloat() * cos(declination).toFloat()).coerceAtLeast(r * 0.2f)
        val dx = cx + rp * sin(hourAngle).toFloat()
        val dy = cyShift - rp * cos(hourAngle).toFloat()
        val isBelowHorizon = dy > cy

        return OrreryProjectionMetrics(
            centerY = cyShift,
            projectedRadius = rp,
            dx = dx,
            dy = dy,
            isBelowHorizon = isBelowHorizon
        )
    }
}

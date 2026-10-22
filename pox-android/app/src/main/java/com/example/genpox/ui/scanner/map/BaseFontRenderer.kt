package com.example.genpox.ui.scanner.map

import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.example.genpox.ui.main.PoxBaseFont

private val cosTable36 = FloatArray(37) { i -> kotlin.math.cos((i * 2.0 * Math.PI) / 36.0).toFloat() }
private val sinTable36 = FloatArray(37) { i -> kotlin.math.sin((i * 2.0 * Math.PI) / 36.0).toFloat() }
private val sharedRingPathPool = List(32) { Path() }

class ProjectedFont(
    var font: PoxBaseFont = PoxBaseFont("", 0.0, 0.0, 'A', 0L, 90000L, 500.0, false),
    var fx: Float = 0f,
    var fy: Float = 0f,
    var maxRadiusPx: Float = 0f,
    var peakHeightPx: Float = 0f,
    var epsilon: Float = 0f,
    var kLobe: Int = 0,
    var phiOffset: Double = 0.0,
    var baseColor: Color = Color.Cyan,
    var flickerMultiplier: Float = 1f,
    var animScale: Float = 1f,
) {
    fun set(
        font: PoxBaseFont,
        fx: Float,
        fy: Float,
        maxRadiusPx: Float,
        peakHeightPx: Float,
        epsilon: Float,
        kLobe: Int,
        phiOffset: Double,
        baseColor: Color,
        flickerMultiplier: Float,
        animScale: Float,
    ) {
        this.font = font
        this.fx = fx
        this.fy = fy
        this.maxRadiusPx = maxRadiusPx
        this.peakHeightPx = peakHeightPx
        this.epsilon = epsilon
        this.kLobe = kLobe
        this.phiOffset = phiOffset
        this.baseColor = baseColor
        this.flickerMultiplier = flickerMultiplier
        this.animScale = animScale
    }
}

fun DrawScope.drawBaseFontRingClipped(
    group: List<ProjectedFont>,
    ringIndex: Int,
    uRing: Float,
    ringAlpha: Float,
    phiOffset: Double,
    rotRad: Double,
    tiltYScale: Float,
    sinTilt: Float,
    baseColor: Color,
    clipIdx: Int,
    scale: Double,
) {
    if (clipIdx >= group.size) {
        val pFont = group[ringIndex]
        val peakHeightPx = 120.dp.toPx() * pFont.animScale
        val maxRadiusPx = ((pFont.font.maxRadiusFeet / 364000.0) * scale).toFloat() * pFont.animScale
        val ringZ = uRing * peakHeightPx
        val ringRadius = maxRadiusPx * kotlin.math.sqrt(1.0f - uRing * uRing)
        if (ringRadius < 0.1f) return

        val ringPath = sharedRingPathPool[0]
        ringPath.reset()
        val cosRot = kotlin.math.cos(rotRad).toFloat()
        val sinRot = kotlin.math.sin(rotRad).toFloat()

        for (i in 0..36) {
            val theta = (i * 2.0 * Math.PI) / 36.0
            val lobeMod = (1.0f + pFont.epsilon * kotlin.math.cos(pFont.kLobe * theta + pFont.phiOffset).toFloat())
            val rBase = ringRadius * lobeMod
            val cosT = cosTable36[i]
            val sinT = sinTable36[i]
            val cosRotated = cosT * cosRot + sinT * sinRot
            val sinRotated = sinT * cosRot - cosT * sinRot
            val px = pFont.fx + rBase * cosRotated
            val py = pFont.fy - rBase * sinRotated * tiltYScale - ringZ * sinTilt
            if (i == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
        }
        ringPath.close()

        drawPath(
            path = ringPath,
            color = baseColor.copy(alpha = ringAlpha),
            style = Stroke(width = 0.8.dp.toPx()),
        )
        return
    }

    if (clipIdx == ringIndex) {
        drawBaseFontRingClipped(
            group, ringIndex, uRing, ringAlpha, phiOffset,
            rotRad, tiltYScale, sinTilt, baseColor, clipIdx + 1, scale,
        )
    } else {
        val currentFont = group[ringIndex]
        val otherFont = group[clipIdx]
        val dx = currentFont.fx - otherFont.fx
        val dy = currentFont.fy - otherFont.fy
        val distSq = dx * dx + dy * dy
        val maxDist = currentFont.maxRadiusPx + otherFont.maxRadiusPx

        if (distSq >= maxDist * maxDist) {
            drawBaseFontRingClipped(
                group, ringIndex, uRing, ringAlpha, phiOffset,
                rotRad, tiltYScale, sinTilt, baseColor, clipIdx + 1, scale,
            )
        } else {
            val peakHeightPx = 120.dp.toPx() * otherFont.animScale
            val maxRadiusPx = ((otherFont.font.maxRadiusFeet / 364000.0) * scale).toFloat() * otherFont.animScale
            val ringZ = uRing * peakHeightPx
            val ringRadius = maxRadiusPx * kotlin.math.sqrt(1.0f - uRing * uRing)

            val poolIdx = (clipIdx + 1).coerceIn(1, sharedRingPathPool.size - 1)
            val otherRingPath = sharedRingPathPool[poolIdx]
            otherRingPath.reset()
            if (ringRadius >= 0.1f) {
                val cosRot = kotlin.math.cos(rotRad).toFloat()
                val sinRot = kotlin.math.sin(rotRad).toFloat()
                for (i in 0..36) {
                    val theta = (i * 2.0 * Math.PI) / 36.0
                    val lobeMod = (1.0f + otherFont.epsilon * kotlin.math.cos(otherFont.kLobe * theta + otherFont.phiOffset).toFloat())
                    val rBase = ringRadius * lobeMod
                    val cosT = cosTable36[i]
                    val sinT = sinTable36[i]
                    val cosRotated = cosT * cosRot + sinT * sinRot
                    val sinRotated = sinT * cosRot - cosT * sinRot
                    val px = otherFont.fx + rBase * cosRotated
                    val py = otherFont.fy - rBase * sinRotated * tiltYScale - ringZ * sinTilt
                    if (i == 0) otherRingPath.moveTo(px, py) else otherRingPath.lineTo(px, py)
                }
                otherRingPath.close()
            }

            clipPath(otherRingPath, clipOp = ClipOp.Difference) {
                drawBaseFontRingClipped(
                    group, ringIndex, uRing, ringAlpha, phiOffset,
                    rotRad, tiltYScale, sinTilt, baseColor, clipIdx + 1, scale,
                )
            }
        }
    }
}

fun DrawScope.drawBaseFontBoundaryClipped(
    group: List<ProjectedFont>,
    boundaryIndex: Int,
    phiOffset: Double,
    rotRad: Double,
    tiltYScale: Float,
    baseColor: Color,
    clipIdx: Int,
    scale: Double,
) {
    if (clipIdx >= group.size) {
        val pFont = group[boundaryIndex]
        val maxRadiusPx = ((pFont.font.maxRadiusFeet / 364000.0) * scale).toFloat() * pFont.animScale
        if (maxRadiusPx < 0.1f) return

        val boundaryPath = sharedRingPathPool[0]
        boundaryPath.reset()
        val cosRot = kotlin.math.cos(rotRad).toFloat()
        val sinRot = kotlin.math.sin(rotRad).toFloat()

        for (i in 0..36) {
            val theta = (i * 2.0 * Math.PI) / 36.0
            val lobeMod = (1.0f + pFont.epsilon * kotlin.math.cos(pFont.kLobe * theta + pFont.phiOffset).toFloat())
            val rBase = maxRadiusPx * lobeMod
            val cosT = cosTable36[i]
            val sinT = sinTable36[i]
            val cosRotated = cosT * cosRot + sinT * sinRot
            val sinRotated = sinT * cosRot - cosT * sinRot
            val px = pFont.fx + rBase * cosRotated
            val py = pFont.fy - rBase * sinRotated * tiltYScale
            if (i == 0) boundaryPath.moveTo(px, py) else boundaryPath.lineTo(px, py)
        }
        boundaryPath.close()

        drawPath(
            path = boundaryPath,
            color = baseColor.copy(alpha = 0.12f * pFont.flickerMultiplier),
            style = Stroke(width = 0.8.dp.toPx()),
        )
        return
    }

    if (clipIdx == boundaryIndex) {
        drawBaseFontBoundaryClipped(
            group,
            boundaryIndex,
            phiOffset,
            rotRad,
            tiltYScale,
            baseColor,
            clipIdx + 1,
            scale,
        )
    } else {
        val currentFont = group[boundaryIndex]
        val otherFont = group[clipIdx]
        val dx = currentFont.fx - otherFont.fx
        val dy = currentFont.fy - otherFont.fy
        val distSq = dx * dx + dy * dy
        val maxDist = currentFont.maxRadiusPx + otherFont.maxRadiusPx

        if (distSq >= maxDist * maxDist) {
            drawBaseFontBoundaryClipped(
                group,
                boundaryIndex,
                phiOffset,
                rotRad,
                tiltYScale,
                baseColor,
                clipIdx + 1,
                scale,
            )
        } else {
            val maxRadiusPx = ((otherFont.font.maxRadiusFeet / 364000.0) * scale).toFloat() * otherFont.animScale
            val poolIdx = (clipIdx + 1).coerceIn(1, sharedRingPathPool.size - 1)
            val otherBoundaryPath = sharedRingPathPool[poolIdx]
            otherBoundaryPath.reset()
            if (maxRadiusPx >= 0.1f) {
                val cosRot = kotlin.math.cos(rotRad).toFloat()
                val sinRot = kotlin.math.sin(rotRad).toFloat()
                for (i in 0..36) {
                    val theta = (i * 2.0 * Math.PI) / 36.0
                    val lobeMod = (1.0f + otherFont.epsilon * kotlin.math.cos(otherFont.kLobe * theta + otherFont.phiOffset).toFloat())
                    val rBase = maxRadiusPx * lobeMod
                    val cosT = cosTable36[i]
                    val sinT = sinTable36[i]
                    val cosRotated = cosT * cosRot + sinT * sinRot
                    val sinRotated = sinT * cosRot - cosT * sinRot
                    val px = otherFont.fx + rBase * cosRotated
                    val py = otherFont.fy - rBase * sinRotated * tiltYScale
                    if (i == 0) otherBoundaryPath.moveTo(px, py) else otherBoundaryPath.lineTo(px, py)
                }
                otherBoundaryPath.close()
            }

            clipPath(otherBoundaryPath, clipOp = ClipOp.Difference) {
                drawBaseFontBoundaryClipped(
                    group,
                    boundaryIndex,
                    phiOffset,
                    rotRad,
                    tiltYScale,
                    baseColor,
                    clipIdx + 1,
                    scale,
                )
            }
        }
    }
}

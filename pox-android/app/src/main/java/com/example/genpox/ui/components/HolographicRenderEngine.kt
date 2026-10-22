package com.example.genpox.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.genpox.data.Creature
import com.example.genpox.data.GraphicsMath
import com.example.genpox.data.WaveMath
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.RectF as AndroidRectF

object HolographicRenderEngine {

    private val reusableAndroidPath = AndroidPath()
    private val reusableAndroidRectF = AndroidRectF()
    private val reusableGridPath = Path()
    private val reusableDatePaint = android.graphics.Paint()

    /**
     * Draws concentric helper and grid circles.
     */
    fun DrawScope.drawConcentricGridCircles(
        cx: Float,
        cy: Float,
        rPx: Float,
        color: Color,
        density: Density,
    ) {
        val strokeWidth = with(density) { 1.dp.toPx() }
        // Outer/inner circular borders of the track
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = rPx * 0.86f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth),
        )
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = rPx * 1.04f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth),
        )
        // Central core glowing outline
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = rPx * 0.4f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth),
        )
    }

    /**
     * Draws the radar sweep lines and active trailing indicators.
     */
    fun DrawScope.drawRadarSweep(
        cx: Float,
        cy: Float,
        rPx: Float,
        sweepAngleRad: Float,
        activeColor: Color,
        activeColorDim: Color,
        density: Density,
    ) {
        val numSweepTailLines = 6
        for (t in 0 until numSweepTailLines) {
            val tailAngleRad = sweepAngleRad - t * 0.08f // trailing angles
            val tailAlpha = (1f - t.toFloat() / numSweepTailLines) * 0.25f

            // Centralize with GraphicsMath
            val tailPt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 0.4f, tailAngleRad.toDouble())

            drawLine(
                color = activeColorDim.copy(alpha = tailAlpha),
                start = Offset(cx, cy),
                end = Offset(tailPt.x, tailPt.y),
                strokeWidth = with(density) { (if (t == 0) 1.5.dp.toPx() else 1.dp.toPx()) },
            )
        }

        // Rotating sonar sweep cursor dot
        val cursorPt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 0.4f, sweepAngleRad.toDouble())
        drawCircle(
            color = activeColor,
            radius = with(density) { 2.5.dp.toPx() },
            center = Offset(cursorPt.x, cursorPt.y),
        )
    }

    /**
     * Draws the procedural oscilloscope radial wave.
     */
    fun DrawScope.drawOscilloscopeWave(
        cx: Float,
        cy: Float,
        rPx: Float,
        splicerSlots: List<String?>,
        targetSequence: String,
        currentSpin: Float,
        wavePhase: Float,
        activeColor: Color,
        activeColorDim: Color,
        density: Density,
    ) {
        val strokeWidth = with(density) { 1.dp.toPx() }
        val glowStrokeWidth = with(density) { 1.5.dp.toPx() }
        val currentSpinDeg = currentSpin * 180f / Math.PI.toFloat()

        for (a in 0 until 360 step 4) {
            // Map the screen-space angle back to the rotating slot index
            var normalizedA = (a + 90f - currentSpinDeg) % 360f
            if (normalizedA < 0f) normalizedA += 360f
            val slotIdx = (normalizedA / 45f).toInt().coerceIn(0, 7)
            val slotGene = splicerSlots.getOrNull(slotIdx)

            val startIdx = slotIdx * 8
            val endIdx = (slotIdx + 1) * 8
            val requiredSegment = if (startIdx < targetSequence.length) {
                targetSequence.substring(startIdx, endIdx.coerceAtMost(targetSequence.length))
            } else {
                ""
            }

            val isAnom = slotGene?.let { WaveMath.isAnomalousGene(it) } ?: false
            val isCoherent = slotGene == requiredSegment

            val waveColor = when {
                slotGene == null -> activeColorDim.copy(alpha = 0.4f)
                isAnom -> Color(0xFFA855F7).copy(alpha = 0.8f)
                isCoherent -> activeColor.copy(alpha = 0.8f)
                else -> Color.Red.copy(alpha = 0.8f)
            }

            val angleRad = a * Math.PI / 180.0

            // Calculate dynamic wave height based on slot state using GraphicsMath
            var h = with(density) {
                GraphicsMath.calculateOscilloscopeHeightDp(a, slotGene, requiredSegment, wavePhase).dp.toPx()
            }

            // Calculate radar sweep excitation effect
            val sweepAngleDeg = (wavePhase * 180.0 / Math.PI) % 360.0
            var diffDeg = (sweepAngleDeg - a) % 360.0
            if (diffDeg < 0) diffDeg += 360.0

            val isExcited = diffDeg <= 20.0
            if (isExcited) {
                val excitationFactor = 1f + ((20.0 - diffDeg) / 20.0).toFloat() * 1.1f
                h *= excitationFactor
            }

            // Centralize coordinates with GraphicsMath
            val startPt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 0.42f, angleRad)
            val endPt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 0.42f + h, angleRad)

            drawLine(
                color = waveColor,
                start = Offset(startPt.x, startPt.y),
                end = Offset(endPt.x, endPt.y),
                strokeWidth = strokeWidth,
            )

            // Draw white glowing peak highlights for excited states
            if (isExcited) {
                val glowAlpha = ((20.0 - diffDeg) / 20.0).toFloat() * 0.7f
                drawLine(
                    color = Color.White.copy(alpha = glowAlpha),
                    start = Offset(startPt.x, startPt.y),
                    end = Offset(endPt.x, endPt.y),
                    strokeWidth = glowStrokeWidth,
                )
            }
        }
    }

    /**
     * Draws the circular genome arcs, cell elements, dividers, ticks, and labels.
     */
    fun DrawScope.drawCircularGenomeSlots(
        cx: Float,
        cy: Float,
        rPx: Float,
        splicerSlots: List<String?>,
        targetSequence: String,
        activeSlotSelection: Int?,
        currentSpin: Float,
        pulseAlpha: Float,
        isCustomScaffoldMode: Boolean,
        activeColor: Color,
        activeColorDim: Color,
        density: Density,
        labelPaint: AndroidPaint,
        geneTextPaint: AndroidPaint,
        getBaseColor: (Char) -> Color,
    ) {
        val currentSpinDeg = currentSpin * 180f / Math.PI.toFloat()
        val tickStrokeWidth = with(density) { 1.dp.toPx() }
        val arcLabelVOffset = with(density) { 3.dp.toPx() }

        for (i in 0 until 8) {
            val slotGene = splicerSlots.getOrNull(i)
            val startIdx = i * 8
            val endIdx = (i + 1) * 8
            val requiredSegment = if (startIdx < targetSequence.length) {
                targetSequence.substring(startIdx, endIdx.coerceAtMost(targetSequence.length))
            } else {
                ""
            }
            val isAnom = slotGene?.let { WaveMath.isAnomalousGene(it) } ?: false
            val isCoherent = slotGene == requiredSegment

            val statusColor = when {
                slotGene == null -> Color(0xFF334155).copy(alpha = 0.5f) // Splicer border color
                isAnom -> Color(0xFFA855F7)
                isCustomScaffoldMode -> Color(0xFF22D3EE)
                isCoherent -> activeColor
                else -> Color.Red
            }

            // Calculate arc angles in degrees
            val startAngle = (i * 45 - 90 + 2f) + currentSpinDeg
            val sweepAngle = 41f

            // Draw pulsing background under the selected segment
            if (activeSlotSelection == i) {
                drawArc(
                    color = statusColor.copy(alpha = pulseAlpha * 0.25f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - rPx * 0.95f, cy - rPx * 0.95f),
                    size = Size(rPx * 1.9f, rPx * 1.9f),
                    style = Stroke(width = rPx * 0.18f),
                )
            }

            // Draw the 8 individual color-coded base cells
            val segmentGeneString = slotGene ?: requiredSegment
            for (b in 0 until 8) {
                val baseChar = segmentGeneString.getOrNull(b) ?: '-'
                val baseColor = getBaseColor(baseChar)

                val baseStartAngle = startAngle + b * 5.125f
                val baseSweepAngle = 4.6f // leaves a 0.525 degree gap

                drawArc(
                    color = baseColor.copy(alpha = if (slotGene != null) 0.6f else 0.15f),
                    startAngle = baseStartAngle,
                    sweepAngle = baseSweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - rPx * 0.95f, cy - rPx * 0.95f),
                    size = Size(rPx * 1.9f, rPx * 1.9f),
                    style = Stroke(width = rPx * 0.18f),
                )
            }

            // Draw outer boundary highlight arc
            drawArc(
                color = if (slotGene == null) statusColor else statusColor.copy(alpha = 0.8f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(cx - rPx * 1.04f, cy - rPx * 1.04f),
                size = Size(rPx * 2.08f, rPx * 2.08f),
                style = Stroke(width = with(density) { (if (activeSlotSelection == i) 3.5.dp.toPx() else 1.5.dp.toPx()) }),
            )

            // Draw inner boundary highlight arc for selected slot
            if (activeSlotSelection == i) {
                drawArc(
                    color = statusColor.copy(alpha = 0.8f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - rPx * 0.86f, cy - rPx * 0.86f),
                    size = Size(rPx * 1.72f, rPx * 1.72f),
                    style = Stroke(width = with(density) { 2.dp.toPx() }),
                )
            }

            // Draw segment division spoke (radial divider) at slot boundaries
            val divisionAngleRad = ((i * 45 - 90) * Math.PI / 180.0) + currentSpin

            // Centralize position calculations in GraphicsMath
            val dsPt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 0.86f, divisionAngleRad)
            val dePt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 1.04f, divisionAngleRad)
            drawLine(
                color = activeColorDim.copy(alpha = 0.25f),
                start = Offset(dsPt.x, dsPt.y),
                end = Offset(dePt.x, dePt.y),
                strokeWidth = tickStrokeWidth,
            )

            // Draw outer coordinate tick marks
            val tsPt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 1.04f, divisionAngleRad)
            val tePt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 1.10f, divisionAngleRad)
            drawLine(
                color = activeColorDim.copy(alpha = 0.4f),
                start = Offset(tsPt.x, tsPt.y),
                end = Offset(tePt.x, tePt.y),
                strokeWidth = tickStrokeWidth,
            )

            // Draw outer base-pair coordinate label (0, 8, 16 ... 56 bp)
            val labelPt = GraphicsMath.calculateCircularPoint(cx, cy, rPx * 1.20f, divisionAngleRad)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "${i * 8} bp",
                    labelPt.x,
                    labelPt.y + arcLabelVOffset,
                    labelPaint,
                )
            }

            // Draw gene sequence text along the arc segment path (clockwise direction)
            val textRadius = rPx * 0.95f
            reusableAndroidPath.reset()
            reusableAndroidRectF.set(cx - textRadius, cy - textRadius, cx + textRadius, cy + textRadius)
            reusableAndroidPath.addArc(reusableAndroidRectF, startAngle, sweepAngle)

            val isTargetAnom = WaveMath.isAnomalousGene(requiredSegment)
            val textPaintColor = when {
                slotGene == null -> {
                    if (isCustomScaffoldMode) {
                        Color(0xFF22D3EE).copy(alpha = 0.3f)
                    } else if (isTargetAnom) {
                        Color(0xFFA855F7).copy(alpha = 0.22f)
                    } else {
                        activeColorDim.copy(alpha = 0.22f)
                    }
                }
                isAnom -> Color(0xFFC084FC)
                isCustomScaffoldMode -> Color(0xFF22D3EE)
                isCoherent -> activeColor
                else -> Color.Red
            }
            geneTextPaint.color = textPaintColor.toArgb()

            val textToDraw = slotGene ?: if (isCustomScaffoldMode) "[ ANY ]" else requiredSegment
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawTextOnPath(
                    textToDraw,
                    reusableAndroidPath,
                    0f, // hOffset - centered automatically by Paint.Align.CENTER
                    arcLabelVOffset, // vOffset
                    geneTextPaint,
                )
            }
        }
    }

    /**
     * Draws the circular genome map for active splicing progress.
     */
    fun DrawScope.drawSplicingProgressGenomeSlots(
        cx: Float,
        cy: Float,
        rPx: Float,
        sequencedBasesCount: Int,
        newlySplicedCreature: Creature?,
        currentSpin: Float,
        activeColor: Color,
        activeColorDim: Color,
        density: Density,
        getBaseColor: (Char) -> Color,
    ) {
        val strokeWidth = with(density) { 1.dp.toPx() }
        val outlineStrokeWidth = with(density) { 1.5.dp.toPx() }

        // Draw outer/inner circular borders of the track
        drawCircle(
            color = activeColorDim.copy(alpha = 0.2f),
            radius = rPx * 0.86f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth),
        )
        drawCircle(
            color = activeColorDim.copy(alpha = 0.2f),
            radius = rPx * 1.04f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth),
        )

        // Draw 8 Arc segments (each with 8 bases)
        val currentSpinDeg = currentSpin * 180f / Math.PI.toFloat()
        val fullDnaSeq = newlySplicedCreature?.sequence ?: "A".repeat(64)
        for (i in 0 until 8) {
            val startAngle = (i * 45 - 90 + 2f) + currentSpinDeg
            val sweepAngle = 41f

            // Draw the 8 individual color-coded base cells
            for (b in 0 until 8) {
                val baseIdx = i * 8 + b
                val baseChar = fullDnaSeq.getOrNull(baseIdx) ?: 'A'
                val isBaseSequenced = baseIdx < sequencedBasesCount
                val baseColor = if (isBaseSequenced) getBaseColor(baseChar) else Color.DarkGray
                val baseAlpha = if (isBaseSequenced) 0.80f else 0.10f

                val baseStartAngle = startAngle + b * 5.125f
                val baseSweepAngle = 4.6f

                drawArc(
                    color = baseColor.copy(alpha = baseAlpha),
                    startAngle = baseStartAngle,
                    sweepAngle = baseSweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - rPx * 0.95f, cy - rPx * 0.95f),
                    size = Size(rPx * 1.9f, rPx * 1.9f),
                    style = Stroke(width = rPx * 0.18f),
                )
            }

            // Outer segment boundary outline
            val sequencedBasesInSlot = (sequencedBasesCount - i * 8).coerceIn(0, 8)
            val outlineColor = if (sequencedBasesInSlot == 8) activeColor else activeColorDim.copy(alpha = 0.2f)
            drawArc(
                color = outlineColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(cx - rPx * 1.04f, cy - rPx * 1.04f),
                size = Size(rPx * 2.08f, rPx * 2.08f),
                style = Stroke(width = outlineStrokeWidth),
            )
        }
    }

    /**
     * Draws the planet transit sky dome diagram (orrery).
     */
    fun DrawScope.drawPlanetTransitOrrery(
        w: Float,
        h: Float,
        cx: Float,
        cy: Float,
        maxRadius: Float,
        cy1: Float,
        rp1: Float,
        dx1: Float,
        dy1: Float,
        isBelowHorizon1: Boolean,
        isSel1: Boolean,
        p1Color: Color,
        cy2: Float,
        rp2: Float,
        dx2: Float,
        dy2: Float,
        isBelowHorizon2: Boolean,
        isSel2: Boolean,
        p2Color: Color,
        dateText: String,
        alignmentState: WaveMath.AlignmentState,
        density: Density,
    ) {
        clipRect {
            val r1 = maxRadius * 0.78f
            val r2 = maxRadius * 1.0f

            // 1. Horizon Line
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(w * 0.02f, cy),
                end = Offset(w * 0.98f, cy),
                strokeWidth = with(density) { 1.5.dp.toPx() },
            )

            // Horizon Date Watermark
            reusableDatePaint.reset()
            reusableDatePaint.color = Color.White.copy(alpha = 0.15f).toArgb()
            reusableDatePaint.textSize = with(density) { 10.dp.toPx() }
            reusableDatePaint.typeface = android.graphics.Typeface.MONOSPACE
            reusableDatePaint.textAlign = android.graphics.Paint.Align.CENTER

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    dateText,
                    cx,
                    cy + with(density) { 14.dp.toPx() },
                    reusableDatePaint,
                )
            }

            // 2. Draw reference elevation grid arcs (concentric sky domes)
            val gridAngles = listOf(0.4f, 0.7f, 1.0f)
            gridAngles.forEach { ratio ->
                val rGrid = maxRadius * ratio
                reusableGridPath.reset()
                val oval = Rect(cx - rGrid, cy - rGrid, cx + rGrid, cy + rGrid)
                reusableGridPath.addArc(oval, 180f, 180f)

                drawPath(
                    path = reusableGridPath,
                    color = Color(0x8000FF41).copy(alpha = 0.15f), // CyberGreenDim copy
                    style = Stroke(
                        width = with(density) { 0.8.dp.toPx() },
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
                    ),
                )
            }

            // Vertical Meridian line
            drawLine(
                color = Color(0x8000FF41).copy(alpha = 0.15f), // CyberGreenDim copy
                start = Offset(cx, cy),
                end = Offset(cx, cy - maxRadius),
                strokeWidth = with(density) { 0.8.dp.toPx() },
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f),
            )

            // 3. Draw Clock Face Elements
            val angleColor = when (alignmentState) {
                WaveMath.AlignmentState.CONJUNCTION -> Color(0xFF00FF66) // Cyber Green
                WaveMath.AlignmentState.OPPOSITION -> Color(0xFFFF1744) // Cyber Red
                else -> Color(0xFFFFEA00) // Cyber Yellow
            }

            // Clock line 1
            drawLine(
                color = angleColor.copy(alpha = 0.5f),
                start = Offset(cx, cy),
                end = Offset(dx1, dy1),
                strokeWidth = with(density) { 1.2.dp.toPx() },
            )

            // Clock line 2
            drawLine(
                color = angleColor.copy(alpha = 0.5f),
                start = Offset(cx, cy),
                end = Offset(dx2, dy2),
                strokeWidth = with(density) { 1.2.dp.toPx() },
            )

            // Clock wedge / sector of separation
            val deg1 = Math.toDegrees(kotlin.math.atan2((dy1 - cy).toDouble(), (dx1 - cx).toDouble()))
            val deg2 = Math.toDegrees(kotlin.math.atan2((dy2 - cy).toDouble(), (dx2 - cx).toDouble()))
            var sweep = (deg2 - deg1) % 360.0
            if (sweep > 180.0) {
                sweep -= 360.0
            } else if (sweep < -180.0) {
                sweep += 360.0
            }

            val rWedge = maxRadius * 0.25f

            drawArc(
                color = angleColor.copy(alpha = 0.08f),
                startAngle = deg1.toFloat(),
                sweepAngle = sweep.toFloat(),
                useCenter = true,
                size = Size(rWedge * 2f, rWedge * 2f),
                topLeft = Offset(cx - rWedge, cy - rWedge),
            )
            drawArc(
                color = angleColor.copy(alpha = 0.25f),
                startAngle = deg1.toFloat(),
                sweepAngle = sweep.toFloat(),
                useCenter = false,
                size = Size(rWedge * 2f, rWedge * 2f),
                topLeft = Offset(cx - rWedge, cy - rWedge),
                style = Stroke(width = with(density) { 1.dp.toPx() }),
            )

            // 4. Draw Planet 1 Path
            // Draw above-horizon (solid Cyber Cyan)
            clipRect(top = 0f, bottom = cy) {
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = rp1,
                    center = Offset(cx, cy1),
                    style = Stroke(width = with(density) { (if (isSel1) 2.dp.toPx() else 1.dp.toPx()) }),
                )
            }

            // Draw below-horizon (dashed Cyber Purple)
            clipRect(top = cy, bottom = h) {
                drawCircle(
                    color = Color(0xFFD500F9).copy(alpha = 0.4f),
                    radius = rp1,
                    center = Offset(cx, cy1),
                    style = Stroke(
                        width = with(density) { (if (isSel1) 1.5.dp.toPx() else 0.8.dp.toPx()) },
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
                    ),
                )
            }

            // 5. Draw Planet 2 Path
            // Draw above-horizon (solid Cyber Cyan)
            clipRect(top = 0f, bottom = cy) {
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = rp2,
                    center = Offset(cx, cy2),
                    style = Stroke(width = with(density) { (if (isSel2) 2.dp.toPx() else 1.dp.toPx()) }),
                )
            }

            // Draw below-horizon (dashed Cyber Purple)
            clipRect(top = cy, bottom = h) {
                drawCircle(
                    color = Color(0xFFD500F9).copy(alpha = 0.4f),
                    radius = rp2,
                    center = Offset(cx, cy2),
                    style = Stroke(
                        width = with(density) { (if (isSel2) 1.5.dp.toPx() else 0.8.dp.toPx()) },
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
                    ),
                )
            }

            // 6. Draw Planet 1 Dot & reticle
            drawCircle(
                color = if (isBelowHorizon1) p1Color.copy(alpha = 0.4f) else p1Color,
                radius = with(density) { (if (isSel1) 6.dp.toPx() else 4.dp.toPx()) },
                center = Offset(dx1, dy1),
            )

            if (isSel1) {
                drawCircle(
                    color = p1Color,
                    radius = with(density) { 9.5.dp.toPx() },
                    center = Offset(dx1, dy1),
                    style = Stroke(width = with(density) { 1.dp.toPx() }, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)),
                )
            }

            // 7. Draw Planet 2 Dot & reticle
            drawCircle(
                color = if (isBelowHorizon2) p2Color.copy(alpha = 0.4f) else p2Color,
                radius = with(density) { (if (isSel2) 6.dp.toPx() else 4.dp.toPx()) },
                center = Offset(dx2, dy2),
            )

            if (isSel2) {
                drawCircle(
                    color = p2Color,
                    radius = with(density) { 9.5.dp.toPx() },
                    center = Offset(dx2, dy2),
                    style = Stroke(width = with(density) { 1.dp.toPx() }, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)),
                )
            }
        }
    }
}

package com.example.genpox.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.ScannerRadarState

@Composable
fun PoxRadarDials(
    state: ScannerRadarState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeSiphonFont by viewModel.activeSiphonedFont.collectAsState()
    val siphonedRate by viewModel.siphonedRate.collectAsState()

    // Calculate zoom multiplier dynamically from state
    val zoomSteps = listOf(4.0f, 2.0f, 1.0f, 0.5f, 0.25f)
    val lowerIndex = state.sliderValue.toInt().coerceIn(0, 3)
    val upperIndex = lowerIndex + 1
    val fraction = state.sliderValue - lowerIndex
    val zoomMultiplier = zoomSteps[lowerIndex] + fraction * (zoomSteps[upperIndex] - zoomSteps[lowerIndex])

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Proximity Siphoning Banner
        if (activeSiphonFont != null && siphonedRate > 0) {
            val font = activeSiphonFont!!
            val baseName = when (font.baseType) {
                'A' -> "ADENINE (A)"
                'G' -> "GUANINE (G)"
                'T' -> "THYMINE (T)"
                'C' -> "CYTOSINE (C)"
                else -> "UNKNOWN"
            }
            val baseColor = when (font.baseType) {
                'A' -> Color(0xFF38BDF8)
                'G' -> Color(0xFF34D399)
                'T' -> Color(0xFFFB7185)
                'C' -> Color(0xFFFBBF24)
                else -> Color.Cyan
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .border(1.dp, baseColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PROXIMITY SIPHONING ACTIVE",
                        color = baseColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "GATHER RATE: +$siphonedRate UNITS/SEC -> $baseName",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 2. Dial Expansion Fractions
        val zoomExpansionFraction by animateFloatAsState(
            targetValue = if (state.zoomExpanded) 1f else 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "zoom_expansion"
        )
        val rotationExpansionFraction by animateFloatAsState(
            targetValue = if (state.rotationExpanded) 1f else 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "rotation_expansion"
        )

        // New Layout Spacing Standards:
        // - Base bottom at bottom = 198.dp (floats exactly 8.dp above PoxHoloNav top boundary of 190.dp)
        val baseBottom = 198.dp

        // - Rotation (collapsed) end = 12.dp.
        // - Zoom (collapsed) end = 66.dp.
        // - When Rotation expands, it pushes Zoom end by 44.dp to 110.dp to maintain exactly 8.dp gap.
        val animatedZoomEnd = (66f + 44f * rotationExpansionFraction).dp
        val animatedRotEnd = 12.dp

        ZoomScrollCircle(
            sliderValue = state.sliderValue,
            onValueChange = { state.sliderValue = it },
            zoomMultiplier = zoomMultiplier,
            zoomExpanded = state.zoomExpanded,
            onToggleExpand = {
                viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                state.zoomExpanded = !state.zoomExpanded
                if (state.zoomExpanded) {
                    state.rotationExpanded = false
                }
            },
            expansionFraction = zoomExpansionFraction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = baseBottom, end = animatedZoomEnd)
        )

        RotationScrollCircle(
            rotationAngle = state.rotationValue,
            onValueChange = { state.rotationValue = it },
            rotationExpanded = state.rotationExpanded,
            onToggleExpand = {
                viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                state.rotationExpanded = !state.rotationExpanded
                if (state.rotationExpanded) {
                    state.zoomExpanded = false
                }
            },
            expansionFraction = rotationExpansionFraction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = baseBottom, end = animatedRotEnd)
        )
    }
}

@Composable
private fun ZoomScrollCircle(
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    zoomMultiplier: Float,
    zoomExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expansionFraction: Float,
    modifier: Modifier = Modifier
) {
    val cyberGreen = Color(0xFF00FF66)

    val currentSize = (46f + 44f * expansionFraction).dp
    val centralSize = (46f + 8f * expansionFraction).dp

    Box(
        modifier = modifier
            .size(currentSize)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f + 0.25f * expansionFraction))
            .pointerInput(zoomExpanded, sliderValue) {
                if (!zoomExpanded) {
                    detectTapGestures {
                        onToggleExpand()
                    }
                } else {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = change.position.x - cx
                                val dy = change.position.y - cy
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                val centerDiscRadiusPx = (27f * expansionFraction).dp.toPx()

                                if (dist >= centerDiscRadiusPx) {
                                    change.consume()
                                    val angleRad = kotlin.math.atan2(dy, dx)
                                    var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                                    if (angleDeg < 0) angleDeg += 360f

                                    // Align 0 (min) at 90 degrees (pointing straight down)
                                    var relativeAngle = angleDeg - 90f
                                    if (relativeAngle < 0) relativeAngle += 360f

                                    val prevValue = sliderValue
                                    val targetValue = (relativeAngle / 360f) * 4f

                                    if (prevValue < 1.0f && targetValue > 3.0f) {
                                        onValueChange(0f)
                                    } else if (prevValue > 3.0f && targetValue < 1.0f) {
                                        onValueChange(4f)
                                    } else {
                                        onValueChange(targetValue)
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val cx = w / 2f

            // 1. Draw the magnifying glass button contents (fades out as expanded)
            if (expansionFraction < 1f) {
                val iconAlpha = 1f - expansionFraction
                val iconColor = cyberGreen.copy(alpha = iconAlpha)
                
                val lensCx = cx - w * 0.05f * (1f - expansionFraction)
                val lensCy = cy - h * 0.05f * (1f - expansionFraction)
                val lensRadius = w * 0.22f
                val strokeW = 1.5.dp.toPx()

                // Draw lens circle
                drawCircle(
                    color = iconColor,
                    radius = lensRadius,
                    center = Offset(lensCx, lensCy),
                    style = Stroke(width = strokeW)
                )

                // Draw connected handle extending to bottom-right
                drawLine(
                    color = iconColor,
                    start = Offset(lensCx + lensRadius * 0.3f, lensCy + lensRadius * 0.3f),
                    end = Offset(cx + w * 0.25f, cy + h * 0.25f),
                    strokeWidth = strokeW * 1.5f
                )

                // Faint outer green circle boundary for the button when collapsed
                drawCircle(
                    color = cyberGreen.copy(alpha = 0.4f * iconAlpha),
                    radius = (w / 2f) - 1.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Draw active track & slider thumb of the circular dial (fades in as expanded)
            if (expansionFraction > 0f) {
                val dialAlpha = expansionFraction
                val trackRadius = (w / 2f) - 10.dp.toPx()

                // Thin green circle background track
                drawCircle(
                    color = cyberGreen.copy(alpha = 0.2f * dialAlpha),
                    radius = trackRadius,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Highlighted active track segment
                val activeSweep = (sliderValue / 4f) * 360f
                drawArc(
                    color = cyberGreen.copy(alpha = dialAlpha),
                    startAngle = 90f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Slider Thumb line pointer (fades in once almost fully expanded)
                if (expansionFraction > 0.8f) {
                    val thumbAlpha = (expansionFraction - 0.8f) / 0.2f
                    val thumbAngle = 90f + activeSweep
                    val thumbRad = Math.toRadians(thumbAngle.toDouble())
                    val ux = kotlin.math.cos(thumbRad).toFloat()
                    val uy = kotlin.math.sin(thumbRad).toFloat()

                    val innerR = trackRadius - 5.dp.toPx()
                    val outerR = trackRadius + 5.dp.toPx()
                    val lineStart = Offset(cx + innerR * ux, cy + innerR * uy)
                    val lineEnd = Offset(cx + outerR * ux, cy + outerR * uy)

                    // Draw outer glow line
                    drawLine(
                        color = cyberGreen.copy(alpha = 0.35f * thumbAlpha),
                        start = lineStart,
                        end = lineEnd,
                        strokeWidth = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Draw solid pointer line
                    drawLine(
                        color = cyberGreen.copy(alpha = thumbAlpha),
                        start = lineStart,
                        end = lineEnd,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // Central overlay disc holding the zoom readout (fades/scales in)
        Box(
            modifier = Modifier
                .size(centralSize)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.85f * expansionFraction))
                .then(
                    if (zoomExpanded) {
                        Modifier.clickable {
                            onToggleExpand()
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (expansionFraction > 0.3f) {
                val textAlpha = (expansionFraction - 0.3f) / 0.7f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(expansionFraction)
                ) {
                    Text(
                        text = "ZOOM",
                        color = cyberGreen.copy(alpha = 0.6f * textAlpha),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.2fx", zoomMultiplier),
                        color = cyberGreen.copy(alpha = textAlpha),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun RotationScrollCircle(
    rotationAngle: Float,
    onValueChange: (Float) -> Unit,
    rotationExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expansionFraction: Float,
    modifier: Modifier = Modifier
) {
    val cyberGreen = Color(0xFF00FF66)

    val currentSize = (46f + 44f * expansionFraction).dp
    val centralSize = (46f + 8f * expansionFraction).dp

    Box(
        modifier = modifier
            .size(currentSize)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f + 0.25f * expansionFraction))
            .pointerInput(rotationExpanded, rotationAngle) {
                if (!rotationExpanded) {
                    detectTapGestures {
                        onToggleExpand()
                    }
                } else {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = change.position.x - cx
                                val dy = change.position.y - cy
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                val centerDiscRadiusPx = (27f * expansionFraction).dp.toPx()

                                if (dist >= centerDiscRadiusPx) {
                                    change.consume()
                                    val angleRad = kotlin.math.atan2(dy, dx)
                                    var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                                    if (angleDeg < 0) angleDeg += 360f

                                    // Align 0 at 90 degrees (pointing straight down)
                                    var relativeAngle = angleDeg - 90f
                                    if (relativeAngle < 0) relativeAngle += 360f

                                    onValueChange(relativeAngle)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val cx = w / 2f

            // 1. Draw double-ended arrow ellipse (fades out as expanded)
            if (expansionFraction < 1f) {
                val iconAlpha = 1f - expansionFraction
                val iconColor = cyberGreen.copy(alpha = iconAlpha)
                val strokeW = 1.5.dp.toPx()

                val rx = w * 0.26f
                val ry = h * 0.16f

                // Draw two arcs
                // Arc 1: bottom-left/right (from 20f to 140f)
                drawArc(
                    color = iconColor,
                    startAngle = 20f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2, ry * 2),
                    style = Stroke(width = strokeW)
                )

                // Arc 2: top-left/right (from 200f to 320f)
                drawArc(
                    color = iconColor,
                    startAngle = 200f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2, ry * 2),
                    style = Stroke(width = strokeW)
                )

                // Arrow head helper
                fun drawArrowHead(thetaDeg: Float, clockwise: Boolean) {
                    val theta = Math.toRadians(thetaDeg.toDouble())
                    val px = cx + rx * kotlin.math.cos(theta).toFloat()
                    val py = cy + ry * kotlin.math.sin(theta).toFloat()
                    
                    val tx = -rx * kotlin.math.sin(theta).toFloat()
                    val ty = ry * kotlin.math.cos(theta).toFloat()
                    val len = kotlin.math.sqrt(tx * tx + ty * ty)
                    if (len < 0.01f) return
                    
                    val dirX = (tx / len) * (if (clockwise) 1f else -1f)
                    val dirY = (ty / len) * (if (clockwise) 1f else -1f)
                    
                    val normX = -dirY
                    val normY = dirX
                    
                    val arrowLen = 5.dp.toPx()
                    val arrowWidth = 3.dp.toPx()
                    
                    val pBackX = px - dirX * arrowLen
                    val pBackY = py - dirY * arrowLen
                    
                    val pLeftX = pBackX + normX * arrowWidth
                    val pLeftY = pBackY + normY * arrowWidth
                    
                    val pRightX = pBackX - normX * arrowWidth
                    val pRightY = pBackY - normY * arrowWidth
                    
                    val arrowPath = Path().apply {
                        moveTo(px, py)
                        lineTo(pLeftX, pLeftY)
                        lineTo(pRightX, pRightY)
                        close()
                    }
                    drawPath(arrowPath, color = iconColor, style = Fill)
                }

                // Draw arrowheads at the ends of the arcs
                drawArrowHead(140f, clockwise = true)
                drawArrowHead(20f, clockwise = false)
                drawArrowHead(320f, clockwise = true)
                drawArrowHead(200f, clockwise = false)

                // Faint outer green circle boundary for the button when collapsed
                drawCircle(
                    color = cyberGreen.copy(alpha = 0.4f * iconAlpha),
                    radius = (w / 2f) - 1.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Draw active track & slider thumb of the circular dial (fades in as expanded)
            if (expansionFraction > 0f) {
                val dialAlpha = expansionFraction
                val trackRadius = (w / 2f) - 10.dp.toPx()

                // Thin green circle background track
                drawCircle(
                    color = cyberGreen.copy(alpha = 0.2f * dialAlpha),
                    radius = trackRadius,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Highlighted active track segment (maps rotationAngle from 0 to 360)
                val activeSweep = (rotationAngle / 360f) * 360f
                drawArc(
                    color = cyberGreen.copy(alpha = dialAlpha),
                    startAngle = 90f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Slider Thumb line pointer (fades in once almost fully expanded)
                if (expansionFraction > 0.8f) {
                    val thumbAlpha = (expansionFraction - 0.8f) / 0.2f
                    val thumbAngle = 90f + activeSweep
                    val thumbRad = Math.toRadians(thumbAngle.toDouble())
                    val ux = kotlin.math.cos(thumbRad).toFloat()
                    val uy = kotlin.math.sin(thumbRad).toFloat()

                    val innerR = trackRadius - 5.dp.toPx()
                    val outerR = trackRadius + 5.dp.toPx()
                    val lineStart = Offset(cx + innerR * ux, cy + innerR * uy)
                    val lineEnd = Offset(cx + outerR * ux, cy + outerR * uy)

                    // Draw outer glow line
                    drawLine(
                        color = cyberGreen.copy(alpha = 0.35f * thumbAlpha),
                        start = lineStart,
                        end = lineEnd,
                        strokeWidth = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Draw solid pointer line
                    drawLine(
                        color = cyberGreen.copy(alpha = thumbAlpha),
                        start = lineStart,
                        end = lineEnd,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // Central overlay disc holding the rotation readout (fades/scales in)
        Box(
            modifier = Modifier
                .size(centralSize)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.85f * expansionFraction))
                .then(
                    if (rotationExpanded) {
                        Modifier.clickable {
                            onToggleExpand()
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (expansionFraction > 0.3f) {
                val textAlpha = (expansionFraction - 0.3f) / 0.7f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(expansionFraction)
                ) {
                    Text(
                        text = "ROTATION",
                        color = cyberGreen.copy(alpha = 0.6f * textAlpha),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.0f°", rotationAngle),
                        color = cyberGreen.copy(alpha = textAlpha),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

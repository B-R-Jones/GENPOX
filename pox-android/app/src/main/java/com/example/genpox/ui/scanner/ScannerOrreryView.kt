package com.example.genpox.ui.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.data.GraphicsMath
import com.example.genpox.data.WaveMath
import com.example.genpox.theme.CyberGreen
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.ui.components.HolographicRenderEngine
import com.example.genpox.ui.main.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ScannerCyberOrreryView(viewModel: MainViewModel) {
    val hostPlanetState by viewModel.selectedHostPlanet.collectAsStateWithLifecycle(initialValue = "EARTH")
    val userLat by viewModel.latitude.collectAsStateWithLifecycle()
    val userLng by viewModel.longitude.collectAsStateWithLifecycle()

    val planetPair = remember(hostPlanetState) {
        when (hostPlanetState.uppercase()) {
            "MERCURY", "VENUS" -> Pair("MERCURY", "VENUS")
            "EARTH", "MARS" -> Pair("EARTH", "MARS")
            "JUPITER", "SATURN" -> Pair("JUPITER", "SATURN")
            "URANUS", "NEPTUNE" -> Pair("URANUS", "NEPTUNE")
            else -> Pair("EARTH", "MARS")
        }
    }
    PlanetTransitArcPanel(
        planet1 = planetPair.first,
        planet2 = planetPair.second,
        selectedPlanet = hostPlanetState,
        userLat = userLat,
        userLng = userLng,
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun HorizontalCyberScaleSlider(
    label: String,
    steps: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var width by remember { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = CyberGreenDim,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = steps[selectedIndex],
                color = CyberGreen,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(steps.size) {
                    detectTapGestures { offset ->
                        if (width > 0) {
                            val segmentWidth = width / (steps.size - 1)
                            val index = (offset.x / segmentWidth).roundToInt().coerceIn(0, steps.size - 1)
                            onIndexSelected(index)
                        }
                    }
                }
                .pointerInput(steps.size) {
                    detectDragGestures { change, _ ->
                        if (width > 0) {
                            val segmentWidth = width / (steps.size - 1)
                            val index = (change.position.x / segmentWidth).roundToInt().coerceIn(0, steps.size - 1)
                            onIndexSelected(index)
                        }
                    }
                },
        ) {
            width = size.width
            val h = size.height
            val trackY = h * 0.35f

            drawLine(
                color = CyberGreenDim.copy(alpha = 0.25f),
                start = Offset(0f, trackY),
                end = Offset(width, trackY),
                strokeWidth = 1.dp.toPx(),
            )

            val segmentWidth = width / (steps.size - 1)

            val paint = android.graphics.Paint().apply {
                color = Color.White.copy(alpha = 0.5f).toArgb()
                textSize = with(density) { 7.sp.toPx() }
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val activePaint = android.graphics.Paint().apply {
                color = CyberGreen.toArgb()
                textSize = with(density) { 7.sp.toPx() }
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }

            for (i in steps.indices) {
                val x = i * segmentWidth
                val isSelected = i == selectedIndex

                drawLine(
                    color = if (isSelected) CyberGreen else CyberGreenDim.copy(alpha = 0.4f),
                    start = Offset(x, trackY - 3.dp.toPx()),
                    end = Offset(x, trackY + 3.dp.toPx()),
                    strokeWidth = if (isSelected) 1.5.dp.toPx() else 0.8.dp.toPx(),
                )

                drawContext.canvas.nativeCanvas.drawText(
                    steps[i],
                    x,
                    h - 1.dp.toPx(),
                    if (isSelected) activePaint else paint,
                )
            }

            val activeX = selectedIndex * segmentWidth
            drawLine(
                color = CyberGreen,
                start = Offset(activeX, trackY - 5.dp.toPx()),
                end = Offset(activeX, trackY + 5.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
            )
        }
    }
}

@Composable
fun PlanetTransitArcPanel(
    planet1: String,
    planet2: String,
    selectedPlanet: String,
    userLat: Double,
    userLng: Double,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val scaleIndex by viewModel.orreryTimeScaleIndex.collectAsStateWithLifecycle()

    val timeState = remember { mutableStateOf(WaveMath.getAdjustedTime()) }
    LaunchedEffect(scaleIndex) {
        while (true) {
            withFrameMillis {
                timeState.value = WaveMath.getAdjustedTime()
            }
        }
    }
    val timeMs = timeState.value

    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    var lastAngle by remember { mutableStateOf<Double?>(null) }

    fun getPlanetCyberColor(planet: String): Color = when (planet.uppercase()) {
        "MERCURY" -> Color(0xFFCFD8DC)
        "VENUS" -> Color(0xFFFFEA00)
        "EARTH" -> Color(0xFF00B0FF)
        "MARS" -> Color(0xFFFF3D00)
        "JUPITER" -> Color(0xFFFF9100)
        "SATURN" -> Color(0xFFFFD54F)
        "URANUS" -> Color(0xFF1DE9B6)
        "NEPTUNE" -> Color(0xFF3D5AFE)
        else -> Color.White
    }

    val baseChar = when (planet1.uppercase()) {
        "MERCURY", "VENUS" -> 'A'
        "EARTH", "MARS" -> 'G'
        "JUPITER", "SATURN" -> 'T'
        "URANUS", "NEPTUNE" -> 'C'
        else -> 'G'
    }

    val alignmentState = remember(planet1, planet2, timeMs) {
        WaveMath.getBaseAlignmentState(baseChar, timeMs)
    }

    val earthPos = remember(timeMs) { WaveMath.getHeliocentricPosition("EARTH", timeMs) }
    val xE = earthPos.first
    val yE = earthPos.second

    val coords1 = remember(planet1, timeMs, userLng, xE, yE) {
        GraphicsMath.calculateOrreryCoordinates(planet1, timeMs, userLng, xE, yE)
    }
    val lambda1 = coords1.lambda
    val dec1 = coords1.declination
    val ha1 = coords1.hourAngle

    val coords2 = remember(planet2, timeMs, userLng, xE, yE) {
        GraphicsMath.calculateOrreryCoordinates(planet2, timeMs, userLng, xE, yE)
    }
    val lambda2 = coords2.lambda
    val dec2 = coords2.declination
    val ha2 = coords2.hourAngle

    val latRad = remember(userLat) { Math.toRadians(userLat) }

    val diffDeg = remember(ha1, ha2) {
        GraphicsMath.calculateHourAngleSeparation(ha1, ha2)
    }

    val qScoreBuff = when (alignmentState) {
        WaveMath.AlignmentState.CONJUNCTION -> "+5.0 Q-Score"
        WaveMath.AlignmentState.OPPOSITION -> "-6.0 Q-Score"
        else -> "±0.0 Q-Score"
    }
    val mutationMultiplier = when (alignmentState) {
        WaveMath.AlignmentState.CONJUNCTION -> "0.5x Mutation"
        WaveMath.AlignmentState.OPPOSITION -> "2.0x Mutation"
        else -> "1.0x Mutation"
    }
    val bondingChance = when (alignmentState) {
        WaveMath.AlignmentState.CONJUNCTION -> "40% Success"
        WaveMath.AlignmentState.OPPOSITION -> "15% Success"
        else -> "20% Success"
    }
    val minQScore = when (alignmentState) {
        WaveMath.AlignmentState.CONJUNCTION -> "MIN Q >= 22.0"
        else -> "MIN Q >= 28.0"
    }

    Box(
        modifier = modifier
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "LOCAL DIURNAL TRANSITS",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            val ha1Hours = ha1 * 12.0 / Math.PI
            val ha1H = ha1Hours.toInt()
            val ha1M = ((ha1Hours - ha1H) * 60.0).toInt()
            Text(
                text = "$planet1: ${ha1H}h ${ha1M}m",
                color = if (selectedPlanet.uppercase() == planet1.uppercase()) CyberGreen else Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )

            val ha2Hours = ha2 * 12.0 / Math.PI
            val ha2H = ha2Hours.toInt()
            val ha2M = ((ha2Hours - ha2H) * 60.0).toInt()
            Text(
                text = "$planet2: ${ha2H}h ${ha2M}m",
                color = if (selectedPlanet.uppercase() == planet2.uppercase()) CyberGreen else Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "CELESTIAL ALIGNMENT",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "SEPARATION: ${String.format("%.1f", diffDeg)}°",
                color = Color.White,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "STATE: ${alignmentState.name}",
                color = when (alignmentState) {
                    WaveMath.AlignmentState.CONJUNCTION -> Color(0xFF00FF66)
                    WaveMath.AlignmentState.OPPOSITION -> Color(0xFFFF1744)
                    else -> Color(0xFFFFEA00)
                },
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "ALIGNMENT RESISTANCE",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = qScoreBuff,
                color = if (alignmentState == WaveMath.AlignmentState.CONJUNCTION) {
                    CyberGreen
                } else if (alignmentState == WaveMath.AlignmentState.OPPOSITION) {
                    Color.Red
                } else {
                    Color.White.copy(alpha = 0.6f)
                },
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = mutationMultiplier,
                color = if (alignmentState == WaveMath.AlignmentState.CONJUNCTION) {
                    CyberGreen
                } else if (alignmentState == WaveMath.AlignmentState.OPPOSITION) {
                    Color.Red
                } else {
                    Color.White.copy(alpha = 0.6f)
                },
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "BONDING EFFICIENCY",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = bondingChance,
                color = if (alignmentState == WaveMath.AlignmentState.CONJUNCTION) CyberGreen else Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = minQScore,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 104.dp)
                .onSizeChanged { size ->
                    canvasWidth = size.width.toFloat()
                    canvasHeight = size.height.toFloat()
                }
                .pointerInput(selectedPlanet, planet1, planet2) {
                    detectTapGestures(
                        onTap = {
                            val otherPlanet = if (selectedPlanet.uppercase() == planet1.uppercase()) planet2 else planet1
                            viewModel.synthManager.playCombinatorTick()
                            viewModel.changeHostPlanet(otherPlanet.uppercase())
                        },
                        onLongPress = {
                            viewModel.rewindOrreryTime()
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cx = canvasWidth / 2f
                            val cy = canvasHeight * 0.5f
                            lastAngle = kotlin.math.atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                        },
                        onDragEnd = {
                            lastAngle = null
                        },
                        onDragCancel = {
                            lastAngle = null
                        },
                        onDrag = { change, _ ->
                            val cx = canvasWidth / 2f
                            val cy = canvasHeight * 0.5f
                            if (canvasWidth > 0 && canvasHeight > 0) {
                                val newAngle = kotlin.math.atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                                val prevAngle = lastAngle
                                if (prevAngle != null) {
                                    var delta = newAngle - prevAngle
                                    if (delta > Math.PI) {
                                        delta -= 2.0 * Math.PI
                                    } else if (delta < -Math.PI) {
                                        delta += 2.0 * Math.PI
                                    }

                                    val timeScaleFactor = WaveMath.timeScale.coerceAtLeast(1.0)
                                    val fullRotationMs = 30L * 24L * 3600L * 1000L
                                    val deltaMs = ((delta / (2.0 * Math.PI)) * fullRotationMs * timeScaleFactor).toLong()

                                    val nextOffset = (WaveMath.manualOffsetMs + deltaMs).coerceAtLeast(0L)
                                    if (nextOffset != WaveMath.manualOffsetMs) {
                                        WaveMath.manualOffsetMs = nextOffset
                                        if (kotlin.math.abs(delta) > 0.04) {
                                            viewModel.synthManager.playBeep(440f, 0.01f, "sine")
                                        }
                                    }
                                }
                                lastAngle = newAngle
                            }
                        },
                    )
                },
        ) {
            val drawScope = this
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.5f

            val maxRadius = kotlin.math.min(w, h) * 0.45f

            val isSel1 = selectedPlanet.uppercase() == planet1.uppercase()
            val isSel2 = selectedPlanet.uppercase() == planet2.uppercase()

            val proj1 = GraphicsMath.projectOrreryPlanet(cx, cy, maxRadius, 0.78f, latRad, dec1, ha1)
            val cy1 = proj1.centerY
            val rp1 = proj1.projectedRadius
            val dx1 = proj1.dx
            val dy1 = proj1.dy
            val isBelowHorizon1 = proj1.isBelowHorizon

            val proj2 = GraphicsMath.projectOrreryPlanet(cx, cy, maxRadius, 1.0f, latRad, dec2, ha2)
            val cy2 = proj2.centerY
            val rp2 = proj2.projectedRadius
            val dx2 = proj2.dx
            val dy2 = proj2.dy
            val isBelowHorizon2 = proj2.isBelowHorizon

            val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }.format(Date(timeMs))

            val p1Color = getPlanetCyberColor(planet1)
            val p2Color = getPlanetCyberColor(planet2)

            HolographicRenderEngine.run {
                drawPlanetTransitOrrery(
                    w = w,
                    h = h,
                    cx = cx,
                    cy = cy,
                    maxRadius = maxRadius,
                    cy1 = cy1,
                    rp1 = rp1,
                    dx1 = dx1,
                    dy1 = dy1,
                    isBelowHorizon1 = isBelowHorizon1,
                    isSel1 = isSel1,
                    p1Color = p1Color,
                    cy2 = cy2,
                    rp2 = rp2,
                    dx2 = dx2,
                    dy2 = dy2,
                    isBelowHorizon2 = isBelowHorizon2,
                    isSel2 = isSel2,
                    p2Color = p2Color,
                    dateText = dateText,
                    alignmentState = alignmentState,
                    density = drawScope,
                )
            }
        }

        HorizontalCyberScaleSlider(
            label = "SCALE",
            steps = listOf("1", "10", "100", "1000", "10000"),
            selectedIndex = scaleIndex,
            onIndexSelected = { viewModel.setOrreryTimeScaleIndex(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 80.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val p1Color = getPlanetCyberColor(planet1)
            val p2Color = getPlanetCyberColor(planet2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(if (selectedPlanet.uppercase() == planet1.uppercase()) p1Color else p1Color.copy(alpha = 0.4f), RoundedCornerShape(50)))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = planet1, color = if (selectedPlanet.uppercase() == planet1.uppercase()) p1Color else Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(if (selectedPlanet.uppercase() == planet2.uppercase()) p2Color else p2Color.copy(alpha = 0.4f), RoundedCornerShape(50)))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = planet2, color = if (selectedPlanet.uppercase() == planet2.uppercase()) p2Color else Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

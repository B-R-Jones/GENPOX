package com.example.genpox.ui.telemetry

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.data.GraphicsMath
import com.example.genpox.theme.CyberCyan
import com.example.genpox.theme.CyberGreen
import com.example.genpox.ui.main.MainViewModel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun WireframeGlobe(
    modifier: Modifier = Modifier,
    isMini: Boolean = false,
    rotationSpeedMs: Int = 20000,
    activeCells: List<String> = emptyList(),
    viewModel: MainViewModel,
) {
    var zoomLevel by remember { mutableStateOf(0) }

    LaunchedEffect(isMini) {
        if (isMini) zoomLevel = 0
    }

    val infiniteTransition = rememberInfiniteTransition(label = "globe_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(rotationSpeedMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    val baseContinents = remember {
        getGlobePart1() + getGlobePart2() + getGlobePart3() + getGlobePart4()
    }

    val smoothedContinents = remember(baseContinents) {
        baseContinents.map { loop -> interpolateClosedLoop(loop, stepsPerSegment = 1) }
    }

    val userLat by viewModel.latitude.collectAsStateWithLifecycle()
    val userLng by viewModel.longitude.collectAsStateWithLifecycle()

    val latVal = userLat.coerceIn(-90.0, 90.0)
    val lngVal = userLng.coerceIn(-180.0, 180.0)
    val latIndex = (((latVal + 90.0) / 180.0) * 240.0).toInt().coerceIn(0, 239) + 10
    val lngIndex = (((lngVal + 180.0) / 360.0) * 254.0).toInt().coerceIn(0, 253) + 1
    val latFraction = (((latVal + 90.0) / 180.0) * 240.0) % 1.0
    val lngFraction = (((lngVal + 180.0) / 360.0) * 254.0) % 1.0
    val subLat = (latFraction * 16.0).toInt().coerceIn(0, 15)
    val subLng = (lngFraction * 16.0).toInt().coerceIn(0, 15)
    val octet4 = ((subLat * 16) + subLng + 1).coerceIn(1, 254)

    Canvas(
        modifier = modifier
            .then(
                if (!isMini) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            viewModel.synthManager.playCombinatorTick()
                            zoomLevel = (zoomLevel + 1) % 4
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f * 0.9f

        val frontColor = Color(0xFF38BDF8)
        val strokeWidth = if (isMini) 1.dp.toPx() else 1.5.dp.toPx()

        val tiltAngle = if (zoomLevel == 1) {
            Math.toRadians(latVal)
        } else {
            Math.toRadians(23.5)
        }

        val currentRotation = if (zoomLevel == 1) {
            Math.PI / 2.0 - Math.toRadians(lngVal)
        } else {
            rotationAngle.toDouble()
        }

        drawCircle(
            color = frontColor.copy(alpha = 0.15f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth),
        )

        if (zoomLevel == 0 || zoomLevel == 1) {
            val scaleFactor = if (zoomLevel == 1) 4.5f else 1.0f

            withTransform({
                if (zoomLevel == 1) {
                    this.scale(scaleFactor, scaleFactor, Offset(centerX, centerY))
                }
            }) {
                val effStroke = strokeWidth / scaleFactor

                val numMeridians = if (isMini) 6 else 12
                val pointsPerLine = if (isMini) 15 else 30
                for (m in 0 until numMeridians) {
                    val lon = (m.toFloat() / numMeridians) * 2.0 * Math.PI - Math.PI
                    val rotLon = lon + currentRotation

                    var prevX = 0f
                    var prevY = 0f
                    var prevZ = 0.0
                    var hasPrev = false

                    for (p in 0..pointsPerLine) {
                        val lat = (p.toFloat() / pointsPerLine) * Math.PI - Math.PI / 2.0

                        val x = -radius.toDouble() * cos(lat) * cos(rotLon)
                        val y = radius.toDouble() * sin(lat)
                        val z = radius.toDouble() * cos(lat) * sin(rotLon)

                        val yt = y * cos(tiltAngle) - z * sin(tiltAngle)
                        val zt = y * sin(tiltAngle) + z * cos(tiltAngle)
                        val xt = x

                        val screenX = (centerX + xt).toFloat()
                        val screenY = (centerY - yt).toFloat()

                        if (hasPrev) {
                            val avgZ = (zt + prevZ) / 2.0
                            val depthPct = ((avgZ / radius.toDouble()).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                            val alpha = (0.15f + 0.5f * depthPct).toFloat()

                            drawLine(
                                color = frontColor.copy(alpha = alpha),
                                start = Offset(prevX, prevY),
                                end = Offset(screenX, screenY),
                                strokeWidth = effStroke,
                            )
                        }
                        prevX = screenX
                        prevY = screenY
                        prevZ = zt
                        hasPrev = true
                    }
                }

                val numParallels = if (isMini) 5 else 9
                val pointsPerParallel = if (isMini) 20 else 40
                for (p in 1 until numParallels) {
                    val lat = (p.toFloat() / numParallels) * Math.PI - Math.PI / 2.0

                    var prevX = 0f
                    var prevY = 0f
                    var prevZ = 0.0
                    var hasPrev = false

                    for (m in 0..pointsPerParallel) {
                        val lon = (m.toFloat() / pointsPerParallel) * 2.0 * Math.PI - Math.PI
                        val rotLon = lon + currentRotation

                        val x = -radius.toDouble() * cos(lat) * cos(rotLon)
                        val y = radius.toDouble() * sin(lat)
                        val z = radius.toDouble() * cos(lat) * sin(rotLon)

                        val yt = y * cos(tiltAngle) - z * sin(tiltAngle)
                        val zt = y * sin(tiltAngle) + z * cos(tiltAngle)
                        val xt = x

                        val screenX = (centerX + xt).toFloat()
                        val screenY = (centerY - yt).toFloat()

                        if (hasPrev) {
                            val avgZ = (zt + prevZ) / 2.0
                            val depthPct = ((avgZ / radius.toDouble()).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                            val alpha = (0.15f + 0.5f * depthPct).toFloat()

                            drawLine(
                                color = frontColor.copy(alpha = alpha),
                                start = Offset(prevX, prevY),
                                end = Offset(screenX, screenY),
                                strokeWidth = effStroke,
                            )
                        }
                        prevX = screenX
                        prevY = screenY
                        prevZ = zt
                        hasPrev = true
                    }
                }

                val numLoops = smoothedContinents.size
                for (lIdx in 0 until numLoops) {
                    val loop = smoothedContinents[lIdx]
                    val path = Path()
                    var firstPoint = true
                    val numPts = loop.size

                    for (ptIdx in 0 until numPts) {
                        val pt = loop[ptIdx]
                        val lat = pt.first
                        val rotLon = pt.second + currentRotation

                        val x = -radius.toDouble() * cos(lat) * cos(rotLon)
                        val y = radius.toDouble() * sin(lat)
                        val z = radius.toDouble() * cos(lat) * sin(rotLon)

                        val yt = y * cos(tiltAngle) - z * sin(tiltAngle)
                        val zt = y * sin(tiltAngle) + z * cos(tiltAngle)
                        val xt = x

                        if (zt >= -radius.toDouble() * 0.1) {
                            val screenX = (centerX + xt).toFloat()
                            val screenY = (centerY - yt).toFloat()

                            if (firstPoint) {
                                path.moveTo(screenX, screenY)
                                firstPoint = false
                            } else {
                                path.lineTo(screenX, screenY)
                            }
                        } else {
                            firstPoint = true
                        }
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF00FF66).copy(alpha = 0.85f),
                        style = Stroke(width = effStroke * 1.25f),
                    )
                }

                val userLatRad = Math.toRadians(latVal)
                val userRotLon = Math.toRadians(lngVal) + currentRotation
                val ux = -radius.toDouble() * cos(userLatRad) * cos(userRotLon)
                val uy = radius.toDouble() * sin(userLatRad)
                val uz = radius.toDouble() * cos(userLatRad) * sin(userRotLon)
                val uyt = uy * cos(tiltAngle) - uz * sin(tiltAngle)
                val uzt = uy * sin(tiltAngle) + uz * cos(tiltAngle)

                if (uzt >= 0) {
                    val userX = (centerX + ux).toFloat()
                    val userY = (centerY - uyt).toFloat()
                    val beaconPulse = (System.currentTimeMillis() % 1000) / 1000f

                    drawCircle(
                        color = Color.Red,
                        radius = 4.dp.toPx() / scaleFactor,
                        center = Offset(userX, userY),
                    )
                    drawCircle(
                        color = Color.Red.copy(alpha = 1f - beaconPulse),
                        radius = (8.dp.toPx() + beaconPulse * 16.dp.toPx()) / scaleFactor,
                        center = Offset(userX, userY),
                        style = Stroke(width = 1.5.dp.toPx() / scaleFactor),
                    )
                }
            }
        } else if (zoomLevel == 2) {
            val gridCols = 10
            val gridRows = 10
            val startX = centerX - radius * 0.8f
            val startY = centerY - radius * 0.8f
            val gridSize = radius * 1.6f
            val cellSize = gridSize / gridCols

            for (i in 0..gridCols) {
                val x = startX + i * cellSize
                drawLine(
                    color = CyberCyan.copy(alpha = 0.3f),
                    start = Offset(x, startY),
                    end = Offset(x, startY + gridSize),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            for (j in 0..gridRows) {
                val y = startY + j * cellSize
                drawLine(
                    color = CyberCyan.copy(alpha = 0.3f),
                    start = Offset(startX, y),
                    end = Offset(startX + gridSize, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val playerX = startX + 5f * cellSize
            val playerY = startY + 5f * cellSize
            val radarPulse = (System.currentTimeMillis() % 1500) / 1500f

            drawCircle(
                color = CyberGreen.copy(alpha = 0.4f * (1f - radarPulse)),
                radius = cellSize * 4f * radarPulse,
                center = Offset(playerX, playerY),
                style = Stroke(width = 1.5.dp.toPx()),
            )

            val pPath = Path().apply {
                moveTo(playerX, playerY - 8.dp.toPx())
                lineTo(playerX + 8.dp.toPx(), playerY)
                lineTo(playerX, playerY + 8.dp.toPx())
                lineTo(playerX - 8.dp.toPx(), playerY)
                close()
            }
            drawPath(pPath, color = CyberGreen)

            val peer1X = startX + 3.5f * cellSize
            val peer1Y = startY + 7.5f * cellSize
            drawCircle(color = CyberCyan, radius = 5.dp.toPx(), center = Offset(peer1X, peer1Y))

            val peer2X = startX + 7.5f * cellSize
            val peer2Y = startY + 4.5f * cellSize
            drawCircle(color = CyberCyan, radius = 5.dp.toPx(), center = Offset(peer2X, peer2Y))

            val anomX = startX + 6.5f * cellSize
            val anomY = startY + 8.5f * cellSize
            val anomPath = Path().apply {
                moveTo(anomX, anomY - 6.dp.toPx())
                lineTo(anomX + 6.dp.toPx(), anomY + 6.dp.toPx())
                lineTo(anomX - 6.dp.toPx(), anomY + 6.dp.toPx())
                close()
            }
            drawPath(anomPath, color = Color(0xFFF97316))
        } else if (zoomLevel == 3) {
            val crystalRadius = radius * 0.45f
            val rotationVal = rotationAngle.toDouble()
            val tiltVal = Math.toRadians(35.0)

            val vertices = GraphicsMath.generate3DBlueprintVertices(crystalRadius.toDouble(), rotationVal)

            val projected = vertices.map { pt ->
                val projectedPt = GraphicsMath.project3DBlueprintPoint(pt, tiltVal, centerX.toDouble(), centerY.toDouble())
                Offset(projectedPt.x, projectedPt.y)
            }

            for (i in 2..5) {
                drawLine(color = CyberCyan, start = projected[0], end = projected[i], strokeWidth = 1.5.dp.toPx())
                drawLine(color = CyberCyan, start = projected[1], end = projected[i], strokeWidth = 1.5.dp.toPx())
            }
            for (i in 2..5) {
                val next = if (i == 5) 2 else i + 1
                drawLine(color = CyberCyan.copy(alpha = 0.6f), start = projected[i], end = projected[next], strokeWidth = 1.5.dp.toPx())
            }

            val corePulse = 0.8f + 0.2f * sin(System.currentTimeMillis() / 150f).toFloat()
            drawCircle(
                color = CyberGreen.copy(alpha = 0.25f),
                radius = crystalRadius * 0.25f * corePulse,
                center = Offset(centerX, centerY),
            )
            drawCircle(
                color = CyberGreen,
                radius = crystalRadius * 0.08f,
                center = Offset(centerX, centerY),
            )

            val wavePulse = (System.currentTimeMillis() % 1600) / 1600f
            drawCircle(
                color = CyberCyan.copy(alpha = 0.8f * (1f - wavePulse)),
                radius = crystalRadius * 1.5f * wavePulse,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        if (!isMini) {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = frontColor.toArgb()
                    textSize = 8.5.sp.toPx()
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }
                val titlePaint = android.graphics.Paint().apply {
                    color = Color.White.toArgb()
                    textSize = 10.sp.toPx()
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }

                val titleText = when (zoomLevel) {
                    0 -> "HOLOGRAPHIC SATELLITE GLOBE (GLOBAL)"
                    1 -> "METROPOLITAN REGION UPLINK (ZOOM +1)"
                    2 -> "LOCAL MESH SECTOR GRID (ZOOM +2)"
                    else -> "PINPOINT NODE TRANSCEIVER CORE (ZOOM +3)"
                }
                canvas.nativeCanvas.drawText(titleText, 16.dp.toPx(), 24.dp.toPx(), titlePaint)

                val infoLines = when (zoomLevel) {
                    0 -> listOf(
                        "SCALE: 1:40,000,000",
                        "GRID RESOLUTION: 1000 KM (GLOBAL)",
                        "CORE LATITUDE: ${"%.4f".format(latVal)}",
                        "CORE LONGITUDE: ${"%.4f".format(lngVal)}",
                    )
                    1 -> listOf(
                        "CURRENT REGION ID: 10.$latIndex.$lngIndex.x",
                        "SCALE: 1:1,500,000",
                        "GRID RESOLUTION: 80 KM",
                        "SURFACE POSITION: CENTERED",
                    )
                    2 -> listOf(
                        "CURRENT SECTOR ID: 10.$latIndex.$lngIndex.$octet4",
                        "SCALE: 1:50,000",
                        "GRID RANGE: 5 KM",
                        "ACTIVE MESH: 3 NODES DETECTED",
                    )
                    else -> listOf(
                        "NODE IDENTIFIER: ${viewModel.localNodeName}",
                        "IP SUITE: 10.$latIndex.$lngIndex.$octet4",
                        "UPLINK RATING: SECURE MESH",
                        "SYSTEM HARMONICS: 924.8 MHZ",
                    )
                }

                var yPos = height - 16.dp.toPx() - (infoLines.size * 14.dp.toPx())
                infoLines.forEach { line ->
                    canvas.nativeCanvas.drawText(line, 16.dp.toPx(), yPos, paint)
                    yPos += 12.dp.toPx()
                }

                val hintPaint = android.graphics.Paint().apply {
                    color = CyberGreen.toArgb()
                    textSize = 8.sp.toPx()
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("TAP TO ZOOM UPLINK (LEVEL $zoomLevel/3)", width - 180.dp.toPx(), 24.dp.toPx(), hintPaint)
            }
        }
    }
}

private fun interpolateClosedLoop(loop: List<Pair<Double, Double>>, stepsPerSegment: Int = 5): List<Pair<Double, Double>> {
    if (loop.size < 3) return loop
    val result = mutableListOf<Pair<Double, Double>>()
    val n = loop.size
    for (i in 0 until n) {
        val p0 = loop[(i - 1 + n) % n]
        val p1 = loop[i]
        val p2 = loop[(i + 1) % n]
        val p3 = loop[(i + 2) % n]

        val lon0 = adjustLongitude(p0.second, p1.second)
        val lon1 = p1.second
        val lon2 = adjustLongitude(p2.second, p1.second)
        val lon3 = adjustLongitude(p3.second, p1.second)

        for (step in 0 until stepsPerSegment) {
            val t = step.toDouble() / stepsPerSegment
            val lat = catmullRom(p0.first, p1.first, p2.first, p3.first, t)
            val lon = catmullRom(lon0, lon1, lon2, lon3, t)
            result.add(Pair(lat, normalizeLongitude(lon)))
        }
    }
    return result
}

private fun adjustLongitude(lon: Double, reference: Double): Double {
    var diff = lon - reference
    while (diff < -Math.PI) diff += 2.0 * Math.PI
    while (diff > Math.PI) diff -= 2.0 * Math.PI
    return reference + diff
}

private fun normalizeLongitude(lon: Double): Double {
    var result = lon
    while (result < -Math.PI) result += 2.0 * Math.PI
    while (result > Math.PI) result -= 2.0 * Math.PI
    return result
}

private fun catmullRom(p0: Double, p1: Double, p2: Double, p3: Double, t: Double): Double {
    val t2 = t * t
    val t3 = t2 * t
    return 0.5 * (
        (2.0 * p1) +
            (-p0 + p2) * t +
            (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
            (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3
        )
}

private fun deg(lat: Double, lon: Double): Pair<Double, Double> = Pair(Math.toRadians(lat), Math.toRadians(lon))

private fun getGlobePart1(): List<List<Pair<Double, Double>>> = listOf(
    listOf(
        deg(-75.0, 0.0), deg(-72.0, 30.0), deg(-68.0, 60.0), deg(-66.0, 90.0),
        deg(-67.0, 120.0), deg(-68.0, 150.0), deg(-70.0, 180.0), deg(-73.0, -150.0),
        deg(-72.0, -120.0), deg(-74.0, -90.0), deg(-76.0, -60.0), deg(-77.0, -30.0),
        deg(-75.0, 0.0),
    ),
    listOf(
        deg(-34.0, 18.0), deg(-30.0, 30.0), deg(-20.0, 35.0), deg(-10.0, 40.0),
        deg(0.0, 42.0), deg(11.0, 51.0), deg(12.0, 43.0), deg(15.0, 39.0),
        deg(30.0, 32.0), deg(37.0, 10.0), deg(33.0, -9.0), deg(15.0, -17.0),
        deg(5.0, -4.0), deg(-5.0, 12.0), deg(-15.0, 12.0), deg(-25.0, 15.0),
        deg(-34.0, 18.0),
    ),
    listOf(
        deg(-54.0, -68.0), deg(-40.0, -62.0), deg(-23.0, -43.0), deg(-5.0, -35.0),
        deg(5.0, -52.0), deg(10.0, -75.0), deg(8.0, -77.0), deg(-2.0, -80.0),
        deg(-18.0, -70.0), deg(-33.0, -71.0), deg(-45.0, -74.0), deg(-54.0, -68.0),
    ),
    listOf(
        deg(25.0, -80.0), deg(30.0, -80.0), deg(35.0, -75.0), deg(44.0, -64.0),
        deg(55.0, -66.0), deg(60.0, -64.0), deg(70.0, -70.0), deg(72.0, -125.0),
        deg(60.0, -140.0), deg(58.0, -135.0), deg(48.0, -124.0), deg(34.0, -118.0),
        deg(20.0, -105.0), deg(16.0, -95.0), deg(18.0, -88.0), deg(25.0, -80.0),
    ),
)

private fun getGlobePart2(): List<List<Pair<Double, Double>>> = listOf(
    listOf(
        deg(36.0, -5.0), deg(43.0, -9.0), deg(48.0, -4.0), deg(51.0, 1.0),
        deg(54.0, 9.0), deg(55.0, 21.0), deg(59.0, 30.0), deg(68.0, 33.0),
        deg(70.0, 28.0), deg(70.0, 18.0), deg(62.0, 5.0), deg(57.0, 8.0),
        deg(53.0, 4.0), deg(44.0, -1.0), deg(43.0, 3.0), deg(41.0, 1.0),
        deg(38.0, -1.0), deg(36.0, -5.0),
    ),
    listOf(
        deg(70.0, 30.0), deg(73.0, 80.0), deg(70.0, 130.0), deg(65.0, 170.0),
        deg(60.0, 160.0), deg(55.0, 140.0), deg(40.0, 120.0), deg(20.0, 108.0),
        deg(8.0, 77.0), deg(25.0, 62.0), deg(30.0, 48.0), deg(40.0, 50.0),
        deg(50.0, 55.0), deg(60.0, 60.0), deg(70.0, 30.0),
    ),
    listOf(
        deg(-38.0, 144.0), deg(-32.0, 152.0), deg(-25.0, 153.0), deg(-15.0, 145.0),
        deg(-12.0, 132.0), deg(-14.0, 126.0), deg(-20.0, 118.0), deg(-33.0, 115.0),
        deg(-35.0, 117.0), deg(-38.0, 144.0),
    ),
    listOf(
        deg(60.0, -45.0),
        deg(65.0, -40.0),
        deg(75.0, -20.0),
        deg(82.0, -30.0),
        deg(81.0, -60.0),
        deg(76.0, -70.0),
        deg(65.0, -50.0),
        deg(60.0, -45.0),
    ),
)

private fun getGlobePart3(): List<List<Pair<Double, Double>>> = listOf(
    listOf(
        deg(78.88, 99.94), deg(78.76, 97.76), deg(79.04, 94.97), deg(79.43, 93.31),
        deg(80.14, 92.55), deg(80.34, 91.18), deg(81.02, 93.78), deg(81.25, 95.94),
        deg(80.75, 97.88), deg(79.78, 100.19), deg(78.88, 99.94),
    ),
    listOf(
        deg(79.66, -87.02), deg(79.34, -85.81), deg(78.29, -89.04), deg(78.22, -90.80),
        deg(78.34, -92.88), deg(78.75, -93.95), deg(79.11, -93.94), deg(79.38, -93.15),
        deg(79.37, -94.97), deg(79.71, -96.08), deg(80.16, -96.71), deg(80.91, -95.32),
        deg(80.98, -94.30), deg(81.21, -94.74), deg(81.26, -92.41), deg(80.72, -91.13),
        deg(80.32, -87.81), deg(79.66, -87.02),
    ),
)

private fun getGlobePart4(): List<List<Pair<Double, Double>>> = listOf(
    listOf(
        deg(83.11, -68.50), deg(82.63, -61.85), deg(82.36, -61.89), deg(81.50, -67.66),
        deg(81.51, -65.48), deg(80.62, -69.47), deg(79.80, -71.18), deg(79.32, -76.91),
        deg(79.20, -75.53), deg(79.02, -76.22), deg(78.53, -75.39), deg(77.21, -79.76),
        deg(76.98, -79.62), deg(77.02, -77.91), deg(76.78, -77.89), deg(76.18, -80.56),
        deg(76.45, -83.17), deg(76.30, -86.11), deg(76.47, -89.49), deg(76.95, -89.62),
        deg(77.18, -87.77), deg(77.90, -88.26), deg(77.54, -84.98), deg(78.18, -86.34),
        deg(78.37, -87.96), deg(78.76, -87.15), deg(79.00, -85.38), deg(79.35, -85.09),
        deg(79.74, -86.51), deg(80.25, -86.93), deg(80.10, -83.41), deg(80.46, -81.85),
        deg(80.52, -87.60), deg(80.86, -89.37), deg(81.55, -91.37), deg(81.89, -91.59),
        deg(82.28, -86.97), deg(82.65, -85.50), deg(82.32, -83.18), deg(82.86, -82.42),
        deg(83.13, -79.31), deg(83.11, -68.50),
    ),
    listOf(
        deg(83.52, -27.10), deg(82.73, -20.85), deg(82.34, -22.69), deg(82.20, -31.90),
        deg(82.02, -31.40), deg(82.13, -27.86), deg(81.79, -24.84), deg(82.09, -22.90),
        deg(81.73, -22.07), deg(81.15, -23.17), deg(81.91, -15.77), deg(81.72, -12.77),
        deg(81.29, -12.21), deg(80.35, -16.85), deg(80.18, -20.05), deg(80.13, -17.73),
        deg(78.75, -19.70), deg(77.64, -19.67), deg(76.99, -18.47), deg(76.63, -21.68),
        deg(76.10, -19.83), deg(75.25, -19.60), deg(75.16, -20.67), deg(74.30, -19.37),
        deg(74.22, -21.59), deg(73.82, -20.43), deg(73.46, -20.76), deg(73.31, -23.57),
        deg(72.63, -22.31), deg(72.18, -22.30), deg(72.60, -24.28), deg(72.33, -24.79),
        deg(72.08, -23.44), deg(71.47, -22.13), deg(70.66, -21.75), deg(70.47, -23.54),
        deg(71.43, -25.54), deg(70.75, -25.20), deg(70.23, -26.36), deg(70.13, -22.35),
        deg(68.47, -27.75), deg(68.12, -31.78), deg(67.74, -32.81), deg(66.68, -34.20),
        deg(65.98, -36.35), deg(65.46, -39.81), deg(64.84, -40.67), deg(64.14, -40.68),
        deg(63.48, -41.19), deg(62.68, -42.82), deg(61.90, -42.42), deg(60.10, -43.38),
        deg(60.04, -44.79), deg(60.85, -46.26), deg(60.86, -48.26), deg(61.41, -49.23),
        deg(62.38, -49.90), deg(63.63, -51.63), deg(64.28, -52.14), deg(65.18, -52.28),
        deg(66.10, -53.66), deg(66.84, -53.30), deg(67.19, -53.97), deg(68.36, -52.98),
        deg(68.73, -51.48), deg(69.15, -51.08), deg(69.93, -50.87), deg(69.28, -53.46),
        deg(69.61, -54.68), deg(70.29, -54.75), deg(70.82, -54.36), deg(70.57, -51.39),
        deg(71.55, -54.00), deg(71.41, -55.00), deg(71.65, -55.83), deg(72.59, -54.72),
        deg(74.71, -57.32), deg(75.10, -58.60), deg(75.52, -58.59), deg(76.10, -61.27),
        deg(76.06, -68.50), deg(77.01, -71.40), deg(77.38, -66.76), deg(77.64, -71.04),
        deg(78.04, -73.30), deg(78.43, -73.16), deg(79.39, -65.71), deg(79.76, -65.32),
        deg(80.12, -68.02), deg(80.52, -67.15), deg(81.32, -62.23), deg(81.77, -62.65),
        deg(82.19, -57.21), deg(82.20, -54.13), deg(81.89, -53.04), deg(82.44, -50.39),
        deg(81.66, -44.52), deg(82.20, -46.90), deg(82.63, -46.76), deg(83.23, -43.41),
        deg(83.18, -39.90), deg(83.55, -38.62), deg(83.52, -27.10),
    ),
)

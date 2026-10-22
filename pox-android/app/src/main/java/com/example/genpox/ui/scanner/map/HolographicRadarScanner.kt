package com.example.genpox.ui.scanner.map

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.data.*
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.example.genpox.ui.main.*
import com.example.genpox.ui.scanner.*
import com.google.android.gms.maps.model.LatLng
import kotlin.math.absoluteValue

fun getGlitchOffsetX(yPx: Float, tMs: Long): Float = 0f

@Composable
fun HolographicRadarScanner(
    viewModel: MainViewModel,
    anomalies: List<PoxAnomaly>,
    baseFonts: List<PoxBaseFont>,
    celestialWells: List<CelestialWell>,
    userLat: Double,
    userLng: Double,
    zoomMultiplier: Float,
    rotationAngle: Float,
    cameraTiltAngle: Float = 90f,
    radarState: ScannerRadarState? = null,
    selectedAnomalyId: String?,
    onSelectAnomaly: (String) -> Unit,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isProfilerEnabled by viewModel.isProfilerEnabled.collectAsStateWithLifecycle()
    val profilerState = viewModel.profilerState
    FrameTimeMonitor(state = profilerState, enabled = isProfilerEnabled)
    SideEffect { if (isProfilerEnabled) profilerState.hudRecomps++ }

    val mathZoom = zoomMultiplier * 0.625f
    val tiltProgress = ((90.0f - cameraTiltAngle) / (90.0f - 15.0f)).coerceIn(0.0f, 1.0f)
    val tiltYScale = kotlin.math.sin(Math.toRadians(cameraTiltAngle.toDouble())).toFloat().coerceIn(0.1f, 1.0f)
    val sinTilt = kotlin.math.sqrt((1.0f - tiltYScale * tiltYScale).coerceAtLeast(0f))
    val tiltZoomFactor = 1.0f + 2.0f * tiltProgress
    val rotRad = Math.toRadians(rotationAngle.toDouble())
    val cosRot = kotlin.math.cos(rotRad).toFloat()
    val sinRot = kotlin.math.sin(rotRad).toFloat()
    val activeMissions by viewModel.activeMissions.collectAsStateWithLifecycle()
    val geometryCache = remember { mutableMapOf<String, CreatureGeometry>() }
    val trackedMissionId by viewModel.trackedMissionId.collectAsStateWithLifecycle()
    val trackedMission = remember(activeMissions, trackedMissionId) {
        activeMissions.find { it.id == trackedMissionId && !it.isReturned }
    }

    val activeConnections by viewModel.networkManager?.activeConnections?.collectAsStateWithLifecycle(initialValue = emptyMap()) ?: remember { mutableStateOf(emptyMap()) }
    val discoveredPeers by viewModel.networkManager?.discoveredPeers?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val activePeers = remember(activeConnections, discoveredPeers) {
        val list = mutableListOf<String>()
        activeConnections.forEach { (_, name) ->
            if (!list.contains(name)) list.add(name)
        }
        discoveredPeers.forEach { peer ->
            if (!list.contains(peer.name)) list.add(peer.name)
        }
        list
    }

    val peerLocations = remember(activePeers, userLat, userLng) {
        activePeers.mapIndexed { idx, name ->
            val hash = name.hashCode().absoluteValue
            val offsetLat = ((hash % 200) - 100) / 33000.0
            val offsetLng = (((hash / 200) % 200) - 100) / 33000.0
            LatLng(userLat + offsetLat, userLng + offsetLng)
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current

    val activeMissionsList = remember(activeMissions) { activeMissions.filter { !it.isReturned } }
    val creaturesState by viewModel.creatures.collectAsStateWithLifecycle()

    val precomputedMissions = remember(activeMissionsList, anomalies, creaturesState) {
        val list = ArrayList<PrecomputedMissionState>(activeMissionsList.size)
        val activeSize = activeMissionsList.size
        val anomaliesSize = anomalies.size
        for (i in 0 until activeSize) {
            val m = activeMissionsList[i]

            var matchedAnomaly: PoxAnomaly? = null
            for (j in 0 until anomaliesSize) {
                val anom = anomalies[j]
                if (Math.abs(anom.lat - m.lat) < 0.0001 && Math.abs(anom.lng - m.lng) < 0.0001) {
                    matchedAnomaly = anom
                    break
                }
            }

            val dLatAnom = userLat - m.lat
            val cosLatAnom = kotlin.math.cos(Math.toRadians(userLat))
            val distGeo = kotlin.math.sqrt(dLatAnom * dLatAnom + (userLng - m.lng) * cosLatAnom * (userLng - m.lng) * cosLatAnom)

            val boundaryRadius = matchedAnomaly?.getBoundaryRadiusForPlayer(userLat, userLng)
                ?: (m.dispatchDistance / (1.0 - m.stalledDepth / 100.0).coerceAtLeast(0.01))

            val rBoundGeo = boundaryRadius / 111000.0
            val tGeo = if (distGeo > 0.0) (rBoundGeo / distGeo).coerceIn(0.0, 1.0) else 0.0
            val boundLat = m.lat + tGeo * (userLat - m.lat)
            val boundLng = m.lng + tGeo * (userLng - m.lng)

            val tStall = m.stalledDepth / 100.0
            val startLat = boundLat + tStall * (m.lat - boundLat)
            val startLng = boundLng + tStall * (m.lng - boundLng)

            val combinedDensity = if (matchedAnomaly != null) {
                var densitySum = 0.0
                for (j in 0 until anomaliesSize) {
                    val anom = anomalies[j]
                    val distFromAnom = DeploymentEngine.calculateDistanceInFeet(boundLat, boundLng, anom.lat, anom.lng)
                    val boundRad = anom.getBoundaryRadiusForPlayer(boundLat, boundLng)
                    if (distFromAnom <= boundRad * 3.0) {
                        val influence = (1.0 - distFromAnom / (boundRad * 3.0)).coerceIn(0.0, 1.0)
                        val factionSign = if (anom.faction == "Infection") -1.0 else 1.0
                        densitySum += factionSign * influence * (anom.density / 100.0)
                    }
                }
                densitySum.coerceIn(-0.33, 0.33)
            } else {
                0.0
            }

            val matchedCreature = creaturesState.find { it.id == m.creatureId }
            val cSequence = matchedCreature?.sequence ?: m.originalSequence
            val hasShield = WaveMath.hasCoherenceShield(matchedCreature, cSequence)
            val effectiveDensity = if (hasShield && combinedDensity > 0.0) 0.0 else combinedDensity

            list.add(
                PrecomputedMissionState(
                    mission = m,
                    hasShield = hasShield,
                    combinedDensity = combinedDensity,
                    boundLat = boundLat,
                    boundLng = boundLng,
                    startLat = startLat,
                    startLng = startLng,
                ),
            )
        }
        list
    }

    val precomputedFontGroups = remember(baseFonts, userLat, userLng) {
        val fontsSize = baseFonts.size
        val result = ArrayList<List<PoxBaseFont>>()
        val visited = BooleanArray(fontsSize)
        val cosLat = kotlin.math.cos(Math.toRadians(userLat))

        for (i in 0 until fontsSize) {
            if (visited[i]) continue
            visited[i] = true

            val group = ArrayList<PoxBaseFont>()
            val fontA = baseFonts[i]
            group.add(fontA)

            for (j in i + 1 until fontsSize) {
                if (!visited[j]) {
                    val fontB = baseFonts[j]
                    val dLat = fontA.lat - fontB.lat
                    val dLng = (fontA.lng - fontB.lng) * cosLat
                    val distGeoSq = dLat * dLat + dLng * dLng
                    val rSumGeo = (fontA.maxRadiusFeet + fontB.maxRadiusFeet) / 364000.0 * 1.2

                    if (distGeoSq < rSumGeo * rSumGeo) {
                        visited[j] = true
                        group.add(fontB)
                    }
                }
            }
            result.add(group)
        }
        result
    }

    var lastCanvasWidth by remember { mutableStateOf(0f) }
    var lastCanvasHeight by remember { mutableStateOf(0f) }
    var pointerCount by remember { mutableIntStateOf(0) }

    val projectedFontPool = remember { List(64) { ProjectedFont() } }
    val projectedFontGroupScratch = remember { ArrayList<ProjectedFont>(64) }

    val timeProvider = { System.currentTimeMillis() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        val widthPx = if (constraints.hasBoundedWidth && constraints.maxWidth > 0) {
            constraints.maxWidth.toFloat()
        } else if (lastCanvasWidth > 0f) {
            lastCanvasWidth
        } else {
            with(density) { 360.dp.toPx() }
        }
        val heightPx = if (constraints.hasBoundedHeight && constraints.maxHeight > 0) {
            constraints.maxHeight.toFloat()
        } else if (lastCanvasHeight > 0f) {
            lastCanvasHeight
        } else {
            with(density) { 640.dp.toPx() }
        }
        val cxPx = widthPx / 2f
        val cyPx = heightPx / 2f

        fun projectPoint(lat: Double, lng: Double): Offset {
            val cosLat = kotlin.math.cos(Math.toRadians(userLat))
            val worldX = ((lng - userLng) * cosLat * 111000.0).toFloat() - (radarState?.panWorldX ?: 0f)
            val worldY = (-((lat - userLat) * 111000.0)).toFloat() - (radarState?.panWorldY ?: 0f)

            val dx = worldX * mathZoom
            val dy = worldY * mathZoom

            val rotX = cxPx + dx * cosRot - dy * sinRot
            val rotY = cyPx + dx * sinRot + dy * cosRot
            val finalY = cyPx - (cyPx - rotY) * tiltYScale

            return Offset(rotX, finalY)
        }

        // --- 1. GOOGLE FILAMENT HARDWARE Z-BUFFER 3D MAP ENGINE ---
        var filamentRenderer by remember { mutableStateOf<com.example.genpox.map.tile.filament.FilamentMapRenderer?>(null) }
        var filamentFps by remember { mutableIntStateOf(60) }

        DisposableEffect(Unit) {
            onDispose {
                filamentRenderer?.destroy()
                filamentRenderer = null
            }
        }

        val activeTilesMap by viewModel.tileStreamManager.activeTiles.collectAsStateWithLifecycle(initialValue = emptyMap())
        val activeTiles = remember(activeTilesMap) { activeTilesMap.values }

        val sectorCoords by viewModel.tileStreamManager.sectorCoordinates.collectAsStateWithLifecycle(initialValue = emptySet())

        val extrusion = radarState?.buildingExtrusion ?: 1.0f
        LaunchedEffect(filamentRenderer, extrusion) {
            filamentRenderer?.buildingExtrusion = extrusion
        }

        LaunchedEffect(filamentRenderer, activeTiles, sectorCoords, anomalies, userLat, userLng) {
            val r = filamentRenderer ?: return@LaunchedEffect
            r.isTileCached = { coord -> viewModel.tileStreamManager.isTileCached(coord) }
            r.setSceneAnchor(userLat, userLng)
            r.buildingExtrusion = extrusion
            r.setAnomalies(anomalies)
            r.setSector(sectorCoords, activeTiles)
        }

        val panX = radarState?.panWorldX ?: 0f
        val panY = radarState?.panWorldY ?: 0f
        LaunchedEffect(filamentRenderer, panX, panY, mathZoom, cameraTiltAngle, rotationAngle) {
            filamentRenderer?.updateCamera(
                newPanX = panX,
                newPanY = panY,
                newZoom = mathZoom,
                newTiltProgress = tiltProgress,
                newRotationDegrees = rotationAngle,
                newPitchAngle = cameraTiltAngle,
            )
        }

        LaunchedEffect(filamentRenderer, isPaused) {
            filamentRenderer?.isPaused = isPaused
        }

        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                android.view.SurfaceView(context).also { sv ->
                    val renderer = com.example.genpox.map.tile.filament.FilamentMapRenderer(
                        surfaceView = sv,
                        onFpsUpdated = { filamentFps = it },
                    )
                    renderer.isTileCached = { coord -> viewModel.tileStreamManager.isTileCached(coord) }
                    renderer.setSceneAnchor(userLat, userLng)
                    renderer.buildingExtrusion = extrusion
                    renderer.setAnomalies(anomalies)
                    renderer.setSector(sectorCoords, activeTiles)
                    renderer.isPaused = isPaused
                    filamentRenderer = renderer
                }
            },
            update = { sv ->
                sv.visibility = if (isPaused) android.view.View.GONE else android.view.View.VISIBLE
                filamentRenderer?.isPaused = isPaused
            },
        )

        SideEffect { if (isProfilerEnabled) profilerState.dynamicRecomps++ }
        ProfilingCanvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    lastCanvasWidth = size.width.toFloat()
                    lastCanvasHeight = size.height.toFloat()
                }
                .then(
                    if (!isPaused) {
                        Modifier
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                        pointerCount = event.changes.count { it.pressed }
                                    }
                                }
                            }
                            .pointerInput(radarState) {
                                detectTransformGestures { _, pan, zoomDelta, rotationDelta ->
                                    if (radarState != null) {
                                        if (zoomDelta != 1.0f) {
                                            val delta = kotlin.math.log2(zoomDelta) * 1.5f
                                            radarState.sliderValue = (radarState.sliderValue - delta).coerceIn(0.0f, 4.0f)
                                        }
                                        if (rotationDelta != 0.0f) {
                                            val newRot = (radarState.rotationValue + rotationDelta) % 360f
                                            radarState.rotationValue = if (newRot < 0f) newRot + 360f else newRot
                                        }

                                        val isMultiTouch = pointerCount >= 2

                                        if ((!radarState.is3DMode || isMultiTouch) && pan != Offset.Zero) {
                                            val safeZoom = radarState.mathZoom.coerceAtLeast(0.1f)
                                            val rotRad = Math.toRadians(radarState.rotationValue.toDouble())
                                            val cosR = kotlin.math.cos(rotRad).toFloat()
                                            val sinR = kotlin.math.sin(rotRad).toFloat()

                                            val pitchRad = Math.toRadians(radarState.cameraTiltAngle.toDouble())
                                            val sinPitch = kotlin.math.sin(pitchRad).toFloat().coerceIn(0.25f, 1.0f)
                                            val effectivePanY = if (radarState.is3DMode) pan.y / sinPitch else pan.y

                                            val k = 1.0f / safeZoom
                                            val deltaTargetX = -k * (pan.x * cosR + effectivePanY * sinR)
                                            val deltaTargetZ = k * (pan.x * sinR - effectivePanY * cosR)

                                            val maxPanMeters = 6000f
                                            radarState.panWorldX = (radarState.panWorldX + deltaTargetX).coerceIn(-maxPanMeters, maxPanMeters)
                                            radarState.panWorldY = (radarState.panWorldY + deltaTargetZ).coerceIn(-maxPanMeters, maxPanMeters)
                                        } else if (radarState.is3DMode && !isMultiTouch) {
                                            if (pan.y != 0f) {
                                                val tiltSensitivity = 0.20f
                                                radarState.cameraTiltAngle = (radarState.cameraTiltAngle + pan.y * tiltSensitivity).coerceIn(15.0f, 90.0f)
                                            }
                                            if (pan.x != 0f) {
                                                val rotSensitivity = 0.35f
                                                val newRot = (radarState.rotationValue - pan.x * rotSensitivity) % 360f
                                                radarState.rotationValue = if (newRot < 0f) newRot + 360f else newRot
                                            }
                                        }
                                    }
                                }
                            }
                            .pointerInput(selectedAnomalyId, anomalies) {
                                detectTapGestures { tapOffset ->
                                    var clickedAnomalyId: String? = null
                                    var minDistanceSq = Float.MAX_VALUE
                                    val hitRadiusSq = (36.dp.toPx() * 36.dp.toPx())

                                    val anomaliesSize = anomalies.size
                                    for (aIdx in 0 until anomaliesSize) {
                                        val anomaly = anomalies[aIdx]
                                        val center = projectPoint(anomaly.lat, anomaly.lng)
                                        val dx = tapOffset.x - center.x
                                        val dy = tapOffset.y - center.y
                                        val distSq = dx * dx + dy * dy

                                        if (distSq <= hitRadiusSq && distSq < minDistanceSq) {
                                            minDistanceSq = distSq
                                            clickedAnomalyId = anomaly.id
                                        }
                                    }

                                    if (clickedAnomalyId != null) {
                                        viewModel.synthManager.playBeep(880f, 0.05f, "sine")
                                        onSelectAnomaly(clickedAnomalyId)
                                    } else {
                                        if (selectedAnomalyId != null) {
                                            viewModel.synthManager.playBeep(330f, 0.05f, "sine")
                                            onSelectAnomaly("")
                                        }
                                    }
                                }
                            }
                    } else Modifier
                ),
            isProfilerEnabled = isProfilerEnabled,
            profilerState = profilerState,
        ) {
            if (isPaused) return@ProfilingCanvas
            val cx = cxPx
            val cy = cyPx
            val width = size.width
            val height = size.height
            val cosLat = kotlin.math.cos(Math.toRadians(userLat))
            val scale = 111000.0 * mathZoom
            val fontGroupsSize = precomputedFontGroups.size
            var fontPoolIdx = 0
            for (fgIdx in 0 until fontGroupsSize) {
                val fontGroup = precomputedFontGroups[fgIdx]
                projectedFontGroupScratch.clear()

                for (i in fontGroup.indices) {
                    val font = fontGroup[i]
                    val p = projectPoint(font.lat, font.lng)
                    val rotX = p.x
                    val finalY = p.y

                    val seed = font.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
                    val epsilon = 0.12f + (seed % 3) * 0.04f
                    val kLobe = 3 + (seed % 3)
                    val phiOffset = (seed % 360) * (Math.PI / 180.0)

                    val maxRadiusPx = ((font.maxRadiusFeet / 364000.0) * scale).toFloat()
                    val peakHeightPx = 120.dp.toPx()

                    // Viewport Bounding Box Culling for Base Fonts
                    if (rotX + maxRadiusPx < 0f ||
                        rotX - maxRadiusPx > width ||
                        finalY + maxRadiusPx < 0f ||
                        finalY - maxRadiusPx > height
                    ) {
                        continue
                    }

                    val baseColor = if (font.isInverted) {
                        Color(0xFF8B5CF6)
                    } else {
                        when (font.baseType) {
                            'A' -> Color(0xFF38BDF8)
                            'G' -> Color(0xFF34D399)
                            'T' -> Color(0xFFFB7185)
                            'C' -> Color(0xFFFBBF24)
                            else -> Color.Cyan
                        }
                    }

                    val elapsedMs = System.currentTimeMillis() - font.spawnTime
                    val animProgress = (elapsedMs / font.durationMs.toFloat()).coerceIn(0.0f, 1.0f)
                    val animScale = if (animProgress < 0.1f) {
                        animProgress / 0.1f
                    } else if (animProgress > 0.9f) {
                        (1.0f - animProgress) / 0.1f
                    } else {
                        1.0f
                    }

                    val flickerMultiplier = 0.85f + 0.15f * kotlin.math.sin((System.currentTimeMillis() % 1200) / 1200f * 2f * Math.PI.toFloat())

                    val pFont = if (fontPoolIdx < projectedFontPool.size) projectedFontPool[fontPoolIdx++] else ProjectedFont()
                    pFont.set(
                        font = font,
                        fx = rotX,
                        fy = finalY,
                        maxRadiusPx = maxRadiusPx,
                        peakHeightPx = peakHeightPx,
                        epsilon = epsilon,
                        kLobe = kLobe,
                        phiOffset = phiOffset,
                        baseColor = baseColor,
                        flickerMultiplier = flickerMultiplier,
                        animScale = animScale,
                    )
                    projectedFontGroupScratch.add(pFont)
                }

                val groupSize = projectedFontGroupScratch.size
                for (i in 0 until groupSize) {
                    val pFont = projectedFontGroupScratch[i]
                    val ringSteps = 5
                    for (r in 1..ringSteps) {
                        val uRing = r.toFloat() / ringSteps
                        val ringAlpha = (0.25f * (1.0f - uRing * 0.5f)) * pFont.flickerMultiplier

                        drawBaseFontRingClipped(
                            group = projectedFontGroupScratch,
                            ringIndex = i,
                            uRing = uRing,
                            ringAlpha = ringAlpha,
                            phiOffset = pFont.phiOffset,
                            rotRad = rotRad,
                            tiltYScale = tiltYScale,
                            sinTilt = sinTilt,
                            baseColor = pFont.baseColor,
                            clipIdx = 0,
                            scale = scale,
                        )
                    }

                    drawBaseFontBoundaryClipped(
                        group = projectedFontGroupScratch,
                        boundaryIndex = i,
                        phiOffset = pFont.phiOffset,
                        rotRad = rotRad,
                        tiltYScale = tiltYScale,
                        baseColor = pFont.baseColor,
                        clipIdx = 0,
                        scale = scale,
                    )
                }
            }

            // Player Marker (anchored at true geographic GPS coordinates)
            val playerCenter = projectPoint(userLat, userLng)
            drawCircle(
                color = CyberGreen,
                radius = 8.dp.toPx(),
                center = playerCenter,
            )
            drawCircle(
                color = CyberGreen.copy(alpha = 0.4f),
                radius = 16.dp.toPx(),
                center = playerCenter,
                style = Stroke(width = 2.dp.toPx()),
            )

            // P2P Peer Markers
            for (pIdx in peerLocations.indices) {
                val peerLoc = peerLocations[pIdx]
                val peerCenter = projectPoint(peerLoc.latitude, peerLoc.longitude)
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 6.dp.toPx(),
                    center = peerCenter,
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                    radius = 12.dp.toPx(),
                    center = peerCenter,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            val anomaliesSize = anomalies.size
            for (aIdx in 0 until anomaliesSize) {
                val anomaly = anomalies[aIdx]
                val anomalyColor = when (anomaly.lifecyclePhase) {
                    AnomalyLifecyclePhase.INCEPTION -> Color(0xFF22D3EE)
                    AnomalyLifecyclePhase.ACTIVE_NEXUS -> Color(0xFFEC4899)
                    AnomalyLifecyclePhase.DESTABILIZING -> Color(0xFFFBBF24)
                    AnomalyLifecyclePhase.VOID_COLLAPSE -> Color(0xFFA855F7)
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val epicenter = projectPoint(anomaly.lat, anomaly.lng)
                            val yPx = epicenter.y
                            val tMs = timeProvider()
                            val glitchOffsetPx = getGlitchOffsetX(yPx, tMs)
                            val xPx = epicenter.x + glitchOffsetPx

                            val widthPx = size.width
                            val heightPx = size.height

                            val isInside = xPx >= 0f && xPx <= widthPx && yPx >= 0f && yPx <= heightPx
                            alpha = if (isInside) 0.9f else 0.0f

                            translationX = xPx - 35.dp.toPx()
                            translationY = yPx - 22.dp.toPx()
                        }
                        .width(70.dp)
                        .border(0.5.dp, anomalyColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = anomaly.name.uppercase(),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp)),
        )
        if (isProfilerEnabled) {
            ProfilerHUDOverlay(
                state = profilerState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 90.dp),
            )
        }
    }
}

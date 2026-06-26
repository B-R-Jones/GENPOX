package com.example.genpox.ui.main

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.Creature
import com.example.genpox.data.GeneSequence
import com.example.genpox.data.HarvestMission
import com.example.genpox.data.WaveMath
import com.example.genpox.data.BuildingStructure
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.absoluteValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.SystemFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Immutable


@Composable
fun ScannerView(viewModel: MainViewModel) {
    val anomalies by viewModel.anomalies.collectAsState()
    val lat by viewModel.latitude.collectAsState()
    val lng by viewModel.longitude.collectAsState()
    val roads by viewModel.roads.collectAsState()
    val buildings by viewModel.buildings.collectAsState()
    val selectedAnomalyId by viewModel.selectedAnomalyId.collectAsState()
    val selectedAnomaly by viewModel.selectedAnomaly.collectAsState()
    val activeMissionCoords by viewModel.activeMissionCoords.collectAsState()
    val depletedAnomalyCoords by viewModel.depletedAnomalyCoords.collectAsState()
    val scannerSubTab by viewModel.scannerSubTab.collectAsState()
    val devForceAnomaly by viewModel.devForceAnomaly.collectAsState()
    var activeAnomalyTab by remember { mutableStateOf("scan") }

    val activeMissions by viewModel.activeMissions.collectAsState()
    val activeMissionsList = remember(activeMissions) { activeMissions.filter { !it.isReturned } }

    var sliderValue by remember { mutableStateOf(2.0f) }
    val zoomSteps = listOf(4.0f, 2.0f, 1.0f, 0.5f, 0.25f)
    val lowerIndex = sliderValue.toInt().coerceIn(0, 3)
    val upperIndex = lowerIndex + 1
    val fraction = sliderValue - lowerIndex
    val zoomMultiplier = zoomSteps[lowerIndex] + fraction * (zoomSteps[upperIndex] - zoomSteps[lowerIndex])

    LaunchedEffect(zoomMultiplier) {
        viewModel.updateZoom(zoomMultiplier)
    }

    var zoomExpanded by remember { mutableStateOf(false) }
    var rotationValue by remember { mutableStateOf(0f) }
    var rotationExpanded by remember { mutableStateOf(false) }

    // Auto-collapse zoom and rotation expanded if sub-tab changes from radar
    LaunchedEffect(scannerSubTab) {
        if (scannerSubTab != "radar") {
            zoomExpanded = false
            rotationExpanded = false
        }
    }

    // Auto-redirect from missions tab if there are no active missions
    LaunchedEffect(activeMissionsList, scannerSubTab) {
        if (activeMissionsList.isEmpty() && scannerSubTab == "missions") {
            viewModel.setScannerSubTab("list")
        }
    }

    val isHarvesting = remember(activeMissionCoords, selectedAnomaly) {
        selectedAnomaly?.let { anomaly ->
            activeMissionCoords.contains("${anomaly.lat},${anomaly.lng}")
        } ?: false
    }

    val subTabs = remember(activeMissionsList) {
        val list = mutableListOf(
            PoxSubTab("radar", "RADAR") { iconColor ->
                Canvas(modifier = Modifier.size(24.dp)) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val strokeW = 1.5.dp.toPx()

                    drawCircle(iconColor, radius = w * 0.38f, center = center, style = Stroke(width = strokeW))
                    drawCircle(iconColor, radius = w * 0.08f, center = center)

                    drawLine(iconColor, Offset(center.x - w * 0.45f, center.y), Offset(center.x - w * 0.18f, center.y), strokeWidth = strokeW)
                    drawLine(iconColor, Offset(center.x + w * 0.18f, center.y), Offset(center.x + w * 0.45f, center.y), strokeWidth = strokeW)
                    drawLine(iconColor, Offset(center.x, center.y - h * 0.45f), Offset(center.x, center.y - h * 0.18f), strokeWidth = strokeW)
                    drawLine(iconColor, Offset(center.x, center.y + h * 0.18f), Offset(center.x, center.y + h * 0.45f), strokeWidth = strokeW)
                }
            },
            PoxSubTab("list", "LIST") { iconColor ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text("???", color = iconColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, lineHeight = 7.sp)
                    Text("???", color = iconColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, lineHeight = 7.sp)
                    Text("???", color = iconColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, lineHeight = 7.sp)
                }
            },
            PoxSubTab("forecast", "FORECAST") { iconColor ->
                WireframeCloudIcon(color = iconColor)
            }
        )
        if (activeMissionsList.isNotEmpty()) {
            list.add(
                PoxSubTab("missions", "MISSIONS") { iconColor ->
                    WireframePickaxeIcon(color = iconColor)
                }
            )
        }
        list
    }

    PoxTabFrame(
        flavorTitle = "G.E.N. P.O.X. ANOMALY LOG V1.2",
        statusText = if (selectedAnomalyId != null) "LOCKED" else "ONLINE",
        statusColor = if (selectedAnomalyId != null) Color.Red else CyberGreen,
        headerTitle = "NEARBY FREQUENCY LOCATOR",
        descriptionText = "Analyze the detailed telemetry signature of detected local anomalies.",
        isScrollable = false,
        subTabs = subTabs,
        activeSubTab = scannerSubTab,
        onSubTabClick = { id, _ -> viewModel.setScannerSubTab(id) },
        viewModel = viewModel,
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedAnomalyId != null && selectedAnomaly != null) {
                    LockedAnomalyDetails(
                        anomaly = selectedAnomaly!!,
                        viewModel = viewModel,
                        userLat = lat,
                        userLng = lng,
                        activeMissionCoords = activeMissionCoords,
                        depletedAnomalyCoords = depletedAnomalyCoords,
                        activeAnomalyTab = activeAnomalyTab,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    when (scannerSubTab) {
                        "radar" -> {
                            HolographicRadarScanner(
                                viewModel = viewModel,
                                anomalies = anomalies,
                                userLat = lat,
                                userLng = lng,
                                roads = roads,
                                buildings = buildings,
                                zoomMultiplier = zoomMultiplier,
                                rotationAngle = rotationValue,
                                selectedAnomalyId = selectedAnomalyId,
                                onSelectAnomaly = { id -> viewModel.setSelectedAnomalyId(id) },
                                zoomExpanded = zoomExpanded,
                                modifier = Modifier.fillMaxSize()
                            )

                            val zoomExpansionFraction by animateFloatAsState(
                                targetValue = if (zoomExpanded) 1f else 0f,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                label = "zoom_expansion"
                            )
                            val rotationExpansionFraction by animateFloatAsState(
                                targetValue = if (rotationExpanded) 1f else 0f,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                label = "rotation_expansion"
                            )

                            val animatedZoomEnd = (70f + 46f * rotationExpansionFraction - 49f * zoomExpansionFraction).dp
                            val animatedZoomBottom = 70.dp

                            val animatedRotEnd = (16f + 100f * zoomExpansionFraction).dp
                            val animatedRotBottom = 70.dp

                            ZoomScrollCircle(
                                sliderValue = sliderValue,
                                onValueChange = { sliderValue = it },
                                zoomMultiplier = zoomMultiplier,
                                zoomExpanded = zoomExpanded,
                                onToggleExpand = {
                                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                    zoomExpanded = !zoomExpanded
                                    if (zoomExpanded) {
                                        rotationExpanded = false
                                    }
                                },
                                expansionFraction = zoomExpansionFraction,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = animatedZoomBottom, end = animatedZoomEnd)
                            )

                            RotationScrollCircle(
                                rotationAngle = rotationValue,
                                onValueChange = { rotationValue = it },
                                rotationExpanded = rotationExpanded,
                                onToggleExpand = {
                                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                    rotationExpanded = !rotationExpanded
                                    if (rotationExpanded) {
                                        zoomExpanded = false
                                    }
                                },
                                expansionFraction = rotationExpansionFraction,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = animatedRotBottom, end = animatedRotEnd)
                            )
                        }
                        "missions" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (activeMissionsList.isNotEmpty()) {
                                    ActiveDeployedSequencesList(
                                        activeMissions = activeMissionsList,
                                        onSelectAnomalyByLatLng = { mLat, mLng ->
                                            val anomaly = anomalies.find { Math.abs(it.lat - mLat) < 0.0001 && Math.abs(it.lng - mLng) < 0.0001 }
                                            if (anomaly != null) {
                                                viewModel.setSelectedAnomalyId(anomaly.id)
                                            }
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                        "forecast" -> {
                            ScannerWaveForecastView(viewModel = viewModel)
                        }
                        else -> { // "list"
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (devForceAnomaly) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.synthManager.playBeep(440f, 0.15f, "sawtooth")
                                                viewModel.recallAllActiveMissions()
                                            },
                                            modifier = Modifier.weight(1f).requiredHeight(36.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFEA580C),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "⚡ DEV: RECALL ALL",
                                                style = Typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.synthManager.playBeep(440f, 0.15f, "sawtooth")
                                                viewModel.refreshMap()
                                            },
                                            modifier = Modifier.weight(1f).requiredHeight(36.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFEA580C),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "⚡ DEV: REFRESH MAP",
                                                style = Typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (anomalies.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "NO ANOMALIES IN RANGE.",
                                                style = Typography.bodySmall,
                                                color = CyberGreenDim
                                            )
                                        }
                                    } else {
                                        anomalies.take(5).forEach { anomaly ->
                                            AnomalyItemRow(
                                                anomaly = anomaly,
                                                userLat = lat,
                                                userLng = lng,
                                                viewModel = viewModel,
                                                onClick = { viewModel.setSelectedAnomalyId(anomaly.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isHarvesting) {
                val factionColor = when (selectedAnomaly?.faction) {
                    "Infection" -> Color(0xFFEF4444)
                    "Mech" -> Color(0xFFFBBF24)
                    "Parasite" -> Color(0xFFA855F7)
                    else -> Color(0xFF22D3EE)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, factionColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { activeAnomalyTab = "scan" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeAnomalyTab == "scan") factionColor else Color.Transparent,
                            contentColor = if (activeAnomalyTab == "scan") (if (selectedAnomaly?.faction == "Parasite") Color.White else Color.Black) else factionColor.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "SEQUENCE SCAN",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { activeAnomalyTab = "logs" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeAnomalyTab == "logs") factionColor else Color.Transparent,
                            contentColor = if (activeAnomalyTab == "logs") (if (selectedAnomaly?.faction == "Parasite") Color.White else Color.Black) else factionColor.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "TELEMETRY LOGS",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

class ProjectedAnomaly {
    var ax: Float = 0f
    var ay: Float = 0f
    var rMax: Float = 0f
    var factionColor: Color = Color.Cyan
    var contourAlpha: Float = 0f
    val path: androidx.compose.ui.graphics.Path = androidx.compose.ui.graphics.Path()
    var r0Pixels: Double = 0.0
    var epsilon: Double = 0.0
    var k: Int = 0
    var phi: Double = 0.0
    val verticesX = FloatArray(37)
    val verticesY = FloatArray(37)
    var numVertices: Int = 0
}

class DirectionTracker {
    var lastValue: Float = 0f
    var isMovingUp: Boolean = false
    fun update(newValue: Float): Boolean {
        if (newValue != lastValue) {
            isMovingUp = newValue < lastValue
            lastValue = newValue
        }
        return isMovingUp
    }
}

fun drawClippedLine(
    canvasDrawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    x1: Float, y1: Float, x2: Float, y2: Float,
    minX: Float, minY: Float, maxX: Float, maxY: Float,
    color: Color,
    strokeWidth: Float
) {
    val INSIDE = 0 // 0000
    val LEFT = 1   // 0001
    val RIGHT = 2  // 0010
    val BOTTOM = 4 // 0100
    val TOP = 8    // 1000

    fun computeCode(x: Float, y: Float): Int {
        var code = INSIDE
        if (x < minX) code = code or LEFT
        else if (x > maxX) code = code or RIGHT
        if (y < minY) code = code or BOTTOM
        else if (y > maxY) code = code or TOP
        return code
    }

    var ix1 = x1
    var iy1 = y1
    var ix2 = x2
    var iy2 = y2

    var code1 = computeCode(ix1, iy1)
    var code2 = computeCode(ix2, iy2)
    var accept = false

    while (true) {
        if (code1 == 0 && code2 == 0) {
            accept = true
            break
        } else if ((code1 and code2) != 0) {
            break
        } else {
            val codeOut = if (code1 != 0) code1 else code2
            var x = 0f
            var y = 0f

            if ((codeOut and TOP) != 0) {
                x = ix1 + (ix2 - ix1) * (maxY - iy1) / (iy2 - iy1)
                y = maxY
            } else if ((codeOut and BOTTOM) != 0) {
                x = ix1 + (ix2 - ix1) * (minY - iy1) / (iy2 - iy1)
                y = minY
            } else if ((codeOut and RIGHT) != 0) {
                y = iy1 + (iy2 - iy1) * (maxX - ix1) / (ix2 - ix1)
                x = maxX
            } else if ((codeOut and LEFT) != 0) {
                y = iy1 + (iy2 - iy1) * (minX - ix1) / (ix2 - ix1)
                x = minX
            }

            if (codeOut == code1) {
                ix1 = x
                iy1 = y
                code1 = computeCode(ix1, iy1)
            } else {
                ix2 = x
                iy2 = y
                code2 = computeCode(ix2, iy2)
            }
        }
    }

    if (accept) {
        canvasDrawScope.drawLine(
            color = color,
            start = Offset(ix1, iy1),
            end = Offset(ix2, iy2),
            strokeWidth = strokeWidth
        )
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGroupContoursClipped(
    group: List<ProjectedAnomaly>,
    groupColor: androidx.compose.ui.graphics.Color,
    groupAlpha: Float,
    alphaFactor: Float,
    groupStartPathIndex: Int,
    contourPathPool: List<androidx.compose.ui.graphics.Path>,
    index: Int,
    clipIdx: Int
) {
    if (clipIdx >= group.size) {
        val pathIdx = groupStartPathIndex + index
        val path = if (pathIdx < contourPathPool.size) contourPathPool[pathIdx] else null
        if (path != null) {
            drawPath(
                path = path,
                color = groupColor.copy(alpha = groupAlpha * alphaFactor * 0.8f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
            )
        }
        return
    }
    if (clipIdx == index) {
        drawGroupContoursClipped(
            group, groupColor, groupAlpha, alphaFactor,
            groupStartPathIndex, contourPathPool, index, clipIdx + 1
        )
    } else {
        val clipPath = group[clipIdx].path
        clipPath(clipPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
            drawGroupContoursClipped(
                group, groupColor, groupAlpha, alphaFactor,
                groupStartPathIndex, contourPathPool, index, clipIdx + 1
            )
        }
    }
}

@Immutable
class StableRoads(val list: List<List<Pair<Double, Double>>>)


@Immutable
class StableBuildings(val list: List<BuildingStructure>)


@Composable
private fun StaticMapLayer(
    roads: StableRoads,
    sortedBuildings: StableBuildings,
    localMapCenterLat: Double,
    localMapCenterLng: Double,
    mathZoom: Float,
    tiltZoomFactor: Float,
    tiltProgress: Float,
    tiltYScale: Float,
    playerXUn: Float,
    playerYUn: Float,
    scale: Double,
    cosLat: Double,
    cxPx: Float,
    cyPx: Float,
    cosRot: Float,
    sinRot: Float,
    isProfilerEnabled: Boolean,
    profilerState: MapProfilerState,
    timeProvider: () -> Long
) {
    val staticBuildingPath = remember { Path() }
    val projectedXBuffer = remember { FloatArray(256) }
    val projectedYBuffer = remember { FloatArray(256) }

    fun projectX(lat: Double, lng: Double): Float {
        val dLng = (lng - localMapCenterLng) * cosLat
        val xUn = cxPx + (dLng * scale).toFloat()
        val dLat = lat - localMapCenterLat
        val yUn = cyPx - (dLat * scale).toFloat()
        val dx = xUn - cxPx
        val dy = yUn - cyPx
        return cxPx + dx * cosRot - dy * sinRot
    }

    fun projectY(lat: Double, lng: Double): Float {
        val dLng = (lng - localMapCenterLng) * cosLat
        val xUn = cxPx + (dLng * scale).toFloat()
        val dLat = lat - localMapCenterLat
        val yUn = cyPx - (dLat * scale).toFloat()
        val dx = xUn - cxPx
        val dy = yUn - cyPx
        val rotY = cyPx + dx * sinRot + dy * cosRot
        return cyPx - (cyPx - rotY) * tiltYScale
    }

    SideEffect { if (isProfilerEnabled) profilerState.staticRecomps++ }
    ProfilingCanvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val time = timeProvider()
                val glitchActive = (time % 1600 < 160)
                translationX = if (glitchActive) {
                    (kotlin.math.sin(time * 0.05f) * 4f).toFloat()
                } else {
                    0f
                }
            },
        isProfilerEnabled = isProfilerEnabled,
        profilerState = profilerState
    ) {
        val maxUnTiltedDist = kotlin.math.sqrt((cxPx * cxPx + (cyPx / tiltYScale.coerceAtLeast(0.01f)) * (cyPx / tiltYScale.coerceAtLeast(0.01f))).toDouble())
        val maxGeoDist = 1.5 * maxUnTiltedDist / scale
        val latMin = localMapCenterLat - maxGeoDist
        val latMax = localMapCenterLat + maxGeoDist
        val lngMin = localMapCenterLng - (maxGeoDist / cosLat)
        val lngMax = localMapCenterLng + (maxGeoDist / cosLat)

        // Draw Vector Roads
        val roadList = roads.list
        val roadsSize = roadList.size
        for (i in 0 until roadsSize) {
            val road = roadList[i]
            val roadSize = road.size
            for (j in 0 until roadSize - 1) {
                val pt1 = road[j]
                val pt2 = road[j + 1]

                // Geographic bounding box culling for the segment
                val segMinLat = if (pt1.first < pt2.first) pt1.first else pt2.first
                val segMaxLat = if (pt1.first > pt2.first) pt1.first else pt2.first
                val segMinLng = if (pt1.second < pt2.second) pt1.second else pt2.second
                val segMaxLng = if (pt1.second > pt2.second) pt1.second else pt2.second

                if (segMaxLat < latMin || segMinLat > latMax || segMinLng < lngMin || segMinLng > lngMax) {
                    continue
                }

                val p1x = projectX(pt1.first, pt1.second)
                val p1y = projectY(pt1.first, pt1.second)
                val p2x = projectX(pt2.first, pt2.second)
                val p2y = projectY(pt2.first, pt2.second)

                drawClippedLine(
                    canvasDrawScope = this,
                    x1 = p1x, y1 = p1y, x2 = p2x, y2 = p2y,
                    minX = -100f, minY = -100f,
                    maxX = size.width + 100f, maxY = size.height + 100f,
                    color = Color(0xFFFFB300).copy(alpha = 0.34f),
                    strokeWidth = 2.0f
                )
            }
        }

        // Draw Vector Buildings
        val zoomFade = (1.0f - ((mathZoom - 1.5f) / 2.0f)).coerceIn(0.0f, 1.0f)
        val buildingList = sortedBuildings.list
        val buildingsSize = buildingList.size

        for (bIdx in 0 until buildingsSize) {
            val building = buildingList[bIdx]
            val points = building.points
            val numPts = points.size.coerceAtMost(256)
            if (numPts < 2) continue

            // Geographic bounding box culling for the building
            var bMinLat = Double.MAX_VALUE
            var bMaxLat = -Double.MAX_VALUE
            var bMinLng = Double.MAX_VALUE
            var bMaxLng = -Double.MAX_VALUE
            for (i in 0 until numPts) {
                val pt = points[i]
                if (pt.lat < bMinLat) bMinLat = pt.lat
                if (pt.lat > bMaxLat) bMaxLat = pt.lat
                if (pt.lng < bMinLng) bMinLng = pt.lng
                if (pt.lng > bMaxLng) bMaxLng = pt.lng
            }

            if (bMaxLat < latMin || bMinLat > latMax || bMaxLng < lngMin || bMinLng > lngMax) {
                continue
            }

            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE

            for (i in 0 until numPts) {
                val pt = points[i]
                val px = projectX(pt.lat, pt.lng)
                val py = projectY(pt.lat, pt.lng)
                projectedXBuffer[i] = px
                projectedYBuffer[i] = py

                if (px < minX) minX = px
                if (px > maxX) maxX = px
                if (py < minY) minY = py
                if (py > maxY) maxY = py
            }

            val widthPx = maxX - minX
            val heightPx = maxY - minY
            val maxDim = maxOf(widthPx, heightPx)

            // LOD Culling & Fade-out
            val lodFade = if (mathZoom >= 1.0f) {
                if (maxDim < 4.5f) continue
                ((maxDim - 4.5f) / 4.5f).coerceIn(0.0f, 1.0f)
            } else {
                if (maxDim < 2.0f) continue
                ((maxDim - 2.0f) / 3.0f).coerceIn(0.0f, 1.0f)
            }

            val baseHash = points[0].lat.hashCode() + points[0].lng.hashCode()
            val heightLevel = 1 + (kotlin.math.abs(baseHash) % 4)
            
            val extX = 0f
            val extY = (-2.0f * heightLevel).dp.toPx() * (1.0f / mathZoom.coerceAtLeast(0.1f))
            
            val maxExt = 30.dp.toPx()
            val finalExtX = extX.coerceIn(-maxExt, maxExt) * tiltProgress
            val finalExtY = extY.coerceIn(-maxExt, maxExt) * tiltProgress

            val minXWithExt = minOf(minX, minX + finalExtX)
            val maxXWithExt = maxOf(maxX, maxX + finalExtX)
            val minYWithExt = minOf(minY, minY + finalExtY)
            val maxYWithExt = maxOf(maxY, maxY + finalExtY)

            val margin = 50f
            if (maxXWithExt < -margin || minXWithExt > size.width + margin ||
                maxYWithExt < -margin || minYWithExt > size.height + margin) {
                continue
            }

            val buildingColor = if (building.isFallback) Color(0xFFFF00FF) else CyberGreen
            val finalAlphaMultiplier = zoomFade * lodFade

            if (finalExtX == 0f && finalExtY == 0f || maxDim < 12.dp.toPx()) {
                // Simplified 2D flat building rendering (highly optimized for flat zoom levels / small dimensions)
                staticBuildingPath.reset()
                staticBuildingPath.moveTo(projectedXBuffer[0], projectedYBuffer[0])
                for (i in 1 until numPts) {
                    staticBuildingPath.lineTo(projectedXBuffer[i], projectedYBuffer[i])
                }
                staticBuildingPath.close()
                drawPath(path = staticBuildingPath, color = Color.Black, style = Fill)
                drawPath(
                    path = staticBuildingPath,
                    color = buildingColor.copy(alpha = 0.12f * finalAlphaMultiplier),
                    style = Fill
                )
                drawPath(
                    path = staticBuildingPath,
                    color = buildingColor.copy(alpha = 0.5f * finalAlphaMultiplier),
                    style = Stroke(width = 1f)
                )
            } else {
                // Full 3D holographic building rendering
                // 1. Draw solid black mask
                staticBuildingPath.reset()
                staticBuildingPath.moveTo(projectedXBuffer[0], projectedYBuffer[0])
                for (i in 1 until numPts) {
                    staticBuildingPath.lineTo(projectedXBuffer[i], projectedYBuffer[i])
                }
                staticBuildingPath.close()
                drawPath(path = staticBuildingPath, color = Color.Black, style = Fill)

                for (i in 0 until numPts - 1) {
                    val ax = projectedXBuffer[i]
                    val ay = projectedYBuffer[i]
                    val bx = projectedXBuffer[i + 1]
                    val by = projectedYBuffer[i + 1]
                    val axTop = ax + finalExtX
                    val ayTop = ay + finalExtY
                    val bxTop = bx + finalExtX
                    val byTop = by + finalExtY
                    staticBuildingPath.reset()
                    staticBuildingPath.moveTo(ax, ay)
                    staticBuildingPath.lineTo(bx, by)
                    staticBuildingPath.lineTo(bxTop, byTop)
                    staticBuildingPath.lineTo(axTop, ayTop)
                    staticBuildingPath.close()
                    drawPath(path = staticBuildingPath, color = Color.Black, style = Fill)
                }

                staticBuildingPath.reset()
                val mRx0 = projectedXBuffer[0] + finalExtX
                val mRy0 = projectedYBuffer[0] + finalExtY
                staticBuildingPath.moveTo(mRx0, mRy0)
                for (i in 1 until numPts) {
                    staticBuildingPath.lineTo(projectedXBuffer[i] + finalExtX, projectedYBuffer[i] + finalExtY)
                }
                staticBuildingPath.close()
                drawPath(path = staticBuildingPath, color = Color.Black, style = Fill)

                // 2. Draw standard translucent holographic fills & outlines
                // A. Draw base fill & outline
                staticBuildingPath.reset()
                staticBuildingPath.moveTo(projectedXBuffer[0], projectedYBuffer[0])
                for (i in 1 until numPts) {
                    staticBuildingPath.lineTo(projectedXBuffer[i], projectedYBuffer[i])
                }
                staticBuildingPath.close()

                drawPath(
                    path = staticBuildingPath,
                    color = buildingColor.copy(alpha = 0.04f * finalAlphaMultiplier),
                    style = Fill
                )
                drawPath(
                    path = staticBuildingPath,
                    color = buildingColor.copy(alpha = 0.15f * finalAlphaMultiplier),
                    style = Stroke(width = 0.75f)
                )

                // B. Draw side walls
                for (i in 0 until numPts - 1) {
                    val ax = projectedXBuffer[i]
                    val ay = projectedYBuffer[i]
                    val bx = projectedXBuffer[i + 1]
                    val by = projectedYBuffer[i + 1]
                    
                    val axTop = ax + finalExtX
                    val ayTop = ay + finalExtY
                    val bxTop = bx + finalExtX
                    val byTop = by + finalExtY

                    staticBuildingPath.reset()
                    staticBuildingPath.moveTo(ax, ay)
                    staticBuildingPath.lineTo(bx, by)
                    staticBuildingPath.lineTo(bxTop, byTop)
                    staticBuildingPath.lineTo(axTop, ayTop)
                    staticBuildingPath.close()

                    drawPath(
                        path = staticBuildingPath,
                        color = buildingColor.copy(alpha = 0.03f * finalAlphaMultiplier),
                        style = Fill
                    )
                    drawPath(
                        path = staticBuildingPath,
                        color = buildingColor.copy(alpha = 0.20f * finalAlphaMultiplier),
                        style = Stroke(width = 0.75f)
                    )
                }

                // C. Draw top roof fill & outline
                staticBuildingPath.reset()
                val rx0 = projectedXBuffer[0] + finalExtX
                val ry0 = projectedYBuffer[0] + finalExtY
                staticBuildingPath.moveTo(rx0, ry0)
                for (i in 1 until numPts) {
                    staticBuildingPath.lineTo(projectedXBuffer[i] + finalExtX, projectedYBuffer[i] + finalExtY)
                }
                staticBuildingPath.close()

                drawPath(
                    path = staticBuildingPath,
                    color = buildingColor.copy(alpha = 0.10f * finalAlphaMultiplier),
                    style = Fill
                )
                drawPath(
                    path = staticBuildingPath,
                    color = buildingColor.copy(alpha = 0.45f * finalAlphaMultiplier),
                    style = Stroke(width = 1f)
                )
            }
        }
    }
}

@Composable
fun HolographicRadarScanner(
    viewModel: MainViewModel,
    anomalies: List<PoxAnomaly>,
    userLat: Double,
    userLng: Double,
    roads: List<List<Pair<Double, Double>>>,
    buildings: List<BuildingStructure>,
    zoomMultiplier: Float,
    rotationAngle: Float,
    selectedAnomalyId: String?,
    onSelectAnomaly: (String) -> Unit,
    zoomExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isProfilerEnabled by viewModel.isProfilerEnabled.collectAsState()
    val profilerState = viewModel.profilerState
    FrameTimeMonitor(state = profilerState, enabled = isProfilerEnabled)
    SideEffect { if (isProfilerEnabled) profilerState.hudRecomps++ }

    val mathZoom = zoomMultiplier * 0.625f
    val tiltProgress = ((0.35f - zoomMultiplier) / (0.35f - 0.26f)).coerceIn(0.0f, 1.0f)
    val tiltYScale = 1.0f - 0.90f * tiltProgress
    val tiltZoomFactor = 1.0f + 2.0f * tiltProgress
    val rotRad = Math.toRadians(rotationAngle.toDouble())
    val cosRot = kotlin.math.cos(rotRad).toFloat()
    val sinRot = kotlin.math.sin(rotRad).toFloat()
    val directionTracker = remember { DirectionTracker() }
    val buildingPath = remember { Path() }
    val sortedBuildings = remember(buildings) {
        buildings.sortedByDescending { building ->
            building.points.firstOrNull()?.lat ?: 0.0
        }
    }
    val activeMissions by viewModel.activeMissions.collectAsState()
    val geometryCache = remember { mutableMapOf<String, CreatureGeometry>() }
    val trackedMissionId by viewModel.trackedMissionId.collectAsState()
    val trackedMission = remember(activeMissions, trackedMissionId) {
        activeMissions.find { it.id == trackedMissionId && !it.isReturned }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "scanline_sweep")
    val scanlineFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanline"
    )

    val contourFlowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "contour_flow"
    )

    val activeMissionsList = remember(activeMissions) { activeMissions.filter { !it.isReturned } }
    val creaturesState by viewModel.creatures.collectAsState()

    // Precompute active mission parameters at recomposition boundaries using remember
    val precomputedMissions = remember(activeMissionsList, anomalies, creaturesState) {
        val list = ArrayList<PrecomputedMissionState>(activeMissionsList.size)
        val activeSize = activeMissionsList.size
        val anomaliesSize = anomalies.size
        for (i in 0 until activeSize) {
            val m = activeMissionsList[i]
            
            // Find matched anomaly using flat loop
            var matchedAnomaly: PoxAnomaly? = null
            for (j in 0 until anomaliesSize) {
                val anom = anomalies[j]
                if (Math.abs(anom.lat - m.lat) < 0.0001 && Math.abs(anom.lng - m.lng) < 0.0001) {
                    matchedAnomaly = anom
                    break
                }
            }
            
            // Geographic boundary and start coordinates calculation
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

            // Combined density calculations
            val combinedDensity = if (matchedAnomaly != null) {
                var densitySum = 0.0
                for (j in 0 until anomaliesSize) {
                    val anom = anomalies[j]
                    val distFromAnom = calculateDistanceInFeet(boundLat, boundLng, anom.lat, anom.lng)
                    val boundRad = anom.getBoundaryRadiusForPlayer(boundLat, boundLng)
                    if (distFromAnom <= boundRad) {
                        val seed = anom.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
                        val phi = (seed % 360) * (Math.PI / 180.0)
                        val omega = 0.02
                        val alpha = 0.002
                        val waveTerm = kotlin.math.cos(omega * distFromAnom + phi) * kotlin.math.exp(-alpha * distFromAnom)
                        densitySum += anom.density * waveTerm
                    }
                }
                densitySum
            } else {
                0.0
            }

            // Creature coherence shield verification
            var creature: Creature? = null
            for (cIdx in 0 until creaturesState.size) {
                val c = creaturesState[cIdx]
                if (c.id == m.creatureId) {
                    creature = c
                    break
                }
            }
            val hasShield = hasCoherenceShield(creature, m.originalSequence)

            list.add(
                PrecomputedMissionState(
                    mission = m,
                    hasShield = hasShield,
                    combinedDensity = combinedDensity,
                    boundLat = boundLat,
                    boundLng = boundLng,
                    startLat = startLat,
                    startLng = startLng
                )
            )
        }
        list
    }
    
    // Reusable Float/Double array buffers for 3D coordinate projection
    val maxVertices = 256
    val projectedXBuffer = remember { FloatArray(maxVertices) }
    val projectedYBuffer = remember { FloatArray(maxVertices) }
    val depthZBuffer = remember { DoubleArray(maxVertices) }

    val projectedInnerXBuffer = remember { FloatArray(maxVertices) }
    val projectedInnerYBuffer = remember { FloatArray(maxVertices) }
    val depthInnerZBuffer = remember { DoubleArray(maxVertices) }

    // Pre-allocated Paint objects to avoid JNI and heap allocations inside DrawScope
    val textSizePx = with(density) { 7.dp.toPx() }
    val textPaints = remember(density) {
        val colors = mapOf(
            "Infection" to Color.Red.toArgb(),
            "Mech" to Color.Yellow.toArgb(),
            "Parasite" to Color(0xFFA855F7).toArgb(),
            "Default" to Color.Cyan.toArgb()
        )
        colors.mapValues { (_, argb) ->
            android.graphics.Paint().apply {
                color = argb
                textSize = textSizePx
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
            }
        }
    }

    val shadowTextPaint = remember(density) {
        android.graphics.Paint().apply {
            color = Color.Black.copy(alpha = 0.8f).toArgb()
            textSize = textSizePx
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val timeState = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeMissionsList.isNotEmpty()) {
        if (activeMissionsList.isNotEmpty()) {
            while (true) {
                withFrameMillis {
                    timeState.value = System.currentTimeMillis()
                }
            }
        }
    }

    // Stable time provider lambda to avoid recomposing the parent body on every frame tick
    val timeProvider = remember { { timeState.value } }

    var localMapCenterLat = userLat
    var localMapCenterLng = userLng

    val selectedAnomaly = anomalies.find { it.id == selectedAnomalyId }

    if (trackedMission != null) {
        // Read timeState.value directly to force parent recomposition ONLY during active target tracking
        val currentTimeMs = timeState.value
        val tm = trackedMission
        val dLatAnom = userLat - tm.lat
        val dLngAnom = (userLng - tm.lng) * Math.cos(Math.toRadians(userLat))
        val distGeo = kotlin.math.sqrt(dLatAnom * dLatAnom + dLngAnom * dLngAnom)

        val tmMatchedAnomaly = anomalies.find { Math.abs(it.lat - tm.lat) < 0.0001 && Math.abs(it.lng - tm.lng) < 0.0001 }
        val boundaryRadius = tmMatchedAnomaly?.getBoundaryRadiusForPlayer(userLat, userLng)
            ?: (tm.dispatchDistance / (1.0 - tm.stalledDepth / 100.0).coerceAtLeast(0.01))

        val rBoundGeo = boundaryRadius / 111000.0
        val tGeo = if (distGeo > 0.0) (rBoundGeo / distGeo).coerceIn(0.0, 1.0) else 0.0
        val boundLat = tm.lat + tGeo * (userLat - tm.lat)
        val boundLng = tm.lng + tGeo * (userLng - tm.lng)

        val tStall = (tm.stalledDepth / 100.0).toDouble()
        val startLat = boundLat + tStall * (tm.lat - boundLat)
        val startLng = boundLng + tStall * (tm.lng - boundLng)

        val dTravel = tm.travelDuration.toFloat()
        val dDescent = tm.descentDuration.toFloat()
        val dHarvest = tm.harvestDuration.toFloat()
        val dAscent = tm.ascentDuration.toFloat()
        val dReturn = tm.transitBackDuration.toFloat()

        val tmCombinedDensity = if (tmMatchedAnomaly != null) {
            var densitySum = 0.0
            anomalies.forEach { anom ->
                val distFromAnom = calculateDistanceInFeet(boundLat, boundLng, anom.lat, anom.lng)
                val boundRad = anom.getBoundaryRadiusForPlayer(boundLat, boundLng)
                if (distFromAnom <= boundRad) {
                    val seed = anom.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
                    val phi = (seed % 360) * (Math.PI / 180.0)
                    val omega = 0.02
                    val alpha = 0.002
                    val waveTerm = kotlin.math.cos(omega * distFromAnom + phi) * kotlin.math.exp(-alpha * distFromAnom)
                    densitySum += anom.density * waveTerm
                }
            }
            densitySum
        } else {
            0.0
        }

        val tmWave = WaveMath.getDailyWaveConfig(currentTimeMs)
        val tmPhaseFraction = tmWave.lunarAge / WaveMath.LUNAR_MONTH_DAYS
        val tmLunarPhaseScale = (1.0 - kotlin.math.cos(tmPhaseFraction * 2.0 * Math.PI)) / 2.0
        val tmDensityShift = 0.2 * (tmLunarPhaseScale - 0.5)
        val tmEffectiveDensity = (tmCombinedDensity + tmDensityShift).coerceIn(-0.33, 0.33)

        val tmCreature = viewModel.creatures.value.find { it.id == tm.creatureId }
        val tmHasShield = hasCoherenceShield(tmCreature, tm.originalSequence)
        val finalAnomalyDensity = if (tmHasShield && tmEffectiveDensity > 0.0) 0.0 else tmEffectiveDensity

        val elapsedSec = (currentTimeMs - tm.startTime) / 1000f

        if (elapsedSec < dTravel) {
            val p = (elapsedSec / dTravel.coerceAtLeast(1f))
            val pWarped = p.toDouble()
            localMapCenterLat = userLat + pWarped * (boundLat - userLat)
            localMapCenterLng = userLng + pWarped * (boundLng - userLng)
        } else if (elapsedSec < dTravel + dDescent) {
            val p = ((elapsedSec - dTravel) / dDescent.coerceAtLeast(1f))
            val pWarped = warpProgress(p, finalAnomalyDensity.toFloat()).toDouble()
            localMapCenterLat = boundLat + pWarped * (startLat - boundLat)
            localMapCenterLng = boundLng + pWarped * (startLng - boundLng)
        } else if (elapsedSec < dTravel + dDescent + dHarvest) {
            localMapCenterLat = startLat
            localMapCenterLng = startLng
        } else if (elapsedSec < dTravel + dDescent + dHarvest + dAscent) {
            val p = ((elapsedSec - dTravel - dDescent - dHarvest) / dAscent.coerceAtLeast(1f))
            val pWarped = warpProgress(p, -finalAnomalyDensity.toFloat()).toDouble()
            localMapCenterLat = startLat + pWarped * (boundLat - startLat)
            localMapCenterLng = startLng + pWarped * (boundLng - startLng)
        } else if (elapsedSec < dTravel + dDescent + dHarvest + dAscent + dReturn) {
            val p = ((elapsedSec - dTravel - dDescent - dHarvest - dAscent) / dReturn.coerceAtLeast(1f))
            val pWarped = p.toDouble()
            localMapCenterLat = boundLat + pWarped * (userLat - boundLat)
            localMapCenterLng = boundLng + pWarped * (userLng - boundLng)
        }
    } else if (selectedAnomaly != null) {
        localMapCenterLat = selectedAnomaly.lat
        localMapCenterLng = selectedAnomaly.lng
    }

    val rawMapCenterLat = localMapCenterLat
    val rawMapCenterLng = localMapCenterLng
    if (trackedMission == null && selectedAnomaly == null) {
        localMapCenterLat = rawMapCenterLat + (userLat - rawMapCenterLat) * tiltProgress.toDouble()
        localMapCenterLng = rawMapCenterLng + (userLng - rawMapCenterLng) * tiltProgress.toDouble()
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val width = maxWidth
        val height = maxHeight
        val cxDp = width / 2
        val cyDp = height / 2
        val maxRDp = minOf(width, height) / 2 * 0.9f

        val heightPx = with(density) { height.toPx() }
        val widthPx = with(density) { width.toPx() }
        val cxPx = widthPx / 2f
        val cyPx = heightPx / 2f
        val maxRPx = minOf(widthPx, heightPx) / 2f * 0.9f

        val maxRangeLat = 0.009 * mathZoom
        val scale = (maxRPx.toDouble() / maxRangeLat) * tiltZoomFactor
        val densityVal = density.density
        val cosLat = Math.cos(Math.toRadians(localMapCenterLat))

        val dLatPlayer = userLat - localMapCenterLat
        val dLngPlayer = (userLng - localMapCenterLng) * cosLat
        val playerXUn = cxPx + (dLngPlayer * scale).toFloat()
        val playerYUn = cyPx - (dLatPlayer * scale).toFloat()

        fun projectPoint(lat: Double, lng: Double): Offset {
            val dLat = lat - localMapCenterLat
            val dLng = (lng - localMapCenterLng) * cosLat
            val xUn = cxPx + (dLng * scale).toFloat()
            val yUn = cyPx - (dLat * scale).toFloat()

            // Rotate around target position (screen center cxPx, cyPx)
            val dx = xUn - cxPx
            val dy = yUn - cyPx
            val rotX = cxPx + dx * cosRot - dy * sinRot
            val rotY = cyPx + dx * sinRot + dy * cosRot

            // Tilt-squash Y relative to cyPx
            val finalY = cyPx - (cyPx - rotY) * tiltYScale
            return Offset(rotX, finalY)
        }

        fun projectX(lat: Double, lng: Double): Float {
            val dLng = (lng - localMapCenterLng) * cosLat
            val xUn = cxPx + (dLng * scale).toFloat()
            val dLat = lat - localMapCenterLat
            val yUn = cyPx - (dLat * scale).toFloat()
            val dx = xUn - cxPx
            val dy = yUn - cyPx
            return cxPx + dx * cosRot - dy * sinRot
        }

        fun projectY(lat: Double, lng: Double): Float {
            val dLng = (lng - localMapCenterLng) * cosLat
            val xUn = cxPx + (dLng * scale).toFloat()
            val dLat = lat - localMapCenterLat
            val yUn = cyPx - (dLat * scale).toFloat()
            val dx = xUn - cxPx
            val dy = yUn - cyPx
            val rotY = cyPx + dx * sinRot + dy * cosRot
            return cyPx - (cyPx - rotY) * tiltYScale
        }

        val playerOffset = projectPoint(userLat, userLng)
        val playerX = playerOffset.x
        val playerY = playerOffset.y

        fun getGlitchOffsetX(y: Float, timeMs: Long): Float {
            val isGlitching = (timeMs % 1600 < 160)
            if (!isGlitching) return 0f
            val glitchCenterY = ((timeMs / 10) % heightPx.toInt()).toFloat()
            val glitchHeight = 90f
            val dy = kotlin.math.abs(y - glitchCenterY)
            if (dy < glitchHeight) {
                val factor = 1.0f - (dy / glitchHeight)
                return (kotlin.math.sin(y * 0.15f) * 14f * factor).toFloat()
            }
            return 0f
        }

        // Touch deck for tap targeting and map dragging
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(anomalies, localMapCenterLat, localMapCenterLng, mathZoom, zoomExpanded, rotationAngle) {
                    detectTapGestures { tapOffset ->
                        if (zoomExpanded) return@detectTapGestures
                        val minTapRadius = 24.dp.toPx()
                        val clickedAnomalies = mutableListOf<Pair<PoxAnomaly, Double>>()

                        anomalies.forEach { anomaly ->
                            val dLat = anomaly.lat - localMapCenterLat
                            val dLng = (anomaly.lng - localMapCenterLng) * cosLat

                            val axUn = cxPx + (dLng * scale).toFloat()
                            val ayUn = cyPx - (dLat * scale).toFloat()

                            // Project tap offset relative to screen center coordinates
                            val dx = tapOffset.x - cxPx
                            val dy = tapOffset.y - cyPx

                            // Unsquash Y coordinate
                            val dyUn = dy / tiltYScale.coerceAtLeast(0.01f)

                            // Reverse rotate by -rotRad
                            val cosRotNeg = kotlin.math.cos(-rotRad).toFloat()
                            val sinRotNeg = kotlin.math.sin(-rotRad).toFloat()
                            val dxGround = dx * cosRotNeg - dyUn * sinRotNeg
                            val dyGround = dx * sinRotNeg + dyUn * cosRotNeg

                            // Tap coordinates on unrotated ground plane relative to screen center
                            val tapXGround = cxPx + dxGround
                            val tapYGround = cyPx + dyGround

                            // Distance and angle on the unrotated ground plane relative to epicenter
                            val adx = tapXGround - axUn
                            val ady = tapYGround - ayUn
                            val tapDistPixels = kotlin.math.sqrt((adx * adx + ady * ady).toDouble())

                            // Determine approach angle theta relative to epicenter
                            val theta = kotlin.math.atan2(ady.toDouble(), adx.toDouble())

                            // Convert heatZoneDiameter/2 (feet) to canvas pixels
                            val r0Pixels = ((anomaly.heatZoneDiameter / 2.0) / 111000.0) * scale

                            val seed = anomaly.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
                            val epsilon = 0.15 + (seed % 3) * 0.05
                            val k = 3 + (seed % 3)
                            val phi = (seed % 360) * (Math.PI / 180.0)

                            val boundaryRadiusPixels = r0Pixels * (1.0 + epsilon * kotlin.math.cos(k * theta + phi))
                            val containmentRadius = maxOf(boundaryRadiusPixels, minTapRadius.toDouble())

                            if (tapDistPixels <= containmentRadius) {
                                clickedAnomalies.add(anomaly to tapDistPixels)
                            }
                        }

                        if (clickedAnomalies.isNotEmpty()) {
                            val selected = clickedAnomalies.minByOrNull { it.second }?.first
                            if (selected != null) {
                                onSelectAnomaly(selected.id)
                            }
                        }
                    }
                }
        ) {
            val anomalyPathPool = remember { List(40) { Path() } }
            val contourPathPool = remember { List(256) { Path() } }
            val mergedAnomalyPathPool = remember { List(40) { Path() } }
            val anomalyPool = remember { List(40) { ProjectedAnomaly() } }
            val visitedBuffer = remember { BooleanArray(40) }
            val groups = remember { List(40) { mutableListOf<ProjectedAnomaly>() } }

            // 1. Static map elements (Vector Roads and Vector Buildings) cached in a hardware graphics layer
            StaticMapLayer(
                roads = remember(roads) { StableRoads(roads) },
                sortedBuildings = remember(sortedBuildings) { StableBuildings(sortedBuildings) },
                localMapCenterLat = localMapCenterLat,
                localMapCenterLng = localMapCenterLng,
                mathZoom = mathZoom,
                tiltZoomFactor = tiltZoomFactor,
                tiltProgress = tiltProgress,
                tiltYScale = tiltYScale,
                playerXUn = playerXUn,
                playerYUn = playerYUn,
                scale = scale,
                cosLat = cosLat,
                cxPx = cxPx,
                cyPx = cyPx,
                cosRot = cosRot,
                sinRot = sinRot,
                isProfilerEnabled = isProfilerEnabled,
                profilerState = profilerState,
                timeProvider = timeProvider
            )

            // 2. Dynamic map elements (crosshair, anomalies, player, creatures, scanlines)
            SideEffect { if (isProfilerEnabled) profilerState.dynamicRecomps++ }
            ProfilingCanvas(
                modifier = Modifier.fillMaxSize(),
                isProfilerEnabled = isProfilerEnabled,
                profilerState = profilerState
            ) {
                if (isProfilerEnabled) {
                    profilerState.resetFrameDrawCounters()
                }
                val timeMs = timeProvider()
                val time = timeMs
                val isGlitching = (timeMs % 1600 < 160)
                val cx = cxPx
                val cy = cyPx
                val maxR = maxRPx
                val playerX = playerX
                val playerY = playerY

                // Calculate scanline vertical position
                val scanlineY = size.height * scanlineFraction
                val movingUp = directionTracker.update(scanlineY)

                // 2. Draw crosshair scope lines centered on player position
                drawLine(Color(0xFF00FF41).copy(alpha = 0.15f), Offset(playerX - maxR, playerY), Offset(playerX + maxR, playerY), strokeWidth = 0.5f)
                drawLine(Color(0xFF00FF41).copy(alpha = 0.15f), Offset(playerX, playerY - maxR), Offset(playerX, playerY + maxR), strokeWidth = 0.5f)

                // 5b. Draw Volumetric Irregular Heatmaps (Outer Boundary static outline ONLY, glowing when swept) using cached projection
                visitedBuffer.fill(false)
                for (gIdx in 0 until 40) {
                    groups[gIdx].clear()
                }
                
                val activeAnomaliesCount = anomalies.size.coerceAtMost(40)
                for (i in 0 until activeAnomaliesCount) {
                    val anomaly = anomalies[i]
                    val item = anomalyPool[i]
                    
                    val ay = projectY(anomaly.lat, anomaly.lng)
                    val ax = projectX(anomaly.lat, anomaly.lng) + getGlitchOffsetX(ay, timeMs)
                    
                    item.ax = ax
                    item.ay = ay
                    
                    val r0Pixels = (((anomaly.heatZoneDiameter / 2.0) / 111000.0) * scale).toFloat()
                    val seed = anomaly.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
                    val epsilon = 0.15f + (seed % 3) * 0.05f
                    val k = 3 + (seed % 3)
                    val phi = (seed % 360) * (Math.PI / 180.0)
                    val rMax = r0Pixels * (1.0f + epsilon)
                    
                    item.rMax = rMax
                    item.r0Pixels = r0Pixels.toDouble()
                    item.epsilon = epsilon.toDouble()
                    item.k = k
                    item.phi = phi
                    item.factionColor = when (anomaly.faction) {
                        "Infection" -> Color.Red
                        "Mech" -> Color.Yellow
                        "Parasite" -> Color(0xFFA855F7)
                        else -> Color.Cyan
                    }
                    
                    val diffY = if (movingUp) ay - scanlineY else scanlineY - ay
                    val sweepIntensity = if (diffY in 0f..110f) {
                        1.0f - (diffY / 110f)
                    } else {
                        0.0f
                    }
                    item.contourAlpha = 0.5f + 0.45f * sweepIntensity
                    
                    // Build path with dynamic glitch and store vertices
                    item.path.reset()
                    val steps = 36
                    item.numVertices = steps + 1
                    for (step in 0..steps) {
                        val theta = (step * 2.0 * Math.PI) / steps
                        val rBase = r0Pixels * (1.0f + epsilon * kotlin.math.cos(k * theta + phi).toFloat())
                        val thetaRotated = theta - rotRad
                        val py = (ay - rBase * kotlin.math.sin(thetaRotated) * tiltYScale).toFloat()
                        val px = (ax + rBase * kotlin.math.cos(thetaRotated)).toFloat()
                        
                        item.verticesX[step] = px
                        item.verticesY[step] = py
                        
                        val pxGlitched = px + getGlitchOffsetX(py, timeMs)
                        if (step == 0) {
                            item.path.moveTo(pxGlitched, py)
                        } else {
                            item.path.lineTo(pxGlitched, py)
                        }
                    }
                    item.path.close()
                }

                // Group overlapping anomalies in primitive queue
                var numGroups = 0
                val queue = IntArray(40)

                for (i in 0 until activeAnomaliesCount) {
                    if (!visitedBuffer[i]) {
                        val currentGroup = groups[numGroups]
                        numGroups++
                        
                        var head = 0
                        var tail = 0
                        queue[tail++] = i
                        visitedBuffer[i] = true

                        while (head < tail) {
                            val curr = queue[head++]
                            currentGroup.add(anomalyPool[curr])

                            for (j in 0 until activeAnomaliesCount) {
                                if (!visitedBuffer[j]) {
                                    val dx = anomalyPool[curr].ax - anomalyPool[j].ax
                                    val dy = anomalyPool[curr].ay - anomalyPool[j].ay
                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                    if (dist < (anomalyPool[curr].rMax + anomalyPool[j].rMax)) {
                                        visitedBuffer[j] = true
                                        queue[tail++] = j
                                    }
                                }
                            }
                        }
                    }
                }

                // Render groups
                var contourPathIndex = 0
                var mergedAnomalyPathIndex = 0
                for (g in 0 until numGroups) {
                    val group = groups[g]
                    val groupSize = group.size
                    if (groupSize > 0) {
                        // Compute unified merged path for clipping using addPath instead of Path.combine
                        val mergedPath = mergedAnomalyPathPool.getOrNull(mergedAnomalyPathIndex) ?: Path()
                        mergedAnomalyPathIndex++
                        mergedPath.reset()
                        
                        for (i in 0 until groupSize) {
                            mergedPath.addPath(group[i].path)
                        }

                        // Draw radial gradient fills for each anomaly, clipped to the UNIFIED merged path.
                        clipPath(mergedPath) {
                            for (i in 0 until groupSize) {
                                val item = group[i]
                                scale(scaleX = 1f, scaleY = tiltYScale, pivot = Offset(item.ax, item.ay)) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(item.factionColor, Color.Transparent),
                                            center = Offset(item.ax, item.ay),
                                            radius = item.rMax
                                        ),
                                        radius = item.rMax,
                                        center = Offset(item.ax, item.ay)
                                    )
                                }
                            }
                        }

                        // Draw 5 evenly spaced concentric contour lines merged seamlessly and animating outward
                        val numContours = 5
                        for (c in 1..numContours) {
                            val scaleVal = (c.toFloat() - 1f + contourFlowProgress) / numContours
                            if (scaleVal < 0.02f) continue
                            
                            val groupStartPathIndex = contourPathIndex
                            
                            for (i in 0 until groupSize) {
                                val item = group[i]
                                val contourPath = contourPathPool.getOrNull(contourPathIndex) ?: Path()
                                contourPathIndex++
                                contourPath.reset()

                                val numVerts = item.numVertices
                                if (numVerts > 0) {
                                    // Scale vertices around epicenter (item.ax, item.ay) in screen space
                                    val vx0 = item.verticesX[0]
                                    val vy0 = item.verticesY[0]
                                    val px0 = item.ax + (vx0 - item.ax) * scaleVal
                                    val py0 = item.ay + (vy0 - item.ay) * scaleVal
                                    contourPath.moveTo(px0 + getGlitchOffsetX(py0, timeMs), py0)

                                    for (vi in 1 until numVerts) {
                                        val vx = item.verticesX[vi]
                                        val vy = item.verticesY[vi]
                                        val px = item.ax + (vx - item.ax) * scaleVal
                                        val py = item.ay + (vy - item.ay) * scaleVal
                                        contourPath.lineTo(px + getGlitchOffsetX(py, timeMs), py)
                                    }
                                }
                                contourPath.close()
                            }
                            
                            val groupColor = group[0].factionColor
                            var maxAlpha = 0f
                            for (i in 0 until groupSize) {
                                if (group[i].contourAlpha > maxAlpha) maxAlpha = group[i].contourAlpha
                            }
                            val groupAlpha = maxAlpha
                            val alphaFactor = (4.0f * scaleVal * (1.0f - scaleVal)).coerceIn(0.0f, 1.0f)
                            
                            for (i in 0 until groupSize) {
                                drawGroupContoursClipped(
                                    group = group,
                                    groupColor = groupColor,
                                    groupAlpha = groupAlpha,
                                    alphaFactor = alphaFactor,
                                    groupStartPathIndex = groupStartPathIndex,
                                    contourPathPool = contourPathPool,
                                    index = i,
                                    clipIdx = 0
                                )
                            }
                        }
                        
                        for (i in 0 until groupSize) {
                            val item = group[i]
                            drawCircle(item.factionColor, radius = 3.5f, center = Offset(item.ax, item.ay))
                            drawCircle(Color.White, radius = 1.2f, center = Offset(item.ax, item.ay))
                        }
                    }
                }

                // 6. Draw player transceiver beacon center
                drawCircle(CyberGreen, radius = 3.5f, center = Offset(playerX, playerY))

                // 6b. Draw miniaturized active harvesting creatures traveling on the map
                val precomputedMissionsSize = precomputedMissions.size
                for (mIdx in 0 until precomputedMissionsSize) {
                    val state = precomputedMissions[mIdx]
                    val m = state.mission
                    val factionColor = when (m.creatureFaction) {
                        "Infection" -> Color.Red
                        "Mech" -> Color.Yellow
                        "Parasite" -> Color(0xFFA855F7)
                        else -> Color.Cyan
                    }

                    // Project coordinates to screen space using precomputed coordinates
                    val boundX = projectX(state.boundLat, state.boundLng)
                    val boundY = projectY(state.boundLat, state.boundLng)

                    val anomX = projectX(m.lat, m.lng)
                    val anomY = projectY(m.lat, m.lng)

                    val startX = projectX(state.startLat, state.startLng)
                    val startY = projectY(state.startLat, state.startLng)

                    // Durations
                    val dTravel = m.travelDuration.toFloat()
                    val dDescent = m.descentDuration.toFloat()
                    val dHarvest = m.harvestDuration.toFloat()
                    val dAscent = m.ascentDuration.toFloat()
                    val dReturn = m.transitBackDuration.toFloat()

                    val wave = WaveMath.getDailyWaveConfig(timeMs)
                    val phaseFraction = wave.lunarAge / WaveMath.LUNAR_MONTH_DAYS
                    val lunarPhaseScale = (1.0 - kotlin.math.cos(phaseFraction * 2.0 * Math.PI)) / 2.0
                    val densityShift = 0.2 * (lunarPhaseScale - 0.5)
                    val effectiveDensity = (state.combinedDensity + densityShift).coerceIn(-0.33, 0.33)

                    val finalAnomalyDensity = if (state.hasShield && effectiveDensity > 0.0) 0.0 else effectiveDensity

                    // Calculate real-time smooth elapsed seconds
                    val elapsedSec = (timeMs - m.startTime) / 1000f

                    var creatureXUnglitched = playerX
                    var creatureY = playerY
                    var stateText = "RETURN"

                    if (elapsedSec < dTravel) {
                        val p = (elapsedSec / dTravel.coerceAtLeast(1f))
                        val pWarped = p
                        creatureXUnglitched = playerX + pWarped * (boundX - playerX)
                        creatureY = playerY + pWarped * (boundY - playerY)
                        stateText = "TRAVEL"
                    } else if (elapsedSec < dTravel + dDescent) {
                        val p = ((elapsedSec - dTravel) / dDescent.coerceAtLeast(1f))
                        val pWarped = warpProgress(p, finalAnomalyDensity.toFloat())
                        creatureXUnglitched = boundX + pWarped * (startX - boundX)
                        creatureY = boundY + pWarped * (startY - boundY)
                        stateText = "DESCEND"
                    } else if (elapsedSec < dTravel + dDescent + dHarvest) {
                        val hoverTime = (timeMs % 1000) * (2.0 * Math.PI / 1000.0)
                        creatureXUnglitched = startX + (kotlin.math.sin(hoverTime) * 2f * densityVal).toFloat()
                        creatureY = startY + (kotlin.math.cos(hoverTime) * 2f * densityVal).toFloat()
                        stateText = "HARVEST"
                    } else if (elapsedSec < dTravel + dDescent + dHarvest + dAscent) {
                        val p = ((elapsedSec - dTravel - dDescent - dHarvest) / dAscent.coerceAtLeast(1f))
                        val pWarped = warpProgress(p, -finalAnomalyDensity.toFloat())
                        creatureXUnglitched = startX + pWarped * (boundX - startX)
                        creatureY = startY + pWarped * (boundY - startY)
                        stateText = "ASCEND"
                    } else if (elapsedSec < dTravel + dDescent + dHarvest + dAscent + dReturn) {
                        val p = ((elapsedSec - dTravel - dDescent - dHarvest - dAscent) / dReturn.coerceAtLeast(1f))
                        val pWarped = p
                        creatureXUnglitched = boundX + pWarped * (playerX - boundX)
                        creatureY = boundY + pWarped * (playerY - boundY)
                        stateText = "RETURN"
                    }

                    // Apply horizontal screen tear glitch offset to the coordinate positions
                    val creatureYGlitch = getGlitchOffsetX(creatureY, timeMs)
                    val creatureX = creatureXUnglitched + creatureYGlitch

                    val boundXGlitched = boundX + getGlitchOffsetX(boundY, timeMs)

                    // Draw trajectory guideline
                    drawLine(
                        color = factionColor.copy(alpha = 0.25f),
                        start = Offset(playerX, playerY),
                        end = Offset(boundXGlitched, boundY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )

                    // Draw miniature creature 3D wireframe model
                    val dna = m.originalSequence ?: "AAAAAAAA"
                    val geometry = geometryCache.getOrPut(dna) { CreatureGeometry(dna) }

                    val spinAngle = (timeMs % 6000) * (2.0 * Math.PI / 6000.0)
                    val breathingPhase = (timeMs % 3000) * (2.0 * Math.PI / 3000.0)
                    val breathScale = 1.0 + geometry.breatheAmp * kotlin.math.sin(breathingPhase * geometry.breatheFreq)

                    val dynamicScale = 0.22f / mathZoom.coerceAtLeast(0.1f)
                    val modelSize = (25.dp.toPx() * (dynamicScale / 0.35f)).coerceIn(10.dp.toPx(), 45.dp.toPx())
                    val densityVal = density.density

                    if (dynamicScale < 0.3f) {
                        // LOD Tier 3: Draw a single glowing contact dot
                        drawCircle(
                            color = factionColor,
                            radius = 3f * densityVal,
                            center = Offset(creatureX, creatureY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.2f * densityVal,
                            center = Offset(creatureX, creatureY)
                        )
                    } else {
                        val tilt = Math.toRadians(18.0)
                        val cosT = kotlin.math.cos(tilt)
                        val sinT = kotlin.math.sin(tilt)

                        val cosS = kotlin.math.cos(spinAngle)
                        val sinS = kotlin.math.sin(spinAngle)

                        val verticesList = geometry.vertices
                        val verticesSize = verticesList.size
                        var vertIdx = 0
                        for (vIdx in 0 until verticesSize) {
                            if (vertIdx >= maxVertices) break
                            val v = verticesList[vIdx]
                            val bx = v.x * breathScale * dynamicScale
                            val by = v.y * breathScale * dynamicScale
                            val bz = v.z * breathScale * dynamicScale

                            val twist = v.y * geometry.twistRate
                            val cosTw = kotlin.math.cos(twist)
                            val sinTw = kotlin.math.sin(twist)
                            val tx = bx * cosTw - bz * sinTw
                            val tz = bx * sinTw + bz * cosTw
                            val ty = by

                            val rx = tx * cosS - tz * sinS
                            val rz = tx * sinS + tz * cosS
                            val ry = ty

                            val finalX = rx
                            val finalY = ry * cosT - rz * sinT
                            val finalZ = ry * sinT + rz * cosT

                            projectedXBuffer[vertIdx] = creatureX + finalX.toFloat()
                            projectedYBuffer[vertIdx] = creatureY - finalY.toFloat()
                            depthZBuffer[vertIdx] = finalZ
                            vertIdx++
                        }

                        val maxRadius = (geometry.baseRadius * breathScale * dynamicScale * 1.4).coerceAtLeast(2.0)

                        val edgesList = geometry.edges
                        val edgesSize = edgesList.size

                        // Draw outer model drop shadows (LOD Tier 1 only: dynamicScale > 0.8f)
                        if (dynamicScale > 0.8f) {
                            for (eIdx in 0 until edgesSize) {
                                val edge = edgesList[eIdx]
                                if (edge.first < vertIdx && edge.second < vertIdx) {
                                    val z1 = depthZBuffer[edge.first]
                                    val z2 = depthZBuffer[edge.second]
                                    val avgZ = (z1 + z2) / 2.0
                                    val depthPct = ((avgZ / maxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                                    val alpha = (0.25f + 0.75f * depthPct).toFloat()
                                    val stroke = (0.8f + 0.8f * depthPct).toFloat()

                                    drawLine(
                                        color = Color.Black.copy(alpha = alpha * 0.8f),
                                        start = Offset(projectedXBuffer[edge.first], projectedYBuffer[edge.first]) + Offset(1.5f * densityVal, 1.5f * densityVal),
                                        end = Offset(projectedXBuffer[edge.second], projectedYBuffer[edge.second]) + Offset(1.5f * densityVal, 1.5f * densityVal),
                                        strokeWidth = stroke
                                    )
                                }
                            }
                        }

                        // Draw outer model main lines (LOD Tier 1 and 2: dynamicScale >= 0.3f)
                        for (eIdx in 0 until edgesSize) {
                            val edge = edgesList[eIdx]
                            if (edge.first < vertIdx && edge.second < vertIdx) {
                                val z1 = depthZBuffer[edge.first]
                                val z2 = depthZBuffer[edge.second]
                                val avgZ = (z1 + z2) / 2.0
                                
                                val depthPct = ((avgZ / maxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                                val alpha = (0.25f + 0.75f * depthPct).toFloat()
                                val stroke = (0.8f + 0.8f * depthPct).toFloat()

                                drawLine(
                                    color = factionColor.copy(alpha = alpha),
                                    start = Offset(projectedXBuffer[edge.first], projectedYBuffer[edge.first]),
                                    end = Offset(projectedXBuffer[edge.second], projectedYBuffer[edge.second]),
                                    strokeWidth = stroke
                                )
                            }
                        }

                        // Project and draw inner core energy reactor (LOD Tier 1 only: dynamicScale > 0.8f)
                        if (dynamicScale > 0.8f) {
                            val coreSpinAngle = spinAngle * 2.5
                            val cosCs = kotlin.math.cos(coreSpinAngle)
                            val sinCs = kotlin.math.sin(coreSpinAngle)

                            val innerVerticesList = geometry.innerVertices
                            val innerVerticesSize = innerVerticesList.size
                            var innerVertIdx = 0
                            for (vIdx in 0 until innerVerticesSize) {
                                if (innerVertIdx >= maxVertices) break
                                val v = innerVerticesList[vIdx]
                                val vx = v.x * dynamicScale
                                val vy = v.y * dynamicScale
                                val vz = v.z * dynamicScale

                                val rx = vx * cosCs - vz * sinCs
                                val rz = vx * sinCs + vz * cosCs
                                val ry = vy

                                val finalX = rx
                                val finalY = ry * cosT - rz * sinT
                                val finalZ = ry * sinT + rz * cosT

                                projectedInnerXBuffer[innerVertIdx] = creatureX + finalX.toFloat()
                                projectedInnerYBuffer[innerVertIdx] = creatureY - finalY.toFloat()
                                depthInnerZBuffer[innerVertIdx] = finalZ
                                innerVertIdx++
                            }

                            val innerMaxRadius = (geometry.innerVertices.firstOrNull()?.x ?: 2.0) * dynamicScale * 1.2
                            val innerCoreColor = Color(0xFFFBBF24)

                            val innerEdgesList = geometry.innerEdges
                            val innerEdgesSize = innerEdgesList.size

                            // Draw inner core drop shadows
                            for (eIdx in 0 until innerEdgesSize) {
                                val edge = innerEdgesList[eIdx]
                                if (edge.first < innerVertIdx && edge.second < innerVertIdx) {
                                    val z1 = depthInnerZBuffer[edge.first]
                                    val z2 = depthInnerZBuffer[edge.second]
                                    val avgZ = (z1 + z2) / 2.0
                                    val depthPct = ((avgZ / innerMaxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                                    val alpha = (0.3f + 0.7f * depthPct).toFloat()
                                    val stroke = (0.6f + 0.6f * depthPct).toFloat()

                                    drawLine(
                                        color = Color.Black.copy(alpha = alpha * 0.8f),
                                        start = Offset(projectedInnerXBuffer[edge.first], projectedInnerYBuffer[edge.first]) + Offset(1.5f * densityVal, 1.5f * densityVal),
                                        end = Offset(projectedInnerXBuffer[edge.second], projectedInnerYBuffer[edge.second]) + Offset(1.5f * densityVal, 1.5f * densityVal),
                                        strokeWidth = stroke
                                    )
                                }
                            }

                            // Draw inner core main lines
                            for (eIdx in 0 until innerEdgesSize) {
                                val edge = innerEdgesList[eIdx]
                                if (edge.first < innerVertIdx && edge.second < innerVertIdx) {
                                    val z1 = depthInnerZBuffer[edge.first]
                                    val z2 = depthInnerZBuffer[edge.second]
                                    val avgZ = (z1 + z2) / 2.0
                                    
                                    val depthPct = ((avgZ / innerMaxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                                    val alpha = (0.3f + 0.7f * depthPct).toFloat()
                                    val stroke = (0.6f + 0.6f * depthPct).toFloat()

                                    drawLine(
                                        color = innerCoreColor.copy(alpha = alpha),
                                        start = Offset(projectedInnerXBuffer[edge.first], projectedInnerYBuffer[edge.first]),
                                        end = Offset(projectedInnerXBuffer[edge.second], projectedInnerYBuffer[edge.second]),
                                        strokeWidth = stroke
                                    )
                                }
                            }
                        }
                    }

                    val lockSize = 35.dp.toPx()
                    val isTracked = m.id == trackedMissionId

                    val rawName = m.creatureName.uppercase()
                    val displayName = if (rawName.length > 8) {
                        val paddedName = rawName + "    "
                        val cycleLen = paddedName.length
                        val scrollIndex = ((timeMs / 250) % cycleLen).toInt()
                        val doublePadded = paddedName + paddedName
                        doublePadded.substring(scrollIndex, scrollIndex + 8)
                    } else {
                        rawName
                    }
                    
                    val nameY = if (isTracked) {
                        creatureY - lockSize - 6f
                    } else {
                        creatureY - modelSize - 4f
                    }

                    // Look up pre-cached paints to avoid JNI/heap allocations
                    val factionPaintKey = if (textPaints.containsKey(m.creatureFaction)) m.creatureFaction else "Default"
                    val textPaint = textPaints[factionPaintKey]!!

                    // Draw shadow
                    if (isProfilerEnabled) {
                        profilerState.drawTextCount += 2
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        displayName,
                        creatureX + 1.5f * densityVal,
                        nameY + 1.5f * densityVal,
                        shadowTextPaint
                    )
                    // Draw original
                    drawContext.canvas.nativeCanvas.drawText(
                        displayName,
                        creatureX,
                        nameY,
                        textPaint
                    )

                    // Draw a target lock indicator around the tracked creature
                    if (isTracked) {
                        val tickLen = 10.dp.toPx()
                        val lockColor = factionColor.copy(alpha = 0.8f)
                        val shadowColor = Color.Black.copy(alpha = 0.8f)
                        val strokeW = 1.5f
                        val offset = Offset(1.5f * densityVal, 1.5f * densityVal)

                        // 1. Draw lock-on shadows
                        // Top-left corner
                        drawLine(shadowColor, Offset(creatureX - lockSize, creatureY - lockSize) + offset, Offset(creatureX - lockSize + tickLen, creatureY - lockSize) + offset, strokeWidth = strokeW)
                        drawLine(shadowColor, Offset(creatureX - lockSize, creatureY - lockSize) + offset, Offset(creatureX - lockSize, creatureY - lockSize + tickLen) + offset, strokeWidth = strokeW)

                        // Top-right corner
                        drawLine(shadowColor, Offset(creatureX + lockSize, creatureY - lockSize) + offset, Offset(creatureX + lockSize - tickLen, creatureY - lockSize) + offset, strokeWidth = strokeW)
                        drawLine(shadowColor, Offset(creatureX + lockSize, creatureY - lockSize) + offset, Offset(creatureX + lockSize, creatureY - lockSize + tickLen) + offset, strokeWidth = strokeW)

                        // Bottom-left corner
                        drawLine(shadowColor, Offset(creatureX - lockSize, creatureY + lockSize) + offset, Offset(creatureX - lockSize + tickLen, creatureY + lockSize) + offset, strokeWidth = strokeW)
                        drawLine(shadowColor, Offset(creatureX - lockSize, creatureY + lockSize) + offset, Offset(creatureX - lockSize, creatureY + lockSize - tickLen) + offset, strokeWidth = strokeW)

                        // Bottom-right corner
                        drawLine(shadowColor, Offset(creatureX + lockSize, creatureY + lockSize) + offset, Offset(creatureX + lockSize - tickLen, creatureY + lockSize) + offset, strokeWidth = strokeW)
                        drawLine(shadowColor, Offset(creatureX + lockSize, creatureY + lockSize) + offset, Offset(creatureX + lockSize, creatureY + lockSize - tickLen) + offset, strokeWidth = strokeW)

                        // 2. Draw lock-on original lines
                        // Top-left corner
                        drawLine(lockColor, Offset(creatureX - lockSize, creatureY - lockSize), Offset(creatureX - lockSize + tickLen, creatureY - lockSize), strokeWidth = strokeW)
                        drawLine(lockColor, Offset(creatureX - lockSize, creatureY - lockSize), Offset(creatureX - lockSize, creatureY - lockSize + tickLen), strokeWidth = strokeW)

                        // Top-right corner
                        drawLine(lockColor, Offset(creatureX + lockSize, creatureY - lockSize), Offset(creatureX + lockSize - tickLen, creatureY - lockSize), strokeWidth = strokeW)
                        drawLine(lockColor, Offset(creatureX + lockSize, creatureY - lockSize), Offset(creatureX + lockSize, creatureY - lockSize + tickLen), strokeWidth = strokeW)

                        // Bottom-left corner
                        drawLine(lockColor, Offset(creatureX - lockSize, creatureY + lockSize), Offset(creatureX - lockSize + tickLen, creatureY + lockSize), strokeWidth = strokeW)
                        drawLine(lockColor, Offset(creatureX - lockSize, creatureY + lockSize), Offset(creatureX - lockSize, creatureY + lockSize - tickLen), strokeWidth = strokeW)

                        // Bottom-right corner
                        drawLine(lockColor, Offset(creatureX + lockSize, creatureY + lockSize), Offset(creatureX + lockSize - tickLen, creatureY + lockSize), strokeWidth = strokeW)
                        drawLine(lockColor, Offset(creatureX + lockSize, creatureY + lockSize), Offset(creatureX + lockSize, creatureY + lockSize - tickLen), strokeWidth = strokeW)

                        // Draw stateText (solid, below the lower boundaries of the lock-on)
                        val stateY = creatureY + lockSize + 12f
                        
                        if (isProfilerEnabled) {
                            profilerState.drawTextCount += 2
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            stateText,
                            creatureX + 1.5f * densityVal,
                            stateY + 1.5f * densityVal,
                            shadowTextPaint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            stateText,
                            creatureX,
                            stateY,
                            textPaint
                        )
                    }
                }

                // 5. Draw bright horizontal scanline sweep ray spanning full width of the container
                // Outer blooming glow
                drawLine(
                    color = CyberGreen.copy(alpha = 0.35f),
                    start = Offset(0f, scanlineY),
                    end = Offset(size.width, scanlineY),
                    strokeWidth = 6.0f
                )
                // Main laser line
                drawLine(
                    color = CyberGreen.copy(alpha = 0.9f),
                    start = Offset(0f, scanlineY),
                    end = Offset(size.width, scanlineY),
                    strokeWidth = 2.5f
                )
                // Bright white hot core
                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = Offset(0f, scanlineY),
                    end = Offset(size.width, scanlineY),
                    strokeWidth = 1.0f
                )
                
                // Draw trailing scanline glow spanning full width (flipping direction correctly when moving up vs down)
                val startY = if (movingUp) scanlineY - 3f else scanlineY - 110f
                val endY = if (movingUp) scanlineY + 110f else scanlineY + 3f
                val colors = if (movingUp) {
                    listOf(
                        Color.Transparent,
                        CyberGreen.copy(alpha = 0.25f),
                        CyberGreen.copy(alpha = 0.03f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color.Transparent,
                        CyberGreen.copy(alpha = 0.03f),
                        CyberGreen.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                }
                val scanlineGlow = Brush.verticalGradient(
                    colors = colors,
                    startY = startY,
                    endY = endY
                )
                drawRect(brush = scanlineGlow, size = size)

                // 7. Render light analog static line aberrations & CRT noise snow
                val rand = java.util.Random(timeMs / 120)
                
                // Random horizontal static line cuts (original effect enhanced)
                if (rand.nextFloat() < 0.5f) {
                    val linesCount = rand.nextInt(4) + 1
                    for (i in 0 until linesCount) {
                        val staticY = rand.nextFloat() * size.height
                        val staticAlpha = rand.nextFloat() * 0.18f + 0.05f
                        val staticWidth = rand.nextFloat() * 300f + 50f
                        val staticX = rand.nextFloat() * (size.width - staticWidth)
                        drawLine(
                            color = CyberGreen.copy(alpha = staticAlpha),
                            start = Offset(staticX, staticY),
                            end = Offset(staticX + staticWidth, staticY),
                            strokeWidth = 1f
                        )
                    }
                }

                // CRT Phosphor Noise Snow / Small static ticks
                if (isGlitching || rand.nextFloat() < 0.25f) {
                    val noiseTicks = if (isGlitching) 35 else 12
                    for (i in 0 until noiseTicks) {
                        val py = rand.nextFloat() * size.height
                        val px = rand.nextFloat() * size.width
                        val tickWidth = rand.nextFloat() * 10f + 3f
                        val alpha = rand.nextFloat() * 0.3f + 0.05f
                        val color = if (rand.nextBoolean()) CyberGreen else Color.White
                        
                        drawLine(
                            color = color.copy(alpha = alpha),
                            start = Offset(px, py),
                            end = Offset(px + tickWidth, py),
                            strokeWidth = 1f
                        )
                    }
                }

                // Random full-width scanline horizontal tracking glitch bar
                if (isGlitching && rand.nextFloat() < 0.3f) {
                    val glitchBarY = rand.nextFloat() * size.height
                    val barHeight = rand.nextFloat() * 15f + 5f
                    drawRect(
                        color = CyberGreen.copy(alpha = 0.08f),
                        topLeft = Offset(0f, glitchBarY),
                        size = androidx.compose.ui.geometry.Size(size.width, barHeight)
                    )
                }
            }

            // Draw Epicenter Hover Labels (shifted with map viewport coordinates and horizontal glitch offset)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                val anomaliesSize = anomalies.size
                for (aIdx in 0 until anomaliesSize) {
                    val anomaly = anomalies[aIdx]
                    val factionColor = when (anomaly.faction) {
                        "Infection" -> Color.Red
                        "Mech" -> Color.Yellow
                        "Parasite" -> Color(0xFFA855F7)
                        else -> Color.Cyan
                    }
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val epicenter = projectPoint(anomaly.lat, anomaly.lng)
                                val yPx = epicenter.y
                                val tMs = timeProvider()
                                val glitchOffsetPx = getGlitchOffsetX(yPx, tMs)
                                val xPx = epicenter.x + glitchOffsetPx

                                val widthPx = width.toPx()
                                val heightPx = height.toPx()

                                val isInside = xPx >= 0f && xPx <= widthPx && yPx >= 0f && yPx <= heightPx
                                alpha = if (isInside) 0.9f else 0.0f

                                translationX = xPx - 35.dp.toPx()
                                translationY = yPx - 22.dp.toPx()
                            }
                            .width(70.dp)
                            .border(0.5.dp, factionColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = anomaly.name.uppercase(),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        // Draw the tab frame border on top of map elements to prevent them from drawing over it
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
        )
        if (isProfilerEnabled) {
            ProfilerHUDOverlay(
                state = profilerState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 90.dp)
            )
        }
    }
}

@Composable
fun ZoomScrollCircle(
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
            .clip(androidx.compose.foundation.shape.CircleShape)
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
                .clip(androidx.compose.foundation.shape.CircleShape)
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
fun RotationScrollCircle(
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
            .clip(androidx.compose.foundation.shape.CircleShape)
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
                .clip(androidx.compose.foundation.shape.CircleShape)
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

fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dx = (lng2 - lng1) * 111000.0 * kotlin.math.cos(Math.toRadians(lat1))
    val dy = (lat2 - lat1) * 111000.0
    val distMeters = kotlin.math.sqrt(dx * dx + dy * dy)
    return distMeters * 3.28084
}

fun warpProgress(pLin: Float, density: Float): Float {
    val d = density.coerceIn(-0.9f, 0.9f)
    if (kotlin.math.abs(d) < 0.001f) {
        return pLin
    }
    return ((1.0 - Math.pow((1.0 - d).toDouble(), pLin.toDouble())) / d).toFloat().coerceIn(0f, 1f)
}

fun hasCoherenceShield(creature: Creature?, originalSequence: String?): Boolean {
    if (creature != null) {
        if (WaveMath.getAnomalousBenefits(creature.sequence).any { it.id == "COHERENCE_SHIELD" }) {
            return true
        }
        creature.appendedGenes.forEach { gene ->
            if (WaveMath.isAnomalousGene(gene) && WaveMath.getBenefitForAnomalousGene(gene).id == "COHERENCE_SHIELD") {
                return true
            }
        }
        return false
    }
    if (originalSequence != null) {
        if (WaveMath.getAnomalousBenefits(originalSequence).any { it.id == "COHERENCE_SHIELD" }) {
            return true
        }
    }
    return false
}

@Composable
fun ScannerWaveForecastView(viewModel: MainViewModel) {
    val todayWave = remember { WaveMath.getDailyWaveConfig(System.currentTimeMillis()) }
    val tomorrowWave = remember { WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 86400000L) }
    val dayAfterWave = remember { WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 172800000L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Today's base-pair wave card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(
                    borderColor = if (todayWave.isSuppressed) Color(0xFF990000) else CyberGreen,
                    backgroundColor = if (todayWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.6f)
                )
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "⚡",
                        color = if (todayWave.isSuppressed) Color.Red else Color(0xFFFFB300),
                        fontSize = 14.sp
                    )
                    Column {
                        Text(
                            text = "TODAY'S BASE-PAIR WAVE",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (todayWave.isSuppressed) "DORMANT (CONGESTED DECAY)" else "ACTIVE: ${todayWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!todayWave.isSuppressed) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${todayWave.primary} ➔ ${todayWave.secondary}",
                            color = CyberGreen,
                            style = Typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "1.12x & 1.62x BOOST",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "NULL",
                        color = Color.Red,
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF330000), RoundedCornerShape(2.dp))
                            .border(1.dp, Color.Red, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Forecast Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tomorrow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .cyberglass(
                        borderColor = if (tomorrowWave.isSuppressed) Color(0xFF990000) else Color.DarkGray,
                        backgroundColor = if (tomorrowWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f)
                    )
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOMORROW BASE-PAIR",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (tomorrowWave.isSuppressed) "DORMANT" else "${tomorrowWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!tomorrowWave.isSuppressed) {
                        Text(
                            text = "${tomorrowWave.primary}➔${tomorrowWave.secondary}",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Day After Tomorrow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .cyberglass(
                        borderColor = if (dayAfterWave.isSuppressed) Color(0xFF990000) else Color.DarkGray,
                        backgroundColor = if (dayAfterWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f)
                    )
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAY AFTER TOMORROW",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (dayAfterWave.isSuppressed) "DORMANT" else "${dayAfterWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!dayAfterWave.isSuppressed) {
                        Text(
                            text = "${dayAfterWave.primary}➔${dayAfterWave.secondary}",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

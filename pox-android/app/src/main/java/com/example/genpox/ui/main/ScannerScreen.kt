package com.example.genpox.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.ui.scanner.map.HolographicRadarScanner
import com.example.genpox.ui.scanner.views.ScannerNormalView

@Stable
class ScannerRadarState(
    sliderValue: Float = 2.0f,
    rotationValue: Float = 0f,
    cameraTiltAngle: Float = 90f,
    is3DMode: Boolean = false,
    panWorldX: Float = 0f,
    panWorldY: Float = 0f,
    zoomExpanded: Boolean = false,
    rotationExpanded: Boolean = false,
    buildingExtrusion: Float = 1.0f,
) {
    var sliderValue by mutableStateOf(sliderValue)
    var rotationValue by mutableStateOf(rotationValue)
    var cameraTiltAngle by mutableStateOf(cameraTiltAngle)
    var is3DMode by mutableStateOf(is3DMode)
    var panWorldX by mutableStateOf(panWorldX)
    var panWorldY by mutableStateOf(panWorldY)
    var zoomExpanded by mutableStateOf(zoomExpanded)
    var rotationExpanded by mutableStateOf(rotationExpanded)
    var buildingExtrusion by mutableFloatStateOf(buildingExtrusion)

    fun resetToNorthTop() {
        rotationValue = 0f
        cameraTiltAngle = 90f
        sliderValue = 2.0f
        is3DMode = false
        panWorldX = 0f
        panWorldY = 0f
    }

    fun toggle2D3D() {
        if (is3DMode) {
            is3DMode = false
            cameraTiltAngle = 90f
            rotationValue = 0f
        } else {
            is3DMode = true
            cameraTiltAngle = 85f
        }
    }

    val zoomMultiplier: Float
        get() {
            val zoomSteps = floatArrayOf(10.0f, 4.0f, 1.5f, 0.70f, 0.40f)
            val lowerIndex = sliderValue.toInt().coerceIn(0, 3)
            val upperIndex = lowerIndex + 1
            val fraction = sliderValue - lowerIndex
            return zoomSteps[lowerIndex] + fraction * (zoomSteps[upperIndex] - zoomSteps[lowerIndex])
        }

    val mathZoom: Float
        get() = zoomMultiplier * 0.625f

    val tiltProgress: Float
        get() = ((90.0f - cameraTiltAngle) / (90.0f - 15.0f)).coerceIn(0.0f, 1.0f)

    val tiltYScale: Float
        get() = kotlin.math.sin(Math.toRadians(cameraTiltAngle.toDouble())).toFloat().coerceIn(0.1f, 1.0f)
}

@Composable
fun rememberScannerRadarState(
    sliderValue: Float = 2.0f,
    rotationValue: Float = 0f,
    cameraTiltAngle: Float = 90f,
    is3DMode: Boolean = false,
    panWorldX: Float = 0f,
    panWorldY: Float = 0f,
    buildingExtrusion: Float = 1.0f,
) = remember {
    ScannerRadarState(
        sliderValue = sliderValue,
        rotationValue = rotationValue,
        cameraTiltAngle = cameraTiltAngle,
        is3DMode = is3DMode,
        panWorldX = panWorldX,
        panWorldY = panWorldY,
        buildingExtrusion = buildingExtrusion,
    )
}

@Composable
fun ScannerView(
    viewModel: MainViewModel,
    isFullscreen: Boolean = false,
    contentOnly: Boolean = false,
    controlsOnly: Boolean = false,
    isPaused: Boolean = false,
    state: ScannerRadarState = rememberScannerRadarState(),
    modifier: Modifier = Modifier,
) {
    val selectedAnomalyId by viewModel.selectedAnomalyId.collectAsStateWithLifecycle()
    val scannerSubTab by viewModel.scannerSubTab.collectAsStateWithLifecycle()

    if (contentOnly || (selectedAnomalyId == null && scannerSubTab == "radar")) {
        ScannerRadarView(
            viewModel = viewModel,
            isFullscreen = isFullscreen,
            contentOnly = contentOnly,
            controlsOnly = controlsOnly,
            isPaused = isPaused,
            state = state,
            modifier = modifier,
        )
    } else {
        ScannerNormalView(viewModel)
    }
}

@Composable
fun ScannerRadarView(
    viewModel: MainViewModel,
    isFullscreen: Boolean,
    contentOnly: Boolean = false,
    controlsOnly: Boolean = false,
    isPaused: Boolean = false,
    state: ScannerRadarState = rememberScannerRadarState(),
    modifier: Modifier = Modifier,
) {
    val anomalies by viewModel.anomalies.collectAsStateWithLifecycle()
    val baseFonts by viewModel.baseFonts.collectAsStateWithLifecycle()
    val celestialWells by viewModel.celestialWells.collectAsStateWithLifecycle()
    val lat by viewModel.latitude.collectAsStateWithLifecycle()
    val lng by viewModel.longitude.collectAsStateWithLifecycle()
    val selectedAnomalyId by viewModel.selectedAnomalyId.collectAsStateWithLifecycle()
    val scannerSubTab by viewModel.scannerSubTab.collectAsStateWithLifecycle()

    val zoomMultiplier = state.zoomMultiplier

    LaunchedEffect(zoomMultiplier) {
        viewModel.updateZoom(zoomMultiplier)
    }

    val panWorldX = state.panWorldX
    val panWorldY = state.panWorldY

    LaunchedEffect(panWorldX, panWorldY) {
        viewModel.updateCameraPan(panWorldX, panWorldY)
    }

    val showAnomaliesOnMap by viewModel.showAnomaliesOnMap.collectAsStateWithLifecycle()
    val showFontsOnMap by viewModel.showFontsOnMap.collectAsStateWithLifecycle()
    val showWellsOnMap by viewModel.showWellsOnMap.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (!controlsOnly) {
            HolographicRadarScanner(
                viewModel = viewModel,
                anomalies = if (showAnomaliesOnMap) anomalies else emptyList(),
                baseFonts = if (showFontsOnMap) baseFonts else emptyList(),
                celestialWells = if (showWellsOnMap) celestialWells else emptyList(),
                userLat = lat,
                userLng = lng,
                zoomMultiplier = zoomMultiplier,
                rotationAngle = state.rotationValue,
                cameraTiltAngle = state.cameraTiltAngle,
                radarState = state,
                selectedAnomalyId = selectedAnomalyId,
                onSelectAnomaly = { id -> viewModel.setSelectedAnomalyId(id) },
                isPaused = isPaused,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

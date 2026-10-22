package com.example.genpox.ui.scanner.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.data.AnomalyLifecyclePhase
import com.example.genpox.theme.CyberBorder
import com.example.genpox.theme.CyberGreen
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.theme.Typography
import com.example.genpox.ui.components.*
import com.example.genpox.ui.main.*
import com.example.genpox.ui.scanner.FontItemRow
import com.example.genpox.ui.scanner.ScannerCyberOrreryView
import com.example.genpox.ui.scanner.ScannerWaveForecastView
import com.example.genpox.ui.scanner.WellItemRow

@Composable
fun ScannerNormalView(viewModel: MainViewModel) {
    val anomalies by viewModel.anomalies.collectAsStateWithLifecycle()
    val baseFonts by viewModel.baseFonts.collectAsStateWithLifecycle()
    val lat by viewModel.latitude.collectAsStateWithLifecycle()
    val lng by viewModel.longitude.collectAsStateWithLifecycle()
    val selectedAnomalyId by viewModel.selectedAnomalyId.collectAsStateWithLifecycle()
    val selectedAnomaly by viewModel.selectedAnomaly.collectAsStateWithLifecycle()
    val activeMissionCoords by viewModel.activeMissionCoords.collectAsStateWithLifecycle()
    val depletedAnomalyCoords by viewModel.depletedAnomalyCoords.collectAsStateWithLifecycle()
    val scannerSubTab by viewModel.scannerSubTab.collectAsStateWithLifecycle()
    val devForceAnomaly by viewModel.devForceAnomaly.collectAsStateWithLifecycle()
    val celestialWells by viewModel.celestialWells.collectAsStateWithLifecycle()
    var activeAnomalyTab by remember { mutableStateOf("scan") }

    val activeMissions by viewModel.activeMissions.collectAsStateWithLifecycle()
    val activeMissionsList = remember(activeMissions) { activeMissions.filter { !it.isReturned } }

    val showAnomaliesOnMap by viewModel.showAnomaliesOnMap.collectAsStateWithLifecycle()
    val showFontsOnMap by viewModel.showFontsOnMap.collectAsStateWithLifecycle()
    val showWellsOnMap by viewModel.showWellsOnMap.collectAsStateWithLifecycle()
    val listSubTab by viewModel.listSubTab.collectAsStateWithLifecycle()

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
                WireframeRadarIcon(color = iconColor, modifier = Modifier.size(24.dp))
            },
            PoxSubTab("list", "LIST") { iconColor ->
                WireframeListIcon(color = iconColor, modifier = Modifier.size(24.dp))
            },
            PoxSubTab("forecast", "FORECAST") { iconColor ->
                WireframeCloudIcon(color = iconColor, modifier = Modifier.size(24.dp))
            },
            PoxSubTab("orrery", "ORRERY") { iconColor ->
                WireframeSolarSystemIcon(color = iconColor, modifier = Modifier.size(24.dp))
            },
        )
        if (activeMissionsList.isNotEmpty()) {
            list.add(
                PoxSubTab("missions", "MISSIONS") { iconColor ->
                    WireframePickaxeIcon(color = iconColor, modifier = Modifier.size(24.dp))
                },
            )
        }
        list
    }

    PoxTabFrame(
        flavorTitle = when (scannerSubTab) {
            "orrery" -> "G.E.N. P.O.X. Glass Sky V1.8"
            else -> "G.E.N. P.O.X. ANOMALY LOG V1.2"
        },
        statusText = if (selectedAnomalyId != null) {
            "LOCKED"
        } else if (scannerSubTab == "list") {
            when (listSubTab) {
                "anomalies" -> if (showAnomaliesOnMap) "ONLINE" else "MUTED"
                "fonts" -> if (showFontsOnMap) "ONLINE" else "MUTED"
                "wells" -> if (showWellsOnMap) "ONLINE" else "MUTED"
                else -> "ONLINE"
            }
        } else {
            "ONLINE"
        },
        statusColor = if (selectedAnomalyId != null) {
            Color.Red
        } else if (scannerSubTab == "list") {
            val isOnline = when (listSubTab) {
                "anomalies" -> showAnomaliesOnMap
                "fonts" -> showFontsOnMap
                "wells" -> showWellsOnMap
                else -> true
            }
            if (isOnline) CyberGreen else Color.Gray
        } else {
            CyberGreen
        },
        onStatusClick = if (selectedAnomalyId == null && scannerSubTab == "list") {
            {
                viewModel.synthManager.playBeep(600f, 0.08f, "sine")
                when (listSubTab) {
                    "anomalies" -> viewModel.toggleAnomaliesOnMap()
                    "fonts" -> viewModel.toggleFontsOnMap()
                    "wells" -> viewModel.toggleWellsOnMap()
                }
            }
        } else {
            null
        },
        headerTitle = when (scannerSubTab) {
            "orrery" -> "Single-Node Cybernetic Orerry"
            else -> "NEARBY FREQUENCY LOCATOR"
        },
        descriptionText = when (scannerSubTab) {
            "orrery" -> "Select a host to regulate P.O.X. node reactors, splicers and more."
            else -> "Analyze the detailed telemetry signature of detected local anomalies."
        },
        isScrollable = false,
        subTabs = subTabs,
        activeSubTab = scannerSubTab,
        onSubTabClick = { id, _ -> viewModel.setScannerSubTab(id) },
        viewModel = viewModel,
        drawSubTabs = false,
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    when (scannerSubTab) {
                        "missions" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (activeMissionsList.isNotEmpty()) {
                                    ActiveDeployedSequencesList(
                                        activeMissions = activeMissionsList,
                                        onSelectAnomalyByLatLng = { mLat, mLng ->
                                            val anomaly = anomalies.find { kotlin.math.abs(it.lat - mLat) < 0.0001 && kotlin.math.abs(it.lng - mLng) < 0.0001 }
                                            if (anomaly != null) {
                                                viewModel.setSelectedAnomalyId(anomaly.id)
                                            }
                                        },
                                        viewModel = viewModel,
                                    )
                                }
                            }
                        }
                        "forecast" -> {
                            ScannerWaveForecastView(viewModel = viewModel)
                        }
                        "orrery" -> {
                            ScannerCyberOrreryView(viewModel = viewModel)
                        }
                        else -> { // "list"
                            val activeListSubTab by viewModel.listSubTab.collectAsStateWithLifecycle()

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (devForceAnomaly) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.synthManager.playBeep(440f, 0.15f, "sawtooth")
                                                viewModel.recallAllActiveMissions()
                                            },
                                            modifier = Modifier.weight(1f).requiredHeight(36.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFEA580C),
                                                contentColor = Color.White,
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = "⚡ DEV: RECALL ALL",
                                                style = Typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
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
                                                contentColor = Color.White,
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = "⚡ DEV: REFRESH MAP",
                                                style = Typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                            )
                                        }
                                    }
                                }

                                when (activeListSubTab) {
                                    "anomalies" -> {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            if (anomalies.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                        .background(Color.Black.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = "NO ANOMALIES IN RANGE.",
                                                        style = Typography.bodySmall,
                                                        color = CyberGreenDim,
                                                    )
                                                }
                                            } else {
                                                anomalies.take(5).forEach { anomaly ->
                                                    AnomalyItemRow(
                                                        anomaly = anomaly,
                                                        userLat = lat,
                                                        userLng = lng,
                                                        viewModel = viewModel,
                                                        onClick = { viewModel.setSelectedAnomalyId(anomaly.id) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    "fonts" -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (baseFonts.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(150.dp)
                                                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                        .background(Color.Black.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = "NO SIGNS OF INTEL STOCK FONTS.",
                                                        style = Typography.bodySmall,
                                                        color = CyberGreenDim,
                                                    )
                                                }
                                            } else {
                                                baseFonts.forEach { font ->
                                                    FontItemRow(
                                                        font = font,
                                                        userLat = lat,
                                                        userLng = lng,
                                                        viewModel = viewModel,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    "wells" -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (celestialWells.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(150.dp)
                                                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                        .background(Color.Black.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = "NO CELESTIAL WELLS REGISTERED.",
                                                        style = Typography.bodySmall,
                                                        color = CyberGreenDim,
                                                    )
                                                }
                                            } else {
                                                celestialWells.forEach { well ->
                                                    WellItemRow(
                                                        well = well,
                                                        userLat = lat,
                                                        userLng = lng,
                                                        viewModel = viewModel,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isHarvesting) {
                val anomalyAccentColor = when (selectedAnomaly?.lifecyclePhase) {
                    AnomalyLifecyclePhase.INCEPTION -> Color(0xFF22D3EE)
                    AnomalyLifecyclePhase.ACTIVE_NEXUS -> Color(0xFFEC4899)
                    AnomalyLifecyclePhase.DESTABILIZING -> Color(0xFFFBBF24)
                    AnomalyLifecyclePhase.VOID_COLLAPSE -> Color(0xFFA855F7)
                    null -> Color(0xFF22D3EE)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, anomalyAccentColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = { activeAnomalyTab = "scan" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeAnomalyTab == "scan") anomalyAccentColor else Color.Transparent,
                            contentColor = if (activeAnomalyTab == "scan") Color.Black else anomalyAccentColor.copy(alpha = 0.7f),
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Text(
                            text = "SEQUENCE SCAN",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Button(
                        onClick = { activeAnomalyTab = "logs" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeAnomalyTab == "logs") anomalyAccentColor else Color.Transparent,
                            contentColor = if (activeAnomalyTab == "logs") Color.Black else anomalyAccentColor.copy(alpha = 0.7f),
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Text(
                            text = "TELEMETRY LOGS",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    )
}

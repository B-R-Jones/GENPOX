package com.example.genpox.ui.main

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.animation.core.*
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


// ==========================================
// ANOMALY DASHBOARD VIEW
// ==========================================
@Composable
fun AnomalyDashboardView(
    viewModel: MainViewModel,
    onAnomalyLogClick: () -> Unit
) {
    val anomalyEngineActive by viewModel.anomalyEngineActive.collectAsState()
    val grandTotalStandardNucleotides by viewModel.grandTotalStandardNucleotides.collectAsState()
    val geneSequences by viewModel.geneSequences.collectAsState()
    
    val anomalousGenesSize = remember(geneSequences) { geneSequences.count { WaveMath.isAnomalousGene(it.sequence) } }
    val multiCountAnomalousGenesSize = remember(geneSequences) { geneSequences.count { WaveMath.isAnomalousGene(it.sequence) && it.count > 1 } }
    
    val coupling = remember(grandTotalStandardNucleotides) { WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis()) }
    val chanceMetrics = remember(grandTotalStandardNucleotides, coupling) {
        WaveMath.getAnomalyEngineSuccessChance(grandTotalStandardNucleotides, coupling)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Top-Level Counts section for Anomaly tab (standardized padding & layout matching Reactor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(borderColor = CyberTheme.purpleBorder, backgroundColor = Color.Black.copy(alpha = 0.4f))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAnomalyLogClick() }
            ) {
                Text(
                    text = "UNIQUE ANOMALOUS GENE IDS",
                    color = CyberTheme.purpleDim,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 9.sp
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⬢ ", color = CyberTheme.purple, fontSize = 14.sp)
                    Text(
                        text = "$anomalousGenesSize",
                        color = Color.White,
                        style = Typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(35.dp)
                    .background(CyberTheme.purpleBorder)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "MULTI-COUNT ANOMALOUS GENE IDS",
                    color = CyberTheme.purpleDim,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 9.sp
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⬢ ", color = CyberTheme.purple, fontSize = 14.sp)
                    Text(
                        text = "$multiCountAnomalousGenesSize",
                        color = Color.White,
                        style = Typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Compact Info panel
        val meetsRequirement = grandTotalStandardNucleotides >= 250000L
        val formattedCount = String.format(Locale.US, "%,d", grandTotalStandardNucleotides)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(
                    borderColor = if (meetsRequirement) Color(0xFFA855F7) else Color(0xFF4A125E),
                    backgroundColor = Color.Black.copy(alpha = 0.6f)
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
                        text = "~",
                        color = Color(0xFFA855F7),
                        fontSize = 14.sp
                    )
                    Column {
                        Text(
                            text = "ANOMALOUS RESOURCE & LOAD",
                            color = Color(0xFFD8B4FE),
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$formattedCount / 250,000 NUCLEOTIDES",
                            color = Color.White,
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RATE: -10K/LOOP",
                        color = Color(0xFFA855F7),
                        style = Typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "RESETS STABILITY",
                        color = Color(0xFFD8B4FE),
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Anomaly Consolidation and Spectrum Coupling Panel
        val formattedFinalChance = String.format(Locale.US, "%.3f%%", chanceMetrics.finalChance)
        val formattedModifier = String.format(Locale.US, "%+.3f%%", chanceMetrics.harmonicModifier)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left: Consolidation Chance
            Box(
                modifier = Modifier
                    .weight(1f)
                    .cyberglass(borderColor = Color(0xFF4A125E), backgroundColor = Color.Black.copy(alpha = 0.45f))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONSOLIDATION CHANCE",
                            color = Color(0xFFD8B4FE),
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SUCCESS PROB",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formattedFinalChance,
                        color = Color(0xFFA855F7),
                        style = Typography.labelSmall,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right: Spectrum Coupling
            Box(
                modifier = Modifier
                    .weight(1f)
                    .cyberglass(borderColor = Color(0xFF4A125E), backgroundColor = Color.Black.copy(alpha = 0.45f))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SPECTRUM COUPLING",
                            color = Color(0xFFD8B4FE),
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "HARMONIC MODIFIER",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formattedModifier,
                        color = Color(0xFFA855F7),
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

// ==========================================
// ANOMALY VAULT VIEW
// ==========================================
@Composable
fun AnomalyVaultView(
    viewModel: MainViewModel,
    onSelectGene: (String) -> Unit,
    onClose: () -> Unit
) {
    val geneSequences by viewModel.geneSequences.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ANOMALY DISCOVERY LOG",
                color = Color(0xFFD8B4FE).copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            Box(
                modifier = Modifier
                    .cyberglass(borderColor = Color.Red, backgroundColor = Color.Transparent)
                    .clickable {
                        viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                        onClose()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕ CLOSE",
                    color = Color.Red,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Get all anomalous genes
        val anomalousGenes = remember(geneSequences) {
            geneSequences.filter { WaveMath.isAnomalousGene(it.sequence) }
        }

        if (anomalousGenes.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "ANOMALY DISCOVERY LOG EMPTY.\nENGAGE ANOMALY ENGINE TO ATTAIN ANOMALOUS GENES.",
                    color = Color(0xFF701A75),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(anomalousGenes) { gene ->
                    val benefit = WaveMath.getBenefitForAnomalousGene(gene.sequence)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberglass(borderColor = CyberTheme.purpleBorder, backgroundColor = CyberTheme.purplePanel)
                            .clickable {
                                onSelectGene(gene.sequence)
                                viewModel.synthManager.playBeep(587f, 0.05f, "triangle")
                            }
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gene.sequence,
                                    style = Typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD8B4FE)
                                )
                                Text(
                                    text = benefit.name.uppercase(),
                                    style = Typography.bodySmall,
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA855F7)
                                )
                                Text(
                                    text = benefit.description.uppercase(),
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    color = Color(0xFFCCC2DC),
                                    lineHeight = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "QTY: ",
                                    style = Typography.bodyMedium,
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD8B4FE)
                                )
                                Text(
                                    text = "x${gene.count}",
                                    style = Typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD8B4FE)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HarvestingCountdownTimer(
    viewModel: MainViewModel,
    creatureId: String
) {
    val activeMissions by viewModel.activeMissions.collectAsState()
    val m = remember(activeMissions, creatureId) {
        activeMissions.find { it.creatureId == creatureId && !it.isReturned }
    }

    if (m != null) {
        if (m.isCompleted) {
            PoxButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                text = "RECALL SEQUENCE & BANK Stockpile",
                onClick = {
                    viewModel.recallMission(m)
                },
                buttonType = PoxButtonType.CYAN_CELESTIAL,
                buttonSize = PoxButtonSize.LARGE,
                sound = PoxButtonSound.BEEP_HIGH,
                viewModel = viewModel
            )
        } else {
            // HARVESTING TEXT
            val remaining = maxOf(0L, m.totalDuration - m.elapsedSeconds)
            val timeStr = String.format(java.util.Locale.US, "%02dm %02ds", remaining / 60, remaining % 60)
            val targetGene = m.harvestedGenes.firstOrNull() ?: "UNKNOWN"
            Text(
                text = "Harvesting: $targetGene ($timeStr remaining)",
                color = Color(0xFF00E1FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }
    }
}

// ==========================================
// 4. SCANNER VIEW (styled MAP or fallback RADAR)
// ==========================================
@Composable
fun AnomalyItemStatus(
    viewModel: MainViewModel,
    anomaly: PoxAnomaly,
    factionColor: Color
) {
    val activeMissions by viewModel.activeMissions.collectAsState()
    val activeMission = remember(activeMissions, anomaly) {
        activeMissions.find { Math.abs(it.lat - anomaly.lat) < 0.0001 && Math.abs(it.lng - anomaly.lng) < 0.0001 && !it.isReturned }
    }
    
    val statusText = if (activeMission == null) {
        "No Active Harvester"
    } else {
        val remaining = maxOf(0L, activeMission.totalDuration - activeMission.elapsedSeconds)
        val timeStr = String.format(java.util.Locale.US, "%02dm %02ds", remaining / 60, remaining % 60)
        "Harvesting: $timeStr"
    }
    val statusColor = if (activeMission == null) Color.Gray else Color(0xFF00E1FF)
    Text(
        text = statusText,
        color = statusColor,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
fun AnomalyItemRow(
    anomaly: PoxAnomaly,
    userLat: Double,
    userLng: Double,
    viewModel: MainViewModel,
    onClick: () -> Unit
) {
    val latDiff = anomaly.lat - userLat
    val lngDiff = anomaly.lng - userLng
    val direction = remember(latDiff, lngDiff) {
        val angle = Math.toDegrees(Math.atan2(latDiff, lngDiff))
        val normalizedAngle = (angle + 360) % 360
        when {
            normalizedAngle >= 337.5 || normalizedAngle < 22.5 -> "E"
            normalizedAngle >= 22.5 && normalizedAngle < 67.5 -> "NE"
            normalizedAngle >= 67.5 && normalizedAngle < 112.5 -> "N"
            normalizedAngle >= 112.5 && normalizedAngle < 157.5 -> "NW"
            normalizedAngle >= 157.5 && normalizedAngle < 202.5 -> "W"
            normalizedAngle >= 202.5 && normalizedAngle < 247.5 -> "SW"
            normalizedAngle >= 247.5 && normalizedAngle < 292.5 -> "S"
            else -> "SE"
        }
    }

    val factionColor = when (anomaly.faction) {
        "Infection" -> Color(0xFFEF4444)
        "Mech" -> Color(0xFFFBBF24)
        "Parasite" -> Color(0xFFA855F7)
        else -> Color(0xFF22D3EE) // Containment
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(CyberPanel)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(factionColor)
                )
                Text(
                    text = anomaly.name.uppercase(),
                    style = Typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "[${anomaly.faction.uppercase()}]",
                    style = Typography.labelSmall,
                    fontSize = 8.sp,
                    color = factionColor
                )
            }
            val densityVal = Math.round(anomaly.density * 100.0).toInt()
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DIST: ${anomaly.distance.toInt()} FT",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "DIR: $direction",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "DENSITY: ${if (densityVal >= 0) "+" else ""}$densityVal%",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
            
            AnomalyItemStatus(
                viewModel = viewModel,
                anomaly = anomaly,
                factionColor = factionColor
            )
        }

        Box(
            modifier = Modifier
                .border(1.dp, factionColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = anomaly.gene,
                color = factionColor,
                style = Typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun getPhaseLabel(phase: String): String {
    return when (phase) {
        "TRAVEL" -> "TRANSIT TO BOUNDARY"
        "DESCENT" -> "DESCENT INTO WELL"
        "HARVESTING" -> "ACTIVE EXTRACTION"
        "ASCENT" -> "ASCENT TO BOUNDARY"
        "TRANST_BACK" -> "TRANSIT TO BASE"
        else -> "EXTRACTION COMPLETED"
    }
}

@Composable
fun LockedAnomalyActiveMissionContent(
    viewModel: MainViewModel,
    anomaly: PoxAnomaly,
    factionColor: Color,
    activeAnomalyTab: String,
    creatures: List<Creature>
) {
    val activeMissions by viewModel.activeMissions.collectAsState()
    val activeMission = remember(activeMissions, anomaly) {
        activeMissions.find { Math.abs(it.lat - anomaly.lat) < 0.0001 && Math.abs(it.lng - anomaly.lng) < 0.0001 && !it.isReturned }
    }

    if (activeMission == null) return

    val isLogTab = activeAnomalyTab == "logs"

    if (isLogTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[ MISSION TELEMETRY LOGS ACTIVE ]",
                    color = factionColor,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ANOMALY PROBE TELEMETRY (OVERRIDE)",
                color = Color.White,
                style = Typography.bodyMedium,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val displayedLogs = remember { mutableStateListOf<String>() }
            LaunchedEffect(activeMission.missionLogs) {
                if (activeMission.missionLogs.isEmpty()) {
                    displayedLogs.clear()
                } else {
                    val isNewRun = displayedLogs.size > activeMission.missionLogs.size ||
                            (displayedLogs.isNotEmpty() && activeMission.missionLogs.firstOrNull() != displayedLogs.firstOrNull())
                    val startIdx = if (isNewRun) {
                        displayedLogs.clear()
                        0
                    } else {
                        displayedLogs.size
                    }
                    for (i in startIdx until activeMission.missionLogs.size) {
                        delay(150L)
                        displayedLogs.add(activeMission.missionLogs[i])
                    }
                }
            }

            val listState = rememberLazyListState()
            LaunchedEffect(displayedLogs.size) {
                if (displayedLogs.isNotEmpty()) {
                    listState.animateScrollToItem(displayedLogs.size - 1)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF050505))
                    .border(1.dp, Color(0xFF2D0A0A), RoundedCornerShape(2.dp))
                    .padding(6.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displayedLogs) { log ->
                    val isAlert = log.contains("FAILED") || log.contains("WARNING") || log.contains("Failed") || log.contains("stalled") || log.contains("STALL") || log.contains("stalling")
                    Text(
                        text = log,
                        color = if (isAlert) Color(0xFFFCA5A5) else Color(0xFF34D399),
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }
        }
    } else {
        val elapsed = activeMission.elapsedSeconds
        val total = activeMission.totalDuration
        val progressPercent = if (total > 0) (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
        val remaining = maxOf(0L, total - elapsed)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "HARVEST DISPATCH IN PROGRESS",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Progress bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(Color.Black, RoundedCornerShape(4.dp))
                        .border(1.dp, factionColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent)
                            .background(Color(0xFF00E1FF), RoundedCornerShape(4.dp))
                    )
                }
                Text(
                    text = "${(progressPercent * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "MISSION PHASE:", color = CyberGreenDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = getPhaseLabel(activeMission.phase),
                    color = Color(0xFF00E1FF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "EST. REMAINING:", color = CyberGreenDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = String.format(java.util.Locale.US, "%02dm %02ds", remaining / 60, remaining % 60),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val trackedMissionId by viewModel.trackedMissionId.collectAsState()
            val isTrackingThis = trackedMissionId == activeMission.id

            PoxButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (isTrackingThis) "✕ STOP TRACKING" else "TRACK HARVESTER",
                onClick = {
                    if (isTrackingThis) {
                        viewModel.setTrackedMissionId(null)
                    } else {
                        viewModel.setTrackedMissionId(activeMission.id)
                    }
                },
                buttonType = if (isTrackingThis) PoxButtonType.GREEN_PHOSPHOR else PoxButtonType.GREEN_MUTED,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (activeMission.isCompleted) {
                val recallButtonType = when (anomaly.faction) {
                    "Infection" -> PoxButtonType.RED_DANGER
                    "Mech" -> PoxButtonType.YELLOW_WARNING
                    "Parasite" -> PoxButtonType.PURPLE_ANOMALY
                    else -> PoxButtonType.CYAN_CELESTIAL
                }
                PoxButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "RECALL SEQUENCE & BANK Stockpile",
                    onClick = {
                        viewModel.recallMission(activeMission)
                        viewModel.setSelectedAnomalyId(null)
                    },
                    buttonType = recallButtonType,
                    buttonSize = PoxButtonSize.LARGE,
                    sound = PoxButtonSound.BEEP_HIGH,
                    viewModel = viewModel
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ANALYSIS IN PROGRESS. RETRIEVAL ACTIVE UPON SYNC COMPLETION.",
                    color = Color.Gray,
                    fontSize = 7.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val activeCreature = remember(creatures, activeMission.creatureId) {
                creatures.find { it.id == activeMission.creatureId }
            }
            val currentSeq = activeCreature?.sequence ?: activeMission.originalSequence ?: ""
            val originalSeq = activeMission.originalSequence ?: currentSeq

            DnaComparisonGrid(
                original = originalSeq,
                current = currentSeq
            )
        }
    }
}

@Composable
fun LockedAnomalyDetails(
    anomaly: PoxAnomaly,
    viewModel: MainViewModel,
    userLat: Double,
    userLng: Double,
    activeMissionCoords: Set<String>,
    depletedAnomalyCoords: Set<String>,
    activeAnomalyTab: String,
    modifier: Modifier = Modifier
) {
    val factionColor = when (anomaly.faction) {
        "Infection" -> Color(0xFFEF4444)
        "Mech" -> Color(0xFFFBBF24)
        "Parasite" -> Color(0xFFA855F7)
        else -> Color(0xFF22D3EE)
    }

    val creatures by viewModel.creatures.collectAsState()

    val isHarvesting = remember(activeMissionCoords, anomaly) {
        activeMissionCoords.contains("${anomaly.lat},${anomaly.lng}")
    }

    val isDepleted = remember(depletedAnomalyCoords, anomaly) {
        depletedAnomalyCoords.contains("${anomaly.lat},${anomaly.lng}")
    }

    val scanRadius by viewModel.scanRadius.collectAsState()
    val isHarvestable = anomaly.distance <= scanRadius

    val isLogTab = isHarvesting && activeAnomalyTab == "logs"

    Column(
        modifier = modifier
            .border(1.dp, factionColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(CyberPanel)
            .padding(if (isLogTab) 0.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (isLogTab) 0.dp else 6.dp)
    ) {
        if (isLogTab) {
            LockedAnomalyActiveMissionContent(
                viewModel = viewModel,
                anomaly = anomaly,
                factionColor = factionColor,
                activeAnomalyTab = activeAnomalyTab,
                creatures = creatures
            )
        } else {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[ TARGET ZONE LOCK: ${anomaly.id} ]",
                    color = factionColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0xFF990000), RoundedCornerShape(2.dp))
                        .background(Color.Black)
                        .clickable {
                            viewModel.synthManager.playBeep(450f, 0.05f, "sine")
                            viewModel.setSelectedAnomalyId(null)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "✕ CLOSE",
                        color = Color.Red,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Details Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, CyberBorder.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "CLASSIFICATION:", color = CyberGreenDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(text = anomaly.faction.uppercase(), color = factionColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "COORDINATES:", color = CyberGreenDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = String.format(java.util.Locale.US, "%.5f, %.5f", anomaly.lat, anomaly.lng),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "DISTANCE:", color = CyberGreenDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "${anomaly.distance.toInt()} FT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "ENVIRONMENT DENSITY:", color = CyberGreenDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    val densityVal = Math.round(anomaly.density * 100.0).toInt()
                    val densityStr = "${if (densityVal >= 0) "+" else ""}$densityVal%"
                    Text(text = densityStr, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "GENE CARRIER:", color = CyberGreenDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(text = anomaly.gene, color = factionColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "EST. ACCURACY:", color = CyberGreenDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    val activeCreature by viewModel.activeCreature.collectAsState()
                    val creaturesList by viewModel.creatures.collectAsState()
                    val selectedCreature = activeCreature ?: creaturesList.maxByOrNull { it.defense }
                    
                    val yieldChance = if (selectedCreature != null) {
                        val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
                        val phaseFraction = wave.lunarAge / WaveMath.LUNAR_MONTH_DAYS
                        val lunarPhaseScale = (1.0 - kotlin.math.cos(phaseFraction * 2.0 * Math.PI)) / 2.0
                        val lunarResistanceMod = 0.7 + 0.6 * lunarPhaseScale

                        val boundaryRadius = anomaly.getBoundaryRadiusForPlayer(userLat, userLng)
                        val R_base = boundaryRadius * 0.1
                        val R_anom = R_base * lunarResistanceMod

                        val resonanceMod = viewModel.getSynodicResonanceMod(selectedCreature.faction, wave.phaseName)
                        val effectiveDefense = selectedCreature.defense + resonanceMod

                        Math.round((effectiveDefense.toDouble() / R_anom) * 100.0).toInt().coerceIn(0, 100)
                    } else {
                        100
                    }
                    Text(text = "$yieldChance% EXTRACT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Action or status
            when {
                isHarvesting -> {
                    LockedAnomalyActiveMissionContent(
                        viewModel = viewModel,
                        anomaly = anomaly,
                        factionColor = factionColor,
                        activeAnomalyTab = activeAnomalyTab,
                        creatures = creatures
                    )
                }
                isDepleted -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ANOMALY SIGNATURE DEPLETED",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                else -> {
                    val dispatchButtonType = when (anomaly.faction) {
                        "Infection" -> PoxButtonType.RED_DANGER
                        "Mech" -> PoxButtonType.YELLOW_WARNING
                        "Parasite" -> PoxButtonType.PURPLE_ANOMALY
                        else -> PoxButtonType.CYAN_CELESTIAL
                    }
                    PoxButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "DISPATCH SEQUENCE TO ANOMALY",
                        onClick = {
                            viewModel.setSelectedAnomalyId(anomaly.id)
                            viewModel.setActiveCreature(null, openedFrom = "scanner")
                            viewModel.selectTab("vault")
                        },
                        buttonType = dispatchButtonType,
                        buttonSize = PoxButtonSize.LARGE,
                        sound = PoxButtonSound.BEEP_HIGH,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveDeployedSequencesList(
    activeMissions: List<HarvestMission>,
    onSelectAnomalyByLatLng: (Double, Double) -> Unit,
    viewModel: MainViewModel
) {
    if (activeMissions.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(CyberPanel)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "ACTIVE DEPLOYED SEQUENCES (${activeMissions.size})",
            color = Color.White,
            style = Typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            activeMissions.forEach { m ->
                val progressPercent = if (m.totalDuration > 0) (m.elapsedSeconds.toFloat() / m.totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                val remaining = maxOf(0L, m.totalDuration - m.elapsedSeconds)
                val factionColor = when (m.creatureFaction) {
                    "Infection" -> Color(0xFFEF4444)
                    "Mech" -> Color(0xFFFBBF24)
                    "Parasite" -> Color(0xFFA855F7)
                    else -> Color(0xFF22D3EE)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, factionColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { onSelectAnomalyByLatLng(m.lat, m.lng) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = m.creatureName.uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = String.format(java.util.Locale.US, "REM: %02dm %02ds", remaining / 60, remaining % 60),
                                color = CyberGreenDim,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "STALL: %3d%%", m.stalledDepth.toInt()),
                                color = CyberGreenDim,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.DarkGray, RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressPercent)
                                    .background(Color(0xFF00E1FF), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    val trackedMissionId by viewModel.trackedMissionId.collectAsState()
                    val isTrackingThis = trackedMissionId == m.id

                    Box(
                        modifier = Modifier
                            .border(1.dp, if (isTrackingThis) CyberGreen else Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            .background(if (isTrackingThis) CyberGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                viewModel.synthManager.playBeep(480f, 0.05f, "sine")
                                if (isTrackingThis) {
                                    viewModel.setTrackedMissionId(null)
                                } else {
                                    viewModel.setTrackedMissionId(m.id)
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isTrackingThis) "TRK_ON" else "TRACK",
                            color = if (isTrackingThis) CyberGreen else Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (m.isCompleted) factionColor else Color.Gray.copy(alpha = 0.4f),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (m.isCompleted) "READY" else "HARVESTING",
                            color = if (m.isCompleted) factionColor else Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

class PrecomputedMissionState(
    val mission: HarvestMission,
    val hasShield: Boolean,
    val combinedDensity: Double,
    val boundLat: Double,
    val boundLng: Double,
    val startLat: Double,
    val startLng: Double
)


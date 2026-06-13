package com.example.genpox.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.Creature
import com.example.genpox.data.GeneSequence
import com.example.genpox.data.HarvestMission
import com.example.genpox.theme.*
import com.example.genpox.ui.components.CanvasMetrics
import com.example.genpox.ui.components.NodeCrystalCanvas
import com.example.genpox.ui.components.MapStyle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// 1. COMBINATOR VIEW (MAIN SCENE)
// ==========================================
import com.example.genpox.data.WaveMath

// ==========================================
// 1. COMBINATOR VIEW (BIO-LAB TAB SCENE)
// ==========================================
@Composable
fun CombinatorView(viewModel: MainViewModel) {
    val bioLabSubTab by viewModel.bioLabSubTab.collectAsState()
    val idleTime by viewModel.idleTime.collectAsState()
    val boostSecondsLeft by viewModel.boostSecondsLeft.collectAsState()
    val anomalyEngineActive by viewModel.anomalyEngineActive.collectAsState()
    val discoveredPacketsLog by viewModel.discoveredPacketsLog.collectAsState()
    val scrollingGene by viewModel.scrollingGene.collectAsState()
    val grandTotalStandardNucleotides by viewModel.grandTotalStandardNucleotides.collectAsState()
    val geneSequences by viewModel.geneSequences.collectAsState()

    // UI colors and themes based on subtab
    val activeColor = if (bioLabSubTab == "pox") CyberGreen else Color(0xFFA855F7)
    val activeColorDim = if (bioLabSubTab == "pox") CyberGreenDim else Color(0xFF701A75)
    val activeBorder = if (bioLabSubTab == "pox") CyberBorder else Color(0xFF4A125E)
    val activePanel = if (bioLabSubTab == "pox") CyberPanel else Color(0xFF150B24)

    // Local Overlay Visibility States
    var showStepSearchOverlay by remember { mutableStateOf(false) }
    var showPacketLogOverlay by remember { mutableStateOf(false) }
    var showAnomalyVaultOverlay by remember { mutableStateOf(false) }

    // Step Search local states
    var stepSearchPrefix by remember { mutableStateOf("") }
    var viewStepSearchMatchesOnly by remember { mutableStateOf(false) }
    var stepSearchSelectedGene by remember { mutableStateOf<String?>(null) }

    // Packet Log local states
    var selectedPacketByGene by remember { mutableStateOf<String?>(null) }
    var packetLogQuery by remember { mutableStateOf("") }

    // Selected anomalous gene details (within Anomaly Vault overlay)
    var selectedAnomalousGene by remember { mutableStateOf<String?>(null) }

    // Wave config calculations
    val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
    
    // Auto-generate metrics for NodeCrystalCanvas based on synodic daily wave configuration
    val metrics = remember(wave, bioLabSubTab) {
        var widthG = 0.5f
        var widthA = 0.5f
        var widthT = 0.5f
        var widthC = 0.5f
        
        when (wave.primary) {
            "G" -> widthG = wave.primaryMultiplier.toFloat()
            "A" -> widthA = wave.primaryMultiplier.toFloat()
            "T" -> widthT = wave.primaryMultiplier.toFloat()
            "C" -> widthC = wave.primaryMultiplier.toFloat()
        }
        when (wave.secondary) {
            "G" -> widthG = wave.secondaryMultiplier.toFloat()
            "A" -> widthA = wave.secondaryMultiplier.toFloat()
            "T" -> widthT = wave.secondaryMultiplier.toFloat()
            "C" -> widthC = wave.secondaryMultiplier.toFloat()
        }
        
        val scale = 0.5f
        CanvasMetrics(
            subnodeDepthG = 0.5f,
            subnodeDepthA = 0.5f,
            subnodeDepthT = 0.5f,
            subnodeDepthC = 0.5f,
            totalNodeDepth = 0.6f,
            subnodeWidthG = widthG * scale,
            subnodeWidthA = widthA * scale,
            subnodeWidthT = widthT * scale,
            subnodeWidthC = widthC * scale,
            totalNodeWidth = 0.8f,
            colorR = if (bioLabSubTab == "pox") 0 else 168,
            colorG = if (bioLabSubTab == "pox") 255 else 85,
            colorB = if (bioLabSubTab == "pox") 102 else 247
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. SUB-TAB HEADER SWITCHER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                    .background(activePanel)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { viewModel.setBioLabSubTab("pox") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bioLabSubTab == "pox") CyberGreen else Color.Transparent,
                        contentColor = if (bioLabSubTab == "pox") Color.Black else CyberGreenDim
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "P.O.X. REACTOR",
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.setBioLabSubTab("anomaly") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bioLabSubTab == "anomaly") Color(0xFFA855F7) else Color.Transparent,
                        contentColor = if (bioLabSubTab == "anomaly") Color.White else Color(0xFF701A75)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "ANOMALY ENGINE",
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. MAIN REACTOR CARD (representing Left Pane)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                    .background(activePanel)
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (bioLabSubTab == "pox") {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[ G.E.N. P.O.X. Tide Pool Reactor V2.4 ]",
                                color = CyberGreenDim,
                                style = Typography.labelSmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(
                                text = "SYSTEMS ON",
                                color = CyberGreen,
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        // Title
                        Text(
                            text = "Single-Node Cybernetic Synthesizer",
                            color = Color.White,
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )

                        // Top-Level Counts section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unique Gene IDs",
                                    color = CyberGreenDim,
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⬢ ",
                                        color = CyberGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${geneSequences.size}",
                                        color = Color.White,
                                        style = Typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(35.dp)
                                    .background(CyberBorder)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "Multi-Count Gene IDs",
                                    color = CyberGreenDim,
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⬢ ",
                                        color = CyberGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${geneSequences.filter { it.count > 1 }.size}",
                                        color = Color.White,
                                        style = Typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Today's base-pair wave card
                        val todayWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
                        val tomorrowWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 86400000L)
                        val dayAfterWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 172800000L)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (todayWave.isSuppressed) Color(0xFF990000).copy(alpha = 0.8f) else CyberGreen,
                                    RoundedCornerShape(4.dp)
                                )
                                .background(if (todayWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.6f))
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
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (todayWave.isSuppressed) "DORMANT (CONGESTED DECAY)" else "ACTIVE: ${todayWave.pair} WAVE",
                                            color = Color.White,
                                            style = Typography.bodySmall,
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
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Text(
                                            text = "1.12x & 1.62x BOOST",
                                            color = CyberGreenDim,
                                            style = Typography.labelSmall,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "NULL",
                                        color = Color.Red,
                                        style = Typography.labelSmall,
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
                                    .border(
                                        1.dp,
                                        if (tomorrowWave.isSuppressed) Color(0xFF990000).copy(alpha = 0.6f) else Color.DarkGray,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(if (tomorrowWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f))
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
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (tomorrowWave.isSuppressed) "DORMANT" else "${tomorrowWave.pair} WAVE",
                                            color = Color.White,
                                            style = Typography.labelSmall,
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
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Day After Tomorrow
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (dayAfterWave.isSuppressed) Color(0xFF990000).copy(alpha = 0.6f) else Color.DarkGray,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(if (dayAfterWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f))
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
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (dayAfterWave.isSuppressed) "DORMANT" else "${dayAfterWave.pair} WAVE",
                                            color = Color.White,
                                            style = Typography.labelSmall,
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
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Anomaly tab content
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[ WARNING: UNKNOWN REACTOR ]",
                                color = Color(0xFFA855F7),
                                style = Typography.labelSmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(
                                text = "SYSTEMS ON",
                                color = Color(0xFFD8B4FE),
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "GENETIC ANOMALY HARMONIZER",
                            color = Color.White,
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )

                        // Top-Level Counts section for Anomaly tab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF4A125E), RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                                        showAnomalyVaultOverlay = true
                                    }
                            ) {
                                Text(
                                    text = "Anomalous Gene IDs",
                                    color = Color(0xFFA855F7),
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⬢ ",
                                        color = Color(0xFFA855F7),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${geneSequences.filter { WaveMath.isAnomalousGene(it.sequence) }.size}",
                                        color = Color.White,
                                        style = Typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(35.dp)
                                    .background(Color(0xFF4A125E))
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "Total Gene Stock",
                                    color = Color(0xFFA855F7),
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⬢ ",
                                        color = CyberGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${geneSequences.size}",
                                        color = Color.White,
                                        style = Typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ANOMALY ENGINE",
                                color = Color(0xFFA855F7),
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = anomalyEngineActive,
                                onCheckedChange = { active ->
                                    viewModel.setAnomalyEngineActive(active)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFA855F7),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.Black
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Info panel
                        val meetsRequirement = grandTotalStandardNucleotides >= 250000L
                        val formattedCount = String.format(Locale.US, "%,d", grandTotalStandardNucleotides)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF4A125E), RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RESOURCE COUNT:",
                                    color = Color.Gray,
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formattedCount,
                                        color = if (meetsRequirement) Color(0xFFA855F7) else Color.Red,
                                        style = Typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = " / 250,000 NUCLEOTIDES",
                                        color = Color.Gray,
                                        style = Typography.labelSmall,
                                        fontSize = 9.sp
                                    )
                                    if (!meetsRequirement) {
                                        Text(
                                            text = " ✕",
                                            color = Color.Red,
                                            style = Typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LOAD WARNING:",
                                    color = Color.Gray,
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "RESETS STABILITY & EMISSIONS TO 0",
                                    color = Color(0xFFFFB300),
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CURRENT RATE:",
                                    color = Color.Gray,
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "-10,000 BASES / LOOP",
                                    color = CyberGreen,
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Anomalous Discovery Card
                        val coupling = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
                        val chanceMetrics = WaveMath.getAnomalyEngineSuccessChance(grandTotalStandardNucleotides, coupling)
                        val formattedFinalChance = String.format(Locale.US, "%.3f%%", chanceMetrics.finalChance)
                        val formattedModifier = String.format(Locale.US, "%+.3f%%", chanceMetrics.harmonicModifier)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ANOMALOUS DISCOVERY",
                                    color = Color(0xFFD8B4FE),
                                    style = Typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formattedFinalChance,
                                    color = Color.White,
                                    style = Typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Spectrum Dial Coupling Modifier:",
                                    color = Color(0xFF00E1FF),
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = formattedModifier,
                                    color = Color(0xFF00E1FF),
                                    style = Typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Ticker Box
                    val displayText = if (bioLabSubTab == "anomaly") {
                        if (!anomalyEngineActive) {
                            "--------"
                        } else {
                            val syms = "XZYW?!$%&@#"
                            scrollingGene.mapIndexed { i, char ->
                                val idx = (char.code + i) % syms.length
                                syms[idx]
                            }.joinToString("")
                        }
                    } else {
                        scrollingGene
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(2.dp, activeColor, RoundedCornerShape(4.dp))
                            .background(Color.Black)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayText,
                            color = activeColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }

                    // Progress bar
                    val isBoosted = boostSecondsLeft > 0
                    val totalCycle = if (isBoosted) 8 else 16
                    val progress = if (bioLabSubTab == "anomaly" && !anomalyEngineActive) {
                        0f
                    } else {
                        ((totalCycle - idleTime).toFloat() / totalCycle.toFloat()).coerceIn(0f, 1f)
                    }

                    val progressColor = if (bioLabSubTab == "anomaly") {
                        if (anomalyEngineActive) Color(0xFFA855F7) else Color.DarkGray
                    } else {
                        CyberGreen
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = Color.Black
                    )

                    // Timer text and booster status
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (bioLabSubTab == "anomaly") {
                                if (anomalyEngineActive) "Anomalous Consolidation in: ${idleTime}s" else "Anomalous Consolidation: IDLE"
                            } else {
                                "Gene Array Ready in: ${idleTime}s"
                            },
                            color = if (bioLabSubTab == "anomaly") {
                                if (anomalyEngineActive) Color(0xFFA855F7) else Color.Gray
                            } else {
                                CyberGreenDim
                            },
                            fontWeight = FontWeight.Bold,
                            style = Typography.bodySmall
                        )

                        if (boostSecondsLeft > 0 && bioLabSubTab == "pox") {
                            Text(
                                text = String.format(Locale.US, "REACTOR BOOST ACTIVE: %02d:%02d REMAINING", boostSecondsLeft / 60, boostSecondsLeft % 60),
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold,
                                style = Typography.labelSmall,
                                modifier = Modifier
                                    .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                    .border(1.dp, CyberGreen.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 5. ACTION NAVIGATION OVERLAY BUTTONS
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { showStepSearchOverlay = true },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f), contentColor = activeColor),
                            border = BorderStroke(1.dp, activeBorder),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(text = "MOLECULAR STEP-SEARCH", style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { showPacketLogOverlay = true },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f), contentColor = activeColor),
                                border = BorderStroke(1.dp, activeBorder),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "BATCH PACKET LOG", style = Typography.labelSmall)
                            }

                            Button(
                                onClick = { showAnomalyVaultOverlay = true },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f), contentColor = activeColor),
                                border = BorderStroke(1.dp, activeBorder),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "DECRYPTED VAULT", style = Typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.synthManager.playBeep(650f, 0.05f, "sine")
                        showPacketLogOverlay = true
                    },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bioLabSubTab == "anomaly") Color(0xFF701A75).copy(alpha = 0.2f) else Color(0xFF003CFF).copy(alpha = 0.2f),
                        contentColor = if (bioLabSubTab == "anomaly") Color(0xFFD8B4FE) else Color(0xFF00E1FF)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (bioLabSubTab == "anomaly") Color(0xFFA855F7).copy(alpha = 0.5f) else Color(0xFF003CFF).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (bioLabSubTab == "anomaly") "View Anomaly Discovery Log" else "View Gene Synthesis Log",
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.triggerManualAcceleration() },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bioLabSubTab == "anomaly") Color(0xFF701A75).copy(alpha = 0.2f) else CyberGreenDim.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (bioLabSubTab == "anomaly") Color(0xFFA855F7).copy(alpha = 0.8f) else CyberGreen.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Manual Acceleration (-2s)",
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // OVERLAY 1: MOLECULAR STEP-SEARCH
        // ==========================================
        if (showStepSearchOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .border(2.dp, activeColor, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MOLECULAR STEP-SEARCH DIRECTORY",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                        Text(
                            text = "CLOSE",
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                showStepSearchOverlay = false
                                stepSearchPrefix = ""
                                viewStepSearchMatchesOnly = false
                                stepSearchSelectedGene = null
                            }
                        )
                    }

                    // Progress indicators (1-2bp, 3-4bp, 5-6bp, 7-8bp)
                    val activeStep = stepSearchPrefix.length / 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("1-2bp", "3-4bp", "5-6bp", "7-8bp").forEachIndexed { idx, stepLabel ->
                            val isCompleted = activeStep > idx
                            val isActive = activeStep == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isActive) activeColor else if (isCompleted) activeColorDim.copy(alpha = 0.5f) else Color.DarkGray,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .background(if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stepLabel,
                                    style = Typography.labelSmall,
                                    color = if (isActive || isCompleted) activeColor else Color.Gray,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Query & Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                            .background(activePanel)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "PREFIX: ",
                                    style = Typography.labelSmall,
                                    color = activeColorDim
                                )
                                for (i in 0 until 4) {
                                    val block = if (stepSearchPrefix.length >= (i + 1) * 2) {
                                        stepSearchPrefix.substring(i * 2, (i + 1) * 2)
                                    } else {
                                        "••"
                                    }
                                    val isCurrent = activeStep == i
                                    Text(
                                        text = block,
                                        style = Typography.bodyMedium,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else if (block != "••") activeColor else Color.DarkGray
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (activeStep > 0) {
                                    Text(
                                        text = "UNDO",
                                        style = Typography.labelSmall,
                                        color = Color.Yellow,
                                        modifier = Modifier.clickable {
                                            stepSearchPrefix = stepSearchPrefix.dropLast(2)
                                            viewStepSearchMatchesOnly = false
                                            viewModel.synthManager.playCombinatorTick()
                                        }
                                    )
                                }
                                Text(
                                    text = "RESET",
                                    style = Typography.labelSmall,
                                    color = Color.Red,
                                    modifier = Modifier.clickable {
                                        stepSearchPrefix = ""
                                        viewStepSearchMatchesOnly = false
                                        stepSearchSelectedGene = null
                                        viewModel.synthManager.playReject()
                                    }
                                )
                            }
                        }
                    }

                    // Main display area: Grid or list of matches
                    val allInventoryGenes = geneSequences.map { it.sequence }
                    val isDone = activeStep == 4

                    if (isDone || viewStepSearchMatchesOnly) {
                        // Matches View
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RESOLVED stock MATCHES:",
                                    style = Typography.labelSmall,
                                    color = activeColorDim
                                )
                                if (!isDone) {
                                    Text(
                                        text = "BACK TO GRID",
                                        style = Typography.labelSmall,
                                        color = activeColor,
                                        modifier = Modifier.clickable { viewStepSearchMatchesOnly = false }
                                    )
                                }
                            }
                            
                            val matches = allInventoryGenes.filter { it.startsWith(stepSearchPrefix) }
                            
                            if (matches.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = "No matching standard or anomalous genes found.", color = activeColorDim, style = Typography.bodySmall)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(matches) { matchSeq ->
                                        val count = geneSequences.find { it.sequence == matchSeq }?.count ?: 0
                                        val isAnom = WaveMath.isAnomalousGene(matchSeq)
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.dp,
                                                    if (isAnom) Color(0xFFA855F7).copy(alpha = 0.5f) else activeBorder,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .background(if (isAnom) Color(0xFF1E0B36) else activePanel)
                                                .clickable {
                                                    stepSearchSelectedGene = matchSeq
                                                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                                }
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = matchSeq,
                                                    style = Typography.bodyMedium,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                                                )
                                                Text(
                                                    text = if (isAnom) "ANOMALY SECTOR" else "STANDARD GENE",
                                                    style = Typography.labelSmall,
                                                    color = if (isAnom) Color(0xFFA855F7) else activeColorDim
                                                )
                                            }
                                            Text(
                                                text = "QTY: x$count",
                                                style = Typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Grid View (Select Couple)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SELECT SEQUENCE COUPLE TO FILTER:",
                                style = Typography.labelSmall,
                                color = activeColorDim,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            val couples = listOf("AA", "AC", "AG", "AT", "CA", "CC", "CG", "CT", "GA", "GC", "GG", "GT", "TA", "TC", "TG", "TT")
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(couples) { couple ->
                                    val potential = stepSearchPrefix + couple
                                    val matchCount = allInventoryGenes.count { it.startsWith(potential) }
                                    val isEnabled = matchCount > 0
                                    
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                if (isEnabled) activeColor else Color.DarkGray.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .background(if (isEnabled) activePanel else Color.Transparent)
                                            .clickable(enabled = isEnabled) {
                                                stepSearchPrefix = potential
                                                viewModel.synthManager.playCombinatorTick()
                                            }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = couple,
                                                style = Typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isEnabled) Color.White else Color.Gray.copy(alpha = 0.3f),
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                            if (isEnabled) {
                                                Text(
                                                    text = "x$matchCount types",
                                                    style = Typography.labelSmall,
                                                    fontSize = 6.5.sp,
                                                    color = activeColorDim
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Intermediate matches shortcut button
                    if (!isDone && stepSearchPrefix.isNotEmpty() && !viewStepSearchMatchesOnly) {
                        Button(
                            onClick = { viewStepSearchMatchesOnly = true },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeColorDim.copy(alpha = 0.15f), contentColor = activeColor),
                            border = BorderStroke(1.dp, activeColorDim.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(text = "VIEW INTERMEDIATE MATCHES", style = Typography.labelSmall)
                        }
                    }
                }
            }
        }

        // ==========================================
        // OVERLAY 2: BATCH PACKET LOG
        // ==========================================
        if (showPacketLogOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .border(2.dp, activeColor, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYNTHESIS PACKET ARCHIVE LOG",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                        Text(
                            text = "CLOSE",
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                showPacketLogOverlay = false
                                packetLogQuery = ""
                                selectedPacketByGene = null
                            }
                        )
                    }

                    // Search Box
                    TextField(
                        value = packetLogQuery,
                        onValueChange = { packetLogQuery = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = activeBorder,
                            focusedContainerColor = activePanel,
                            unfocusedContainerColor = activePanel
                        ),
                        placeholder = { Text("Search gene sequence in logs...", color = activeColorDim, style = Typography.bodySmall) },
                        textStyle = Typography.bodySmall
                    )

                    // Logs List
                    val filteredPackets = discoveredPacketsLog.filter { packet ->
                        packetLogQuery.isEmpty() || packet.genes.any { it.contains(packetLogQuery.uppercase()) }
                    }

                    if (filteredPackets.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = "NO SYNTHESIS PACKETS RECORDED IN THIS SECTOR.",
                                color = activeColorDim,
                                style = Typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredPackets) { packet ->
                                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(packet.timestamp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (packet.isAnomalous) Color(0xFFA855F7).copy(alpha = 0.5f) else activeBorder,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .background(if (packet.isAnomalous) Color(0xFF150B24) else activePanel)
                                        .padding(6.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Packet Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (packet.isAnomalous) "ANOMALOUS UNSTABLE FUSION" else "STANDARD REACTOR SYNTH",
                                                style = Typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (packet.isAnomalous) Color(0xFFD8B4FE) else activeColor
                                            )
                                            Text(
                                                text = timeStr,
                                                style = Typography.labelSmall,
                                                color = activeColorDim,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }

                                        // Genes display
                                        if (packet.isAnomalous) {
                                            val anomalousGene = packet.genes.firstOrNull() ?: "DECAYED!"
                                            val isDecayed = anomalousGene == "DECAYED!"
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, if (isDecayed) Color.Red.copy(alpha = 0.3f) else Color(0xFFA855F7).copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                                    .background(Color.Black)
                                                    .clickable(enabled = !isDecayed) {
                                                        selectedPacketByGene = anomalousGene
                                                        viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                                    }
                                                    .padding(6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = anomalousGene,
                                                    style = Typography.bodyMedium,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDecayed) Color.Red else Color(0xFFD8B4FE)
                                                )
                                                if (!isDecayed) {
                                                    Text(
                                                        text = "SECURED [DIAGNOSE]",
                                                        style = Typography.labelSmall,
                                                        fontSize = 7.5.sp,
                                                        color = Color(0xFFD8B4FE)
                                                    )
                                                } else {
                                                    Text(
                                                        text = "DECOMPOSED",
                                                        style = Typography.labelSmall,
                                                        fontSize = 7.5.sp,
                                                        color = Color.Red
                                                    )
                                                }
                                            }
                                        } else {
                                            // 8 standard genes grid (4x2)
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(4),
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                items(packet.genes) { gene ->
                                                    Box(
                                                        modifier = Modifier
                                                            .border(1.dp, activeBorder.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                                            .background(Color.Black)
                                                            .clickable {
                                                                selectedPacketByGene = gene
                                                                viewModel.synthManager.playBeep(330f, 0.04f, "sine")
                                                            }
                                                            .padding(2.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = gene,
                                                            style = Typography.labelSmall,
                                                            fontSize = 7.5.sp,
                                                            color = activeColor,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
            }
        }

        // ==========================================
        // OVERLAY 3: DECRYPTED ANOMALY VAULT
        // ==========================================
        if (showAnomalyVaultOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .border(2.dp, Color(0xFFA855F7), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌌 DECRYPTED ANOMALY VAULT",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD8B4FE)
                        )
                        Text(
                            text = "CLOSE",
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                showAnomalyVaultOverlay = false
                                selectedAnomalousGene = null
                            }
                        )
                    }

                    // Get all anomalous genes
                    val anomalousGenes = geneSequences.filter { WaveMath.isAnomalousGene(it.sequence) }

                    if (anomalousGenes.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = "DECRYPTED VAULT EMPTY.\nENGAGE ANOMALY ENGINE TO ATTAIN COSMIC GENES.",
                                color = Color(0xFF701A75),
                                style = Typography.bodySmall,
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
                                        .border(1.dp, Color(0xFF4A125E), RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E0B36))
                                        .clickable {
                                            selectedAnomalousGene = gene.sequence
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
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD8B4FE)
                                            )
                                            Text(
                                                text = benefit.name,
                                                style = Typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFA855F7)
                                            )
                                            Text(
                                                text = benefit.description,
                                                style = Typography.labelSmall,
                                                color = Color(0xFFCCC2DC),
                                                lineHeight = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "QTY: x${gene.count}",
                                            style = Typography.bodyMedium,
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

        // ==========================================
        // SUB-DIAGNOSTIC POPUP: GENE DETAILS
        // ==========================================
        selectedPacketByGene?.let { gene ->
            val isAnom = WaveMath.isAnomalousGene(gene)
            val benefit = if (isAnom) WaveMath.getBenefitForAnomalousGene(gene) else null
            
            AlertDialog(
                onDismissRequest = { selectedPacketByGene = null },
                containerColor = if (isAnom) Color(0xFF150B24) else activePanel,
                title = {
                    Text(
                        text = if (isAnom) "🌌 GENE ANOMALY DIAGNOSTIC" else "🔬 STANDARD GENE",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SEQUENCE: $gene",
                            style = Typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                        )

                        if (isAnom && benefit != null) {
                            Text(
                                text = "CLASSIFICATION: SYNODIC ANOMALOUS UNIT",
                                style = Typography.bodySmall,
                                color = Color(0xFFA855F7),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PASSIVE ACTION: ${benefit.name}",
                                style = Typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = benefit.description,
                                style = Typography.labelSmall,
                                color = Color.LightGray,
                                lineHeight = 12.sp
                            )
                        } else {
                            Text(
                                text = "CLASSIFICATION: STABLE NUCLEOTIDE UNIT",
                                style = Typography.bodySmall,
                                color = activeColorDim,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Base sequence configuration matches local sector synthesis. Highly stable chromatids.",
                                style = Typography.labelSmall,
                                color = Color.LightGray,
                                lineHeight = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedPacketByGene = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAnom) Color(0xFFA855F7) else activeColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("CLOSE DIAGNOSTIC", style = Typography.labelSmall)
                    }
                }
            )
        }

        // ==========================================
        // SUB-DIAGNOSTIC POPUP: STEP-SEARCH GENE DETAILS
        // ==========================================
        stepSearchSelectedGene?.let { gene ->
            val isAnom = WaveMath.isAnomalousGene(gene)
            val benefit = if (isAnom) WaveMath.getBenefitForAnomalousGene(gene) else null
            
            AlertDialog(
                onDismissRequest = { stepSearchSelectedGene = null },
                containerColor = if (isAnom) Color(0xFF150B24) else activePanel,
                title = {
                    Text(
                        text = if (isAnom) "🌌 DECRYPTED ANOMALY MOLECULE" else "🔬 MOLECULAR STOCK DETAILED VIEW",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SEQUENCE: $gene",
                            style = Typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                        )

                        if (isAnom && benefit != null) {
                            Text(
                                text = "TACTICAL PERK: ${benefit.name}",
                                style = Typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = benefit.description,
                                style = Typography.labelSmall,
                                color = Color.LightGray,
                                lineHeight = 12.sp
                            )
                        } else {
                            Text(
                                text = "Standard stable sequence. Ideal for creature telomere reinforcement operations.",
                                style = Typography.labelSmall,
                                color = Color.LightGray,
                                lineHeight = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { stepSearchSelectedGene = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAnom) Color(0xFFA855F7) else activeColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("DISMISS", style = Typography.labelSmall)
                    }
                }
            )
        }

        // ==========================================
        // SUB-DIAGNOSTIC POPUP: VAULT GENE DETAILS
        // ==========================================
        selectedAnomalousGene?.let { gene ->
            val benefit = WaveMath.getBenefitForAnomalousGene(gene)
            
            AlertDialog(
                onDismissRequest = { selectedAnomalousGene = null },
                containerColor = Color(0xFF150B24),
                title = {
                    Text(
                        text = "🌌 ANOMALY QUANTUM DECRYPT",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD8B4FE)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SEQUENCE: $gene",
                            style = Typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD8B4FE)
                        )
                        Text(
                            text = "PERK ID: ${benefit.id}",
                            style = Typography.labelSmall,
                            color = Color(0xFFA855F7)
                        )
                        Text(
                            text = "DESIGNATION: ${benefit.name}",
                            style = Typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = benefit.description,
                            style = Typography.labelSmall,
                            color = Color.LightGray,
                            lineHeight = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedAnomalousGene = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("DISMISS", style = Typography.labelSmall)
                    }
                }
            )
        }
    }
}

// ==========================================
// 2. SPLICER VIEW
// ==========================================
@Composable
fun SplicerView(viewModel: MainViewModel) {
    val creatures by viewModel.creatures.collectAsState()
    val inventoryGenes by viewModel.geneSequences.collectAsState()
    val selectedCreature by viewModel.selectedSplicerCreature.collectAsState()
    val selectedGene by viewModel.selectedGeneToAppend.collectAsState()

    if (creatures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "NO HOST CREATURES COMPILED.\nUSE COMBINATOR TAB TO SEED HOST DNA.",
                color = CyberGreenDim,
                style = Typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "CHROMOSOME SPLICING BAY", style = Typography.titleMedium, color = CyberGreen)

        if (selectedCreature == null) {
            // Screen 1: Select host creature to splice
            Text(text = "SELECT HOST GENOME BASE:", style = Typography.labelSmall, color = CyberGreenDim)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(creatures) { creature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(CyberPanel)
                            .clickable { viewModel.setSplicerCreature(creature) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = creature.name, style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                            Text(text = "SECTOR: ${creature.faction} | TELO: ${creature.telomeres ?: 100}%", style = Typography.bodySmall, color = CyberGreenDim)
                        }
                        Text(text = "ENGAGE", style = Typography.labelSmall, color = CyberGreen)
                    }
                }
            }
        } else {
            // Screen 2: host creature selected, choose gene to weld
            val host = selectedCreature!!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "HOST: ${host.name}", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                Text(
                    text = "DISENGAGE",
                    style = Typography.labelSmall,
                    color = Color.Red,
                    modifier = Modifier.clickable { viewModel.setSplicerCreature(null) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "GENE SEQUENCE:", style = Typography.labelSmall, color = CyberGreenDim)
                    Text(text = host.sequence.take(32) + "...", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "WELDED GENES: ${host.appendedGenes?.joinToString(", ") ?: "NONE"}", style = Typography.bodySmall, color = CyberGreen)
                    Text(text = "TELOMERES STABILITY: ${host.telomeres ?: 100}%", style = Typography.bodySmall, color = if ((host.telomeres ?: 100) < 40) Color.Red else CyberGreen)
                }
            }

            Text(text = "SELECT GENE TO WELD (+10% TELOMERES):", style = Typography.labelSmall, color = CyberGreenDim)

            if (inventoryGenes.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "GENE DECK EMPTY.\nHARVEST MAP ANOMALIES TO SECURE GENES.",
                        color = CyberGreenDim,
                        style = Typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(inventoryGenes) { gene ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (selectedGene == gene.sequence) CyberGreen else CyberBorder, RoundedCornerShape(4.dp))
                                .background(if (selectedGene == gene.sequence) CyberBorder else CyberPanel)
                                .clickable { viewModel.selectGeneToAppend(gene.sequence) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = gene.sequence, style = Typography.bodyMedium, color = CyberGreen)
                            Text(text = "QTY: ${gene.count}", style = Typography.bodySmall, color = CyberGreenDim)
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.weldGeneToCreature() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedGene != null) CyberGreen else CyberPanel,
                    contentColor = if (selectedGene != null) Color.Black else CyberGreenDim
                ),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, CyberBorder),
                enabled = selectedGene != null
            ) {
                Text(text = "FUSE GENE TELEMETRY", style = Typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 3. GEN-VAULT (LIBRARY) VIEW
// ==========================================
@Composable
fun VaultView(viewModel: MainViewModel) {
    val creatures by viewModel.creatures.collectAsState()
    val activeCreature by viewModel.activeCreature.collectAsState()

    if (creatures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "GEN-VAULT DATABANKS OFFLINE.\nSEED GENETIC HOSTS IN COMBINATOR.",
                color = CyberGreenDim,
                style = Typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (activeCreature == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "SECURE GEN-VAULT LIBRARY", style = Typography.titleMedium, color = CyberGreen)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(creatures) { creature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(CyberPanel)
                            .clickable { viewModel.setActiveCreature(creature) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = creature.name, style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                            Text(text = "SECTOR: ${creature.faction} | HP: ${creature.vitality}", style = Typography.bodySmall, color = CyberGreenDim)
                        }
                        Text(text = "DECRYPT >", style = Typography.labelSmall, color = CyberGreen)
                    }
                }
            }
        }
    } else {
        // Creature detailed HUD card
        val c = activeCreature!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "VAULT TELEMETRY DATA", style = Typography.titleMedium, color = CyberGreen)
                Text(
                    text = "VAULT LIST",
                    style = Typography.labelSmall,
                    color = CyberGreen,
                    modifier = Modifier.clickable { viewModel.setActiveCreature(null) }
                )
            }

            // The ASCII artwork box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = c.asciiArt,
                        style = Typography.bodyLarge,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = CyberGreen,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bio description block
            Column {
                Text(text = c.name.uppercase(), style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                Text(text = "CLASSIFICATION: ${c.type}", style = Typography.bodySmall, color = CyberGreenDim)
                Text(text = "SECTOR: ${c.faction} | ORIGIN: ${c.origin}", style = Typography.bodySmall, color = CyberGreenDim)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(CyberPanel)
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "COMBAT TELEMETRY:", style = Typography.labelSmall, color = CyberGreen)
                    Text(text = "HP (VITALITY): ${c.vitality}", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "ATTACK RATIO: ${c.attack}", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "DEFENSE SHIELD: ${c.defense}", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "SPEED ACCEL: ${c.speed}", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "INTEGRATED WEAPON: ${c.primaryWeapon}", style = Typography.bodySmall, color = CyberGreen)
                }
            }

            Text(
                text = c.lore,
                style = Typography.bodySmall,
                color = CyberGreenDim,
                lineHeight = 16.sp
            )

            Button(
                onClick = {
                    viewModel.synthManager.playCreatureSequenceAudio(c.sequence)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPanel, contentColor = CyberGreen),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, CyberBorder)
            ) {
                Text(text = "EMIT SEQUENCE RESONANCE", style = Typography.labelSmall)
            }
        }
    }
}

// ==========================================
// 4. SCANNER VIEW (styled MAP or fallback RADAR)
// ==========================================
@Composable
fun ScannerView(viewModel: MainViewModel) {
    val mapVisible = remember { mutableStateOf(false) } // Toggle to holographic radar by default for retro look

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "TACTICAL AREA SCANNER", style = Typography.titleMedium, color = CyberGreen)
            Text(
                text = if (mapVisible.value) "HOLO-RADAR" else "GEOMAP",
                style = Typography.labelSmall,
                color = CyberGreen,
                modifier = Modifier
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .clickable { mapVisible.value = !mapVisible.value }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        if (mapVisible.value) {
            // Real map screen container
            GoogleMapsScanner(viewModel)
        } else {
            // Holographic radar sweep view
            HolographicRadarScanner(viewModel)
        }

        // Active harvest missions list
        Text(text = "ACTIVE HARVEST DISPATCHES:", style = Typography.labelSmall, color = CyberGreenDim)
        val activeMissions by viewModel.activeMissions.collectAsState()

        if (activeMissions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "NO HARVEST SHUTTLES DISPATCHED.", style = Typography.bodySmall, color = CyberGreenDim)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(activeMissions) { mission ->
                    val elapsed = (System.currentTimeMillis() - mission.startTime) / 1000f
                    val progress = (elapsed / mission.totalDuration).coerceIn(0f, 1f)
                    val isDone = progress >= 1f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(CyberPanel)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${mission.creatureName} (${mission.creatureFaction})", style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = CyberGreen)
                            Text(text = "GENE DEST: ${mission.harvestedGenes.joinToString()}", style = Typography.bodySmall, color = CyberGreenDim)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                color = CyberGreen,
                                trackColor = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.recallMission(mission) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDone) CyberGreen else CyberPanel,
                                contentColor = if (isDone) Color.Black else CyberGreenDim
                            ),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            enabled = isDone,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(text = "RECALL", style = Typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// 4a. Google Maps wrapper
@Composable
fun GoogleMapsScanner(viewModel: MainViewModel) {
    val lat by viewModel.latitude.collectAsState()
    val lng by viewModel.longitude.collectAsState()
    val scanRadius by viewModel.scanRadius.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()
    val creatures by viewModel.creatures.collectAsState()

    val location = LatLng(lat, lng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 17f)
    }

    val selectedAnomaly = remember { mutableStateOf<PoxAnomaly?>(null) }

    LaunchedEffect(lat, lng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 17f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = MapStyleOptions(MapStyle.JSON_STYLE)
            )
        ) {
            // Draw center player location
            Marker(
                state = MarkerState(position = location),
                title = "PLAYER TRANSMITTER BEACON"
            )

            // Draw Anomalies
            anomalies.forEach { anomaly ->
                Marker(
                    state = MarkerState(position = LatLng(anomaly.lat, anomaly.lng)),
                    title = "${anomaly.name} (${anomaly.gene})",
                    onClick = {
                        selectedAnomaly.value = anomaly
                        viewModel.synthManager.playBeep(770f, 0.05f, "square")
                        false
                    }
                )
            }
        }

        // Selected anomaly popup HUD overlay
        selectedAnomaly.value?.let { anomaly ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .border(1.dp, CyberGreen, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = anomaly.name, style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = CyberGreen)
                        Text(
                            text = "CLOSE",
                            style = Typography.labelSmall,
                            color = Color.Red,
                            modifier = Modifier.clickable { selectedAnomaly.value = null }
                        )
                    }
                    Text(text = "GENE SIGNATURE: ${anomaly.gene}", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "DISTANCE: ${anomaly.distance.toInt()} FT | SECTOR: ${anomaly.faction}", style = Typography.bodySmall, color = CyberGreenDim)

                    if (creatures.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "DISPATCH FOR PROBE:", style = Typography.labelSmall, color = CyberGreenDim)
                            val listState = remember { mutableStateOf(false) }

                            Button(
                                onClick = { listState.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPanel, contentColor = CyberGreen),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = "CHOOSE UNIT", style = Typography.labelSmall)
                            }

                            if (listState.value) {
                                AlertDialog(
                                    onDismissRequest = { listState.value = false },
                                    containerColor = CyberPanel,
                                    title = { Text("SELECT EXPEDITION UNIT", style = Typography.titleMedium, color = CyberGreen) },
                                    text = {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(creatures) { c ->
                                                Text(
                                                    text = c.name,
                                                    style = Typography.bodyMedium,
                                                    color = CyberGreen,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.dispatchMission(c, anomaly)
                                                            listState.value = false
                                                            selectedAnomaly.value = null
                                                        }
                                                        .padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {}
                                )
                            }
                        }
                    } else {
                        Text(text = "NO COMPILED CREATURES AVAILABLE TO DISPATCH.", style = Typography.bodySmall, color = Color.Red)
                    }
                }
            }
        }
    }
}

// 4b. Custom holographic radar view fallback (Premium retro visual)
@Composable
fun HolographicRadarScanner(viewModel: MainViewModel) {
    val anomalies by viewModel.anomalies.collectAsState()
    val creatures by viewModel.creatures.collectAsState()
    val selectedAnomaly = remember { mutableStateOf<PoxAnomaly?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = size.width / 2 * 0.9f

            // Radar concentric rings
            drawCircle(Color(0xFF00FF41).copy(alpha = 0.15f), radius = maxR, center = Offset(cx, cy), style = Stroke(width = 1f))
            drawCircle(Color(0xFF00FF41).copy(alpha = 0.1f), radius = maxR * 0.6f, center = Offset(cx, cy), style = Stroke(width = 0.5f))
            drawCircle(Color(0xFF00FF41).copy(alpha = 0.05f), radius = maxR * 0.3f, center = Offset(cx, cy), style = Stroke(width = 0.5f))

            // Crosshair lines
            drawLine(Color(0xFF00FF41).copy(alpha = 0.15f), Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 0.5f)
            drawLine(Color(0xFF00FF41).copy(alpha = 0.15f), Offset(cx, cy - maxR), Offset(cx, cy + maxR), strokeWidth = 0.5f)

            // Dynamic sweep ray
            val radians = Math.toRadians(sweepAngle.toDouble())
            val sweepX = cx + maxR * cos(radians).toFloat()
            val sweepY = cy + maxR * sin(radians).toFloat()
            drawLine(
                brush = Brush.sweepGradient(
                    colors = listOf(CyberGreen.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(cx, cy)
                ),
                start = Offset(cx, cy),
                end = Offset(sweepX, sweepY),
                strokeWidth = 2f
            )

            // Draw center beacon player transceiver
            drawCircle(CyberGreen, radius = 3.5f, center = Offset(cx, cy))
        }

        // Clickable interactive blips matching anomalies
        Box(modifier = Modifier.size(190.dp)) {
            anomalies.forEachIndexed { idx, anomaly ->
                val angleOffset = idx * 72f
                val distanceFraction = 0.2f + (idx * 0.15f)
                val rad = Math.toRadians(angleOffset.toDouble())
                val x = 95 + (85 * distanceFraction * cos(rad)).toFloat()
                val y = 95 + (85 * distanceFraction * sin(rad)).toFloat()

                val factionColor = when (anomaly.faction) {
                    "Infection" -> Color.Red
                    "Mech" -> Color.Yellow
                    "Parasite" -> Color(0xFFA855F7)
                    else -> Color.Cyan
                }

                Box(
                    modifier = Modifier
                        .offset(x.dp - 6.dp, y.dp - 6.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(factionColor.copy(alpha = 0.8f))
                        .clickable {
                            selectedAnomaly.value = anomaly
                            viewModel.synthManager.playBeep(660f, 0.05f, "sine")
                        }
                )
            }
        }

        // Anomaly overlay
        selectedAnomaly.value?.let { anomaly ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .border(1.dp, CyberGreen, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = anomaly.name, style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = CyberGreen)
                        Text(
                            text = "CLOSE",
                            style = Typography.labelSmall,
                            color = Color.Red,
                            modifier = Modifier.clickable { selectedAnomaly.value = null }
                        )
                    }
                    Text(text = "GENE COORD: ${anomaly.gene}", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "SECTOR: ${anomaly.faction} | DIST: ${anomaly.distance.toInt()} FT", style = Typography.bodySmall, color = CyberGreenDim)

                    if (creatures.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "DEPLOY EXPEDITION UNIT:", style = Typography.labelSmall, color = CyberGreenDim)
                            val listState = remember { mutableStateOf(false) }

                            Button(
                                onClick = { listState.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPanel, contentColor = CyberGreen),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = "SELECT UNIT", style = Typography.labelSmall)
                            }

                            if (listState.value) {
                                AlertDialog(
                                    onDismissRequest = { listState.value = false },
                                    containerColor = CyberPanel,
                                    title = { Text("CHOOSE EXPEDITION UNIT", style = Typography.titleMedium, color = CyberGreen) },
                                    text = {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(creatures) { c ->
                                                Text(
                                                    text = c.name,
                                                    style = Typography.bodyMedium,
                                                    color = CyberGreen,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.dispatchMission(c, anomaly)
                                                            listState.value = false
                                                            selectedAnomaly.value = null
                                                        }
                                                        .padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {}
                                )
                            }
                        }
                    } else {
                        Text(text = "NO TELEMETRY HOSTS TO DEPLOY.", style = Typography.bodySmall, color = Color.Red)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. FORECAST VIEW
// ==========================================
@Composable
fun ForecastView(viewModel: MainViewModel) {
    val dailyBounties by viewModel.dailyBounties.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "DAILY GENE FORECASTS", style = Typography.titleMedium, color = CyberGreen)
        Text(
            text = "SCAN TARGET TELEMETRIES HARVEST PATHWAYS:",
            style = Typography.labelSmall,
            color = CyberGreenDim
        )

        // Draw forecast grids
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(dailyBounties.take(28)) { bounty ->
                val first8 = bounty.take(8)
                Box(
                    modifier = Modifier
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .background(CyberPanel)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = first8, style = Typography.bodySmall, fontSize = 9.sp, color = CyberGreen)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "CO coherence", style = Typography.bodySmall, fontSize = 6.sp, color = CyberGreenDim)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. INVENTORY VIEW
// ==========================================
@Composable
fun InventoryView(viewModel: MainViewModel) {
    val geneSequences by viewModel.geneSequences.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "TRANSCIEVER GENE INVENTORY", style = Typography.titleMedium, color = CyberGreen)

        if (geneSequences.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO GENE GENES RESOLVED.\nHARVEST TARGETS VIA MAP SCANNER.",
                    color = CyberGreenDim,
                    style = Typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(geneSequences) { seq ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(CyberPanel)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "BLOCK: ${seq.sequence}", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                            Text(
                                text = "ACQUIRED: " + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(seq.discoveredAt)),
                                style = Typography.bodySmall,
                                color = CyberGreenDim
                            )
                        }
                        Text(
                            text = "QTY: ${seq.count}",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. SETTINGS VIEW
// ==========================================
@Composable
fun SettingsView(viewModel: MainViewModel) {
    val apiKey by viewModel.geminiApiKey.collectAsState()
    val mute by viewModel.muteSound.collectAsState()
    val textState = remember { mutableStateOf("") }

    LaunchedEffect(apiKey) {
        textState.value = apiKey
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "SYSTEM CONFIGURATION", style = Typography.titleMedium, color = CyberGreen)

        // API Key Settings Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "GEMINI API CREDENTIALS", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                Text(text = "Paste your Google AI Studio API Key to unlock high-fidelity creature compiling.", style = Typography.bodySmall, color = CyberGreenDim)

                TextField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CyberGreen,
                        unfocusedTextColor = CyberGreen,
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black
                    ),
                    placeholder = { Text("Paste AIStudio API Key...", color = CyberGreenDim, style = Typography.bodySmall) }
                )

                Button(
                    onClick = { viewModel.saveApiKey(textState.value) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("SAVE KEY TO DATASTORE", style = Typography.labelSmall)
                }
            }
        }

        // General settings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "SOUND MANAGEMENT", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "MUTE retro synthesis chimes:", style = Typography.bodySmall, color = CyberGreenDim)
                    Switch(
                        checked = mute,
                        onCheckedChange = { viewModel.setMute(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyberGreen,
                            uncheckedThumbColor = CyberGreenDim,
                            uncheckedTrackColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}

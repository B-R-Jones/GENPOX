package com.example.genpox.ui.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.genpox.PoxApplication
import com.example.genpox.data.WaveMath
import com.example.genpox.theme.*
import com.example.genpox.ui.components.PoxCameraScanner
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PoxApplication
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(app.repository)
    )

    val selectedTab by viewModel.selectedTab.collectAsState()
    val logs by viewModel.terminalLogs.collectAsState()
    val showScanner by viewModel.showScanner.collectAsState()
    val geneSequences by viewModel.geneSequences.collectAsState()

    val countA = remember(geneSequences) {
        geneSequences.filter { !WaveMath.isAnomalousGene(it.sequence) }.sumOf { gene ->
            gene.sequence.count { it == 'A' || it == 'a' } * gene.count
        }
    }
    val countG = remember(geneSequences) {
        geneSequences.filter { !WaveMath.isAnomalousGene(it.sequence) }.sumOf { gene ->
            gene.sequence.count { it == 'G' || it == 'g' } * gene.count
        }
    }
    val countT = remember(geneSequences) {
        geneSequences.filter { !WaveMath.isAnomalousGene(it.sequence) }.sumOf { gene ->
            gene.sequence.count { it == 'T' || it == 't' } * gene.count
        }
    }
    val countC = remember(geneSequences) {
        geneSequences.filter { !WaveMath.isAnomalousGene(it.sequence) }.sumOf { gene ->
            gene.sequence.count { it == 'C' || it == 'c' } * gene.count
        }
    }

    val logListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to the latest log automatically
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            coroutineScope.launch {
                logListState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. CRT CONSOLE HEADER SYSTEM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(CyberPanel)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "G.E.N.P.O.X. MAIN",
                        style = Typography.titleLarge,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGreen
                    )
                    Text(
                        text = "STATUS: ONLINE [PORTRAIT ENHANCED]",
                        style = Typography.labelSmall,
                        fontSize = 8.sp,
                        color = CyberGreenDim
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // SCAN CODE ACTION BUTTON
                    Text(
                        text = "SCAN CODE",
                        style = Typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .background(CyberGreen, RoundedCornerShape(4.dp))
                            .clickable { viewModel.toggleScanner(true) }
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    )

                    // GNPX Developer Mode Badge
                    val devForceAnomaly by viewModel.devForceAnomaly.collectAsState()
                    Text(
                        text = "GNPX",
                        style = Typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (devForceAnomaly) Color(0xFFF97316) else Color.White,
                        modifier = Modifier
                            .background(
                                if (devForceAnomaly) Color(0xFFF97316).copy(alpha = 0.15f) else Color(0xFF00FF41).copy(alpha = 0.10f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (devForceAnomaly) Color(0xFFF97316) else Color(0x8000FF41),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { viewModel.toggleDevForceAnomaly() }
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    )

                    // Active pulsator badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(CyberGreen)
                        )
                        Text(
                            text = "NTP_SYNC",
                            style = Typography.labelSmall,
                            fontSize = 8.sp,
                            color = CyberGreen
                        )
                    }
                }
            }

            // 1b. Simplified global Nucleotide stockpile counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Pair("A", countA),
                    Pair("G", countG),
                    Pair("T", countT),
                    Pair("C", countC)
                ).forEach { (nucleotide, count) ->
                    Text(
                        text = "[ $nucleotide: $count ]",
                        color = CyberGreen,
                        style = Typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Text(
                    text = "• GEN ACTIVE",
                    color = CyberGreenDim,
                    style = Typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // 2. MAIN HUD ACTIVE DISPLAY SCREEN
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                    .background(CyberPanel)
                    .padding(10.dp)
            ) {
                when (selectedTab) {
                    "combinator" -> CombinatorView(viewModel)
                    "splicer" -> SplicerView(viewModel)
                    "vault" -> VaultView(viewModel)
                    "scanner" -> ScannerView(viewModel)
                    "forecast" -> ForecastView(viewModel)
                    "inventory" -> InventoryView(viewModel)
                    "settings" -> SettingsView(viewModel)
                }
            }

            // 3. RETRO TERMINAL PRINT LOGS SCREEN (Global Footer Ticker)
            val discoveredPacketsLog by viewModel.discoveredPacketsLog.collectAsState()
            val bottomLogText = remember(discoveredPacketsLog) {
                if (discoveredPacketsLog.isNotEmpty()) {
                    val packet = discoveredPacketsLog.last()
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(packet.timestamp))
                    val count = packet.genes.size
                    val packetTypeStr = if (packet.isAnomalous) "ANOMALOUS FUSION" else "GENE SYNTHESIS"
                    val packetDetailStr = if (packet.isAnomalous) "ANOMALOUS GENE DETECTED" else "$count NEW GENES"
                    "[$timeStr] ${packetTypeStr.uppercase()} COMPLETE: PACKET READY ($packetDetailStr)"
                } else {
                    "[18:55:21] GENE SYNTHESIS COMPLETE: PACKET READY (5 NEW GENES)"
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = bottomLogText,
                    color = CyberGreen,
                    style = Typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // 4. BOTTOM TACTILE RUBBER NAVIGATION DECK KEYS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Pair("combinator", "BIO-LAB"),
                        Pair("splicer", "SPLICER"),
                        Pair("vault", "GEN-VAULT"),
                        Pair("scanner", "SCANNER")
                    ).forEach { tab ->
                        Button(
                            onClick = { viewModel.selectTab(tab.first) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == tab.first) CyberGreen else CyberPanel,
                                contentColor = if (selectedTab == tab.first) Color.Black else CyberGreenDim
                            ),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = tab.second,
                                style = Typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Pair("forecast", "FORECAST"),
                        Pair("inventory", "INVENTORY"),
                        Pair("settings", "SETTINGS")
                    ).forEach { tab ->
                        Button(
                            onClick = { viewModel.selectTab(tab.first) },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == tab.first) CyberGreen else CyberPanel,
                                contentColor = if (selectedTab == tab.first) Color.Black else CyberGreenDim
                            ),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = tab.second,
                                style = Typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Camera barcode scanner overlay
        if (showScanner) {
            PoxCameraScanner(
                onBarcodeScanned = { data ->
                    viewModel.processScannedBarcode(data)
                },
                onClose = {
                    viewModel.toggleScanner(false)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


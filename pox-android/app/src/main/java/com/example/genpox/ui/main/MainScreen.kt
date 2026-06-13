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
import com.example.genpox.theme.*
import com.example.genpox.ui.components.PoxCameraScanner
import kotlinx.coroutines.launch

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackgroundDark)
                .safeContentPadding()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // 3. RETRO TERMINAL PRINT LOGS SCREEN
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .padding(6.dp)
            ) {
                LazyColumn(
                    state = logListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = Typography.labelSmall,
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                            color = if (log.contains("ERR")) Color.Red else if (log.contains("OK")) CyberGreen else CyberGreenDim
                        )
                    }
                }
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
                        Pair("combinator", "COMPILER"),
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


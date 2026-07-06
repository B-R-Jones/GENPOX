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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.genpox.data.network.PoxNetworkManager
import com.example.genpox.theme.*

import com.example.genpox.ui.components.phosphorMonitor
import com.example.genpox.ui.components.PoxCameraScanner
import com.example.genpox.ui.components.CloudChamberVisualizer
import com.example.genpox.ui.components.PoxHoloNav
import com.example.genpox.ui.components.PoxSubTab
import com.example.genpox.ui.components.PoxRadarDials
import com.example.genpox.ui.components.HoloNavButton
import com.example.genpox.ui.components.PoxUnifiedHoloNav
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
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
        factory = MainViewModelFactory(app.repository, app.synthManager)
    )

    // Initialize synthManager context and networkManager
    LaunchedEffect(viewModel) {
        viewModel.synthManager.initialize(context)
        viewModel.networkManager = PoxNetworkManager(context.applicationContext, viewModel.synthManager)
    }


    // ------------------ GEOLOCATION CLIENT IMPLEMENTATION ------------------
    var locationPermissionGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            locationPermissionGranted = true
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) return@DisposableEffect onDispose {}

        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        
        val locationListener = object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                viewModel.updateLocation(location.latitude, location.longitude)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Fetch initial last known location to start immediately
        try {
            var bestLocation: android.location.Location? = null
            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                val loc = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                if (loc != null) bestLocation = loc
            }
            if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                val loc = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    if (bestLocation == null || loc.time > bestLocation.time) {
                        bestLocation = loc
                    }
                }
            }
            bestLocation?.let {
                viewModel.updateLocation(it.latitude, it.longitude)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Request periodic updates
        try {
            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds
                    5f, // 5 meters
                    locationListener
                )
            }
            if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    locationListener
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                locationManager.removeUpdates(locationListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // -----------------------------------------------------------------------

    // Lifecycle observer to stop audio when app goes to background
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                event == androidx.lifecycle.Lifecycle.Event.ON_STOP
            ) {
                viewModel.synthManager.stopAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Window focus observer to stop audio when app window loses focus (e.g. notification shade)
    val isWindowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(isWindowFocused) {
        if (!isWindowFocused) {
            viewModel.synthManager.stopAll()
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsState()
    val logs by viewModel.terminalLogs.collectAsState()
    val showScanner by viewModel.showScanner.collectAsState()
    val geneSequences by viewModel.geneSequences.collectAsState()
    val disintegratedModal by viewModel.disintegratedModal.collectAsState()
    val scannerSubTab by viewModel.scannerSubTab.collectAsState()
    val selectedAnomalyId by viewModel.selectedAnomalyId.collectAsState()
    val isScannerRadar = selectedTab == "scanner" && scannerSubTab == "radar" && selectedAnomalyId == null
    val scannerRadarState = rememberScannerRadarState()

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

    // Root CRT Phosphor filter animation ticker and color mapping
    val selectedReagent by viewModel.selectedReagent.collectAsState()
    val baseColor = getActiveReagentColor(selectedReagent)
    val rootInfiniteTransition = rememberInfiniteTransition(label = "root_crt_time")
    val timeMsFloat by rootInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing)
        ),
        label = "time_ms"
    )
    val timeMs = timeMsFloat.toLong()

    val scanlineOpacity by viewModel.crtScanlineOpacity.collectAsState()
    val grilleOpacity by viewModel.crtGrilleOpacity.collectAsState()
    val bloomIntensity by viewModel.crtBloomIntensity.collectAsState()
    val curvatureBezel by viewModel.crtCurvatureBezel.collectAsState()
    val glitchIntensity by viewModel.crtGlitchIntensity.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackgroundDark)
            .phosphorMonitor(
                baseColor = baseColor,
                scanlineOpacity = scanlineOpacity,
                grilleOpacity = grilleOpacity,
                bloomIntensity = bloomIntensity,
                curvatureBezel = curvatureBezel,
                glitchIntensity = glitchIntensity,
                timeMs = timeMs
            )
    ) {
        // Persistent Cloud Chamber Background Animation
        CloudChamberVisualizer(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        if (isScannerRadar) {
            ScannerView(
                viewModel = viewModel,
                isFullscreen = true,
                contentOnly = true,
                state = scannerRadarState,
                modifier = Modifier.fillMaxSize()
            )
        }

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
                    var tapCount by remember { mutableStateOf(0) }
                    Text(
                        text = "G.E.N.P.O.X. MAIN",
                        style = Typography.titleLarge,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGreen,
                        modifier = Modifier.clickable {
                            tapCount++
                            if (tapCount >= 5) {
                                tapCount = 0
                                viewModel.toggleProfiler()
                            }
                        }
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

            // 2. MAIN HUD ACTIVE DISPLAY SCREEN
            if (isScannerRadar) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(CyberPanel, RoundedCornerShape(6.dp))
                        .drawBehind {
                            val stroke = 1.dp.toPx()
                            drawRoundRect(
                                color = CyberBorder,
                                topLeft = Offset(stroke / 2f, stroke / 2f),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                style = Stroke(width = stroke)
                            )
                        }
                        .padding(10.dp)
                ) {
                    when (selectedTab) {
                        "combinator" -> CombinatorView(viewModel)
                        "splicer" -> SplicerView(viewModel)
                        "vault" -> VaultView(viewModel)
                        "scanner" -> ScannerView(viewModel, isFullscreen = false)
                        "network" -> NetworkView(viewModel)
                        "settings" -> SettingsView(viewModel)
                    }
                }
            }

            val logsChannel = remember { Channel<String>(Channel.UNLIMITED) }
            var lastSeenLogSize by remember { mutableStateOf(logs.size) }
            var displayedLogText by remember { mutableStateOf(logs.lastOrNull() ?: "GENPOX COMPILER SYSTEM v2.0 READY.") }

            LaunchedEffect(logs) {
                if (logs.size > lastSeenLogSize) {
                    for (i in lastSeenLogSize until logs.size) {
                        logsChannel.trySend(logs[i])
                    }
                } else if (logs.size < lastSeenLogSize) {
                    // Drain any queued logs in the channel because logs were cleared or reset
                    while (true) {
                        val result = logsChannel.tryReceive()
                        if (result.isFailure) break
                    }
                    displayedLogText = logs.lastOrNull() ?: "GENPOX COMPILER SYSTEM v2.0 READY."
                }
                lastSeenLogSize = logs.size
            }

            LaunchedEffect(Unit) {
                while (true) {
                    val nextLog = logsChannel.receive()
                    displayedLogText = nextLog
                    delay(1500L)
                }
            }

            val flashAlpha = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                viewModel.logFlashTrigger.collect { peerName ->
                    displayedLogText = "message received from $peerName"
                    repeat(2) {
                        flashAlpha.animateTo(0.6f, animationSpec = tween(200))
                        flashAlpha.animateTo(0f, animationSpec = tween(200))
                    }
                }
            }

            val animatedBorderColor = lerp(CyberBorder, CyberGreen, flashAlpha.value)
            val animatedBgColor = lerp(Color.Black, CyberGreen.copy(alpha = 0.25f), flashAlpha.value)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .border(1.dp, animatedBorderColor, RoundedCornerShape(4.dp))
                    .background(animatedBgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = displayedLogText,
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
                        Pair("vault", "GEN-VAULT")
                    ).forEach { tab ->
                        Button(
                            onClick = { viewModel.selectTab(tab.first) },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
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
                        Pair("scanner", "SCANNER"),
                        Pair("network", "NETWORK"),
                        Pair("settings", "SETTINGS")
                    ).forEach { tab ->
                        Button(
                            onClick = { viewModel.selectTab(tab.first) },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
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

        if (isScannerRadar) {
            PoxRadarDials(
                state = scannerRadarState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(6.dp)
            )
        }

        if (selectedTab == "scanner") {
            val scannerSubTab by viewModel.scannerSubTab.collectAsState()
            val listSubTab by viewModel.listSubTab.collectAsState()
            val activeMissions by viewModel.activeMissions.collectAsState()
            val activeMissionsList = remember(activeMissions) { activeMissions.filter { !it.isReturned } }

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
                    }
                )
                if (activeMissionsList.isNotEmpty()) {
                    list.add(
                        PoxSubTab("missions", "MISSIONS") { iconColor ->
                            WireframePickaxeIcon(color = iconColor, modifier = Modifier.size(24.dp))
                        }
                    )
                }
                list
            }

            val mainRow = subTabs.map { tab ->
                val isActive = scannerSubTab == tab.id
                HoloNavButton(
                    id = tab.id,
                    borderColor = if (isActive) CyberGreen else CyberGreen.copy(alpha = 0.24f),
                    glowColor = if (isActive) CyberGreen.copy(alpha = 0.15f) else Color.Transparent,
                    backgroundColor = if (isActive) CyberGreen else Color(0xFF0F172A).copy(alpha = 0.85f),
                    enabled = true,
                    onClick = {
                        if (scannerSubTab != tab.id) {
                            viewModel.synthManager.playCombinatorTick()
                            viewModel.setScannerSubTab(tab.id)
                        }
                    },
                    bounce = tab.bounce,
                    content = { iconColor ->
                        tab.icon(iconColor)
                    }
                )
            }

            val listSecondaryTabs = listOf(
                PoxSubTab("anomalies", "ANOMALIES") { iconColor ->
                    WireframeAnomalyIcon(color = iconColor, modifier = Modifier.size(24.dp))
                },
                PoxSubTab("fonts", "FONTS") { iconColor ->
                    WireframeFontIcon(color = iconColor, modifier = Modifier.size(24.dp))
                },
                PoxSubTab("wells", "WELLS") { iconColor ->
                    WireframeWellIcon(color = iconColor, modifier = Modifier.size(24.dp))
                }
            )

            val secondaryRow = listSecondaryTabs.map { tab ->
                val isActive = listSubTab == tab.id
                HoloNavButton(
                    id = tab.id,
                    borderColor = if (isActive) Color(0xFF22D3EE) else Color(0xFF22D3EE).copy(alpha = 0.24f),
                    glowColor = if (isActive) Color(0xFF22D3EE).copy(alpha = 0.15f) else Color.Transparent,
                    backgroundColor = if (isActive) Color(0xFF22D3EE) else Color(0xFF0F172A).copy(alpha = 0.85f),
                    enabled = true,
                    onClick = {
                        if (listSubTab != tab.id) {
                            viewModel.synthManager.playCombinatorTick()
                            viewModel.setListSubTab(tab.id)
                        }
                    },
                    bounce = tab.bounce,
                    content = { iconColor ->
                        tab.icon(iconColor)
                    }
                )
            }

            val hostPlanetState by viewModel.selectedHostPlanet.collectAsState()

            val activeBaseGroup = remember(hostPlanetState) {
                when (hostPlanetState.uppercase()) {
                    "MERCURY", "VENUS" -> "orrery_A"
                    "EARTH", "MARS" -> "orrery_G"
                    "JUPITER", "SATURN" -> "orrery_T"
                    "URANUS", "NEPTUNE" -> "orrery_C"
                    else -> "orrery_G"
                }
            }

            val orrerySecondaryTabs = listOf(
                PoxSubTab("orrery_A", "ADENINE") { iconColor ->
                    WireframeAdenineIcon(color = iconColor, modifier = Modifier.size(24.dp))
                },
                PoxSubTab("orrery_G", "GUANINE") { iconColor ->
                    WireframeGuanineIcon(color = iconColor, modifier = Modifier.size(24.dp))
                },
                PoxSubTab("orrery_T", "THYMINE") { iconColor ->
                    WireframeThymineIcon(color = iconColor, modifier = Modifier.size(24.dp))
                },
                PoxSubTab("orrery_C", "CYTOSINE") { iconColor ->
                    WireframeCytosineIcon(color = iconColor, modifier = Modifier.size(24.dp))
                }
            )

            val orrerySecondaryRow = orrerySecondaryTabs.map { tab ->
                val isActive = activeBaseGroup == tab.id
                HoloNavButton(
                    id = tab.id,
                    borderColor = if (isActive) CyberGreen else CyberGreen.copy(alpha = 0.24f),
                    glowColor = if (isActive) CyberGreen.copy(alpha = 0.15f) else Color.Transparent,
                    backgroundColor = if (isActive) CyberGreen else Color(0xFF0F172A).copy(alpha = 0.85f),
                    enabled = true,
                    onClick = {
                        if (activeBaseGroup != tab.id) {
                            viewModel.synthManager.playCombinatorTick()
                            val defaultPlanet = when (tab.id) {
                                "orrery_A" -> "MERCURY"
                                "orrery_G" -> "EARTH"
                                "orrery_T" -> "JUPITER"
                                "orrery_C" -> "URANUS"
                                else -> "EARTH"
                            }
                            viewModel.changeHostPlanet(defaultPlanet)
                        } else {
                            viewModel.synthManager.playCombinatorTick()
                            val togglePlanet = when (tab.id) {
                                "orrery_A" -> if (hostPlanetState.uppercase() == "MERCURY") "VENUS" else "MERCURY"
                                "orrery_G" -> if (hostPlanetState.uppercase() == "EARTH") "MARS" else "EARTH"
                                "orrery_T" -> if (hostPlanetState.uppercase() == "JUPITER") "SATURN" else "JUPITER"
                                "orrery_C" -> if (hostPlanetState.uppercase() == "URANUS") "NEPTUNE" else "URANUS"
                                else -> "EARTH"
                            }
                            viewModel.changeHostPlanet(togglePlanet)
                        }
                    },
                    bounce = tab.bounce,
                    content = { iconColor ->
                        tab.icon(iconColor)
                    }
                )
            }

            val rows = when (scannerSubTab) {
                "list" -> listOf(mainRow, secondaryRow)
                "orrery" -> listOf(mainRow, orrerySecondaryRow)
                else -> listOf(mainRow)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(6.dp)
            ) {
                PoxUnifiedHoloNav(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 148.dp, end = 22.dp),
                    rows = rows,
                    activeButtonIds = setOf(scannerSubTab, listSubTab, activeBaseGroup),
                    allowStacking = true,
                    viewModel = viewModel
                )
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

        // Disintegration modal overlay (sci-fi retro red style)
        disintegratedModal?.let { data ->
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    viewModel.synthManager.playSynthesisSuccess()
                    viewModel.clearDisintegratedModal()
                    viewModel.selectTab("vault")
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Titlebar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            .background(Color.Red.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.Red, RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = "[ GENOME STABILITY FAILURE ]",
                            color = Color.Red,
                            style = Typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .background(Color.Red.copy(alpha = 0.05f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "SEQUENCE TERMINATED: ${data.name.uppercase()}",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "CRITICAL ALARM: Telomere length reached 0%. Cellular transcription has failed, leading to full chromosomal instability. The species' genetic carrier construct has disintegrated entirely.",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            lineHeight = 12.sp
                        )
                    }

                    // Genes grid / returned-lost list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Genes Kept
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF1B4332).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .background(Color.Black)
                                .padding(8.dp)
                                .height(120.dp)
                        ) {
                            Text(
                                text = "GENES KEPT (${data.returnedBlocks.size})",
                                color = Color(0xFF2D6A4F),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    data.returnedBlocks.forEach { seq ->
                                        Text(
                                            text = seq,
                                            color = Color(0xFF52B788),
                                            fontSize = 8.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            modifier = Modifier
                                                .background(Color(0xFF1B4332).copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Genes Lost
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF590D22).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .background(Color.Black)
                                .padding(8.dp)
                                .height(120.dp)
                        ) {
                            Text(
                                text = "GENES LOST (${data.destroyedBlocks.size})",
                                color = Color(0xFFA4133C),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    data.destroyedBlocks.forEach { seq ->
                                        Text(
                                            text = seq,
                                            color = Color(0xFFFF4D6D),
                                            fontSize = 8.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            modifier = Modifier
                                                .background(Color(0xFF590D22).copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Button: RETURN TO GEN-VAULT DATA
                    Button(
                        onClick = {
                            viewModel.synthManager.playSynthesisSuccess()
                            viewModel.clearDisintegratedModal()
                            viewModel.selectTab("vault")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Red
                        ),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "RETURN TO GEN-VAULT DATA",
                            style = Typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                }
            }
        }
    }
}




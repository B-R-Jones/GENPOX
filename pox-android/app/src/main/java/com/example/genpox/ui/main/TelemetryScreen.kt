package com.example.genpox.ui.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.genpox.data.WaveMath
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import kotlinx.coroutines.delay
import kotlin.math.*
import androidx.compose.animation.core.*
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone
import java.util.Date
import java.text.SimpleDateFormat

data class TelemetryMetric(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val formula: String,
    val description: String,
    val effects: List<String>
)

@Composable
fun TelemetryView(viewModel: MainViewModel) {
    val metrics = remember {
        listOf(
            TelemetryMetric(
                id = "lunar_cycle",
                title = "LUNAR SYNODIC CYCLE",
                category = "COSMIC & LUNAR",
                summary = "Tracks the 29.53-day synodic lunar period which shifts global environmental properties.",
                formula = "Age = diffDays % 29.53059\nPhase = f(Age)",
                description = "The Moon acts as a celestial driver, altering baseline nucleotide multipliers and anomaly density. As the Moon age increases, the system transitions between dark and light moon phases, affecting phenotypic trigger conditions.",
                effects = listOf(
                    "Determines daily nucleotide base pairs.",
                    "Triggers lunar anomalous gene passive activations.",
                    "Drives periodic fluctuations in environmental density drag."
                )
            ),
            TelemetryMetric(
                id = "moon_modifier",
                title = "SINUSOIDAL MOON MODIFIER",
                category = "COSMIC & LUNAR",
                summary = "Calculates the dynamic multiplier peaks based on lunar orbit positions.",
                formula = "MoonAngle = (2 * PI * Age) / 29.53 - PI / 2\nModifier = 0.0125 * sin(MoonAngle)\nMult_1 = 1.125 + Modifier\nMult_2 = 1.625 + Modifier",
                description = "Orbital positioning introduces gravitational tides that distort gene generation. Modifiers oscillate sinusodially to boost nucleotide synthesis rates.",
                effects = listOf(
                    "Alters daily primary multiplier weights.",
                    "Alters daily secondary multiplier weights.",
                    "Increases nucleotide generation yields during Full Moon peaks."
                )
            ),
            TelemetryMetric(
                id = "markov_synthesis",
                title = "MARKOV GENETIC SYNTHESIS",
                category = "GENETIC SYNTHESIS",
                summary = "Weighted nucleotide selector algorithm for generating daily base-pair sequence blocks.",
                formula = "If Prev == B1 -> Weight(B2) = Mult_2\nElse -> Weight(B1) = Mult_1\nProbability = Weight_i / Sum(Weights)",
                description = "The P.O.X. Reactor synthesizes sequences using conditional weight distributions. If a sequence matches the daily active base pair pattern, consecutive nucleotides are bound with elevated probabilities.",
                effects = listOf(
                    "Controls gene sequence pattern synthesis.",
                    "Applies daily primary/secondary multipliers.",
                    "Influences automated mutation frequencies."
                )
            ),
            TelemetryMetric(
                id = "seed_hash",
                title = "DETERMINISTIC SEED HASHING",
                category = "GENETIC SYNTHESIS",
                summary = "Polynomial rolling hash algorithm used to generate daily-seeded base pairs and reactor states.",
                formula = "Hash = (Hash shl 5) - Hash + char.code\nSignedHash = Hash.toInt()\nDeterministicSeed = abs(SignedHash)",
                description = "Mimics JavaScript 32-bit signed integer hash computation from a calendar date string (YYYY-MM-DD), ensuring global synchronization of wave configurations.",
                effects = listOf(
                    "Synchronizes wave base pairs across web and Android client nodes.",
                    "Decides weekly nucleotide reactor suppression days."
                )
            ),
            TelemetryMetric(
                id = "spectrum_coupling",
                title = "SPECTRUM COUPLING HARMONIC",
                category = "REACTOR & ANOMALY PHYSICS",
                summary = "Diurnal sinusoidal oscillation tracking local sector electromagnetic fields.",
                formula = "t = daySeconds / 86400.0\nCoupling(t) = 80.0 + 12.375 * sin(t * 8 * PI)",
                description = "Simulates a 6-hour sinusoidal wave coupling. Elevated coupling values increase reactor stability and boost anomaly well engine dispatch success rates.",
                effects = listOf(
                    "Modifies anomaly engine dispatch success chance.",
                    "Triggers temporal combat benefits when coupling exceeds 82%."
                )
            ),
             TelemetryMetric(
                id = "logarithmic_success",
                title = "LOGARITHMIC SUCCESS CHANCE",
                category = "REACTOR & ANOMALY PHYSICS",
                summary = "Calculates the fusion success rate of the anomaly engine based on cumulative consumed bases.",
                formula = "t = (ln(Consumed) - ln(10000)) / (ln(250000) - ln(10000))\nBaseChance = 1.0 + 99.0 * t\nPeakBoost = 6.5 * exp(- (dist / 1.8)^2)\nHarmonicMod = (Coupling - 80) * 0.25\nFinalChance = (BaseChance + PeakBoost + HarmonicMod)",
                description = "Anomaly engine fusion success probability scales logarithmically with the cumulative standard nucleotides consumed during the active run (minimum 250k stockpile required to activate), featuring local resonance peaks near multiples of 14% and harmonic coupling modifiers.",
                effects = listOf(
                    "Determines anomaly engine unstable fusion success rate.",
                    "Adds Gaussian bumps near resonance frequencies."
                )
            ),
            TelemetryMetric(
                id = "density_drag",
                title = "ENVIRONMENT DENSITY DRAG",
                category = "REACTOR & ANOMALY PHYSICS",
                summary = "Calculates drag modifiers on anomaly well descent and ascent travel speeds.",
                formula = "EffDensity = Clamped(Density + LunarShift, -0.33, 0.33)\nIf CoherenceShield -> EffDensity = Min(0.0, EffDensity)\nVelocityMultiplier = 1.0 - EffDensity\nDuration = Distance / (V_travel * VelocityMultiplier)",
                description = "Gravitational density drag modifies creature velocities during vertical transit inside anomalies. Negative density speeds transit up, positive slows it down, and Coherence Shield nullifies drag penalties.",
                effects = listOf(
                    "Controls anomaly descent and ascent times.",
                    "Incorporates dynamic lunar shifts into well drag."
                )
            ),
            TelemetryMetric(
                id = "phenotype_encoding",
                title = "PHENOTYPIC COMBAT EXPRESSION",
                category = "PHENOTYPIC COMBAT",
                summary = "Decodes genomic sequence scans into passive traits and triggers.",
                formula = "Prefix = s0.code, Suffix = s1.code\nEffectIndex = (s0 + s1) % 6\nTriggerIndex = (s6 + s7) % 8",
                description = "Special base characters inside genomic sequences encode phenotypic benefits. Prefix/suffix names and combat actions are determined by the leading nucleotides, while conditional activation triggers are encoded at the sequence tail.",
                effects = listOf(
                    "Decodes double strikes, self-destructs, and evasions.",
                    "Establishes combat vitality, time, and harmonic trigger conditions."
                )
            )
        )
    }

    var selectedItem by remember { mutableStateOf<TelemetryMetric?>(null) }
    var isGlobeExpanded by remember { mutableStateOf(false) }
    val heatwaveCells by viewModel.heatwaveCells.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (selectedItem == null) {
                // Title Header (Sans-Serif, Uppercase, standardized)
                Text(
                    text = "TELEMETRY METRIC LIBRARY",
                    color = CyberGreenDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Scrollable List organized categorically
                val categories = remember(metrics) { metrics.groupBy { it.category } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categories.forEach { (category, categoryMetrics) ->
                        item {
                            Text(
                                text = category,
                                color = Color(0xFFF97316), // Accent color for category header
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Default,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                        items(categoryMetrics) { metric ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.synthManager.playBeep(520f, 0.05f, "sine")
                                        selectedItem = metric
                                    },
                                colors = CardDefaults.cardColors(containerColor = CyberPanel),
                                border = BorderStroke(1.dp, CyberBorder),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = metric.title,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Default
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = metric.summary,
                                        color = CyberGreenDim,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Default
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "FORMULA: " + metric.formula.lineSequence().first(),
                                            color = factionColorForCategory(category).copy(alpha = 0.8f),
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "EXPAND 🗃",
                                            color = CyberGreen,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Default
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(55.dp)) // Avoid overlap block
                    }
                }
            } else {
                // Full frame expanded view
                TelemetryDetailScreen(
                    metric = selectedItem!!,
                    onBack = {
                        viewModel.synthManager.playBeep(450f, 0.05f, "sine")
                        selectedItem = null
                    }
                )
            }
        }

        // Overlay: Small blue square button in the bottom right
        if (!isGlobeExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 12.dp)
                    .size(42.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                    .border(1.5.dp, Color(0xFF22D3EE), RoundedCornerShape(4.dp))
                    .clickable {
                        viewModel.synthManager.playBeep(600f, 0.08f, "sine")
                        isGlobeExpanded = true
                    }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                WireframeGlobe(
                    modifier = Modifier.fillMaxSize(),
                    isMini = true,
                    rotationSpeedMs = 15000,
                    activeCells = heatwaveCells,
                    viewModel = viewModel
                )
            }
        }

        // Expanded full-frame spinning wireframe Earth
        if (isGlobeExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable {
                        viewModel.synthManager.playBeep(400f, 0.08f, "sine")
                        isGlobeExpanded = false
                    }
            ) {
                WireframeGlobe(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    isMini = false,
                    rotationSpeedMs = 30000, // slowly spinning
                    activeCells = heatwaveCells,
                    viewModel = viewModel
                )

                // Close overlay button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .border(1.dp, Color(0xFF22D3EE), RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            viewModel.synthManager.playBeep(400f, 0.08f, "sine")
                            isGlobeExpanded = false
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✕ CLOSE",
                        color = Color(0xFF22D3EE),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                }
            }
        }
    }
}

private fun factionColorForCategory(category: String): Color {
    return when (category) {
        "COSMIC & LUNAR" -> Color(0xFF22D3EE)
        "GENETIC SYNTHESIS" -> Color(0xFFA855F7)
        "REACTOR & ANOMALY PHYSICS" -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }
}

@Composable
fun TelemetryDetailScreen(
    metric: TelemetryMetric,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back Button (Sans-serif uppercase)
        Box(
            modifier = Modifier
                .border(1.dp, CyberGreen, RoundedCornerShape(2.dp))
                .background(Color.Black)
                .clickable { onBack() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .align(Alignment.Start)
        ) {
            Text(
                text = "✕ BACK TO LIBRARY",
                color = CyberGreen,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }

        // Header Title (Sans-Serif, Uppercase)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = metric.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Default
            )
            Text(
                text = "[ CATEGORY: ${metric.category} ]",
                color = factionColorForCategory(metric.category),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }

        // Scientific Explanation Text (Sans-serif)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(8.dp)
        ) {
            Text(
                text = metric.description,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                lineHeight = 12.sp
            )
        }

        // Formula / Equation block (Monospace)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "MATHEMATICAL EXPRESSIONS:",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(4.dp))
                    .background(Color(0xFF030303))
                    .padding(8.dp)
            ) {
                Text(
                    text = metric.formula,
                    color = Color(0xFF00FF41),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                )
            }
        }

        // Interactive visual widget / Canvas Graph
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "INTERACTIVE SIMULATOR & GRAPH:",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                InteractiveWidgetRouter(metric.id)
            }
        }

        // Non-exhaustive effects
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "SYSTEM PHENOTYPIC EFFECTS:",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                metric.effects.forEach { effect ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "•", color = CyberGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = effect,
                            color = Color.LightGray,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Default,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(55.dp)) // Avoid overlay block
    }
}

@Composable
fun InteractiveWidgetRouter(id: String) {
    when (id) {
        "lunar_cycle" -> LunarCycleSimulator()
        "moon_modifier" -> MoonModifierGraph()
        "markov_synthesis" -> MarkovTransitionMatrix()
        "seed_hash" -> DeterministicHashFlowchart()
        "spectrum_coupling" -> SpectrumCouplingSineGraph()
        "logarithmic_success" -> LogarithmicSuccessGraph()
        "density_drag" -> DensityDragBarChart()
        "phenotype_encoding" -> PhenotypeGeneDecoder()
    }
}

@Composable
fun LunarCycleSimulator() {
    var moonAge by remember { mutableStateOf(14.77f) } // Default Full Moon
    val phaseName = when {
        moonAge < 1.0f || moonAge > 28.53f -> "New Moon"
        moonAge >= 1.0f && moonAge < 6.38f -> "Waxing Crescent"
        moonAge >= 6.38f && moonAge < 8.38f -> "First Quarter"
        moonAge >= 8.38f && moonAge < 13.77f -> "Waxing Gibbous"
        moonAge >= 13.77f && moonAge < 15.77f -> "Full Moon"
        moonAge >= 15.77f && moonAge < 21.15f -> "Waning Gibbous"
        moonAge >= 21.15f && moonAge < 23.15f -> "Third Quarter"
        else -> "Waning Crescent"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "SLIDE LUNAR AGE (DAYS):", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%.2f d [%s]", moonAge, phaseName.uppercase()),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        PoxSlider(
            value = moonAge,
            onValueChange = { moonAge = it },
            valueRange = 0f..29.53f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray
        )

        // Moon Phase shadow drawing
        Canvas(modifier = Modifier.size(60.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Background base space
            drawCircle(color = Color(0xFF0F172A), radius = radius)

            // Calculate fraction of light
            val angle = (2.0 * Math.PI * moonAge.toDouble()) / 29.53059
            val percentLight = (1.0 + cos(angle - Math.PI)) / 2.0

            // Draw moon light shape based on age
            if (moonAge < 14.77f) {
                // Waxing phases (light on right)
                // Draw full circle light
                drawCircle(color = Color(0xFFFEF08A), radius = radius)
                // Draw shadow on left
                val shadowWidth = (1.0 - percentLight) * 2.0 * radius
                if (moonAge < 7.38f) {
                    // Crescent shadow (covers left half + part of right)
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(0f, 0f),
                        size = Size(radius, size.height)
                    )
                    // Draw curved shadow on right side
                    val curveW = (radius - shadowWidth).toFloat()
                    drawOval(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(radius, 0f),
                        size = Size(abs(curveW), size.height)
                    )
                } else {
                    // Gibbous (shadow only on far left edge)
                    drawOval(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(0f, 0f),
                        size = Size(shadowWidth.toFloat(), size.height)
                    )
                }
            } else {
                // Waning phases (light on left)
                drawCircle(color = Color(0xFFFEF08A), radius = radius)
                val shadowWidth = (1.0 - percentLight) * 2.0 * radius
                if (moonAge > 22.15f) {
                    // Crescent shadow (covers right half + part of left)
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(radius, 0f),
                        size = Size(radius, size.height)
                    )
                    val curveW = (radius - shadowWidth).toFloat()
                    drawOval(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(0f, 0f),
                        size = Size(abs(curveW), size.height)
                    )
                } else {
                    // Gibbous (shadow only on right edge)
                    drawOval(
                        color = Color(0xFF0F172A),
                        topLeft = Offset((size.width - shadowWidth).toFloat(), 0f),
                        size = Size(shadowWidth.toFloat(), size.height)
                    )
                }
            }

            // Glow border stroke
            drawCircle(color = CyberGreen.copy(alpha = 0.5f), radius = radius, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

@Composable
fun MoonModifierGraph() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val w = size.width
        val h = size.height

        // Draw grid lines
        val cols = 8
        for (i in 0..cols) {
            val x = (i.toFloat() / cols) * w
            drawLine(Color(0xFF111111), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        drawLine(Color(0xFF222222), Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1.5f)

        // Plot modifier sine curve
        val path = Path()
        val numPoints = 100
        for (i in 0..numPoints) {
            val t = i.toFloat() / numPoints
            val moonAge = t * 29.53f
            val moonAngle = (2.0 * Math.PI * moonAge) / 29.53059 - Math.PI / 2.0
            val moonModifier = 0.0125 * sin(moonAngle) // range -0.0125 to 0.0125

            // Normalize Y coordinate
            val yNorm = (moonModifier - (-0.0125)) / 0.025
            val xCoord = t * w
            val yCoord = h - (yNorm.toFloat() * h)

            if (i == 0) {
                path.moveTo(xCoord, yCoord)
            } else {
                path.lineTo(xCoord, yCoord)
            }
        }
        drawPath(path, Color(0xFF22D3EE), style = Stroke(width = 2.dp.toPx()))

        // Draw labels
        drawCircle(color = Color.Red, radius = 4f, center = Offset(w / 2, h / 2))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "AGE 0d (NEW: -0.0125)", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
        Text(text = "AGE 14.7d (FULL: +0.0125)", color = CyberGreen, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
        Text(text = "AGE 29.5d (NEW)", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun MarkovTransitionMatrix() {
    var primaryBase by remember { mutableStateOf("A") }
    var secondaryBase by remember { mutableStateOf("G") }

    val bases = listOf("A", "G", "T", "C")
    val pMult = 1.125
    val sMult = 1.625

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "SELECT ACTIVE BASE PAIR WAVE:",
            color = Color.White,
            fontSize = 8.sp,
            fontFamily = FontFamily.Default
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bases.forEach { b ->
                Box(
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (primaryBase == b) CyberGreen else Color.DarkGray,
                            RoundedCornerShape(2.dp)
                        )
                        .background(if (primaryBase == b) CyberPanel else Color.Transparent)
                        .clickable {
                            primaryBase = b
                            secondaryBase = when (b) {
                                "A" -> "G"
                                "G" -> "C"
                                "T" -> "A"
                                else -> "T"
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = b, color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Text(
            text = "TRANSITION WEIGHTS FOR WAVE [ $primaryBase$secondaryBase ]:",
            color = CyberGreen,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // Matrix Grid
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Header Row
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    Text(text = "PREV \\ NEXT", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                }
                bases.forEach { b ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = b, color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Data Rows
            bases.forEach { prev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (prev == primaryBase) Color(0x1500FF66) else Color.Transparent)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Text(text = prev, color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                    bases.forEach { next ->
                        val weight = if (prev == primaryBase && next == secondaryBase) {
                            sMult
                        } else if (prev != primaryBase && next == primaryBase) {
                            pMult
                        } else {
                            1.0
                        }
                        val color = when (weight) {
                            sMult -> Color(0xFFA855F7)
                            pMult -> Color(0xFF22D3EE)
                            else -> Color.White
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format(Locale.US, "%.3f", weight),
                                color = color,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeterministicHashFlowchart() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "DATE STAMP", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(text = "-->", color = Color.Gray, fontSize = 8.sp)
            Text(text = "ROLLING HASH", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(text = "-->", color = Color.Gray, fontSize = 8.sp)
            Text(text = "DETERMINISTIC SEED", color = CyberGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
        Box(
            modifier = Modifier
                .border(1.dp, CyberBorder, RoundedCornerShape(2.dp))
                .background(Color(0xFF030303))
                .padding(8.dp)
        ) {
            Text(
                text = "hash = 0\nfor i in 0..len:\n  hash = (hash shl 5) - hash + char.code\nseed = abs(hash.toInt())",
                color = Color(0xFF00FF41),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
fun SpectrumCouplingSineGraph() {
    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    val hours = cal.get(Calendar.HOUR_OF_DAY)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)
    val daySeconds = hours * 3600 + minutes * 60 + seconds
    val dayFraction = daySeconds.toDouble() / 86400.0
    val currentCoupling = 80.0 + 12.375 * sin(dayFraction * 2.0 * Math.PI * 4.0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "TIME TICKER (DIURNAL):", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%02d:%02d:%02d -> %.2f%% COUPLING", hours, minutes, seconds, currentCoupling),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val w = size.width
            val h = size.height

            // Grid lines
            val intervals = 6
            for (i in 0..intervals) {
                val x = (i.toFloat() / intervals) * w
                drawLine(Color(0xFF111111), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            }
            drawLine(Color(0xFF222222), Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1.5f)

            // Plot Coupling
            val path = Path()
            val numPoints = 120
            for (i in 0..numPoints) {
                val fraction = i.toFloat() / numPoints
                val couplingVal = 80.0 + 12.375 * sin(fraction * 2.0 * Math.PI * 4.0)
                // Normalize Y to fit h (coupling ranges from 67.625 to 92.375)
                val normY = (couplingVal - 65.0) / 30.0
                val xCoord = fraction * w
                val yCoord = h - (normY.toFloat() * h)

                if (i == 0) {
                    path.moveTo(xCoord, yCoord)
                } else {
                    path.lineTo(xCoord, yCoord)
                }
            }
            drawPath(path, Color(0xFF34D399), style = Stroke(width = 2.dp.toPx()))

            // Draw current dot indicator
            val currentNormY = (currentCoupling - 65.0) / 30.0
            val dotX = dayFraction.toFloat() * w
            val dotY = h - (currentNormY.toFloat() * h)
            drawCircle(color = Color.Red, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
            drawCircle(color = Color.Red.copy(alpha = 0.4f), radius = 8.dp.toPx(), center = Offset(dotX, dotY), style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

@Composable
fun LogarithmicSuccessGraph() {
    var totalGenes by remember { mutableStateOf(120000f) }
    var coupling by remember { mutableStateOf(80f) }

    // Calculate dynamic success chance
    val successChance = remember(totalGenes, coupling) {
        val grandTotal = totalGenes.toLong()
        val minLog = Math.log(10000.0)
        val maxLog = Math.log(250000.0)
        val currentLog = Math.log(grandTotal.toDouble().coerceIn(10000.0, 250000.0))
        val t = (currentLog - minLog) / (maxLog - minLog)
        val baseChance = 1.0 + 99.0 * t

        val multiplesOf14 = listOf(14.0, 28.0, 42.0, 56.0, 70.0, 84.0, 98.0)
        var peakBoost = 0.0
        for (peak in multiplesOf14) {
            val dist = abs(baseChance - peak)
            if (dist < 5.0) {
                peakBoost = Math.max(peakBoost, 6.5 * exp(-(dist / 1.8).pow(2.0)))
            }
        }
        val harmonicModifier = (coupling - 80.0) * 0.25
        (baseChance + peakBoost + harmonicModifier).coerceIn(1.0, 100.0)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "CUMULATIVE CONSUMED BASES:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = "${totalGenes.toInt()} BASES",
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        PoxSlider(
            value = totalGenes,
            onValueChange = { totalGenes = it },
            valueRange = 10000f..250000f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "COUPLING AMPLITUDE:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = "${coupling.toInt()}%",
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        PoxSlider(
            value = coupling,
            onValueChange = { coupling = it },
            valueRange = 60f..100f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray
        )

        Text(
            text = String.format(Locale.US, "CALCULATED SUCCESS RATE: %.2f%%", successChance),
            color = factionColorForCategory("REACTOR & ANOMALY PHYSICS"),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val w = size.width
            val h = size.height

            // Grid
            drawLine(Color(0xFF111111), Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), strokeWidth = 1f)
            drawLine(Color(0xFF111111), Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 1f)
            drawLine(Color(0xFF111111), Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), strokeWidth = 1f)

            // Plot line graph from 10k to 250k
            val path = Path()
            val numPoints = 150
            for (i in 0..numPoints) {
                val pt = i.toFloat() / numPoints
                val gVol = 10000.0 + pt * (250000.0 - 10000.0)

                val minLog = Math.log(10000.0)
                val maxLog = Math.log(250000.0)
                val currentLog = Math.log(gVol)
                val ptT = (currentLog - minLog) / (maxLog - minLog)
                val base = 1.0 + 99.0 * ptT

                val multiplesOf14 = listOf(14.0, 28.0, 42.0, 56.0, 70.0, 84.0, 98.0)
                var pBoost = 0.0
                for (peak in multiplesOf14) {
                    val dist = abs(base - peak)
                    if (dist < 5.0) {
                        pBoost = Math.max(pBoost, 6.5 * exp(-(dist / 1.8).pow(2.0)))
                    }
                }
                val finalCh = (base + pBoost + (coupling - 80.0) * 0.25).coerceIn(1.0, 100.0)

                val xCoord = pt * w
                val yCoord = h - (finalCh.toFloat() / 100f * h)

                if (i == 0) {
                    path.moveTo(xCoord, yCoord)
                } else {
                    path.lineTo(xCoord, yCoord)
                }
            }
            drawPath(path, Color(0xFFFBBF24), style = Stroke(width = 2.dp.toPx()))

            // Mark current state point
            val currentPctX = (totalGenes - 10000f) / (250000f - 10000f)
            val dotX = currentPctX * w
            val dotY = h - (successChance.toFloat() / 100f * h)
            drawCircle(color = Color.Red, radius = 4.dp.toPx(), center = Offset(dotX, dotY))
        }
    }
}

@Composable
fun DensityDragBarChart() {
    var baseDensity by remember { mutableStateOf(0.15f) } // default positive density
    var lunarShift by remember { mutableStateOf(-0.05f) }
    var shieldActive by remember { mutableStateOf(false) }

    val effectiveDensity = (baseDensity + lunarShift).coerceIn(-0.33f, 0.33f)
    val mitigatedDensity = if (shieldActive && effectiveDensity > 0f) 0f else effectiveDensity
    val velocityMultiplier = 1.0f - mitigatedDensity

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "BASE ANOMALY DENSITY:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%+.1f%%", baseDensity * 100),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        PoxSlider(
            value = baseDensity,
            onValueChange = { baseDensity = it },
            valueRange = -0.33f..0.33f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "LUNAR INFLUENCE SHIFT:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%+.1f%%", lunarShift * 100),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        PoxSlider(
            value = lunarShift,
            onValueChange = { lunarShift = it },
            valueRange = -0.06f..0.06f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "COHERENCE SHIELD ACTIVE:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Checkbox(
                checked = shieldActive,
                onCheckedChange = { shieldActive = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberGreen,
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.Black
                ),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Multiplier display
        Text(
            text = String.format(Locale.US, "TRANSIT VELOCITY MULTIPLIER: %.3fx", velocityMultiplier),
            color = if (velocityMultiplier >= 1.0f) CyberGreen else Color(0xFFEF4444),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        // Visual bar chart comparisons
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            val w = size.width
            val h = size.height

            // Draw baseline indicator at 1.0x (which is width/2 or width * 0.7)
            val baselineX = w * 0.65f
            drawLine(Color.Gray, Offset(baselineX, 0f), Offset(baselineX, h), strokeWidth = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))

            // Draw current velocity multiplier bar
            val barWidth = baselineX * velocityMultiplier
            drawRect(
                color = if (velocityMultiplier >= 1.0f) CyberGreen.copy(alpha = 0.8f) else Color(0xFFEF4444).copy(alpha = 0.8f),
                topLeft = Offset(0f, h * 0.25f),
                size = Size(barWidth, h * 0.5f)
            )

            // Outline of maximum/minimum speeds
            drawRect(
                color = Color.DarkGray,
                topLeft = Offset(0f, h * 0.25f),
                size = Size(w, h * 0.5f),
                style = Stroke(width = 1f)
            )
        }
    }
}

@Composable
fun PhenotypeGeneDecoder() {
    var customSequence by remember { mutableStateOf("X?W!$@#&") } // Default anomalous mix

    // Parse sequence benefits
    val parsedBenefits = remember(customSequence) {
        val benefits = mutableListOf<String>()
        val characters = "XZYW?!$%&@#"
        val chunk = customSequence.padEnd(8, 'A').substring(0, 8)
        if (chunk.any { it in characters }) {
            val prefix = when (chunk[0]) {
                'X' -> "Vortex"
                'Z' -> "Zero-Point"
                'Y' -> "Quantum"
                'W' -> "Tachyon"
                '?' -> "Shrouded"
                '!' -> "Overdrive"
                '$' -> "Bio-Organic"
                '%' -> "Plasma"
                '&' -> "Eldritch"
                '@' -> "Temporal"
                '#' -> "Cosmic"
                else -> "Prime"
            }
            val suffix = when (chunk[1]) {
                'X' -> "Phase-Strike"
                'Z' -> "Mirror-Shield"
                'Y' -> "Reverb"
                'W' -> "Extraction-Unit"
                '?' -> "Siphon"
                '!' -> "Anomaly"
                '$' -> "Resonance"
                '%' -> "Helix"
                '&' -> "Well"
                '@' -> "Pulse"
                '#' -> "Matrix"
                else -> "Weld"
            }
            val charVal = { c: Char ->
                when (c) {
                    'X', 'Z', 'Y', 'W' -> 3
                    '?', '!' -> 4
                    '$', '%' -> 5
                    '&', '@', '#' -> 6
                    else -> 1
                }
            }
            val rawPower = charVal(chunk[2]) + charVal(chunk[3]) + charVal(chunk[4]) + charVal(chunk[5])
            val effectIndex = (chunk[0].code + chunk[1].code) % 6
            val baseDesc = when (effectIndex) {
                0 -> "Attacks deal ${(1.2 + 0.04 * rawPower)}x damage."
                1 -> "Upon fall, self-destructs and deals ${(40.0 + 8.0 * rawPower).toInt()} flat damage."
                2 -> "Boosts gene harvesting rates (${(30.0 + 3.0 * rawPower).coerceAtMost(100.0).toInt()}% chance for +1 extra gene on win)."
                3 -> "Heals ${(4.0 + rawPower).toInt()} HP on each attack."
                4 -> "Grants a ${(10.0 + 1.5 * rawPower).toInt()}% evasion chance."
                5 -> "Grants immunity to positive density drag during descent/ascent."
                else -> ""
            }
            val triggerIndex = (chunk[6].code + chunk[7].code) % 8
            val triggerDesc = when (triggerIndex) {
                0 -> "Always active in combat."
                1 -> "Only active during Dark moon phases."
                2 -> "Only active during Light moon phases."
                3 -> "Only active when under 40% Vitality."
                4 -> "Only active when above 70% Vitality."
                5 -> "Only active during the first 3 turns of combat."
                6 -> "Only active after turn 6 of combat."
                7 -> "Only active when local Spectrum Wave Coupling is above 82%."
                else -> "Always active."
            }
            benefits.add("GENE EXPRESSION: $prefix $suffix")
            benefits.add("PHENOTYPE EFFECT: $baseDesc")
            benefits.add("TRIGGER THRESHOLD: $triggerDesc")
        } else {
            benefits.add("SEQUENCE CONTAINS NO ANOMALOUS COMBAT BENIFIT GENES.")
        }
        benefits
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "INPUT 8-CHAR ANOMALOUS GENE:",
            color = Color.White,
            fontSize = 8.sp,
            fontFamily = FontFamily.Default
        )
        OutlinedTextField(
            value = customSequence,
            onValueChange = {
                if (it.length <= 8) {
                    customSequence = it.uppercase()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA855F7)
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA855F7),
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030303)),
            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                parsedBenefits.forEach { line ->
                    Text(
                        text = line,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

@Composable fun WireframeGlobe(
    modifier: Modifier = Modifier,
    isMini: Boolean = false,
    rotationSpeedMs: Int = 20000,
    activeCells: List<String> = emptyList(),
    viewModel: MainViewModel
) {
    var zoomLevel by remember { mutableStateOf(0) }
    
    // Reset zoom when mini or expanded
    LaunchedEffect(isMini) {
        if (isMini) zoomLevel = 0
    }

    val infiniteTransition = rememberInfiniteTransition(label = "globe_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(rotationSpeedMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val baseContinents = remember {
        getGlobePart1() + getGlobePart2() + getGlobePart3() + getGlobePart4()
    }

    val smoothedContinents = remember(baseContinents) {
        baseContinents.map { loop -> interpolateClosedLoop(loop, stepsPerSegment = 1) }
    }

    // Collect user coordinates for centering zoom
    val userLat by viewModel.latitude.collectAsState()
    val userLng by viewModel.longitude.collectAsState()

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
                } else Modifier
            )
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f * 0.9f

        val frontColor = Color(0xFF38BDF8)
        val strokeWidth = if (isMini) 1.dp.toPx() else 1.5.dp.toPx()

        // TILT and ROTATION configuration per zoom level
        val tiltAngle = if (zoomLevel == 1) {
            Math.toRadians(latVal)
        } else {
            Math.toRadians(23.5) // Standard Earth tilt
        }

        val currentRotation = if (zoomLevel == 1) {
            Math.PI / 2.0 - Math.toRadians(lngVal)
        } else {
            rotationAngle.toDouble()
        }

        // Draw outer boundary circle
        drawCircle(
            color = frontColor.copy(alpha = 0.15f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth)
        )

        if (zoomLevel == 0 || zoomLevel == 1) {
            // Run global / regional 3D projection
            val scaleFactor = if (zoomLevel == 1) 4.5f else 1.0f
            
            withTransform({
                if (zoomLevel == 1) {
                    this.scale(scaleFactor, scaleFactor, Offset(centerX, centerY))
                }
            }) {
                val effStroke = strokeWidth / scaleFactor

                // 1. Meridians
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
                                strokeWidth = effStroke
                            )
                        }
                        prevX = screenX
                        prevY = screenY
                        prevZ = zt
                        hasPrev = true
                    }
                }

                // 2. Parallels
                val numParallels = if (isMini) 5 else 9
                val pointsPerParallel = if (isMini) 20 else 40
                for (pa in 1 until numParallels) {
                    val lat = (pa.toFloat() / numParallels) * Math.PI - Math.PI / 2.0

                    var prevX = 0f
                    var prevY = 0f
                    var prevZ = 0.0
                    var hasPrev = false

                    for (p in 0..pointsPerParallel) {
                        val lon = (p.toFloat() / pointsPerParallel) * 2.0 * Math.PI - Math.PI
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
                                strokeWidth = effStroke
                            )
                        }
                        prevX = screenX
                        prevY = screenY
                        prevZ = zt
                        hasPrev = true
                    }
                }

                // 3. Continents
                for (continent in smoothedContinents) {
                    val cSize = continent.size
                    for (i in 0 until cSize) {
                        val pt1 = continent[i]
                        val pt2 = continent[(i + 1) % cSize]

                        val lat1 = pt1.first
                        val lon1 = pt1.second + currentRotation
                        val x1 = -radius.toDouble() * cos(lat1) * cos(lon1)
                        val y1 = radius.toDouble() * sin(lat1)
                        val z1 = radius.toDouble() * cos(lat1) * sin(lon1)
                        val yt1 = y1 * cos(tiltAngle) - z1 * sin(tiltAngle)
                        val zt1 = y1 * sin(tiltAngle) + z1 * cos(tiltAngle)
                        val xt1 = x1
                        val screenX1 = (centerX + xt1).toFloat()
                        val screenY1 = (centerY - yt1).toFloat()

                        val lat2 = pt2.first
                        val lon2 = pt2.second + currentRotation
                        val x2 = -radius.toDouble() * cos(lat2) * cos(lon2)
                        val y2 = radius.toDouble() * sin(lat2)
                        val z2 = radius.toDouble() * cos(lat2) * sin(lon2)
                        val yt2 = y2 * cos(tiltAngle) - z2 * sin(tiltAngle)
                        val zt2 = y2 * sin(tiltAngle) + z2 * cos(tiltAngle)
                        val xt2 = x2
                        val screenX2 = (centerX + xt2).toFloat()
                        val screenY2 = (centerY - yt2).toFloat()

                        val avgZ = (zt1 + zt2) / 2.0
                        val depthPct = ((avgZ / radius.toDouble()).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                        val alpha = (0.28f + 0.72f * depthPct).toFloat()

                        drawLine(
                            color = frontColor.copy(alpha = alpha),
                            start = Offset(screenX1, screenY1),
                            end = Offset(screenX2, screenY2),
                            strokeWidth = effStroke * 1.2f
                        )
                    }
                }

                // 4. Tactical Dots
                activeCells.forEach { cellKey ->
                    try {
                        val parts = cellKey.split(",")
                        val cellX = parts[0].toInt()
                        val cellY = parts[1].toInt()
                        val cellLatDeg = (cellX + 0.5) * 0.015
                        val cellLngDeg = (cellY + 0.5) * 0.015
                        val latRad = Math.toRadians(cellLatDeg)
                        val lonRad = Math.toRadians(cellLngDeg)
                        val rotLon = lonRad + currentRotation

                        val x = -radius.toDouble() * cos(latRad) * cos(rotLon)
                        val y = radius.toDouble() * sin(latRad)
                        val z = radius.toDouble() * cos(latRad) * sin(rotLon)

                        val yt = y * cos(tiltAngle) - z * sin(tiltAngle)
                        val zt = y * sin(tiltAngle) + z * cos(tiltAngle)
                        val xt = x

                        val screenX = (centerX + xt).toFloat()
                        val screenY = (centerY - yt).toFloat()

                        val isFront = zt >= 0
                        val depthPct = ((zt / radius.toDouble()).coerceIn(-1.0, 1.0) + 1.0) / 2.0
                        val baseColor = if (cellKey.contains("-")) Color(0xFFF43F5E) else Color(0xFFD946EF)
                        val alpha = if (isFront) (0.5f + 0.5f * depthPct.toFloat()) else (0.05f + 0.15f * depthPct.toFloat())
                        val sizeRadius = (if (isFront) 5.dp.toPx() else 3.dp.toPx()) / scaleFactor

                        drawCircle(
                            color = baseColor.copy(alpha = alpha),
                            radius = sizeRadius,
                            center = Offset(screenX, screenY)
                        )
                    } catch (e: Exception) {}
                }
            }

            // Draw center coordinate beacon when zoomed to Level 1
            if (zoomLevel == 1) {
                // Flash core player coordinates in center of screen
                val pulse = (System.currentTimeMillis() % 1500) / 1500f
                drawCircle(
                    color = CyberGreen.copy(alpha = 1f - pulse),
                    radius = 24.dp.toPx() * pulse,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = CyberGreen,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }
        } else if (zoomLevel == 2) {
            // Draw 2D Tactical mesh grid
            val gridSize = 10
            val cellSize = radius * 1.5f / gridSize
            val startX = centerX - (gridSize * cellSize) / 2f
            val startY = centerY - (gridSize * cellSize) / 2f

            for (i in 0..gridSize) {
                drawLine(
                    color = frontColor.copy(alpha = 0.25f),
                    start = Offset(startX + i * cellSize, startY),
                    end = Offset(startX + i * cellSize, startY + gridSize * cellSize),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = frontColor.copy(alpha = 0.25f),
                    start = Offset(startX, startY + i * cellSize),
                    end = Offset(startX + gridSize * cellSize, startY + i * cellSize),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val playerX = startX + 5.5f * cellSize
            val playerY = startY + 5.5f * cellSize
            val pulse = (System.currentTimeMillis() % 2000) / 2000f

            drawCircle(
                color = CyberGreen.copy(alpha = 1f - pulse),
                radius = cellSize * 2.5f * pulse,
                center = Offset(playerX, playerY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Draw Player Diamond
            val pPath = Path().apply {
                moveTo(playerX, playerY - 8.dp.toPx())
                lineTo(playerX + 8.dp.toPx(), playerY)
                lineTo(playerX, playerY + 8.dp.toPx())
                lineTo(playerX - 8.dp.toPx(), playerY)
                close()
            }
            drawPath(pPath, color = CyberGreen)

            // Draw mock nodes & anomalies in nearby sectors
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
            // Draw 3D Crystal Core Blueprint
            val crystalRadius = radius * 0.45f
            val rotationVal = rotationAngle.toDouble() * 1.5
            val tiltVal = Math.toRadians(35.0)

            val vertices = listOf(
                doubleArrayOf(0.0, crystalRadius.toDouble(), 0.0), // 0: Top
                doubleArrayOf(0.0, -crystalRadius.toDouble(), 0.0), // 1: Bottom
                doubleArrayOf(crystalRadius.toDouble() * cos(rotationVal), 0.0, crystalRadius.toDouble() * sin(rotationVal)), // 2
                doubleArrayOf(crystalRadius.toDouble() * cos(rotationVal + Math.PI/2), 0.0, crystalRadius.toDouble() * sin(rotationVal + Math.PI/2)), // 3
                doubleArrayOf(crystalRadius.toDouble() * cos(rotationVal + Math.PI), 0.0, crystalRadius.toDouble() * sin(rotationVal + Math.PI)), // 4
                doubleArrayOf(crystalRadius.toDouble() * cos(rotationVal + 3*Math.PI/2), 0.0, crystalRadius.toDouble() * sin(rotationVal + 3*Math.PI/2)) // 5
            )

            val projected = vertices.map { pt ->
                val x = pt[0]
                val y = pt[1]
                val z = pt[2]
                val yt = y * cos(tiltVal) - z * sin(tiltVal)
                Offset((centerX + x).toFloat(), (centerY - yt).toFloat())
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
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = CyberGreen,
                radius = crystalRadius * 0.08f,
                center = Offset(centerX, centerY)
            )

            // Radiating signal rings
            val wavePulse = (System.currentTimeMillis() % 1600) / 1600f
            drawCircle(
                color = CyberCyan.copy(alpha = 0.8f * (1f - wavePulse)),
                radius = crystalRadius * 1.5f * wavePulse,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Draw Telemetry overlays using Native Canvas
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

                // Render Top Title based on zoom level
                val titleText = when (zoomLevel) {
                    0 -> "HOLOGRAPHIC SATELLITE GLOBE (GLOBAL)"
                    1 -> "METROPOLITAN REGION UPLINK (ZOOM +1)"
                    2 -> "LOCAL MESH SECTOR GRID (ZOOM +2)"
                    else -> "PINPOINT NODE TRANSCEIVER CORE (ZOOM +3)"
                }
                canvas.nativeCanvas.drawText(titleText, 16.dp.toPx(), 24.dp.toPx(), titlePaint)

                // Render Bottom info details
                val infoLines = when (zoomLevel) {
                    0 -> listOf(
                        "SCALE: 1:40,000,000",
                        "GRID RESOLUTION: 1000 KM (GLOBAL)",
                        "CORE LATITUDE: ${"%.4f".format(latVal)}",
                        "CORE LONGITUDE: ${"%.4f".format(lngVal)}"
                    )
                    1 -> listOf(
                        "CURRENT REGION ID: 10.$latIndex.$lngIndex.x",
                        "SCALE: 1:1,500,000",
                        "GRID RESOLUTION: 80 KM",
                        "SURFACE POSITION: CENTERED"
                    )
                    2 -> listOf(
                        "CURRENT SECTOR ID: 10.$latIndex.$lngIndex.$octet4",
                        "SCALE: 1:50,000",
                        "GRID RANGE: 5 KM",
                        "ACTIVE MESH: 3 NODES DETECTED"
                    )
                    else -> listOf(
                        "NODE IDENTIFIER: ${viewModel.localNodeName}",
                        "IP SUITE: 10.$latIndex.$lngIndex.$octet4",
                        "UPLINK RATING: SECURE MESH",
                        "SYSTEM HARMONICS: 924.8 MHZ"
                    )
                }

                var yPos = height - 16.dp.toPx() - (infoLines.size * 14.dp.toPx())
                infoLines.forEach { line ->
                    canvas.nativeCanvas.drawText(line, 16.dp.toPx(), yPos, paint)
                    yPos += 12.dp.toPx()
                }

                // Draw Zoom Instructions Hint at top-right
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

// Spline & Meridian Wrap Math Helpers
private fun interpolateClosedLoop(loop: List<Pair<Double, Double>>, stepsPerSegment: Int = 5): List<Pair<Double, Double>> {
    if (loop.size < 3) return loop
    val result = mutableListOf<Pair<Double, Double>>()
    val n = loop.size
    for (i in 0 until n) {
        val p0 = loop[(i - 1 + n) % n]
        val p1 = loop[i]
        val p2 = loop[(i + 1) % n]
        val p3 = loop[(i + 2) % n]

        // Handle longitude wrap-around for circular coordinate interpolation
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


// Top-Level Helper for high-fidelity coordinate vectors
private fun deg(lat: Double, lon: Double): Pair<Double, Double> {
    return Pair(Math.toRadians(lat), Math.toRadians(lon))
}

private fun getGlobePart1(): List<List<Pair<Double, Double>>> {
    return listOf(
        // Loop 0 (7 points)
        listOf(
            deg(-80.04, -59.57), deg(-81.00, -60.16), deg(-80.92, -64.49), deg(-80.26, -66.29),
            deg(-80.39, -61.88), deg(-79.63, -60.61), deg(-80.04, -59.57)
        ),
        // Loop 1 (8 points)
        listOf(
            deg(-79.50, -159.21), deg(-79.63, -161.13), deg(-79.28, -162.44), deg(-78.60, -163.71),
            deg(-78.22, -163.11), deg(-78.38, -161.25), deg(-79.05, -159.48), deg(-79.50, -159.21)
        ),
        // Loop 2 (13 points)
        listOf(
            deg(-78.05, -45.15), deg(-78.48, -43.92), deg(-79.09, -43.49), deg(-80.03, -43.33),
            deg(-81.03, -50.48), deg(-80.97, -52.85), deg(-80.63, -54.16), deg(-80.22, -53.99),
            deg(-79.95, -51.85), deg(-79.61, -50.99), deg(-78.05, -48.66), deg(-77.83, -46.66),
            deg(-78.05, -45.15)
        ),
        // Loop 3 (8 points)
        listOf(
            deg(-73.50, -121.21), deg(-73.66, -119.92), deg(-73.48, -118.72), deg(-74.09, -120.23),
            deg(-74.01, -121.62), deg(-73.66, -122.62), deg(-73.32, -122.41), deg(-73.50, -121.21)
        ),
        // Loop 4 (5 points)
        listOf(
            deg(-73.48, -125.56), deg(-73.87, -124.03), deg(-73.46, -127.28), deg(-73.25, -126.56),
            deg(-73.48, -125.56)
        ),
        // Loop 5 (8 points)
        listOf(
            deg(-71.93, -98.98), deg(-71.95, -96.79), deg(-72.52, -96.20), deg(-72.50, -100.78),
            deg(-72.31, -101.80), deg(-71.89, -102.33), deg(-71.72, -101.70), deg(-71.93, -98.98)
        ),
        // Loop 6 (17 points)
        listOf(
            deg(-70.96, -68.45), deg(-71.41, -68.33), deg(-72.17, -68.78), deg(-72.50, -71.08),
            deg(-72.48, -72.39), deg(-72.09, -71.90), deg(-72.37, -74.19), deg(-72.07, -74.95),
            deg(-71.66, -75.01), deg(-71.15, -73.23), deg(-71.19, -72.07), deg(-70.68, -71.78),
            deg(-69.51, -71.74), deg(-69.04, -71.17), deg(-68.88, -70.25), deg(-69.25, -69.72),
            deg(-70.96, -68.45)
        ),
        // Loop 7 (267 points)
        listOf(
            deg(-64.15, -58.61), deg(-64.37, -59.05), deg(-64.31, -60.61), deg(-64.80, -62.02),
            deg(-65.09, -62.51), deg(-65.48, -62.65), deg(-65.86, -62.59), deg(-66.19, -62.12),
            deg(-66.50, -63.75), deg(-67.58, -65.51), deg(-67.95, -65.67), deg(-68.68, -64.78),
            deg(-69.23, -63.20), deg(-70.72, -61.81), deg(-73.17, -60.69), deg(-73.70, -60.83),
            deg(-74.44, -61.96), deg(-74.58, -63.30), deg(-75.26, -64.35), deg(-76.22, -69.80),
            deg(-76.63, -70.60), deg(-76.71, -77.24), deg(-77.10, -76.93), deg(-77.28, -75.40),
            deg(-77.91, -73.66), deg(-78.22, -74.77), deg(-78.12, -76.50), deg(-78.38, -77.93),
            deg(-79.18, -78.02), deg(-80.26, -75.36), deg(-82.38, -59.69), deg(-83.22, -58.22),
            deg(-81.73, -49.76), deg(-82.08, -42.81), deg(-81.65, -42.16), deg(-81.36, -40.77),
            deg(-81.34, -38.24), deg(-80.34, -28.55), deg(-79.63, -29.69), deg(-79.26, -29.69),
            deg(-79.46, -35.64), deg(-79.08, -35.91), deg(-78.34, -35.78), deg(-77.65, -32.21),
            deg(-76.67, -28.88), deg(-76.11, -22.46), deg(-75.13, -17.52), deg(-74.50, -15.70),
            deg(-74.11, -15.41), deg(-73.87, -16.47), deg(-73.15, -15.45), deg(-72.40, -12.29),
            deg(-71.27, -10.30), deg(-71.70, -7.42), deg(-71.32, -7.38), deg(-70.93, -6.87),
            deg(-71.03, -5.79), deg(-71.40, -5.54), deg(-71.46, -4.34), deg(-71.23, -0.66),
            deg(-71.64, -0.23), deg(-70.46, 6.27), deg(-69.89, 7.74), deg(-70.15, 8.49),
            deg(-70.01, 9.53), deg(-70.83, 10.82), deg(-70.64, 11.95), deg(-70.25, 12.40),
            deg(-69.97, 13.42), deg(-70.03, 14.73), deg(-70.40, 15.13), deg(-70.03, 15.95),
            deg(-69.89, 19.26), deg(-70.07, 21.45), deg(-70.70, 22.57), deg(-70.46, 27.09),
            deg(-69.66, 31.99), deg(-69.38, 32.75), deg(-68.50, 33.87), deg(-68.66, 34.91),
            deg(-69.01, 35.30), deg(-69.25, 36.16), deg(-69.17, 37.20), deg(-69.78, 38.65),
            deg(-69.54, 39.67), deg(-69.11, 40.02), deg(-68.60, 41.96), deg(-67.60, 46.50),
            deg(-67.72, 47.44), deg(-67.09, 48.99), deg(-66.88, 50.75), deg(-66.52, 50.95),
            deg(-66.25, 51.79), deg(-65.82, 54.53), deg(-65.97, 56.36), deg(-66.25, 57.16),
            deg(-66.68, 57.26), deg(-67.29, 58.74), deg(-67.41, 59.94), deg(-67.95, 61.43),
            deg(-68.01, 62.39), deg(-67.41, 64.05), deg(-67.93, 68.89), deg(-68.97, 69.71),
            deg(-69.23, 69.67), deg(-69.68, 69.56), deg(-70.31, 67.81), deg(-70.70, 67.95),
            deg(-70.68, 69.07), deg(-71.07, 68.93), deg(-71.85, 67.95), deg(-72.26, 69.87),
            deg(-72.09, 71.02), deg(-70.72, 73.08), deg(-69.87, 73.86), deg(-69.46, 77.64),
            deg(-68.33, 79.11), deg(-67.21, 82.78), deg(-67.15, 86.75), deg(-66.88, 87.48),
            deg(-66.21, 87.99), deg(-66.95, 88.83), deg(-67.15, 89.67), deg(-67.11, 94.18),
            deg(-67.39, 95.78), deg(-67.11, 98.68), deg(-67.25, 99.72), deg(-65.56, 102.83),
            deg(-65.97, 104.24), deg(-66.93, 106.18), deg(-66.70, 110.24), deg(-66.13, 111.74),
            deg(-65.88, 113.60), deg(-66.70, 115.60), deg(-66.66, 116.70), deg(-67.27, 119.83),
            deg(-67.19, 120.87), deg(-66.56, 122.32), deg(-66.48, 123.22), deg(-66.76, 128.80),
            deg(-66.43, 130.78), deg(-66.21, 134.76), deg(-65.31, 135.07), deg(-65.58, 135.70),
            deg(-66.78, 136.62), deg(-66.95, 137.46), deg(-66.92, 145.49), deg(-67.23, 146.20),
            deg(-67.60, 146.00), deg(-67.90, 146.65), deg(-68.39, 148.84), deg(-68.87, 152.50),
            deg(-68.89, 153.64), deg(-68.56, 154.28), deg(-69.38, 156.81), deg(-69.60, 159.18),
            deg(-69.99, 159.67), deg(-70.58, 161.57), deg(-70.83, 167.31), deg(-71.70, 171.21),
            deg(-72.09, 171.09), deg(-73.66, 169.29), deg(-74.38, 166.09), deg(-75.46, 164.23),
            deg(-76.24, 163.57), deg(-77.07, 163.49), deg(-78.18, 164.74), deg(-78.32, 166.60),
            deg(-78.75, 167.00), deg(-79.16, 161.77), deg(-80.95, 159.79), deg(-82.40, 163.71),
            deg(-83.34, 168.90), deg(-83.83, 169.40), deg(-84.04, 172.28), deg(-84.41, 173.22),
            deg(-84.16, 175.99), deg(-84.71, 180.00), deg(-90.00, 180.00), deg(-90.00, -180.00),
            deg(-84.71, -180.00), deg(-84.14, -179.06), deg(-84.45, -177.26), deg(-84.10, -175.93),
            deg(-84.53, -174.38), deg(-84.06, -172.89), deg(-83.88, -169.95), deg(-84.57, -167.02),
            deg(-85.37, -158.07), deg(-85.10, -155.19), deg(-85.61, -148.53), deg(-85.04, -143.11),
            deg(-84.57, -142.89), deg(-84.30, -150.06), deg(-83.90, -150.90), deg(-83.69, -153.59),
            deg(-82.45, -152.67), deg(-82.04, -152.86), deg(-81.10, -156.84), deg(-81.00, -152.10),
            deg(-81.34, -150.65), deg(-80.34, -146.42), deg(-79.93, -146.77), deg(-79.36, -149.53),
            deg(-79.06, -155.33), deg(-78.03, -158.05), deg(-76.89, -158.37), deg(-77.30, -156.97),
            deg(-77.07, -153.74), deg(-77.50, -152.92), deg(-77.40, -151.33), deg(-76.58, -147.61),
            deg(-76.48, -146.10), deg(-75.73, -146.50), deg(-75.38, -146.20), deg(-75.20, -144.91),
            deg(-75.54, -144.32), deg(-75.09, -141.64), deg(-74.97, -138.86), deg(-74.30, -135.21),
            deg(-74.52, -121.07), deg(-74.03, -117.47), deg(-74.24, -116.22), deg(-73.71, -113.94),
            deg(-74.71, -112.30), deg(-74.42, -111.26), deg(-75.18, -107.56), deg(-74.95, -104.88),
            deg(-75.30, -100.65), deg(-74.87, -100.12), deg(-74.19, -101.25), deg(-74.11, -102.55),
            deg(-73.73, -103.11), deg(-72.62, -103.68), deg(-72.91, -99.14), deg(-73.56, -97.69),
            deg(-73.62, -96.34), deg(-73.17, -92.44), deg(-73.40, -91.42), deg(-73.32, -90.09),
            deg(-72.56, -89.23), deg(-73.01, -88.42), deg(-73.09, -86.01), deg(-73.48, -85.19),
            deg(-73.85, -81.47), deg(-73.13, -80.30), deg(-73.52, -79.30), deg(-73.42, -77.93),
            deg(-73.97, -76.22), deg(-73.87, -74.89), deg(-73.40, -72.83), deg(-73.01, -68.94),
            deg(-72.48, -67.37), deg(-72.05, -67.13), deg(-71.64, -67.25), deg(-70.11, -68.49),
            deg(-69.72, -68.54), deg(-68.15, -67.43), deg(-67.33, -67.74), deg(-66.88, -67.25),
            deg(-64.64, -63.00), deg(-64.58, -62.04), deg(-63.39, -58.59), deg(-63.27, -57.81),
            deg(-63.53, -57.22), deg(-63.86, -57.60), deg(-64.15, -58.61)
        ),
        // Loop 8 (16 points)
        listOf(
            deg(-53.85, -67.75), deg(-54.45, -66.45), deg(-54.70, -65.05), deg(-55.20, -65.50),
            deg(-55.25, -66.45), deg(-54.90, -66.96), deg(-55.30, -67.29), deg(-55.61, -68.15),
            deg(-55.50, -69.23), deg(-54.50, -72.26), deg(-52.84, -74.66), deg(-54.07, -71.11),
            deg(-52.93, -70.27), deg(-52.52, -69.35), deg(-52.64, -68.63), deg(-53.85, -67.75)
        ),
        // Loop 9 (10 points)
        listOf(
            deg(-51.10, -58.55), deg(-51.55, -57.75), deg(-51.90, -58.05), deg(-52.20, -59.40),
            deg(-51.85, -59.85), deg(-52.30, -60.70), deg(-51.85, -61.20), deg(-51.25, -60.00),
            deg(-51.50, -59.15), deg(-51.10, -58.55)
        ),
        // Loop 10 (5 points)
        listOf(
            deg(-49.71, 70.28), deg(-49.77, 68.75), deg(-48.63, 68.94), deg(-49.06, 70.53),
            deg(-49.71, 70.28)
        ),
        // Loop 11 (12 points)
        listOf(
            deg(-40.79, 145.40), deg(-41.14, 146.36), deg(-40.88, 148.29), deg(-42.06, 148.36),
            deg(-42.41, 148.02), deg(-43.21, 147.91), deg(-42.94, 147.56), deg(-43.63, 146.87),
            deg(-43.55, 146.05), deg(-41.16, 144.72), deg(-40.70, 144.74), deg(-40.79, 145.40)
        ),
        // Loop 12 (20 points)
        listOf(
            deg(-40.92, 173.02), deg(-41.33, 173.25), deg(-40.93, 173.96), deg(-41.35, 174.25),
            deg(-41.77, 174.25), deg(-43.37, 172.71), deg(-43.85, 173.08), deg(-43.87, 172.31),
            deg(-44.24, 171.45), deg(-45.91, 170.62), deg(-46.64, 169.33), deg(-46.22, 166.68),
            deg(-45.85, 166.51), deg(-45.11, 167.05), deg(-44.12, 168.30), deg(-43.03, 170.52),
            deg(-41.51, 171.95), deg(-40.96, 172.10), deg(-40.49, 172.80), deg(-40.92, 173.02)
        ),
        // Loop 13 (28 points)
        listOf(
            deg(-36.16, 174.61), deg(-37.21, 175.34), deg(-36.53, 175.36), deg(-36.80, 175.81),
            deg(-37.56, 175.96), deg(-37.88, 176.76), deg(-37.96, 177.44), deg(-37.58, 178.01),
            deg(-37.70, 178.52), deg(-39.17, 177.97), deg(-39.15, 177.21), deg(-39.45, 176.94),
            deg(-39.88, 177.03), deg(-41.29, 176.01), deg(-41.69, 175.24), deg(-41.28, 174.65),
            deg(-40.46, 175.23), deg(-39.91, 174.90), deg(-39.51, 173.82), deg(-39.15, 173.85),
            deg(-38.80, 174.57), deg(-37.38, 174.70), deg(-36.53, 174.32), deg(-34.53, 172.64),
            deg(-34.45, 173.01), deg(-35.01, 173.55), deg(-35.27, 174.33), deg(-36.16, 174.61)
        ),
        // Loop 14 (6 points)
        listOf(
            deg(-22.16, 167.12), deg(-22.40, 166.74), deg(-21.68, 165.47), deg(-20.11, 164.03),
            deg(-20.46, 165.02), deg(-22.16, 167.12)
        ),
        // Loop 15 (8 points)
        listOf(
            deg(-17.34, 178.37), deg(-17.63, 178.72), deg(-18.15, 178.55), deg(-18.16, 177.38),
            deg(-17.72, 177.29), deg(-17.38, 177.67), deg(-17.50, 178.13), deg(-17.34, 178.37)
        ),
        // Loop 16 (6 points)
        listOf(
            deg(-16.80, 179.36), deg(-17.01, 178.73), deg(-16.64, 178.60), deg(-16.07, 180.00),
            deg(-16.56, 180.00), deg(-16.80, 179.36)
        ),
        // Loop 17 (5 points)
        listOf(
            deg(-14.93, 167.11), deg(-15.74, 167.27), deg(-15.67, 166.79), deg(-14.63, 166.63),
            deg(-14.93, 167.11)
        ),
        // Loop 18 (28 points)
        listOf(
            deg(-13.56, 50.06), deg(-15.23, 50.48), deg(-15.71, 50.38), deg(-16.00, 50.20),
            deg(-15.41, 49.86), deg(-15.71, 49.67), deg(-16.88, 49.77), deg(-17.11, 49.50),
            deg(-17.95, 49.44), deg(-24.94, 47.10), deg(-25.60, 45.41), deg(-24.99, 44.04),
            deg(-22.06, 43.25), deg(-21.34, 43.43), deg(-21.16, 43.89), deg(-20.07, 44.37),
            deg(-19.44, 44.46), deg(-17.41, 43.96), deg(-16.22, 44.45), deg(-15.78, 46.31),
            deg(-14.59, 47.71), deg(-14.09, 48.01), deg(-13.66, 47.87), deg(-13.78, 48.29),
            deg(-13.09, 48.85), deg(-12.49, 48.86), deg(-12.04, 49.19), deg(-13.56, 50.06)
        ),
        // Loop 19 (135 points)
        listOf(
            deg(-13.76, 143.56), deg(-14.55, 143.92), deg(-14.17, 144.56), deg(-14.98, 145.37),
            deg(-16.29, 145.49), deg(-16.91, 145.89), deg(-17.76, 146.16), deg(-18.28, 146.06),
            deg(-18.96, 146.39), deg(-20.39, 148.85), deg(-20.63, 148.72), deg(-21.26, 149.29),
            deg(-22.34, 149.68), deg(-22.12, 150.08), deg(-22.56, 150.48), deg(-22.40, 150.73),
            deg(-23.46, 150.90), deg(-25.27, 152.86), deg(-26.07, 153.14), deg(-27.26, 153.09),
            deg(-28.11, 153.57), deg(-31.64, 152.89), deg(-32.55, 152.45), deg(-33.04, 151.71),
            deg(-35.67, 150.33), deg(-37.43, 150.00), deg(-37.77, 149.42), deg(-37.81, 148.30),
            deg(-39.04, 146.32), deg(-38.42, 144.88), deg(-37.90, 145.03), deg(-38.81, 143.61),
            deg(-38.02, 140.64), deg(-37.40, 139.99), deg(-36.14, 139.57), deg(-35.73, 139.08),
            deg(-35.61, 138.12), deg(-35.13, 138.45), deg(-34.38, 138.21), deg(-35.08, 137.72),
            deg(-35.26, 136.83), deg(-33.64, 137.89), deg(-32.90, 137.81), deg(-34.09, 136.37),
            deg(-34.89, 135.99), deg(-34.48, 135.21), deg(-33.95, 135.24), deg(-32.85, 134.09),
            deg(-32.62, 134.27), deg(-31.50, 131.33), deg(-31.59, 129.54), deg(-32.28, 127.10),
            deg(-32.22, 126.15), deg(-32.96, 124.22), deg(-33.89, 123.66), deg(-33.98, 119.89),
            deg(-34.51, 119.30), deg(-34.46, 119.01), deg(-35.06, 118.02), deg(-35.03, 116.63),
            deg(-34.20, 115.03), deg(-33.62, 115.05), deg(-33.26, 115.71), deg(-31.61, 115.69),
            deg(-30.60, 115.16), deg(-29.46, 115.04), deg(-28.52, 114.62), deg(-28.12, 114.17),
            deg(-27.33, 114.05), deg(-26.12, 113.34), deg(-26.55, 113.78), deg(-25.62, 113.44),
            deg(-26.30, 114.23), deg(-25.79, 114.22), deg(-24.38, 113.39), deg(-23.06, 113.84),
            deg(-22.48, 113.74), deg(-21.76, 114.15), deg(-22.52, 114.23), deg(-21.83, 114.65),
            deg(-20.70, 116.71), deg(-20.75, 117.44), deg(-19.95, 119.25), deg(-19.68, 120.86),
            deg(-18.20, 122.24), deg(-17.25, 122.31), deg(-16.41, 123.01), deg(-17.27, 123.43),
            deg(-17.07, 123.86), deg(-16.60, 123.50), deg(-16.11, 123.82), deg(-16.33, 124.26),
            deg(-15.57, 124.38), deg(-14.68, 125.17), deg(-14.51, 125.67), deg(-14.23, 125.69),
            deg(-14.35, 126.13), deg(-14.10, 126.14), deg(-13.82, 127.07), deg(-14.87, 128.36),
            deg(-14.97, 129.62), deg(-14.42, 129.41), deg(-13.62, 129.89), deg(-13.36, 130.34),
            deg(-13.11, 130.18), deg(-12.54, 130.62), deg(-12.18, 131.22), deg(-12.11, 132.58),
            deg(-11.60, 132.56), deg(-11.27, 131.82), deg(-11.13, 132.36), deg(-11.79, 133.55),
            deg(-12.25, 135.30), deg(-11.86, 136.49), deg(-12.35, 136.95), deg(-13.29, 136.31),
            deg(-13.32, 135.96), deg(-13.72, 136.08), deg(-14.72, 135.43), deg(-15.00, 135.50),
            deg(-16.81, 138.30), deg(-17.06, 139.11), deg(-17.37, 139.26), deg(-17.71, 140.22),
            deg(-17.37, 140.88), deg(-16.39, 141.27), deg(-15.04, 141.70), deg(-13.70, 141.52),
            deg(-12.74, 141.84), deg(-12.41, 141.69), deg(-11.04, 142.14), deg(-10.67, 142.52),
            deg(-11.78, 142.87), deg(-12.83, 143.52), deg(-13.76, 143.56)
        ),
        // Loop 20 (5 points)
        listOf(
            deg(-10.48, 162.12), deg(-10.83, 162.40), deg(-10.82, 161.70), deg(-10.20, 161.32),
            deg(-10.48, 162.12)
        ),
        // Loop 21 (6 points)
        listOf(
            deg(-10.24, 120.72), deg(-10.26, 120.30), deg(-9.56, 118.97), deg(-9.36, 119.90),
            deg(-9.97, 120.78), deg(-10.24, 120.72)
        ),
        // Loop 22 (6 points)
        listOf(
            deg(-9.87, 160.85), deg(-9.79, 159.85), deg(-9.64, 159.64), deg(-9.24, 159.70),
            deg(-9.40, 160.36), deg(-9.87, 160.85)
        ),
        // Loop 23 (6 points)
        listOf(
            deg(-9.60, 161.68), deg(-9.78, 161.53), deg(-8.92, 160.79), deg(-8.32, 160.58),
            deg(-8.32, 160.92), deg(-9.60, 161.68)
        ),
        // Loop 24 (8 points)
        listOf(
            deg(-10.14, 124.44), deg(-10.24, 123.46), deg(-9.29, 123.98), deg(-8.66, 125.09),
            deg(-8.27, 126.96), deg(-8.40, 127.34), deg(-9.39, 125.09), deg(-10.14, 124.44)
        ),
        // Loop 25 (8 points)
        listOf(
            deg(-8.10, 117.90), deg(-8.36, 118.26), deg(-8.28, 118.88), deg(-8.71, 119.13),
            deg(-9.03, 116.74), deg(-8.46, 117.08), deg(-8.45, 117.63), deg(-8.10, 117.90)
        ),
        // Loop 26 (8 points)
        listOf(
            deg(-8.09, 122.90), deg(-8.65, 122.76), deg(-8.93, 121.25), deg(-8.81, 119.92),
            deg(-8.44, 119.92), deg(-8.24, 120.72), deg(-8.54, 121.34), deg(-8.09, 122.90)
        ),
        // Loop 27 (5 points)
        listOf(
            deg(-8.34, 159.88), deg(-8.54, 159.92), deg(-7.42, 158.21), deg(-7.56, 158.82),
            deg(-8.34, 159.88)
        ),
        // Loop 28 (19 points)
        listOf(
            deg(-6.78, 108.62), deg(-6.88, 110.54), deg(-6.47, 110.76), deg(-6.95, 112.61),
            deg(-7.59, 112.98), deg(-7.78, 114.48), deg(-8.37, 115.71), deg(-8.75, 114.56),
            deg(-8.35, 113.46), deg(-8.30, 111.52), deg(-7.64, 108.69), deg(-7.77, 108.28),
            deg(-7.35, 106.45), deg(-6.92, 106.28), deg(-6.85, 105.37), deg(-5.90, 106.05),
            deg(-5.95, 107.27), deg(-6.42, 108.49), deg(-6.78, 108.62)
        ),
        // Loop 29 (5 points)
        listOf(
            deg(-6.21, 134.72), deg(-6.90, 134.21), deg(-6.14, 134.11), deg(-5.45, 134.50),
            deg(-6.21, 134.72)
        ),
        // Loop 30 (7 points)
        listOf(
            deg(-6.82, 155.88), deg(-6.92, 155.60), deg(-6.54, 155.17), deg(-5.14, 154.51),
            deg(-5.04, 154.65), deg(-6.54, 156.02), deg(-6.82, 155.88)
        )
    )
}

private fun getGlobePart2(): List<List<Pair<Double, Double>>> {
    return listOf(
        // Loop 31 (14 points)
        listOf(
            deg(-5.48, 151.98), deg(-5.56, 151.46), deg(-5.84, 151.30), deg(-6.32, 150.24),
            deg(-5.75, 148.32), deg(-5.44, 148.40), deg(-5.51, 149.85), deg(-5.00, 150.14),
            deg(-5.53, 150.24), deg(-5.46, 150.81), deg(-4.76, 151.65), deg(-4.17, 151.54),
            deg(-4.31, 152.34), deg(-5.48, 151.98)
        ),
        // Loop 32 (6 points)
        listOf(
            deg(-3.46, 127.25), deg(-3.79, 126.87), deg(-3.61, 126.18), deg(-3.18, 125.99),
            deg(-3.13, 127.00), deg(-3.46, 127.25)
        ),
        // Loop 33 (7 points)
        listOf(
            deg(-3.09, 130.47), deg(-3.86, 130.83), deg(-3.45, 129.99), deg(-3.39, 127.90),
            deg(-2.84, 128.14), deg(-2.80, 129.37), deg(-3.09, 130.47)
        ),
        // Loop 34 (8 points)
        listOf(
            deg(-4.50, 153.14), deg(-4.77, 152.83), deg(-3.79, 152.41), deg(-2.74, 150.66),
            deg(-2.50, 150.94), deg(-3.24, 152.24), deg(-3.98, 153.02), deg(-4.50, 153.14)
        ),
        // Loop 35 (56 points)
        listOf(
            deg(-1.15, 134.14), deg(-2.77, 134.42), deg(-3.37, 135.46), deg(-2.31, 136.29),
            deg(-1.70, 137.44), deg(-1.70, 138.33), deg(-3.86, 144.58), deg(-4.88, 145.83),
            deg(-5.47, 145.98), deg(-6.08, 147.65), deg(-6.61, 147.89), deg(-6.72, 146.97),
            deg(-7.39, 147.19), deg(-8.04, 148.08), deg(-9.10, 148.73), deg(-9.07, 149.31),
            deg(-9.51, 149.27), deg(-9.68, 150.04), deg(-9.87, 149.74), deg(-10.29, 150.80),
            deg(-10.58, 150.69), deg(-10.65, 150.03), deg(-10.39, 149.78), deg(-10.13, 147.91),
            deg(-8.94, 146.57), deg(-8.07, 146.05), deg(-7.63, 144.74), deg(-8.25, 143.29),
            deg(-8.98, 143.41), deg(-9.33, 142.63), deg(-9.12, 141.03), deg(-8.30, 140.14),
            deg(-8.10, 139.13), deg(-8.38, 138.88), deg(-8.41, 137.61), deg(-7.60, 138.04),
            deg(-7.32, 138.67), deg(-5.39, 137.93), deg(-4.55, 135.99), deg(-4.46, 135.16),
            deg(-3.54, 133.66), deg(-4.02, 133.37), deg(-4.11, 132.98), deg(-3.31, 132.75),
            deg(-2.82, 131.99), deg(-2.46, 133.07), deg(-2.48, 133.78), deg(-2.21, 133.70),
            deg(-2.21, 132.23), deg(-1.62, 131.84), deg(-1.43, 130.94), deg(-0.94, 130.52),
            deg(-0.70, 131.87), deg(-0.37, 132.38), deg(-0.78, 133.99), deg(-1.15, 134.14)
        ),
        // Loop 36 (40 points)
        listOf(
            deg(1.42, 125.24), deg(0.43, 124.44), deg(0.24, 123.69), deg(0.43, 122.72),
            deg(0.24, 120.18), deg(-0.52, 120.04), deg(-1.41, 120.94), deg(-0.96, 121.48),
            deg(-0.62, 123.34), deg(-1.08, 123.26), deg(-0.93, 122.82), deg(-1.52, 122.39),
            deg(-1.90, 121.51), deg(-3.19, 122.45), deg(-3.53, 122.27), deg(-4.68, 123.17),
            deg(-5.34, 123.16), deg(-5.63, 122.63), deg(-5.28, 122.24), deg(-4.46, 122.72),
            deg(-4.85, 121.74), deg(-4.57, 121.49), deg(-4.19, 121.62), deg(-3.60, 120.90),
            deg(-2.63, 120.97), deg(-2.93, 120.31), deg(-5.53, 120.43), deg(-5.67, 119.80),
            deg(-5.38, 119.37), deg(-4.46, 119.65), deg(-3.49, 119.50), deg(-3.49, 119.08),
            deg(-2.80, 118.77), deg(-2.15, 119.18), deg(0.57, 120.04), deg(1.31, 120.89),
            deg(0.88, 122.93), deg(0.92, 124.08), deg(1.64, 125.07), deg(1.42, 125.24)
        ),
        // Loop 37 (13 points)
        listOf(
            deg(1.13, 128.69), deg(0.26, 128.64), deg(0.36, 128.12), deg(-0.25, 127.97),
            deg(-0.78, 128.38), deg(-0.90, 128.10), deg(-0.27, 127.70), deg(1.01, 127.40),
            deg(1.81, 127.60), deg(2.17, 127.93), deg(1.63, 128.00), deg(1.54, 128.59),
            deg(1.13, 128.69)
        ),
        // Loop 38 (22 points)
        listOf(
            deg(-5.85, 105.82), deg(-5.87, 104.71), deg(-4.22, 102.58), deg(-2.80, 101.40),
            deg(-0.65, 100.14), deg(0.18, 99.26), deg(1.82, 98.60), deg(2.45, 97.70),
            deg(3.31, 97.18), deg(4.97, 95.38), deg(5.48, 95.29), deg(5.25, 97.48),
            deg(2.10, 100.64), deg(2.08, 101.66), deg(0.56, 103.08), deg(0.10, 103.84),
            deg(-0.71, 103.44), deg(-1.08, 104.37), deg(-2.34, 104.89), deg(-2.43, 105.62),
            deg(-3.06, 106.11), deg(-5.85, 105.82)
        ),
        // Loop 39 (38 points)
        listOf(
            deg(1.83, 117.88), deg(0.90, 119.00), deg(0.78, 117.81), deg(0.10, 117.48),
            deg(-0.80, 117.52), deg(-1.49, 116.56), deg(-4.01, 116.15), deg(-3.66, 116.00),
            deg(-4.11, 114.86), deg(-3.50, 114.47), deg(-3.44, 113.76), deg(-3.12, 113.26),
            deg(-3.48, 112.07), deg(-2.99, 111.70), deg(-2.93, 110.22), deg(-1.59, 110.07),
            deg(-1.31, 109.57), deg(-0.46, 109.09), deg(1.34, 109.07), deg(2.01, 109.66),
            deg(1.66, 110.40), deg(1.85, 111.17), deg(2.70, 111.37), deg(3.10, 113.00),
            deg(4.90, 114.60), deg(6.14, 116.22), deg(6.92, 116.73), deg(6.93, 117.13),
            deg(6.42, 117.64), deg(5.99, 117.69), deg(5.41, 119.18), deg(5.02, 119.11),
            deg(4.97, 118.44), deg(4.48, 118.62), deg(4.14, 117.88), deg(3.23, 117.31),
            deg(2.29, 118.05), deg(1.83, 117.88)
        ),
        // Loop 40 (24 points)
        listOf(
            deg(8.41, 126.38), deg(7.19, 126.54), deg(6.27, 126.20), deg(7.29, 125.83),
            deg(6.79, 125.36), deg(6.05, 125.68), deg(5.58, 125.40), deg(6.16, 124.22),
            deg(6.89, 123.94), deg(7.36, 124.24), deg(7.83, 123.61), deg(7.42, 123.30),
            deg(7.46, 122.83), deg(6.90, 122.09), deg(7.19, 121.92), deg(8.03, 122.31),
            deg(8.69, 123.49), deg(8.24, 123.84), deg(8.51, 124.60), deg(8.96, 124.76),
            deg(8.99, 125.47), deg(9.76, 125.41), deg(9.29, 126.22), deg(8.41, 126.38)
        ),
        // Loop 41 (9 points)
        listOf(
            deg(6.20, 81.22), deg(5.97, 80.35), deg(6.76, 79.87), deg(8.20, 79.70),
            deg(9.82, 80.15), deg(9.27, 80.84), deg(7.52, 81.79), deg(6.48, 81.64),
            deg(6.20, 81.22)
        ),
        // Loop 42 (7 points)
        listOf(
            deg(10.11, -60.94), deg(10.09, -61.95), deg(10.37, -61.66), deg(10.76, -61.68),
            deg(10.89, -61.10), deg(10.86, -60.90), deg(10.11, -60.94)
        ),
        // Loop 43 (9 points)
        listOf(
            deg(10.28, 123.98), deg(9.02, 123.00), deg(9.71, 122.38), deg(10.26, 122.84),
            deg(10.88, 122.95), deg(10.94, 123.50), deg(10.27, 123.34), deg(11.23, 124.08),
            deg(10.28, 123.98)
        ),
        // Loop 44 (6 points)
        listOf(
            deg(9.32, 118.50), deg(8.37, 117.17), deg(10.38, 118.99), deg(11.37, 119.51),
            deg(10.55, 119.69), deg(9.32, 118.50)
        ),
        // Loop 45 (6 points)
        listOf(
            deg(11.89, 121.88), deg(11.58, 122.48), deg(11.58, 123.12), deg(11.17, 123.10),
            deg(10.44, 122.00), deg(11.89, 121.88)
        ),
        // Loop 46 (13 points)
        listOf(
            deg(12.16, 125.50), deg(11.05, 125.78), deg(11.31, 125.01), deg(10.36, 125.28),
            deg(10.13, 124.80), deg(10.84, 124.76), deg(10.89, 124.46), deg(11.50, 124.30),
            deg(11.42, 124.89), deg(11.79, 124.88), deg(12.56, 124.27), deg(12.54, 125.23),
            deg(12.16, 125.50)
        ),
        // Loop 47 (5 points)
        listOf(
            deg(13.07, 121.53), deg(12.21, 121.26), deg(13.47, 120.32), deg(13.43, 121.18),
            deg(13.07, 121.53)
        ),
        // Loop 48 (29 points)
        listOf(
            deg(18.50, 121.32), deg(18.22, 121.94), deg(18.48, 122.25), deg(17.81, 122.17),
            deg(17.09, 122.52), deg(16.26, 122.25), deg(15.93, 121.66), deg(15.12, 121.51),
            deg(14.33, 121.73), deg(14.34, 122.70), deg(13.78, 123.95), deg(13.24, 123.86),
            deg(13.00, 124.18), deg(12.54, 124.08), deg(13.55, 122.93), deg(13.19, 122.67),
            deg(13.78, 122.03), deg(13.64, 121.13), deg(13.86, 120.63), deg(14.27, 120.68),
            deg(14.53, 120.99), deg(14.76, 120.69), deg(14.40, 120.56), deg(14.97, 120.07),
            deg(16.36, 119.88), deg(16.03, 120.29), deg(17.60, 120.39), deg(18.51, 120.72),
            deg(18.50, 121.32)
        ),
        // Loop 49 (6 points)
        listOf(
            deg(18.23, -65.59), deg(17.98, -65.85), deg(17.95, -67.18), deg(18.37, -67.24),
            deg(18.51, -66.28), deg(18.23, -65.59)
        ),
        // Loop 50 (7 points)
        listOf(
            deg(17.87, -76.90), deg(17.70, -77.21), deg(18.23, -78.34), deg(18.52, -77.80),
            deg(18.40, -76.90), deg(17.89, -76.20), deg(17.87, -76.90)
        ),
        // Loop 51 (26 points)
        listOf(
            deg(19.87, -72.58), deg(19.71, -71.71), deg(19.88, -70.81), deg(19.65, -69.95),
            deg(19.29, -69.77), deg(19.31, -69.22), deg(19.02, -69.25), deg(18.61, -68.32),
            deg(18.21, -68.69), deg(18.43, -69.95), deg(18.18, -70.52), deg(18.43, -70.67),
            deg(18.28, -71.00), deg(17.60, -71.40), deg(18.04, -71.71), deg(18.21, -72.37),
            deg(18.03, -73.92), deg(18.34, -74.46), deg(18.66, -74.37), deg(18.45, -72.69),
            deg(18.67, -72.33), deg(19.10, -72.79), deg(19.48, -72.78), deg(19.64, -73.42),
            deg(19.92, -73.19), deg(19.87, -72.58)
        ),
        // Loop 52 (8 points)
        listOf(
            deg(18.68, 110.34), deg(18.20, 109.48), deg(18.51, 108.66), deg(19.37, 108.63),
            deg(19.82, 109.12), deg(20.08, 110.79), deg(19.70, 111.01), deg(18.68, 110.34)
        ),
        // Loop 53 (7 points)
        listOf(
            deg(19.08, -155.54), deg(18.92, -155.69), deg(19.06, -155.94), deg(19.70, -156.07),
            deg(20.27, -155.86), deg(19.51, -154.81), deg(19.08, -155.54)
        ),
        // Loop 54 (5 points)
        listOf(
            deg(20.64, -156.08), deg(20.57, -156.41), deg(20.93, -156.71), deg(20.92, -156.26),
            deg(20.64, -156.08)
        ),
        // Loop 55 (5 points)
        listOf(
            deg(21.32, -157.65), deg(21.31, -158.13), deg(21.58, -158.29), deg(21.72, -158.03),
            deg(21.32, -157.65)
        ),
        // Loop 56 (24 points)
        listOf(
            deg(22.77, -79.68), deg(22.40, -79.28), deg(22.51, -78.35), deg(21.21, -76.52),
            deg(21.02, -75.60), deg(20.74, -75.67), deg(20.69, -74.93), deg(20.28, -74.18),
            deg(20.05, -74.30), deg(19.92, -74.96), deg(19.86, -77.76), deg(20.41, -77.09),
            deg(20.74, -78.14), deg(21.60, -78.72), deg(21.56, -79.28), deg(22.39, -82.17),
            deg(22.64, -81.80), deg(22.69, -82.78), deg(21.91, -84.05), deg(21.90, -84.97),
            deg(22.79, -83.78), deg(23.19, -82.27), deg(23.11, -80.62), deg(22.77, -79.68)
        ),
        // Loop 57 (6 points)
        listOf(
            deg(23.76, -77.53), deg(23.71, -77.78), deg(24.58, -78.41), deg(25.21, -78.19),
            deg(25.17, -77.89), deg(23.76, -77.53)
        ),
        // Loop 58 (8 points)
        listOf(
            deg(22.79, 121.18), deg(21.97, 120.75), deg(22.81, 120.22), deg(23.56, 120.11),
            deg(24.54, 120.69), deg(25.30, 121.50), deg(25.00, 121.95), deg(22.79, 121.18)
        ),
        // Loop 59 (5 points)
        listOf(
            deg(26.58, -77.82), deg(26.42, -78.91), deg(26.79, -78.98), deg(26.84, -77.85),
            deg(26.58, -77.82)
        ),
        // Loop 60 (5 points)
        listOf(
            deg(26.59, -77.00), deg(25.88, -77.17), deg(26.53, -77.34), deg(27.04, -77.79),
            deg(26.59, -77.00)
        ),
        // Loop 61 (12 points)
        listOf(
            deg(34.15, 134.64), deg(33.81, 134.77), deg(33.20, 134.20), deg(33.52, 133.79),
            deg(33.29, 133.28), deg(32.70, 133.01), deg(32.99, 132.36), deg(33.46, 132.37),
            deg(34.06, 132.92), deg(33.94, 133.49), deg(34.36, 133.90), deg(34.15, 134.64)
        )
    )
}

private fun getGlobePart3(): List<List<Pair<Double, Double>>> {
    return listOf(
        // Loop 62 (7 points)
        listOf(
            deg(35.67, 34.58), deg(35.25, 33.90), deg(34.98, 34.00), deg(34.57, 32.98),
            deg(34.70, 32.49), deg(35.10, 32.26), deg(35.67, 34.58)
        ),
        // Loop 63 (8 points)
        listOf(
            deg(35.71, 23.70), deg(35.37, 24.25), deg(35.30, 26.29), deg(35.00, 26.16),
            deg(34.92, 24.72), deg(35.08, 24.74), deg(35.28, 23.51), deg(35.71, 23.70)
        ),
        // Loop 64 (5 points)
        listOf(
            deg(38.23, 15.52), deg(36.62, 15.10), deg(37.61, 12.43), deg(38.13, 12.57),
            deg(38.23, 15.52)
        ),
        // Loop 65 (9 points)
        listOf(
            deg(41.21, 9.21), deg(40.50, 9.81), deg(39.18, 9.67), deg(39.24, 9.21),
            deg(38.91, 8.81), deg(39.17, 8.43), deg(40.95, 8.16), deg(40.90, 8.71),
            deg(41.21, 9.21)
        ),
        // Loop 66 (35 points)
        listOf(
            deg(37.14, 140.98), deg(36.34, 140.60), deg(35.84, 140.77), deg(35.14, 140.25),
            deg(34.67, 138.98), deg(34.61, 137.22), deg(33.46, 135.79), deg(33.85, 135.12),
            deg(34.60, 135.08), deg(34.38, 133.34), deg(33.90, 132.16), deg(33.89, 130.99),
            deg(33.15, 132.00), deg(31.45, 131.33), deg(31.03, 130.69), deg(31.42, 130.20),
            deg(32.32, 130.45), deg(32.61, 129.81), deg(33.30, 129.41), deg(33.60, 130.35),
            deg(34.23, 130.88), deg(35.43, 132.62), deg(35.73, 134.61), deg(35.53, 135.68),
            deg(37.30, 136.72), deg(36.83, 137.39), deg(38.22, 139.43), deg(39.44, 140.05),
            deg(40.56, 139.88), deg(41.20, 140.31), deg(41.38, 141.37), deg(39.99, 141.91),
            deg(39.18, 141.88), deg(38.17, 140.96), deg(37.14, 140.98)
        ),
        // Loop 67 (6 points)
        listOf(
            deg(42.15, 9.56), deg(41.38, 9.23), deg(41.58, 8.78), deg(42.26, 8.54),
            deg(43.01, 9.39), deg(42.15, 9.56)
        ),
        // Loop 68 (15 points)
        listOf(
            deg(44.17, 143.91), deg(43.96, 144.61), deg(44.38, 145.32), deg(43.26, 145.54),
            deg(42.99, 144.06), deg(42.00, 143.18), deg(42.68, 141.61), deg(41.58, 141.07),
            deg(41.57, 139.96), deg(42.56, 139.82), deg(43.33, 140.31), deg(43.39, 141.38),
            deg(45.55, 141.97), deg(44.51, 143.14), deg(44.17, 143.91)
        ),
        // Loop 69 (7 points)
        listOf(
            deg(46.55, -63.66), deg(46.44, -62.01), deg(45.97, -62.87), deg(46.39, -64.14),
            deg(46.73, -64.39), deg(47.04, -64.01), deg(46.55, -63.66)
        ),
        // Loop 70 (5 points)
        listOf(
            deg(49.11, -61.81), deg(49.40, -63.59), deg(49.87, -64.52), deg(49.71, -62.86),
            deg(49.11, -61.81)
        ),
        // Loop 71 (11 points)
        listOf(
            deg(48.51, -123.51), deg(48.37, -124.01), deg(48.83, -125.66), deg(49.81, -127.03),
            deg(49.99, -128.06), deg(50.54, -128.44), deg(50.77, -128.36), deg(50.30, -125.76),
            deg(49.48, -124.92), deg(49.06, -123.92), deg(48.51, -123.51)
        ),
        // Loop 72 (28 points)
        listOf(
            deg(50.69, -56.13), deg(49.81, -56.80), deg(50.15, -56.14), deg(49.94, -55.47),
            deg(49.59, -55.82), deg(49.31, -54.94), deg(49.56, -54.47), deg(49.25, -53.48),
            deg(48.52, -53.79), deg(48.69, -53.09), deg(47.54, -52.65), deg(46.66, -53.07),
            deg(46.81, -54.18), deg(47.63, -53.96), deg(47.75, -54.24), deg(46.88, -55.40),
            deg(46.92, -56.00), deg(47.39, -55.29), deg(47.63, -56.25), deg(47.60, -59.27),
            deg(47.90, -59.42), deg(48.25, -58.80), deg(48.52, -59.23), deg(49.13, -58.39),
            deg(50.72, -57.36), deg(51.29, -56.74), deg(51.59, -55.41), deg(50.69, -56.13)
        ),
        // Loop 73 (8 points)
        listOf(
            deg(54.04, -132.71), deg(54.12, -131.75), deg(52.98, -132.05), deg(52.18, -131.18),
            deg(52.18, -131.58), deg(53.41, -133.05), deg(54.17, -133.18), deg(54.04, -132.71)
        ),
        // Loop 74 (18 points)
        listOf(
            deg(50.75, 143.65), deg(48.98, 144.65), deg(49.31, 143.17), deg(47.86, 142.56),
            deg(46.84, 143.53), deg(46.14, 143.51), deg(46.74, 142.75), deg(45.97, 142.09),
            deg(48.86, 141.90), deg(50.95, 142.18), deg(51.94, 141.59), deg(53.30, 141.68),
            deg(53.76, 142.61), deg(54.23, 142.21), deg(54.37, 142.65), deg(52.74, 143.26),
            deg(51.76, 143.24), deg(50.75, 143.65)
        ),
        // Loop 75 (11 points)
        listOf(
            deg(52.26, -6.79), deg(51.67, -8.56), deg(51.82, -9.98), deg(52.86, -9.17),
            deg(53.88, -9.69), deg(55.13, -7.57), deg(55.17, -6.73), deg(54.55, -5.66),
            deg(53.87, -6.20), deg(53.15, -6.03), deg(52.26, -6.79)
        ),
        // Loop 76 (6 points)
        listOf(
            deg(55.61, 12.69), deg(54.80, 12.09), deg(55.36, 11.04), deg(55.78, 10.90),
            deg(56.11, 12.37), deg(55.61, 12.69)
        ),
        // Loop 77 (8 points)
        listOf(
            deg(57.12, -153.01), deg(56.73, -154.01), deg(56.99, -154.52), deg(57.46, -154.67),
            deg(57.97, -153.23), deg(57.90, -152.56), deg(57.59, -152.14), deg(57.12, -153.01)
        ),
        // Loop 78 (40 points)
        listOf(
            deg(58.63, -3.01), deg(57.55, -4.07), deg(57.68, -1.96), deg(56.87, -2.22),
            deg(55.97, -3.12), deg(55.91, -2.09), deg(54.62, -1.11), deg(54.46, -0.43),
            deg(52.93, 0.47), deg(52.74, 1.68), deg(52.10, 1.56), deg(51.81, 1.05),
            deg(51.29, 1.45), deg(50.77, 0.55), deg(50.50, -2.49), deg(50.70, -2.96),
            deg(50.23, -3.62), deg(50.34, -4.54), deg(49.96, -5.25), deg(50.16, -5.78),
            deg(51.21, -4.31), deg(51.43, -3.41), deg(51.59, -4.98), deg(51.99, -5.27),
            deg(52.30, -4.22), deg(52.84, -4.77), deg(53.50, -4.58), deg(53.40, -3.09),
            deg(53.98, -2.95), deg(54.62, -3.63), deg(54.79, -4.84), deg(55.06, -5.08),
            deg(55.51, -4.72), deg(55.78, -5.05), deg(55.31, -5.59), deg(56.28, -5.64),
            deg(56.79, -6.15), deg(57.82, -5.79), deg(58.63, -5.01), deg(58.63, -3.01)
        ),
        // Loop 79 (5 points)
        listOf(
            deg(59.91, -165.58), deg(59.75, -166.19), deg(60.21, -167.46), deg(60.29, -165.67),
            deg(59.91, -165.58)
        ),
        // Loop 80 (5 points)
        listOf(
            deg(62.16, -79.27), deg(61.63, -79.66), deg(62.02, -80.36), deg(62.39, -79.93),
            deg(62.16, -79.27)
        ),
        // Loop 81 (7 points)
        listOf(
            deg(62.71, -81.90), deg(62.16, -83.07), deg(62.18, -83.77), deg(62.45, -83.99),
            deg(62.91, -83.25), deg(62.90, -81.88), deg(62.71, -81.90)
        ),
        // Loop 82 (7 points)
        listOf(
            deg(63.78, -171.73), deg(63.30, -168.69), deg(62.98, -169.53), deg(63.38, -170.67),
            deg(63.32, -171.55), deg(63.41, -171.79), deg(63.78, -171.73)
        ),
        // Loop 83 (16 points)
        listOf(
            deg(65.66, -85.16), deg(65.22, -84.98), deg(65.37, -84.46), deg(64.46, -81.64),
            deg(63.98, -81.55), deg(64.06, -80.82), deg(63.73, -80.10), deg(63.41, -80.99),
            deg(63.65, -82.55), deg(64.10, -83.11), deg(63.05, -85.52), deg(63.64, -85.87),
            deg(63.54, -87.22), deg(64.04, -86.35), deg(65.74, -85.88), deg(65.66, -85.16)
        ),
        // Loop 84 (18 points)
        listOf(
            deg(66.46, -14.51), deg(65.81, -14.74), deg(65.13, -13.61), deg(64.36, -14.91),
            deg(63.50, -18.66), deg(63.96, -22.76), deg(64.40, -21.78), deg(64.89, -23.96),
            deg(65.08, -22.18), deg(65.38, -22.23), deg(65.61, -24.33), deg(66.26, -23.65),
            deg(66.41, -22.13), deg(65.73, -20.58), deg(66.28, -19.06), deg(65.99, -17.80),
            deg(66.53, -16.17), deg(66.46, -14.51)
        ),
        // Loop 85 (8 points)
        listOf(
            deg(67.15, -75.87), deg(67.10, -76.99), deg(67.59, -77.24), deg(68.15, -76.81),
            deg(68.29, -75.90), deg(68.01, -75.11), deg(67.44, -75.22), deg(67.15, -75.87)
        ),
        // Loop 86 (21 points)
        listOf(
            deg(66.58, -175.01), deg(66.34, -174.34), deg(67.06, -174.57), deg(66.91, -171.86),
            deg(65.98, -169.90), deg(65.54, -170.89), deg(65.44, -172.53), deg(64.46, -172.56),
            deg(64.25, -172.96), deg(64.28, -173.89), deg(64.92, -175.98), deg(65.36, -176.21),
            deg(65.39, -178.36), deg(65.74, -178.90), deg(66.11, -178.69), deg(65.87, -179.88),
            deg(65.40, -179.43), deg(64.98, -180.00), deg(68.96, -180.00), deg(67.21, -174.93),
            deg(66.58, -175.01)
        ),
        // Loop 87 (8 points)
        listOf(
            deg(69.11, -95.65), deg(68.76, -96.27), deg(69.06, -97.62), deg(68.95, -98.43),
            deg(69.40, -99.80), deg(70.14, -98.22), deg(69.68, -96.56), deg(69.11, -95.65)
        ),
        // Loop 88 (5 points)
        listOf(
            deg(70.83, 180.00), deg(70.78, 178.90), deg(71.10, 178.73), deg(71.52, 180.00),
            deg(70.83, 180.00)
        ),
        // Loop 89 (5 points)
        listOf(
            deg(70.89, -178.69), deg(70.83, -180.00), deg(71.52, -180.00), deg(71.27, -177.58),
            deg(70.89, -178.69)
        ),
        // Loop 90 (612 points)
        listOf(
            deg(69.50, -90.55), deg(68.48, -90.55), deg(69.26, -89.22), deg(68.62, -88.02),
            deg(67.87, -88.32), deg(67.20, -87.35), deg(67.92, -86.31), deg(68.78, -85.58),
            deg(69.88, -85.52), deg(69.66, -82.62), deg(69.16, -81.28), deg(68.67, -81.22),
            deg(68.13, -81.96), deg(67.60, -81.26), deg(67.11, -81.39), deg(66.41, -83.34),
            deg(66.26, -84.74), deg(66.56, -85.77), deg(64.78, -87.32), deg(64.10, -88.48),
            deg(64.03, -89.91), deg(63.61, -90.70), deg(62.96, -90.77), deg(62.84, -91.93),
            deg(62.02, -93.16), deg(60.90, -94.24), deg(60.11, -94.63), deg(58.95, -94.68),
            deg(58.78, -93.22), deg(57.09, -92.30), deg(57.28, -90.90), deg(56.85, -89.04),
            deg(56.00, -87.32), deg(55.30, -85.01), deg(55.15, -82.27), deg(54.28, -82.44),
            deg(53.28, -82.13), deg(52.16, -81.40), deg(51.21, -79.91), deg(51.53, -79.14),
            deg(52.56, -78.60), deg(54.14, -79.12), deg(54.67, -79.83), deg(55.14, -78.23),
            deg(55.84, -77.10), deg(56.53, -76.54), deg(57.20, -76.62), deg(58.05, -77.30),
            deg(58.80, -78.52), deg(59.85, -77.34), deg(62.32, -78.11), deg(62.55, -77.41),
            deg(62.18, -74.67), deg(62.44, -73.84), deg(61.53, -71.68), deg(61.14, -71.37),
            deg(61.06, -69.59), deg(58.96, -69.29), deg(58.80, -68.37), deg(58.21, -67.65),
            deg(58.77, -66.20), deg(60.34, -64.58), deg(56.97, -61.40), deg(56.34, -61.80),
            deg(55.20, -59.57), deg(54.63, -57.33), deg(53.78, -56.94), deg(53.65, -56.16),
            deg(53.27, -55.76), deg(52.15, -55.68), deg(51.42, -57.13), deg(51.06, -58.77),
            deg(50.24, -60.03), deg(50.08, -61.72), deg(50.23, -66.40), deg(49.51, -67.24),
            deg(49.07, -68.51), deg(46.82, -71.10), deg(46.99, -70.26), deg(48.30, -68.65),
            deg(49.13, -66.55), deg(49.23, -65.06), deg(48.74, -64.17), deg(48.07, -65.12),
            deg(46.24, -64.47), deg(45.74, -63.17), deg(45.88, -61.52), deg(47.01, -60.52),
            deg(46.28, -60.45), deg(45.92, -59.80), deg(45.27, -61.04), deg(44.27, -64.25),
            deg(43.55, -65.36), deg(43.62, -66.12), deg(44.47, -66.16), deg(45.29, -64.43),
            deg(45.14, -67.14), deg(44.81, -66.96), deg(43.68, -70.12), deg(43.03, -70.69),
            deg(42.34, -70.83), deg(41.81, -70.49), deg(41.78, -70.08), deg(42.15, -70.18),
            deg(41.92, -69.88), deg(41.64, -69.97), deg(40.93, -73.71), deg(41.12, -72.24),
            deg(40.93, -71.95), deg(40.63, -73.34), deg(40.75, -73.95), deg(40.47, -74.26),
            deg(40.43, -73.96), deg(39.71, -74.18), deg(38.94, -74.91), deg(39.50, -75.53),
            deg(38.40, -75.06), deg(37.22, -75.94), deg(37.94, -75.72), deg(38.32, -76.23),
            deg(39.15, -76.35), deg(38.72, -76.54), deg(38.08, -76.33), deg(38.23, -76.96),
            deg(37.92, -76.30), deg(36.97, -76.26), deg(36.90, -75.97), deg(35.55, -75.73),
            deg(34.81, -76.36), deg(34.51, -77.40), deg(33.93, -78.05), deg(33.49, -79.06),
            deg(33.16, -79.20), deg(32.51, -80.30), deg(31.44, -81.34), deg(30.73, -81.49),
            deg(30.04, -81.31), deg(26.88, -80.06), deg(25.21, -80.38), deg(25.20, -81.17),
            deg(25.64, -81.33), deg(25.87, -81.71), deg(27.89, -82.86), deg(28.55, -82.65),
            deg(29.10, -82.93), deg(30.09, -84.10), deg(29.64, -85.11), deg(30.40, -86.40),
            deg(30.32, -89.18), deg(30.18, -89.60), deg(29.29, -89.22), deg(29.12, -90.15),
            deg(29.15, -90.88), deg(29.68, -91.63), deg(29.71, -93.85), deg(29.48, -94.69),
            deg(28.74, -95.60), deg(28.31, -96.59), deg(27.38, -97.37), deg(25.87, -97.14),
            deg(24.27, -97.70), deg(22.44, -97.87), deg(20.64, -97.19), deg(19.32, -96.29),
            deg(18.83, -95.90), deg(18.56, -94.84), deg(18.14, -94.43), deg(18.70, -92.04),
            deg(19.28, -90.77), deg(21.00, -90.28), deg(21.49, -88.54), deg(21.54, -87.05),
            deg(21.33, -86.81), deg(20.85, -86.85), deg(19.65, -87.62), deg(19.47, -87.44),
            deg(18.26, -87.84), deg(18.50, -88.30), deg(18.35, -88.11), deg(16.53, -88.36),
            deg(15.89, -88.93), deg(15.69, -88.12), deg(15.86, -87.90), deg(15.76, -86.90),
            deg(16.00, -84.98), deg(15.27, -83.41), deg(15.00, -83.15), deg(14.31, -83.18),
            deg(13.57, -83.52), deg(12.42, -83.47), deg(11.10, -83.81), deg(9.00, -82.21),
            deg(9.03, -81.71), deg(8.79, -81.44), deg(9.61, -79.57), deg(9.42, -78.50),
            deg(8.67, -77.35), deg(8.64, -76.84), deg(9.44, -75.67), deg(10.62, -75.48),
            deg(11.08, -74.91), deg(11.10, -74.28), deg(11.31, -74.20), deg(11.23, -73.41),
            deg(12.44, -71.75), deg(12.11, -71.14), deg(11.54, -71.36), deg(11.42, -71.95),
            deg(10.97, -71.62), deg(10.45, -71.63), deg(9.87, -72.07), deg(9.07, -71.70),
            deg(9.14, -71.26), deg(9.86, -71.04), deg(10.21, -71.35), deg(10.97, -71.40),
            deg(11.38, -70.16), deg(11.85, -70.29), deg(12.16, -69.94), deg(11.46, -69.58),
            deg(11.44, -68.88), deg(10.89, -68.23), deg(10.55, -68.19), deg(10.65, -66.23),
            deg(10.20, -65.66), deg(10.08, -64.89), deg(10.39, -64.33), deg(10.64, -64.32),
            deg(10.72, -61.88), deg(10.42, -62.73), deg(9.95, -62.39), deg(9.87, -61.59),
            deg(9.38, -60.83), deg(8.58, -60.67), deg(8.60, -60.15), deg(8.00, -59.10),
            deg(7.35, -58.48), deg(6.83, -58.45), deg(6.81, -58.08), deg(5.97, -57.15),
            deg(5.77, -55.95), deg(6.03, -55.03), deg(5.76, -53.96), deg(5.41, -52.88),
            deg(4.57, -51.82), deg(4.16, -51.66), deg(4.20, -51.32), deg(1.90, -50.51),
            deg(1.74, -49.97), deg(1.05, -49.95), deg(0.22, -50.70), deg(-0.08, -50.39),
            deg(-0.24, -48.62), deg(-1.24, -48.58), deg(-0.58, -47.82), deg(-1.55, -44.91),
            deg(-2.14, -44.42), deg(-2.69, -44.58), deg(-2.38, -43.42), deg(-2.91, -41.47),
            deg(-2.87, -39.98), deg(-3.70, -38.50), deg(-4.82, -37.22), deg(-5.15, -35.60),
            deg(-5.46, -35.24), deg(-7.34, -34.73), deg(-9.00, -35.13), deg(-11.04, -37.05),
            deg(-13.04, -38.42), deg(-13.06, -38.67), deg(-13.79, -38.95), deg(-15.67, -38.88),
            deg(-17.87, -39.27), deg(-18.26, -39.58), deg(-19.60, -39.76), deg(-20.90, -40.77),
            deg(-21.94, -40.94), deg(-22.37, -41.75), deg(-22.97, -41.99), deg(-22.97, -43.07),
            deg(-23.35, -44.65), deg(-24.09, -46.47), deg(-24.89, -47.65), deg(-25.88, -48.50),
            deg(-26.62, -48.64), deg(-27.18, -48.47), deg(-28.67, -48.89), deg(-29.22, -49.59),
            deg(-30.98, -50.70), deg(-32.25, -52.26), deg(-33.20, -52.71), deg(-34.40, -53.81),
            deg(-34.95, -54.94), deg(-34.86, -56.22), deg(-34.43, -57.14), deg(-34.46, -57.82),
            deg(-33.91, -58.43), deg(-34.43, -58.50), deg(-35.29, -57.23), deg(-35.98, -57.36),
            deg(-36.41, -56.74), deg(-36.90, -56.79), deg(-38.18, -57.75), deg(-38.72, -59.23),
            deg(-38.83, -62.34), deg(-40.68, -62.15), deg(-41.03, -62.75), deg(-41.17, -63.77),
            deg(-40.80, -64.73), deg(-41.06, -65.12), deg(-42.06, -64.98), deg(-42.36, -64.30),
            deg(-42.04, -63.76), deg(-42.56, -63.46), deg(-42.87, -64.38), deg(-43.50, -65.18),
            deg(-45.04, -65.57), deg(-45.04, -66.51), deg(-45.55, -67.29), deg(-46.30, -67.58),
            deg(-47.03, -66.60), deg(-47.24, -65.64), deg(-48.13, -65.99), deg(-48.70, -67.17),
            deg(-49.87, -67.82), deg(-50.26, -68.73), deg(-50.73, -69.14), deg(-51.77, -68.82),
            deg(-52.35, -68.15), deg(-52.29, -69.46), deg(-52.90, -70.85), deg(-53.83, -71.01),
            deg(-53.53, -72.56), deg(-52.26, -74.95), deg(-51.63, -75.26), deg(-51.04, -74.98),
            deg(-50.38, -75.48), deg(-48.67, -75.61), deg(-47.71, -75.18), deg(-46.94, -74.13),
            deg(-46.65, -75.64), deg(-45.76, -74.69), deg(-44.10, -74.35), deg(-44.45, -73.24),
            deg(-42.38, -72.72), deg(-42.12, -73.39), deg(-43.37, -73.70), deg(-43.22, -74.33),
            deg(-39.94, -73.68), deg(-39.26, -73.22), deg(-37.16, -73.59), deg(-37.12, -73.17),
            deg(-32.42, -71.44), deg(-30.92, -71.67), deg(-30.10, -71.37), deg(-28.86, -71.49),
            deg(-27.64, -70.91), deg(-21.39, -70.09), deg(-19.76, -70.16), deg(-18.35, -70.37),
            deg(-17.77, -71.38), deg(-17.36, -71.46), deg(-16.36, -73.44), deg(-14.65, -76.01),
            deg(-13.82, -76.42), deg(-13.53, -76.26), deg(-7.19, -79.76), deg(-6.14, -81.25),
            deg(-5.69, -80.93), deg(-4.74, -81.41), deg(-4.04, -81.10), deg(-2.66, -79.77),
            deg(-2.22, -79.99), deg(-2.69, -80.37), deg(-2.25, -80.97), deg(-1.97, -80.76),
            deg(-1.06, -80.93), deg(-0.91, -80.58), deg(0.36, -80.02), deg(0.77, -80.09),
            deg(1.38, -78.86), deg(1.69, -78.99), deg(1.77, -78.62), deg(2.27, -78.66),
            deg(2.63, -78.43), deg(2.70, -77.93), deg(3.85, -77.13), deg(4.09, -77.50),
            deg(4.67, -77.31), deg(6.69, -77.48), deg(7.51, -78.21), deg(8.05, -78.43),
            deg(8.32, -78.18), deg(9.00, -79.12), deg(8.93, -79.56), deg(8.09, -80.48),
            deg(7.55, -80.00), deg(7.22, -80.89), deg(7.82, -81.06), deg(7.71, -81.52),
            deg(8.11, -81.72), deg(8.29, -82.82), deg(8.07, -82.85), deg(8.45, -83.51),
            deg(8.66, -83.71), deg(9.05, -83.63), deg(9.62, -84.65), deg(10.09, -84.98),
            deg(9.56, -85.11), deg(9.93, -85.66), deg(10.75, -85.66), deg(10.90, -85.94),
            deg(11.09, -85.71), deg(12.91, -87.67), deg(12.98, -87.32), deg(13.30, -87.49),
            deg(13.16, -88.48), deg(13.93, -91.23), deg(15.62, -93.36), deg(16.20, -94.69),
            deg(15.65, -96.56), deg(17.17, -100.83), deg(17.92, -101.92), deg(18.29, -103.50),
            deg(19.32, -104.99), deg(19.95, -105.49), deg(20.43, -105.73), deg(20.53, -105.40),
            deg(21.42, -105.27), deg(22.77, -106.03), deg(25.17, -108.40), deg(25.58, -109.26),
            deg(25.82, -109.44), deg(26.44, -109.29), deg(27.16, -110.39), deg(27.86, -110.64),
            deg(27.94, -111.18), deg(28.95, -112.23), deg(30.79, -113.16), deg(31.17, -113.15),
            deg(31.80, -114.78), deg(31.39, -114.94), deg(30.16, -114.67), deg(26.66, -111.62),
            deg(24.30, -110.66), deg(24.27, -110.17), deg(23.19, -109.43), deg(22.82, -110.03),
            deg(23.43, -110.29), deg(24.74, -112.18), deg(26.01, -112.30), deg(27.14, -114.47),
            deg(27.72, -115.06), deg(27.74, -114.57), deg(28.12, -114.20), deg(28.57, -114.16),
            deg(29.56, -115.52), deg(33.05, -117.30), deg(33.74, -118.41), deg(34.03, -118.52),
            deg(34.61, -120.62), deg(35.16, -120.74), deg(36.16, -121.71), deg(37.78, -122.51),
            deg(38.95, -123.73), deg(39.77, -123.87), deg(40.31, -124.40), deg(42.00, -124.21),
            deg(42.77, -124.53), deg(43.71, -124.14), deg(45.52, -123.90), deg(46.86, -124.08),
            deg(48.18, -124.69), deg(48.38, -124.57), deg(48.04, -123.12), deg(47.10, -122.59),
            deg(47.36, -122.34), deg(49.00, -122.84), deg(50.42, -125.62), deg(50.83, -127.44),
            deg(51.72, -127.99), deg(52.33, -127.85), deg(52.76, -129.13), deg(53.56, -129.31),
            deg(54.29, -130.51), deg(54.80, -130.54), deg(55.50, -131.97), deg(56.37, -132.25),
            deg(57.18, -133.54), deg(58.12, -134.08), deg(58.21, -136.63), deg(58.50, -137.80),
            deg(59.54, -139.87), deg(60.08, -142.57), deg(60.00, -143.96), deg(60.88, -147.11),
            deg(60.67, -148.22), deg(59.98, -148.02), deg(59.16, -151.72), deg(59.75, -151.86),
            deg(60.73, -151.41), deg(61.03, -150.35), deg(61.28, -150.62), deg(59.35, -154.02),
            deg(58.86, -153.29), deg(58.15, -154.23), deg(57.42, -156.31), deg(56.98, -156.56),
            deg(56.46, -158.12), deg(55.99, -158.43), deg(54.40, -164.79), deg(54.57, -164.94),
            deg(55.90, -161.80), deg(56.01, -160.56), deg(57.57, -157.72), deg(58.33, -157.55),
            deg(58.92, -157.04), deg(58.62, -158.19), deg(58.79, -158.52), deg(58.42, -159.06),
            deg(58.93, -159.71), deg(58.57, -159.98), deg(59.07, -160.36), deg(58.67, -161.97),
            deg(59.63, -161.87), deg(59.99, -162.52), deg(59.80, -163.82), deg(60.51, -165.35),
            deg(61.07, -165.35), deg(61.50, -166.12), deg(63.15, -164.56), deg(63.06, -163.07),
            deg(63.54, -162.26), deg(63.46, -161.53), deg(63.77, -160.77), deg(64.22, -160.96),
            deg(64.40, -161.52), deg(64.79, -160.78), deg(64.34, -162.76), deg(64.56, -163.55),
            deg(64.45, -164.96), deg(64.69, -166.43), deg(65.67, -168.11), deg(66.58, -164.47),
            deg(66.58, -163.65), deg(66.08, -163.79), deg(66.12, -161.68), deg(66.74, -162.49),
            deg(67.12, -163.72), deg(68.04, -165.39), deg(68.36, -166.76), deg(68.88, -166.20),
            deg(68.92, -164.43), deg(69.37, -163.17), deg(69.86, -162.93), deg(70.33, -161.91),
            deg(70.89, -159.04), deg(70.82, -158.12), deg(71.36, -156.58), deg(71.15, -155.07),
            deg(70.70, -154.34), deg(70.89, -153.90), deg(70.83, -152.21), deg(70.60, -152.27),
            deg(70.43, -150.74), deg(70.53, -149.72), deg(69.99, -144.92), deg(70.15, -143.59),
            deg(68.90, -136.50), deg(69.63, -134.41), deg(69.51, -132.93), deg(70.19, -129.79),
            deg(69.78, -129.11), deg(70.01, -128.36), deg(70.48, -128.14), deg(70.38, -127.45),
            deg(69.48, -125.76), deg(70.16, -124.42), deg(69.40, -124.29), deg(69.56, -123.06),
            deg(69.86, -122.68), deg(69.80, -121.47), deg(69.01, -117.60), deg(68.91, -115.25),
            deg(68.40, -113.90), deg(67.90, -115.30), deg(67.69, -113.50), deg(67.98, -109.95),
            deg(67.38, -108.88), deg(67.89, -107.79), deg(68.31, -108.81), deg(68.65, -108.17),
            deg(68.80, -106.15), deg(68.02, -104.34), deg(68.10, -103.22), deg(67.65, -101.45),
            deg(67.78, -98.44), deg(68.40, -98.56), deg(68.58, -97.67), deg(68.24, -96.12),
            deg(67.29, -96.13), deg(68.09, -95.49), deg(68.06, -94.68), deg(69.07, -94.23),
            deg(70.09, -96.47), deg(71.19, -96.39), deg(71.92, -95.21), deg(71.76, -93.89),
            deg(71.32, -92.88), deg(70.19, -91.52), deg(69.70, -92.41), deg(69.50, -90.55)
        ),
        // Loop 91 (33 points)
        listOf(
            deg(73.12, -114.17), deg(72.65, -114.67), deg(72.96, -112.44), deg(72.45, -111.05),
            deg(72.96, -109.92), deg(72.63, -109.01), deg(71.65, -108.19), deg(72.07, -107.69),
            deg(73.09, -108.40), deg(73.24, -107.52), deg(73.08, -106.52), deg(72.67, -105.40),
            deg(70.99, -104.46), deg(70.02, -100.98), deg(69.58, -101.09), deg(69.50, -102.73),
            deg(69.12, -102.09), deg(68.75, -102.43), deg(69.18, -105.96), deg(68.54, -113.31),
            deg(69.01, -113.85), deg(69.28, -115.22), deg(69.17, -116.11), deg(69.96, -117.34),
            deg(70.37, -112.42), deg(70.60, -114.35), deg(70.54, -117.90), deg(70.91, -118.43),
            deg(71.31, -116.11), deg(71.56, -119.40), deg(72.71, -117.87), deg(73.31, -115.19),
            deg(73.12, -114.17)
        ),
        // Loop 92 (5 points)
        listOf(
            deg(73.42, -104.50), deg(72.76, -105.38), deg(73.46, -106.94), deg(73.64, -105.26),
            deg(73.42, -104.50)
        )
    )
}

private fun getGlobePart4(): List<List<Pair<Double, Double>>> {
    return listOf(
        // Loop 93 (8 points)
        listOf(
            deg(73.10, -76.34), deg(72.83, -76.25), deg(72.74, -79.49), deg(73.33, -80.88),
            deg(73.69, -80.83), deg(73.76, -80.35), deg(73.65, -78.06), deg(73.10, -76.34)
        ),
        // Loop 94 (61 points)
        listOf(
            deg(73.16, -86.56), deg(72.53, -85.77), deg(73.34, -84.85), deg(73.75, -82.32),
            deg(72.72, -80.60), deg(72.06, -80.75), deg(72.35, -78.77), deg(72.75, -77.82),
            deg(71.77, -74.23), deg(71.33, -74.10), deg(71.56, -72.24), deg(70.92, -71.20),
            deg(70.53, -68.79), deg(70.12, -67.91), deg(69.19, -66.97), deg(68.72, -68.81),
            deg(67.85, -64.86), deg(66.93, -63.42), deg(66.86, -61.85), deg(66.16, -62.16),
            deg(65.00, -63.92), deg(65.43, -65.15), deg(66.39, -66.72), deg(66.26, -68.02),
            deg(65.69, -68.14), deg(64.38, -65.32), deg(63.39, -64.67), deg(62.67, -65.01),
            deg(63.75, -68.78), deg(62.28, -66.33), deg(61.93, -66.17), deg(62.91, -71.02),
            deg(63.40, -72.24), deg(63.68, -71.89), deg(64.68, -74.83), deg(64.39, -74.82),
            deg(64.23, -77.71), deg(64.57, -78.56), deg(65.31, -77.90), deg(65.45, -73.96),
            deg(65.81, -74.29), deg(67.28, -72.65), deg(67.73, -72.93), deg(68.07, -73.31),
            deg(68.55, -74.84), deg(68.89, -76.87), deg(69.15, -76.23), deg(69.77, -77.29),
            deg(70.17, -78.96), deg(69.87, -79.49), deg(69.74, -81.31), deg(69.97, -84.94),
            deg(70.41, -88.68), deg(70.76, -89.51), deg(71.22, -88.47), deg(71.22, -89.89),
            deg(72.24, -90.21), deg(73.13, -89.44), deg(73.54, -88.41), deg(73.80, -85.83),
            deg(73.16, -86.56)
        ),
        // Loop 95 (14 points)
        listOf(
            deg(73.84, -100.36), deg(73.63, -99.16), deg(73.76, -97.38), deg(73.47, -97.12),
            deg(72.99, -98.05), deg(72.56, -96.54), deg(71.66, -96.72), deg(71.27, -98.36),
            deg(71.36, -99.32), deg(72.51, -102.50), deg(72.83, -102.48), deg(72.71, -100.44),
            deg(73.36, -101.54), deg(73.84, -100.36)
        ),
        // Loop 96 (6 points)
        listOf(
            deg(73.21, 143.60), deg(73.37, 139.86), deg(73.77, 140.81), deg(73.86, 142.06),
            deg(73.48, 143.48), deg(73.21, 143.60)
        ),
        // Loop 97 (10 points)
        listOf(
            deg(72.77, -93.20), deg(72.02, -94.27), deg(72.06, -95.41), deg(72.94, -96.03),
            deg(73.44, -96.02), deg(73.86, -95.50), deg(74.13, -94.50), deg(73.86, -90.51),
            deg(72.97, -92.00), deg(72.77, -93.20)
        ),
        // Loop 98 (12 points)
        listOf(
            deg(71.40, -120.46), deg(70.90, -123.09), deg(71.34, -123.62), deg(71.87, -125.93),
            deg(73.68, -123.94), deg(74.29, -124.92), deg(74.45, -121.54), deg(74.19, -117.56),
            deg(73.48, -115.51), deg(72.52, -119.22), deg(71.82, -120.46), deg(71.40, -120.46)
        ),
        // Loop 99 (5 points)
        listOf(
            deg(75.08, 150.73), deg(74.69, 149.58), deg(75.17, 146.12), deg(75.50, 146.36),
            deg(75.08, 150.73)
        ),
        // Loop 100 (6 points)
        listOf(
            deg(74.98, -93.61), deg(74.59, -94.16), deg(74.93, -96.82), deg(75.38, -96.29),
            deg(75.65, -94.85), deg(74.98, -93.61)
        ),
        // Loop 101 (9 points)
        listOf(
            deg(75.56, 145.09), deg(74.82, 144.30), deg(74.85, 140.61), deg(74.61, 138.96),
            deg(75.26, 136.97), deg(75.95, 137.51), deg(76.14, 138.83), deg(76.09, 141.47),
            deg(75.56, 145.09)
        ),
        // Loop 102 (10 points)
        listOf(
            deg(76.72, -98.50), deg(76.26, -97.74), deg(75.74, -97.70), deg(75.00, -98.16),
            deg(74.90, -99.81), deg(75.06, -100.88), deg(75.64, -100.86), deg(75.56, -102.50),
            deg(76.34, -102.57), deg(76.72, -98.50)
        ),
        // Loop 103 (21 points)
        listOf(
            deg(76.20, -108.21), deg(75.85, -107.82), deg(75.97, -105.88), deg(75.48, -105.70),
            deg(75.01, -106.31), deg(74.85, -109.70), deg(74.42, -112.22), deg(74.39, -113.74),
            deg(74.72, -113.87), deg(75.16, -111.79), deg(75.04, -116.31), deg(75.22, -117.71),
            deg(76.20, -116.35), deg(76.48, -115.40), deg(76.14, -112.59), deg(75.55, -110.81),
            deg(75.47, -109.07), deg(76.43, -110.50), deg(76.79, -109.58), deg(76.68, -108.55),
            deg(76.20, -108.21)
        ),
        // Loop 104 (20 points)
        listOf(
            deg(70.72, 57.54), deg(70.76, 53.68), deg(71.21, 53.41), deg(71.47, 51.60),
            deg(72.01, 51.46), deg(72.23, 52.48), deg(72.77, 52.44), deg(73.63, 54.43),
            deg(73.75, 53.51), deg(74.63, 55.90), deg(75.08, 55.63), deg(76.25, 61.17),
            deg(76.94, 68.16), deg(76.54, 68.85), deg(76.23, 68.18), deg(75.26, 61.58),
            deg(74.31, 58.48), deg(72.37, 55.42), deg(71.54, 55.62), deg(70.72, 57.54)
        ),
        // Loop 105 (19 points)
        listOf(
            deg(77.10, -94.68), deg(76.78, -93.57), deg(76.78, -91.61), deg(76.45, -90.74),
            deg(76.07, -90.97), deg(75.61, -89.19), deg(75.48, -86.38), deg(75.71, -81.13),
            deg(75.34, -80.06), deg(74.92, -79.83), deg(74.44, -81.95), deg(74.52, -89.76),
            deg(74.84, -92.42), deg(75.88, -92.89), deg(76.32, -93.89), deg(76.44, -95.96),
            deg(76.75, -97.12), deg(77.16, -96.75), deg(77.10, -94.68)
        ),
        // Loop 106 (7 points)
        listOf(
            deg(77.65, -116.20), deg(76.88, -116.34), deg(76.53, -117.11), deg(75.90, -121.50),
            deg(76.12, -122.85), deg(77.51, -119.10), deg(77.65, -116.20)
        ),
        // Loop 107 (858 points)
        listOf(
            deg(76.97, 106.97), deg(76.48, 107.24), deg(76.72, 108.15), deg(76.71, 111.08),
            deg(75.85, 114.13), deg(75.33, 113.89), deg(74.18, 109.40), deg(73.79, 112.12),
            deg(73.98, 113.02), deg(73.34, 113.53), deg(73.59, 113.97), deg(73.75, 115.57),
            deg(73.59, 118.78), deg(73.12, 119.02), deg(72.97, 123.20), deg(73.74, 123.26),
            deg(73.57, 126.98), deg(73.04, 128.59), deg(72.40, 129.05), deg(71.98, 128.46),
            deg(71.19, 129.72), deg(70.79, 131.29), deg(71.84, 132.25), deg(71.39, 133.86),
            deg(71.66, 135.56), deg(71.35, 137.50), deg(71.63, 138.23), deg(71.49, 139.87),
            deg(72.42, 139.15), deg(72.85, 140.47), deg(72.20, 149.50), deg(71.61, 150.35),
            deg(70.84, 152.97), deg(71.03, 157.01), deg(70.87, 159.00), deg(70.45, 159.83),
            deg(69.72, 159.71), deg(69.44, 160.94), deg(69.64, 162.28), deg(69.58, 167.84),
            deg(68.69, 169.58), deg(69.01, 170.82), deg(69.65, 170.01), deg(70.10, 170.45),
            deg(69.88, 175.72), deg(68.96, 180.00), deg(64.98, 180.00), deg(64.53, 178.71),
            deg(64.61, 177.41), deg(64.08, 178.31), deg(62.98, 179.37), deg(62.57, 179.49),
            deg(62.30, 179.23), deg(62.52, 177.36), deg(61.65, 173.68), deg(60.34, 170.70),
            deg(59.88, 170.33), deg(60.57, 168.90), deg(59.79, 166.30), deg(60.16, 165.84),
            deg(59.73, 164.88), deg(59.87, 163.54), deg(59.21, 163.22), deg(58.24, 162.02),
            deg(57.84, 162.05), deg(57.62, 163.19), deg(56.16, 163.06), deg(56.12, 162.13),
            deg(55.29, 161.70), deg(54.86, 162.12), deg(54.34, 160.37), deg(53.20, 160.02),
            deg(52.96, 158.53), deg(51.94, 158.23), deg(51.01, 156.79), deg(55.38, 155.43),
            deg(56.77, 155.91), deg(57.36, 156.76), deg(57.83, 156.81), deg(58.06, 158.36),
            deg(60.34, 161.87), deg(61.14, 163.67), deg(62.55, 164.47), deg(62.47, 163.26),
            deg(61.64, 162.66), deg(60.54, 160.12), deg(61.77, 159.30), deg(61.43, 156.72),
            deg(59.76, 154.22), deg(59.15, 155.04), deg(58.78, 151.27), deg(59.50, 151.34),
            deg(59.66, 149.78), deg(59.16, 148.54), deg(59.34, 145.49), deg(59.04, 142.20),
            deg(54.73, 135.13), deg(54.60, 136.70), deg(53.98, 137.19), deg(53.76, 138.16),
            deg(54.25, 138.80), deg(54.19, 139.90), deg(53.09, 141.35), deg(52.24, 141.38),
            deg(51.24, 140.60), deg(48.45, 140.06), deg(47.00, 138.55), deg(46.31, 138.22),
            deg(43.40, 134.87), deg(42.81, 133.54), deg(42.80, 132.91), deg(43.28, 132.28),
            deg(42.55, 130.94), deg(42.22, 130.78), deg(42.28, 130.40), deg(41.94, 129.97),
            deg(41.60, 129.67), deg(40.88, 129.71), deg(39.76, 127.53), deg(39.21, 127.39),
            deg(38.61, 128.35), deg(36.78, 129.46), deg(35.63, 129.47), deg(35.08, 129.09),
            deg(34.39, 126.49), deg(35.68, 126.56), deg(36.73, 126.12), deg(36.89, 126.86),
            deg(37.75, 126.17), deg(37.94, 125.69), deg(37.67, 125.28), deg(38.11, 124.71),
            deg(38.67, 125.22), deg(39.55, 125.32), deg(39.93, 124.27), deg(39.64, 122.87),
            deg(39.17, 122.13), deg(38.90, 121.05), deg(39.36, 121.59), deg(39.75, 121.38),
            deg(40.42, 122.17), deg(40.95, 121.64), deg(39.90, 119.64), deg(39.25, 119.02),
            deg(39.20, 118.04), deg(38.74, 117.53), deg(38.06, 118.06), deg(37.90, 118.88),
            deg(37.45, 118.91), deg(37.16, 119.70), deg(37.87, 120.82), deg(37.45, 122.36),
            deg(36.93, 122.52), deg(36.65, 121.10), deg(36.11, 120.64), deg(35.61, 119.66),
            deg(34.91, 119.15), deg(34.36, 120.23), deg(33.38, 120.62), deg(31.69, 121.91),
            deg(30.95, 121.89), deg(30.68, 121.26), deg(30.14, 121.50), deg(29.83, 122.09),
            deg(28.23, 121.68), deg(28.14, 121.13), deg(24.55, 118.66), deg(22.78, 115.89),
            deg(22.67, 114.76), deg(22.22, 114.15), deg(22.55, 113.81), deg(22.05, 113.24),
            deg(21.40, 110.79), deg(20.34, 110.44), deg(20.28, 109.89), deg(21.01, 109.63),
            deg(21.40, 109.86), deg(21.72, 108.52), deg(20.70, 106.72), deg(19.75, 105.88),
            deg(19.06, 105.66), deg(16.70, 107.36), deg(16.08, 108.27), deg(15.28, 108.88),
            deg(13.43, 109.34), deg(11.67, 109.20), deg(10.36, 107.22), deg(8.60, 105.16),
            deg(9.24, 104.80), deg(9.92, 105.08), deg(10.49, 104.33), deg(10.63, 103.50),
            deg(12.19, 102.59), deg(12.65, 101.69), deg(12.63, 100.83), deg(13.41, 100.98),
            deg(13.41, 100.10), deg(12.31, 100.02), deg(9.96, 99.15), deg(9.24, 99.22),
            deg(9.21, 99.87), deg(7.43, 100.46), deg(6.86, 101.02), deg(6.74, 101.62),
            deg(5.52, 102.96), deg(4.86, 103.38), deg(2.79, 103.50), deg(2.52, 103.85),
            deg(1.29, 104.23), deg(1.23, 103.52), deg(2.76, 101.39), deg(5.31, 100.20),
            deg(6.04, 100.31), deg(6.46, 100.09), deg(8.38, 98.50), deg(7.79, 98.34),
            deg(8.35, 98.15), deg(9.93, 98.55), deg(10.68, 98.46), deg(11.44, 98.76),
            deg(12.03, 98.43), deg(13.12, 98.51), deg(13.64, 98.10), deg(16.10, 97.60),
            deg(16.93, 97.16), deg(15.71, 95.37), deg(16.04, 94.19), deg(17.28, 94.53),
            deg(18.21, 94.32), deg(19.37, 93.54), deg(19.73, 93.66), deg(19.86, 93.08),
            deg(20.67, 92.37), deg(22.77, 91.42), deg(22.81, 90.50), deg(22.39, 90.59),
            deg(21.84, 90.27), deg(22.06, 89.03), deg(21.69, 88.89), deg(21.50, 86.98),
            deg(20.74, 87.03), deg(20.15, 86.50), deg(19.48, 85.06), deg(18.30, 83.94),
            deg(17.02, 82.19), deg(16.56, 82.19), deg(15.90, 80.32), deg(15.14, 80.03),
            deg(13.01, 80.29), deg(12.06, 79.86), deg(10.36, 79.86), deg(10.31, 79.34),
            deg(9.55, 78.89), deg(9.22, 79.19), deg(8.93, 78.28), deg(7.97, 77.54),
            deg(8.90, 76.59), deg(11.31, 75.75), deg(12.74, 74.86), deg(14.62, 74.44),
            deg(15.99, 73.53), deg(19.21, 72.82), deg(21.36, 72.63), deg(20.76, 71.18),
            deg(20.88, 70.47), deg(22.09, 69.16), deg(22.45, 69.64), deg(22.84, 69.35),
            deg(23.94, 67.44), deg(24.66, 67.15), deg(25.43, 66.37), deg(25.08, 61.50),
            deg(25.74, 57.40), deg(26.97, 56.97), deg(27.14, 56.49), deg(26.48, 54.72),
            deg(26.81, 53.49), deg(27.58, 52.48), deg(27.87, 51.52), deg(30.15, 50.12),
            deg(29.99, 49.58), deg(30.32, 48.94), deg(29.93, 48.57), deg(29.98, 47.97),
            deg(27.69, 48.81), deg(26.69, 50.15), deg(25.94, 50.11), deg(24.75, 50.81),
            deg(25.48, 50.74), deg(26.01, 51.01), deg(26.11, 51.29), deg(25.80, 51.59),
            deg(24.63, 51.39), deg(24.02, 51.79), deg(24.12, 54.01), deg(26.40, 56.36),
            deg(24.92, 56.40), deg(24.24, 56.85), deg(23.88, 57.40), deg(23.57, 58.73),
            deg(22.31, 59.81), deg(20.43, 58.49), deg(20.48, 58.03), deg(20.24, 57.83),
            deg(18.94, 57.69), deg(18.95, 57.23), deg(18.57, 56.61), deg(17.88, 56.28),
            deg(17.88, 55.66), deg(17.63, 55.27), deg(17.23, 55.27), deg(16.38, 52.39),
            deg(15.60, 52.17), deg(14.71, 49.57), deg(14.00, 48.68), deg(14.01, 47.94),
            deg(13.59, 47.35), deg(13.29, 45.63), deg(12.70, 44.99), deg(12.64, 43.48),
            deg(15.21, 42.60), deg(15.26, 42.81), deg(16.77, 42.65), deg(18.67, 41.22),
            deg(19.49, 40.94), deg(20.34, 39.80), deg(21.29, 39.14), deg(22.58, 39.07),
            deg(23.69, 38.49), deg(24.29, 37.48), deg(25.60, 36.93), deg(28.06, 35.13),
            deg(28.06, 34.63), deg(29.50, 34.92), deg(27.65, 33.92), deg(28.42, 33.14),
            deg(29.85, 32.42), deg(28.71, 32.73), deg(26.14, 34.10), deg(23.93, 35.69),
            deg(23.75, 35.49), deg(23.10, 35.53), deg(22.00, 36.87), deg(18.61, 37.48),
            deg(18.00, 38.41), deg(15.92, 39.27), deg(14.49, 41.18), deg(12.39, 43.32),
            deg(11.97, 43.29), deg(11.74, 42.72), deg(11.28, 43.47), deg(10.45, 44.12),
            deg(10.44, 44.61), deg(12.02, 51.11), deg(10.64, 51.05), deg(6.80, 49.45),
            deg(4.22, 47.74), deg(2.86, 46.56), deg(0.29, 43.14), deg(-1.68, 41.59),
            deg(-2.57, 40.26), deg(-4.35, 39.60), deg(-4.68, 39.20), deg(-5.91, 38.74),
            deg(-6.48, 38.80), deg(-6.84, 39.44), deg(-8.49, 39.19), deg(-10.77, 40.48),
            deg(-14.69, 40.78), deg(-16.10, 40.09), deg(-16.72, 39.45), deg(-17.59, 37.41),
            deg(-19.78, 34.79), deg(-20.50, 34.70), deg(-22.14, 35.39), deg(-22.09, 35.56),
            deg(-23.54, 35.37), deg(-23.71, 35.61), deg(-24.48, 35.04), deg(-25.73, 32.57),
            deg(-26.15, 32.66), deg(-26.22, 32.92), deg(-28.75, 32.20), deg(-29.40, 31.33),
            deg(-31.14, 30.06), deg(-32.77, 28.22), deg(-33.23, 27.46), deg(-33.67, 25.91),
            deg(-33.94, 25.78), deg(-33.86, 22.57), deg(-34.82, 19.62), deg(-34.14, 18.38),
            deg(-32.61, 17.93), deg(-32.43, 18.25), deg(-31.66, 18.22), deg(-27.09, 15.21),
            deg(-23.85, 14.41), deg(-22.11, 14.26), deg(-20.87, 13.35), deg(-19.05, 12.61),
            deg(-18.07, 11.79), deg(-15.79, 11.78), deg(-13.55, 12.50), deg(-12.04, 13.63),
            deg(-10.73, 13.69), deg(-9.17, 12.88), deg(-8.56, 13.24), deg(-5.04, 11.92),
            deg(-2.14, 9.41), deg(-1.11, 8.80), deg(1.01, 9.49), deg(1.16, 9.31),
            deg(3.07, 9.80), deg(3.73, 9.40), deg(3.90, 8.95), deg(4.77, 8.50),
            deg(4.24, 6.70), deg(4.26, 5.90), deg(5.61, 5.03), deg(6.27, 4.33),
            deg(6.14, 1.87), deg(4.71, -1.96), deg(5.17, -4.65), deg(4.99, -5.83),
            deg(4.34, -7.52), deg(4.83, -9.00), deg(6.79, -11.44), deg(7.26, -12.43),
            deg(7.80, -12.95), deg(8.90, -13.25), deg(10.21, -14.58), deg(10.88, -14.84),
            deg(11.52, -16.09), deg(12.17, -16.61), deg(13.15, -16.84), deg(13.60, -16.71),
            deg(14.37, -17.13), deg(14.73, -17.63), deg(14.92, -17.19), deg(16.14, -16.46),
            deg(16.67, -16.55), deg(18.11, -16.15), deg(20.09, -16.28), deg(21.00, -17.06),
            deg(21.89, -16.97), deg(22.68, -16.26), deg(23.72, -15.98), deg(24.52, -15.09),
            deg(26.25, -14.44), deg(26.62, -13.77), deg(27.64, -13.14), deg(28.04, -12.62),
            deg(28.15, -11.69), deg(29.93, -9.56), deg(31.18, -9.81), deg(32.56, -9.30),
            deg(33.24, -8.66), deg(34.11, -6.91), deg(35.76, -5.93), deg(35.76, -5.19),
            deg(35.33, -4.59), deg(35.17, -2.17), deg(35.71, -1.21), deg(35.89, -0.13),
            deg(36.61, 1.47), deg(36.87, 4.82), deg(36.72, 5.32), deg(37.11, 6.26),
            deg(36.95, 8.42), deg(37.35, 9.51), deg(37.23, 10.21), deg(36.72, 10.18),
            deg(37.09, 11.03), deg(36.90, 11.10), deg(36.41, 10.60), deg(35.95, 10.59),
            deg(35.70, 10.94), deg(34.83, 10.81), deg(34.33, 10.15), deg(33.79, 10.34),
            deg(33.77, 10.86), deg(33.29, 11.11), deg(33.14, 11.49), deg(32.27, 15.25),
            deg(31.38, 15.71), deg(30.27, 19.09), deg(30.99, 20.05), deg(31.75, 19.82),
            deg(32.24, 20.13), deg(32.84, 21.54), deg(32.64, 22.90), deg(32.19, 23.24),
            deg(31.90, 24.92), deg(31.57, 25.16), deg(31.59, 26.50), deg(30.87, 28.91),
            deg(31.47, 30.10), deg(31.56, 30.98), deg(31.43, 31.69), deg(30.93, 31.96),
            deg(31.26, 32.19), deg(30.97, 33.77), deg(31.55, 34.56), deg(32.83, 34.96),
            deg(34.64, 36.00), deg(35.41, 35.91), deg(35.82, 36.15), deg(36.28, 35.78),
            deg(36.65, 36.16), deg(36.80, 34.71), deg(36.22, 34.03), deg(36.11, 32.51),
            deg(36.64, 31.70), deg(36.68, 30.62), deg(36.26, 30.39), deg(36.14, 29.70),
            deg(36.68, 28.73), deg(36.66, 27.64), deg(37.65, 27.05), deg(38.21, 26.32),
            deg(38.99, 26.80), deg(39.46, 26.17), deg(40.42, 27.28), deg(40.46, 28.82),
            deg(41.22, 29.24), deg(41.09, 31.15), deg(41.74, 32.35), deg(42.02, 33.51),
            deg(42.04, 35.17), deg(40.95, 38.35), deg(41.01, 40.37), deg(41.54, 41.55),
            deg(41.96, 41.70), deg(42.65, 41.45), deg(45.24, 36.68), deg(45.40, 37.40),
            deg(46.24, 38.23), deg(46.64, 37.67), deg(47.04, 39.15), deg(47.26, 39.12),
            deg(46.65, 35.82), deg(46.27, 34.96), deg(45.65, 35.02), deg(45.41, 35.51),
            deg(45.47, 36.53), deg(45.11, 36.33), deg(44.94, 35.24), deg(44.36, 33.88),
            deg(44.56, 33.33), deg(45.03, 33.55), deg(45.33, 32.45), deg(45.85, 33.59),
            deg(46.08, 33.30), deg(46.33, 31.74), deg(46.71, 31.68), deg(46.58, 30.75),
            deg(45.29, 29.60), deg(45.04, 29.63), deg(44.91, 28.84), deg(43.71, 28.56),
            deg(43.29, 28.04), deg(42.58, 27.67), deg(41.62, 28.12), deg(41.30, 28.99),
            deg(41.05, 28.81), deg(41.00, 27.62), deg(40.15, 26.36), deg(40.82, 26.06),
            deg(40.95, 24.93), deg(40.69, 23.71), deg(40.13, 24.41), deg(39.96, 23.90),
            deg(39.96, 23.34), deg(40.48, 22.81), deg(40.26, 22.63), deg(39.66, 22.85),
            deg(39.19, 23.35), deg(38.97, 22.97), deg(38.22, 24.03), deg(37.66, 24.04),
            deg(37.92, 23.12), deg(37.41, 23.41), deg(37.31, 22.78), deg(36.42, 23.15),
            deg(36.41, 22.49), deg(36.85, 21.67), deg(38.31, 21.12), deg(39.92, 19.96),
            deg(40.25, 19.41), deg(41.72, 19.54), deg(42.28, 18.88), deg(43.51, 16.02),
            deg(44.24, 15.17), deg(44.32, 15.38), deg(44.74, 14.92), deg(45.08, 14.90),
            deg(45.23, 14.26), deg(44.80, 13.95), deg(45.14, 13.66), deg(45.48, 13.68),
            deg(45.59, 13.94), deg(45.74, 13.14), deg(45.38, 12.33), deg(44.60, 12.26),
            deg(44.09, 12.59), deg(43.59, 13.53), deg(42.76, 14.03), deg(41.96, 15.14),
            deg(41.96, 15.93), deg(41.74, 16.17), deg(41.54, 15.89), deg(40.88, 17.52),
            deg(40.17, 18.48), deg(39.81, 18.29), deg(40.28, 17.74), deg(40.44, 16.87),
            deg(39.80, 16.45), deg(39.42, 17.17), deg(38.90, 17.05), deg(38.84, 16.64),
            deg(37.99, 16.10), deg(37.91, 15.68), deg(38.96, 16.11), deg(40.05, 15.41),
            deg(41.19, 13.63), deg(41.25, 12.89), deg(41.70, 12.11), deg(42.93, 10.51),
            deg(43.92, 10.20), deg(44.37, 8.89), deg(43.13, 6.53), deg(43.40, 4.56),
            deg(43.08, 3.10), deg(41.89, 3.04), deg(41.23, 2.09), deg(41.01, 0.81),
            deg(39.31, -0.28), deg(38.74, 0.11), deg(38.29, -0.47), deg(37.64, -0.68),
            deg(37.44, -1.44), deg(36.67, -2.15), deg(36.68, -4.37), deg(35.95, -5.38),
            deg(36.03, -5.87), deg(36.94, -6.52), deg(37.10, -7.45), deg(36.84, -7.86),
            deg(36.87, -8.90), deg(38.27, -8.84), deg(38.36, -9.29), deg(38.74, -9.53),
            deg(39.39, -9.45), deg(39.76, -9.05), deg(40.76, -8.77), deg(42.59, -8.98),
            deg(43.03, -9.39), deg(43.75, -7.98), deg(43.40, -4.35), deg(43.42, -1.90),
            deg(44.02, -1.38), deg(46.01, -1.19), deg(47.57, -2.96), deg(47.96, -4.49),
            deg(48.68, -4.59), deg(48.90, -3.30), deg(48.64, -1.62), deg(49.78, -1.93),
            deg(49.35, -0.99), deg(50.13, 1.34), deg(50.95, 1.64), deg(51.62, 3.83),
            deg(53.09, 4.71), deg(53.51, 6.07), deg(53.48, 6.91), deg(53.69, 7.10),
            deg(53.75, 7.94), deg(53.53, 8.12), deg(54.02, 8.80), deg(55.52, 8.12),
            deg(56.54, 8.09), deg(57.11, 8.54), deg(57.17, 9.42), deg(57.73, 10.58),
            deg(57.22, 10.55), deg(56.89, 10.25), deg(56.61, 10.37), deg(56.46, 10.91),
            deg(55.47, 9.65), deg(54.60, 9.94), deg(54.36, 10.95), deg(54.01, 10.94),
            deg(54.47, 12.52), deg(53.76, 14.12), deg(54.85, 17.62), deg(54.68, 18.62),
            deg(54.44, 18.70), deg(54.43, 19.66), deg(54.87, 19.89), deg(55.19, 21.27),
            deg(56.78, 21.09), deg(57.41, 21.58), deg(57.75, 22.52), deg(57.01, 23.32),
            deg(57.03, 24.12), deg(58.38, 24.43), deg(58.26, 24.06), deg(58.61, 23.43),
            deg(59.19, 23.34), deg(59.61, 25.86), deg(59.48, 27.98), deg(60.03, 29.12),
            deg(60.50, 28.07), deg(59.85, 22.87), deg(60.39, 22.29), deg(60.72, 21.32),
            deg(61.71, 21.54), deg(62.61, 21.06), deg(63.19, 21.54), deg(63.82, 22.44),
            deg(65.11, 25.40), deg(65.53, 25.29), deg(66.01, 23.90), deg(65.72, 22.18),
            deg(65.03, 21.21), deg(64.41, 21.37), deg(62.75, 17.85), deg(61.34, 17.12),
            deg(60.08, 18.79), deg(58.95, 17.87), deg(58.72, 16.83), deg(57.04, 16.45),
            deg(56.10, 15.88), deg(56.20, 14.67), deg(55.41, 14.10), deg(55.36, 12.94),
            deg(56.31, 12.63), deg(58.86, 11.03), deg(59.47, 10.36), deg(58.31, 8.38),
            deg(58.08, 7.05), deg(58.59, 5.67), deg(61.97, 4.99), deg(62.61, 5.91),
            deg(63.45, 8.55), deg(64.49, 10.53), deg(67.81, 14.76), deg(69.82, 19.18),
            deg(70.26, 21.38), deg(70.20, 23.02), deg(71.03, 24.55), deg(71.19, 28.17),
            deg(70.45, 31.29), deg(70.19, 30.01), deg(69.56, 31.10), deg(69.91, 32.13),
            deg(69.30, 33.78), deg(69.06, 36.51), deg(67.93, 40.29), deg(67.46, 41.06),
            deg(66.79, 41.13), deg(66.27, 40.02), deg(66.00, 38.38), deg(66.76, 33.92),
            deg(66.63, 33.18), deg(65.90, 34.81), deg(64.41, 34.94), deg(63.85, 37.01),
            deg(64.33, 37.14), deg(64.78, 36.52), deg(65.14, 37.18), deg(64.52, 39.59),
            deg(64.76, 40.44), deg(65.50, 39.76), deg(66.48, 42.09), deg(66.07, 43.95),
            deg(66.76, 44.53), deg(67.35, 43.70), deg(67.95, 44.19), deg(68.57, 43.45),
            deg(68.25, 46.25), deg(67.69, 46.82), deg(67.57, 45.56), deg(67.01, 45.56),
            deg(66.67, 46.35), deg(66.88, 47.89), deg(67.52, 48.14), deg(68.86, 53.72),
            deg(68.81, 54.47), deg(68.20, 53.49), deg(68.10, 54.73), deg(68.44, 55.44),
            deg(68.47, 57.32), deg(68.88, 58.80), deg(68.28, 59.94), deg(68.94, 61.08),
            deg(69.52, 60.03), deg(69.85, 60.55), deg(69.55, 63.50), deg(68.09, 68.51),
            deg(68.62, 69.18), deg(69.14, 68.16), deg(69.36, 68.14), deg(69.45, 66.93),
            deg(69.93, 67.26), deg(71.03, 66.69), deg(71.93, 68.54), deg(72.84, 69.20),
            deg(73.04, 69.94), deg(72.78, 72.59), deg(72.22, 72.80), deg(71.41, 71.85),
            deg(71.09, 72.47), deg(70.39, 72.79), deg(69.02, 72.56), deg(68.41, 73.67),
            deg(67.74, 73.24), deg(66.32, 71.28), deg(66.17, 72.42), deg(66.53, 72.82),
            deg(66.79, 73.92), deg(67.28, 74.19), deg(67.76, 75.05), deg(68.33, 74.47),
            deg(68.99, 74.94), deg(69.07, 73.84), deg(69.63, 73.60), deg(70.63, 74.40),
            deg(71.45, 73.10), deg(72.12, 74.89), deg(72.83, 74.66), deg(72.86, 75.16),
            deg(72.30, 75.68), deg(71.34, 75.29), deg(71.15, 76.36), deg(71.87, 75.90),
            deg(72.27, 77.58), deg(72.32, 79.65), deg(71.75, 81.50), deg(72.58, 80.61),
            deg(73.65, 80.51), deg(73.94, 86.82), deg(74.46, 86.01), deg(75.12, 87.17),
            deg(75.14, 88.32), deg(75.64, 90.26), deg(75.77, 92.90), deg(76.05, 93.23),
            deg(76.14, 95.86), deg(75.92, 96.68), deg(76.45, 98.92), deg(76.43, 100.76),
            deg(76.86, 101.04), deg(77.29, 101.99), deg(77.70, 104.35), deg(77.37, 106.07),
            deg(77.13, 104.71), deg(76.97, 106.97)
        ),
        // Loop 108 (6 points)
        listOf(
            deg(77.52, -93.84), deg(77.56, -96.17), deg(77.83, -96.44), deg(77.82, -94.42),
            deg(77.63, -93.72), deg(77.52, -93.84)
        ),
        // Loop 109 (6 points)
        listOf(
            deg(77.70, -110.19), deg(77.41, -112.05), deg(77.73, -113.53), deg(78.05, -112.72),
            deg(78.00, -109.85), deg(77.70, -110.19)
        ),
        // Loop 110 (8 points)
        listOf(
            deg(77.85, 24.72), deg(77.44, 22.49), deg(77.68, 20.73), deg(77.94, 21.42),
            deg(78.25, 20.81), deg(78.45, 22.88), deg(78.08, 23.28), deg(77.85, 24.72)
        ),
        // Loop 111 (8 points)
        listOf(
            deg(78.06, -95.83), deg(77.85, -97.31), deg(78.08, -98.12), deg(78.46, -98.55),
            deg(78.87, -98.63), deg(78.77, -96.75), deg(78.42, -95.56), deg(78.06, -95.83)
        ),
        // Loop 112 (9 points)
        listOf(
            deg(78.32, -100.06), deg(77.91, -99.67), deg(78.34, -102.95), deg(78.38, -105.18),
            deg(78.68, -104.21), deg(78.92, -105.42), deg(79.30, -105.49), deg(78.80, -100.83),
            deg(78.32, -100.06)
        ),
        // Loop 113 (6 points)
        listOf(
            deg(78.31, 105.08), deg(77.92, 99.44), deg(79.23, 101.26), deg(79.35, 102.09),
            deg(78.71, 105.37), deg(78.31, 105.08)
        ),
        // Loop 114 (18 points)
        listOf(
            deg(79.70, 18.25), deg(78.96, 21.54), deg(78.56, 19.03), deg(77.83, 18.47),
            deg(77.64, 17.59), deg(76.81, 17.12), deg(76.77, 15.91), deg(77.38, 13.76),
            deg(77.74, 14.67), deg(78.02, 13.17), deg(78.87, 11.22), deg(79.65, 10.44),
            deg(80.01, 13.17), deg(79.66, 13.72), deg(79.67, 15.14), deg(80.02, 15.52),
            deg(80.05, 16.99), deg(79.70, 18.25)
        ),
        // Loop 115 (12 points)
        listOf(
            deg(80.41, 25.45), deg(80.06, 27.41), deg(79.52, 25.92), deg(79.40, 23.02),
            deg(79.57, 20.08), deg(79.84, 19.90), deg(79.86, 18.46), deg(80.32, 17.37),
            deg(80.60, 20.46), deg(80.36, 21.91), deg(80.66, 22.92), deg(80.41, 25.45)
        ),
        // Loop 116 (10 points)
        listOf(
            deg(80.55, 51.14), deg(80.01, 47.59), deg(80.25, 46.50), deg(80.56, 47.07),
            deg(80.59, 44.85), deg(80.78, 48.32), deg(80.51, 48.52), deg(80.92, 50.04),
            deg(80.70, 51.52), deg(80.55, 51.14)
        ),
        // Loop 117 (11 points)
        listOf(
            deg(78.88, 99.94), deg(78.76, 97.76), deg(79.04, 94.97), deg(79.43, 93.31),
            deg(80.14, 92.55), deg(80.34, 91.18), deg(81.02, 93.78), deg(81.25, 95.94),
            deg(80.75, 97.88), deg(79.78, 100.19), deg(78.88, 99.94)
        ),
        // Loop 118 (18 points)
        listOf(
            deg(79.66, -87.02), deg(79.34, -85.81), deg(78.29, -89.04), deg(78.22, -90.80),
            deg(78.34, -92.88), deg(78.75, -93.95), deg(79.11, -93.94), deg(79.38, -93.15),
            deg(79.37, -94.97), deg(79.71, -96.08), deg(80.16, -96.71), deg(80.91, -95.32),
            deg(80.98, -94.30), deg(81.21, -94.74), deg(81.26, -92.41), deg(80.72, -91.13),
            deg(80.32, -87.81), deg(79.66, -87.02)
        ),
        // Loop 119 (42 points)
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
            deg(83.13, -79.31), deg(83.11, -68.50)
        ),
        // Loop 120 (103 points)
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
            deg(83.18, -39.90), deg(83.55, -38.62), deg(83.52, -27.10)
        )
    )
}

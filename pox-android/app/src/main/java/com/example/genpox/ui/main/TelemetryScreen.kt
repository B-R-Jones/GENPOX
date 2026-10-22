package com.example.genpox.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.genpox.theme.CyberBorder
import com.example.genpox.theme.CyberGreen
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.theme.CyberPanel
import com.example.genpox.theme.CyberRed
import com.example.genpox.ui.telemetry.*

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
                    "Drives periodic fluctuations in environmental density drag.",
                ),
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
                    "Increases nucleotide generation yields during Full Moon peaks.",
                ),
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
                    "Influences automated mutation frequencies.",
                ),
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
                    "Decides weekly nucleotide reactor suppression days.",
                ),
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
                    "Triggers temporal combat benefits when coupling exceeds 82%.",
                ),
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
                    "Adds Gaussian bumps near resonance frequencies.",
                ),
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
                    "Incorporates dynamic lunar shifts into well drag.",
                ),
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
                    "Establishes combat vitality, time, and harmonic trigger conditions.",
                ),
            ),
        )
    }

    var selectedItem by remember { mutableStateOf<TelemetryMetric?>(null) }
    var isGlobeExpanded by remember { mutableStateOf(false) }
    val heatwaveCells by viewModel.heatwaveCells.collectAsStateWithLifecycle(initialValue = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (selectedItem == null) {
                Text(
                    text = "TELEMETRY METRIC LIBRARY",
                    color = CyberGreenDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                val categories = remember(metrics) { metrics.groupBy { it.category } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    categories.forEach { (category, categoryMetrics) ->
                        item {
                            Text(
                                text = category,
                                color = Color(0xFFF97316),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Default,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
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
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = metric.title,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Default,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = metric.summary,
                                        color = CyberGreenDim,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Default,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "FORMULA: " + metric.formula.lineSequence().first(),
                                            color = factionColorForCategory(category).copy(alpha = 0.8f),
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                        Text(
                                            text = "EXPAND 🗃",
                                            color = CyberGreen,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Default,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(55.dp))
                    }
                }
            } else {
                TelemetryDetailScreen(
                    metric = selectedItem!!,
                    onBack = {
                        viewModel.synthManager.playBeep(450f, 0.05f, "sine")
                        selectedItem = null
                    },
                    interactiveWidgetContent = { metricId ->
                        InteractiveWidgetRouter(id = metricId)
                    },
                )
            }
        }

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
                contentAlignment = Alignment.Center,
            ) {
                WireframeGlobe(
                    modifier = Modifier.fillMaxSize(),
                    isMini = true,
                    rotationSpeedMs = 15000,
                    activeCells = heatwaveCells,
                    viewModel = viewModel,
                )
            }
        }

        if (isGlobeExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable {
                        viewModel.synthManager.playBeep(400f, 0.08f, "sine")
                        isGlobeExpanded = false
                    },
            ) {
                WireframeGlobe(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    isMini = false,
                    rotationSpeedMs = 30000,
                    activeCells = heatwaveCells,
                    viewModel = viewModel,
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(CyberRed, RoundedCornerShape(2.dp))
                        .clickable {
                            viewModel.synthManager.playBeep(400f, 0.08f, "sine")
                            isGlobeExpanded = false
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "CLOSE",
                        color = Color.Black,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                    )
                }
            }
        }
    }
}

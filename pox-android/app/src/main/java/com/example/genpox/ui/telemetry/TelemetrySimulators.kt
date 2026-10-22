package com.example.genpox.ui.telemetry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.theme.CyberGreen
import com.example.genpox.ui.components.PoxSlider
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

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
    var moonAge by remember { mutableStateOf(14.77f) }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "SLIDE LUNAR AGE (DAYS):", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%.2f d [%s]", moonAge, phaseName.uppercase()),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        PoxSlider(
            value = moonAge,
            onValueChange = { moonAge = it },
            valueRange = 0f..29.53f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray,
        )

        Canvas(modifier = Modifier.size(60.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            drawCircle(color = Color(0xFF0F172A), radius = radius)

            val angle = (2.0 * Math.PI * moonAge.toDouble()) / 29.53059
            val percentLight = (1.0 + cos(angle - Math.PI)) / 2.0

            if (moonAge < 14.77f) {
                drawCircle(color = Color(0xFFFEF08A), radius = radius)
                val shadowWidth = (1.0 - percentLight) * 2.0 * radius
                if (moonAge < 7.38f) {
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(0f, 0f),
                        size = Size(radius, size.height),
                    )
                    val curveW = (radius - shadowWidth).toFloat()
                    drawOval(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(radius, 0f),
                        size = Size(abs(curveW), size.height),
                    )
                } else {
                    drawOval(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(0f, 0f),
                        size = Size(shadowWidth.toFloat(), size.height),
                    )
                }
            } else {
                drawCircle(color = Color(0xFFFEF08A), radius = radius)
                val shadowWidth = (1.0 - percentLight) * 2.0 * radius
                drawOval(
                    color = Color(0xFF0F172A),
                    topLeft = Offset((radius - shadowWidth).toFloat(), 0f),
                    size = Size(shadowWidth.toFloat(), size.height),
                )
            }
        }
    }
}

@Composable
fun MoonModifierGraph() {
    var testAge by remember { mutableStateOf(14.77f) }

    val primaryMultiplier = remember(testAge) {
        val angle = (2.0 * Math.PI * testAge.toDouble()) / 29.53059
        (1.0 + 0.12 * sin(angle)).toFloat()
    }
    val secondaryMultiplier = remember(testAge) {
        val angle = (2.0 * Math.PI * testAge.toDouble()) / 29.53059
        (1.0 + 0.62 * sin(angle)).toFloat()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "PRIMARY / SECONDARY MULTIPLIERS:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%.2fx / %.2fx", primaryMultiplier, secondaryMultiplier),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        PoxSlider(
            value = testAge,
            onValueChange = { testAge = it },
            valueRange = 0f..29.53f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            val w = size.width
            val h = size.height

            drawLine(Color.DarkGray, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1f)

            val primaryPath = Path()
            val secondaryPath = Path()

            for (i in 0..100) {
                val age = (i.toFloat() / 100f) * 29.53059f
                val angle = (2.0 * Math.PI * age.toDouble()) / 29.53059
                val pMult = 1.0 + 0.12 * sin(angle)
                val sMult = 1.0 + 0.62 * sin(angle)

                val x = (i.toFloat() / 100f) * w
                val pY = h / 2 - ((pMult - 1.0) * (h / 2) * 1.2).toFloat()
                val sY = h / 2 - ((sMult - 1.0) * (h / 2) * 1.2).toFloat()

                if (i == 0) {
                    primaryPath.moveTo(x, pY)
                    secondaryPath.moveTo(x, sY)
                } else {
                    primaryPath.lineTo(x, pY)
                    secondaryPath.lineTo(x, sY)
                }
            }

            drawPath(secondaryPath, Color(0xFFA855F7), style = Stroke(width = 1.5.dp.toPx()))
            drawPath(primaryPath, CyberGreen, style = Stroke(width = 1.5.dp.toPx()))

            val curX = (testAge / 29.53f) * w
            drawLine(Color.Red, Offset(curX, 0f), Offset(curX, h), strokeWidth = 1.5.dp.toPx())
        }
    }
}

@Composable
fun MarkovTransitionMatrix() {
    val nucleotides = listOf("A", "G", "T", "C")
    var selectedBase by remember { mutableStateOf("A") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "SELECT CURRENT NUCLEOTIDE:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                nucleotides.forEach { base ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, if (selectedBase == base) CyberGreen else Color.DarkGray, RoundedCornerShape(2.dp))
                            .background(if (selectedBase == base) CyberGreen.copy(alpha = 0.2f) else Color.Black)
                            .clickable { selectedBase = base }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = base,
                            color = if (selectedBase == base) CyberGreen else Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }

        val probs = when (selectedBase) {
            "A" -> listOf(0.40f, 0.30f, 0.15f, 0.15f)
            "G" -> listOf(0.20f, 0.45f, 0.25f, 0.10f)
            "T" -> listOf(0.15f, 0.15f, 0.50f, 0.20f)
            else -> listOf(0.10f, 0.20f, 0.30f, 0.40f)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            nucleotides.forEachIndexed { idx, target ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "$selectedBase ➔ $target:", color = Color.LightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    LinearProgressIndicator(
                        progress = { probs[idx] },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(8.dp),
                        color = when (target) {
                            "A" -> Color(0xFF38BDF8)
                            "G" -> Color(0xFF34D399)
                            "T" -> Color(0xFFFB7185)
                            else -> Color(0xFFFBBF24)
                        },
                        trackColor = Color.DarkGray,
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f%%", probs[idx] * 100),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
fun DeterministicHashFlowchart() {
    var lat by remember { mutableStateOf(37.422f) }
    var lng by remember { mutableStateOf(-122.084f) }

    val seed = remember(lat, lng) {
        val latInt = (lat * 1000).toInt()
        val lngInt = (lng * 1000).toInt()
        (latInt xor (lngInt shl 16)).toLong() and 0xFFFFFFFFL
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "LAT / LNG INPUT:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%.3f, %.3f", lat, lng),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        PoxSlider(
            value = lat,
            onValueChange = { lat = it },
            valueRange = 37.0f..38.0f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGreen.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                .background(Color(0xFF030303))
                .padding(6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "DETERMINISTIC SEED HASH:", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = String.format(Locale.US, "0x%08X", seed),
                    color = CyberGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text(text = "PSEUDO-RANDOM NUMBER GENERATOR STEP ➔ ANOMALY SPECIES DETERMINED", color = Color.LightGray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun SpectrumCouplingSineGraph() {
    var waveFrequency by remember { mutableStateOf(1.0f) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "WAVE FREQUENCY HARMONIC:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%.2f Hz", waveFrequency),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        PoxSlider(
            value = waveFrequency,
            onValueChange = { waveFrequency = it },
            valueRange = 0.5f..3.0f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            val w = size.width
            val h = size.height

            val sinePath = Path()
            for (i in 0..100) {
                val x = (i.toFloat() / 100f) * w
                val angle = (i.toFloat() / 100f) * 2.0 * Math.PI * waveFrequency
                val y = h / 2 - (sin(angle) * (h / 2 * 0.8)).toFloat()

                if (i == 0) {
                    sinePath.moveTo(x, y)
                } else {
                    sinePath.lineTo(x, y)
                }
            }

            drawPath(sinePath, Color(0xFF38BDF8), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun LogarithmicSuccessGraph() {
    var totalGenes by remember { mutableStateOf(120000f) }
    var coupling by remember { mutableStateOf(80f) }

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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "CUMULATIVE CONSUMED BASES:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = "${totalGenes.toInt()} BASES",
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        PoxSlider(
            value = totalGenes,
            onValueChange = { totalGenes = it },
            valueRange = 10000f..250000f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "COUPLING AMPLITUDE:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = "${coupling.toInt()}%",
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        PoxSlider(
            value = coupling,
            onValueChange = { coupling = it },
            valueRange = 60f..100f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray,
        )

        Text(
            text = String.format(Locale.US, "CALCULATED SUCCESS RATE: %.2f%%", successChance),
            color = Color(0xFFFBBF24),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
        ) {
            val w = size.width
            val h = size.height

            drawLine(Color(0xFF111111), Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), strokeWidth = 1f)
            drawLine(Color(0xFF111111), Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 1f)
            drawLine(Color(0xFF111111), Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), strokeWidth = 1f)

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

            val currentPctX = (totalGenes - 10000f) / (250000f - 10000f)
            val dotX = currentPctX * w
            val dotY = h - (successChance.toFloat() / 100f * h)
            drawCircle(color = Color.Red, radius = 4.dp.toPx(), center = Offset(dotX, dotY))
        }
    }
}

@Composable
fun DensityDragBarChart() {
    var baseDensity by remember { mutableStateOf(0.15f) }
    var lunarShift by remember { mutableStateOf(-0.05f) }
    var shieldActive by remember { mutableStateOf(false) }

    val effectiveDensity = (baseDensity + lunarShift).coerceIn(-0.33f, 0.33f)
    val mitigatedDensity = if (shieldActive && effectiveDensity > 0f) 0f else effectiveDensity
    val velocityMultiplier = 1.0f - mitigatedDensity

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "BASE ANOMALY DENSITY:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%+.1f%%", baseDensity * 100),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        PoxSlider(
            value = baseDensity,
            onValueChange = { baseDensity = it },
            valueRange = -0.33f..0.33f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "LUNAR INFLUENCE SHIFT:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Text(
                text = String.format(Locale.US, "%+.1f%%", lunarShift * 100),
                color = CyberGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        PoxSlider(
            value = lunarShift,
            onValueChange = { lunarShift = it },
            valueRange = -0.06f..0.06f,
            activeColor = CyberGreen,
            inactiveColor = Color.DarkGray,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "COHERENCE SHIELD ACTIVE:", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Default)
            Checkbox(
                checked = shieldActive,
                onCheckedChange = { shieldActive = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberGreen,
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.Black,
                ),
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = String.format(Locale.US, "TRANSIT VELOCITY MULTIPLIER: %.3fx", velocityMultiplier),
            color = if (velocityMultiplier >= 1.0f) CyberGreen else Color(0xFFEF4444),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            val w = size.width
            val h = size.height

            val baselineX = w * 0.65f
            drawLine(Color.Gray, Offset(baselineX, 0f), Offset(baselineX, h), strokeWidth = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))

            val barWidth = baselineX * velocityMultiplier
            drawRect(
                color = if (velocityMultiplier >= 1.0f) CyberGreen.copy(alpha = 0.8f) else Color(0xFFEF4444).copy(alpha = 0.8f),
                topLeft = Offset(0f, h * 0.25f),
                size = Size(barWidth, h * 0.5f),
            )

            drawRect(
                color = Color.DarkGray,
                topLeft = Offset(0f, h * 0.25f),
                size = Size(w, h * 0.5f),
                style = Stroke(width = 1f),
            )
        }
    }
}

@Composable
fun PhenotypeGeneDecoder() {
    var customSequence by remember { mutableStateOf("X?W!$@#&") }

    val parsedBenefits = remember(customSequence) {
        val benefits = mutableListOf<String>()
        val characters = "XZYW?!$%&@#"
        val hasAnomalousChar = customSequence.any { characters.contains(it) }

        if (customSequence.length == 8 && hasAnomalousChar) {
            val chunk = customSequence.uppercase()
            val val1 = chunk[0].code + chunk[1].code
            val val2 = chunk[2].code + chunk[3].code
            val val3 = chunk[4].code + chunk[5].code

            val prefix = when (val1 % 5) {
                0 -> "VORTEX"
                1 -> "QUANTUM"
                2 -> "HYDRA"
                3 -> "NEURAL"
                else -> "COHERENCE"
            }
            val suffix = when (val2 % 5) {
                0 -> "SHIELD"
                1 -> "BURST"
                2 -> "REGENERATOR"
                3 -> "AMPLIFIER"
                else -> "RESONATOR"
            }
            val baseDesc = when (val3 % 6) {
                0 -> "+30% Damage Mitigation against elemental hazards."
                1 -> "+45% Critical Strike Chance on initial strike."
                2 -> "Restores 5% Vitality per turn during active phase."
                3 -> "+50% Synthesis speed boost when salt > 0.08M."
                4 -> "Immunity to Environmental Hazard Tension collapse."
                else -> "+15% Overall combat stats across all domains."
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "INPUT 8-CHAR ANOMALOUS GENE:",
            color = Color.White,
            fontSize = 8.sp,
            fontFamily = FontFamily.Default,
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
                color = Color(0xFFA855F7),
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA855F7),
                unfocusedBorderColor = Color.DarkGray,
            ),
        )

        Spacer(modifier = Modifier.height(2.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030303)),
            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(2.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                parsedBenefits.forEach { line ->
                    Text(
                        text = line,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 10.sp,
                    )
                }
            }
        }
    }
}

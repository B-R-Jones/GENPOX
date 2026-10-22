package com.example.genpox.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

object WaveMath {
    const val LUNAR_MONTH_DAYS = 29.53059

    // Standard Reference New Moon: May 17, 2026, 01:54:00 UTC
    private val REFERENCE_NEW_MOON_MS: Long by lazy {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse("2026-05-17T01:54:00Z")?.time ?: 1778982840000L
    }

    data class PlanetOrbitalElements(
        val name: String,
        val semiMajorAxis: Double,
        val eccentricity: Double,
        val orbitalPeriod: Double,
        val longitudeOfPerihelion: Double,
        val siderealRotationPeriod: Double,
    )

    val planetElements = mapOf(
        "MERCURY" to PlanetOrbitalElements("Mercury", 0.3871, 0.2056, 87.969, 77.456, 58.646),
        "VENUS" to PlanetOrbitalElements("Venus", 0.7233, 0.0068, 224.701, 131.532, -243.025),
        "EARTH" to PlanetOrbitalElements("Earth", 1.0000, 0.0167, 365.256, 102.947, 0.9973),
        "MARS" to PlanetOrbitalElements("Mars", 1.5237, 0.0934, 686.980, 336.041, 1.0260),
        "JUPITER" to PlanetOrbitalElements("Jupiter", 5.2028, 0.0484, 4332.589, 14.754, 0.4135),
        "SATURN" to PlanetOrbitalElements("Saturn", 9.5388, 0.0541, 10759.22, 92.431, 0.4440),
        "URANUS" to PlanetOrbitalElements("Uranus", 19.1914, 0.0473, 30685.4, 170.964, -0.7183),
        "NEPTUNE" to PlanetOrbitalElements("Neptune", 30.0611, 0.0086, 60189.0, 44.971, 0.6713),
    )

    enum class AlignmentState {
        CONJUNCTION,
        OPPOSITION,
        QUADRATURE,
        NEUTRAL,
    }

    fun getHeliocentricPosition(planetName: String, timeMs: Long): Pair<Double, Double> {
        val elem = planetElements[planetName.uppercase()] ?: return Pair(0.0, 0.0)
        val tDays = timeMs.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        val mAngle = (2.0 * Math.PI * (tDays / elem.orbitalPeriod)) % (2.0 * Math.PI)
        var eAngle = mAngle
        val e = elem.eccentricity
        for (i in 0 until 5) {
            val delta = (eAngle - e * kotlin.math.sin(eAngle) - mAngle) / (1.0 - e * kotlin.math.cos(eAngle))
            eAngle -= delta
        }
        val xOrb = elem.semiMajorAxis * (kotlin.math.cos(eAngle) - e)
        val yOrb = elem.semiMajorAxis * kotlin.math.sqrt(1.0 - e * e) * kotlin.math.sin(eAngle)
        val periRad = Math.toRadians(elem.longitudeOfPerihelion)
        val xEcl = xOrb * kotlin.math.cos(periRad) - yOrb * kotlin.math.sin(periRad)
        val yEcl = xOrb * kotlin.math.sin(periRad) + yOrb * kotlin.math.cos(periRad)
        return Pair(xEcl, yEcl)
    }

    fun getPairAlignmentState(planet1: String, planet2: String, timeMs: Long): AlignmentState {
        val (xE, yE) = getHeliocentricPosition("EARTH", timeMs)
        val lambda1 = if (planet1.uppercase() == "EARTH") {
            kotlin.math.atan2(-yE, -xE) // Sun Proxy
        } else {
            val (x1, y1) = getHeliocentricPosition(planet1, timeMs)
            kotlin.math.atan2(y1 - yE, x1 - xE)
        }
        val lambda2 = if (planet2.uppercase() == "EARTH") {
            kotlin.math.atan2(-yE, -xE) // Sun Proxy
        } else {
            val (x2, y2) = getHeliocentricPosition(planet2, timeMs)
            kotlin.math.atan2(y2 - yE, x2 - xE)
        }
        var diff = kotlin.math.abs(lambda1 - lambda2) % (2.0 * Math.PI)
        if (diff > Math.PI) {
            diff = 2.0 * Math.PI - diff
        }
        val diffDeg = Math.toDegrees(diff)
        return when {
            diffDeg < 30.0 -> AlignmentState.CONJUNCTION
            diffDeg >= 150.0 -> AlignmentState.OPPOSITION
            diffDeg in 75.0..105.0 -> AlignmentState.QUADRATURE
            else -> AlignmentState.NEUTRAL
        }
    }

    @Volatile
    var isTestMode: Boolean = false

    @Volatile
    var timeScale: Double = 1.0

    @Volatile
    var baseOffsetMs: Long = 0L

    @Volatile
    var manualOffsetMs: Long = 0L

    @Volatile
    var appStartTime: Long = System.currentTimeMillis()

    fun getAdjustedTime(realTimeMs: Long = System.currentTimeMillis()): Long {
        if (isTestMode) return realTimeMs
        val elapsed = realTimeMs - appStartTime
        return appStartTime + baseOffsetMs + manualOffsetMs + (elapsed * timeScale).toLong()
    }

    fun setScale(newScale: Double) {
        val now = System.currentTimeMillis()
        val currentAdjustedWithoutManual = now + baseOffsetMs + ((now - appStartTime) * timeScale).toLong()
        appStartTime = now
        baseOffsetMs = currentAdjustedWithoutManual - now
        timeScale = newScale
    }

    fun setManualOffset(offsetMs: Long) {
        val now = System.currentTimeMillis()
        val currentAdjustedWithoutManual = now + baseOffsetMs + ((now - appStartTime) * timeScale).toLong()
        appStartTime = now
        baseOffsetMs = currentAdjustedWithoutManual - now
        manualOffsetMs = offsetMs
    }

    fun getBaseAlignmentState(base: Char, timeMs: Long): AlignmentState {
        if (isTestMode) return AlignmentState.NEUTRAL
        return when (base.uppercaseChar()) {
            'A' -> getPairAlignmentState("MERCURY", "VENUS", timeMs)
            'G' -> getPairAlignmentState("EARTH", "MARS", timeMs)
            'T' -> getPairAlignmentState("JUPITER", "SATURN", timeMs)
            'C' -> getPairAlignmentState("URANUS", "NEPTUNE", timeMs)
            else -> AlignmentState.NEUTRAL
        }
    }

    data class WaveConfig(
        val pair: String,
        val primary: String,
        val secondary: String,
        val isSuppressed: Boolean,
        val lunarAge: Double,
        val isLunarInfluence: Boolean,
        val phaseName: String,
        val isNewMoon: Boolean,
        val isFullMoon: Boolean,
        val primaryMultiplier: Double,
        val secondaryMultiplier: Double,
    )

    data class AnomalyChance(
        val baseChance: Double,
        val peakBoost: Double,
        val harmonicModifier: Double,
        val finalChance: Double,
    )

    data class AnomalousBenefit(
        val id: String,
        val name: String,
        val description: String,
        val effectType: String,
        val magnitude: Double = 0.0,
        val chance: Double = 0.0,
        val triggerIndex: Int = 0,
        val triggerDesc: String = "",
    )

    fun getDeterministicHash(dateStr: String): Long {
        var hash = 0L
        for (i in 0 until dateStr.length) {
            hash = (hash shl 5) - hash + dateStr[i].code.toLong()
            hash = hash and 0xFFFFFFFFL // mimic Javascript 32-bit integer conversion
        }
        // Mimic Math.abs on JavaScript 32-bit signed int
        val signedHash = hash.toInt()
        return abs(signedHash.toLong())
    }

    fun getDailyWaveConfig(dateMs: Long): WaveConfig {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateStr = sdf.format(Date(dateMs))
        val hash = getDeterministicHash(dateStr)

        val diffDays = (dateMs.toDouble() - REFERENCE_NEW_MOON_MS.toDouble()) / (1000.0 * 60.0 * 60.0 * 24.0)
        val lunarAge = ((diffDays % LUNAR_MONTH_DAYS) + LUNAR_MONTH_DAYS) % LUNAR_MONTH_DAYS

        val isLunarInfluence = lunarAge < 8.0

        var phaseName = "Waxing Crescent"
        var isNewMoon = false
        var isFullMoon = false

        when {
            lunarAge < 1.0 || lunarAge > 28.53059 -> {
                phaseName = "New Moon"
                isNewMoon = true
            }
            lunarAge >= 1.0 && lunarAge < 6.38 -> phaseName = "Waxing Crescent"
            lunarAge >= 6.38 && lunarAge < 8.38 -> phaseName = "First Quarter"
            lunarAge >= 8.38 && lunarAge < 13.77 -> phaseName = "Waxing Gibbous"
            lunarAge >= 13.77 && lunarAge < 15.77 -> {
                phaseName = "Full Moon"
                isFullMoon = true
            }
            lunarAge >= 15.77 && lunarAge < 21.15 -> phaseName = "Waning Gibbous"
            lunarAge >= 21.15 && lunarAge < 23.15 -> phaseName = "Third Quarter"
            else -> phaseName = "Waning Crescent"
        }

        val bases = listOf("A", "G", "T", "C")
        val primaryIdx = (hash % 4).toInt()
        val secondaryIdx = ((hash + 1 + (hash % 3)) % 4).toInt()

        val primary = bases[primaryIdx]
        val secondary = bases[secondaryIdx]
        val pair = primary + secondary

        // Suppression on a deterministic random week of the calendar month (Week 1, 2, 3, or 4)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMs }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) // 0-indexed

        val monthStr = String.format(Locale.US, "%04d-%02d", year, month + 1)
        val monthHash = getDeterministicHash(monthStr)
        val suppressionWeek = (monthHash % 4).toInt() // Selects Week 0, 1, 2, or 3
        val currentWeek = (dayOfMonth - 1) / 7 // Current day's week index (0 to 4)

        val isSuppressed = if (currentWeek == suppressionWeek && dayOfMonth <= 28) {
            (hash % 100) < 50
        } else {
            false
        }

        val moonAngle = (2.0 * Math.PI * lunarAge) / LUNAR_MONTH_DAYS - Math.PI / 2.0
        val moonModifier = 0.0125 * sin(moonAngle)

        val primaryMultiplier = 1.125 + moonModifier
        val secondaryMultiplier = 1.625 + moonModifier

        return WaveConfig(
            pair = pair,
            primary = primary,
            secondary = secondary,
            isSuppressed = isSuppressed,
            lunarAge = lunarAge,
            isLunarInfluence = isLunarInfluence,
            phaseName = phaseName,
            isNewMoon = isNewMoon,
            isFullMoon = isFullMoon,
            primaryMultiplier = primaryMultiplier,
            secondaryMultiplier = secondaryMultiplier,
        )
    }

    fun generateWaveGeneBlock(wave: WaveConfig?, random: Random = Random()): String {
        val bases = listOf("A", "G", "T", "C")
        if (wave == null || wave.isSuppressed) {
            var res = ""
            for (i in 0 until 8) {
                res += bases[random.nextInt(bases.size)]
            }
            return res
        }

        val b1 = wave.primary
        val b2 = wave.secondary
        val m1 = wave.primaryMultiplier
        val m2 = wave.secondaryMultiplier

        var res = ""
        for (i in 0 until 8) {
            val prevChar = if (i > 0) res[i - 1].toString() else ""
            val weights = mutableMapOf("A" to 1.0, "G" to 1.0, "T" to 1.0, "C" to 1.0)
            if (prevChar == b1) {
                weights[b2] = m2
            } else {
                weights[b1] = m1
            }

            val sum = weights.values.sum()
            val r = random.nextDouble() * sum
            var acc = 0.0
            var selected = "A"
            for ((base, w) in weights) {
                acc += w
                if (r < acc) {
                    selected = base
                    break
                }
            }
            res += selected
        }
        return res
    }

    fun getSpectrumWaveCoupling(timeMs: Long): Double {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        val hours = cal.get(Calendar.HOUR_OF_DAY)
        val minutes = cal.get(Calendar.MINUTE)
        val seconds = cal.get(Calendar.SECOND)
        val daySeconds = hours * 3600 + minutes * 60 + seconds
        val dayFraction = daySeconds.toDouble() / 86400.0
        val baseValue = 80.0
        val sinusoidalFluctuation = 12.375 * sin(dayFraction * 2.0 * Math.PI * 4.0)
        val hackBonus = 0.0
        return (baseValue + sinusoidalFluctuation + hackBonus).coerceIn(0.0, 100.0)
    }

    fun getAnomalyEngineSuccessChance(grandTotal: Long, coupling: Double): AnomalyChance {
        if (grandTotal <= 10000) {
            return AnomalyChance(1.0, 0.0, 0.0, 1.0)
        }
        if (grandTotal >= 250000) {
            return AnomalyChance(100.0, 0.0, 0.0, 100.0)
        }

        val minLog = Math.log(10000.0)
        val maxLog = Math.log(250000.0)
        val currentLog = Math.log(grandTotal.toDouble())
        val t = (currentLog - minLog) / (maxLog - minLog)

        val baseChance = 1.0 + 99.0 * t

        val multiplesOf14 = listOf(14.0, 28.0, 42.0, 56.0, 70.0, 84.0, 98.0)
        var peakBoost = 0.0
        for (peak in multiplesOf14) {
            val dist = abs(baseChance - peak)
            if (dist < 5.0) {
                val boostVal = 6.5 * exp(-(dist / 1.8).pow(2.0))
                peakBoost = Math.max(peakBoost, boostVal)
            }
        }

        val harmonicModifier = (coupling - 80.0) * 0.25
        val finalChance = (baseChance + peakBoost + harmonicModifier).coerceIn(1.0, 100.0)

        return AnomalyChance(
            baseChance = baseChance,
            peakBoost = peakBoost,
            harmonicModifier = harmonicModifier,
            finalChance = finalChance,
        )
    }

    fun generateAnomalousGene(random: Random = Random()): String {
        val chars = "XZYW?!$%&@#"
        var result = ""
        for (i in 0 until 8) {
            result += chars[random.nextInt(chars.length)]
        }
        return result
    }

    fun isAnomalousGene(seq: String): Boolean {
        val chars = "XZYW?!$%&@#"
        return seq.any { it in chars }
    }

    fun getBenefitForAnomalousGene(gene: String): AnomalousBenefit {
        val padded = gene.padEnd(8, 'A')
        val s0 = padded[0]
        val s1 = padded[1]
        val s2 = padded[2]
        val s3 = padded[3]
        val s4 = padded[4]
        val s5 = padded[5]
        val s6 = padded[6]
        val s7 = padded[7]

        val prefix = when (s0) {
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

        val suffix = when (s1) {
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

        val rawPower = charVal(s2) + charVal(s3) + charVal(s4) + charVal(s5)
        val effectIndex = (s0.code + s1.code) % 6

        var id = "DOUBLE_STRIKE"
        var effectType = "double_attack"
        var magnitude = 0.0
        var chance = 0.0
        var baseDesc = ""

        when (effectIndex) {
            0 -> {
                id = "DOUBLE_STRIKE"
                effectType = "double_attack"
                magnitude = 1.2 + 0.04 * rawPower
                baseDesc = "Attacks deal ${String.format(Locale.US, "%.2f", magnitude)}x damage."
            }
            1 -> {
                id = "SELF_DESTRUCT"
                effectType = "self_destruct"
                magnitude = 40.0 + 8.0 * rawPower
                baseDesc = "Upon fall, self-destructs and deals ${magnitude.toInt()} flat damage."
            }
            2 -> {
                id = "HARVEST_BOOST"
                effectType = "harvest_boost"
                chance = (30.0 + 3.0 * rawPower).coerceAtMost(100.0)
                baseDesc = "Dramatically boosts gene harvesting rates (${chance.toInt()}% chance for +1 extra gene on win)."
            }
            3 -> {
                id = "HEALTH_REGEN"
                effectType = "health_regen"
                magnitude = 4.0 + rawPower
                baseDesc = "Heals ${magnitude.toInt()} HP on each attack."
            }
            4 -> {
                id = "PHASE_SHIFT"
                effectType = "phase_shift"
                chance = 10.0 + 1.5 * rawPower
                baseDesc = "Grants a ${chance.toInt()}% evasion chance to phase shift past incoming attacks."
            }
            5 -> {
                id = "COHERENCE_SHIELD"
                effectType = "coherence_shield"
                magnitude = 1.0
                baseDesc = "Grants immunity to positive density drag during descent/ascent into anomalies."
            }
        }

        val triggerIndex = (s6.code + s7.code) % 8
        val triggerDesc = when (triggerIndex) {
            0 -> "Always active in combat."
            1 -> "Only active during Dark moon phases."
            2 -> "Only active during Light moon phases."
            3 -> "Only active when under 40% Vitality."
            4 -> "Only active when above 70% Vitality."
            5 -> "Only active during the first 3 turns of combat."
            6 -> "Only active after turn 6 of combat."
            7 -> "Only active when local Spectrum Wave Coupling is above 82%."
            else -> "Always active in combat."
        }

        return AnomalousBenefit(
            id = id,
            name = "$prefix $suffix",
            description = "$baseDesc ($triggerDesc)",
            effectType = effectType,
            magnitude = magnitude,
            chance = chance,
            triggerIndex = triggerIndex,
            triggerDesc = triggerDesc,
        )
    }

    fun getAnomalousBenefits(sequence: String): List<AnomalousBenefit> {
        val benefits = mutableListOf<AnomalousBenefit>()
        for (i in 0 until sequence.length step 8) {
            if (i + 8 <= sequence.length) {
                val chunk = sequence.substring(i, i + 8)
                if (isAnomalousGene(chunk)) {
                    val b = getBenefitForAnomalousGene(chunk)
                    if (benefits.none { it.id == b.id }) {
                        benefits.add(b)
                    }
                }
            }
        }
        return benefits
    }

    fun hasCoherenceShield(creature: Creature?, originalSequence: String?): Boolean {
        if (creature != null) {
            if (getAnomalousBenefits(creature.sequence).any { it.id == "COHERENCE_SHIELD" }) {
                return true
            }
            creature.appendedGenes.forEach { gene ->
                if (isAnomalousGene(gene) && getBenefitForAnomalousGene(gene).id == "COHERENCE_SHIELD") {
                    return true
                }
            }
            return false
        }
        if (originalSequence != null) {
            if (getAnomalousBenefits(originalSequence).any { it.id == "COHERENCE_SHIELD" }) {
                return true
            }
        }
        return false
    }

    data class AnomalyLifecycleMetrics(
        val phase: AnomalyLifecyclePhase,
        val progress: Float, // Progress within overall epoch [0.0, 1.0)
        val phaseProgress: Float, // Progress within current phase [0.0, 1.0)
        val timeRemainingMs: Long, // Time remaining in current phase
        val epochTimeRemainingMs: Long, // Time remaining in current epoch
        val radiusMultiplier: Float,
        val heightMultiplier: Float,
        val peakAltitudeMeters: Float,
        val mutationFluxMod: Float,
        val harvestYieldMod: Float,
        val densityMod: Float,
        val forcefieldResistanceMod: Float,
    )

    /**
     * Evaluates the deterministic lifecycle metrics and morphological scaling factors for an anomaly
     * based on its seed and the provided timestamp.
     */
    /**
     * Evaluates the deterministic lifecycle metrics and morphological scaling factors for an anomaly
     * based on its seed and the provided timestamp.
     */
    fun calculateAnomalyLifecycle(
        seed: Long,
        timeMs: Long,
        epochDurationMs: Long = 6L * 3600L * 1000L, // 6 hour default epoch
        baseSummitMeters: Float = 850f,
    ): AnomalyLifecycleMetrics {
        val safeEpochDuration = if (epochDurationMs > 0L) epochDurationMs else 6L * 3600L * 1000L
        val phaseOffset = kotlin.math.abs(seed % safeEpochDuration)
        val epochElapsed = ((timeMs + phaseOffset) % safeEpochDuration + safeEpochDuration) % safeEpochDuration
        val progress = (epochElapsed.toDouble() / safeEpochDuration.toDouble()).toFloat().coerceIn(0f, 0.9999f)
        val epochTimeRemainingMs = safeEpochDuration - epochElapsed

        val inceptionEndMs = (0.15 * safeEpochDuration).toLong()
        val activeEndMs = (0.70 * safeEpochDuration).toLong()
        val destabilizingEndMs = (0.90 * safeEpochDuration).toLong()
        val voidCollapseEndMs = safeEpochDuration

        return when {
            progress < 0.15f -> {
                // 1. INCEPTION (0.00 to 0.15): Nascent seedling emerging from bedrock
                val u = progress / 0.15f
                val phaseRemaining = (inceptionEndMs - epochElapsed).coerceAtLeast(0L)
                val ease = 0.5f * (1.0f - kotlin.math.cos((u * Math.PI).toFloat()))
                val radiusMult = 0.35f + 0.65f * ease
                val heightMult = 0.10f + 0.90f * ease
                AnomalyLifecycleMetrics(
                    phase = AnomalyLifecyclePhase.INCEPTION,
                    progress = progress,
                    phaseProgress = u,
                    timeRemainingMs = phaseRemaining,
                    epochTimeRemainingMs = epochTimeRemainingMs,
                    radiusMultiplier = radiusMult,
                    heightMultiplier = heightMult,
                    peakAltitudeMeters = baseSummitMeters * heightMult,
                    mutationFluxMod = 0.50f,
                    harvestYieldMod = 0.70f,
                    densityMod = -0.15f * (1.0f - u),
                    forcefieldResistanceMod = 0.60f + 0.40f * u,
                )
            }
            progress < 0.70f -> {
                // 2. ACTIVE NEXUS (0.15 to 0.70): Stable colossal titan with organic biological breathing
                val u = (progress - 0.15f) / 0.55f
                val phaseRemaining = (activeEndMs - epochElapsed).coerceAtLeast(0L)
                val breathWave = kotlin.math.sin((u * 6.0 * Math.PI).toFloat())
                val heightBreath = kotlin.math.sin((u * 6.0 * Math.PI).toFloat())
                val radiusMult = 1.0f + 0.025f * breathWave
                val heightMult = 1.0f + 0.035f * heightBreath
                AnomalyLifecycleMetrics(
                    phase = AnomalyLifecyclePhase.ACTIVE_NEXUS,
                    progress = progress,
                    phaseProgress = u,
                    timeRemainingMs = phaseRemaining,
                    epochTimeRemainingMs = epochTimeRemainingMs,
                    radiusMultiplier = radiusMult,
                    heightMultiplier = heightMult,
                    peakAltitudeMeters = baseSummitMeters * heightMult,
                    mutationFluxMod = 1.0f,
                    harvestYieldMod = 1.0f,
                    densityMod = 0.0f,
                    forcefieldResistanceMod = 1.0f,
                )
            }
            progress < 0.90f -> {
                // 3. DESTABILIZING (0.70 to 0.90): Overcharged volumetric caldera with energetic bulge and harmonic tremor
                val u = (progress - 0.70f) / 0.20f
                val phaseRemaining = (destabilizingEndMs - epochElapsed).coerceAtLeast(0L)
                val bulge = kotlin.math.sin((u * Math.PI).toFloat())
                val tremor = 0.02f * kotlin.math.sin((u * 16.0 * Math.PI).toFloat()) * bulge
                val heightMult = 1.0f + 0.25f * bulge + tremor
                val radiusMult = 1.0f + 0.10f * kotlin.math.sin((u * 2.0 * Math.PI).toFloat()) + tremor
                AnomalyLifecycleMetrics(
                    phase = AnomalyLifecyclePhase.DESTABILIZING,
                    progress = progress,
                    phaseProgress = u,
                    timeRemainingMs = phaseRemaining,
                    epochTimeRemainingMs = epochTimeRemainingMs,
                    radiusMultiplier = radiusMult,
                    heightMultiplier = heightMult,
                    peakAltitudeMeters = baseSummitMeters * heightMult,
                    mutationFluxMod = 2.0f,
                    harvestYieldMod = 2.0f,
                    densityMod = 0.20f,
                    forcefieldResistanceMod = 1.35f,
                )
            }
            else -> {
                // 4. VOID COLLAPSE (0.90 to 1.00): Imploding singularity vortex connecting seamlessly from 1.0x
                val u = (progress - 0.90f) / 0.10f
                val phaseRemaining = (voidCollapseEndMs - epochElapsed).coerceAtLeast(0L)
                val decay = (1.0f - u) * (1.0f - u)
                val heightMult = 0.05f + 0.95f * decay
                val radiusMult = 0.15f + 0.85f * (1.0f - u)
                AnomalyLifecycleMetrics(
                    phase = AnomalyLifecyclePhase.VOID_COLLAPSE,
                    progress = progress,
                    phaseProgress = u,
                    timeRemainingMs = phaseRemaining,
                    epochTimeRemainingMs = epochTimeRemainingMs,
                    radiusMultiplier = radiusMult,
                    heightMultiplier = heightMult,
                    peakAltitudeMeters = baseSummitMeters * heightMult,
                    mutationFluxMod = 3.0f,
                    harvestYieldMod = 0.20f,
                    densityMod = -0.30f,
                    forcefieldResistanceMod = 0.40f,
                )
            }
        }
    }

    /**
     * Evaluates the deterministic lifecycle metrics and morphological scaling factors for an anomaly
     * based on its latitude and longitude coordinate hash and the provided timestamp.
     */
    fun calculateAnomalyLifecycle(
        lat: Double,
        lng: Double,
        timeMs: Long = getAdjustedTime(),
        epochDurationMs: Long = 6L * 3600L * 1000L,
        baseSummitMeters: Float = 850f,
    ): AnomalyLifecycleMetrics {
        val seed = (lat.hashCode().toLong() shl 32) xor (lng.hashCode().toLong() and 0xFFFFFFFFL)
        return calculateAnomalyLifecycle(seed, timeMs, epochDurationMs, baseSummitMeters)
    }

    /**
     * Evaluates the deterministic lifecycle metrics and morphological scaling factors for an anomaly
     * based on its anomaly ID and the provided timestamp.
     */
    fun calculateAnomalyLifecycle(
        anomalyId: String,
        timeMs: Long = getAdjustedTime(),
        epochDurationMs: Long = 6L * 3600L * 1000L,
        baseSummitMeters: Float = 850f,
    ): AnomalyLifecycleMetrics {
        val seed = (anomalyId.hashCode().toLong() and 0xFFFFFFFFL)
        return calculateAnomalyLifecycle(seed, timeMs, epochDurationMs, baseSummitMeters)
    }

    /**
     * Evaluates the lifecycle metrics and morphological scaling factors for an anomaly
     * based directly on a fractional lifecycle progress [0.0f, 1.0f).
     */
    fun calculateAnomalyLifecycleForProgress(
        progress: Float,
        epochDurationMs: Long = 6L * 3600L * 1000L,
        baseSummitMeters: Float = 850f,
    ): AnomalyLifecycleMetrics {
        val safeEpochDuration = if (epochDurationMs > 0L) epochDurationMs else 6L * 3600L * 1000L
        val p = progress.coerceIn(0f, 0.9999f)
        val elapsed = (p * safeEpochDuration).toLong()
        return calculateAnomalyLifecycle(seed = 0L, timeMs = elapsed, epochDurationMs = safeEpochDuration, baseSummitMeters = baseSummitMeters)
    }

    /**
     * Applies pre-calculated lifecycle metrics directly to an anomaly.
     */
    fun updateAnomalyWithLifecycleMetrics(
        anomaly: PoxAnomaly,
        metrics: AnomalyLifecycleMetrics,
    ): PoxAnomaly = anomaly.copy(
        lifecyclePhase = metrics.phase,
        lifecycleProgress = metrics.progress,
        timeRemainingMs = metrics.timeRemainingMs,
        peakAltitudeMeters = metrics.peakAltitudeMeters,
        mutationFluxMod = metrics.mutationFluxMod,
        harvestYieldMod = metrics.harvestYieldMod,
        density = (anomaly.density + metrics.densityMod).coerceIn(-0.33, 0.33),
    )

    /**
     * Updates an anomaly's lifecycle metrics (phase, progress, altitude, multipliers)
     * based on its deterministic ID and gene hash and the provided or current adjusted time.
     */
    fun updateAnomalyWithLifecycle(
        anomaly: PoxAnomaly,
        timeMs: Long = getAdjustedTime(),
        epochDurationMs: Long = 6L * 3600L * 1000L,
    ): PoxAnomaly {
        val rawRadius = if (anomaly.heatZoneDiameter > 0) {
            (anomaly.heatZoneDiameter / 3.28084 / 2.0).toFloat()
        } else {
            1000.0f
        }
        val radiusMeters = rawRadius.coerceIn(2.5f, 4000.0f)
        val spectralClass = anomaly.spectralClass
        val baseSummitMeters = spectralClass.calculateBaseCeiling(radiusMeters, anomaly.morphology)
        val seed = (anomaly.id.hashCode().toLong() and 0xFFFFFFFFL) xor (anomaly.gene.hashCode().toLong() shl 32)
        val metrics = calculateAnomalyLifecycle(seed, timeMs, epochDurationMs, baseSummitMeters)
        return updateAnomalyWithLifecycleMetrics(anomaly, metrics)
    }
}

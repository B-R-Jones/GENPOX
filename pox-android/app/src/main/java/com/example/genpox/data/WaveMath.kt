package com.example.genpox.data

import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.pow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
import java.util.Random

object WaveMath {
    const val LUNAR_MONTH_DAYS = 29.53059
    
    // Standard Reference New Moon: May 17, 2026, 01:54:00 UTC
    private val REFERENCE_NEW_MOON_MS: Long by lazy {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse("2026-05-17T01:54:00Z")?.time ?: 1778982840000L
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
        val secondaryMultiplier: Double
    )

    data class AnomalyChance(
        val baseChance: Double,
        val peakBoost: Double,
        val harmonicModifier: Double,
        val finalChance: Double
    )

    data class AnomalousBenefit(
        val id: String,
        val name: String,
        val description: String,
        val effectType: String,
        val magnitude: Double = 0.0,
        val chance: Double = 0.0,
        val triggerIndex: Int = 0,
        val triggerDesc: String = ""
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
            secondaryMultiplier = secondaryMultiplier
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
            finalChance = finalChance
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
        if (seq.length != 8) return false
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
            triggerDesc = triggerDesc
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
}

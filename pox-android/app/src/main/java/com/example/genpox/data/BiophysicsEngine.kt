package com.example.genpox.data

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.log10
import java.util.Random

object BiophysicsEngine {

    /**
     * Calculates the Melting Temperature (Tm) of a sequence in Celsius.
     * Uses a salt-corrected Wallace-Breslauer formulation.
     */
    fun calculateMeltingTemperature(sequence: String, saltMolar: Double = 0.05): Double {
        val gcCount = sequence.count { it == 'G' || it == 'C' || it == 'Z' || it == '%' || it == '&' }
        val atCount = sequence.length - gcCount
        
        // Wallace formula: Tm = 2*(A+T) + 4*(G+C)
        val baseTm = (2.0 * atCount) + (4.0 * gcCount)
        
        // Salt correction factor: Shifts Tm based on monovalent ion concentration
        val correction = if (saltMolar > 0.0) {
            -16.6 * log10(0.05 / saltMolar)
        } else {
            0.0
        }
        
        return baseTm + correction
    }

    /**
     * Calculates the Minimum Free Energy (MFE) in kcal/mol of a single-stranded sequence.
     * Uses Nussinov's dynamic programming loop-state estimation algorithm.
     */
    fun calculateMinimumFreeEnergy(sequence: String): Double {
        val n = sequence.length
        if (n < 4) return 0.0
        
        // DP table for minimum energy values
        val dp = Array(n) { DoubleArray(n) { 0.0 } }
        
        // Complementary pair checker
        val isPair = { c1: Char, c2: Char ->
            (c1 == 'A' && c2 == 'T') || (c1 == 'T' && c2 == 'A') ||
            (c1 == 'G' && c2 == 'C') || (c1 == 'C' && c2 == 'G') ||
            (c1 == 'X' && c2 == 'Y') || (c1 == 'Y' && c2 == 'X')
        }
        
        // Hydrogen bonding energy values
        val getEnergy = { c1: Char, c2: Char ->
            when {
                (c1 == 'G' && c2 == 'C') || (c1 == 'C' && c2 == 'G') -> -3.0 // 3 H-bonds
                (c1 == 'A' && c2 == 'T') || (c1 == 'T' && c2 == 'A') -> -2.0 // 2 H-bonds
                (c1 == 'X' && c2 == 'Y') || (c1 == 'Y' && c2 == 'X') -> -4.0 // Anomalous quantum pairing
                else -> 0.0
            }
        }

        // Loop over interval widths (minimum loop size = 3 bases, so width k starts at 4)
        for (k in 4 until n) {
            for (i in 0 until n - k) {
                val j = i + k
                var minEnergy = dp[i + 1][j]
                minEnergy = Math.min(minEnergy, dp[i][j - 1])
                
                if (isPair(sequence[i], sequence[j])) {
                    minEnergy = Math.min(minEnergy, dp[i + 1][j - 1] + getEnergy(sequence[i], sequence[j]))
                }
                
                for (mid in i + 1 until j) {
                    minEnergy = Math.min(minEnergy, dp[i][mid] + dp[mid + 1][j])
                }
                dp[i][j] = minEnergy
            }
        }
        
        return dp[0][n - 1]
    }

    /**
     * Generates a simulated Phred Quality Score (Q) based on polymerase enzyme fidelity.
     * Higher Q-scores correspond to lower error rates.
     */
    fun calculatePhredQScore(polymerase: String, random: Random = Random()): Double {
        return when (polymerase.uppercase()) {
            "TAQ" -> 15.0 + random.nextDouble() * 10.0 // Q15 to Q25 (higher errors)
            "PFU" -> 35.0 + random.nextDouble() * 5.0  // Q35 to Q40 (proofreading, highly accurate)
            "TTH" -> 25.0 + random.nextDouble() * 8.0  // Q25 to Q33 (heat-tolerant)
            else -> 30.0
        }
    }

    /**
     * Calculates the Codon Adaptation Index (CAI) of a 64-character DNA sequence
     * against a specific creature faction's codon preference bias.
     */
    fun calculateCodonAdaptationIndex(sequence: String, faction: String): Double {
        val seq64 = sequence.take(64).uppercase()
        if (seq64.length < 3) return 1.0
        
        var productLog = 0.0
        var count = 0
        
        val biasBase = when (faction.uppercase()) {
            "INFECTION" -> 'A'
            "MECH" -> 'G'
            "PARASITE" -> 'T'
            "CONTAINMENT" -> 'C'
            else -> 'A'
        }
        
        // Loop over the sequence in codon triplets (3 bases each)
        for (i in 0 until seq64.length - 2 step 3) {
            val codon = seq64.substring(i, i + 3)
            val preferredBaseCount = codon.count { it == biasBase }
            
            // Procedural codon weight scaling:
            // 3 bias bases (e.g. AAA for Infection) = 1.0 weight
            // 0 bias bases (e.g. CGC for Infection) = 0.2 weight
            val weight = 0.2 + 0.8 * (preferredBaseCount.toDouble() / 3.0)
            
            productLog += Math.log(weight)
            count++
        }
        
        if (count == 0) return 1.0
        return exp(productLog / count)
    }
}

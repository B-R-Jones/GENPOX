package com.example.genpox

import com.example.genpox.data.BiophysicsEngine
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class BiophysicsEngineTest {

    @Test
    fun testWallaceMeltingTemperatureWithSalt() {
        val seq = "AAGCTAGC" // 8-bp: 4 GC, 4 AT
        // Base Tm = 2 * 4 + 4 * 4 = 24.0°C
        
        // Salt = 0.05M (correction should be 0.0)
        val tm05 = BiophysicsEngine.calculateMeltingTemperature(seq, 0.05)
        assertEquals(24.0, tm05, 0.001)
        
        // Salt = 0.20M (correction shifts Tm up)
        val tm20 = BiophysicsEngine.calculateMeltingTemperature(seq, 0.20)
        assertTrue("Tm with higher salt should be greater than base Tm", tm20 > tm05)
        
        // Salt = 0.01M (correction shifts Tm down)
        val tm01 = BiophysicsEngine.calculateMeltingTemperature(seq, 0.01)
        assertTrue("Tm with lower salt should be lower than base Tm", tm01 < tm05)
    }

    @Test
    fun testMinimumFreeEnergyFolding() {
        // Highly complementary sequence (hairpin former)
        val gcHairpin = "GGGGCCCC" // complementary GC pairs
        val energyGc = BiophysicsEngine.calculateMinimumFreeEnergy(gcHairpin)
        assertTrue("Complementary GC sequence should have negative folding energy", energyGc < 0.0)
        
        // Non-complementary sequence (no pairing)
        val nonPairing = "GGGGGGGG"
        val energyNone = BiophysicsEngine.calculateMinimumFreeEnergy(nonPairing)
        assertEquals("Non-pairing sequence should have 0.0 energy", 0.0, energyNone, 0.001)
    }

    @Test
    fun testCodonAdaptationIndexBounds() {
        val seq = "ATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCG" // 64-bp
        
        val factions = listOf("INFECTION", "MECH", "PARASITE", "CONTAINMENT")
        for (faction in factions) {
            val cai = BiophysicsEngine.calculateCodonAdaptationIndex(seq, faction)
            assertTrue("CAI should be >= 0.0", cai >= 0.0)
            assertTrue("CAI should be <= 1.0", cai <= 1.0)
        }
    }

    @Test
    fun testPolymeraseQScoreRanges() {
        val random = java.util.Random(42)
        val taqQ = BiophysicsEngine.calculatePhredQScore("TAQ", random)
        assertTrue("Taq Q-score should be between 15 and 25", taqQ in 15.0..25.0)

        val pfuQ = BiophysicsEngine.calculatePhredQScore("PFU", random)
        assertTrue("Pfu Q-score should be between 35 and 40", pfuQ in 35.0..40.0)

        val tthQ = BiophysicsEngine.calculatePhredQScore("TTH", random)
        assertTrue("Tth Q-score should be between 25 and 33", tthQ in 25.0..33.0)
    }

    @Test
    fun testThermalYieldMath() {
        // Verification of Gaussian yield curve: exp(-(temp - Tm)^2 / (2 * sigma^2))
        val tm = 50.0
        val sigma = 15.0 // Taq
        
        // At perfect alignment, yield should be exp(0) = 1.0
        val diffPerfect = 50.0 - tm
        val yieldPerfect = Math.exp(-(diffPerfect * diffPerfect) / (2.0 * sigma * sigma))
        assertEquals(1.0, yieldPerfect, 0.0001)
        
        // At 15 degrees deviation (1 sigma), yield should be exp(-0.5) ~ 0.6065
        val diff1Sigma = 65.0 - tm
        val yield1Sigma = Math.exp(-(diff1Sigma * diff1Sigma) / (2.0 * sigma * sigma))
        assertEquals(Math.exp(-0.5), yield1Sigma, 0.0001)
    }
}

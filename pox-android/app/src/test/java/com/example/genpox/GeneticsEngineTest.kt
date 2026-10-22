package com.example.genpox

import com.example.genpox.data.GeneticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneticsEngineTest {

    @Test
    fun testWallaceMeltingTemperatureWithSalt() {
        val seq = "AAGCTAGC" // 8-bp: 4 GC, 4 AT
        // Base Tm = 2 * 4 + 4 * 4 = 24.0°C

        // Salt = 0.05M (correction should be 0.0)
        val tm05 = GeneticsEngine.calculateMeltingTemperature(seq, 0.05)
        assertEquals(24.0, tm05, 0.001)

        // Salt = 0.20M (correction shifts Tm up)
        val tm20 = GeneticsEngine.calculateMeltingTemperature(seq, 0.20)
        assertTrue("Tm with higher salt should be greater than base Tm", tm20 > tm05)

        // Salt = 0.01M (correction shifts Tm down)
        val tm01 = GeneticsEngine.calculateMeltingTemperature(seq, 0.01)
        assertTrue("Tm with lower salt should be lower than base Tm", tm01 < tm05)
    }

    @Test
    fun testMinimumFreeEnergyFolding() {
        // Highly complementary sequence (hairpin former)
        val gcHairpin = "GGGGCCCC" // complementary GC pairs
        val energyGc = GeneticsEngine.calculateMinimumFreeEnergy(gcHairpin)
        assertTrue("Complementary GC sequence should have negative folding energy", energyGc < 0.0)

        // Non-complementary sequence (no pairing)
        val nonPairing = "GGGGGGGG"
        val energyNone = GeneticsEngine.calculateMinimumFreeEnergy(nonPairing)
        assertEquals("Non-pairing sequence should have 0.0 energy", 0.0, energyNone, 0.001)
    }

    @Test
    fun testCodonAdaptationIndexBounds() {
        val seq = "ATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCG" // 64-bp

        val factions = listOf("INFECTION", "MECH", "PARASITE", "CONTAINMENT")
        for (faction in factions) {
            val cai = GeneticsEngine.calculateCodonAdaptationIndex(seq, faction)
            assertTrue("CAI should be >= 0.0", cai >= 0.0)
            assertTrue("CAI should be <= 1.0", cai <= 1.0)
        }
    }

    @Test
    fun testPolymeraseQScoreRanges() {
        val random = java.util.Random(42)
        val taqQ = GeneticsEngine.calculatePhredQScore("TAQ", random)
        assertTrue("Taq Q-score should be between 15 and 25", taqQ in 15.0..25.0)

        val pfuQ = GeneticsEngine.calculatePhredQScore("PFU", random)
        assertTrue("Pfu Q-score should be between 35 and 40", pfuQ in 35.0..40.0)

        val tthQ = GeneticsEngine.calculatePhredQScore("TTH", random)
        assertTrue("Tth Q-score should be between 25 and 33", tthQ in 25.0..33.0)
    }

    @Test
    fun testQScoreConversionMath() {
        // Q30 should represent 1 in 1000 error probability (0.001)
        val errorProb30 = GeneticsEngine.qScoreToErrorProbability(30.0)
        assertEquals(0.001, errorProb30, 0.0001)

        val qScore30 = GeneticsEngine.errorProbabilityToQScore(0.001)
        assertEquals(30.0, qScore30, 0.0001)

        // Q10 should represent 1 in 10 error probability (0.1)
        val errorProb10 = GeneticsEngine.qScoreToErrorProbability(10.0)
        assertEquals(0.1, errorProb10, 0.0001)

        val qScore10 = GeneticsEngine.errorProbabilityToQScore(0.1)
        assertEquals(10.0, qScore10, 0.0001)
    }
}

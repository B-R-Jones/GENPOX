package com.example.genpox.ui.scanner.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyPassRendererTest {

    @Test
    fun testEvaluateSmoothAnomalyColorsAtMilestones() {
        // At progress = 0.05, should return inception palette (electric cyan)
        val earlyColors = AnomalyPassRenderer.evaluateSmoothAnomalyColors(0.05f, 0.0f)
        assertEquals(0x22, earlyColors.sr)
        assertEquals(0xD3, earlyColors.sg)
        assertEquals(0xEE, earlyColors.sb)

        // At progress = 0.40, should return active nexus (neon pink)
        val midColors = AnomalyPassRenderer.evaluateSmoothAnomalyColors(0.40f, 0.0f)
        assertEquals(0xCC, midColors.sr)
        assertEquals(0x1A, midColors.sg)
        assertEquals(0x44, midColors.sb)

        // Color RGB components should always be in [0, 255]
        for (p in 0..100) {
            val progress = p / 100f
            val c = AnomalyPassRenderer.evaluateSmoothAnomalyColors(progress, 0.5f)
            assertTrue(c.sr in 0..255)
            assertTrue(c.sg in 0..255)
            assertTrue(c.sb in 0..255)
            assertTrue(c.fr in 0..255)
            assertTrue(c.fg in 0..255)
            assertTrue(c.fb in 0..255)
        }
    }
}

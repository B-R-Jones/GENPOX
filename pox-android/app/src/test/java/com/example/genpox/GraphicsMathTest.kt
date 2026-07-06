package com.example.genpox

import com.example.genpox.data.GraphicsMath
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull

class GraphicsMathTest {

    @Test
    fun testLunarBaselineMod() {
        val modNewMoon = GraphicsMath.calculateLunarBaselineMod(0.0)
        assertEquals(1.0f, modNewMoon, 0.001f)

        val modFullMoon = GraphicsMath.calculateLunarBaselineMod(0.5)
        assertEquals(1.075f, modFullMoon, 0.001f)
    }

    @Test
    fun testSkewnessAndWaveAmplitude() {
        val widths = listOf(10f, 10f, 10f, 10f)
        val skewnessZero = GraphicsMath.calculateSkewness(widths)
        assertEquals(0f, skewnessZero, 0.001f)

        val ampZero = GraphicsMath.calculateWaveAmplitude(skewnessZero)
        assertEquals(0.5f, ampZero, 0.001f)

        val variableWidths = listOf(5f, 10f, 15f, 20f)
        val skewnessPositive = GraphicsMath.calculateSkewness(variableWidths)
        assertTrue(skewnessPositive > 0f)
        
        val ampPositive = GraphicsMath.calculateWaveAmplitude(skewnessPositive)
        assertTrue(ampPositive > 0.5f)
    }

    @Test
    fun testCrystalVertices() {
        val vertices = GraphicsMath.calculateCrystalVertices(100f, 100f, 50f, 1f, 1f, 1f, 1f)
        assertEquals(4, vertices.size)
        // ptG: cx, cy - r * G
        assertEquals(100f, vertices[0].x)
        assertEquals(50f, vertices[0].y)
        // ptT: cx + r * T, cy
        assertEquals(150f, vertices[1].x)
        assertEquals(100f, vertices[1].y)
    }

    @Test
    fun testHelixStrand() {
        val metrics = GraphicsMath.calculateHelixStrand(
            index = 0,
            numStrands = 10,
            w = 200f,
            h = 400f,
            rotationPhase = 0f,
            pulseScale = 1.0f,
            isGlitching = false,
            glitchProgress = 0f,
            isAnomaly = false,
            glitchThresholdPx = 30f,
            glitchOffsetPx = 10f
        )
        assertNotNull(metrics)
        assertTrue(metrics.x1 > 0f)
        assertTrue(metrics.x2 > 0f)
        assertEquals(400f * 0.12f, metrics.y, 0.001f)
        assertTrue(metrics.radius1Dp > 0f)
        assertTrue(metrics.radius2Dp > 0f)
    }

    @Test
    fun test3DBlueprintProjection() {
        val vertices3D = GraphicsMath.generate3DBlueprintVertices(100.0, 0.0)
        assertEquals(6, vertices3D.size)
        
        // Top vertex (0, 100, 0)
        assertEquals(0.0, vertices3D[0].x, 0.001)
        assertEquals(100.0, vertices3D[0].y, 0.001)
        assertEquals(0.0, vertices3D[0].z, 0.001)

        val projected = GraphicsMath.project3DBlueprintPoint(vertices3D[0], Math.toRadians(30.0), 200.0, 200.0)
        // y = 100.0, z = 0.0, tilt = 30 deg.
        // yt = 100 * cos(30) - 0 = 100 * 0.866 = 86.6025
        // projectedY = centerY - yt = 200.0 - 86.6025 = 113.3975
        assertEquals(200.0f, projected.x, 0.001f)
        assertEquals(113.3975f, projected.y, 0.05f)
    }

    @Test
    fun testOscilloscopeHeights() {
        val heightNull = GraphicsMath.calculateOscilloscopeHeightDp(0, null, "AAGGAAGG", 0f)
        assertTrue(heightNull in 0.5f..2.5f)

        val heightCoherent = GraphicsMath.calculateOscilloscopeHeightDp(0, "AAGGAAGG", "AAGGAAGG", 0f)
        assertTrue(heightCoherent > 2.0f)
    }

    @Test
    fun testOrreryCelestialMath() {
        val xE = 1.0
        val yE = 0.0
        val coords = GraphicsMath.calculateOrreryCoordinates(
            planetName = "VENUS",
            timeMs = 1778982840000L,
            userLng = -122.4194,
            xE = xE,
            yE = yE
        )
        assertNotNull(coords)
        assertTrue(coords.declination in -1.0..1.0)
        assertTrue(coords.hourAngle in 0.0..(2.0 * Math.PI))
    }
}

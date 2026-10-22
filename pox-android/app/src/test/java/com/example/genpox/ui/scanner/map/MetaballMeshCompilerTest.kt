package com.example.genpox.ui.scanner.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaballMeshCompilerTest {

    @Test
    fun testEvaluateDensityWyvillFalloff() {
        val emitters = listOf(
            MetaballMeshCompiler.MetaballEmitter(x = 0f, y = 0f, z = 50f, radiusXy = 50f, radiusZ = 50f, weight = 1.0f),
        )

        // At center, density should be 1.0
        val centerDensity = MetaballMeshCompiler.evaluateDensity(0f, 0f, 50f, emitters)
        assertEquals(1.0f, centerDensity, 1e-4f)

        // At halfway, density should be (1 - (25/50)^2)^3 = (1 - 0.25)^3 = (0.75)^3 = 0.421875
        val halfDensity = MetaballMeshCompiler.evaluateDensity(0f, 0f, 75f, emitters)
        assertEquals(0.421875f, halfDensity, 1e-4f)

        // At boundary or beyond, density should be 0.0
        val edgeDensity = MetaballMeshCompiler.evaluateDensity(0f, 0f, 100f, emitters)
        assertEquals(0.0f, edgeDensity, 1e-4f)

        val farDensity = MetaballMeshCompiler.evaluateDensity(0f, 0f, 150f, emitters)
        assertEquals(0.0f, farDensity, 1e-4f)
    }

    @Test
    fun testGenerateProceduralEmittersHorizontalSprawl() {
        val emitters = MetaballMeshCompiler.generateProceduralEmitters(
            centerX = 100f,
            centerY = -50f,
            totalRadius = 800f,
            seed = 12345L,
        )

        assertTrue("Should generate at least 5 emitters for sprawling mass", emitters.size >= 5)

        // Core emitter
        val core = emitters[0]
        assertEquals(100f, core.x, 1e-4f)
        assertEquals(-50f, core.y, 1e-4f)
        assertTrue("Core horizontal radius should be proportional (>= 300m)", core.radiusXy >= 300f)
        assertTrue("Core vertical height should be proportional (>= 100m)", core.radiusZ >= 100f)

        // All emitters should have positive dimensions
        for (e in emitters) {
            assertTrue("Horizontal radius must be positive", e.radiusXy > 0f)
            assertTrue("Vertical radius must be positive", e.radiusZ > 0f)
        }
    }

    @Test
    fun testResolveNodalStructure() {
        val anomaly = com.example.genpox.data.PoxAnomaly(
            id = "test_anom_01",
            name = "Test Anomaly",
            lat = 37.7749,
            lng = -122.4194,
            gene = "A1T2G3C4",
            faction = "Anomaly",
            distance = 500.0,
            heatZoneDiameter = 6561.68, // 2000m
            density = 0.8,
        )

        val seed = (anomaly.id.hashCode().toLong() and 0xFFFFFFFFL) xor (anomaly.gene.hashCode().toLong() shl 32)
        val epochDurationMs = 6L * 3600L * 1000L
        val phaseOffset = kotlin.math.abs(seed % epochDurationMs)
        val activeNexusTimeMs = ((0.40 * epochDurationMs).toLong() - phaseOffset + epochDurationMs * 2) % epochDurationMs
        val nodalStructure = MetaballMeshCompiler.resolveNodalStructure(anomaly, activeNexusTimeMs)
        assertEquals("test_anom_01", nodalStructure.anomalyId)
        assertEquals("Must produce 8 tactical nodes (1 core + 5 ground lobes + 2 satellites)", 8, nodalStructure.nodes.size)

        // Verify Core Summit node
        val core = nodalStructure.nodes[0]
        assertEquals(MetaballMeshCompiler.AnomalyNodeType.CORE_SUMMIT, core.type)
        assertEquals(anomaly.lat, core.lat, 1e-4)
        assertEquals(anomaly.lng, core.lng, 1e-4)
        assertTrue("Core summit altitude must be elevated > 50m in Active Nexus", core.altitudeMeters > 50f)

        // Verify Ground Lobes
        for (i in 1 until nodalStructure.nodes.size) {
            val lobe = nodalStructure.nodes[i]
            assertTrue("Ground lobe altitude must be non-negative", lobe.altitudeMeters >= 0f)
        }
    }

    @Test
    fun testContains3D() {
        val anomaly = com.example.genpox.data.PoxAnomaly(
            id = "test_anom_02",
            name = "Test Anomaly 2",
            lat = 37.7749,
            lng = -122.4194,
            gene = "A1T2G3C4",
            faction = "Infection",
            distance = 500.0,
            heatZoneDiameter = 6561.68, // 2000m
            density = 0.8,
        )

        // Point at anomaly center on the ground should be inside
        val isInsideCenter = MetaballMeshCompiler.contains3D(anomaly, 37.7749, -122.4194, 50f)
        assertTrue("Center of anomaly at 50m altitude must be inside bio-mass", isInsideCenter)

        // Point far away (10km) should be outside
        val isInsideFar = MetaballMeshCompiler.contains3D(anomaly, 37.8500, -122.4194, 0f)
        assertFalse("Point 10km away must NOT be inside bio-mass", isInsideFar)
    }

    @Test
    fun testGenerateProceduralEmittersSpikeMorphology() {
        val spikeEmitters = MetaballMeshCompiler.generateProceduralEmitters(
            centerX = 0f,
            centerY = 0f,
            totalRadius = 15f,
            seed = 42L,
            morphology = com.example.genpox.data.AnomalyMorphology.SPIKE,
            baseCeilingMeters = 50f,
        )

        assertEquals("Spike must generate exactly 4 emitters (1 core + 3 spires)", 4, spikeEmitters.size)

        for (e in spikeEmitters) {
            assertTrue("Emitter Z must be non-negative", e.z >= 0f)
            assertTrue("Horizontal radius must be positive", e.radiusXy > 0f)
            assertTrue("Vertical radius must be positive", e.radiusZ > 0f)
        }
    }

    @Test
    fun testSpectralClassAllSevenTiersBothMorphologies() {
        val classes = com.example.genpox.data.AnomalySpectralClass.values()
        assertEquals(7, classes.size)

        // Test Sprawl classification
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_M, com.example.genpox.data.AnomalySpectralClass.fromRadius(50f, com.example.genpox.data.AnomalyMorphology.SPRAWL))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_K, com.example.genpox.data.AnomalySpectralClass.fromRadius(200f, com.example.genpox.data.AnomalyMorphology.SPRAWL))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_G, com.example.genpox.data.AnomalySpectralClass.fromRadius(400f, com.example.genpox.data.AnomalyMorphology.SPRAWL))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_F, com.example.genpox.data.AnomalySpectralClass.fromRadius(800f, com.example.genpox.data.AnomalyMorphology.SPRAWL))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_A, com.example.genpox.data.AnomalySpectralClass.fromRadius(1200f, com.example.genpox.data.AnomalyMorphology.SPRAWL))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_B, com.example.genpox.data.AnomalySpectralClass.fromRadius(2000f, com.example.genpox.data.AnomalyMorphology.SPRAWL))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_O, com.example.genpox.data.AnomalySpectralClass.fromRadius(3500f, com.example.genpox.data.AnomalyMorphology.SPRAWL))

        // Test Spike classification
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_M, com.example.genpox.data.AnomalySpectralClass.fromRadius(12f, com.example.genpox.data.AnomalyMorphology.SPIKE))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_K, com.example.genpox.data.AnomalySpectralClass.fromRadius(25f, com.example.genpox.data.AnomalyMorphology.SPIKE))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_G, com.example.genpox.data.AnomalySpectralClass.fromRadius(40f, com.example.genpox.data.AnomalyMorphology.SPIKE))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_F, com.example.genpox.data.AnomalySpectralClass.fromRadius(80f, com.example.genpox.data.AnomalyMorphology.SPIKE))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_A, com.example.genpox.data.AnomalySpectralClass.fromRadius(150f, com.example.genpox.data.AnomalyMorphology.SPIKE))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_B, com.example.genpox.data.AnomalySpectralClass.fromRadius(400f, com.example.genpox.data.AnomalyMorphology.SPIKE))
        assertEquals(com.example.genpox.data.AnomalySpectralClass.CLASS_O, com.example.genpox.data.AnomalySpectralClass.fromRadius(800f, com.example.genpox.data.AnomalyMorphology.SPIKE))

        // Test Ceiling Calculations
        val classMSprawlCeiling = com.example.genpox.data.AnomalySpectralClass.CLASS_M.calculateBaseCeiling(100f, com.example.genpox.data.AnomalyMorphology.SPRAWL)
        assertEquals(4.0f, classMSprawlCeiling, 0.01f)

        val classMSpikeCeiling = com.example.genpox.data.AnomalySpectralClass.CLASS_M.calculateBaseCeiling(15f, com.example.genpox.data.AnomalyMorphology.SPIKE)
        assertEquals(50.0f, classMSpikeCeiling, 0.01f)

        val classOSpikeCeiling = com.example.genpox.data.AnomalySpectralClass.CLASS_O.calculateBaseCeiling(1000f, com.example.genpox.data.AnomalyMorphology.SPIKE)
        assertEquals(3000.0f, classOSpikeCeiling, 0.01f)
    }
}

package com.example.genpox.map.tile

import com.example.genpox.data.AnomalyLifecyclePhase
import com.example.genpox.data.AnomalyMorphology
import com.example.genpox.data.AnomalySpawningEngine
import com.example.genpox.data.BuildingStructure
import com.example.genpox.data.PoxAnomaly
import com.example.genpox.data.RoadPoint
import com.example.genpox.data.WaveMath
import com.example.genpox.ui.scanner.map.MetaballMeshCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyFilamentIntegrationTest {

    @Test
    fun testAnomalySpawningFromTileGeometry() {
        val testRoads = listOf(
            listOf(
                RoadPoint(39.0990, -94.5790),
                RoadPoint(39.0990, -94.5780),
                RoadPoint(39.1000, -94.5780),
            ),
            listOf(
                RoadPoint(39.0980, -94.5780),
                RoadPoint(39.1000, -94.5780),
            ),
        )

        val testBuildings = listOf(
            BuildingStructure(
                points = listOf(
                    RoadPoint(39.0992, -94.5785),
                    RoadPoint(39.0992, -94.5782),
                    RoadPoint(39.0996, -94.5782),
                    RoadPoint(39.0996, -94.5785),
                ),
                heightMeters = 45f,
            ),
        )

        val anomalies = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = 39.0997,
            sectorLng = -94.5786,
            roads = testRoads,
            buildings = testBuildings,
            maxAnomalies = 1,
        )

        assertEquals(1, anomalies.size)
        val anomaly = anomalies.first()
        assertTrue(anomaly.id.isNotEmpty())
        assertTrue(anomaly.heatZoneDiameter > 0)
        // With tall building, evaluate morphology should be valid
        assertTrue(anomaly.morphology in listOf(AnomalyMorphology.SPIKE, AnomalyMorphology.SPRAWL))
    }

    @Test
    fun testBuildingInfectionMetaballDensity() {
        val centerLat = 39.0997
        val centerLng = -94.5786

        val anomaly = PoxAnomaly(
            id = "ANOM_TEST_01",
            name = "Test Anomaly Nexus",
            lat = centerLat,
            lng = centerLng,
            gene = "ACTG_NEXUS",
            distance = 0.0,
            heatZoneDiameter = 1200.0, // feet (~365 meters diameter, ~182m radius)
            morphology = AnomalyMorphology.SPRAWL,
        )

        // Point near center (within 30 meters)
        val nearLat = centerLat + 0.0001
        val nearLng = centerLng + 0.0001
        val isContainedNear = MetaballMeshCompiler.contains3D(anomaly, nearLat, nearLng, 0f)
        assertTrue("Point near anomaly center should be inside metaball density field", isContainedNear)

        // Point far outside (500 meters away)
        val farLat = centerLat + 0.005
        val farLng = centerLng + 0.005
        val isContainedFar = MetaballMeshCompiler.contains3D(anomaly, farLat, farLng, 0f)
        assertFalse("Point far outside anomaly should not be inside metaball density field", isContainedFar)
    }

    @Test
    fun testWaveMathLifecycleMultipliersAcrossPhases() {
        val baseSummit = 500f
        val epochDuration = 6L * 3600L * 1000L // 6 hours

        // Phase 1: Inception (elapsed = 5% of epoch)
        val inceptionMetrics = WaveMath.calculateAnomalyLifecycle(
            seed = 0L,
            timeMs = (0.05 * epochDuration).toLong(),
            epochDurationMs = epochDuration,
            baseSummitMeters = baseSummit,
        )
        assertEquals(AnomalyLifecyclePhase.INCEPTION, inceptionMetrics.phase)
        assertTrue(inceptionMetrics.radiusMultiplier in 0.35f..1.0f)
        assertTrue(inceptionMetrics.heightMultiplier in 0.10f..1.0f)

        // Phase 2: Active Nexus (elapsed = 40% of epoch)
        val activeMetrics = WaveMath.calculateAnomalyLifecycle(
            seed = 0L,
            timeMs = (0.40 * epochDuration).toLong(),
            epochDurationMs = epochDuration,
            baseSummitMeters = baseSummit,
        )
        assertEquals(AnomalyLifecyclePhase.ACTIVE_NEXUS, activeMetrics.phase)
        assertTrue(activeMetrics.radiusMultiplier in 0.95f..1.05f)
        assertTrue(activeMetrics.heightMultiplier in 0.95f..1.05f)

        // Phase 3: Destabilizing (elapsed = 80% of epoch)
        val destabMetrics = WaveMath.calculateAnomalyLifecycle(
            seed = 0L,
            timeMs = (0.80 * epochDuration).toLong(),
            epochDurationMs = epochDuration,
            baseSummitMeters = baseSummit,
        )
        assertEquals(AnomalyLifecyclePhase.DESTABILIZING, destabMetrics.phase)
        assertTrue(destabMetrics.radiusMultiplier >= 1.0f)
        assertTrue(destabMetrics.heightMultiplier >= 1.0f)

        // Phase 4: Void Collapse (elapsed = 95% of epoch)
        val collapseMetrics = WaveMath.calculateAnomalyLifecycle(
            seed = 0L,
            timeMs = (0.95 * epochDuration).toLong(),
            epochDurationMs = epochDuration,
            baseSummitMeters = baseSummit,
        )
        assertEquals(AnomalyLifecyclePhase.VOID_COLLAPSE, collapseMetrics.phase)
        assertTrue(collapseMetrics.radiusMultiplier <= 1.0f)
        assertTrue(collapseMetrics.heightMultiplier <= 1.0f)
    }
}

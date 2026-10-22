package com.example.genpox

import com.example.genpox.data.DeploymentEngine
import com.example.genpox.data.PoxAnomaly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeploymentEngineTest {

    @Test
    fun testCalculateDistanceInFeet() {
        // Palo Alto coordinates (approximate distance between two nearby coordinates)
        val lat1 = 37.4220
        val lng1 = -122.0841
        val lat2 = 37.4230
        val lng2 = -122.0830

        val dist = DeploymentEngine.calculateDistanceInFeet(lat1, lng1, lat2, lng2)
        assertTrue("Distance should be positive", dist > 0.0)
        assertTrue("Distance between nearby points should be reasonable in feet", dist in 300.0..600.0)
    }

    @Test
    fun testCalculateBoundaryRadius() {
        val anomalyId = "ANM-TEST-1"
        val diameter = 500.0
        val theta = 0.0

        // Standard boundary radius R(theta)
        val radius = DeploymentEngine.calculateBoundaryRadius(anomalyId, diameter, theta)
        val baseRadius = diameter / 2.0 // 250.0

        // Epsilon should range from 0.15 to 0.25, so radius should be within [base * 0.75, base * 1.25]
        assertTrue("Radius should be within boundary limits", radius in (baseRadius * 0.7)..(baseRadius * 1.3))
    }

    @Test
    fun testSynodicResonanceMod() {
        // Infection faction: +15 in Waxing Crescent/Gibbous, -10 in Waning Crescent/Gibbous
        assertEquals(15, DeploymentEngine.getSynodicResonanceMod("Infection", "Waxing Crescent"))
        assertEquals(-10, DeploymentEngine.getSynodicResonanceMod("Infection", "Waning Gibbous"))
        assertEquals(0, DeploymentEngine.getSynodicResonanceMod("Infection", "Full Moon"))

        // Mech faction: +15 in Quarters, -10 in New/Full Moon
        assertEquals(15, DeploymentEngine.getSynodicResonanceMod("Mech", "First Quarter"))
        assertEquals(-10, DeploymentEngine.getSynodicResonanceMod("Mech", "New Moon"))
    }

    @Test
    fun testCalculateEffectiveDefense() {
        val effectiveDef = DeploymentEngine.calculateEffectiveDefense(20, "Infection", "Waxing Crescent")
        assertEquals(35, effectiveDef)
    }

    @Test
    fun testCalculateStallDepth() {
        // High defense vs low boundary resistance
        val stallDepthFull = DeploymentEngine.calculateStallDepth(100, 100.0, 14.0) // lunarAge 14 is Full Moon approx
        assertEquals(100.0, stallDepthFull, 0.001)

        // Low defense vs high boundary resistance should clamp to >= 0.0
        val stallDepthLow = DeploymentEngine.calculateStallDepth(0, 100.0, 14.0)
        assertEquals(0.0, stallDepthLow, 0.001)
    }

    @Test
    fun testCalculateMutationInterval() {
        // Stalled depth 100% (high penetration, close to core, high mutation rate, low interval)
        val intervalDeep = DeploymentEngine.calculateMutationInterval(100.0, 14.0)
        // Stalled depth 0% (surface, low mutation rate, high interval)
        val intervalSurface = DeploymentEngine.calculateMutationInterval(0.0, 14.0)

        assertTrue("Deeper penetration should mutate faster (lower interval duration)", intervalDeep < intervalSurface)
    }

    @Test
    fun testCalculateTransitMetrics() {
        val metrics = DeploymentEngine.calculateTransitMetrics(
            creatureSpeed = 30,
            anomalyDistance = 1000.0,
            boundaryRadius = 200.0,
            stalledDepth = 50.0,
            effectiveDensity = 0.1,
            hasCoherenceShield = false,
        )

        assertTrue("Travel velocity should be positive", metrics.travelVelocity > 0.0)
        assertTrue("Travel duration should be positive", metrics.travelDuration > 0)
        assertTrue("Descent velocity should be positive", metrics.descentVelocity > 0.0)
        assertTrue("Descent duration should be positive", metrics.descentDuration > 0)
        assertEquals(60L, metrics.harvestDuration)
        assertEquals(metrics.descentDuration, metrics.ascentDuration)
        assertEquals(metrics.travelDuration, metrics.returnDuration)
        assertEquals(
            metrics.travelDuration + metrics.descentDuration + metrics.harvestDuration + metrics.ascentDuration + metrics.returnDuration,
            metrics.totalDuration,
        )
    }

    @Test
    fun testCombinedWaveDensity() {
        val landingLat = 37.4220
        val landingLng = -122.0841

        val anomalies = listOf(
            PoxAnomaly(
                id = "ANM-1",
                name = "Test Anomaly",
                lat = 37.4220,
                lng = -122.0841,
                gene = "AGTCGTAC",
                faction = "Infection",
                distance = 0.0,
                heatZoneDiameter = 500.0,
                density = 0.3,
            ),
        )

        val density = DeploymentEngine.calculateCombinedWaveDensity(landingLat, landingLng, anomalies)
        assertTrue("Combined density should be positive since user is at the epicenter", density > 0.0)
    }
}

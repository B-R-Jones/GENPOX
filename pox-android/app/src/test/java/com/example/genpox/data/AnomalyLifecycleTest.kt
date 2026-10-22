package com.example.genpox.data

import com.example.genpox.ui.scanner.map.MetaballMeshCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyLifecycleTest {

    private val epochDurationMs = 6L * 3600L * 1000L // 6 hours

    @Test
    fun testLifecyclePhaseTransitions() {
        val seed = 0L // Zero seed has 0 offset, so timeMs directly maps to epoch elapsed

        // 1. Inception: 5% into epoch
        val timeInception = (0.05 * epochDurationMs).toLong()
        val metricsInception = WaveMath.calculateAnomalyLifecycle(seed, timeInception, epochDurationMs)
        assertEquals(AnomalyLifecyclePhase.INCEPTION, metricsInception.phase)
        assertTrue("Inception height should be scaled down (< 0.8)", metricsInception.heightMultiplier < 0.8f)
        assertTrue("Inception radius should be scaled down (< 0.8)", metricsInception.radiusMultiplier < 0.8f)
        assertEquals(0.50f, metricsInception.mutationFluxMod, 1e-4f)
        assertEquals(0.70f, metricsInception.harvestYieldMod, 1e-4f)

        // 2. Active Nexus: 40% into epoch (subtle organic breathing within +/- 4%)
        val timeActive = (0.40 * epochDurationMs).toLong()
        val metricsActive = WaveMath.calculateAnomalyLifecycle(seed, timeActive, epochDurationMs)
        assertEquals(AnomalyLifecyclePhase.ACTIVE_NEXUS, metricsActive.phase)
        assertTrue("Active Nexus height should be near nominal (0.95 to 1.05)", metricsActive.heightMultiplier in 0.95f..1.05f)
        assertTrue("Active Nexus radius should be near nominal (0.95 to 1.05)", metricsActive.radiusMultiplier in 0.95f..1.05f)
        assertTrue("Active Nexus summit should be near 850m", metricsActive.peakAltitudeMeters in 800f..900f)
        assertEquals(1.0f, metricsActive.mutationFluxMod, 1e-4f)
        assertEquals(1.0f, metricsActive.harvestYieldMod, 1e-4f)

        // 3. Destabilizing: 80% into epoch
        val timeDestabilizing = (0.80 * epochDurationMs).toLong()
        val metricsDestabilizing = WaveMath.calculateAnomalyLifecycle(seed, timeDestabilizing, epochDurationMs)
        assertEquals(AnomalyLifecyclePhase.DESTABILIZING, metricsDestabilizing.phase)
        assertTrue("Destabilizing height should bulge (> 1.0)", metricsDestabilizing.heightMultiplier > 1.0f)
        assertTrue("Destabilizing peak altitude should exceed 850m", metricsDestabilizing.peakAltitudeMeters > 850f)
        assertEquals(2.0f, metricsDestabilizing.mutationFluxMod, 1e-4f)
        assertEquals(2.0f, metricsDestabilizing.harvestYieldMod, 1e-4f)

        // 4. Void Collapse: 95% into epoch
        val timeCollapse = (0.95 * epochDurationMs).toLong()
        val metricsCollapse = WaveMath.calculateAnomalyLifecycle(seed, timeCollapse, epochDurationMs)
        assertEquals(AnomalyLifecyclePhase.VOID_COLLAPSE, metricsCollapse.phase)
        assertTrue("Void collapse height should implode (< 0.35)", metricsCollapse.heightMultiplier < 0.35f)
        assertTrue("Void collapse radius should contract (< 0.60)", metricsCollapse.radiusMultiplier < 0.60f)
        assertEquals(3.0f, metricsCollapse.mutationFluxMod, 1e-4f)
        assertEquals(0.20f, metricsCollapse.harvestYieldMod, 1e-4f)
    }

    @Test
    fun testLifecycleModuloDeterminism() {
        val seed = 42891724L
        val t0 = 1750000000000L
        val metrics1 = WaveMath.calculateAnomalyLifecycle(seed, t0, epochDurationMs)
        val metrics2 = WaveMath.calculateAnomalyLifecycle(seed, t0, epochDurationMs)

        // Deterministic identical results
        assertEquals(metrics1.phase, metrics2.phase)
        assertEquals(metrics1.progress, metrics2.progress, 1e-5f)
        assertEquals(metrics1.peakAltitudeMeters, metrics2.peakAltitudeMeters, 1e-5f)

        // Modulo wrapping after full epoch
        val tNextEpoch = t0 + epochDurationMs
        val metricsNextEpoch = WaveMath.calculateAnomalyLifecycle(seed, tNextEpoch, epochDurationMs)
        assertEquals(metrics1.phase, metricsNextEpoch.phase)
        assertEquals(metrics1.progress, metricsNextEpoch.progress, 1e-4f)
    }

    @Test
    fun testUpdateAnomalyWithLifecycle() {
        val baseAnomaly = PoxAnomaly(
            id = "ANM-TEST",
            name = "Test Anomaly",
            lat = 37.7749,
            lng = -122.4194,
            gene = "XZYW?!$%",
            faction = "Anomaly",
            distance = 500.0,
            heatZoneDiameter = 5000.0,
        )

        val updated = WaveMath.updateAnomalyWithLifecycle(baseAnomaly, 1000000L, epochDurationMs)
        assertTrue("Lifecycle phase should be set", updated.lifecyclePhase in AnomalyLifecyclePhase.values())
        assertTrue("Progress should be between 0.0 and 1.0", updated.lifecycleProgress in 0f..1f)
        assertTrue("Peak altitude should be positive", updated.peakAltitudeMeters > 0f)
    }

    @Test
    fun testExactSecondPhaseCountdownTicks() {
        val seed = 12345L
        val t0 = 10000000L
        val m0 = WaveMath.calculateAnomalyLifecycle(seed, t0, epochDurationMs)
        val m1 = WaveMath.calculateAnomalyLifecycle(seed, t0 + 1000L, epochDurationMs)
        assertEquals(m0.phase, m1.phase)
        assertEquals("timeRemainingMs must decrease by exactly 1000ms after 1000ms passes", 1000L, m0.timeRemainingMs - m1.timeRemainingMs)
    }

    @Test
    fun testDeploymentEngineLifecycleModifiers() {
        // Standard active resistance
        val stallDepthActive = DeploymentEngine.calculateStallDepth(
            effectiveDefense = 50,
            boundaryRadius = 1000.0,
            lunarAge = 14.76, // Full Moon
            forcefieldResistanceMod = 1.0,
        )

        // Destabilizing increased resistance (harder to pierce)
        val stallDepthDestabilizing = DeploymentEngine.calculateStallDepth(
            effectiveDefense = 50,
            boundaryRadius = 1000.0,
            lunarAge = 14.76,
            forcefieldResistanceMod = 1.35,
        )
        assertTrue("Higher resistance should reduce stall depth", stallDepthDestabilizing < stallDepthActive)

        // Mutation interval: higher flux mod should shorten interval (faster mutations)
        val intervalStandard = DeploymentEngine.calculateMutationInterval(
            stalledDepth = 50.0,
            lunarAge = 14.76,
            mutationFluxMod = 1.0,
        )
        val intervalDestabilizing = DeploymentEngine.calculateMutationInterval(
            stalledDepth = 50.0,
            lunarAge = 14.76,
            mutationFluxMod = 2.0,
        )
        assertTrue("Destabilizing mutation flux should trigger faster mutations (lower interval)", intervalDestabilizing <= intervalStandard)
    }

    @Test
    fun testEmittersScaleWithLifecycleMetrics() {
        val baseEmitters = MetaballMeshCompiler.generateProceduralEmitters(
            centerX = 0f,
            centerY = 0f,
            totalRadius = 1000f,
            seed = 123L,
            heightScale = 1.0f,
            radiusScale = 1.0f,
        )

        val collapsedEmitters = MetaballMeshCompiler.generateProceduralEmitters(
            centerX = 0f,
            centerY = 0f,
            totalRadius = 1000f,
            seed = 123L,
            heightScale = 0.1f,
            radiusScale = 0.2f,
        )

        val bulgedEmitters = MetaballMeshCompiler.generateProceduralEmitters(
            centerX = 0f,
            centerY = 0f,
            totalRadius = 1000f,
            seed = 123L,
            heightScale = 1.25f,
            radiusScale = 1.1f,
        )

        assertTrue("Collapsed core height should be smaller than base", collapsedEmitters[0].radiusZ < baseEmitters[0].radiusZ)
        assertTrue("Collapsed core radius should be smaller than base", collapsedEmitters[0].radiusXy < baseEmitters[0].radiusXy)
        assertTrue("Bulged core height should be larger than base", bulgedEmitters[0].radiusZ > baseEmitters[0].radiusZ)
    }

    @Test
    fun testCalculateAnomalyLifecycleForProgress() {
        val mInception = WaveMath.calculateAnomalyLifecycleForProgress(0.05f)
        assertEquals(AnomalyLifecyclePhase.INCEPTION, mInception.phase)

        val mActive = WaveMath.calculateAnomalyLifecycleForProgress(0.40f)
        assertEquals(AnomalyLifecyclePhase.ACTIVE_NEXUS, mActive.phase)

        val mDestabilizing = WaveMath.calculateAnomalyLifecycleForProgress(0.80f)
        assertEquals(AnomalyLifecyclePhase.DESTABILIZING, mDestabilizing.phase)

        val mCollapse = WaveMath.calculateAnomalyLifecycleForProgress(0.95f)
        assertEquals(AnomalyLifecyclePhase.VOID_COLLAPSE, mCollapse.phase)
    }

    @Test
    fun testUpdateAnomalyWithLifecycleMetrics() {
        val baseAnomaly = PoxAnomaly(
            id = "ANM-TEST-SPHERE",
            name = "Test Anomaly Sphere",
            lat = 37.4220,
            lng = -122.0841,
            gene = "XZYW?!$%",
            faction = "Anomaly",
            distance = 100.0,
            heatZoneDiameter = 2000.0,
        )
        val metrics = WaveMath.calculateAnomalyLifecycleForProgress(0.80f)
        val updated = WaveMath.updateAnomalyWithLifecycleMetrics(baseAnomaly, metrics)

        assertEquals(AnomalyLifecyclePhase.DESTABILIZING, updated.lifecyclePhase)
        assertEquals(metrics.peakAltitudeMeters, updated.peakAltitudeMeters, 1e-4f)
        assertEquals(2.0f, updated.mutationFluxMod, 1e-4f)
        assertEquals(2.0f, updated.harvestYieldMod, 1e-4f)
    }

    @Test
    fun test60SecondAcceleratedLifecycleDemoLoop() {
        val demoDurationMs = 60_000L

        // 1. At 0s: Inception start (0% of 60s), phase remaining = 9s
        val t0 = 0L
        val m0 = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = t0, epochDurationMs = demoDurationMs)
        assertEquals(AnomalyLifecyclePhase.INCEPTION, m0.phase)
        assertEquals(9_000L, m0.timeRemainingMs)
        assertEquals(0.10f * 850f, m0.peakAltitudeMeters, 1f)

        // 2. At 9s: Active Nexus start (15% of 60s), phase remaining = 33s (42s - 9s)
        val t9 = 9_000L
        val m9 = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = t9, epochDurationMs = demoDurationMs)
        assertEquals(AnomalyLifecyclePhase.ACTIVE_NEXUS, m9.phase)
        assertEquals(33_000L, m9.timeRemainingMs)
        assertEquals(850f, m9.peakAltitudeMeters, 1e-4f)

        // 3. At 42s: Destabilizing start (70% of 60s), phase remaining = 12s (54s - 42s)
        val t42 = 42_000L
        val m42 = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = t42, epochDurationMs = demoDurationMs)
        assertEquals(AnomalyLifecyclePhase.DESTABILIZING, m42.phase)
        assertEquals(12_000L, m42.timeRemainingMs)
        assertTrue(m42.peakAltitudeMeters >= 850f)

        // 4. At 54s: Void Collapse start (90% of 60s), phase remaining = 6s (60s - 54s)
        val t54 = 54_000L
        val m54 = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = t54, epochDurationMs = demoDurationMs)
        assertEquals(AnomalyLifecyclePhase.VOID_COLLAPSE, m54.phase)
        assertEquals(6_000L, m54.timeRemainingMs)
        assertEquals(850f, m54.peakAltitudeMeters, 1e-4f)

        // 5. At 60s: Modulo wraps back to Inception
        val t60 = 60_000L
        val m60 = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = t60, epochDurationMs = demoDurationMs)
        assertEquals(AnomalyLifecyclePhase.INCEPTION, m60.phase)
        assertEquals(9_000L, m60.timeRemainingMs)
    }

    @Test
    fun testPhaseBoundaryContinuity() {
        val duration = 60_000L

        // Boundary 1: Inception (15% - epsilon) -> Active Nexus (15%)
        val mInceptionEnd = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = 8_999L, epochDurationMs = duration)
        val mActiveStart = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = 9_000L, epochDurationMs = duration)
        assertEquals(1.0f, mInceptionEnd.heightMultiplier, 0.01f)
        assertEquals(1.0f, mActiveStart.heightMultiplier, 0.01f)
        assertEquals(1.0f, mInceptionEnd.radiusMultiplier, 0.01f)
        assertEquals(1.0f, mActiveStart.radiusMultiplier, 0.01f)

        // Boundary 2: Active Nexus (70% - epsilon) -> Destabilizing (70%)
        val mActiveEnd = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = 41_999L, epochDurationMs = duration)
        val mDestabilizingStart = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = 42_000L, epochDurationMs = duration)
        assertEquals(1.0f, mActiveEnd.heightMultiplier, 0.01f)
        assertEquals(1.0f, mDestabilizingStart.heightMultiplier, 0.01f)
        assertEquals(1.0f, mActiveEnd.radiusMultiplier, 0.01f)
        assertEquals(1.0f, mDestabilizingStart.radiusMultiplier, 0.01f)

        // Boundary 3: Destabilizing (90% - epsilon) -> Void Collapse (90%)
        val mDestabilizingEnd = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = 53_999L, epochDurationMs = duration)
        val mCollapseStart = WaveMath.calculateAnomalyLifecycle(seed = 0L, timeMs = 54_000L, epochDurationMs = duration)
        assertEquals(1.0f, mDestabilizingEnd.heightMultiplier, 0.01f)
        assertEquals(1.0f, mCollapseStart.heightMultiplier, 0.01f)
        assertEquals(1.0f, mDestabilizingEnd.radiusMultiplier, 0.01f)
        assertEquals(1.0f, mCollapseStart.radiusMultiplier, 0.01f)
    }
}

package com.example.genpox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AnomalySpawningEngineTest {

    private val testSectorLat = 37.7749
    private val testSectorLng = -122.4194

    @Test
    fun testDeterminism_sameInputsProduceIdenticalAnomalies() {
        val roads = listOf(
            listOf(RoadPoint(37.7740, -122.4200), RoadPoint(37.7750, -122.4200), RoadPoint(37.7760, -122.4200)),
            listOf(RoadPoint(37.7750, -122.4210), RoadPoint(37.7750, -122.4200), RoadPoint(37.7750, -122.4190)),
        )
        val buildings = listOf(
            BuildingStructure(
                points = listOf(
                    RoadPoint(37.7742, -122.4198),
                    RoadPoint(37.7748, -122.4198),
                    RoadPoint(37.7748, -122.4192),
                    RoadPoint(37.7742, -122.4192),
                ),
                heightMeters = 45f,
            ),
        )

        val run1 = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = testSectorLat,
            sectorLng = testSectorLng,
            roads = roads,
            buildings = buildings,
            timeMs = 100000L,
        )
        val run2 = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = testSectorLat,
            sectorLng = testSectorLng,
            roads = roads,
            buildings = buildings,
            timeMs = 100000L,
        )

        assertEquals(run1.size, run2.size)
        for (i in run1.indices) {
            val a1 = run1[i]
            val a2 = run2[i]
            assertEquals(a1.id, a2.id)
            assertEquals(a1.lat, a2.lat, 1e-6)
            assertEquals(a1.lng, a2.lng, 1e-6)
            assertEquals(a1.morphology, a2.morphology)
            assertEquals(a1.spectralClass, a2.spectralClass)
            assertEquals(a1.heatZoneDiameter, a2.heatZoneDiameter, 1e-3)
            assertEquals(a1.peakAltitudeMeters, a2.peakAltitudeMeters, 1e-3f)
        }
    }

    @Test
    fun testMorphologyEvaluation_denseTallClusterYieldsSpike() {
        // High coverage and tall buildings
        val morphology = AnomalySpawningEngine.evaluateMorphology(
            buildingCoverageRatio = 0.35,
            averageBuildingHeight = 45.0f,
        )
        assertEquals(AnomalyMorphology.SPIKE, morphology)
    }

    @Test
    fun testMorphologyEvaluation_sparseLowProfileYieldsSprawl() {
        // Low coverage and suburban 1-story buildings
        val morphology = AnomalySpawningEngine.evaluateMorphology(
            buildingCoverageRatio = 0.05,
            averageBuildingHeight = 4.0f,
        )
        assertEquals(AnomalyMorphology.SPRAWL, morphology)
    }

    @Test
    fun testPoissonDiskExclusion_anomaliesDoNotOverlap() {
        // Provide dense intersecting roads that offer multiple candidate spawn points
        val roads = mutableListOf<List<RoadPoint>>()
        for (i in 0..6) {
            val latOffset = i * 0.001
            roads.add(listOf(RoadPoint(testSectorLat + latOffset, testSectorLng - 0.005), RoadPoint(testSectorLat + latOffset, testSectorLng + 0.005)))
        }
        for (j in 0..6) {
            val lngOffset = j * 0.001
            roads.add(listOf(RoadPoint(testSectorLat - 0.005, testSectorLng + lngOffset), RoadPoint(testSectorLat + 0.005, testSectorLng + lngOffset)))
        }

        val anomalies = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = testSectorLat,
            sectorLng = testSectorLng,
            roads = roads,
            buildings = emptyList(),
            maxAnomalies = 4,
            minDistanceMultiplier = 1.2,
        )

        assertTrue(anomalies.isNotEmpty())

        for (i in 0 until anomalies.size) {
            for (j in (i + 1) until anomalies.size) {
                val a1 = anomalies[i]
                val a2 = anomalies[j]
                val r1Meters = a1.heatZoneDiameter / 3.28084 / 2.0
                val r2Meters = a2.heatZoneDiameter / 3.28084 / 2.0
                val distMeters = GeoMath.calculateHaversineDistanceInFeet(a1.lat, a1.lng, a2.lat, a2.lng) * 0.3048

                val minSeparation = 1.2 * (r1Meters + r2Meters)
                assertTrue(
                    "Anomalies ${a1.id} and ${a2.id} violate Poisson distance: dist=$distMeters, minSeparation=$minSeparation",
                    distMeters >= minSeparation - 0.01, // allow minute numerical precision tolerance
                )
            }
        }
    }

    @Test
    fun testSpectralClassCeilingConsistency() {
        val anomalies = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = testSectorLat,
            sectorLng = testSectorLng,
            roads = emptyList<List<RoadPoint>>(),
            buildings = emptyList(),
        )

        assertTrue(anomalies.isNotEmpty())
        for (anom in anomalies) {
            val radiusMeters = (anom.heatZoneDiameter / 3.28084 / 2.0).toFloat()
            val expectedBaseCeiling = anom.spectralClass.calculateBaseCeiling(radiusMeters, anom.morphology)
            assertTrue(anom.peakAltitudeMeters > 0f)
            // Verify peak altitude is dynamically scaled by the lifecycle phase within [0.1x, 1.5x] of base ceiling
            assertTrue(
                "Peak altitude ${anom.peakAltitudeMeters} should be bounded by base ceiling $expectedBaseCeiling",
                anom.peakAltitudeMeters in (expectedBaseCeiling * 0.1f)..(expectedBaseCeiling * 1.5f),
            )
        }
    }

    @Test
    fun testFallbackGeneration_emptyInputsProduceValidAnomalies() {
        val anomalies = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = testSectorLat,
            sectorLng = testSectorLng,
            roads = emptyList<List<RoadPoint>>(),
            buildings = emptyList(),
            maxAnomalies = 2,
        )

        assertTrue(anomalies.isNotEmpty())
        assertTrue(anomalies.size in 1..2)
        for (anom in anomalies) {
            assertNotNull(anom.id)
            assertTrue(anom.heatZoneDiameter > 0.0)
            assertTrue(anom.peakAltitudeMeters > 0f)
            assertTrue(abs(anom.lat - testSectorLat) < 0.02)
            assertTrue(abs(anom.lng - testSectorLng) < 0.02)
        }
    }

    @Test
    fun testShoelaceBuildingAreaCalculation() {
        // 100m x 100m square building at equator
        val dDeg = 100.0 / 111000.0
        val squareBuilding = BuildingStructure(
            points = listOf(
                RoadPoint(0.0, 0.0),
                RoadPoint(dDeg, 0.0),
                RoadPoint(dDeg, dDeg),
                RoadPoint(0.0, dDeg),
            ),
            heightMeters = 20f,
        )

        val area = AnomalySpawningEngine.calculateBuildingFootprintArea(squareBuilding, cosLat = 1.0)
        // 100m * 100m = 10,000 sq meters
        assertEquals(10000.0, area, 50.0)
    }

    @Test
    fun testSectorGridAlignmentAndDeterministicCenterInvariance() {
        val cellSize = 0.015
        val cellX = 2518
        val cellY = -8161
        val centerLat = (cellX + 0.5) * cellSize
        val centerLng = (cellY + 0.5) * cellSize

        val roads = listOf(
            listOf(RoadPoint(centerLat - 0.002, centerLng), RoadPoint(centerLat + 0.002, centerLng)),
            listOf(RoadPoint(centerLat, centerLng - 0.002), RoadPoint(centerLat, centerLng + 0.002)),
        )

        val run1 = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = centerLat,
            sectorLng = centerLng,
            roads = roads,
            timeMs = 500000L,
        )
        val run2 = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = centerLat,
            sectorLng = centerLng,
            roads = roads,
            timeMs = 500000L,
        )

        assertEquals(run1.size, run2.size)
        for (i in run1.indices) {
            assertEquals(run1[i].id, run2[i].id)
            assertEquals(run1[i].lat, run2[i].lat, 1e-9)
            assertEquals(run1[i].lng, run2[i].lng, 1e-9)
            assertEquals(run1[i].heatZoneDiameter, run2[i].heatZoneDiameter, 1e-6)
        }
    }

    @Test
    fun testComputeSectorSeedMatches015Grid() {
        val lat = 37.7749
        val lng = -122.4194
        val seed1 = AnomalySpawningEngine.computeSectorSeed(lat, lng)
        // Slightly different camera pan inside same 0.015 deg cell
        val seed2 = AnomalySpawningEngine.computeSectorSeed(lat + 0.002, lng + 0.002)
        assertEquals(seed1, seed2)
    }

    @Test
    fun testGlobalCrossSectorExclusionPreventsOverlap() {
        val sectorALat = 37.7749
        val sectorALng = -122.4194
        val sectorBLat = sectorALat + 0.005 // ~555m away (adjacent candidate area)
        val sectorBLng = sectorALng

        // Pre-existing anomaly in Sector A with a 200m radius (diameter = 400m * 3.28084 feet)
        val existingAnomaly = PoxAnomaly(
            id = "ANM-EXISTING",
            name = "Existing Sector A Anomaly",
            lat = sectorALat,
            lng = sectorALng,
            gene = "ACGTACGT",
            faction = "Anomaly",
            distance = 0.0,
            heatZoneDiameter = 400.0 * 3.28084,
            density = 0.0,
        )

        // Try to spawn in Sector B with candidate right near Sector A's anomaly
        val candidateRoads = listOf(
            listOf(RoadPoint(sectorBLat - 0.001, sectorBLng), RoadPoint(sectorBLat + 0.001, sectorBLng)),
            listOf(RoadPoint(sectorBLat, sectorBLng - 0.001), RoadPoint(sectorBLat, sectorBLng + 0.001)),
        )

        val spawned = AnomalySpawningEngine.generateAnomaliesForSector(
            sectorLat = sectorBLat,
            sectorLng = sectorBLng,
            roads = candidateRoads,
            existingAnomalies = listOf(existingAnomaly),
            maxAnomalies = 1,
        )

        // Any spawned anomaly in Sector B must be at least 750m away from existingAnomaly
        for (anom in spawned) {
            val distMeters = GeoMath.calculateHaversineDistanceInFeet(anom.lat, anom.lng, existingAnomaly.lat, existingAnomaly.lng) * 0.3048
            assertTrue(
                "Spawned anomaly ${anom.id} at dist=$distMeters is within 750m exclusion zone of existing anomaly",
                distMeters >= 750.0 - 1.0,
            )
        }
    }

    @Test
    fun testSpectralClassRealisticDistribution_standardIntersectionYieldsLocalizedClass() {
        // Standard suburban 4-way intersection (degree 4) with normal 100m clearance
        val spectralClass = AnomalySpawningEngine.determineSpectralClass(
            connectivityScore = 2.5,
            spatialClearanceMeters = 100.0,
            intersectionDegree = 4,
            morphology = AnomalyMorphology.SPRAWL,
            pointSeed = 12345L,
        )
        // Must NOT be Class O, B, or A
        assertTrue(
            "Standard intersection should produce localized Class M, K, or G; got: $spectralClass",
            spectralClass == AnomalySpectralClass.CLASS_K ||
                spectralClass == AnomalySpectralClass.CLASS_G ||
                spectralClass == AnomalySpectralClass.CLASS_M,
        )
    }
}

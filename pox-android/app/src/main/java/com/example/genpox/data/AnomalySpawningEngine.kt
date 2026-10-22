package com.example.genpox.data

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Data-driven engine for determining anomaly placement, morphology (Spike vs. Sprawl),
 * and physical scale (Spectral Class M through O) based strictly on geographic,
 * topological, and structural map analysis.
 */
object AnomalySpawningEngine {

    private const val METERS_PER_DEG_LAT = 111000.0
    private const val FEET_PER_METER = 3.28084
    private const val DEFAULT_CELL_SIZE_DEG = 0.015 // ~1.66 km sector window

    /**
     * Internal candidate spawn point with topological scoring metadata.
     */
    data class CandidateSpawnPoint(
        val lat: Double,
        val lng: Double,
        val intersectionDegree: Int = 0,
        val localBuildingCount: Int = 0,
        val localBuildingCoverageRatio: Double = 0.0,
        val averageBuildingHeight: Float = 0f,
        val spatialClearanceMeters: Double = 50.0,
        val connectivityScore: Double = 0.0,
    )

    /**
     * Overload supporting Pair<Double, Double> coordinate paths for seamless UI ViewModel integration.
     */
    @JvmName("generateAnomaliesForSectorPairs")
    fun generateAnomaliesForSector(
        sectorLat: Double,
        sectorLng: Double,
        roads: List<List<Pair<Double, Double>>>,
        buildings: List<BuildingStructure> = emptyList(),
        existingAnomalies: List<PoxAnomaly> = emptyList(),
        seed: Long = computeSectorSeed(sectorLat, sectorLng),
        maxAnomalies: Int = 1,
        minDistanceMultiplier: Double = 1.25,
        timeMs: Long = WaveMath.getAdjustedTime(),
    ): List<PoxAnomaly> {
        val roadPoints = roads.map { way -> way.map { RoadPoint(it.first, it.second) } }
        return generateAnomaliesForSector(
            sectorLat = sectorLat,
            sectorLng = sectorLng,
            roads = roadPoints,
            buildings = buildings,
            existingAnomalies = existingAnomalies,
            seed = seed,
            maxAnomalies = maxAnomalies,
            minDistanceMultiplier = minDistanceMultiplier,
            timeMs = timeMs,
        )
    }

    /**
     * Generates a deterministic list of anomalies for a given sector coordinate window,
     * using the supplied road network and building structure geometry.
     */
    fun generateAnomaliesForSector(
        sectorLat: Double,
        sectorLng: Double,
        roads: List<List<RoadPoint>> = emptyList(),
        buildings: List<BuildingStructure> = emptyList(),
        existingAnomalies: List<PoxAnomaly> = emptyList(),
        seed: Long = computeSectorSeed(sectorLat, sectorLng),
        maxAnomalies: Int = 1,
        minDistanceMultiplier: Double = 1.25,
        timeMs: Long = WaveMath.getAdjustedTime(),
    ): List<PoxAnomaly> {
        val candidates = extractCandidateSpawnPoints(
            sectorLat = sectorLat,
            sectorLng = sectorLng,
            roads = roads,
            buildings = buildings,
            seed = seed,
        )

        if (candidates.isEmpty()) return emptyList()

        val selected = mutableListOf<PoxAnomaly>()

        for (candidate in candidates) {
            if (selected.size >= maxAnomalies) break

            val morphology = evaluateMorphology(
                buildingCoverageRatio = candidate.localBuildingCoverageRatio,
                averageBuildingHeight = candidate.averageBuildingHeight,
            )

            val pSeed = candidatePointSeed(candidate.lat, candidate.lng, seed)

            val spectralClass = determineSpectralClass(
                connectivityScore = candidate.connectivityScore,
                spatialClearanceMeters = candidate.spatialClearanceMeters,
                intersectionDegree = candidate.intersectionDegree,
                morphology = morphology,
                pointSeed = pSeed,
            )

            val radiusMeters = calculateDeterministicRadius(
                spectralClass = spectralClass,
                morphology = morphology,
                pointSeed = pSeed,
            )

            val heatZoneDiameterFeet = radiusMeters * 2.0 * FEET_PER_METER
            val ceilingMeters = spectralClass.calculateBaseCeiling(radiusMeters, morphology)

            // Global Poisson-disk spatial exclusion check:
            // Compare against both newly selected anomalies AND all existing anomalies in the world/neighboring sectors.
            val allExisting = selected + existingAnomalies
            var isOccluded = false
            for (existing in allExisting) {
                val existingRadiusMeters = (existing.heatZoneDiameter / FEET_PER_METER / 2.0)
                val distMeters = GeoMath.calculateHaversineDistanceInFeet(
                    candidate.lat,
                    candidate.lng,
                    existing.lat,
                    existing.lng,
                ) * (1.0 / FEET_PER_METER)

                // Minimum hard buffer of 750m between centers, plus scaled radii clearance
                val minAllowedDistance = max(750.0, minDistanceMultiplier * (radiusMeters + existingRadiusMeters))
                if (distMeters < minAllowedDistance) {
                    isOccluded = true
                    break
                }
            }

            if (isOccluded) continue

            val pointHash = candidatePointSeed(candidate.lat, candidate.lng, seed).hashCode() and 0x7FFFFFFF
            val gene = generateDeterministicGene(pointHash)
            val baseDensity = ((pointHash % 50) - 25) / 100.0 // [-0.25, 0.24]

            val morphologyName = if (morphology == AnomalyMorphology.SPIKE) "Spike" else "Sprawl"
            val anomalyId = "ANM-${spectralClass.code}-${morphology.name}-${pointHash % 10000}"
            val anomalyName = "Class ${spectralClass.code} $morphologyName"

            val baseAnomaly = PoxAnomaly(
                id = anomalyId,
                name = anomalyName,
                lat = candidate.lat,
                lng = candidate.lng,
                gene = gene,
                faction = "Anomaly",
                distance = 0.0,
                heatZoneDiameter = heatZoneDiameterFeet,
                density = baseDensity,
                morphology = morphology,
                peakAltitudeMeters = ceilingMeters,
                spectralClass = spectralClass,
            )

            val resolvedAnomaly = WaveMath.updateAnomalyWithLifecycle(baseAnomaly, timeMs)
            selected.add(resolvedAnomaly)
        }

        return selected
    }

    /**
     * Evaluates whether an anomaly should form as a SPIKE or a SPRAWL based on
     * the local building coverage ratio and average building height.
     */
    fun evaluateMorphology(
        buildingCoverageRatio: Double,
        averageBuildingHeight: Float,
    ): AnomalyMorphology {
        // High density / tall building structures favor a Spike (narrow horizontal footprint, towering summit).
        // Low density / low-profile suburban or open terrain favors a Sprawl.
        val heightFactor = (averageBuildingHeight / 25.0f).coerceIn(0.0f, 2.0f).toDouble()
        val score = buildingCoverageRatio * 1.5 + heightFactor * 0.5

        return if (score >= 0.40 || averageBuildingHeight >= 30.0f) {
            AnomalyMorphology.SPIKE
        } else {
            AnomalyMorphology.SPRAWL
        }
    }

    /**
     * Determines the Spectral Class (Class M through Class O) based on road connectivity,
     * spatial clearance, intersection degree, and deterministic seed percentile.
     */
    fun determineSpectralClass(
        connectivityScore: Double,
        spatialClearanceMeters: Double,
        intersectionDegree: Int,
        morphology: AnomalyMorphology,
        pointSeed: Long = 0L,
    ): AnomalySpectralClass {
        val roll = if (pointSeed != 0L) (kotlin.math.abs(pointSeed.hashCode()) % 1000) else 500

        // Class O (Metropolitan Biosphere Titan, 2500m-4000m radius): Apex singular regional phenomenon (0.2% chance)
        if (intersectionDegree >= 6 && spatialClearanceMeters >= 1500.0 && connectivityScore >= 4.0 && roll < 2) {
            return AnomalySpectralClass.CLASS_O
        }
        // Class B (Metro Core Megastructure, 1500m-2500m radius): Legendary metropolitan core (0.8% chance)
        if (intersectionDegree >= 5 && spatialClearanceMeters >= 1000.0 && connectivityScore >= 3.2 && roll < 10) {
            return AnomalySpectralClass.CLASS_B
        }
        // Class A (Township Basin Monolith, 1000m-1500m radius): Monumental event (2% chance)
        if (intersectionDegree >= 4 && spatialClearanceMeters >= 600.0 && connectivityScore >= 2.8 && roll < 30) {
            return AnomalySpectralClass.CLASS_A
        }
        // Class F (Inter-District Corridor, 500m-1000m radius): Major regional hub (5% chance)
        if (intersectionDegree >= 4 && spatialClearanceMeters >= 350.0 && connectivityScore >= 2.2 && roll < 80) {
            return AnomalySpectralClass.CLASS_F
        }
        // Class G (Urban District Ward, 300m-500m radius): High-density multi-way intersection
        if (intersectionDegree >= 3 && spatialClearanceMeters >= 150.0 && connectivityScore >= 1.8) {
            return AnomalySpectralClass.CLASS_G
        }
        // Class K (Block-Scale Bio-Sprawl, 150m-300m radius): Standard neighborhood intersection
        if (intersectionDegree >= 2 && connectivityScore >= 1.0) {
            return AnomalySpectralClass.CLASS_K
        }
        // Class M (Alleyway Conduit, 25m-100m radius): Default local street anomaly
        return AnomalySpectralClass.CLASS_M
    }

    /**
     * Extracts scored candidate coordinates by analyzing road intersections,
     * building clusters, and open space clearings.
     */
    /**
     * Internal precomputed structure metadata for O(1) spatial queries without allocations.
     */
    data class PrecomputedBuilding(
        val cx: Double,
        val cy: Double,
        val area: Double,
        val height: Float,
    )

    /**
     * Extracts scored candidate coordinates by analyzing road intersections,
     * building clusters, and open space clearings.
     */
    fun extractCandidateSpawnPoints(
        sectorLat: Double,
        sectorLng: Double,
        roads: List<List<RoadPoint>>,
        buildings: List<BuildingStructure>,
        seed: Long,
    ): List<CandidateSpawnPoint> {
        val candidates = mutableListOf<CandidateSpawnPoint>()
        val cosLat = cos(Math.toRadians(sectorLat)).coerceAtLeast(0.01)
        val latMeters = METERS_PER_DEG_LAT
        val lngMeters = METERS_PER_DEG_LAT * cosLat
        val bldgCellMeters = 250.0

        // Precompute building centroids, areas, and heights once
        val precomputed = ArrayList<PrecomputedBuilding>(buildings.size)
        val bldgGrid = HashMap<Pair<Long, Long>, MutableList<PrecomputedBuilding>>()

        for (b in buildings) {
            val c = calculateBuildingCentroid(b)
            if (c.lat != 0.0 || c.lng != 0.0) {
                val area = calculateBuildingFootprintArea(b, cosLat)
                val h = if (b.heightMeters > 0f) b.heightMeters else 4.0f
                val pb = PrecomputedBuilding(c.lat, c.lng, area, h)
                precomputed.add(pb)
                val gx = (c.lng * lngMeters / bldgCellMeters).toLong()
                val gy = (c.lat * latMeters / bldgCellMeters).toLong()
                bldgGrid.getOrPut(Pair(gx, gy)) { ArrayList() }.add(pb)
            }
        }

        fun getBuildingsNear(lat: Double, lng: Double, radiusMeters: Double): List<PrecomputedBuilding> {
            val list = mutableListOf<PrecomputedBuilding>()
            val centerGx = (lng * lngMeters / bldgCellMeters).toLong()
            val centerGy = (lat * latMeters / bldgCellMeters).toLong()
            val rCells = (radiusMeters / bldgCellMeters).toInt() + 1
            for (dx in -rCells..rCells) {
                for (dy in -rCells..rCells) {
                    val bucket = bldgGrid[Pair(centerGx + dx, centerGy + dy)] ?: continue
                    for (b in bucket) {
                        if (distanceMeters(lat, lng, b.cx, b.cy, cosLat) <= radiusMeters) {
                            list.add(b)
                        }
                    }
                }
            }
            return list
        }

        fun getClearanceNear(lat: Double, lng: Double, maxCheckMeters: Double = 600.0): Double {
            val near = getBuildingsNear(lat, lng, maxCheckMeters)
            if (near.isEmpty()) return maxCheckMeters
            var minD = maxCheckMeters
            for (b in near) {
                val d = distanceMeters(lat, lng, b.cx, b.cy, cosLat)
                if (d < minD) minD = d
            }
            return max(15.0, minD)
        }

        fun getCoverageRatioNear(localBldgs: List<PrecomputedBuilding>, probeRadiusMeters: Double): Double {
            if (localBldgs.isEmpty()) return 0.0
            val circleArea = Math.PI * probeRadiusMeters * probeRadiusMeters
            var sumArea = 0.0
            for (b in localBldgs) sumArea += b.area
            return (sumArea / circleArea).coerceIn(0.0, 1.0)
        }

        fun getAvgHeight(localBldgs: List<PrecomputedBuilding>): Float {
            if (localBldgs.isEmpty()) return 0f
            var sumH = 0f
            for (b in localBldgs) sumH += b.height
            return sumH / localBldgs.size
        }

        // 1. Road Intersection Analysis: Take top 10 highest-degree intersections
        val roadIntersections = findRoadIntersections(roads, cosLat)
        val sortedIntersections = roadIntersections.entries
            .sortedWith(
                compareByDescending<Map.Entry<RoadPoint, Int>> { it.value }
                    .thenBy { it.key.lat }
                    .thenBy { it.key.lng },
            )
            .take(10)

        for ((point, degree) in sortedIntersections) {
            val localBldgs = getBuildingsNear(point.lat, point.lng, 250.0)
            val coverage = getCoverageRatioNear(localBldgs, 250.0)
            val avgHeight = getAvgHeight(localBldgs)
            val clearance = getClearanceNear(point.lat, point.lng)
            val connectivity = (degree.toDouble() / 2.0) + (roads.size.toDouble() / 40.0).coerceAtMost(1.5)

            candidates.add(
                CandidateSpawnPoint(
                    lat = point.lat,
                    lng = point.lng,
                    intersectionDegree = degree,
                    localBuildingCount = localBldgs.size,
                    localBuildingCoverageRatio = coverage,
                    averageBuildingHeight = avgHeight,
                    spatialClearanceMeters = clearance,
                    connectivityScore = connectivity,
                ),
            )
        }

        // 2. Building Cluster Analysis (Centroids of dense structures)
        if (precomputed.isNotEmpty()) {
            val sampleStep = max(1, precomputed.size / 4)
            for (i in precomputed.indices step sampleStep) {
                val bldg = precomputed[i]
                val isNearExisting = candidates.any {
                    distanceMeters(bldg.cx, bldg.cy, it.lat, it.lng, cosLat) < 60.0
                }
                if (isNearExisting) continue

                val localBldgs = getBuildingsNear(bldg.cx, bldg.cy, 250.0)
                val coverage = getCoverageRatioNear(localBldgs, 250.0)
                val avgHeight = getAvgHeight(localBldgs)
                val clearance = getClearanceNear(bldg.cx, bldg.cy)

                candidates.add(
                    CandidateSpawnPoint(
                        lat = bldg.cx,
                        lng = bldg.cy,
                        intersectionDegree = 1,
                        localBuildingCount = localBldgs.size,
                        localBuildingCoverageRatio = coverage,
                        averageBuildingHeight = avgHeight,
                        spatialClearanceMeters = clearance,
                        connectivityScore = 0.5,
                    ),
                )
            }
        }

        // 3. Fallback Terrain Jitter (if insufficient data in rural or sparse sectors)
        if (candidates.size < 3) {
            val needed = 3 - candidates.size
            val quadrantOffsets = listOf(
                Pair(-0.30, -0.30),
                Pair(0.30, 0.30),
                Pair(-0.30, 0.30),
            )
            for (k in 0 until needed) {
                val qOffset = quadrantOffsets[k % quadrantOffsets.size]
                val jitterHash = (seed xor (k.toLong() * 9973L)).hashCode() and 0x7FFFFFFF
                val jLat = (((jitterHash % 1000) / 1000.0) - 0.5) * (DEFAULT_CELL_SIZE_DEG * 0.15)
                val jLng = ((((jitterHash / 1000) % 1000) / 1000.0) - 0.5) * (DEFAULT_CELL_SIZE_DEG * 0.15)
                val dLat = qOffset.first * DEFAULT_CELL_SIZE_DEG + jLat
                val dLng = qOffset.second * DEFAULT_CELL_SIZE_DEG + jLng
                val fLat = sectorLat + dLat
                val fLng = sectorLng + (dLng / cosLat)

                val localBldgs = getBuildingsNear(fLat, fLng, 250.0)
                val coverage = getCoverageRatioNear(localBldgs, 250.0)
                val avgHeight = getAvgHeight(localBldgs)
                val clearance = getClearanceNear(fLat, fLng)

                candidates.add(
                    CandidateSpawnPoint(
                        lat = fLat,
                        lng = fLng,
                        intersectionDegree = 0,
                        localBuildingCount = localBldgs.size,
                        localBuildingCoverageRatio = coverage,
                        averageBuildingHeight = avgHeight,
                        spatialClearanceMeters = clearance.coerceIn(50.0, 180.0),
                        connectivityScore = 0.05,
                    ),
                )
            }
        }

        // Sort candidates by strategic prominence score (connectivity + local density) descending with deterministic tie-breakers
        candidates.sortWith(
            compareByDescending<CandidateSpawnPoint> {
                it.connectivityScore * 2.0 + it.localBuildingCoverageRatio * 3.0 + (it.averageBuildingHeight / 15.0)
            }.thenBy { it.lat }.thenBy { it.lng },
        )

        return candidates
    }

    /**
     * Finds intersections across the road network using a 20m spatial hash grid.
     */
    fun findRoadIntersections(
        roads: List<List<RoadPoint>>,
        cosLat: Double,
        gridResolutionMeters: Double = 20.0,
    ): Map<RoadPoint, Int> {
        val grid = mutableMapOf<Pair<Long, Long>, MutableList<Pair<RoadPoint, Int>>>() // GridKey -> List<(Point, WayIndex)>

        val latMetersPerDeg = METERS_PER_DEG_LAT
        val lngMetersPerDeg = METERS_PER_DEG_LAT * cosLat

        roads.forEachIndexed { wayIdx, way ->
            val sampledIndices = mutableListOf<Int>()
            if (way.isNotEmpty()) sampledIndices.add(0)
            if (way.size > 1) sampledIndices.add(way.lastIndex)
            for (idx in 1 until way.lastIndex step 3) {
                sampledIndices.add(idx)
            }

            for (idx in sampledIndices) {
                val pt = way[idx]
                val gx = (pt.lng * lngMetersPerDeg / gridResolutionMeters).toLong()
                val gy = (pt.lat * latMetersPerDeg / gridResolutionMeters).toLong()
                val key = Pair(gx, gy)
                grid.getOrPut(key) { mutableListOf() }.add(Pair(pt, wayIdx))
            }
        }

        val intersections = mutableMapOf<RoadPoint, Int>()

        for ((_, entries) in grid) {
            val distinctWays = entries.map { it.second }.distinct()
            if (distinctWays.size >= 2) {
                var sumLat = 0.0
                var sumLng = 0.0
                entries.forEach {
                    sumLat += it.first.lat
                    sumLng += it.first.lng
                }
                val avgPoint = RoadPoint(sumLat / entries.size, sumLng / entries.size)
                intersections[avgPoint] = distinctWays.size
            }
        }

        return intersections
    }

    /**
     * Calculates the Building Coverage Ratio (packing fraction) within a probe radius:
     * Area_buildings / Area_circle.
     */
    fun calculateCoverageRatio(
        centerLat: Double,
        centerLng: Double,
        probeRadiusMeters: Double,
        buildings: List<BuildingStructure>,
        cosLat: Double,
    ): Double {
        if (buildings.isEmpty()) return 0.0
        val circleArea = Math.PI * probeRadiusMeters * probeRadiusMeters
        var totalBuildingArea = 0.0

        for (b in buildings) {
            totalBuildingArea += calculateBuildingFootprintArea(b, cosLat)
        }

        return (totalBuildingArea / circleArea).coerceIn(0.0, 1.0)
    }

    /**
     * Calculates the footprint area of a single building in square meters using the Shoelace formula.
     */
    fun calculateBuildingFootprintArea(building: BuildingStructure, cosLat: Double): Double {
        val pts = building.points
        if (pts.size < 3) return 0.0

        val refLat = pts[0].lat
        val refLng = pts[0].lng
        var area = 0.0

        for (i in pts.indices) {
            val j = (i + 1) % pts.size
            val xi = (pts[i].lng - refLng) * METERS_PER_DEG_LAT * cosLat
            val yi = (pts[i].lat - refLat) * METERS_PER_DEG_LAT
            val xj = (pts[j].lng - refLng) * METERS_PER_DEG_LAT * cosLat
            val yj = (pts[j].lat - refLat) * METERS_PER_DEG_LAT
            area += (xi * yj - xj * yi)
        }

        return abs(area) * 0.5
    }

    /**
     * Computes the centroid coordinate of a building structure.
     */
    fun calculateBuildingCentroid(building: BuildingStructure): RoadPoint {
        val pts = building.points
        if (pts.isEmpty()) return RoadPoint(0.0, 0.0)
        var sumLat = 0.0
        var sumLng = 0.0
        for (p in pts) {
            sumLat += p.lat
            sumLng += p.lng
        }
        return RoadPoint(sumLat / pts.size, sumLng / pts.size)
    }

    /**
     * Calculates the average building height in meters for a collection of structures.
     */
    fun calculateAverageHeight(buildings: List<BuildingStructure>): Float {
        if (buildings.isEmpty()) return 0f
        var totalHeight = 0f
        for (b in buildings) {
            totalHeight += if (b.heightMeters > 0f) b.heightMeters else 4.0f
        }
        return totalHeight / buildings.size
    }

    /**
     * Finds all buildings whose centroid is within probeRadiusMeters from (centerLat, centerLng).
     */
    fun findBuildingsInRadius(
        centerLat: Double,
        centerLng: Double,
        probeRadiusMeters: Double,
        buildings: List<BuildingStructure>,
        cosLat: Double,
    ): List<BuildingStructure> {
        if (buildings.isEmpty()) return emptyList()
        val result = mutableListOf<BuildingStructure>()
        for (b in buildings) {
            val centroid = calculateBuildingCentroid(b)
            if (distanceMeters(centerLat, centerLng, centroid.lat, centroid.lng, cosLat) <= probeRadiusMeters) {
                result.add(b)
            }
        }
        return result
    }

    /**
     * Estimates the spatial clearance around a coordinate by measuring the distance
     * to the nearest building or structural boundary.
     */
    fun calculateSpatialClearance(
        lat: Double,
        lng: Double,
        buildings: List<BuildingStructure>,
        cosLat: Double,
    ): Double {
        if (buildings.isEmpty()) return 800.0
        var minClearance = 800.0

        for (b in buildings) {
            val centroid = calculateBuildingCentroid(b)
            val dist = distanceMeters(lat, lng, centroid.lat, centroid.lng, cosLat)
            if (dist < minClearance) {
                minClearance = dist
            }
        }

        return max(15.0, minClearance)
    }

    /**
     * Deterministically calculates anomaly radius within the bounds of its assigned spectral class.
     */
    fun calculateDeterministicRadius(
        spectralClass: AnomalySpectralClass,
        morphology: AnomalyMorphology,
        pointSeed: Long,
    ): Float {
        val (minR, maxR) = if (morphology == AnomalyMorphology.SPIKE) {
            spectralClass.spikeMinRadius to spectralClass.spikeMaxRadius
        } else {
            spectralClass.sprawlMinRadius to spectralClass.sprawlMaxRadius
        }

        val fraction = ((pointSeed.hashCode() and 0x7FFFFFFF) % 1000) / 1000.0f
        return minR + fraction * (maxR - minR)
    }

    /**
     * Fast equirectangular distance calculation in meters.
     */
    fun distanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
        cosLat: Double = cos(Math.toRadians((lat1 + lat2) / 2.0)),
    ): Double {
        val dx = (lng2 - lng1) * METERS_PER_DEG_LAT * cosLat
        val dy = (lat2 - lat1) * METERS_PER_DEG_LAT
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Generates a 64-bit coordinate seed for a sector.
     */
    fun computeSectorSeed(lat: Double, lng: Double): Long {
        val cellX = Math.floor(lat / DEFAULT_CELL_SIZE_DEG).toLong()
        val cellY = Math.floor(lng / DEFAULT_CELL_SIZE_DEG).toLong()
        return (cellX shl 32) xor (cellY and 0xFFFFFFFFL)
    }

    /**
     * Generates a deterministic point seed from latitude, longitude, and sector seed.
     */
    private fun candidatePointSeed(lat: Double, lng: Double, sectorSeed: Long): Long {
        val latHash = (lat * 100000.0).toLong()
        val lngHash = (lng * 100000.0).toLong()
        return (latHash shl 16) xor lngHash xor sectorSeed
    }

    /**
     * Deterministically synthesizes an 8-character nucleotide sequence from a seed.
     */
    fun generateDeterministicGene(seed: Int): String {
        val bases = charArrayOf('A', 'C', 'G', 'T')
        var current = (seed and 0x7FFFFFFF)
        val sb = StringBuilder(8)
        for (i in 0 until 8) {
            sb.append(bases[current and 3])
            current = (current * 1664525 + 1013904223) and 0x7FFFFFFF
        }
        return sb.toString()
    }
}

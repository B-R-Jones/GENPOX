package com.example.genpox.ui.scanner.map

import com.example.genpox.data.AnomalyMorphology
import com.example.genpox.data.PoxAnomaly
import com.example.genpox.data.WaveMath
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Procedural Metaball Volumetric Generator for GENPOX Anomalies.
 *
 * Evaluates anisotropic implicit scalar density fields (horizontal sprawl with vertical ground-hugging profile)
 * and procedural bio-mass emitters for GPU rendering and gameplay interactions.
 */
object MetaballMeshCompiler {

    enum class AnomalyNodeType {
        CORE_SUMMIT,
        GROUND_LOBE,
        SATELLITE_SPIRE,
    }

    data class AnomalyNodalStructure(
        val anomalyId: String,
        val nodes: List<AnomalyNode>,
    )

    data class AnomalyNode(
        val index: Int,
        val type: AnomalyNodeType,
        val lat: Double,
        val lng: Double,
        val altitudeMeters: Float,
        val radiusXyMeters: Float,
        val radiusZMeters: Float,
        val weight: Float,
    )

    data class MetaballEmitter(
        val x: Float,
        val y: Float,
        val z: Float,
        val radiusXy: Float,
        val radiusZ: Float,
        val weight: Float = 1.0f,
    )

    /**
     * Resolves the geographic lat/lng coordinates and 3D altitude of all nodal emitters
     * within an anomaly's procedural bio-mass for gameplay targeting and harvesting systems.
     */
    fun resolveNodalStructure(
        anomaly: PoxAnomaly,
        timeMs: Long = WaveMath.getAdjustedTime(),
    ): AnomalyNodalStructure {
        val rawRadius = if (anomaly.heatZoneDiameter > 0) {
            (anomaly.heatZoneDiameter / 3.28084 / 2.0).toFloat()
        } else {
            1000.0f
        }
        val radiusMeters = rawRadius.coerceIn(2.5f, 4000.0f)
        val spectralClass = anomaly.spectralClass
        val baseSummitMeters = spectralClass.calculateBaseCeiling(radiusMeters, anomaly.morphology)
        val seed = (anomaly.id.hashCode().toLong() and 0xFFFFFFFFL) xor (anomaly.gene.hashCode().toLong() shl 32)
        val metrics = WaveMath.calculateAnomalyLifecycleForProgress(anomaly.lifecycleProgress, baseSummitMeters = baseSummitMeters)
        val emitters = generateProceduralEmitters(
            centerX = 0f,
            centerY = 0f,
            totalRadius = radiusMeters,
            seed = seed,
            heightScale = metrics.heightMultiplier,
            radiusScale = metrics.radiusMultiplier,
            baseCeilingMeters = baseSummitMeters,
            morphology = anomaly.morphology,
        )

        val cosLat = cos(Math.toRadians(anomaly.lat))
        val metersPerDegLat = 111000.0
        val metersPerDegLng = metersPerDegLat * cosLat

        val nodes = emitters.mapIndexed { idx, e ->
            val dLat = -(e.y / metersPerDegLat)
            val dLng = e.x / metersPerDegLng
            val nodeType = when {
                idx == 0 -> AnomalyNodeType.CORE_SUMMIT
                idx in 1..4 -> AnomalyNodeType.GROUND_LOBE
                else -> AnomalyNodeType.SATELLITE_SPIRE
            }
            AnomalyNode(
                index = idx,
                type = nodeType,
                lat = anomaly.lat + dLat,
                lng = anomaly.lng + dLng,
                altitudeMeters = e.z,
                radiusXyMeters = e.radiusXy,
                radiusZMeters = e.radiusZ,
                weight = e.weight,
            )
        }

        return AnomalyNodalStructure(anomaly.id, nodes)
    }

    /**
     * Checks if a 3D coordinate (latitude, longitude, altitude) is contained within
     * the anomaly's volumetric bio-mass field.
     */
    fun contains3D(
        anomaly: PoxAnomaly,
        targetLat: Double,
        targetLng: Double,
        targetAltitudeMeters: Float = 0f,
        densityThreshold: Float = 0.22f,
    ): Boolean {
        val cosLat = cos(Math.toRadians(anomaly.lat))
        val dx = ((targetLng - anomaly.lng) * cosLat * 111000.0).toFloat()
        val dy = (-(targetLat - anomaly.lat) * 111000.0).toFloat()
        val rawRadius = if (anomaly.heatZoneDiameter > 0) {
            (anomaly.heatZoneDiameter / 3.28084 / 2.0).toFloat()
        } else {
            1000.0f
        }
        val radiusMeters = rawRadius.coerceIn(2.5f, 4000.0f)
        val spectralClass = anomaly.spectralClass
        val baseSummitMeters = spectralClass.calculateBaseCeiling(radiusMeters, anomaly.morphology)
        val seed = (anomaly.id.hashCode().toLong() and 0xFFFFFFFFL) xor (anomaly.gene.hashCode().toLong() shl 32)
        val metrics = WaveMath.calculateAnomalyLifecycleForProgress(anomaly.lifecycleProgress, baseSummitMeters = baseSummitMeters)
        val emitters = generateProceduralEmitters(
            centerX = 0f,
            centerY = 0f,
            totalRadius = radiusMeters,
            seed = seed,
            heightScale = metrics.heightMultiplier,
            radiusScale = metrics.radiusMultiplier,
            baseCeilingMeters = baseSummitMeters,
            morphology = anomaly.morphology,
        )
        val density = evaluateDensity(dx, dy, targetAltitudeMeters, emitters)
        return density >= densityThreshold
    }

    /**
     * Evaluates Wyvill compact cubic falloff density field with anisotropic (horizontal vs vertical) radii:
     * d_norm^2 = (dx/R_xy)^2 + (dy/R_xy)^2 + (dz/R_z)^2
     * f(d) = (1 - d_norm^2)^3 for d_norm < 1, else 0.
     */
    fun evaluateDensity(x: Float, y: Float, z: Float, emitters: List<MetaballEmitter>): Float {
        var totalDensity = 0.0f
        for (i in emitters.indices) {
            val e = emitters[i]
            val dx = x - e.x
            val dy = y - e.y
            val dz = z - e.z
            val normD2 = (dx * dx + dy * dy) / (e.radiusXy * e.radiusXy) + (dz * dz) / (e.radiusZ * e.radiusZ)
            if (normD2 < 1.0f) {
                val q = 1.0f - normD2
                totalDensity += e.weight * (q * q * q)
            }
        }
        return totalDensity
    }

    /**
     * Computes the outward surface normal of the implicit scalar field at (x, y, z)
     * using the exact analytical gradient of the Wyvill cubic falloff polynomial:
     * grad f = -6 * weight * (1 - d_norm^2)^2 * (dx/rx^2, dy/ry^2, dz/rz^2)
     */
    fun evaluateNormal(x: Float, y: Float, z: Float, emitters: List<MetaballEmitter>): Triple<Float, Float, Float> {
        var gx = 0.0f
        var gy = 0.0f
        var gz = 0.0f
        for (i in emitters.indices) {
            val e = emitters[i]
            val dx = x - e.x
            val dy = y - e.y
            val dz = z - e.z
            val rx2 = e.radiusXy * e.radiusXy
            val rz2 = e.radiusZ * e.radiusZ
            val normD2 = (dx * dx + dy * dy) / rx2 + (dz * dz) / rz2
            if (normD2 < 1.0f) {
                val q = 1.0f - normD2
                val factor = -6.0f * e.weight * (q * q)
                gx += factor * (dx / rx2)
                gy += factor * (dy / rx2)
                gz += factor * (dz / rz2)
            }
        }
        val len = sqrt(gx * gx + gy * gy + gz * gz)
        return if (len > 1e-5f) {
            Triple(-gx / len, -gy / len, -gz / len)
        } else {
            Triple(0f, 0f, 1f)
        }
    }

    /**
     * Generates a deterministic cluster of metaball emitters centered at [centerX], [centerY]
     * modulated by [morphology] (horizontal SPRAWL vs vertical SPIKE).
     */
    fun generateProceduralEmitters(
        centerX: Float,
        centerY: Float,
        totalRadius: Float,
        seed: Long,
        heightScale: Float = 1.0f,
        radiusScale: Float = 1.0f,
        baseCeilingMeters: Float = 150.0f,
        morphology: AnomalyMorphology = AnomalyMorphology.SPRAWL,
    ): List<MetaballEmitter> {
        val rand = Random(seed)
        val emitters = ArrayList<MetaballEmitter>(7)

        val effectiveRadiusXy = totalRadius * radiusScale
        val maxVerticalHeight = baseCeilingMeters * heightScale

        // 1. Primary Core Summit Emitter (Apex of anomaly)
        val coreRadiusXy = effectiveRadiusXy * (0.45f + rand.nextFloat() * 0.10f)
        val coreRadiusZ = maxVerticalHeight * (0.80f + rand.nextFloat() * 0.20f)
        val coreZ = coreRadiusZ * 0.55f

        emitters.add(
            MetaballEmitter(
                x = centerX,
                y = centerY,
                z = coreZ,
                radiusXy = coreRadiusXy,
                radiusZ = coreRadiusZ,
                weight = 1.15f + rand.nextFloat() * 0.15f,
            ),
        )

        if (morphology == AnomalyMorphology.SPIKE) {
            // SPIKE: Concentrated vertical columns with high aspect ratio
            val spireCount = 3
            for (i in 0 until spireCount) {
                val angle = (i.toFloat() / spireCount) * (Math.PI * 2.0).toFloat() + (rand.nextFloat() * 0.4f - 0.2f)
                val dist = effectiveRadiusXy * (0.25f + rand.nextFloat() * 0.15f)
                val spireRadiusXy = effectiveRadiusXy * (0.25f + rand.nextFloat() * 0.08f)
                val spireRadiusZ = maxVerticalHeight * (0.75f + rand.nextFloat() * 0.20f)
                val spireZ = maxVerticalHeight * (0.40f + rand.nextFloat() * 0.20f)
                emitters.add(
                    MetaballEmitter(
                        x = centerX + kotlin.math.cos(angle) * dist,
                        y = centerY + kotlin.math.sin(angle) * dist,
                        z = spireZ,
                        radiusXy = spireRadiusXy,
                        radiusZ = spireRadiusZ,
                        weight = 0.95f + rand.nextFloat() * 0.15f,
                    ),
                )
            }
        } else {
            // BALANCED / SPRAWL: Radial ground lobes
            val lobeCount = if (morphology == AnomalyMorphology.SPRAWL) 5 else 4
            val baseAngleOffset = rand.nextFloat() * (Math.PI * 2.0).toFloat()
            for (i in 0 until lobeCount) {
                val baseAngle = baseAngleOffset + (i.toFloat() / lobeCount) * (Math.PI * 2.0).toFloat()
                val angleJitter = (rand.nextFloat() - 0.5f) * (Math.PI / 4.0).toFloat()
                val angle = baseAngle + angleJitter

                val dist = effectiveRadiusXy * (0.35f + rand.nextFloat() * 0.20f)
                val lobeRadiusXy = effectiveRadiusXy * (0.55f + rand.nextFloat() * 0.15f)
                val lobeRadiusZ = maxVerticalHeight * (0.50f + rand.nextFloat() * 0.15f)
                val lobeZ = lobeRadiusZ * 0.40f
                emitters.add(
                    MetaballEmitter(
                        x = centerX + kotlin.math.cos(angle) * dist,
                        y = centerY + kotlin.math.sin(angle) * dist,
                        z = lobeZ,
                        radiusXy = lobeRadiusXy,
                        radiusZ = lobeRadiusZ,
                        weight = 0.90f + rand.nextFloat() * 0.20f,
                    ),
                )
            }

            // Secondary Satellite Lobes
            val satelliteCount = 2
            for (i in 0 until satelliteCount) {
                val angle = rand.nextFloat() * (Math.PI * 2.0).toFloat()
                val dist = effectiveRadiusXy * (0.45f + rand.nextFloat() * 0.25f)
                val lobeRadiusXy = effectiveRadiusXy * (0.45f + rand.nextFloat() * 0.15f)
                val lobeRadiusZ = maxVerticalHeight * (0.60f + rand.nextFloat() * 0.20f)
                val lobeZ = maxVerticalHeight * (0.35f + rand.nextFloat() * 0.25f)
                emitters.add(
                    MetaballEmitter(
                        x = centerX + kotlin.math.cos(angle) * dist,
                        y = centerY + kotlin.math.sin(angle) * dist,
                        z = lobeZ,
                        radiusXy = lobeRadiusXy,
                        radiusZ = lobeRadiusZ,
                        weight = 0.85f + rand.nextFloat() * 0.20f,
                    ),
                )
            }
        }

        return emitters
    }
}

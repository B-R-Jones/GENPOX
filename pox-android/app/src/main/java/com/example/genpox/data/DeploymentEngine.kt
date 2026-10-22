package com.example.genpox.data

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow

object DeploymentEngine {

    const val LUNAR_MONTH_DAYS = 29.53059

    /**
     * Calculates the distance in feet between two geographic coordinates.
     */
    fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = GeoMath.calculateHaversineDistanceInFeet(lat1, lng1, lat2, lng2)

    /**
     * Calculates the lobed non-circular boundary radius R(theta) of an anomaly.
     */
    fun calculateBoundaryRadius(anomalyId: String, heatZoneDiameter: Double, theta: Double): Double {
        val seed = anomalyId.hashCode().let { if (it == Int.MIN_VALUE) 0 else abs(it) }
        val r0 = heatZoneDiameter / 2.0
        val epsilon = 0.15 + (seed % 3) * 0.05 // 0.15, 0.20, 0.25
        val k = 3 + (seed % 3) // 3, 4, 5 lobes
        val phi = (seed % 360) * (Math.PI / 180.0)
        return r0 * (1.0 + epsilon * cos(k * theta + phi))
    }

    /**
     * Calculates the boundary radius for an anomaly given a target coordinate (e.g. user coordinate).
     */
    fun calculateBoundaryRadiusForCoordinates(
        anomalyId: String,
        heatZoneDiameter: Double,
        anomalyLat: Double,
        anomalyLng: Double,
        targetLat: Double,
        targetLng: Double,
    ): Double {
        val dLat = targetLat - anomalyLat
        val dLng = (targetLng - anomalyLng) * cos(Math.toRadians(targetLat))
        val theta = kotlin.math.atan2(dLat, dLng)
        return calculateBoundaryRadius(anomalyId, heatZoneDiameter, theta)
    }

    /**
     * Resolves the defense resonance modifier based on creature faction and current lunar phase.
     */
    fun getSynodicResonanceMod(faction: String, phaseName: String): Int = when (faction) {
        "Infection" -> {
            if (phaseName == "Waxing Crescent" || phaseName == "Waxing Gibbous") {
                15
            } else if (phaseName == "Waning Crescent" || phaseName == "Waning Gibbous") {
                -10
            } else {
                0
            }
        }
        "Mech" -> {
            if (phaseName == "First Quarter" || phaseName == "Third Quarter") {
                15
            } else if (phaseName == "New Moon" || phaseName == "Full Moon") {
                -10
            } else {
                0
            }
        }
        "Parasite" -> {
            if (phaseName == "Full Moon") {
                15
            } else if (phaseName == "New Moon") {
                -10
            } else {
                0
            }
        }
        "Containment" -> {
            if (phaseName == "New Moon") {
                15
            } else if (phaseName == "Full Moon") {
                -10
            } else {
                0
            }
        }
        else -> 0
    }

    /**
     * Resolves the effective defense after applying resonance modifiers.
     */
    fun calculateEffectiveDefense(baseDefense: Int, faction: String, phaseName: String): Int = baseDefense + getSynodicResonanceMod(faction, phaseName)

    /**
     * Calculates the Stall Depth (S) percentage reached within the forcefield.
     */
    fun calculateStallDepth(
        effectiveDefense: Int,
        boundaryRadius: Double,
        lunarAge: Double,
        forcefieldResistanceMod: Double = 1.0,
    ): Double {
        val phaseFraction = lunarAge / LUNAR_MONTH_DAYS
        val lunarPhaseScale = (1.0 - cos(phaseFraction * 2.0 * Math.PI)) / 2.0
        val lunarResistanceMod = (0.7 + 0.6 * lunarPhaseScale) * forcefieldResistanceMod
        val R_base = boundaryRadius * 0.1
        val R_anom = R_base * lunarResistanceMod
        return (effectiveDefense.toDouble() / R_anom * 100.0).coerceIn(0.0, 100.0)
    }

    /**
     * Calculates the expected mutation interval in seconds.
     */
    fun calculateMutationInterval(
        stalledDepth: Double,
        lunarAge: Double,
        mutationFluxMod: Double = 1.0,
    ): Long {
        val phaseFraction = lunarAge / LUNAR_MONTH_DAYS
        val lunarPhaseScale = (1.0 - cos(phaseFraction * 2.0 * Math.PI)) / 2.0
        val lunarMutationMod = (0.5 + 1.0 * lunarPhaseScale) * mutationFluxMod.coerceAtLeast(0.1)
        return Math.round((480.0 * Math.pow(2.0, -stalledDepth / 25.0)) / lunarMutationMod / 16.0).coerceAtLeast(1L)
    }

    /**
     * Calculates the combined wave density at the landing coordinates from nearby anomalies.
     */
    fun calculateCombinedWaveDensity(
        landingLat: Double,
        landingLng: Double,
        anomalies: List<PoxAnomaly>,
    ): Double {
        var combinedDensity = 0.0
        anomalies.forEach { anom ->
            val distFromAnom = calculateDistanceInFeet(landingLat, landingLng, anom.lat, anom.lng)
            val boundRad = calculateBoundaryRadiusForCoordinates(
                anomalyId = anom.id,
                heatZoneDiameter = anom.heatZoneDiameter,
                anomalyLat = anom.lat,
                anomalyLng = anom.lng,
                targetLat = landingLat,
                targetLng = landingLng,
            )
            if (distFromAnom <= boundRad) {
                val seed = anom.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else abs(it) }
                val phi = (seed % 360) * (Math.PI / 180.0)
                val omega = 0.02
                val alpha = 0.002
                val waveTerm = cos(omega * distFromAnom + phi) * exp(-alpha * distFromAnom)
                combinedDensity += anom.density * waveTerm
            }
        }
        return combinedDensity
    }

    /**
     * Computes transit velocities and duration phases.
     */
    data class TransitMetrics(
        val travelVelocity: Double,
        val travelDuration: Long,
        val descentVelocity: Double,
        val descentDuration: Long,
        val harvestDuration: Long,
        val ascentDuration: Long,
        val returnDuration: Long,
        val totalDuration: Long,
    )

    fun calculateTransitMetrics(
        creatureSpeed: Int,
        anomalyDistance: Double,
        boundaryRadius: Double,
        stalledDepth: Double,
        effectiveDensity: Double,
        hasCoherenceShield: Boolean,
    ): TransitMetrics {
        val travelTimeComponent = 32.0
        val travelDistanceComponent = 16.0
        val speedFactor = 1350.0
        val travelVelocity = (((creatureSpeed.toDouble() / 50.0) * travelDistanceComponent) / travelTimeComponent) * speedFactor

        val travelDistance = maxOf(0.0, anomalyDistance - boundaryRadius)
        val travelDuration = if (travelVelocity > 0.0) {
            Math.round(travelDistance / travelVelocity).coerceAtLeast(1L)
        } else {
            32L
        }

        val finalDensity = if (hasCoherenceShield && effectiveDensity > 0.0) 0.0 else effectiveDensity
        val descentVelocity = travelVelocity * (1.0 - 2.0 * finalDensity).coerceAtLeast(0.1) * 0.024

        val descentDistance = boundaryRadius * (stalledDepth / 100.0)
        val descentDuration = if (descentVelocity > 0.0) {
            Math.round(descentDistance / descentVelocity).coerceAtLeast(1L)
        } else {
            32L
        }

        val harvestDuration = 60L
        val ascentDuration = descentDuration
        val returnDuration = travelDuration
        val totalDuration = travelDuration + descentDuration + harvestDuration + ascentDuration + returnDuration

        return TransitMetrics(
            travelVelocity = travelVelocity,
            travelDuration = travelDuration,
            descentVelocity = descentVelocity,
            descentDuration = descentDuration,
            harvestDuration = harvestDuration,
            ascentDuration = ascentDuration,
            returnDuration = returnDuration,
            totalDuration = totalDuration,
        )
    }

    /**
     * Non-linear velocity warp progress transformation under environment density.
     */
    fun warpProgress(pLin: Float, density: Float): Float {
        val d = density.coerceIn(-0.9f, 0.9f)
        if (abs(d) < 0.001f) {
            return pLin
        }
        return ((1.0 - (1.0 - d).pow(pLin.toDouble())) / d).toFloat().coerceIn(0f, 1f)
    }
}

package com.example.genpox.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Consolidated geographic distance and spatial math utility.
 */
object GeoMath {
    private const val EARTH_RADIUS_FEET = 20902231.0
    private const val FEET_PER_METER = 3.28084
    private const val METERS_PER_LAT_DEGREE = 111000.0

    /**
     * Fast equirectangular (flat-surface) distance calculation in feet.
     * Ideal for localized tactical radar displays and map ranges (< 50 miles).
     */
    fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val meanLat = Math.toRadians((lat1 + lat2) / 2.0)
        val dx = (lng2 - lng1) * METERS_PER_LAT_DEGREE * cos(meanLat)
        val dy = (lat2 - lat1) * METERS_PER_LAT_DEGREE
        val distMeters = sqrt(dx * dx + dy * dy)
        return distMeters * FEET_PER_METER
    }

    /**
     * Great-Circle Haversine distance in feet for spherical earth geometry.
     */
    fun calculateHaversineDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_FEET * c
    }

    /**
     * Identifies synthetic fallback road polylines (straight-line coordinate grids).
     */
    fun isFallbackRoad(road: List<RoadPoint>): Boolean {
        if (road.size == 75) {
            val first = road[0]
            if (road.all { it.lat == first.lat } || road.all { it.lng == first.lng }) return true
        }
        return false
    }

    /**
     * Identifies synthetic fallback road polylines from coordinate pairs.
     */
    fun isFallbackRoadCoordinates(road: List<Pair<Double, Double>>): Boolean {
        if (road.size == 75) {
            val first = road[0]
            if (road.all { it.first == first.first } || road.all { it.second == first.second }) return true
        }
        return false
    }
}

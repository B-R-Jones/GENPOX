package com.example.genpox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun testZeroDistanceForIdenticalCoordinates() {
        val lat = 37.7749
        val lng = -122.4194

        val flatDist = GeoMath.calculateDistanceInFeet(lat, lng, lat, lng)
        val haversineDist = GeoMath.calculateHaversineDistanceInFeet(lat, lng, lat, lng)

        assertEquals(0.0, flatDist, 0.001)
        assertEquals(0.0, haversineDist, 0.001)
    }

    @Test
    fun testDistanceSymmetry() {
        val lat1 = 37.4220
        val lng1 = -122.0841
        val lat2 = 37.4230
        val lng2 = -122.0830

        val d12Flat = GeoMath.calculateDistanceInFeet(lat1, lng1, lat2, lng2)
        val d21Flat = GeoMath.calculateDistanceInFeet(lat2, lng2, lat1, lng1)
        assertEquals(d12Flat, d21Flat, 0.001)

        val d12Haversine = GeoMath.calculateHaversineDistanceInFeet(lat1, lng1, lat2, lng2)
        val d21Haversine = GeoMath.calculateHaversineDistanceInFeet(lat2, lng2, lat1, lng1)
        assertEquals(d12Haversine, d21Haversine, 0.001)
    }

    @Test
    fun testLocalDistanceMagnitude() {
        // ~100m distance in Palo Alto
        val lat1 = 37.4220
        val lng1 = -122.0841
        val lat2 = 37.4230
        val lng2 = -122.0830

        val flatDist = GeoMath.calculateDistanceInFeet(lat1, lng1, lat2, lng2)
        val haversineDist = GeoMath.calculateHaversineDistanceInFeet(lat1, lng1, lat2, lng2)

        assertTrue("Flat distance should be between 300 and 600 feet", flatDist in 300.0..600.0)
        assertTrue("Haversine distance should be between 300 and 600 feet", haversineDist in 300.0..600.0)

        // Flat distance and Haversine distance should be within 1% of each other for localized coordinates
        val ratio = flatDist / haversineDist
        assertTrue("Flat and Haversine should agree closely on local scale", ratio in 0.98..1.02)
    }

    @Test
    fun testKnownLatitudeDegreeDistance() {
        // 1 degree latitude = ~111 km = ~364,000 feet
        val lat1 = 0.0
        val lng1 = 0.0
        val lat2 = 1.0
        val lng2 = 0.0

        val haversineDist = GeoMath.calculateHaversineDistanceInFeet(lat1, lng1, lat2, lng2)
        assertTrue("1 degree latitude should be approximately 364,000 feet", haversineDist in 360000.0..370000.0)
    }
}

package com.example.genpox.map.tile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileMathTest {

    @Test
    fun testNullIslandAtZoomOne() {
        val coord = TileMath.latLngToTile(0.0, 0.0, zoom = 1)
        assertEquals(1, coord.z)
        assertEquals(1, coord.x)
        assertEquals(1, coord.y)
    }

    @Test
    fun testKansasCityTileContainsPoint() {
        val kcLat = 39.0997
        val kcLng = -94.5786
        val zoom = 15

        val tile = TileMath.latLngToTile(kcLat, kcLng, zoom)
        val bbox = TileMath.tileToBoundingBox(tile.z, tile.x, tile.y)

        assertTrue("kcLat must be >= minLat", kcLat >= bbox.minLat)
        assertTrue("kcLat must be <= maxLat", kcLat <= bbox.maxLat)
        assertTrue("kcLng must be >= minLng", kcLng >= bbox.minLng)
        assertTrue("kcLng must be <= maxLng", kcLng <= bbox.maxLng)
    }

    @Test
    fun testSanFranciscoTileContainsPoint() {
        val sfLat = 37.7749
        val sfLng = -122.4194
        val zoom = 14

        val tile = TileMath.latLngToTile(sfLat, sfLng, zoom)
        val bbox = TileMath.tileToBoundingBox(tile.z, tile.x, tile.y)

        assertTrue(sfLat in bbox.minLat..bbox.maxLat)
        assertTrue(sfLng in bbox.minLng..bbox.maxLng)
    }

    @Test
    fun testLocalCoordinatesMatchBoundingBoxCorners() {
        val zoom = 15
        val x = 7774
        val y = 12790

        val bbox = TileMath.tileToBoundingBox(zoom, x, y)

        // NW corner (local 0, 0)
        val (nwLat, nwLng) = TileMath.tileLocalToLatLng(x, y, zoom, 0, 0, extent = 4096)
        assertEquals(bbox.maxLat, nwLat, 0.0001)
        assertEquals(bbox.minLng, nwLng, 0.0001)

        // SE corner (local 4096, 4096)
        val (seLat, seLng) = TileMath.tileLocalToLatLng(x, y, zoom, 4096, 4096, extent = 4096)
        assertEquals(bbox.minLat, seLat, 0.0001)
        assertEquals(bbox.maxLng, seLng, 0.0001)

        // Center (local 2048, 2048)
        val (centerLat, centerLng) = TileMath.tileToCenterLatLng(zoom, x, y)
        val (midLat, midLng) = TileMath.tileLocalToLatLng(x, y, zoom, 2048, 2048, extent = 4096)
        assertEquals(centerLat, midLat, 0.0001)
        assertEquals(centerLng, midLng, 0.0001)
    }

    @Test
    fun testComputeSurroundingTilesGridSizes() {
        val lat = 39.0997
        val lng = -94.5786

        val r0 = TileMath.computeSurroundingTiles(lat, lng, zoom = 15, radius = 0)
        assertEquals(1, r0.size)

        val r1 = TileMath.computeSurroundingTiles(lat, lng, zoom = 15, radius = 1)
        assertEquals(9, r1.size)

        val r2 = TileMath.computeSurroundingTiles(lat, lng, zoom = 15, radius = 2)
        assertEquals(25, r2.size)

        // Center tile should be included in all
        val center = TileMath.latLngToTile(lat, lng, zoom = 15)
        assertTrue(r0.contains(center))
        assertTrue(r1.contains(center))
        assertTrue(r2.contains(center))
    }
}

package com.example.genpox.map.tile

import kotlin.math.*

/**
 * Mathematical utilities for Web Mercator (EPSG:3857) Slippy Map tile conversions
 * according to the OpenStreetMap and Mapbox Vector Tile (MVT) specifications.
 */
object TileMath {

    const val DEFAULT_EXTENT = 4096
    const val DEFAULT_ZOOM = 15

    /**
     * Converts WGS84 GPS decimal degrees to a Slippy Map tile coordinate at the specified zoom level.
     */
    fun latLngToTile(lat: Double, lng: Double, zoom: Int): TileCoordinate {
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val clampedLng = lng.coerceIn(-180.0, 180.0)
        val n = 1 shl zoom

        val x = floor((clampedLng + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val latRad = Math.toRadians(clampedLat)
        val y = floor((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n).toInt().coerceIn(0, n - 1)

        return TileCoordinate(zoom, x, y)
    }

    /**
     * Calculates the exact WGS84 bounding box for a given tile coordinate.
     */
    fun tileToBoundingBox(z: Int, x: Int, y: Int): TileBoundingBox {
        val n = 1 shl z
        val west = x.toDouble() / n * 360.0 - 180.0
        val east = (x + 1.0) / n * 360.0 - 180.0

        val north = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * y.toDouble() / n))))
        val south = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * (y + 1.0) / n))))

        return TileBoundingBox(
            minLat = min(south, north),
            minLng = min(west, east),
            maxLat = max(south, north),
            maxLng = max(west, east),
        )
    }

    /**
     * Calculates the center GPS coordinate for a tile.
     */
    fun tileToCenterLatLng(z: Int, x: Int, y: Int): Pair<Double, Double> {
        val n = 1 shl z
        val centerLng = (x + 0.5) / n * 360.0 - 180.0
        val centerLat = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * (y + 0.5) / n))))
        return Pair(centerLat, centerLng)
    }

    /**
     * Converts a tile-local integer coordinate (0..extent, where (0,0) is NW corner)
     * into exact WGS84 GPS latitude and longitude.
     */
    fun tileLocalToLatLng(
        tileX: Int,
        tileY: Int,
        zoom: Int,
        localX: Int,
        localY: Int,
        extent: Int = DEFAULT_EXTENT,
    ): Pair<Double, Double> {
        val n = 1 shl zoom
        val globalX = tileX + (localX.toDouble() / extent)
        val globalY = tileY + (localY.toDouble() / extent)

        val lng = globalX / n * 360.0 - 180.0
        val lat = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * globalY / n))))
        return Pair(lat, lng)
    }

    /**
     * Converts a tile-local integer coordinate into a RoadPoint directly.
     */
    fun tileLocalToRoadPoint(
        tileX: Int,
        tileY: Int,
        zoom: Int,
        localX: Int,
        localY: Int,
        extent: Int = DEFAULT_EXTENT,
    ): com.example.genpox.data.RoadPoint {
        val n = 1 shl zoom
        val globalX = tileX + (localX.toDouble() / extent)
        val globalY = tileY + (localY.toDouble() / extent)

        val lng = globalX / n * 360.0 - 180.0
        val lat = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * globalY / n))))
        return com.example.genpox.data.RoadPoint(lat, lng)
    }

    /**
     * Computes the square grid of surrounding tile coordinates centered on a GPS location.
     * radius = 0 -> 1 tile (1x1)
     * radius = 1 -> 9 tiles (3x3)
     * radius = 2 -> 25 tiles (5x5)
     */
    fun computeSurroundingTiles(
        centerLat: Double,
        centerLng: Double,
        zoom: Int = DEFAULT_ZOOM,
        radius: Int = 1,
    ): List<TileCoordinate> {
        val centerTile = latLngToTile(centerLat, centerLng, zoom)
        val n = 1 shl zoom
        val tiles = ArrayList<TileCoordinate>((2 * radius + 1) * (2 * radius + 1))

        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                val tx = (centerTile.x + dx).coerceIn(0, n - 1)
                val ty = (centerTile.y + dy).coerceIn(0, n - 1)
                tiles.add(TileCoordinate(zoom, tx, ty))
            }
        }
        return tiles
    }
}

/**
 * Pre-computes constants for projecting local tile coordinates (0..extent)
 * to WGS84 RoadPoints with minimal arithmetic operations.
 */
class TileProjection(
    tileX: Int,
    tileY: Int,
    zoom: Int,
    extent: Int = TileMath.DEFAULT_EXTENT,
) {
    private val n = (1 shl zoom).toDouble()
    private val invN = 1.0 / n
    private val invExtent = 1.0 / extent.toDouble()
    private val lngMultiplier = 360.0 * invN * invExtent
    private val lngBase = (tileX.toDouble() * invN) * 360.0 - 180.0
    private val yBaseCoeff = PI * (1.0 - 2.0 * tileY * invN)
    private val yDeltaCoeff = -2.0 * PI * invN * invExtent

    fun toRoadPoint(localX: Int, localY: Int): com.example.genpox.data.RoadPoint {
        val lng = lngBase + localX * lngMultiplier
        val latArg = yBaseCoeff + localY * yDeltaCoeff
        val lat = Math.toDegrees(atan(sinh(latArg)))
        return com.example.genpox.data.RoadPoint(lat, lng)
    }
}

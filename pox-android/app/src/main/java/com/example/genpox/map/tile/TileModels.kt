package com.example.genpox.map.tile

import com.example.genpox.data.BuildingStructure
import com.example.genpox.data.RoadPoint

/**
 * Standard Slippy Map Quadtree Tile Coordinate (z/x/y).
 */
data class TileCoordinate(
    val z: Int,
    val x: Int,
    val y: Int,
) {
    val key: String get() = "$z/$x/$y"

    override fun toString(): String = key
}

/**
 * Geographic bounding box in WGS84 decimal degrees.
 */
data class TileBoundingBox(
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double,
)

/**
 * Extracted and normalized vector geometry for a single tile.
 */
data class TileGeometry(
    val coordinate: TileCoordinate,
    val roads: List<List<RoadPoint>>,
    val buildings: List<BuildingStructure>,
    val isFromCache: Boolean = false,
)

/**
 * Pure data provider contract for the vector tile engine.
 * Completely divorced from UI, ViewModel, network protocols, or legacy caches.
 */
interface TileDataProvider {
    /**
     * Retrieves or decodes a single tile by coordinate.
     * Returns null if the tile is unavailable or empty.
     */
    suspend fun getTile(z: Int, x: Int, y: Int): TileGeometry?

    /**
     * Batch retrieves multiple tiles concurrently.
     */
    suspend fun getTiles(coordinates: List<TileCoordinate>): Map<TileCoordinate, TileGeometry>

    /**
     * Checks if a tile is currently cached and valid locally within its persistence lifetime.
     */
    fun isTileCached(z: Int, x: Int, y: Int): Boolean = false
}

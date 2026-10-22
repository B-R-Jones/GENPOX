package com.example.genpox.map.tile

import android.util.Log
import com.example.genpox.util.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the dynamic multi-tile streaming lifecycle for the vector tile map.
 * Given a continuous GPS location or viewport center, streams surrounding Zoom 14 tiles
 * asynchronously at a consistent, metered sporadic cadence across a 5x5 sector.
 */
@Singleton
class TileStreamManager @Inject constructor(
    val tileDataProvider: TileDataProvider,
    private val dispatchers: DispatchersProvider,
) {
    companion object {
        private const val TAG = "TileStreamManager"
        const val DEFAULT_STREAM_ZOOM = 14
        const val DEFAULT_RADIUS = 2 // 2 -> 5x5 sector (~12 km x 12 km viewport, 25 cells)
        private const val MAX_ACTIVE_TILES = 256 // holds up to 16x16 tiles (~40 km wide) to prevent eviction churn
        private const val LOAD_METER_CADENCE_MS = 1600L // cadence: 1.6s between new tile discoveries
    }

    fun isTileCached(coord: TileCoordinate): Boolean =
        tileDataProvider.isTileCached(coord.z, coord.x, coord.y)

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private var currentStreamJob: Job? = null

    private val _activeTiles = MutableStateFlow<Map<TileCoordinate, TileGeometry>>(emptyMap())
    val activeTiles: StateFlow<Map<TileCoordinate, TileGeometry>> = _activeTiles.asStateFlow()

    private val _sectorCoordinates = MutableStateFlow<Set<TileCoordinate>>(emptySet())
    val sectorCoordinates: StateFlow<Set<TileCoordinate>> = _sectorCoordinates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastCenterLat: Double = Double.NaN
    private var lastCenterLng: Double = Double.NaN
    private var lastRadius: Int = -1

    /**
     * Updates the streaming viewport center.
     * Computes the target tile neighborhood and fetches any missing tiles.
     * Persisted/cached tiles are loaded instantly in batch before the sector is updated,
     * while new uncached tiles are streamed asynchronously at a consistent metered rate.
     */
    fun updateCenter(
        lat: Double,
        lng: Double,
        radius: Int = DEFAULT_RADIUS,
        forceRefresh: Boolean = false,
    ) {
        val centerTile = TileMath.latLngToTile(lat, lng, DEFAULT_STREAM_ZOOM)
        val lastTile = if (!lastCenterLat.isNaN()) {
            TileMath.latLngToTile(lastCenterLat, lastCenterLng, DEFAULT_STREAM_ZOOM)
        } else {
            null
        }

        if (!forceRefresh && centerTile == lastTile && radius == lastRadius && _activeTiles.value.isNotEmpty()) {
            return
        }

        lastCenterLat = lat
        lastCenterLng = lng
        lastRadius = radius

        val targetCoordinates = TileMath.computeSurroundingTiles(lat, lng, DEFAULT_STREAM_ZOOM, radius)
        val targetSet = targetCoordinates.toSet()

        val existingCoords = _activeTiles.value.keys
        val missingCoords = targetCoordinates.filterNot { existingCoords.contains(it) }

        if (missingCoords.isEmpty() && !forceRefresh) {
            _sectorCoordinates.value = targetSet
            return
        }

        currentStreamJob?.cancel()
        currentStreamJob = scope.launch {
            _isLoading.value = true
            try {
                // 1. Separate cached (already persisted) tiles vs uncached tiles
                val cachedCoords = missingCoords.filter { tileDataProvider.isTileCached(it.z, it.x, it.y) }
                val uncachedCoords = missingCoords.filterNot { tileDataProvider.isTileCached(it.z, it.x, it.y) }

                // 2. Immediately load all cached tiles into activeTiles in parallel before setting sectorCoordinates
                if (cachedCoords.isNotEmpty()) {
                    val loadedCachedList = cachedCoords.map { coord ->
                        async(dispatchers.io) {
                            val tile = tileDataProvider.getTile(coord.z, coord.x, coord.y)
                            if (tile != null) coord to tile else null
                        }
                    }.awaitAll().filterNotNull()

                    if (loadedCachedList.isNotEmpty()) {
                        _activeTiles.update { current ->
                            val combined = current.toMutableMap()
                            for ((c, t) in loadedCachedList) {
                                combined[c] = t
                            }
                            if (combined.size > MAX_ACTIVE_TILES) {
                                val excess = combined.keys.filterNot { targetSet.contains(it) }
                                excess.take(combined.size - MAX_ACTIVE_TILES).forEach { combined.remove(it) }
                            }
                            combined
                        }
                    }
                }

                // 3. Update sectorCoordinates now that all cached tiles are in activeTiles
                _sectorCoordinates.value = targetSet

                // 4. For uncached tiles, prioritize center cell if missing, then randomize remainder
                val uncachedQueue = if (uncachedCoords.contains(centerTile)) {
                    listOf(centerTile) + (uncachedCoords - centerTile).shuffled()
                } else {
                    uncachedCoords.shuffled()
                }

                for (coord in uncachedQueue) {
                    if (!isActive) break

                    val tile = tileDataProvider.getTile(coord.z, coord.x, coord.y)
                    if (tile != null) {
                        _activeTiles.update { current ->
                            val combined = current.toMutableMap()
                            combined[coord] = tile

                            // Keep active tiles trimmed to nearby neighborhood (LRU pruning)
                            if (combined.size > MAX_ACTIVE_TILES) {
                                val excess = combined.keys.filterNot { targetSet.contains(it) }
                                excess.take(combined.size - MAX_ACTIVE_TILES).forEach { combined.remove(it) }
                            }

                            combined
                        }
                    }

                    // Consistent metered pacing for sporadic load choreography of new cells
                    delay(LOAD_METER_CADENCE_MS)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Normal job cancellation when viewport center moves
            } catch (e: Exception) {
                Log.e(TAG, "Error streaming vector tiles for ($lat, $lng): ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clear() {
        currentStreamJob?.cancel()
        _activeTiles.value = emptyMap()
        _sectorCoordinates.value = emptySet()
        lastCenterLat = Double.NaN
        lastCenterLng = Double.NaN
        lastRadius = -1
    }
}

package com.example.genpox.map.tile.filament

import com.example.genpox.data.PoxAnomaly
import com.example.genpox.map.tile.TileGeometry
import com.example.genpox.map.tile.TileMath
import com.example.genpox.map.tile.TileCoordinate
import com.example.genpox.map.tile.PolygonTriangulator
import com.example.genpox.ui.scanner.map.MetaballMeshCompiler
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.math.cos

/**
 * Lightweight growth buffer for primitive floats to eliminate object boxing.
 */
class FloatBufferBuilder(initialCapacity: Int = 4096) {
    var data = FloatArray(initialCapacity)
        private set
    var size = 0
        private set

    fun add(v: Float) {
        if (size >= data.size) {
            data = data.copyOf(data.size * 2)
        }
        data[size++] = v
    }

    fun add3(x: Float, y: Float, z: Float) {
        if (size + 3 > data.size) {
            data = data.copyOf(kotlin.math.max(data.size * 2, size + 3))
        }
        data[size++] = x
        data[size++] = y
        data[size++] = z
    }

    fun add5(x: Float, y: Float, z: Float, u: Float, v: Float) {
        if (size + 5 > data.size) {
            data = data.copyOf(kotlin.math.max(data.size * 2, size + 5))
        }
        data[size++] = x
        data[size++] = y
        data[size++] = z
        data[size++] = u
        data[size++] = v
    }

    fun toFloatArray(): FloatArray = if (size == data.size) data else data.copyOf(size)
}

/**
 * Lightweight growth buffer for primitive ints to eliminate object boxing.
 */
class IntBufferBuilder(initialCapacity: Int = 4096) {
    var data = IntArray(initialCapacity)
        private set
    var size = 0
        private set

    fun add(v: Int) {
        if (size >= data.size) {
            data = data.copyOf(data.size * 2)
        }
        data[size++] = v
    }

    fun add3(i1: Int, i2: Int, i3: Int) {
        if (size + 3 > data.size) {
            data = data.copyOf(kotlin.math.max(data.size * 2, size + 3))
        }
        data[size++] = i1
        data[size++] = i2
        data[size++] = i3
    }

    fun toIntArray(): IntArray = if (size == data.size) data else data.copyOf(size)
}

/**
 * Pre-compiled geometry data in tile-local space, computed asynchronously on any background thread.
 */
data class CompiledTileData(
    val coordinate: TileCoordinate,
    val centerLat: Double,
    val centerLng: Double,
    val wallPositions: FloatArray,
    val wallIndices: IntArray,
    val roofPositions: FloatArray,
    val roofIndices: IntArray,
    val infectedWallPositions: FloatArray,
    val infectedWallIndices: IntArray,
    val infectedRoofPositions: FloatArray,
    val infectedRoofIndices: IntArray,
    val rimPositions: FloatArray,
    val rimIndices: IntArray,
    val roadPositions: FloatArray,
    val roadIndices: IntArray,
    val buildingCount: Int,
    val infectedBuildingCount: Int,
    val roadPolylineCount: Int,
)

/**
 * Encapsulates the GPU renderable entities and buffers for a vector tile in Google Filament.
 * Vertices are compiled in tile-local coordinates and positioned in world space via TransformManager.
 */
data class FilamentTileMesh(
    val coordinate: TileCoordinate,
    val centerLat: Double,
    val centerLng: Double,
    val wallEntity: Int = 0,
    val roofEntity: Int = 0,
    val wireframeEntity: Int = 0,
    val roadEntity: Int = 0,
    val infectedWallEntity: Int = 0,
    val infectedRoofEntity: Int = 0,
    val vertexBuffers: List<VertexBuffer>,
    val indexBuffers: List<IndexBuffer>,
    val buildingCount: Int,
    val infectedBuildingCount: Int = 0,
    val roadPolylineCount: Int,
    var fadeWallMat: com.google.android.filament.MaterialInstance? = null,
    var fadeRoofMat: com.google.android.filament.MaterialInstance? = null,
    var fadeWireframeMat: com.google.android.filament.MaterialInstance? = null,
    var fadeRoadMat: com.google.android.filament.MaterialInstance? = null,
    var fadeInfectedWallMat: com.google.android.filament.MaterialInstance? = null,
    var fadeInfectedRoofMat: com.google.android.filament.MaterialInstance? = null,
    val spawnTimeNanos: Long = System.nanoTime(),
    var isTransitionComplete: Boolean = false,
) {
    fun destroy(engine: Engine) {
        val em = EntityManager.get()
        val rm = engine.renderableManager

        val tm = engine.transformManager
        val entities = intArrayOf(
            wallEntity, roofEntity, wireframeEntity, roadEntity,
            infectedWallEntity, infectedRoofEntity,
        )
        for (e in entities) {
            if (e != 0) {
                if (tm.hasComponent(e)) {
                    tm.destroy(e)
                }
                if (rm.hasComponent(e)) {
                    rm.destroy(e)
                }
                if (em.isAlive(e)) {
                    em.destroy(e)
                }
            }
        }
        for (vb in vertexBuffers) {
            engine.destroyVertexBuffer(vb)
        }
        for (ib in indexBuffers) {
            engine.destroyIndexBuffer(ib)
        }
        fadeWallMat?.let { engine.destroyMaterialInstance(it) }
        fadeRoofMat?.let { engine.destroyMaterialInstance(it) }
        fadeWireframeMat?.let { engine.destroyMaterialInstance(it) }
        fadeRoadMat?.let { engine.destroyMaterialInstance(it) }
        fadeInfectedWallMat?.let { engine.destroyMaterialInstance(it) }
        fadeInfectedRoofMat?.let { engine.destroyMaterialInstance(it) }
    }
}

/**
 * Encapsulates the flat static screen ground quad rendered for an unloaded cell.
 */
data class FilamentStaticQuad(
    val coordinate: TileCoordinate,
    val centerLat: Double,
    val centerLng: Double,
    val entity: Int,
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer,
    val materialInstance: com.google.android.filament.MaterialInstance,
) {
    fun destroy(engine: Engine) {
        val em = EntityManager.get()
        val rm = engine.renderableManager
        val tm = engine.transformManager
        if (entity != 0) {
            if (tm.hasComponent(entity)) {
                tm.destroy(entity)
            }
            if (rm.hasComponent(entity)) {
                rm.destroy(entity)
            }
            if (em.isAlive(entity)) {
                em.destroy(entity)
            }
        }
        engine.destroyVertexBuffer(vertexBuffer)
        engine.destroyIndexBuffer(indexBuffer)
        engine.destroyMaterialInstance(materialInstance)
    }
}

object FilamentTileMeshCompiler {

    /**
     * Compiles geometry for multiple tiles concurrently on background worker threads (Dispatchers.Default).
     * Eliminates ear-clipping triangulation and computational geometry overhead from the Android UI thread.
     */
    suspend fun compileGeometriesAsync(
        tiles: Collection<TileGeometry>,
        anomalies: List<PoxAnomaly> = emptyList(),
    ): List<CompiledTileData> = withContext(Dispatchers.Default) {
        tiles.map { geom ->
            async {
                compileTileGeometry(geom, anomalies)
            }
        }.awaitAll()
    }

    /**
     * Pure CPU computational geometry compilation.
     * Generates local vertices centered at the tile's own geographic center (0, 0, 0).
     * Can execute safely on Dispatchers.Default without blocking the Android UI thread.
     */
    fun compileTileGeometry(
        geometry: TileGeometry,
        anomalies: List<PoxAnomaly> = emptyList(),
    ): CompiledTileData {
        val coord = geometry.coordinate
        val (tileCenterLat, tileCenterLng) = TileMath.tileToCenterLatLng(coord.z, coord.x, coord.y)
        val bbox = TileMath.tileToBoundingBox(coord.z, coord.x, coord.y)
        val cosTile = cos(Math.toRadians(tileCenterLat))

        val wallPositions = FloatBufferBuilder(8192)
        val wallIndices = IntBufferBuilder(8192)

        val roofPositions = FloatBufferBuilder(4096)
        val roofIndices = IntBufferBuilder(4096)

        val rimPositions = FloatBufferBuilder(4096)
        val rimIndices = IntBufferBuilder(4096)

        var validBuildingCount = 0

        for (b in geometry.buildings) {
            if (b.points.size < 3) continue

            // Deduplicate closing point if identical to first
            val isClosed = b.points.first().lat == b.points.last().lat &&
                b.points.first().lng == b.points.last().lng
            val count = if (isClosed && b.points.size >= 4) b.points.size - 1 else b.points.size
            if (count < 3) continue

            val coords = FloatArray(count * 2)
            var bMinX = Float.MAX_VALUE
            var bMaxX = -Float.MAX_VALUE
            var bMinZ = Float.MAX_VALUE
            var bMaxZ = -Float.MAX_VALUE

            for (i in 0 until count) {
                val p = b.points[i]
                val x = ((p.lng - tileCenterLng) * cosTile * 111000.0).toFloat()
                val z = (-(p.lat - tileCenterLat) * 111000.0).toFloat()
                coords[i * 2] = x
                coords[i * 2 + 1] = z
                if (x < bMinX) bMinX = x
                if (x > bMaxX) bMaxX = x
                if (z < bMinZ) bMinZ = z
                if (z > bMaxZ) bMaxZ = z
            }

            val dx = (bMaxX - bMinX).coerceAtLeast(0f)
            val dz = (bMaxZ - bMinZ).coerceAtLeast(0f)
            val approxArea = dx * dz

            val height = if (b.heightMeters > 0f) {
                b.heightMeters
            } else {
                if (approxArea > 2500f) {
                    18f
                } else if (approxArea > 800f) {
                    12f
                } else {
                    6f
                }
            }

            // Normalize winding to CCW in place
            var signedArea = 0f
            for (i in 0 until count) {
                val next = (i + 1) % count
                val x1 = coords[i * 2]
                val z1 = coords[i * 2 + 1]
                val x2 = coords[next * 2]
                val z2 = coords[next * 2 + 1]
                signedArea += (x1 * z2 - x2 * z1)
            }
            if (signedArea < 0f) {
                for (i in 0 until count / 2) {
                    val j = count - 1 - i
                    val tx = coords[i * 2]
                    val tz = coords[i * 2 + 1]
                    coords[i * 2] = coords[j * 2]
                    coords[i * 2 + 1] = coords[j * 2 + 1]
                    coords[j * 2] = tx
                    coords[j * 2 + 1] = tz
                }
            }

            // A. Extrude Wall Quads
            for (i in 0 until count) {
                val next = (i + 1) % count
                val x1 = coords[i * 2]
                val z1 = coords[i * 2 + 1]
                val x2 = coords[next * 2]
                val z2 = coords[next * 2 + 1]

                val baseIdx = wallPositions.size / 3

                wallPositions.add3(x1, 0f, z1)
                wallPositions.add3(x1, height, z1)
                wallPositions.add3(x2, height, z2)
                wallPositions.add3(x2, 0f, z2)

                // Triangle 1: CCW & CW
                wallIndices.add3(baseIdx, baseIdx + 1, baseIdx + 2)
                wallIndices.add3(baseIdx, baseIdx + 2, baseIdx + 1)
                // Triangle 2: CCW & CW
                wallIndices.add3(baseIdx, baseIdx + 2, baseIdx + 3)
                wallIndices.add3(baseIdx, baseIdx + 3, baseIdx + 2)
            }

            // B. Triangulate Roof Cap
            val roofBaseIdx = roofPositions.size / 3
            for (i in 0 until count) {
                val rx = coords[i * 2]
                val rz = coords[i * 2 + 1]
                roofPositions.add3(rx, height, rz)
            }
            PolygonTriangulator.triangulatePolygonEarClipping(coords, count) { i1, i2, i3 ->
                roofIndices.add3(roofBaseIdx + i1, roofBaseIdx + i3, roofBaseIdx + i2)
                roofIndices.add3(roofBaseIdx + i1, roofBaseIdx + i2, roofBaseIdx + i3)
            }

            // C. Roof Perimeter Wireframe Rims
            val rimY = height + 0.05f
            for (i in 0 until count) {
                val next = (i + 1) % count
                val x1 = coords[i * 2]
                val z1 = coords[i * 2 + 1]
                val x2 = coords[next * 2]
                val z2 = coords[next * 2 + 1]

                val rimBaseIdx = rimPositions.size / 3
                rimPositions.add3(x1, rimY, z1)
                rimPositions.add3(x2, rimY, z2)
                rimIndices.add(rimBaseIdx)
                rimIndices.add(rimBaseIdx + 1)
            }

            validBuildingCount++
        }

        // -------------------------------------------------------------
        // 2. EXTRACT & COMPILE 2D/3D ROAD POLYLINES
        // -------------------------------------------------------------
        val roadPositions = FloatBufferBuilder(4096)
        val roadIndices = IntBufferBuilder(4096)

        for (road in geometry.roads) {
            if (road.size < 2) continue
            for (i in 0 until road.size - 1) {
                val p1 = road[i]
                val p2 = road[i + 1]

                val x1 = ((p1.lng - tileCenterLng) * cosTile * 111000.0).toFloat()
                val z1 = (-(p1.lat - tileCenterLat) * 111000.0).toFloat()
                val x2 = ((p2.lng - tileCenterLng) * cosTile * 111000.0).toFloat()
                val z2 = (-(p2.lat - tileCenterLat) * 111000.0).toFloat()

                val baseIdx = roadPositions.size / 3
                roadPositions.add3(x1, 0.02f, z1)
                roadPositions.add3(x2, 0.02f, z2)

                roadIndices.add(baseIdx)
                roadIndices.add(baseIdx + 1)
            }
        }

        return CompiledTileData(
            coordinate = coord,
            centerLat = tileCenterLat,
            centerLng = tileCenterLng,
            wallPositions = wallPositions.toFloatArray(),
            wallIndices = wallIndices.toIntArray(),
            roofPositions = roofPositions.toFloatArray(),
            roofIndices = roofIndices.toIntArray(),
            infectedWallPositions = EMPTY_FLOAT_ARRAY,
            infectedWallIndices = EMPTY_INT_ARRAY,
            infectedRoofPositions = EMPTY_FLOAT_ARRAY,
            infectedRoofIndices = EMPTY_INT_ARRAY,
            rimPositions = rimPositions.toFloatArray(),
            rimIndices = rimIndices.toIntArray(),
            roadPositions = roadPositions.toFloatArray(),
            roadIndices = roadIndices.toIntArray(),
            buildingCount = validBuildingCount,
            infectedBuildingCount = 0,
            roadPolylineCount = geometry.roads.size,
        )
    }

    private val EMPTY_FLOAT_ARRAY = FloatArray(0)
    private val EMPTY_INT_ARRAY = IntArray(0)

    /**
     * Uploads pre-compiled tile buffers to the Google Filament GPU Engine.
     * Takes < 1ms on the UI thread as all computational geometry was completed in advance.
     */
    fun uploadTileToGpu(
        engine: Engine,
        compiled: CompiledTileData,
        materials: FilamentMaterials,
        isEmerging: Boolean = true,
    ): FilamentTileMesh {
        val vbs = mutableListOf<VertexBuffer>()
        val ibs = mutableListOf<IndexBuffer>()
        val em = EntityManager.get()

        // Local bounding box centered at tile local origin (0, 0, 0)
        val defaultBox = Box(0f, 100f, 0f, 1500f, 500f, 1500f)

        val tm = engine.transformManager

        val fadeWallMat = if (isEmerging && compiled.wallPositions.isNotEmpty()) materials.createFadeWallInstance().apply { setParameter("opacity", 0.0f) } else null
        val fadeRoofMat = if (isEmerging && compiled.roofPositions.isNotEmpty()) materials.createFadeRoofInstance().apply { setParameter("opacity", 0.0f) } else null
        val fadeWireframeMat = if (isEmerging && compiled.rimPositions.isNotEmpty()) materials.createFadeWireframeInstance().apply { setParameter("opacity", 0.0f) } else null
        val fadeRoadMat = if (isEmerging && compiled.roadPositions.isNotEmpty()) materials.createFadeRoadInstance().apply { setParameter("opacity", 0.0f) } else null

        // Entity 1: Buildings Walls (Dynamic GPU distance field handles infection shading)
        val wallEntity = if (compiled.wallPositions.isNotEmpty()) {
            val (vb, ib) = buildBuffers(engine, compiled.wallPositions, compiled.wallIndices)
            vbs.add(vb)
            ibs.add(ib)
            val e = em.create()
            tm.create(e)
            RenderableManager.Builder(1)
                .boundingBox(defaultBox)
                .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)
                .material(0, fadeWallMat ?: materials.wallMaterial)
                .culling(false)
                .build(engine, e)
            e
        } else 0

        // Entity 2: Buildings Roofs (Dynamic GPU distance field handles infection shading)
        val roofEntity = if (compiled.roofPositions.isNotEmpty()) {
            val (vb, ib) = buildBuffers(engine, compiled.roofPositions, compiled.roofIndices)
            vbs.add(vb)
            ibs.add(ib)
            val e = em.create()
            tm.create(e)
            RenderableManager.Builder(1)
                .boundingBox(defaultBox)
                .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)
                .material(0, fadeRoofMat ?: materials.roofMaterial)
                .culling(false)
                .build(engine, e)
            e
        } else 0

        val infectedWallEntity = 0
        val infectedRoofEntity = 0

        // Entity 5: Roof Wireframe Rims
        val wireframeEntity = if (compiled.rimPositions.isNotEmpty()) {
            val (vb, ib) = buildBuffers(engine, compiled.rimPositions, compiled.rimIndices)
            vbs.add(vb)
            ibs.add(ib)
            val e = em.create()
            tm.create(e)
            RenderableManager.Builder(1)
                .boundingBox(defaultBox)
                .geometry(0, RenderableManager.PrimitiveType.LINES, vb, ib)
                .material(0, fadeWireframeMat ?: materials.wireframeMaterial)
                .culling(false)
                .build(engine, e)
            e
        } else 0

        // Entity 6: Roads
        val roadEntity = if (compiled.roadPositions.isNotEmpty()) {
            val (vb, ib) = buildBuffers(engine, compiled.roadPositions, compiled.roadIndices)
            vbs.add(vb)
            ibs.add(ib)
            val e = em.create()
            tm.create(e)
            RenderableManager.Builder(1)
                .boundingBox(defaultBox)
                .geometry(0, RenderableManager.PrimitiveType.LINES, vb, ib)
                .material(0, fadeRoadMat ?: materials.roadMaterial)
                .culling(false)
                .build(engine, e)
            e
        } else 0

        return FilamentTileMesh(
            coordinate = compiled.coordinate,
            centerLat = compiled.centerLat,
            centerLng = compiled.centerLng,
            wallEntity = wallEntity,
            roofEntity = roofEntity,
            wireframeEntity = wireframeEntity,
            roadEntity = roadEntity,
            infectedWallEntity = infectedWallEntity,
            infectedRoofEntity = infectedRoofEntity,
            vertexBuffers = vbs,
            indexBuffers = ibs,
            buildingCount = compiled.buildingCount,
            infectedBuildingCount = compiled.infectedBuildingCount,
            roadPolylineCount = compiled.roadPolylineCount,
            fadeWallMat = fadeWallMat,
            fadeRoofMat = fadeRoofMat,
            fadeWireframeMat = fadeWireframeMat,
            fadeRoadMat = fadeRoadMat,
            fadeInfectedWallMat = null,
            fadeInfectedRoofMat = null,
            spawnTimeNanos = System.nanoTime(),
            isTransitionComplete = !isEmerging,
        )
    }

    /**
     * Backward-compatible direct compilation method.
     */
    fun compileTile(
        engine: Engine,
        geometry: TileGeometry,
        materials: FilamentMaterials,
        anchorLat: Double = TileMath.tileToCenterLatLng(geometry.coordinate.z, geometry.coordinate.x, geometry.coordinate.y).first,
        anchorLng: Double = TileMath.tileToCenterLatLng(geometry.coordinate.z, geometry.coordinate.x, geometry.coordinate.y).second,
        anomalies: List<PoxAnomaly> = emptyList(),
        isEmerging: Boolean = true,
    ): FilamentTileMesh {
        val compiled = compileTileGeometry(geometry, anomalies)
        return uploadTileToGpu(engine, compiled, materials, isEmerging)
    }

    private fun buildBuffers(
        engine: Engine,
        positions: FloatArray,
        indices: IntArray,
    ): Pair<VertexBuffer, IndexBuffer> {
        val vertCount = positions.size / 3

        val vb = VertexBuffer.Builder()
            .vertexCount(vertCount)
            .bufferCount(1)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
            .build(engine)

        val posBuffer: FloatBuffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        posBuffer.put(positions)
        posBuffer.flip()
        vb.setBufferAt(engine, 0, posBuffer)

        val ib = IndexBuffer.Builder()
            .indexCount(indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.UINT)
            .build(engine)

        val idxBuffer: IntBuffer = ByteBuffer.allocateDirect(indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        idxBuffer.put(indices)
        idxBuffer.flip()
        ib.setBuffer(engine, idxBuffer)

        return Pair(vb, ib)
    }

    /**
     * Compiles a flat static screen ground quad for an unloaded cell.
     */
    fun compileStaticGroundQuad(
        engine: Engine,
        coord: TileCoordinate,
        materials: FilamentMaterials,
    ): FilamentStaticQuad {
        val (centerLat, centerLng) = TileMath.tileToCenterLatLng(coord.z, coord.x, coord.y)
        val bbox = TileMath.tileToBoundingBox(coord.z, coord.x, coord.y)
        val cosTile = cos(Math.toRadians(centerLat))

        val minX = ((bbox.minLng - centerLng) * cosTile * 111000.0).toFloat()
        val maxX = ((bbox.maxLng - centerLng) * cosTile * 111000.0).toFloat()
        val minZ = (-(bbox.maxLat - centerLat) * 111000.0).toFloat()
        val maxZ = (-(bbox.minLat - centerLat) * 111000.0).toFloat()

        val y = 0.10f
        // 4 vertices with interleaved Position (FLOAT3) and UV0 (FLOAT2)
        // Stride: 20 bytes (5 floats per vertex)
        val vertexData = floatArrayOf(
            minX, y, minZ,  0f, 0f, // 0: Top-Left (NW)
            maxX, y, minZ,  1f, 0f, // 1: Top-Right (NE)
            maxX, y, maxZ,  1f, 1f, // 2: Bottom-Right (SE)
            minX, y, maxZ,  0f, 1f, // 3: Bottom-Left (SW)
        )

        val indices = intArrayOf(
            0, 2, 1,  0, 1, 2,
            0, 3, 2,  0, 2, 3,
        )

        val vb = VertexBuffer.Builder()
            .vertexCount(4)
            .bufferCount(1)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 20)
            .attribute(VertexBuffer.VertexAttribute.UV0, 0, VertexBuffer.AttributeType.FLOAT2, 12, 20)
            .build(engine)

        val posBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        posBuffer.put(vertexData)
        posBuffer.flip()
        vb.setBufferAt(engine, 0, posBuffer)

        val ib = IndexBuffer.Builder()
            .indexCount(indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.UINT)
            .build(engine)

        val idxBuffer = ByteBuffer.allocateDirect(indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        idxBuffer.put(indices)
        idxBuffer.flip()
        ib.setBuffer(engine, idxBuffer)

        val matInstance = materials.createStaticGroundInstance().apply {
            setParameter("time", 0.0f)
            setParameter("opacity", 1.0f)
        }

        val em = EntityManager.get()
        val entity = em.create()
        val tm = engine.transformManager
        tm.create(entity)

        val defaultBox = Box(0f, 0f, 0f, 2000f, 50f, 2000f)

        RenderableManager.Builder(1)
            .boundingBox(defaultBox)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)
            .material(0, matInstance)
            .culling(false)
            .build(engine, entity)

        return FilamentStaticQuad(
            coordinate = coord,
            centerLat = centerLat,
            centerLng = centerLng,
            entity = entity,
            vertexBuffer = vb,
            indexBuffer = ib,
            materialInstance = matInstance,
        )
    }
}

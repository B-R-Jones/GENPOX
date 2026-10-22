package com.example.genpox.map.tile.filament

import com.example.genpox.data.AnomalyMorphology
import com.example.genpox.data.PoxAnomaly
import com.example.genpox.data.WaveMath
import com.example.genpox.ui.scanner.map.AnomalyPassRenderer
import com.example.genpox.ui.scanner.map.MarchingCubesTables
import com.example.genpox.ui.scanner.map.MetaballMeshCompiler
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * GPU Renderable entity representing a 3D volumetric Pox Anomaly in Google Filament.
 * Manages native vertex/index buffers, dynamic lifecycle scaling, and pulsing shaders.
 */
data class FilamentAnomalyRenderable(
    val entity: Int,
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer,
    val materialInstance: MaterialInstance,
    val anomalyId: String,
    val worldLat: Double,
    val worldLng: Double,
    val baseRadiusMeters: Float,
    val baseCeilingMeters: Float,
    val seed: Long,
    val morphology: AnomalyMorphology,
) {
    fun destroy(engine: Engine) {
        val em = EntityManager.get()
        val rm = engine.renderableManager
        if (entity != 0) {
            rm.destroy(entity)
            em.destroy(entity)
        }
        engine.destroyVertexBuffer(vertexBuffer)
        engine.destroyIndexBuffer(indexBuffer)
        engine.destroyMaterialInstance(materialInstance)
    }
}

/**
 * Procedural Filament 3D Mesh Compiler for GENPOX Volumetric Anomalies.
 * Uses 3D Marching Cubes to generate organic isosurfaces polygonized into
 * 32-bit UINT GPU buffers with zero CPU overhead per-frame hardware scaling.
 */
object FilamentAnomalyMeshCompiler {

    /**
     * Compiles a PoxAnomaly into a standalone Filament 3D Renderable Entity.
     * The mesh is baked at normalized unit scale centered at (0, 0, 0)
     * and animated dynamically on the GPU via TransformManager.
     */
    fun compileAnomaly(
        engine: Engine,
        anomaly: PoxAnomaly,
        materials: FilamentMaterials,
        gridResXy: Int = 32,
        gridResZ: Int = 24,
        threshold: Float = 0.22f,
    ): FilamentAnomalyRenderable {
        val rawRadius = if (anomaly.heatZoneDiameter > 0) {
            (anomaly.heatZoneDiameter / 3.28084 / 2.0).toFloat()
        } else {
            1000.0f
        }
        val radiusMeters = rawRadius.coerceIn(2.5f, 4000.0f)
        val spectralClass = anomaly.spectralClass
        val baseSummitMeters = spectralClass.calculateBaseCeiling(radiusMeters, anomaly.morphology)
        val seed = (anomaly.id.hashCode().toLong() and 0xFFFFFFFFL) xor (anomaly.gene.hashCode().toLong() shl 32)

        val emitters = MetaballMeshCompiler.generateProceduralEmitters(
            centerX = 0f,
            centerY = 0f,
            totalRadius = radiusMeters,
            seed = seed,
            heightScale = 1.0f,
            radiusScale = 1.0f,
            baseCeilingMeters = baseSummitMeters,
            morphology = anomaly.morphology,
        )

        // 1. Calculate Bounding Box
        var bMinX = Float.MAX_VALUE
        var bMaxX = -Float.MAX_VALUE
        var bMinY = Float.MAX_VALUE
        var bMaxY = -Float.MAX_VALUE
        var bMaxZ = 0.0f
        for (e in emitters) {
            if (e.x - e.radiusXy < bMinX) bMinX = e.x - e.radiusXy
            if (e.x + e.radiusXy > bMaxX) bMaxX = e.x + e.radiusXy
            if (e.y - e.radiusXy < bMinY) bMinY = e.y - e.radiusXy
            if (e.y + e.radiusXy > bMaxY) bMaxY = e.y + e.radiusXy
            if (e.z + e.radiusZ > bMaxZ) bMaxZ = e.z + e.radiusZ
        }

        val paddingXy = minOf(20.0f, maxOf(4.0f, (bMaxX - bMinX) * 0.08f))
        val paddingZ = minOf(6.0f, maxOf(0.4f, bMaxZ * 0.10f))
        val minX = bMinX - paddingXy
        val maxX = bMaxX + paddingXy
        val minY = bMinY - paddingXy
        val maxY = bMaxY + paddingXy
        val minZ = 0.0f
        val maxZ = bMaxZ + paddingZ

        val stepX = (maxX - minX) / gridResXy
        val stepY = (maxY - minY) / gridResXy
        val stepZ = (maxZ - minZ) / gridResZ

        // 2. Sample 3D Grid Scalar Field
        val dimX = gridResXy + 1
        val dimY = gridResXy + 1
        val dimZ = gridResZ + 1
        val grid = FloatArray(dimX * dimY * dimZ)

        fun gridIndex(ix: Int, iy: Int, iz: Int) = iz * (dimX * dimY) + iy * dimX + ix

        for (iz in 0 until dimZ) {
            val gz = minZ + iz * stepZ
            for (iy in 0 until dimY) {
                val gy = minY + iy * stepY
                for (ix in 0 until dimX) {
                    val gx = minX + ix * stepX
                    grid[gridIndex(ix, iy, iz)] = MetaballMeshCompiler.evaluateDensity(gx, gy, gz, emitters)
                }
            }
        }

        // 3. Marching Cubes Triangulation with Shared Edge-Welded Vertices
        // Filament mapping: X -> East/West, Y -> Skyward (Altitude Z), Z -> North/South (Y)
        val rawPositions = ArrayList<Float>(16384)
        val rawIndices = ArrayList<Int>(16384)
        val edgeMap = IntArray(dimX * dimY * dimZ * 3) { -1 }

        fun getGlobalEdgeIndex(ix: Int, iy: Int, iz: Int, localEdge: Int): Int {
            var gx = ix
            var gy = iy
            var gz = iz
            var dir = 0
            when (localEdge) {
                0 -> { dir = 0 }
                1 -> { gx = ix + 1; dir = 1 }
                2 -> { gy = iy + 1; dir = 0 }
                3 -> { dir = 1 }
                4 -> { gz = iz + 1; dir = 0 }
                5 -> { gx = ix + 1; gz = iz + 1; dir = 1 }
                6 -> { gy = iy + 1; gz = iz + 1; dir = 0 }
                7 -> { gz = iz + 1; dir = 1 }
                8 -> { dir = 2 }
                9 -> { gx = ix + 1; dir = 2 }
                10 -> { gx = ix + 1; gy = iy + 1; dir = 2 }
                11 -> { gy = iy + 1; dir = 2 }
            }
            return (gz * (dimY * dimX) + gy * dimX + gx) * 3 + dir
        }

        val cornerPos = Array(8) { FloatArray(3) }
        val cornerVal = FloatArray(8)
        val edgeVerts = Array(12) { FloatArray(3) }

        fun getOrCreateVertex(ix: Int, iy: Int, iz: Int, localEdge: Int): Int {
            val globalIdx = getGlobalEdgeIndex(ix, iy, iz, localEdge)
            val existing = edgeMap[globalIdx]
            if (existing != -1) return existing

            val v = edgeVerts[localEdge]
            val newIdx = rawPositions.size / 3
            // Convert to Filament coordinates: (X, Altitude/Y, North-South/Z)
            rawPositions.add(v[0]) // Filament X = local x
            rawPositions.add(v[2]) // Filament Y = local z (altitude)
            rawPositions.add(v[1]) // Filament Z = local y
            edgeMap[globalIdx] = newIdx
            return newIdx
        }

        for (iz in 0 until gridResZ) {
            val z0 = minZ + iz * stepZ
            val z1 = z0 + stepZ
            for (iy in 0 until gridResXy) {
                val y0 = minY + iy * stepY
                val y1 = y0 + stepY
                for (ix in 0 until gridResXy) {
                    val x0 = minX + ix * stepX
                    val x1 = x0 + stepX

                    cornerPos[0][0] = x0; cornerPos[0][1] = y0; cornerPos[0][2] = z0
                    cornerVal[0] = grid[gridIndex(ix, iy, iz)]

                    cornerPos[1][0] = x1; cornerPos[1][1] = y0; cornerPos[1][2] = z0
                    cornerVal[1] = grid[gridIndex(ix + 1, iy, iz)]

                    cornerPos[2][0] = x1; cornerPos[2][1] = y1; cornerPos[2][2] = z0
                    cornerVal[2] = grid[gridIndex(ix + 1, iy + 1, iz)]

                    cornerPos[3][0] = x0; cornerPos[3][1] = y1; cornerPos[3][2] = z0
                    cornerVal[3] = grid[gridIndex(ix, iy + 1, iz)]

                    cornerPos[4][0] = x0; cornerPos[4][1] = y0; cornerPos[4][2] = z1
                    cornerVal[4] = grid[gridIndex(ix, iy, iz + 1)]

                    cornerPos[5][0] = x1; cornerPos[5][1] = y0; cornerPos[5][2] = z1
                    cornerVal[5] = grid[gridIndex(ix + 1, iy, iz + 1)]

                    cornerPos[6][0] = x1; cornerPos[6][1] = y1; cornerPos[6][2] = z1
                    cornerVal[6] = grid[gridIndex(ix + 1, iy + 1, iz + 1)]

                    cornerPos[7][0] = x0; cornerPos[7][1] = y1; cornerPos[7][2] = z1
                    cornerVal[7] = grid[gridIndex(ix, iy + 1, iz + 1)]

                    var cubeIndex = 0
                    for (c in 0 until 8) {
                        if (cornerVal[c] >= threshold) {
                            cubeIndex = cubeIndex or (1 shl c)
                        }
                    }

                    if (cubeIndex == 0 || cubeIndex == 255) continue

                    val edgeMask = MarchingCubesTables.EDGE_TABLE[cubeIndex]
                    if (edgeMask == 0) continue

                    for (e in 0 until 12) {
                        if ((edgeMask and (1 shl e)) != 0) {
                            val cPair = MarchingCubesTables.EDGE_CORNER_MAP[e]
                            val c1 = cPair.first
                            val c2 = cPair.second
                            val v1 = cornerVal[c1]
                            val v2 = cornerVal[c2]
                            val mu = if (abs(v2 - v1) > 1e-5f) {
                                (threshold - v1) / (v2 - v1)
                            } else {
                                0.5f
                            }.coerceIn(0.0f, 1.0f)

                            val p1 = cornerPos[c1]
                            val p2 = cornerPos[c2]

                            edgeVerts[e][0] = p1[0] + mu * (p2[0] - p1[0])
                            edgeVerts[e][1] = p1[1] + mu * (p2[1] - p1[1])
                            edgeVerts[e][2] = max(0.0f, p1[2] + mu * (p2[2] - p1[2]))
                        }
                    }

                    val triList = MarchingCubesTables.TRI_TABLE[cubeIndex]
                    var tIdx = 0
                    while (tIdx < triList.size && triList[tIdx] != -1) {
                        val e0 = triList[tIdx]
                        val e1 = triList[tIdx + 1]
                        val e2 = triList[tIdx + 2]
                        tIdx += 3

                        val idx0 = getOrCreateVertex(ix, iy, iz, e0)
                        val idx1 = getOrCreateVertex(ix, iy, iz, e1)
                        val idx2 = getOrCreateVertex(ix, iy, iz, e2)

                        // Dual winding for complete double-sided illumination and occlusion
                        rawIndices.add(idx0)
                        rawIndices.add(idx1)
                        rawIndices.add(idx2)

                        rawIndices.add(idx0)
                        rawIndices.add(idx2)
                        rawIndices.add(idx1)
                    }
                }
            }
        }

        // 4. Build GPU Vertex & Index Buffers
        val vertCount = rawPositions.size / 3
        val vb = VertexBuffer.Builder()
            .vertexCount(vertCount)
            .bufferCount(1)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
            .build(engine)

        val posBuffer: FloatBuffer = ByteBuffer.allocateDirect(rawPositions.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (f in rawPositions) posBuffer.put(f)
        posBuffer.flip()
        vb.setBufferAt(engine, 0, posBuffer)

        val ib = IndexBuffer.Builder()
            .indexCount(rawIndices.size)
            .bufferType(IndexBuffer.Builder.IndexType.UINT)
            .build(engine)

        val idxBuffer: IntBuffer = ByteBuffer.allocateDirect(rawIndices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        for (i in rawIndices) idxBuffer.put(i)
        idxBuffer.flip()
        ib.setBuffer(engine, idxBuffer)

        // 5. Create Entity and Renderable
        val em = EntityManager.get()
        val entity = em.create()
        val materialInst = materials.createAnomalyMaterialInstance()

        val boundingBox = Box(
            0f, maxZ * 0.5f, 0f,
            radiusMeters, maxZ * 0.5f, radiusMeters,
        )

        RenderableManager.Builder(1)
            .boundingBox(boundingBox)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)
            .material(0, materialInst)
            .culling(false)
            .build(engine, entity)

        return FilamentAnomalyRenderable(
            entity = entity,
            vertexBuffer = vb,
            indexBuffer = ib,
            materialInstance = materialInst,
            anomalyId = anomaly.id,
            worldLat = anomaly.lat,
            worldLng = anomaly.lng,
            baseRadiusMeters = radiusMeters,
            baseCeilingMeters = baseSummitMeters,
            seed = seed,
            morphology = anomaly.morphology,
        )
    }

    /**
     * Updates an anomaly renderable's 4x4 transform matrix and pulsing shader uniforms.
     * Executes entirely on the GPU via TransformManager without any CPU vertex iteration.
     */
    fun updateTransform(
        engine: Engine,
        renderable: FilamentAnomalyRenderable,
        anchorLat: Double,
        anchorLng: Double,
        timeMs: Long = WaveMath.getAdjustedTime(),
    ) {
        val cosAnchor = cos(Math.toRadians(anchorLat))
        val relX = ((renderable.worldLng - anchorLng) * cosAnchor * 111000.0).toFloat()
        val relZ = (-(renderable.worldLat - anchorLat) * 111000.0).toFloat()

        val metrics = WaveMath.calculateAnomalyLifecycle(
            seed = renderable.seed,
            timeMs = timeMs,
            baseSummitMeters = renderable.baseCeilingMeters,
        )

        val tm = engine.transformManager
        val instance = tm.getInstance(renderable.entity)
        if (instance != 0) {
            val transform = FloatArray(16)
            // Column-major 4x4 matrix: Scale (r, h, r) * Translation (relX, 0, relZ)
            transform[0] = metrics.radiusMultiplier
            transform[5] = metrics.heightMultiplier
            transform[10] = metrics.radiusMultiplier
            transform[12] = relX
            transform[13] = 0.0f
            transform[14] = relZ
            transform[15] = 1.0f
            tm.setTransform(instance, transform)
        }

        // Update lifecycle color uniforms
        val ambientPulse = sin((timeMs / 1000.0 * 2.5).toFloat())
        val colors = AnomalyPassRenderer.evaluateSmoothAnomalyColors(metrics.progress, ambientPulse)
        renderable.materialInstance.setParameter(
            "baseColor",
            colors.fr / 255.0f,
            colors.fg / 255.0f,
            colors.fb / 255.0f,
            1.0f,
        )
    }
}

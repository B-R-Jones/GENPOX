package com.example.genpox.map.tile.filament

import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import com.example.genpox.data.PoxAnomaly
import com.example.genpox.data.WaveMath
import com.example.genpox.map.tile.TileCoordinate
import com.example.genpox.map.tile.TileGeometry
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.Filament
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance 3D hardware-accelerated Map Renderer powered by Google Filament.
 * Provides Vulkan/OpenGL ES hardware Z-buffer depth testing, 4-draw-call tile batching,
 * dynamic shader pulse animations, and true 3D orbital camera projections.
 *
 * Uses tile-local coordinate systems positioned via TransformManager to ensure zero-overhead
 * anchor adjustments and eliminates lag spikes when panning or streaming sectors.
 */
class FilamentMapRenderer(
    private val surfaceView: SurfaceView,
    private val onFpsUpdated: (Int) -> Unit = {},
) {

    private val engine: Engine
    private val renderer: Renderer
    private val scene: Scene
    private val view: View
    private val camera: Camera
    private val uiHelper: UiHelper

    private var swapChain: SwapChain? = null
    private val materials: FilamentMaterials
    private val activeTileMeshes = mutableMapOf<TileCoordinate, FilamentTileMesh>()
    private var currentSectorCoords: Set<TileCoordinate> = emptySet()
    private val entitiesInScene = mutableSetOf<Int>()
    var isTileCached: ((TileCoordinate) -> Boolean)? = null

    private val activeAnomalyMeshes = mutableMapOf<String, FilamentAnomalyRenderable>()
    private var currentAnomalies: List<PoxAnomaly> = emptyList()
    var anchorLat: Double = 39.0997
    var anchorLng: Double = -94.5786

    private var width = 1
    private var height = 1
    private var aspect = 1.0

    // Camera state
    private var panX = 0f
    private var panZ = 0f
    private var zoom = 1.0f
    private var tiltProgress = 0f
    private var pitchAngle: Float? = null
    private var rotationDegrees = 0f

    // Reusable transform scratch buffer to prevent per-frame allocations
    private val transformScratch = FloatArray(16)

    // FPS calculation
    private var frameCount = 0
    private var lastFpsTimestamp = 0L

    private val rendererScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var asyncMeshJob: Job? = null
    private val compiledDataCache = mutableMapOf<TileCoordinate, CompiledTileData>()

    private val choreographer = Choreographer.getInstance()
    private var isDestroyed = false

    var isPaused: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (!value && !isDestroyed) {
                choreographer.postFrameCallback(frameCallback)
            }
        }

    private fun addSceneEntity(entity: Int) {
        if (entity != 0 && entitiesInScene.add(entity)) {
            scene.addEntity(entity)
        }
    }

    private fun removeSceneEntity(entity: Int) {
        if (entity != 0 && entitiesInScene.remove(entity)) {
            scene.removeEntity(entity)
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isDestroyed || isPaused) return
            choreographer.postFrameCallback(this)

            // FPS Counter
            frameCount++
            if (lastFpsTimestamp == 0L) lastFpsTimestamp = frameTimeNanos
            val elapsedNanos = frameTimeNanos - lastFpsTimestamp
            if (elapsedNanos >= 1_000_000_000L) {
                val fps = (frameCount * 1_000_000_000.0 / elapsedNanos).toInt()
                onFpsUpdated(fps)
                frameCount = 0
                lastFpsTimestamp = frameTimeNanos
            }

            renderFrame(frameTimeNanos)
        }
    }

    init {
        Filament.init()
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        val cameraEntity = engine.entityManager.create()
        camera = engine.createCamera(cameraEntity)

        view.camera = camera
        view.scene = scene

        // Cyberpunk Void background: #010804
        val clearOptions = renderer.clearOptions
        clearOptions.clear = true
        clearOptions.clearColor = doubleArrayOf(0.004, 0.031, 0.016, 1.0)
        renderer.clearOptions = clearOptions

        materials = FilamentMaterials(engine)

        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowChanged(surface: Surface) {
                    swapChain?.let { engine.destroySwapChain(it) }
                    swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
                }

                override fun onDetachedFromSurface() {
                    swapChain?.let { engine.destroySwapChain(it) }
                    swapChain = null
                }

                override fun onResized(w: Int, h: Int) {
                    width = w.coerceAtLeast(1)
                    height = h.coerceAtLeast(1)
                    aspect = width.toDouble() / height.toDouble()
                    view.viewport = Viewport(0, 0, width, height)
                    updateCameraProjection()
                }
            }
            attachTo(surfaceView)
        }

        choreographer.postFrameCallback(frameCallback)
    }

    private val activeTileGeometries = mutableMapOf<TileCoordinate, TileGeometry>()
    private val activeStaticQuads = mutableMapOf<TileCoordinate, FilamentStaticQuad>()

    /**
     * Updates the geographic scene anchor.
     * Repositions all active tile meshes and static screen quads in world space via hardware TransformManager
     * in < 0.01ms without destroying or re-allocating any GPU vertex or index buffers.
     */
    fun setSceneAnchor(lat: Double, lng: Double) {
        val dLat = kotlin.math.abs(anchorLat - lat)
        val dLng = kotlin.math.abs(anchorLng - lng)
        if (dLat > 0.000001 || dLng > 0.000001) {
            anchorLat = lat
            anchorLng = lng
            for (mesh in activeTileMeshes.values) {
                updateTileTransform(mesh)
            }
            for (quad in activeStaticQuads.values) {
                updateStaticQuadTransform(quad)
            }
        }
    }

    /**
     * Controls the real-time extrusion height of 3D buildings from 0.0 (flat at ground) to 1.0 (full height).
     */
    var buildingExtrusion: Float = 1.0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field != clamped) {
                field = clamped
                for (mesh in activeTileMeshes.values) {
                    updateTileTransform(mesh)
                }
            }
        }

    private fun updateTileTransform(mesh: FilamentTileMesh, extrusionOverride: Float? = null) {
        val cosAnchor = cos(Math.toRadians(anchorLat))
        val tileWorldX = ((mesh.centerLng - anchorLng) * cosAnchor * 111000.0).toFloat()
        val tileWorldZ = (-(mesh.centerLat - anchorLat) * 111000.0).toFloat()

        val tm = engine.transformManager

        // Building entities scale vertically based on extrusionOverride or buildingExtrusion (0f to 1f)
        val activeExtrusion = extrusionOverride ?: buildingExtrusion
        val buildingScaleY = kotlin.math.max(0.0001f, activeExtrusion)

        // 4x4 Column-Major Matrix for Buildings (walls, roofs, wireframe rims)
        transformScratch[0] = 1f; transformScratch[1] = 0f; transformScratch[2] = 0f; transformScratch[3] = 0f
        transformScratch[4] = 0f; transformScratch[5] = buildingScaleY; transformScratch[6] = 0f; transformScratch[7] = 0f
        transformScratch[8] = 0f; transformScratch[9] = 0f; transformScratch[10] = 1f; transformScratch[11] = 0f
        transformScratch[12] = tileWorldX; transformScratch[13] = 0f; transformScratch[14] = tileWorldZ; transformScratch[15] = 1f

        val buildingEntities = intArrayOf(
            mesh.wallEntity, mesh.roofEntity, mesh.wireframeEntity,
        )
        for (e in buildingEntities) {
            if (e != 0) {
                val instance = if (tm.hasComponent(e)) tm.getInstance(e) else tm.create(e)
                tm.setTransform(instance, transformScratch)
            }
        }

        // Road entities remain on the ground with standard 1.0f vertical scale
        if (mesh.roadEntity != 0) {
            transformScratch[5] = 1.0f
            val instance = if (tm.hasComponent(mesh.roadEntity)) tm.getInstance(mesh.roadEntity) else tm.create(mesh.roadEntity)
            tm.setTransform(instance, transformScratch)
        }
    }

    private fun updateStaticQuadTransform(quad: FilamentStaticQuad) {
        val cosAnchor = cos(Math.toRadians(anchorLat))
        val tileWorldX = ((quad.centerLng - anchorLng) * cosAnchor * 111000.0).toFloat()
        val tileWorldZ = (-(quad.centerLat - anchorLat) * 111000.0).toFloat()

        val tm = engine.transformManager
        transformScratch[0] = 1f; transformScratch[1] = 0f; transformScratch[2] = 0f; transformScratch[3] = 0f
        transformScratch[4] = 0f; transformScratch[5] = 1f; transformScratch[6] = 0f; transformScratch[7] = 0f
        transformScratch[8] = 0f; transformScratch[9] = 0f; transformScratch[10] = 1f; transformScratch[11] = 0f
        transformScratch[12] = tileWorldX; transformScratch[13] = 0f; transformScratch[14] = tileWorldZ; transformScratch[15] = 1f

        if (quad.entity != 0) {
            val instance = if (tm.hasComponent(quad.entity)) tm.getInstance(quad.entity) else tm.create(quad.entity)
            tm.setTransform(instance, transformScratch)
        }
    }

    private fun updateSectorStaticQuads() {
        // Evict quads outside currentSectorCoords or whose 3D mesh has completed its emergence transition
        val toRemove = activeStaticQuads.keys.filter { coord ->
            coord !in currentSectorCoords || activeTileMeshes[coord]?.isTransitionComplete == true
        }
        for (coord in toRemove) {
            val quad = activeStaticQuads.remove(coord) ?: continue
            removeSceneEntity(quad.entity)
            quad.destroy(engine)
        }

        // Add quads for sector coordinates that do not yet have a 3D mesh (and are not cached on disk), OR whose 3D mesh is still emerging
        for (coord in currentSectorCoords) {
            val mesh = activeTileMeshes[coord]
            val isCached = isTileCached?.invoke(coord) ?: false
            val needsQuad = (mesh == null && !isCached) || (mesh != null && !mesh.isTransitionComplete)
            if (needsQuad && !activeStaticQuads.containsKey(coord)) {
                val quad = FilamentTileMeshCompiler.compileStaticGroundQuad(
                    engine = engine,
                    coord = coord,
                    materials = materials,
                )
                activeStaticQuads[coord] = quad
                addSceneEntity(quad.entity)
                updateStaticQuadTransform(quad)
                android.util.Log.d("FilamentMapRenderer", "Spawned static quad for $coord (meshNull=${mesh == null}, isCached=$isCached)")
            }
        }
    }

    fun setTileGeometry(geometry: TileGeometry) {
        setTiles(listOf(geometry))
    }

    fun setSector(sectorCoords: Set<TileCoordinate>, tiles: Collection<TileGeometry>) {
        currentSectorCoords = sectorCoords
        setTiles(tiles)
        updateSectorStaticQuads()
        android.util.Log.d("FilamentMapRenderer", "setSector: coords=${sectorCoords.size}, tiles=${tiles.size}, activeQuads=${activeStaticQuads.size}, activeMeshes=${activeTileMeshes.size}")
    }

    fun setSectorCoordinates(coords: Set<TileCoordinate>) {
        currentSectorCoords = coords
        updateSectorStaticQuads()
    }

    /**
     * Updates the multi-tile scene:
     * Adds GPU meshes for new incoming tile geometries with emergence rise animations.
     * Geometry compilation (polygon triangulation) runs off-thread on Dispatchers.Default,
     * while GPU uploads take < 1ms on the UI thread.
     * Evicts GPU meshes for tiles that are no longer active.
     */
    fun setTiles(tiles: Collection<TileGeometry>) {
        activeTileGeometries.clear()
        for (g in tiles) {
            activeTileGeometries[g.coordinate] = g
        }
        val newCoordSet = tiles.map { it.coordinate }.toSet()

        // 1. Remove tiles that are no longer active
        val toRemove = activeTileMeshes.keys.filter { it !in newCoordSet }
        for (coord in toRemove) {
            val old = activeTileMeshes.remove(coord) ?: continue
            removeSceneEntity(old.wallEntity)
            removeSceneEntity(old.roofEntity)
            removeSceneEntity(old.wireframeEntity)
            removeSceneEntity(old.roadEntity)
            old.destroy(engine)
        }

        // 2. Identify missing tiles needing GPU upload
        val missingFromGpu = tiles.filter { !activeTileMeshes.containsKey(it.coordinate) }
        val toAttachImmediately = mutableListOf<CompiledTileData>()
        val needBackgroundCompilation = mutableListOf<TileGeometry>()

        for (geom in missingFromGpu) {
            val cached = compiledDataCache[geom.coordinate]
            if (cached != null) {
                toAttachImmediately.add(cached)
            } else {
                needBackgroundCompilation.add(geom)
            }
        }

        // Attach any pre-compiled tiles immediately (< 1ms per tile)
        for (compiled in toAttachImmediately) {
            val geom = activeTileGeometries[compiled.coordinate] ?: continue
            attachCompiledTile(compiled, isEmerging = !geom.isFromCache)
        }

        // Compile any uncached tiles asynchronously off the UI thread
        if (needBackgroundCompilation.isNotEmpty()) {
            val anoms = currentAnomalies
            asyncMeshJob?.cancel()
            asyncMeshJob = rendererScope.launch {
                val compiledBatch = withContext(Dispatchers.Default) {
                    needBackgroundCompilation.map { geom ->
                        async {
                            FilamentTileMeshCompiler.compileTileGeometry(geom, anoms)
                        }
                    }.awaitAll()
                }

                if (!isDestroyed) {
                    for (compiled in compiledBatch) {
                        compiledDataCache[compiled.coordinate] = compiled
                        val geom = activeTileGeometries[compiled.coordinate] ?: continue
                        if (compiled.coordinate in currentSectorCoords && !activeTileMeshes.containsKey(compiled.coordinate)) {
                            attachCompiledTile(compiled, isEmerging = !geom.isFromCache)
                        }
                    }
                    updateSectorStaticQuads()
                }
            }
        }
    }

    private fun attachCompiledTile(compiled: CompiledTileData, isEmerging: Boolean) {
        val mesh = FilamentTileMeshCompiler.uploadTileToGpu(
            engine = engine,
            compiled = compiled,
            materials = materials,
            isEmerging = isEmerging,
        )
        activeTileMeshes[compiled.coordinate] = mesh

        if (mesh.wallEntity != 0) addSceneEntity(mesh.wallEntity)
        if (mesh.roofEntity != 0) addSceneEntity(mesh.roofEntity)
        if (mesh.wireframeEntity != 0) addSceneEntity(mesh.wireframeEntity)
        if (mesh.roadEntity != 0) addSceneEntity(mesh.roadEntity)
        updateTileTransform(mesh, extrusionOverride = if (mesh.isTransitionComplete) null else 0.0001f)

        // Emerging tile needs its SMPTE static quad for Phase 1 cross-fade
        if (!mesh.isTransitionComplete && !activeStaticQuads.containsKey(compiled.coordinate)) {
            val quad = FilamentTileMeshCompiler.compileStaticGroundQuad(
                engine = engine,
                coord = compiled.coordinate,
                materials = materials,
            )
            activeStaticQuads[compiled.coordinate] = quad
            addSceneEntity(quad.entity)
            updateStaticQuadTransform(quad)
            android.util.Log.d("FilamentMapRenderer", "Spawned emergence static quad for ${compiled.coordinate}")
        }
    }

    /**
     * Updates the active volumetric anomalies rendered in Filament.
     * Adds new anomaly meshes to the scene and evicts absent ones.
     */
    fun setAnomalies(anomalies: Collection<PoxAnomaly>) {
        val newIdSet = anomalies.map { it.id }.toSet()
        currentAnomalies = anomalies.toList()
        android.util.Log.i("FilamentMapRenderer", "setAnomalies: count=${anomalies.size}, list=${anomalies.map { "${it.id} lat=${it.lat}, lng=${it.lng}, heatZone=${it.heatZoneDiameter}" }}")

        // 1. Remove anomalies no longer in the active collection
        val toRemove = activeAnomalyMeshes.keys.filter { it !in newIdSet }
        for (id in toRemove) {
            val old = activeAnomalyMeshes.remove(id) ?: continue
            scene.removeEntity(old.entity)
            old.destroy(engine)
        }

        // 2. Add newly spawned anomalies
        for (anom in anomalies) {
            if (activeAnomalyMeshes.containsKey(anom.id)) continue
            val renderable = FilamentAnomalyMeshCompiler.compileAnomaly(
                engine = engine,
                anomaly = anom,
                materials = materials,
            )
            activeAnomalyMeshes[anom.id] = renderable
            scene.addEntity(renderable.entity)
        }
    }

    fun updateCamera(
        newPanX: Float,
        newPanY: Float,
        newZoom: Float,
        newTiltProgress: Float,
        newRotationDegrees: Float,
        newPitchAngle: Float? = null,
    ) {
        panX = newPanX
        panZ = newPanY
        zoom = newZoom
        tiltProgress = newTiltProgress
        pitchAngle = newPitchAngle
        rotationDegrees = newRotationDegrees

        updateCameraProjection()
    }

    private fun updateCameraProjection() {
        if (width <= 0 || height <= 0) return

        // Pitch tilt: 90 deg (top-down) down to 15 deg (tilted oblique)
        val pitchDeg = (pitchAngle?.toDouble() ?: (90.0 - (tiltProgress * 60.0))).coerceIn(15.0, 90.0)
        val pitchRad = Math.toRadians(pitchDeg)
        val yawRad = Math.toRadians(rotationDegrees.toDouble())

        // Calculate distance so that zoom accurately corresponds to screen pixels per meter
        val fovDeg = 45.0
        val fovRad = Math.toRadians(fovDeg)
        val distance = if (height > 1) {
            (height / (2.0 * kotlin.math.tan(fovRad / 2.0) * zoom.coerceAtLeast(0.01f))).coerceIn(10.0, 50000.0)
        } else {
            (1400.0 / zoom.coerceAtLeast(0.05f)).coerceIn(10.0, 30000.0)
        }

        val horizontalDist = distance * cos(pitchRad)
        val elevation = (distance * sin(pitchRad)).coerceAtLeast(1.0)

        val eyeX = panX + horizontalDist * sin(yawRad)
        val eyeY = elevation
        val eyeZ = panZ + horizontalDist * cos(yawRad)

        // Continuous Up vector for all pitch angles (90 deg top-down to horizontal) and yaw rotations.
        // Prevents gimbal lock / 180-degree flip between top-down 2D and 3D oblique views.
        val upX = -sin(pitchRad) * sin(yawRad)
        val upY = cos(pitchRad)
        val upZ = -sin(pitchRad) * cos(yawRad)

        // Point camera at the ground focus target (panX, 0, panZ)
        camera.lookAt(
            eyeX, eyeY, eyeZ,
            panX.toDouble(), 0.0, panZ.toDouble(),
            upX, upY, upZ,
        )

        // Perspective frustum with near/far clipping planes
        camera.setProjection(
            fovDeg,
            aspect,
            1.0,
            50000.0,
            Camera.Fov.VERTICAL,
        )
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val sc = swapChain ?: return

        // Update real-time shader pulse uniform (seconds, bounded to preserve float precision in shaders)
        val timeSeconds = ((android.os.SystemClock.uptimeMillis() % 100_000L) / 1000f)
        materials.updatePulse(timeSeconds)

        // Animate CRT static noise on unloaded cell static quads
        for (quad in activeStaticQuads.values) {
            quad.materialInstance.setParameter("time", timeSeconds)
        }

        // 1. Emerging tiles animation:
        //    Phase 1 (0 to 3s): Fade in buildings at 0 height, cross-fade out static TV bars
        //    Phase 2 (3 to 6s): Extrude buildings slowly to max
        //    Phase 3 (6s+): Transition complete
        for (mesh in activeTileMeshes.values) {
            if (mesh.isTransitionComplete) continue

            val elapsedSec = (frameTimeNanos - mesh.spawnTimeNanos) / 1_000_000_000.0f

            if (elapsedSec < 3.0f) {
                // Phase 1: Fade in over 3 seconds at 0 height
                val rawProgress = (elapsedSec / 3.0f).coerceIn(0f, 1f)
                val smoothAlpha = rawProgress * rawProgress * (3f - 2f * rawProgress)

                mesh.fadeWallMat?.setParameter("opacity", smoothAlpha)
                mesh.fadeRoofMat?.setParameter("opacity", smoothAlpha)
                mesh.fadeWireframeMat?.setParameter("opacity", smoothAlpha)
                mesh.fadeRoadMat?.setParameter("opacity", smoothAlpha)

                // Cross-fade out the static TV bar quad if present
                activeStaticQuads[mesh.coordinate]?.materialInstance?.setParameter(
                    "opacity",
                    (1.0f - smoothAlpha).coerceIn(0f, 1f)
                )

                updateTileTransform(mesh, extrusionOverride = 0.0001f)

            } else if (elapsedSec < 6.0f) {
                // Phase 2: Extrude over 3 seconds to max
                val quad = activeStaticQuads.remove(mesh.coordinate)
                if (quad != null) {
                    removeSceneEntity(quad.entity)
                    quad.destroy(engine)
                }

                swapToSolidMaterials(mesh)

                val extrudeProgress = ((elapsedSec - 3.0f) / 3.0f).coerceIn(0f, 1f)
                val smoothExtrude = extrudeProgress * extrudeProgress * (3f - 2f * extrudeProgress)
                val currentHeight = smoothExtrude * buildingExtrusion

                updateTileTransform(mesh, extrusionOverride = currentHeight)

            } else {
                // Phase 3: Transition complete
                val quad = activeStaticQuads.remove(mesh.coordinate)
                if (quad != null) {
                    removeSceneEntity(quad.entity)
                    quad.destroy(engine)
                }

                swapToSolidMaterials(mesh)
                updateTileTransform(mesh, extrusionOverride = buildingExtrusion)
                mesh.isTransitionComplete = true
            }
        }

        // 2. Update dynamic anomaly hardware transforms and colors on GPU
        val currentMs = WaveMath.getAdjustedTime()
        for (anomRenderable in activeAnomalyMeshes.values) {
            FilamentAnomalyMeshCompiler.updateTransform(
                engine = engine,
                renderable = anomRenderable,
                anchorLat = anchorLat,
                anchorLng = anchorLng,
                timeMs = currentMs,
            )
        }

        // 3. Update building shader infection distance field uniforms in real time
        val anomalyUniforms = materials.updateAnomalyUniforms(currentAnomalies, anchorLat, anchorLng, currentMs)
        for (mesh in activeTileMeshes.values) {
            if (!mesh.isTransitionComplete) {
                mesh.fadeWallMat?.let { materials.applyAnomalyUniforms(it, anomalyUniforms) }
                mesh.fadeRoofMat?.let { materials.applyAnomalyUniforms(it, anomalyUniforms) }
                mesh.fadeWireframeMat?.let { materials.applyAnomalyUniforms(it, anomalyUniforms) }
            }
        }

        if (renderer.beginFrame(sc, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    private fun swapToSolidMaterials(mesh: FilamentTileMesh) {
        if (mesh.fadeWallMat == null && mesh.fadeRoofMat == null) return

        val rm = engine.renderableManager
        if (mesh.wallEntity != 0 && rm.hasComponent(mesh.wallEntity)) {
            rm.setMaterialInstanceAt(rm.getInstance(mesh.wallEntity), 0, materials.wallMaterial)
        }
        if (mesh.roofEntity != 0 && rm.hasComponent(mesh.roofEntity)) {
            rm.setMaterialInstanceAt(rm.getInstance(mesh.roofEntity), 0, materials.roofMaterial)
        }
        if (mesh.wireframeEntity != 0 && rm.hasComponent(mesh.wireframeEntity)) {
            rm.setMaterialInstanceAt(rm.getInstance(mesh.wireframeEntity), 0, materials.wireframeMaterial)
        }
        if (mesh.roadEntity != 0 && rm.hasComponent(mesh.roadEntity)) {
            rm.setMaterialInstanceAt(rm.getInstance(mesh.roadEntity), 0, materials.roadMaterial)
        }

        mesh.fadeWallMat?.let { engine.destroyMaterialInstance(it); mesh.fadeWallMat = null }
        mesh.fadeRoofMat?.let { engine.destroyMaterialInstance(it); mesh.fadeRoofMat = null }
        mesh.fadeWireframeMat?.let { engine.destroyMaterialInstance(it); mesh.fadeWireframeMat = null }
        mesh.fadeRoadMat?.let { engine.destroyMaterialInstance(it); mesh.fadeRoadMat = null }
    }

    fun destroy() {
        isDestroyed = true
        choreographer.removeFrameCallback(frameCallback)
        asyncMeshJob?.cancel()
        rendererScope.cancel()
        compiledDataCache.clear()

        for (mesh in activeTileMeshes.values) {
            removeSceneEntity(mesh.wallEntity)
            removeSceneEntity(mesh.roofEntity)
            removeSceneEntity(mesh.wireframeEntity)
            removeSceneEntity(mesh.roadEntity)
            mesh.destroy(engine)
        }
        activeTileMeshes.clear()

        for (anom in activeAnomalyMeshes.values) {
            removeSceneEntity(anom.entity)
            anom.destroy(engine)
        }
        activeAnomalyMeshes.clear()

        for (quad in activeStaticQuads.values) {
            removeSceneEntity(quad.entity)
            quad.destroy(engine)
        }
        activeStaticQuads.clear()
        entitiesInScene.clear()

        materials.destroy()

        val em = engine.entityManager
        engine.destroyCameraComponent(camera.entity)
        em.destroy(camera.entity)

        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyRenderer(renderer)

        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null

        engine.destroy()
        uiHelper.detach()
    }
}

package com.example.genpox.map.tile.filament

import android.util.Log
import com.example.genpox.data.PoxAnomaly
import com.example.genpox.data.WaveMath
import com.example.genpox.ui.scanner.map.MetaballMeshCompiler
import com.google.android.filament.Engine
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.filamat.MaterialPackage
import java.nio.ByteBuffer
import kotlin.math.cos

/**
 * Manages compiled Cyberpunk shaders for Google Filament.
 * Consolidates shaders into 3 core parameterized materials (Opaque Unlit, Transparent Fade,
 * and Procedural SMPTE Static Ground) with a real-time GPU distance field for continuous,
 * seamless building infection highlighting across tile boundaries without CPU triangulation overhead.
 */
class FilamentMaterials(private val engine: Engine) {

    companion object {
        private const val TAG = "FilamentMaterials"

        // Cyberpunk Authentic Color Palettes
        val COLOR_WALL = floatArrayOf(0.004f, 0.180f, 0.063f, 1.0f)       // Deep Emerald (#012E10)
        val COLOR_ROOF = floatArrayOf(0.012f, 0.298f, 0.110f, 1.0f)       // Obsidian / Cyber Jade (#034C1C)
        val COLOR_WIREFRAME = floatArrayOf(0.000f, 1.000f, 0.533f, 1.0f)  // Vivid Neon Cyber Green (#00FF88)
        val COLOR_ROAD = floatArrayOf(0.000f, 1.000f, 0.533f, 1.0f)       // Cyber Green Glow (#00FF88)
        val COLOR_INFECTED_WALL = floatArrayOf(0.160f, 0.020f, 0.035f, 1.0f) // Deep Crimson (#290509)
        val COLOR_INFECTED_ROOF = floatArrayOf(0.320f, 0.080f, 0.020f, 1.0f) // Smoldering Amber (#521405)
        val COLOR_INFECTED_WIREFRAME = floatArrayOf(1.000f, 0.450f, 0.050f, 1.0f) // Neon Cyber Amber (#FF730D)
        val COLOR_ANOMALY = floatArrayOf(0.800f, 0.100f, 0.270f, 0.85f)    // Cyber Neon Pink Glow (#CC1A44)
    }

    private val rawSolidMaterial: Material
    private val rawFadeMaterial: Material
    private val rawStaticGroundMaterial: Material

    val wallMaterial: MaterialInstance
    val roofMaterial: MaterialInstance
    val wireframeMaterial: MaterialInstance
    val roadMaterial: MaterialInstance
    val infectedWallMaterial: MaterialInstance
    val infectedRoofMaterial: MaterialInstance
    val anomalyMaterial: MaterialInstance

    init {
        MaterialBuilder.init()

        // 1. Core Parameterized Unlit Opaque Material with Distance Field Infection
        rawSolidMaterial = buildParameterizedSolidMaterial()

        // 2. Core Parameterized Unlit Transparent Fade Material with Distance Field Infection
        rawFadeMaterial = buildParameterizedFadeMaterial()

        // 3. Unloaded Cell Static Ground Quad: SMPTE CRT TV Bars
        rawStaticGroundMaterial = buildStaticGroundMaterial()

        // Instantiate solid material instances with authentic colors and infection uniforms
        wallMaterial = rawSolidMaterial.createInstance().apply {
            initInstance(this, COLOR_WALL, COLOR_INFECTED_WALL)
        }
        roofMaterial = rawSolidMaterial.createInstance().apply {
            initInstance(this, COLOR_ROOF, COLOR_INFECTED_ROOF)
        }
        wireframeMaterial = rawSolidMaterial.createInstance().apply {
            initInstance(this, COLOR_WIREFRAME, COLOR_INFECTED_WIREFRAME)
        }
        roadMaterial = rawSolidMaterial.createInstance().apply {
            initInstance(this, COLOR_ROAD, COLOR_ROAD)
        }
        infectedWallMaterial = rawSolidMaterial.createInstance().apply {
            initInstance(this, COLOR_INFECTED_WALL, COLOR_INFECTED_WALL)
        }
        infectedRoofMaterial = rawSolidMaterial.createInstance().apply {
            initInstance(this, COLOR_INFECTED_ROOF, COLOR_INFECTED_ROOF)
        }
        anomalyMaterial = rawFadeMaterial.createInstance().apply {
            initInstance(this, COLOR_ANOMALY, COLOR_ANOMALY)
            setParameter("opacity", 1.0f)
        }
    }

    private fun initInstance(mat: MaterialInstance, baseColor: FloatArray, infectedColor: FloatArray) {
        mat.setParameter("baseColor", baseColor[0], baseColor[1], baseColor[2], baseColor[3])
        mat.setParameter("infectedColor", infectedColor[0], infectedColor[1], infectedColor[2], infectedColor[3])
        mat.setParameter("anomaly0", 0f, 0f, 0f, 1f)
        mat.setParameter("anomaly1", 0f, 0f, 0f, 1f)
        mat.setParameter("anomaly2", 0f, 0f, 0f, 1f)
        mat.setParameter("anomaly3", 0f, 0f, 0f, 1f)
        mat.setParameter("anomaly4", 0f, 0f, 0f, 1f)
        mat.setParameter("anomaly5", 0f, 0f, 0f, 1f)
        mat.setParameter("anomaly6", 0f, 0f, 0f, 1f)
        mat.setParameter("anomaly7", 0f, 0f, 0f, 1f)
    }

    private fun buildParameterizedSolidMaterial(): Material {
        val shaderCode = """
            float evalEmitter(vec2 wpXz, vec4 emitter) {
                if (emitter.z <= 0.0) return 0.0;
                float d = length(wpXz - emitter.xy);
                float r = emitter.z;
                float w = max(1.0, emitter.w);
                return clamp((r - d) / w, 0.0, 1.0);
            }

            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                vec3 wp = getWorldPosition() + getWorldCameraPosition();
                float infection = 0.0;

                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly0));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly1));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly2));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly3));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly4));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly5));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly6));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly7));

                material.baseColor = mix(materialParams.baseColor, materialParams.infectedColor, infection);
            }
        """.trimIndent()

        val builder = MaterialBuilder()
            .name("SolidParameterizedMat")
            .platform(MaterialBuilder.Platform.MOBILE)
            .shading(MaterialBuilder.Shading.UNLIT)
            .blending(MaterialBuilder.BlendingMode.OPAQUE)
            .culling(MaterialBuilder.CullingMode.NONE)
            .doubleSided(true)
            .depthWrite(true)
            .depthCulling(true)
            .require(MaterialBuilder.VertexAttribute.POSITION)
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "baseColor")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "infectedColor")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly0")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly1")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly2")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly3")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly4")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly5")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly6")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly7")
            .material(shaderCode)

        val pkg: MaterialPackage = builder.build(engine)
        if (!pkg.isValid) {
            val err = "FILAMENT SHADER COMPILE FAILED for SolidParameterizedMat"
            Log.e(TAG, err)
            error(err)
        }
        val buffer: ByteBuffer = pkg.buffer
        return Material.Builder()
            .payload(buffer, buffer.remaining())
            .build(engine)
    }

    private fun buildParameterizedFadeMaterial(): Material {
        val shaderCode = """
            float evalEmitter(vec2 wpXz, vec4 emitter) {
                if (emitter.z <= 0.0) return 0.0;
                float d = length(wpXz - emitter.xy);
                float r = emitter.z;
                float w = max(1.0, emitter.w);
                return clamp((r - d) / w, 0.0, 1.0);
            }

            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                vec3 wp = getWorldPosition() + getWorldCameraPosition();
                float infection = 0.0;

                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly0));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly1));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly2));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly3));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly4));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly5));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly6));
                infection = max(infection, evalEmitter(wp.xz, materialParams.anomaly7));

                vec4 c = mix(materialParams.baseColor, materialParams.infectedColor, infection);
                float a = clamp(materialParams.opacity, 0.0, 1.0);
                material.baseColor = vec4(c.rgb * c.a * a, c.a * a);
            }
        """.trimIndent()

        val builder = MaterialBuilder()
            .name("FadeParameterizedMat")
            .platform(MaterialBuilder.Platform.MOBILE)
            .shading(MaterialBuilder.Shading.UNLIT)
            .blending(MaterialBuilder.BlendingMode.TRANSPARENT)
            .culling(MaterialBuilder.CullingMode.NONE)
            .doubleSided(true)
            .depthWrite(false)
            .depthCulling(true)
            .require(MaterialBuilder.VertexAttribute.POSITION)
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "baseColor")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "infectedColor")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT, "opacity")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly0")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly1")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly2")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly3")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly4")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly5")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly6")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "anomaly7")
            .material(shaderCode)

        val pkg: MaterialPackage = builder.build(engine)
        if (!pkg.isValid) {
            val err = "FILAMENT SHADER COMPILE FAILED for FadeParameterizedMat"
            Log.e(TAG, err)
            error(err)
        }
        val buffer: ByteBuffer = pkg.buffer
        return Material.Builder()
            .payload(buffer, buffer.remaining())
            .build(engine)
    }

    private fun buildStaticGroundMaterial(): Material {
        val shaderCode = """
            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                vec2 uv = clamp(getUV0(), 0.0, 1.0);
                float barIdx = clamp(floor(uv.x * 7.0), 0.0, 6.0);
                vec3 barColor = vec3(0.05, 0.05, 0.80); // Blue
                if (barIdx < 0.5) barColor = vec3(0.80, 0.80, 0.80);      // White/Gray
                else if (barIdx < 1.5) barColor = vec3(0.80, 0.80, 0.05); // Yellow
                else if (barIdx < 2.5) barColor = vec3(0.05, 0.80, 0.80); // Cyan
                else if (barIdx < 3.5) barColor = vec3(0.05, 0.80, 0.05); // Green
                else if (barIdx < 4.5) barColor = vec3(0.80, 0.05, 0.80); // Magenta
                else if (barIdx < 5.5) barColor = vec3(0.80, 0.05, 0.05); // Red

                float pTime = materialParams.time;
                vec2 pSeed = fract(uv * 250.0 + fract(pTime * 29.53) * 43.17);
                float snow = fract(sin(dot(pSeed, vec2(12.9898, 78.233))) * 43758.5453);

                // Minimal static noise grain over clear, vibrant SMPTE test bars
                vec3 staticRgb = clamp(barColor * (0.95 + 0.10 * snow) + vec3((snow - 0.5) * 0.03), 0.0, 1.0);
                float a = clamp(materialParams.opacity, 0.0, 1.0);
                material.baseColor = vec4(staticRgb * a, a);
            }
        """.trimIndent()

        val builder = MaterialBuilder()
            .name("StaticGroundMat")
            .platform(MaterialBuilder.Platform.MOBILE)
            .shading(MaterialBuilder.Shading.UNLIT)
            .blending(MaterialBuilder.BlendingMode.TRANSPARENT)
            .culling(MaterialBuilder.CullingMode.NONE)
            .doubleSided(true)
            .depthWrite(false)
            .depthCulling(true)
            .require(MaterialBuilder.VertexAttribute.POSITION)
            .require(MaterialBuilder.VertexAttribute.UV0)
            .uniformParameter(MaterialBuilder.UniformType.FLOAT, "time")
            .uniformParameter(MaterialBuilder.UniformType.FLOAT, "opacity")
            .material(shaderCode)

        val pkg: MaterialPackage = builder.build(engine)
        if (!pkg.isValid) {
            val err = "FILAMENT SHADER COMPILE FAILED for StaticGroundMat"
            Log.e(TAG, err)
            error(err)
        }
        val buffer: ByteBuffer = pkg.buffer
        return Material.Builder()
            .payload(buffer, buffer.remaining())
            .build(engine)
    }

    fun createFadeWallInstance(): MaterialInstance = rawFadeMaterial.createInstance().apply {
        initInstance(this, COLOR_WALL, COLOR_INFECTED_WALL)
        setParameter("opacity", 1.0f)
    }

    fun createFadeRoofInstance(): MaterialInstance = rawFadeMaterial.createInstance().apply {
        initInstance(this, COLOR_ROOF, COLOR_INFECTED_ROOF)
        setParameter("opacity", 1.0f)
    }

    fun createFadeWireframeInstance(): MaterialInstance = rawFadeMaterial.createInstance().apply {
        initInstance(this, COLOR_WIREFRAME, COLOR_INFECTED_WIREFRAME)
        setParameter("opacity", 1.0f)
    }

    fun createFadeRoadInstance(): MaterialInstance = rawFadeMaterial.createInstance().apply {
        initInstance(this, COLOR_ROAD, COLOR_ROAD)
        setParameter("opacity", 1.0f)
    }

    fun createFadeInfectedWallInstance(): MaterialInstance = rawFadeMaterial.createInstance().apply {
        initInstance(this, COLOR_INFECTED_WALL, COLOR_INFECTED_WALL)
        setParameter("opacity", 1.0f)
    }

    fun createFadeInfectedRoofInstance(): MaterialInstance = rawFadeMaterial.createInstance().apply {
        initInstance(this, COLOR_INFECTED_ROOF, COLOR_INFECTED_ROOF)
        setParameter("opacity", 1.0f)
    }

    fun createStaticGroundInstance(): MaterialInstance = rawStaticGroundMaterial.createInstance()

    fun updatePulse(timeSeconds: Float) {
        // Dynamic pulse handled in frame loop via updateAnomalyUniforms
    }

    fun createAnomalyMaterialInstance(): MaterialInstance = rawFadeMaterial.createInstance().apply {
        initInstance(this, COLOR_ANOMALY, COLOR_ANOMALY)
        setParameter("opacity", 1.0f)
    }

    fun calculateAnomalyUniformData(
        anomalies: Collection<PoxAnomaly>,
        anchorLat: Double,
        anchorLng: Double,
        timeMs: Long = WaveMath.getAdjustedTime(),
    ): Array<FloatArray> {
        val cosAnchor = cos(Math.toRadians(anchorLat))
        val metersPerDegLat = 111000.0
        val metersPerDegLng = metersPerDegLat * cosAnchor

        val defaultEmitter = floatArrayOf(0f, 0f, 0f, 1f)
        val result = Array(8) { defaultEmitter }
        if (anomalies.isEmpty()) return result

        val anomalyList = anomalies.toList()
        var slotIdx = 0

        val maxEmittersPerAnomaly = when {
            anomalyList.size == 1 -> 8
            anomalyList.size == 2 -> 4
            else -> maxOf(1, 8 / anomalyList.size)
        }

        for (a in anomalyList) {
            if (slotIdx >= 8) break

            val anomRelX = ((a.lng - anchorLng) * metersPerDegLng).toFloat()
            val anomRelZ = (-(a.lat - anchorLat) * metersPerDegLat).toFloat()

            val rawRadius = if (a.heatZoneDiameter > 0) {
                (a.heatZoneDiameter / 3.28084 / 2.0).toFloat()
            } else {
                1000.0f
            }
            val radiusMeters = rawRadius.coerceIn(2.5f, 4000.0f)
            val baseSummitMeters = a.spectralClass.calculateBaseCeiling(radiusMeters, a.morphology)
            val seed = (a.id.hashCode().toLong() and 0xFFFFFFFFL) xor (a.gene.hashCode().toLong() shl 32)

            val metrics = WaveMath.calculateAnomalyLifecycle(
                seed = seed,
                timeMs = timeMs,
                baseSummitMeters = baseSummitMeters,
            )

            val emitters = MetaballMeshCompiler.generateProceduralEmitters(
                centerX = 0f,
                centerY = 0f,
                totalRadius = radiusMeters,
                seed = seed,
                heightScale = 1.0f,
                radiusScale = 1.0f,
                baseCeilingMeters = baseSummitMeters,
                morphology = a.morphology,
            )

            val selectedEmitters = emitters.take(maxEmittersPerAnomaly)
            for (e in selectedEmitters) {
                if (slotIdx >= 8) break
                val emitterWorldX = anomRelX + e.x * metrics.radiusMultiplier
                val emitterWorldZ = anomRelZ + e.y * metrics.radiusMultiplier
                val footprintRadius = (e.radiusXy * 0.72f * metrics.radiusMultiplier).coerceAtLeast(1.0f)
                val featherWidth = (footprintRadius * 0.20f).coerceIn(4.0f, 15.0f)

                result[slotIdx++] = floatArrayOf(
                    emitterWorldX,
                    emitterWorldZ,
                    footprintRadius,
                    featherWidth,
                )
            }
        }

        return result
    }

    fun applyAnomalyUniforms(mat: MaterialInstance, uniformData: Array<FloatArray>) {
        mat.setParameter("anomaly0", uniformData[0][0], uniformData[0][1], uniformData[0][2], uniformData[0][3])
        mat.setParameter("anomaly1", uniformData[1][0], uniformData[1][1], uniformData[1][2], uniformData[1][3])
        mat.setParameter("anomaly2", uniformData[2][0], uniformData[2][1], uniformData[2][2], uniformData[2][3])
        mat.setParameter("anomaly3", uniformData[3][0], uniformData[3][1], uniformData[3][2], uniformData[3][3])
        mat.setParameter("anomaly4", uniformData[4][0], uniformData[4][1], uniformData[4][2], uniformData[4][3])
        mat.setParameter("anomaly5", uniformData[5][0], uniformData[5][1], uniformData[5][2], uniformData[5][3])
        mat.setParameter("anomaly6", uniformData[6][0], uniformData[6][1], uniformData[6][2], uniformData[6][3])
        mat.setParameter("anomaly7", uniformData[7][0], uniformData[7][1], uniformData[7][2], uniformData[7][3])
    }

    private var lastUniformLogTime = 0L

    fun updateAnomalyUniforms(
        anomalies: Collection<PoxAnomaly>,
        anchorLat: Double,
        anchorLng: Double,
        timeMs: Long = WaveMath.getAdjustedTime(),
    ): Array<FloatArray> {
        val uniformData = calculateAnomalyUniformData(anomalies, anchorLat, anchorLng, timeMs)
        val now = System.currentTimeMillis()
        if (now - lastUniformLogTime > 2000L) {
            lastUniformLogTime = now
            Log.i(TAG, "updateAnomalyUniforms: anoms=${anomalies.size}, anchor=($anchorLat, $anchorLng), " +
                "u0=[${uniformData[0].joinToString()}], u1=[${uniformData[1].joinToString()}], " +
                "u2=[${uniformData[2].joinToString()}], u3=[${uniformData[3].joinToString()}]")
        }
        applyAnomalyUniforms(wallMaterial, uniformData)
        applyAnomalyUniforms(roofMaterial, uniformData)
        applyAnomalyUniforms(wireframeMaterial, uniformData)
        return uniformData
    }

    fun destroy() {
        engine.destroyMaterialInstance(wallMaterial)
        engine.destroyMaterialInstance(roofMaterial)
        engine.destroyMaterialInstance(wireframeMaterial)
        engine.destroyMaterialInstance(roadMaterial)
        engine.destroyMaterialInstance(infectedWallMaterial)
        engine.destroyMaterialInstance(infectedRoofMaterial)
        engine.destroyMaterialInstance(anomalyMaterial)

        engine.destroyMaterial(rawSolidMaterial)
        engine.destroyMaterial(rawFadeMaterial)
        engine.destroyMaterial(rawStaticGroundMaterial)

        MaterialBuilder.shutdown()
    }
}

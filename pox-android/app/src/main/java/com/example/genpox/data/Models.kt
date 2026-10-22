package com.example.genpox.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@JvmInline
value class DnaSequence(val value: String) {
    init {
        require(value.all { it in "AGTCXZYW" }) { "Invalid nucleotide in sequence: $value" }
    }
}

@JvmInline
value class GeneBlock(val value: String) {
    init {
        require(value.length == 8) { "Gene block must be exactly 8 characters: $value" }
    }
}

@Entity(tableName = "creatures")
@Serializable
data class Creature(
    @PrimaryKey val id: String,
    val sequence: String,
    val name: String,
    val faction: String,
    val type: String,
    val vitality: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val primaryWeapon: String,
    val lore: String,
    val asciiArt: String,
    val discoveredAt: Long,
    val origin: String,
    val isAutoHacker: Boolean = false,
    val isAutoDefender: Boolean = false,
    val appendedGenes: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val telomeres: Int = 100,
    val isFullCoherence: Boolean = false,
    val coherenceType: String? = null,
    val isMutated: Boolean = false,
    val originalSequence: String? = null,
    val codonAdaptationIndex: Double = 1.0,
    val heatResistance: Double = 0.0,
    val radResistance: Double = 0.0,
    val kineticResistance: Double = 0.0,
)

@Entity(tableName = "gene_sequences")
@Serializable
data class GeneSequence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequence: String, // 8-character gene sequence
    val count: Int = 1,
    val discoveredAt: Long,
    val averageQScore: Double = 30.0,
    val meltingTemp: Double = 0.0,
    val minimumFreeEnergy: Double = 0.0,
    val polymeraseSignature: String = "TAQ",
)

@Entity(tableName = "harvest_missions")
@Serializable
data class HarvestMission(
    @PrimaryKey val id: String,
    val creatureId: String,
    val creatureName: String,
    val creatureFaction: String,
    val lat: Double,
    val lng: Double,
    val startTime: Long,
    val totalDuration: Long,
    val harvestedPayload: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val isReturned: Boolean = false,
    val dispatchDistance: Double = 0.0,
    val stalledDepth: Double = 0.0,
    val originalSequence: String? = null,
    val elapsedSeconds: Long = 0,
    val missionLogs: List<String> = emptyList(),
    val phase: String = "TRAVEL",
    val travelDuration: Long = 0,
    val descentDuration: Long = 0,
    val harvestDuration: Long = 60,
    val ascentDuration: Long = 0,
    val transitBackDuration: Long = 0,
    val currentPhaseElapsed: Long = 0,
)

@Serializable
data class NearbyUser(
    val uid: String,
    val distance: Double,
    val activeTradePending: Boolean = false,
    val creaturesAvailable: List<Creature> = emptyList(),
    val targetSequence: String? = null,
)

@Serializable
data class BuildingStructure(
    val points: List<RoadPoint>,
    val isFallback: Boolean = false,
    val heightMeters: Float = 0f,
)

@Serializable
data class RoadPoint(val lat: Double, val lng: Double)

@Entity(tableName = "reagent_stock")
@Serializable
data class ReagentStock(
    @PrimaryKey val id: String,
    val name: String,
    val count: Int,
    val type: String,
    val description: String,
)

interface GeographicHazard {
    val id: String
    val lat: Double
    val lng: Double
    val hazardType: String
    fun contains(targetLat: Double, targetLng: Double): Boolean
    fun distanceTo(targetLat: Double, targetLng: Double): Double
    fun getHazardPowerAt(targetLat: Double, targetLng: Double, lunarAge: Double): Double
    fun generateHarvestPayload(creature: Creature, waveCoupling: Double, hasHarvestBoost: Boolean): List<String>
}

@Serializable
enum class AnomalyMorphology {
    SPRAWL,
    SPIKE,
}

@Serializable
enum class AnomalySpectralClass(
    val code: String,
    val title: String,
    val sprawlMinRadius: Float,
    val sprawlMaxRadius: Float,
    val sprawlMinCeiling: Float,
    val sprawlMaxCeiling: Float,
    val spikeMinRadius: Float,
    val spikeMaxRadius: Float,
    val spikeMinCeiling: Float,
    val spikeMaxCeiling: Float,
) {
    CLASS_M(
        code = "M",
        title = "Alleyway Conduit",
        sprawlMinRadius = 25f,
        sprawlMaxRadius = 100f,
        sprawlMinCeiling = 3f,
        sprawlMaxCeiling = 4f,
        spikeMinRadius = 10f,
        spikeMaxRadius = 15f,
        spikeMinCeiling = 25f,
        spikeMaxCeiling = 50f,
    ),
    CLASS_K(
        code = "K",
        title = "Block-Scale Bio-Sprawl",
        sprawlMinRadius = 150f,
        sprawlMaxRadius = 300f,
        sprawlMinCeiling = 10f,
        sprawlMaxCeiling = 25f,
        spikeMinRadius = 15f,
        spikeMaxRadius = 30f,
        spikeMinCeiling = 50f,
        spikeMaxCeiling = 75f,
    ),
    CLASS_G(
        code = "G",
        title = "Urban District Ward",
        sprawlMinRadius = 300f,
        sprawlMaxRadius = 500f,
        sprawlMinCeiling = 100f,
        sprawlMaxCeiling = 200f,
        spikeMinRadius = 30f,
        spikeMaxRadius = 50f,
        spikeMinCeiling = 75f,
        spikeMaxCeiling = 150f,
    ),
    CLASS_F(
        code = "F",
        title = "Inter-District Corridor",
        sprawlMinRadius = 500f,
        sprawlMaxRadius = 1000f,
        sprawlMinCeiling = 200f,
        sprawlMaxCeiling = 500f,
        spikeMinRadius = 50f,
        spikeMaxRadius = 100f,
        spikeMinCeiling = 150f,
        spikeMaxCeiling = 300f,
    ),
    CLASS_A(
        code = "A",
        title = "Township Basin Monolith",
        sprawlMinRadius = 1000f,
        sprawlMaxRadius = 1500f,
        sprawlMinCeiling = 500f,
        sprawlMaxCeiling = 1000f,
        spikeMinRadius = 100f,
        spikeMaxRadius = 200f,
        spikeMinCeiling = 300f,
        spikeMaxCeiling = 750f,
    ),
    CLASS_B(
        code = "B",
        title = "Metro Core Megastructure",
        sprawlMinRadius = 1500f,
        sprawlMaxRadius = 2500f,
        sprawlMinCeiling = 800f,
        sprawlMaxCeiling = 1200f,
        spikeMinRadius = 200f,
        spikeMaxRadius = 600f,
        spikeMinCeiling = 1000f,
        spikeMaxCeiling = 1500f,
    ),
    CLASS_O(
        code = "O",
        title = "Metropolitan Biosphere Titan",
        sprawlMinRadius = 2500f,
        sprawlMaxRadius = 4000f,
        sprawlMinCeiling = 1200f,
        sprawlMaxCeiling = 1750f,
        spikeMinRadius = 600f,
        spikeMaxRadius = 1000f,
        spikeMinCeiling = 1500f,
        spikeMaxCeiling = 3000f,
    ),
    ;

    fun calculateBaseCeiling(radiusMeters: Float, morphology: AnomalyMorphology = AnomalyMorphology.SPRAWL): Float {
        val (minR, maxR, minH, maxH) = if (morphology == AnomalyMorphology.SPIKE) {
            listOf(spikeMinRadius, spikeMaxRadius, spikeMinCeiling, spikeMaxCeiling)
        } else {
            listOf(sprawlMinRadius, sprawlMaxRadius, sprawlMinCeiling, sprawlMaxCeiling)
        }
        val t = ((radiusMeters - minR) / (maxR - minR).coerceAtLeast(1f)).coerceIn(0f, 1f)
        return minH + t * (maxH - minH)
    }

    companion object {
        fun fromRadius(radiusMeters: Float, morphology: AnomalyMorphology = AnomalyMorphology.SPRAWL): AnomalySpectralClass = if (morphology == AnomalyMorphology.SPIKE) {
            when {
                radiusMeters <= 15f -> CLASS_M
                radiusMeters <= 30f -> CLASS_K
                radiusMeters <= 50f -> CLASS_G
                radiusMeters <= 100f -> CLASS_F
                radiusMeters <= 200f -> CLASS_A
                radiusMeters <= 600f -> CLASS_B
                else -> CLASS_O
            }
        } else {
            when {
                radiusMeters <= 125f -> CLASS_M
                radiusMeters <= 300f -> CLASS_K
                radiusMeters <= 500f -> CLASS_G
                radiusMeters <= 1000f -> CLASS_F
                radiusMeters <= 1500f -> CLASS_A
                radiusMeters <= 2500f -> CLASS_B
                else -> CLASS_O
            }
        }
    }
}

@Serializable
enum class AnomalyLifecyclePhase {
    INCEPTION,
    ACTIVE_NEXUS,
    DESTABILIZING,
    VOID_COLLAPSE,
}

@Serializable
data class PoxAnomaly(
    override val id: String,
    val name: String,
    override val lat: Double,
    override val lng: Double,
    val gene: String,
    val faction: String = "Anomaly",
    val distance: Double,
    val heatZoneDiameter: Double,
    val density: Double = 0.0,
    val morphology: AnomalyMorphology = AnomalyMorphology.SPRAWL,
    val lifecyclePhase: AnomalyLifecyclePhase = AnomalyLifecyclePhase.ACTIVE_NEXUS,
    val lifecycleProgress: Float = 0.5f,
    val timeRemainingMs: Long = 0L,
    val peakAltitudeMeters: Float = 850f,
    val mutationFluxMod: Float = 1.0f,
    val harvestYieldMod: Float = 1.0f,
    val spectralClass: AnomalySpectralClass = AnomalySpectralClass.fromRadius((heatZoneDiameter / 3.28084 / 2.0).toFloat(), morphology),
) : GeographicHazard {
    override val hazardType: String
        get() = "RADIATION"

    override fun contains(targetLat: Double, targetLng: Double): Boolean {
        val distFeet = distanceTo(targetLat, targetLng)
        val boundaryFeet = getBoundaryRadiusForPlayer(targetLat, targetLng)
        return distFeet <= boundaryFeet
    }

    override fun distanceTo(targetLat: Double, targetLng: Double): Double = DeploymentEngine.calculateDistanceInFeet(lat, lng, targetLat, targetLng)

    override fun getHazardPowerAt(targetLat: Double, targetLng: Double, lunarAge: Double): Double = maxOf(0.1, density)

    override fun generateHarvestPayload(creature: Creature, waveCoupling: Double, hasHarvestBoost: Boolean): List<String> {
        val payload = mutableListOf<String>()
        payload.add(gene)
        if (hasHarvestBoost && java.util.Random().nextDouble() < 0.40) {
            payload.add(gene)
        }
        if (harvestYieldMod > 1.0f && java.util.Random().nextDouble() < (harvestYieldMod - 1.0f).coerceAtMost(1.0f)) {
            payload.add(gene)
        }
        return payload
    }

    fun getBoundaryRadius(theta: Double): Double = DeploymentEngine.calculateBoundaryRadius(id, heatZoneDiameter, theta)

    fun getBoundaryRadiusForPlayer(userLat: Double, userLng: Double): Double = DeploymentEngine.calculateBoundaryRadiusForCoordinates(id, heatZoneDiameter, lat, lng, userLat, userLng)
}

@Serializable
data class CelestialWell(
    override val id: String,
    val name: String,
    override val lat: Double,
    override val lng: Double,
    val radiusFeet: Double,
    val type: String, // "GREENHOUSE", "MAGNETOSPHERIC", "GRAVITATIONAL"
    val hazardPower: Double, // base intensity multiplier
) : GeographicHazard {
    override val hazardType: String
        get() = when (type.uppercase()) {
            "GREENHOUSE" -> "HEAT"
            "MAGNETOSPHERIC" -> "RADIATION"
            else -> "KINETIC"
        }

    override fun contains(targetLat: Double, targetLng: Double): Boolean = distanceTo(targetLat, targetLng) <= radiusFeet

    override fun distanceTo(targetLat: Double, targetLng: Double): Double = DeploymentEngine.calculateDistanceInFeet(lat, lng, targetLat, targetLng)

    override fun getHazardPowerAt(targetLat: Double, targetLng: Double, lunarAge: Double): Double = hazardPower

    override fun generateHarvestPayload(creature: Creature, waveCoupling: Double, hasHarvestBoost: Boolean): List<String> {
        val payload = mutableListOf<String>()
        val reward = when (type.uppercase()) {
            "GREENHOUSE" -> "STOCK_A"
            "MAGNETOSPHERIC" -> "STOCK_G"
            "GRAVITATIONAL" -> "STOCK_T"
            else -> "STOCK_C"
        }
        payload.add(reward)
        if (hasHarvestBoost && java.util.Random().nextDouble() < 0.40) {
            payload.add(reward)
        }
        return payload
    }
}




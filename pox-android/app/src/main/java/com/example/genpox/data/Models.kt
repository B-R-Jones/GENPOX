package com.example.genpox.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

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
    val kineticResistance: Double = 0.0
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
    val polymeraseSignature: String = "TAQ"
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
    val currentPhaseElapsed: Long = 0
)

@Serializable
data class NearbyUser(
    val uid: String,
    val distance: Double,
    val activeTradePending: Boolean = false,
    val creaturesAvailable: List<Creature> = emptyList(),
    val targetSequence: String? = null
)

@Entity(tableName = "cached_road_cells")
@Serializable
data class CachedRoadCell(
    @PrimaryKey val cellKey: String, // "cellX,cellY"
    val roadsJson: String,           // JSON representation of List<List<RoadPoint>>
    val fetchedAt: Long              // Timestamp in ms
)

@Entity(tableName = "cached_building_cells")
@Serializable
data class CachedBuildingCell(
    @PrimaryKey val cellKey: String, // "cellX,cellY"
    val buildingsJson: String,       // JSON representation of List<BuildingStructure>
    val fetchedAt: Long              // Timestamp in ms
)

@Serializable
data class BuildingStructure(
    val points: List<RoadPoint>,
    val isFallback: Boolean = false
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
    val description: String
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
data class PoxAnomaly(
    override val id: String,
    val name: String,
    override val lat: Double,
    override val lng: Double,
    val gene: String,
    val faction: String,
    val distance: Double,
    val heatZoneDiameter: Double,
    val density: Double = 0.0
) : GeographicHazard {
    override val hazardType: String
        get() = if (faction == "Mech" || faction == "Containment") "KINETIC" else "RADIATION"

    override fun contains(targetLat: Double, targetLng: Double): Boolean {
        val distFeet = distanceTo(targetLat, targetLng)
        val boundaryFeet = getBoundaryRadiusForPlayer(targetLat, targetLng)
        return distFeet <= boundaryFeet
    }

    override fun distanceTo(targetLat: Double, targetLng: Double): Double {
        return DeploymentEngine.calculateDistanceInFeet(lat, lng, targetLat, targetLng)
    }

    override fun getHazardPowerAt(targetLat: Double, targetLng: Double, lunarAge: Double): Double {
        return maxOf(0.1, density)
    }

    override fun generateHarvestPayload(creature: Creature, waveCoupling: Double, hasHarvestBoost: Boolean): List<String> {
        val payload = mutableListOf<String>()
        payload.add(gene)
        if (hasHarvestBoost && java.util.Random().nextDouble() < 0.40) {
            payload.add(gene)
        }
        return payload
    }

    fun getBoundaryRadius(theta: Double): Double {
        return DeploymentEngine.calculateBoundaryRadius(id, heatZoneDiameter, theta)
    }

    fun getBoundaryRadiusForPlayer(userLat: Double, userLng: Double): Double {
        return DeploymentEngine.calculateBoundaryRadiusForCoordinates(id, heatZoneDiameter, lat, lng, userLat, userLng)
    }
}

@Serializable
data class CelestialWell(
    override val id: String,
    val name: String,
    override val lat: Double,
    override val lng: Double,
    val radiusFeet: Double,
    val type: String, // "GREENHOUSE", "MAGNETOSPHERIC", "GRAVITATIONAL"
    val hazardPower: Double // base intensity multiplier
) : GeographicHazard {
    override val hazardType: String
        get() = when (type.uppercase()) {
            "GREENHOUSE" -> "HEAT"
            "MAGNETOSPHERIC" -> "RADIATION"
            else -> "KINETIC"
        }

    override fun contains(targetLat: Double, targetLng: Double): Boolean {
        return distanceTo(targetLat, targetLng) <= radiusFeet
    }

    override fun distanceTo(targetLat: Double, targetLng: Double): Double {
        return DeploymentEngine.calculateDistanceInFeet(lat, lng, targetLat, targetLng)
    }

    override fun getHazardPowerAt(targetLat: Double, targetLng: Double, lunarAge: Double): Double {
        return hazardPower
    }

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


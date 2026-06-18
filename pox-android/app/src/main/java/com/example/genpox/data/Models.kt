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
    val appendedGenes: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val telomeres: Int = 100,
    val isFullCoherence: Boolean = false,
    val coherenceType: String? = null,
    val isMutated: Boolean = false,
    val originalSequence: String? = null
)

@Entity(tableName = "gene_sequences")
@Serializable
data class GeneSequence(
    @PrimaryKey val sequence: String, // 8-character gene sequence
    val count: Int,
    val discoveredAt: Long
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
    val harvestedGenes: List<String> = emptyList(),
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

@Serializable
data class RoadPoint(val lat: Double, val lng: Double)

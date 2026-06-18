package com.example.genpox.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Dao
interface PoxDao {
    // Creatures
    @Query("SELECT * FROM creatures ORDER BY discoveredAt DESC")
    fun getAllCreatures(): Flow<List<Creature>>

    @Query("SELECT * FROM creatures WHERE id = :id LIMIT 1")
    suspend fun getCreatureById(id: String): Creature?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreature(creature: Creature)

    @Update
    suspend fun updateCreature(creature: Creature)

    @Delete
    suspend fun deleteCreature(creature: Creature)

    // Gene Sequences
    @Query("SELECT * FROM gene_sequences ORDER BY discoveredAt DESC")
    fun getAllGeneSequences(): Flow<List<GeneSequence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneSequence(sequence: GeneSequence)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneSequences(sequences: List<GeneSequence>)

    @Update
    suspend fun updateGeneSequence(sequence: GeneSequence)

    @Delete
    suspend fun deleteGeneSequence(sequence: GeneSequence)

    @Transaction
    suspend fun updateGeneStock(toInsertOrUpdate: List<GeneSequence>, toDelete: List<GeneSequence>) {
        toDelete.forEach { deleteGeneSequence(it) }
        insertGeneSequences(toInsertOrUpdate)
    }

    // Harvest Missions
    @Query("SELECT * FROM harvest_missions WHERE isReturned = 0 ORDER BY startTime ASC")
    fun getActiveMissions(): Flow<List<HarvestMission>>

    @Query("SELECT * FROM harvest_missions")
    fun getAllMissions(): Flow<List<HarvestMission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: HarvestMission)

    @Update
    suspend fun updateMission(mission: HarvestMission)

    // Cached Road Cells
    @Query("SELECT * FROM cached_road_cells WHERE cellKey = :cellKey LIMIT 1")
    suspend fun getCachedRoadCell(cellKey: String): CachedRoadCell?

    @Query("SELECT * FROM cached_road_cells")
    suspend fun getAllCachedRoadCells(): List<CachedRoadCell>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedRoadCell(cell: CachedRoadCell)
}

@Database(entities = [Creature::class, GeneSequence::class, HarvestMission::class, CachedRoadCell::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PoxDatabase : RoomDatabase() {
    abstract fun poxDao(): PoxDao
}

package com.example.genpox.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
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

    @Query("DELETE FROM creatures")
    suspend fun deleteAllCreatures()

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

    @Query("DELETE FROM gene_sequences")
    suspend fun deleteAllGeneSequences()

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

    // Reagent Stockpile
    @Query("SELECT * FROM reagent_stock ORDER BY name ASC")
    fun getAllReagentStock(): Flow<List<ReagentStock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReagentStock(reagents: List<ReagentStock>)

    @Query("UPDATE reagent_stock SET count = max(0, count - 1) WHERE id = :id")
    suspend fun decrementReagentStock(id: String)
}

@Database(entities = [Creature::class, GeneSequence::class, HarvestMission::class, ReagentStock::class], version = 21, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PoxDatabase : RoomDatabase() {
    abstract fun poxDao(): PoxDao
}

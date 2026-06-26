package com.example.genpox.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    // Database access
    val allCreatures: Flow<List<Creature>>
    suspend fun getCreatureById(id: String): Creature?
    suspend fun insertCreature(creature: Creature)
    suspend fun updateCreature(creature: Creature)
    suspend fun deleteCreature(creature: Creature)
    suspend fun deleteAllCreatures()

    val allGeneSequences: Flow<List<GeneSequence>>
    suspend fun insertGeneSequence(sequence: GeneSequence)
    suspend fun insertGeneSequences(sequences: List<GeneSequence>)
    suspend fun updateGeneSequence(sequence: GeneSequence)
    suspend fun deleteGeneSequence(sequence: GeneSequence)
    suspend fun deleteAllGeneSequences()
    suspend fun updateGeneStock(toInsertOrUpdate: List<GeneSequence>, toDelete: List<GeneSequence>)

    val activeMissions: Flow<List<HarvestMission>>
    val allMissions: Flow<List<HarvestMission>>
    suspend fun insertMission(mission: HarvestMission)
    suspend fun updateMission(mission: HarvestMission)

    // Cached road cells
    suspend fun getCachedRoadCell(cellKey: String): CachedRoadCell?
    suspend fun getAllCachedRoadCells(): List<CachedRoadCell>
    suspend fun insertCachedRoadCell(cell: CachedRoadCell)

    // Cached building cells
    suspend fun getCachedBuildingCell(cellKey: String): CachedBuildingCell?
    suspend fun getAllCachedBuildingCells(): List<CachedBuildingCell>
    suspend fun insertCachedBuildingCell(cell: CachedBuildingCell)

    suspend fun clearMapCache()

    // Settings access
    val geminiApiKey: Flow<String>
    val muteSound: Flow<Boolean>
    val scanRadius: Flow<Float>
    val targetSequence: Flow<String>
    val rawStockA: Flow<Long>
    val rawStockG: Flow<Long>
    val rawStockT: Flow<Long>
    val rawStockC: Flow<Long>
    suspend fun saveGeminiApiKey(apiKey: String)
    suspend fun setMuteSound(mute: Boolean)
    suspend fun setScanRadius(radius: Float)
    suspend fun saveTargetSequence(seq: String)
    suspend fun saveRawStocks(a: Long, g: Long, t: Long, c: Long)
}

class DefaultDataRepository(private val context: Context) : DataRepository {
    private val database: PoxDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            PoxDatabase::class.java,
            "pox_database"
        ).fallbackToDestructiveMigration().build()
    }

    private val poxDao: PoxDao by lazy { database.poxDao() }
    private val settings: PoxSettings by lazy { PoxSettings(context) }

    // Database access overrides
    override val allCreatures: Flow<List<Creature>>
        get() = poxDao.getAllCreatures()

    override suspend fun getCreatureById(id: String): Creature? = poxDao.getCreatureById(id)

    override suspend fun insertCreature(creature: Creature) = poxDao.insertCreature(creature)

    override suspend fun updateCreature(creature: Creature) = poxDao.updateCreature(creature)

    override suspend fun deleteCreature(creature: Creature) = poxDao.deleteCreature(creature)
    override suspend fun deleteAllCreatures() = poxDao.deleteAllCreatures()

    override val allGeneSequences: Flow<List<GeneSequence>>
        get() = poxDao.getAllGeneSequences()

    override suspend fun insertGeneSequence(sequence: GeneSequence) = poxDao.insertGeneSequence(sequence)

    override suspend fun insertGeneSequences(sequences: List<GeneSequence>) = poxDao.insertGeneSequences(sequences)

    override suspend fun updateGeneSequence(sequence: GeneSequence) = poxDao.updateGeneSequence(sequence)

    override suspend fun deleteGeneSequence(sequence: GeneSequence) = poxDao.deleteGeneSequence(sequence)
    override suspend fun deleteAllGeneSequences() = poxDao.deleteAllGeneSequences()

    override suspend fun updateGeneStock(toInsertOrUpdate: List<GeneSequence>, toDelete: List<GeneSequence>) =
        poxDao.updateGeneStock(toInsertOrUpdate, toDelete)

    override val activeMissions: Flow<List<HarvestMission>>
        get() = poxDao.getActiveMissions()

    override val allMissions: Flow<List<HarvestMission>>
        get() = poxDao.getAllMissions()

    override suspend fun insertMission(mission: HarvestMission) = poxDao.insertMission(mission)

    override suspend fun updateMission(mission: HarvestMission) = poxDao.updateMission(mission)

    override suspend fun getCachedRoadCell(cellKey: String): CachedRoadCell? = poxDao.getCachedRoadCell(cellKey)

    override suspend fun getAllCachedRoadCells(): List<CachedRoadCell> = poxDao.getAllCachedRoadCells()

    override suspend fun insertCachedRoadCell(cell: CachedRoadCell) = poxDao.insertCachedRoadCell(cell)

    override suspend fun getCachedBuildingCell(cellKey: String): CachedBuildingCell? = poxDao.getCachedBuildingCell(cellKey)

    override suspend fun getAllCachedBuildingCells(): List<CachedBuildingCell> = poxDao.getAllCachedBuildingCells()

    override suspend fun insertCachedBuildingCell(cell: CachedBuildingCell) = poxDao.insertCachedBuildingCell(cell)

    override suspend fun clearMapCache() {
        poxDao.clearCachedRoadCells()
        poxDao.clearCachedBuildingCells()
    }

    // Settings access overrides
    override val geminiApiKey: Flow<String>
        get() = settings.geminiApiKey

    override val muteSound: Flow<Boolean>
        get() = settings.muteSound

    override val scanRadius: Flow<Float>
        get() = settings.scanRadius

    override val targetSequence: Flow<String>
        get() = settings.targetSequence

    override val rawStockA: Flow<Long>
        get() = settings.rawStockA

    override val rawStockG: Flow<Long>
        get() = settings.rawStockG

    override val rawStockT: Flow<Long>
        get() = settings.rawStockT

    override val rawStockC: Flow<Long>
        get() = settings.rawStockC

    override suspend fun saveGeminiApiKey(apiKey: String) = settings.saveGeminiApiKey(apiKey)

    override suspend fun setMuteSound(mute: Boolean) = settings.setMuteSound(mute)

    override suspend fun setScanRadius(radius: Float) = settings.setScanRadius(radius)

    override suspend fun saveTargetSequence(seq: String) = settings.saveTargetSequence(seq)

    override suspend fun saveRawStocks(a: Long, g: Long, t: Long, c: Long) =
        settings.saveRawStocks(a, g, t, c)
}

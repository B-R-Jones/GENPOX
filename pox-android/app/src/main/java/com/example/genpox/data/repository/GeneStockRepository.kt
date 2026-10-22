package com.example.genpox.data.repository

import com.example.genpox.data.GeneSequence
import com.example.genpox.data.PoxDao
import com.example.genpox.data.ReagentStock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface GeneStockRepository {
    val allGeneSequences: Flow<List<GeneSequence>>
    suspend fun insertGeneSequence(sequence: GeneSequence)
    suspend fun insertGeneSequences(sequences: List<GeneSequence>)
    suspend fun updateGeneSequence(sequence: GeneSequence)
    suspend fun deleteGeneSequence(sequence: GeneSequence)
    suspend fun deleteAllGeneSequences()
    suspend fun updateGeneStock(toInsertOrUpdate: List<GeneSequence>, toDelete: List<GeneSequence>)

    val allReagentStock: Flow<List<ReagentStock>>
    suspend fun insertReagentStock(reagents: List<ReagentStock>)
    suspend fun decrementReagentStock(id: String)
}

@Singleton
class DefaultGeneStockRepository @Inject constructor(
    private val poxDao: PoxDao,
) : GeneStockRepository {
    override val allGeneSequences: Flow<List<GeneSequence>>
        get() = poxDao.getAllGeneSequences()

    override suspend fun insertGeneSequence(sequence: GeneSequence) = poxDao.insertGeneSequence(sequence)

    override suspend fun insertGeneSequences(sequences: List<GeneSequence>) = poxDao.insertGeneSequences(sequences)

    override suspend fun updateGeneSequence(sequence: GeneSequence) = poxDao.updateGeneSequence(sequence)

    override suspend fun deleteGeneSequence(sequence: GeneSequence) = poxDao.deleteGeneSequence(sequence)

    override suspend fun deleteAllGeneSequences() = poxDao.deleteAllGeneSequences()

    override suspend fun updateGeneStock(toInsertOrUpdate: List<GeneSequence>, toDelete: List<GeneSequence>) = poxDao.updateGeneStock(toInsertOrUpdate, toDelete)

    override val allReagentStock: Flow<List<ReagentStock>>
        get() = poxDao.getAllReagentStock()

    override suspend fun insertReagentStock(reagents: List<ReagentStock>) = poxDao.insertReagentStock(reagents)

    override suspend fun decrementReagentStock(id: String) = poxDao.decrementReagentStock(id)
}

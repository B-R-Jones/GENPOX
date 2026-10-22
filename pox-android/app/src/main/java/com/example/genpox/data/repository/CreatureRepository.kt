package com.example.genpox.data.repository

import com.example.genpox.data.Creature
import com.example.genpox.data.PoxDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface CreatureRepository {
    val allCreatures: Flow<List<Creature>>
    suspend fun getCreatureById(id: String): Creature?
    suspend fun insertCreature(creature: Creature)
    suspend fun updateCreature(creature: Creature)
    suspend fun deleteCreature(creature: Creature)
    suspend fun deleteAllCreatures()
}

@Singleton
class DefaultCreatureRepository @Inject constructor(
    private val poxDao: PoxDao,
) : CreatureRepository {
    override val allCreatures: Flow<List<Creature>>
        get() = poxDao.getAllCreatures()

    override suspend fun getCreatureById(id: String): Creature? = poxDao.getCreatureById(id)

    override suspend fun insertCreature(creature: Creature) = poxDao.insertCreature(creature)

    override suspend fun updateCreature(creature: Creature) = poxDao.updateCreature(creature)

    override suspend fun deleteCreature(creature: Creature) = poxDao.deleteCreature(creature)

    override suspend fun deleteAllCreatures() = poxDao.deleteAllCreatures()
}

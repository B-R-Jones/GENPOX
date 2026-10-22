package com.example.genpox.data.repository

import com.example.genpox.data.HarvestMission
import com.example.genpox.data.PoxDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface MissionRepository {
    val activeMissions: Flow<List<HarvestMission>>
    val allMissions: Flow<List<HarvestMission>>
    suspend fun insertMission(mission: HarvestMission)
    suspend fun updateMission(mission: HarvestMission)
}

@Singleton
class DefaultMissionRepository @Inject constructor(
    private val poxDao: PoxDao,
) : MissionRepository {
    override val activeMissions: Flow<List<HarvestMission>>
        get() = poxDao.getActiveMissions()

    override val allMissions: Flow<List<HarvestMission>>
        get() = poxDao.getAllMissions()

    override suspend fun insertMission(mission: HarvestMission) = poxDao.insertMission(mission)

    override suspend fun updateMission(mission: HarvestMission) = poxDao.updateMission(mission)
}

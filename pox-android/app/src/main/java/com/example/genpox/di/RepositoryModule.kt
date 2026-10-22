package com.example.genpox.di

import com.example.genpox.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCreatureRepository(
        impl: DefaultCreatureRepository,
    ): CreatureRepository

    @Binds
    @Singleton
    abstract fun bindGeneStockRepository(
        impl: DefaultGeneStockRepository,
    ): GeneStockRepository

    @Binds
    @Singleton
    abstract fun bindMissionRepository(
        impl: DefaultMissionRepository,
    ): MissionRepository


    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: DefaultUserPreferencesRepository,
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindTileDataProvider(
        impl: com.example.genpox.map.tile.VectorTileRepository,
    ): com.example.genpox.map.tile.TileDataProvider
}

package com.example.genpox.di

import android.content.Context
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.location.DefaultLocationDataSource
import com.example.genpox.data.location.DefaultLocationRepository
import com.example.genpox.data.location.LocationDataSource
import com.example.genpox.data.location.LocationRepository
import com.example.genpox.util.DefaultDispatchersProvider
import com.example.genpox.util.DispatchersProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleBinds {

    @Binds
    @Singleton
    abstract fun bindDispatchersProvider(
        impl: DefaultDispatchersProvider,
    ): DispatchersProvider

    @Binds
    @Singleton
    abstract fun bindLocationDataSource(
        impl: DefaultLocationDataSource,
    ): LocationDataSource

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: DefaultLocationRepository,
    ): LocationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDefaultDispatchersProvider(): DefaultDispatchersProvider = DefaultDispatchersProvider()

    @Provides
    @Singleton
    fun providePoxSynthManager(@ApplicationContext context: Context): PoxSynthManager = PoxSynthManager().apply { initialize(context) }
}

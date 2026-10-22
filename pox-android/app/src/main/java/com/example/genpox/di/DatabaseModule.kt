package com.example.genpox.di

import android.content.Context
import androidx.room.Room
import com.example.genpox.data.DataRepository
import com.example.genpox.data.DefaultDataRepository
import com.example.genpox.data.PoxDao
import com.example.genpox.data.PoxDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModuleBinds {

    @Binds
    @Singleton
    abstract fun bindDataRepository(
        impl: DefaultDataRepository,
    ): DataRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePoxDatabase(@ApplicationContext context: Context): PoxDatabase = Room.databaseBuilder(
        context,
        PoxDatabase::class.java,
        "pox_database",
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun providePoxDao(database: PoxDatabase): PoxDao = database.poxDao()
}

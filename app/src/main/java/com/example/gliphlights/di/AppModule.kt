package com.example.gliphlights.di

import android.content.Context
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.GlyphRepositoryImpl
import com.example.gliphlights.repository.SettingsRepository
import com.example.gliphlights.repository.SettingsRepositoryImpl
import com.example.gliphlights.sdk.GlyphManagerWrapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGlyphManagerWrapper(
        @ApplicationContext context: Context
    ): GlyphManagerWrapper {
        return GlyphManagerWrapper(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGlyphRepository(
        impl: GlyphRepositoryImpl
    ): GlyphRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}

package com.example.gliphlights.di

import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.PresetRepository
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlyphEntryPoint {
    fun glyphRepository(): GlyphRepository
    fun glyphSessionArbiter(): GlyphSessionArbiter
    fun presetRepository(): PresetRepository
}

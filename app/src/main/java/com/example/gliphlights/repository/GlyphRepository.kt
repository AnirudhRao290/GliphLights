package com.example.gliphlights.repository

import com.example.gliphlights.models.AnimationParams
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.SdkResult
import kotlinx.coroutines.flow.StateFlow

interface GlyphRepository {
    val glyphState: StateFlow<GlyphState>
    val deviceInfo: StateFlow<DeviceInfo>
    val isConnected: StateFlow<Boolean>
    val isSessionActive: StateFlow<Boolean>

    suspend fun initialize(): SdkResult<Unit>
    suspend fun register(): SdkResult<Unit>
    suspend fun openSession(): SdkResult<Unit>
    suspend fun closeSession(): SdkResult<Unit>
    suspend fun toggleAll(): SdkResult<Unit>
    suspend fun toggleChannels(channels: List<Int>): SdkResult<Unit>
    suspend fun setChannels(channels: List<Int>): SdkResult<Unit>
    suspend fun animateAll(params: AnimationParams = AnimationParams()): SdkResult<Unit>
    suspend fun animateChannels(channels: List<Int>, params: AnimationParams = AnimationParams()): SdkResult<Unit>
    suspend fun displayProgress(progress: Int, reverse: Boolean = false): SdkResult<Unit>
    suspend fun turnOff(): SdkResult<Unit>
    suspend fun turnOffChannels(channels: List<Int>): SdkResult<Unit>
    suspend fun toggleWithBrightness(channels: List<Int>, brightness: Float): SdkResult<Unit>
    suspend fun cleanup()
}

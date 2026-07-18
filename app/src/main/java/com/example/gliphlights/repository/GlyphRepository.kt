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
    /**
     * Turns lights off without clearing the persisted last-state used by
     * [com.example.gliphlights.models.StartupBehavior.SHOW_LAST_STATE].
     */
    suspend fun turnOffPreservingLastState(): SdkResult<Unit>
    suspend fun turnOffChannels(channels: List<Int>): SdkResult<Unit>
    suspend fun toggleWithBrightness(channels: List<Int>, brightness: Float): SdkResult<Unit>
    /**
     * Once per process: if startup behavior is SHOW_LAST_STATE, restore the last
     * active Glyph channels onto hardware. No-op when DO_NOTHING or nothing saved.
     */
    suspend fun applyStartupBehavior()
    suspend fun cleanup()
}

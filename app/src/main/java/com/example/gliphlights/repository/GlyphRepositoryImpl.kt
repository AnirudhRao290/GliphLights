package com.example.gliphlights.repository

import com.example.gliphlights.models.AnimationParams
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.sdk.GlyphManagerWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphRepositoryImpl @Inject constructor(
    private val glyphManager: GlyphManagerWrapper
) : GlyphRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    override val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    override val glyphState: StateFlow<GlyphState> = glyphManager.glyphState
    override val deviceInfo: StateFlow<DeviceInfo> = glyphManager.deviceInfo

    init {
        scope.launch {
            glyphManager.sessionState.collectLatest { state ->
                _isConnected.value = state != GlyphManagerWrapper.SessionState.DISCONNECTED
                _isSessionActive.value = state == GlyphManagerWrapper.SessionState.SESSION_ACTIVE
            }
        }
    }

    override suspend fun initialize(): SdkResult<Unit> = glyphManager.init()

    override suspend fun register(): SdkResult<Unit> = glyphManager.register()

    override suspend fun openSession(): SdkResult<Unit> = glyphManager.openSession()

    override suspend fun closeSession(): SdkResult<Unit> = glyphManager.closeSession()

    override suspend fun toggleAll(): SdkResult<Unit> {
        val allChannels = GlyphZone.A.channels + GlyphZone.B.channels + GlyphZone.C.channels
        return glyphManager.toggleChannels(allChannels)
    }

    override suspend fun toggleChannels(channels: List<Int>): SdkResult<Unit> =
        glyphManager.toggleChannels(channels)

    override suspend fun animateAll(params: AnimationParams): SdkResult<Unit> {
        val allChannels = GlyphZone.A.channels + GlyphZone.B.channels + GlyphZone.C.channels
        return glyphManager.animateChannels(allChannels, params)
    }

    override suspend fun animateChannels(channels: List<Int>, params: AnimationParams): SdkResult<Unit> =
        glyphManager.animateChannels(channels, params)

    override suspend fun displayProgress(progress: Int, reverse: Boolean): SdkResult<Unit> =
        glyphManager.displayProgress(progress, reverse)

    override suspend fun turnOff(): SdkResult<Unit> = glyphManager.turnOff()

    override suspend fun turnOffChannels(channels: List<Int>): SdkResult<Unit> =
        glyphManager.turnOffChannels(channels)

    override suspend fun toggleWithBrightness(channels: List<Int>, brightness: Float): SdkResult<Unit> =
        glyphManager.toggleWithBrightness(channels, brightness)

    override suspend fun cleanup() {
        glyphManager.cleanup()
        scope.cancel()
    }
}

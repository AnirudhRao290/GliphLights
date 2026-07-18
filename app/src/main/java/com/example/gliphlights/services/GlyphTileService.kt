package com.example.gliphlights.services

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class GlyphTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenJob: Job? = null

    @Inject
    lateinit var glyphRepository: GlyphRepository

    @Inject
    lateinit var sessionArbiter: GlyphSessionArbiter

    override fun onStartListening() {
        super.onStartListening()
        listenJob?.cancel()
        listenJob = scope.launch {
            glyphRepository.glyphState.collect { state ->
                applyTileState(state)
            }
        }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val token = UUID.randomUUID().toString()
            when (val acquire = sessionArbiter.acquire(GlyphClient.TILE_WIDGET, token)) {
                is AcquireResult.Denied -> {
                    applyTileState(glyphRepository.glyphState.value)
                    return@launch
                }
                is AcquireResult.Granted -> {
                    try {
                        if (ensureSession()) {
                            glyphRepository.toggleAll()
                        }
                    } finally {
                        sessionArbiter.release(GlyphClient.TILE_WIDGET, acquire.token)
                    }
                }
            }
            applyTileState(glyphRepository.glyphState.value)
        }
    }

    private suspend fun ensureSession(): Boolean {
        if (glyphRepository.isSessionActive.value) return true
        if (!glyphRepository.isConnected.value) {
            if (glyphRepository.initialize() is SdkResult.Error) return false
            if (glyphRepository.register() is SdkResult.Error) return false
        }
        return glyphRepository.openSession() is SdkResult.Success
    }

    private fun applyTileState(state: GlyphState) {
        val tile = qsTile ?: return
        tile.state = if (state.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (state.isActive) "Glyph On" else "Glyph Off"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = if (state.isActive) "Active" else "Inactive"
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        listenJob?.cancel()
        listenJob = null
        super.onDestroy()
        scope.cancel()
    }
}

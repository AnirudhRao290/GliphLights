package com.example.gliphlights.editor.sdk

import android.util.Log
import com.example.gliphlights.editor.model.AnimationModel
import com.example.gliphlights.repository.GlyphRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphSdkRenderer @Inject constructor(
    private val glyphRepository: GlyphRepository
) {
    companion object {
        private const val TAG = "GlyphSdkRenderer"
        private const val MIN_FRAME_INTERVAL_MS = 33L
    }

    private var scope: CoroutineScope? = null
    private var lastCommandTime = 0L
    private var pendingModel: AnimationModel? = null
    private var coalesceJob: Job? = null

    fun start() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    fun stop() {
        scope?.launch {
            glyphRepository.turnOff()
        }
        scope?.cancel()
        scope = null
        pendingModel = null
        coalesceJob?.cancel()
    }

    fun render(model: AnimationModel) {
        val currentScope = scope ?: return
        val now = System.currentTimeMillis()
        val elapsed = now - lastCommandTime

        if (elapsed >= MIN_FRAME_INTERVAL_MS) {
            lastCommandTime = now
            sendCommand(model)
        } else {
            pendingModel = model
            coalesceJob?.cancel()
            coalesceJob = currentScope.launch {
                val delayMs = MIN_FRAME_INTERVAL_MS - elapsed
                kotlinx.coroutines.delay(delayMs)
                val latest = pendingModel ?: return@launch
                pendingModel = null
                lastCommandTime = System.currentTimeMillis()
                sendCommand(latest)
            }
        }
    }

    private fun sendCommand(model: AnimationModel) {
        val activeChannels = model.activeChannels.toList()
        scope?.launch {
            val isActive = glyphRepository.isSessionActive.first()
            if (!isActive) return@launch

            if (activeChannels.isEmpty()) {
                glyphRepository.turnOff()
            } else {
                glyphRepository.toggleChannels(activeChannels)
            }
        }
    }

    fun destroy() {
        stop()
    }
}

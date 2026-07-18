package com.example.gliphlights.physics

import android.util.Log
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.physics.model.PhysicsAnimationModel
import com.example.gliphlights.repository.GlyphRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Mirrors physics AnimationModel onto Glyph hardware. No physics logic here.
 */
class PhysicsHardwareBridge(
    private val glyphRepository: GlyphRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "PhysicsHardware"
        private const val THRESHOLD = 0.12f
    }

    private var job: Job? = null
    private var lastSig = ""

    suspend fun ensureSession(): SdkResult<Unit> {
        if (glyphRepository.isSessionActive.value) return SdkResult.Success(Unit)
        if (!glyphRepository.isConnected.value) {
            val init = glyphRepository.initialize()
            if (init is SdkResult.Error) return init
            val reg = glyphRepository.register()
            if (reg is SdkResult.Error) return reg
        }
        return glyphRepository.openSession()
    }

    fun start(modelFlow: StateFlow<PhysicsAnimationModel>) {
        stop(turnOff = false)
        job = scope.launch {
            modelFlow
                .map { model ->
                    buildList {
                        for (i in model.nodeAlphas.indices) {
                            if (model.nodeAlphas[i] >= THRESHOLD) add(i)
                        }
                    }
                }
                .distinctUntilChanged()
                .collectLatest { channels ->
                    push(channels)
                }
        }
    }

    fun stop(turnOff: Boolean = true) {
        job?.cancel()
        job = null
        lastSig = ""
        if (turnOff) {
            scope.launch {
                if (glyphRepository.isSessionActive.value) glyphRepository.turnOff()
            }
        }
    }

    private suspend fun push(channels: List<Int>) {
        if (!glyphRepository.isSessionActive.value) return
        val sig = channels.joinToString(",")
        if (sig == lastSig) return
        lastSig = sig
        try {
            if (channels.isEmpty()) glyphRepository.turnOff()
            else glyphRepository.setChannels(channels)
        } catch (e: Exception) {
            Log.e(TAG, "push failed", e)
        }
    }
}

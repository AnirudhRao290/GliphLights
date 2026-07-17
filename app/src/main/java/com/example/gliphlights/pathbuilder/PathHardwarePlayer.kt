package com.example.gliphlights.pathbuilder

import android.util.Log
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.pathbuilder.model.EngineSnapshot
import com.example.gliphlights.repository.GlyphRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Mirrors AnimationEngine preview alphas onto physical Glyph lights via setChannels.
 */
class PathHardwarePlayer(
    private val glyphRepository: GlyphRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "PathHardwarePlayer"
        private const val ALPHA_THRESHOLD = 0.18f
    }

    private var syncJob: Job? = null
    private var lastSignature: String = ""

    val isSyncing: Boolean get() = syncJob?.isActive == true

    suspend fun ensureSession(): SdkResult<Unit> {
        if (glyphRepository.isSessionActive.value) {
            return SdkResult.Success(Unit)
        }
        if (!glyphRepository.isConnected.value) {
            val init = glyphRepository.initialize()
            if (init is SdkResult.Error) return init
            val reg = glyphRepository.register()
            if (reg is SdkResult.Error) return reg
        }
        return glyphRepository.openSession()
    }

    fun startSync(snapshotFlow: kotlinx.coroutines.flow.StateFlow<EngineSnapshot>) {
        stopSync(turnOff = false)
        syncJob = scope.launch {
            snapshotFlow
                .map { snap -> channelsFromAlphas(snap.nodeAlphas) }
                .distinctUntilChanged()
                .collectLatest { channels ->
                    pushFrame(channels)
                }
        }
    }

    fun stopSync(turnOff: Boolean = true) {
        syncJob?.cancel()
        syncJob = null
        lastSignature = ""
        if (turnOff) {
            scope.launch {
                if (glyphRepository.isSessionActive.value) {
                    glyphRepository.turnOff()
                }
            }
        }
    }

    private suspend fun pushFrame(channels: List<Int>) {
        if (!glyphRepository.isSessionActive.value) return
        val signature = channels.joinToString(",")
        if (signature == lastSignature) return
        lastSignature = signature
        try {
            if (channels.isEmpty()) {
                glyphRepository.turnOff()
            } else {
                glyphRepository.setChannels(channels)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push frame", e)
        }
    }

    private fun channelsFromAlphas(alphas: FloatArray): List<Int> {
        val out = ArrayList<Int>(8)
        for (i in alphas.indices) {
            if (alphas[i] >= ALPHA_THRESHOLD) out.add(i)
        }
        return out
    }
}

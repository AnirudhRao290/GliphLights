package com.example.gliphlights.pathbuilder

import android.util.Log
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.pathbuilder.model.EngineSnapshot
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Mirrors AnimationEngine preview alphas onto physical Glyph lights via setChannels.
 */
class PathHardwarePlayer(
    private val glyphRepository: GlyphRepository,
    private val sessionArbiter: GlyphSessionArbiter,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "PathHardwarePlayer"
        private const val ALPHA_THRESHOLD = 0.18f
    }

    private var syncJob: Job? = null
    private var preemptJob: Job? = null
    private var lastSignature: String = ""
    private var ownershipToken: String? = null
    private var onPreempted: (() -> Unit)? = null

    val isSyncing: Boolean get() = syncJob?.isActive == true

    fun setOnPreempted(callback: (() -> Unit)?) {
        onPreempted = callback
    }

    suspend fun ensureSession(): SdkResult<Unit> {
        when (val acquired = sessionArbiter.acquire(
            GlyphClient.PATH,
            ownershipToken ?: UUID.randomUUID().toString()
        )) {
            is AcquireResult.Granted -> ownershipToken = acquired.token
            is AcquireResult.Denied -> {
                return SdkResult.Error(Exception(acquired.reason), acquired.reason)
            }
        }
        if (glyphRepository.isSessionActive.value) {
            return SdkResult.Success(Unit)
        }
        if (!glyphRepository.isConnected.value) {
            val init = glyphRepository.initialize()
            if (init is SdkResult.Error) {
                releaseOwnership()
                return init
            }
            val reg = glyphRepository.register()
            if (reg is SdkResult.Error) {
                releaseOwnership()
                return reg
            }
        }
        val opened = glyphRepository.openSession()
        if (opened is SdkResult.Error) {
            releaseOwnership()
        }
        return opened
    }

    fun startSync(snapshotFlow: kotlinx.coroutines.flow.StateFlow<EngineSnapshot>) {
        stopSync(turnOff = false, releaseOwnership = false)
        preemptJob = scope.launch {
            sessionArbiter.preemptEvents.collect { event ->
                if (event.victim == GlyphClient.PATH) {
                    ownershipToken = null
                    syncJob?.cancel()
                    syncJob = null
                    lastSignature = ""
                    onPreempted?.invoke()
                }
            }
        }
        syncJob = scope.launch {
            snapshotFlow
                .map { snap -> channelsFromAlphas(snap.nodeAlphas) }
                .distinctUntilChanged()
                .collectLatest { channels ->
                    pushFrame(channels)
                }
        }
    }

    fun stopSync(turnOff: Boolean = true, releaseOwnership: Boolean = true) {
        syncJob?.cancel()
        syncJob = null
        preemptJob?.cancel()
        preemptJob = null
        lastSignature = ""
        if (releaseOwnership) {
            releaseOwnership()
        }
        if (turnOff) {
            scope.launch {
                if (glyphRepository.isSessionActive.value) {
                    glyphRepository.turnOff()
                }
            }
        }
    }

    private fun releaseOwnership() {
        val token = ownershipToken ?: return
        ownershipToken = null
        scope.launch {
            sessionArbiter.release(GlyphClient.PATH, token)
        }
    }

    private suspend fun pushFrame(channels: List<Int>) {
        if (!sessionArbiter.canWrite(GlyphClient.PATH, ownershipToken)) return
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

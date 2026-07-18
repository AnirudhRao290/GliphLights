package com.example.gliphlights.physics

import android.util.Log
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.physics.model.PhysicsAnimationModel
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Mirrors physics AnimationModel onto Glyph hardware. No physics logic here.
 */
class PhysicsHardwareBridge(
    private val glyphRepository: GlyphRepository,
    private val sessionArbiter: GlyphSessionArbiter,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "PhysicsHardware"
        private const val THRESHOLD = 0.12f
    }

    private var job: Job? = null
    private var preemptJob: Job? = null
    private var lastSig = ""
    private var ownershipToken: String? = null
    private var onPreempted: (() -> Unit)? = null

    fun setOnPreempted(callback: (() -> Unit)?) {
        onPreempted = callback
    }

    suspend fun ensureSession(): SdkResult<Unit> {
        when (val acquired = sessionArbiter.acquire(
            GlyphClient.PHYSICS,
            ownershipToken ?: UUID.randomUUID().toString()
        )) {
            is AcquireResult.Granted -> ownershipToken = acquired.token
            is AcquireResult.Denied -> {
                return SdkResult.Error(Exception(acquired.reason), acquired.reason)
            }
        }
        if (glyphRepository.isSessionActive.value) return SdkResult.Success(Unit)
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

    fun start(modelFlow: StateFlow<PhysicsAnimationModel>) {
        stop(turnOff = false, releaseOwnership = false)
        preemptJob = scope.launch {
            sessionArbiter.preemptEvents.collect { event ->
                if (event.victim == GlyphClient.PHYSICS) {
                    ownershipToken = null
                    job?.cancel()
                    job = null
                    lastSig = ""
                    onPreempted?.invoke()
                }
            }
        }
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

    fun stop(turnOff: Boolean = true, releaseOwnership: Boolean = true) {
        job?.cancel()
        job = null
        preemptJob?.cancel()
        preemptJob = null
        lastSig = ""
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
            sessionArbiter.release(GlyphClient.PHYSICS, token)
        }
    }

    private suspend fun push(channels: List<Int>) {
        if (!sessionArbiter.canWrite(GlyphClient.PHYSICS, ownershipToken)) return
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

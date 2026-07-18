package com.example.gliphlights.editor.sdk

import android.util.Log
import com.example.gliphlights.editor.model.AnimationModel
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphSdkRenderer @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val sessionArbiter: GlyphSessionArbiter
) {
    companion object {
        private const val TAG = "GlyphSdkRenderer"
        private const val MIN_FRAME_INTERVAL_MS = 33L
    }

    private var scope: CoroutineScope? = null
    private var lastCommandTime = 0L
    private var pendingModel: AnimationModel? = null
    private var coalesceJob: Job? = null
    private var ownershipToken: String? = null

    fun start() {
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
        scope?.launch {
            when (val result = sessionArbiter.acquire(GlyphClient.EDITOR, ownershipToken ?: UUID.randomUUID().toString())) {
                is AcquireResult.Granted -> ownershipToken = result.token
                is AcquireResult.Denied -> Log.w(TAG, "acquire denied: ${result.reason}")
            }
        }
        scope?.launch {
            sessionArbiter.preemptEvents.collect { event ->
                if (event.victim == GlyphClient.EDITOR) {
                    ownershipToken = null
                    Log.d(TAG, "Editor preempted by ${event.by.label}")
                }
            }
        }
    }

    fun stop() {
        coalesceJob?.cancel()
        coalesceJob = null
        pendingModel = null
        val token = ownershipToken
        ownershipToken = null
        scope?.cancel()
        scope = null
        lastCommandTime = 0L
        if (token != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                sessionArbiter.release(GlyphClient.EDITOR, token)
            }
        }
    }

    fun render(model: AnimationModel) {
        val currentScope = scope ?: run {
            Log.w(TAG, "render: scope was null, auto-starting renderer")
            start()
            scope ?: return
        }
        val channels = model.activeChannels
        Log.d(TAG, "render: channels=$channels")
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
        Log.d(TAG, "sendCommand: channels=$activeChannels")
        scope?.launch {
            if (!sessionArbiter.canWrite(GlyphClient.EDITOR, ownershipToken)) {
                when (val result = sessionArbiter.acquire(
                    GlyphClient.EDITOR,
                    ownershipToken ?: UUID.randomUUID().toString()
                )) {
                    is AcquireResult.Granted -> ownershipToken = result.token
                    is AcquireResult.Denied -> {
                        Log.w(TAG, "sendCommand: ${result.reason}")
                        return@launch
                    }
                }
            }
            val isActive = glyphRepository.isSessionActive.first()
            Log.d(TAG, "sendCommand: session active=$isActive")
            if (!isActive) {
                Log.w(TAG, "sendCommand: session not active, dropping command")
                return@launch
            }

            if (activeChannels.isEmpty()) {
                Log.d(TAG, "sendCommand: turning off all channels")
                glyphRepository.turnOff()
            } else {
                Log.d(TAG, "sendCommand: setChannels $activeChannels")
                val result = glyphRepository.setChannels(activeChannels)
                Log.d(TAG, "sendCommand: setChannels result=$result")
            }
        }
    }

    fun destroy() {
        stop()
    }
}

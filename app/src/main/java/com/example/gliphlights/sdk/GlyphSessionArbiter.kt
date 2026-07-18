package com.example.gliphlights.sdk

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Priority: EDITOR > PATH > PHYSICS > VISUALIZER >
 * FOCUS > PRESENCE > CLOCK > PERFORM > AMBIENT > TILE/WIDGET
 */
enum class SessionOwner(val priority: Int, val label: String) {
    EDITOR(100, "Editor"),
    PATH(80, "Path Builder"),
    PHYSICS(60, "Physics Lab"),
    VISUALIZER(40, "Visualizer"),
    FOCUS(38, "Focus Timer"),
    PRESENCE(37, "Presence"),
    CLOCK(36, "Glyph Clock"),
    PERFORM(35, "Perform"),
    AMBIENT(30, "Ambient"),
    TILE_WIDGET(20, "Widget / QS")
}

/** Alias used by hardware players / widget / QS. */
typealias GlyphClient = SessionOwner

sealed class AcquireResult {
    data class Granted(val token: String = "") : AcquireResult() {
        companion object {
            /** Token-less granted for ViewModel-style tryAcquire. */
            val UnitGranted: AcquireResult = Granted("")
        }
    }

    data class Denied(
        val reason: String,
        val owner: SessionOwner? = null
    ) : AcquireResult() {
        val message: String get() = reason
    }
}

data class PreemptEvent(
    val previous: SessionOwner,
    val by: SessionOwner,
    val message: String
) {
    val victim: SessionOwner get() = previous
    val messageForPrevious: String get() = message
}

@Singleton
class GlyphSessionArbiter @Inject constructor() {

    companion object {
        private const val TAG = "GlyphSessionArbiter"
    }

    private val mutex = Mutex()

    private var currentOwner: SessionOwner? = null
    private var ownerToken: String? = null

    private val _ownerState = MutableStateFlow<SessionOwner?>(null)
    /** Current session owner; null when free. */
    val ownerState: StateFlow<SessionOwner?> = _ownerState.asStateFlow()
    val owner: StateFlow<SessionOwner?> = ownerState

    private val _busyMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val busyMessages: SharedFlow<String> = _busyMessages.asSharedFlow()

    private val _preemptEvents = MutableSharedFlow<PreemptEvent>(extraBufferCapacity = 4)
    val preemptEvents: SharedFlow<PreemptEvent> = _preemptEvents.asSharedFlow()

    /**
     * ViewModel-friendly acquire without an explicit token.
     */
    suspend fun tryAcquire(client: SessionOwner): AcquireResult {
        return when (val result = acquire(client, UUID.randomUUID().toString())) {
            is AcquireResult.Granted -> AcquireResult.Granted(result.token)
            is AcquireResult.Denied -> result
        }
    }

    suspend fun acquire(client: SessionOwner, token: String = UUID.randomUUID().toString()): AcquireResult =
        mutex.withLock {
            val held = currentOwner
            if (held == null) {
                return@withLock grant(client, token)
            }
            if (held == client && (ownerToken == token || token.isEmpty())) {
                return@withLock AcquireResult.Granted(ownerToken ?: token)
            }
            if (held == client) {
                return@withLock grant(client, token)
            }
            if (client.priority > held.priority) {
                val victim = held
                Log.d(TAG, "${client.label} preempts ${victim.label}")
                _preemptEvents.tryEmit(
                    PreemptEvent(
                        previous = victim,
                        by = client,
                        message = "Glyph busy — ${client.label} took over"
                    )
                )
                _busyMessages.tryEmit("Glyph busy — ${client.label} took over from ${victim.label}")
                return@withLock grant(client, token)
            }
            if (client.priority == held.priority) {
                return@withLock grant(client, token)
            }
            val reason = "Glyph busy — ${held.label} is using the lights"
            Log.d(TAG, "${client.label} denied: $reason")
            _busyMessages.tryEmit(reason)
            AcquireResult.Denied(reason, held)
        }

    suspend fun release(client: SessionOwner, token: String? = null) = mutex.withLock {
        if (currentOwner != client) return@withLock
        if (token != null && ownerToken != null && token != ownerToken && token.isNotEmpty()) {
            return@withLock
        }
        Log.d(TAG, "${client.label} released session")
        currentOwner = null
        ownerToken = null
        _ownerState.value = null
    }

    fun isOwner(client: SessionOwner, token: String? = null): Boolean {
        if (currentOwner != client) return false
        if (token == null || token.isEmpty()) return true
        return ownerToken == token
    }

    fun currentOwner(): SessionOwner? = currentOwner

    fun canWrite(client: SessionOwner, token: String? = null): Boolean = isOwner(client, token)

    private fun grant(client: SessionOwner, token: String): AcquireResult.Granted {
        currentOwner = client
        ownerToken = token.ifEmpty { UUID.randomUUID().toString() }
        _ownerState.value = client
        Log.d(TAG, "Granted to ${client.label} token=${ownerToken!!.take(8)}")
        return AcquireResult.Granted(ownerToken!!)
    }
}

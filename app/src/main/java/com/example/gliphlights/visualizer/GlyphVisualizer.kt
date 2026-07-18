package com.example.gliphlights.visualizer

import android.util.Log
import com.example.gliphlights.audio.AudioCaptureManager
import com.example.gliphlights.audio.SignalProcessor
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphVisualizer @Inject constructor(
    private val audioCaptureManager: AudioCaptureManager,
    private val signalProcessor: SignalProcessor,
    private val glyphRepository: GlyphRepository,
    private val sessionArbiter: GlyphSessionArbiter
) {
    companion object {
        private const val TAG = "GlyphVisualizer"
        private const val MIN_FRAME_INTERVAL_MS = 33L
    }

    private var engineScope: CoroutineScope? = null
    private var audioJob: Job? = null
    private var preemptJob: Job? = null
    private var lastFrameTime = 0L
    private var frameCount = 0L
    private var startTime = 0L
    private var ownershipToken: String? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _fps = MutableStateFlow(0.0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _updateRate = MutableStateFlow(0)
    val updateRate: StateFlow<Int> = _updateRate.asStateFlow()

    private val _latency = MutableStateFlow(0L)
    val latency: StateFlow<Long> = _latency.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastCommand = MutableStateFlow<GlyphCommand?>(null)
    val lastCommand: StateFlow<GlyphCommand?> = _lastCommand.asStateFlow()

    private val bakeSamples = ArrayList<Pair<Long, GlyphCommand>>(256)
    private var baking = false
    private var bakeStartMs = 0L

    private var currentMode: VisualizationMode = PulseMode()

    fun setMode(mode: VisualizationMode) {
        currentMode.reset()
        currentMode = mode
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    /** Begin capturing commands for Path bake (call while running). */
    fun startBakeCapture() {
        bakeSamples.clear()
        bakeStartMs = System.currentTimeMillis()
        baking = true
    }

    fun stopBakeCapture(): List<Pair<Long, GlyphCommand>> {
        baking = false
        return bakeSamples.toList()
    }

    fun isBaking(): Boolean = baking

    /**
     * Starts audio capture and Glyph output. Returns false if the session
     * could not be acquired (caller should show [statusMessage]).
     */
    suspend fun start(): Boolean {
        if (_isRunning.value) return true

        when (val acquired = sessionArbiter.acquire(
            GlyphClient.VISUALIZER,
            ownershipToken ?: UUID.randomUUID().toString()
        )) {
            is AcquireResult.Granted -> ownershipToken = acquired.token
            is AcquireResult.Denied -> {
                _statusMessage.value = acquired.reason
                return false
            }
        }

        if (!glyphRepository.isSessionActive.value) {
            if (!glyphRepository.isConnected.value) {
                val init = glyphRepository.initialize()
                if (init is SdkResult.Error) {
                    _statusMessage.value = init.message
                    releaseOwnership()
                    return false
                }
                val reg = glyphRepository.register()
                if (reg is SdkResult.Error) {
                    _statusMessage.value = reg.message
                    releaseOwnership()
                    return false
                }
            }
            val opened = glyphRepository.openSession()
            if (opened is SdkResult.Error) {
                _statusMessage.value = opened.message
                releaseOwnership()
                return false
            }
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        engineScope = scope
        startTime = System.currentTimeMillis()
        frameCount = 0

        if (!audioCaptureManager.start()) {
            Log.e(TAG, "Failed to start audio capture")
            releaseOwnership()
            scope.cancel()
            engineScope = null
            _statusMessage.value = "Failed to start audio capture"
            return false
        }

        preemptJob = scope.launch {
            sessionArbiter.preemptEvents.collect { event ->
                if (event.victim == GlyphClient.VISUALIZER && _isRunning.value) {
                    ownershipToken = null
                    _statusMessage.value = event.message
                    haltWithoutRelease()
                }
            }
        }

        audioJob = scope.launch {
            audioCaptureManager.audioBuffer.collect { buffer ->
                if (!sessionArbiter.canWrite(GlyphClient.VISUALIZER, ownershipToken)) return@collect
                val processingStart = System.currentTimeMillis()
                signalProcessor.processBuffer(buffer)
                val amplitude = signalProcessor.amplitude.value

                val now = System.currentTimeMillis()
                if (now - lastFrameTime >= MIN_FRAME_INTERVAL_MS) {
                    val command = currentMode.render(amplitude, now)
                    if (command != null &&
                        sessionArbiter.canWrite(GlyphClient.VISUALIZER, ownershipToken)
                    ) {
                        frameCount++
                        _lastCommand.value = command
                        if (baking) {
                            bakeSamples.add(now - bakeStartMs to command)
                        }
                        glyphRepository.toggleWithBrightness(
                            command.channels,
                            command.brightness
                        )
                    }
                    lastFrameTime = now
                }

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > 0) {
                    _fps.value = (frameCount * 1000.0f / elapsed)
                    _updateRate.value = (frameCount * 1000 / elapsed).toInt()
                }
                _latency.value = System.currentTimeMillis() - processingStart
            }
        }

        _isRunning.value = true
        Log.d(TAG, "Visualizer started (mode=${currentMode.displayName})")
        return true
    }

    fun stop() {
        val token = ownershipToken
        ownershipToken = null
        haltWithoutRelease()
        val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        cleanupScope.launch {
            if (glyphRepository.isSessionActive.value) {
                glyphRepository.turnOff()
            }
            if (token != null) {
                sessionArbiter.release(GlyphClient.VISUALIZER, token)
            }
            cleanupScope.cancel()
        }
        Log.d(TAG, "Visualizer stopped")
    }

    private fun haltWithoutRelease() {
        audioJob?.cancel()
        audioJob = null
        preemptJob?.cancel()
        preemptJob = null
        audioCaptureManager.stop()
        signalProcessor.reset()
        currentMode.reset()
        _isRunning.value = false
        _fps.value = 0.0f
        _updateRate.value = 0
        _latency.value = 0L
        engineScope?.cancel()
        engineScope = null
    }

    private fun releaseOwnership() {
        val token = ownershipToken ?: return
        ownershipToken = null
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            sessionArbiter.release(GlyphClient.VISUALIZER, token)
        }
    }

    fun destroy() {
        stop()
    }
}

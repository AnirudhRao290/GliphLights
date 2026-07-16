package com.example.gliphlights.visualizer

import android.util.Log
import com.example.gliphlights.audio.AudioCaptureManager
import com.example.gliphlights.audio.SignalProcessor
import com.example.gliphlights.repository.GlyphRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphVisualizer @Inject constructor(
    private val audioCaptureManager: AudioCaptureManager,
    private val signalProcessor: SignalProcessor,
    private val glyphRepository: GlyphRepository
) {
    companion object {
        private const val TAG = "GlyphVisualizer"
        private const val MIN_FRAME_INTERVAL_MS = 33L
    }

    private var engineScope: CoroutineScope? = null
    private var audioJob: Job? = null
    private var lastFrameTime = 0L
    private var frameCount = 0L
    private var startTime = 0L

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _fps = MutableStateFlow(0.0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _updateRate = MutableStateFlow(0)
    val updateRate: StateFlow<Int> = _updateRate.asStateFlow()

    private val _latency = MutableStateFlow(0L)
    val latency: StateFlow<Long> = _latency.asStateFlow()

    private var currentMode: VisualizationMode = PulseMode()

    fun setMode(mode: VisualizationMode) {
        currentMode.reset()
        currentMode = mode
    }

    fun start() {
        if (_isRunning.value) return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        engineScope = scope
        startTime = System.currentTimeMillis()
        frameCount = 0

        if (!audioCaptureManager.start()) {
            Log.e(TAG, "Failed to start audio capture")
            scope.cancel()
            return
        }

        audioJob = scope.launch {
            audioCaptureManager.audioBuffer.collect { buffer ->
                val processingStart = System.currentTimeMillis()
                signalProcessor.processBuffer(buffer)
                val amplitude = signalProcessor.amplitude.value

                val now = System.currentTimeMillis()
                if (now - lastFrameTime >= MIN_FRAME_INTERVAL_MS) {
                    val command = currentMode.render(amplitude, now)
                    if (command != null) {
                        frameCount++
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
    }

    fun stop() {
        audioJob?.cancel()
        audioJob = null

        audioCaptureManager.stop()
        signalProcessor.reset()
        currentMode.reset()

        _isRunning.value = false
        _fps.value = 0.0f
        _updateRate.value = 0
        _latency.value = 0L

        val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        cleanupScope.launch {
            glyphRepository.turnOff()
            cleanupScope.cancel()
        }
        engineScope?.cancel()
        engineScope = null

        Log.d(TAG, "Visualizer stopped")
    }

    fun destroy() {
        stop()
    }
}

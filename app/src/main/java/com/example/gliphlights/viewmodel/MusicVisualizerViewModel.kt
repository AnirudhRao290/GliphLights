package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.audio.AudioCaptureManager
import com.example.gliphlights.audio.SignalProcessor
import com.example.gliphlights.pathbuilder.SequenceRepository
import com.example.gliphlights.pathbuilder.bake.VisualizerPathBaker
import com.example.gliphlights.repository.PresetRepository
import com.example.gliphlights.visualizer.BeatMode
import com.example.gliphlights.visualizer.GlowMode
import com.example.gliphlights.visualizer.GlyphVisualizer
import com.example.gliphlights.visualizer.PulseMode
import com.example.gliphlights.visualizer.VisualizationMode
import com.example.gliphlights.visualizer.WaveMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicVisualizerUiState(
    val isRunning: Boolean = false,
    val mode: VisualizationModeType = VisualizationModeType.PULSE,
    val sensitivity: Float = 1.0f,
    val noiseGate: Float = 0.05f,
    val audioLevel: Float = 0.0f,
    val rawAmplitude: Float = 0.0f,
    val peakAmplitude: Float = 0.0f,
    val fps: Float = 0.0f,
    val updateRate: Int = 0,
    val latency: Long = 0L,
    val hasPermission: Boolean = false,
    val permissionRequested: Boolean = false,
    val showDebug: Boolean = false,
    val isBaking: Boolean = false,
    val bakeProgressMs: Long = 0L,
    val statusMessage: String? = null
)

enum class VisualizationModeType(val displayName: String) {
    PULSE("Pulse"),
    WAVE("Wave"),
    BEAT("Beat"),
    GLOW("Glow")
}

@HiltViewModel
class MusicVisualizerViewModel @Inject constructor(
    private val audioCaptureManager: AudioCaptureManager,
    private val signalProcessor: SignalProcessor,
    private val glyphVisualizer: GlyphVisualizer,
    private val sequenceRepository: SequenceRepository,
    private val presetRepository: PresetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicVisualizerUiState())
    val uiState: StateFlow<MusicVisualizerUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(hasPermission = audioCaptureManager.hasPermission()) }
        observeVisualizerState()
        observeAudioLevel()
    }

    private fun observeVisualizerState() {
        viewModelScope.launch {
            glyphVisualizer.isRunning.collect { running ->
                _uiState.update { it.copy(isRunning = running) }
            }
        }
        viewModelScope.launch {
            glyphVisualizer.fps.collect { fps ->
                _uiState.update { it.copy(fps = fps) }
            }
        }
        viewModelScope.launch {
            glyphVisualizer.updateRate.collect { rate ->
                _uiState.update { it.copy(updateRate = rate) }
            }
        }
        viewModelScope.launch {
            glyphVisualizer.latency.collect { latency ->
                _uiState.update { it.copy(latency = latency) }
            }
        }
        viewModelScope.launch {
            glyphVisualizer.statusMessage.collect { message ->
                if (message != null) {
                    _uiState.update { it.copy(statusMessage = message) }
                }
            }
        }
    }

    fun clearStatus() {
        glyphVisualizer.clearStatus()
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun observeAudioLevel() {
        viewModelScope.launch {
            signalProcessor.amplitude.collect { amp ->
                _uiState.update { it.copy(audioLevel = amp) }
            }
        }
        viewModelScope.launch {
            signalProcessor.rawAmplitude.collect { raw ->
                _uiState.update { it.copy(rawAmplitude = raw) }
            }
        }
        viewModelScope.launch {
            signalProcessor.peakAmplitude.collect { peak ->
                _uiState.update { it.copy(peakAmplitude = peak) }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted, permissionRequested = true) }
        if (granted) {
            start()
        }
    }

    fun start() {
        if (!_uiState.value.hasPermission) return
        viewModelScope.launch {
            signalProcessor.setSensitivity(_uiState.value.sensitivity)
            signalProcessor.setNoiseGateThreshold(_uiState.value.noiseGate)
            val started = glyphVisualizer.start()
            if (!started) {
                _uiState.update {
                    it.copy(
                        statusMessage = glyphVisualizer.statusMessage.value
                            ?: "Glyph busy"
                    )
                }
            }
        }
    }

    fun stop() {
        glyphVisualizer.stop()
    }

    fun selectMode(mode: VisualizationModeType) {
        _uiState.update { it.copy(mode = mode) }
        glyphVisualizer.setMode(mode.toImplementation())
    }

    fun updateSensitivity(value: Float) {
        _uiState.update { it.copy(sensitivity = value) }
        signalProcessor.setSensitivity(value)
    }

    fun updateNoiseGate(value: Float) {
        _uiState.update { it.copy(noiseGate = value) }
        signalProcessor.setNoiseGateThreshold(value)
    }

    fun toggleDebug() {
        _uiState.update { it.copy(showDebug = !it.showDebug) }
    }

    /**
     * Records ~5s of live viz frames, bakes to PATH, dual-writes Sequence + Preset.
     */
    fun bakeFiveSeconds() {
        if (!_uiState.value.isRunning || _uiState.value.isBaking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBaking = true, bakeProgressMs = 0L, statusMessage = "Baking 5s…") }
            glyphVisualizer.startBakeCapture()
            val duration = 5_000L
            val step = 100L
            var elapsed = 0L
            while (elapsed < duration) {
                delay(step)
                elapsed += step
                _uiState.update { it.copy(bakeProgressMs = elapsed.coerceAtMost(duration)) }
                if (!_uiState.value.isRunning) break
            }
            val samples = glyphVisualizer.stopBakeCapture()
            val baked = VisualizerPathBaker.bake(samples, targetDurationMs = duration)
            if (baked.nodes.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isBaking = false,
                        bakeProgressMs = 0L,
                        statusMessage = "Bake empty — play louder audio and retry"
                    )
                }
                return@launch
            }
            val name = "Viz Bake ${System.currentTimeMillis() % 10000}"
            sequenceRepository.save(name, baked.nodes, baked.settings)
            presetRepository.savePath(name, baked.nodes, baked.settings)
            _uiState.update {
                it.copy(
                    isBaking = false,
                    bakeProgressMs = 0L,
                    statusMessage = "Saved “$name” (${baked.nodes.size} nodes) — play from Dashboard"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        glyphVisualizer.destroy()
        signalProcessor.destroy()
        audioCaptureManager.destroy()
    }

    private fun VisualizationModeType.toImplementation(): VisualizationMode {
        return when (this) {
            VisualizationModeType.PULSE -> PulseMode()
            VisualizationModeType.WAVE -> WaveMode()
            VisualizationModeType.BEAT -> BeatMode()
            VisualizationModeType.GLOW -> GlowMode()
        }
    }
}

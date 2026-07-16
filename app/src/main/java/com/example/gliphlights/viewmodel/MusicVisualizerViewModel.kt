package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.audio.AudioCaptureManager
import com.example.gliphlights.audio.SignalProcessor
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.visualizer.BeatMode
import com.example.gliphlights.visualizer.GlowMode
import com.example.gliphlights.visualizer.GlyphVisualizer
import com.example.gliphlights.visualizer.PulseMode
import com.example.gliphlights.visualizer.VisualizationMode
import com.example.gliphlights.visualizer.WaveMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    val showDebug: Boolean = false
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
    private val glyphVisualizer: GlyphVisualizer
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
        signalProcessor.setSensitivity(_uiState.value.sensitivity)
        signalProcessor.setNoiseGateThreshold(_uiState.value.noiseGate)
        glyphVisualizer.start()
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

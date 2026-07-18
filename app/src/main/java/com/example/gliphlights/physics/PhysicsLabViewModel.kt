package com.example.gliphlights.physics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.physics.engine.PhysicsEngine
import com.example.gliphlights.physics.model.PhysicsAnimationModel
import com.example.gliphlights.physics.model.PhysicsMode
import com.example.gliphlights.physics.model.PhysicsParams
import com.example.gliphlights.physics.sensor.SensorManagerWrapper
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhysicsLabUiState(
    val running: Boolean = false,
    val mode: PhysicsMode = PhysicsMode.GRAVITY,
    val params: PhysicsParams = PhysicsParams(),
    val layout: GlyphNodeLayout? = null,
    val hardwareEnabled: Boolean = false,
    val hardwareBusy: Boolean = false,
    val showParams: Boolean = false,
    val statusMessage: String? = null,
    val fps: Int = 0,
    val settingsLoaded: Boolean = false,
    val gravityX: Float = 0f,
    val gravityY: Float = 9.81f
)

@HiltViewModel
class PhysicsLabViewModel @Inject constructor(
    private val sensors: SensorManagerWrapper,
    private val glyphRepository: GlyphRepository,
    private val settingsRepository: PhysicsSettingsRepository,
    private val sessionArbiter: GlyphSessionArbiter
) : ViewModel() {

    private val engine = PhysicsEngine()
    private val hardware = PhysicsHardwareBridge(
        glyphRepository,
        sessionArbiter,
        viewModelScope
    )

    private val _uiState = MutableStateFlow(PhysicsLabUiState())
    val uiState: StateFlow<PhysicsLabUiState> = _uiState.asStateFlow()

    private val _model = MutableStateFlow(PhysicsAnimationModel.empty())
    val animationModel: StateFlow<PhysicsAnimationModel> = _model.asStateFlow()

    private var loopJob: Job? = null
    private var layoutBound = false

    init {
        hardware.setOnPreempted {
            _uiState.update {
                it.copy(
                    hardwareEnabled = false,
                    statusMessage = "Glyph taken by another studio tool"
                )
            }
        }
        viewModelScope.launch {
            val saved = settingsRepository.settings.first()
            if (saved != null) {
                engine.mode = saved.mode
                engine.params = saved.params
                _uiState.update {
                    it.copy(
                        mode = saved.mode,
                        params = saved.params,
                        settingsLoaded = true
                    )
                }
            } else {
                applyModeDefaults(PhysicsMode.GRAVITY, persist = false)
                _uiState.update { it.copy(settingsLoaded = true) }
            }
        }
        viewModelScope.launch {
            sessionArbiter.preemptEvents.collect { event ->
                if (event.victim == GlyphClient.PHYSICS) {
                    _uiState.update {
                        it.copy(
                            hardwareEnabled = false,
                            statusMessage = event.message
                        )
                    }
                }
            }
        }
    }

    fun onLayoutCreated(layout: GlyphNodeLayout) {
        engine.bindLayout(layout)
        layoutBound = true
        _uiState.update { it.copy(layout = layout) }
        if (_uiState.value.running) restartLoop()
    }

    fun start() {
        sensors.start()
        _uiState.update { it.copy(running = true) }
        restartLoop()
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        sensors.stop()
        hardware.stop(turnOff = true)
        _uiState.update { it.copy(running = false, hardwareEnabled = false, fps = 0) }
    }

    fun setMode(mode: PhysicsMode) {
        engine.mode = mode
        engine.reset()
        _uiState.update { it.copy(mode = mode) }
        applyModeDefaults(mode, persist = false)
    }

    fun updateParams(params: PhysicsParams) {
        engine.params = params
        _uiState.update { it.copy(params = params) }
        _uiState.value.layout?.let { engine.bindLayout(it) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            settingsRepository.save(state.mode, state.params)
            _uiState.update { it.copy(statusMessage = "Physics settings saved") }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.clear()
            val mode = _uiState.value.mode
            applyModeDefaults(mode, persist = false)
            engine.reset()
            _uiState.update { it.copy(statusMessage = "Physics settings reset") }
        }
    }

    fun toggleParams() {
        _uiState.update { it.copy(showParams = !it.showParams) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun onMagnetTap(x: Float, y: Float) {
        if (_uiState.value.mode != PhysicsMode.MAGNET) return
        engine.setMagnet(x, y)
    }

    fun clearMagnet() {
        engine.clearMagnet()
    }

    fun playOnGlyph() {
        viewModelScope.launch {
            _uiState.update { it.copy(hardwareBusy = true) }
            if (!_uiState.value.running) start()
            when (val r = hardware.ensureSession()) {
                is SdkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            hardwareBusy = false,
                            statusMessage = r.message ?: "Glyph session failed"
                        )
                    }
                }
                is SdkResult.Success -> {
                    hardware.start(_model)
                    _uiState.update {
                        it.copy(
                            hardwareEnabled = true,
                            hardwareBusy = false,
                            statusMessage = "Physics streaming to Glyph"
                        )
                    }
                }
            }
        }
    }

    fun stopGlyph() {
        hardware.stop(turnOff = true)
        _uiState.update { it.copy(hardwareEnabled = false, statusMessage = "Glyph stopped") }
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }

    private fun restartLoop() {
        loopJob?.cancel()
        if (!layoutBound) return
        loopJob = viewModelScope.launch {
            var last = System.nanoTime()
            var frames = 0
            var fpsWindow = last
            while (isActive && _uiState.value.running) {
                val now = System.nanoTime()
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0.008f, 0.05f)
                last = now
                val sample = sensors.latest()
                engine.params = _uiState.value.params
                engine.mode = _uiState.value.mode
                _model.value = engine.step(sample, dt)
                frames++
                if (now - fpsWindow >= 1_000_000_000L) {
                    _uiState.update {
                        it.copy(
                            fps = frames,
                            gravityX = sample.gravityX,
                            gravityY = sample.gravityY
                        )
                    }
                    frames = 0
                    fpsWindow = now
                }
                delay(16L)
            }
        }
    }

    private fun applyModeDefaults(mode: PhysicsMode, persist: Boolean) {
        val p = defaultsFor(mode)
        updateParams(p)
        if (persist) {
            viewModelScope.launch {
                settingsRepository.save(mode, p)
            }
        }
    }

    companion object {
        fun defaultsFor(mode: PhysicsMode): PhysicsParams = when (mode) {
            PhysicsMode.GRAVITY -> PhysicsParams(flowSpeed = 1.1f, fluidAmount = 0.4f, trailLength = 4)
            PhysicsMode.FLUID -> PhysicsParams(fluidAmount = 0.5f, viscosity = 0.45f, surfaceTension = 0.35f)
            PhysicsMode.MERCURY -> PhysicsParams(
                fluidAmount = 0.35f, viscosity = 0.8f, surfaceTension = 0.75f, flowSpeed = 0.55f
            )
            PhysicsMode.SAND -> PhysicsParams(particleCount = 56, damping = 0.9f, gravityStrength = 1.2f)
            PhysicsMode.BUBBLE -> PhysicsParams(fluidAmount = 0.4f, flowSpeed = 0.9f, viscosity = 0.35f)
            PhysicsMode.MAGNET -> PhysicsParams(fluidAmount = 0.45f, flowSpeed = 1.3f)
            PhysicsMode.ZERO_G -> PhysicsParams(particleCount = 40, damping = 0.995f, gravityStrength = 0f)
            PhysicsMode.PINBALL -> PhysicsParams(particleCount = 24, damping = 0.97f, flowSpeed = 1.2f)
        }
    }
}

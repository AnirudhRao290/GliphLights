package com.example.gliphlights.viewmodel

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.models.ErrorState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphUiState
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.presets.GlyphPackShare
import com.example.gliphlights.presets.GlyphPreset
import com.example.gliphlights.presets.PresetPlayer
import com.example.gliphlights.presets.PresetType
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.PresetRepository
import com.example.gliphlights.repository.SettingsRepository
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.services.AmbientRitualService
import com.example.gliphlights.ui.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DashboardActivityMode(val label: String) {
    IDLE("Idle"),
    LIVE("Live"),
    ANIMATING("Animating")
}

data class StudioDestination(
    val route: String,
    val title: String,
    val subtitle: String
) {
    companion object {
        val Editor = StudioDestination(
            route = Screen.Editor.route,
            title = "Glyph Editor",
            subtitle = "Paint nodes on the doughnut"
        )
        val PathBuilder = StudioDestination(
            route = Screen.PathBuilder.route,
            title = "Path Builder",
            subtitle = "Draw sequences & play them back"
        )
        val PhysicsLab = StudioDestination(
            route = Screen.PhysicsLab.route,
            title = "Physics Lab",
            subtitle = "Gravity, fluid, sand & more"
        )

        fun fromRoute(route: String): StudioDestination = when (route) {
            Screen.PathBuilder.route -> PathBuilder
            Screen.PhysicsLab.route -> PhysicsLab
            else -> Editor
        }

        val all = listOf(Editor, PathBuilder, PhysicsLab)
    }
}

data class DashboardHeroState(
    val activityMode: DashboardActivityMode = DashboardActivityMode.IDLE,
    val fps: Int = 0,
    val continueDestination: StudioDestination = StudioDestination.Editor,
    val presets: List<GlyphPreset> = emptyList(),
    val progressHudPercent: Int = 0,
    val progressHudLabel: String = "",
    val statusMessage: String? = null,
    val shareIntent: Intent? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val glyphRepository: GlyphRepository,
    private val settingsRepository: SettingsRepository,
    private val presetRepository: PresetRepository,
    private val presetPlayer: PresetPlayer,
    private val glyphPackShare: GlyphPackShare
) : ViewModel() {

    private val _uiState = MutableStateFlow<GlyphUiState>(GlyphUiState.Loading)
    val uiState: StateFlow<GlyphUiState> = _uiState.asStateFlow()

    private val _errorState = MutableStateFlow<ErrorState>(ErrorState.None)
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    private val _heroState = MutableStateFlow(DashboardHeroState())
    val heroState: StateFlow<DashboardHeroState> = _heroState.asStateFlow()

    private var updateCountWindow = 0
    private var animatingUntilMs = 0L
    private var focusJob: Job? = null

    init {
        initializeSdk()
        observeGlyphState()
        observeContinueDestination()
        observePresets()
        startFpsTicker()
    }

    private fun initializeSdk() {
        viewModelScope.launch {
            _uiState.value = GlyphUiState.Loading

            val initResult = glyphRepository.initialize()
            if (initResult is SdkResult.Error) {
                _errorState.value = ErrorState.SdkUnavailable(initResult.message)
                _uiState.value = GlyphUiState.Error(initResult.message)
                return@launch
            }

            val registerResult = glyphRepository.register()
            if (registerResult is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(registerResult.message, registerResult.exception)
                _uiState.value = GlyphUiState.Error(registerResult.message)
                return@launch
            }

            val sessionResult = glyphRepository.openSession()
            if (sessionResult is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(sessionResult.message, sessionResult.exception)
                _uiState.value = GlyphUiState.Error(sessionResult.message)
                return@launch
            }

            glyphRepository.applyStartupBehavior()
        }
    }

    private fun observeGlyphState() {
        viewModelScope.launch {
            combine(
                glyphRepository.glyphState,
                glyphRepository.deviceInfo
            ) { state, device ->
                Pair(state, device)
            }.catch { e ->
                _errorState.value = ErrorState.RuntimeError(e.message ?: "Unknown error", e)
            }.collect { (state, device) ->
                updateCountWindow++
                _uiState.value = GlyphUiState.Success(
                    glyphState = state,
                    deviceInfo = device
                )
                refreshActivityMode(state)
            }
        }
    }

    private fun observeContinueDestination() {
        viewModelScope.launch {
            settingsRepository.lastStudioRoute.collect { route ->
                _heroState.update {
                    it.copy(continueDestination = StudioDestination.fromRoute(route))
                }
            }
        }
    }

    private fun observePresets() {
        viewModelScope.launch {
            presetRepository.presets.collect { list ->
                _heroState.update { it.copy(presets = list) }
            }
        }
    }

    private fun startFpsTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val fps = updateCountWindow.coerceIn(0, 120)
                updateCountWindow = 0
                val glyph = (uiState.value as? GlyphUiState.Success)?.glyphState
                val showFps = glyph?.isActive == true && fps > 0
                _heroState.update { it.copy(fps = if (showFps) fps else 0) }
                refreshActivityMode(glyph ?: GlyphState.INACTIVE)
            }
        }
    }

    private fun refreshActivityMode(state: GlyphState) {
        val now = System.currentTimeMillis()
        val mode = when {
            now < animatingUntilMs -> DashboardActivityMode.ANIMATING
            state.isActive -> DashboardActivityMode.LIVE
            else -> DashboardActivityMode.IDLE
        }
        _heroState.update { it.copy(activityMode = mode) }
    }

    fun rememberStudio(destination: StudioDestination) {
        viewModelScope.launch {
            settingsRepository.updateLastStudioRoute(destination.route)
            _heroState.update { it.copy(continueDestination = destination) }
        }
    }

    fun toggleAll() {
        viewModelScope.launch {
            val currentState = (uiState.value as? GlyphUiState.Success)?.glyphState
            val result = if (currentState?.isActive == true) {
                glyphRepository.turnOff()
            } else {
                glyphRepository.toggleAll()
            }
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun animateAll() {
        viewModelScope.launch {
            animatingUntilMs = System.currentTimeMillis() + 4_000L
            refreshActivityMode(
                (uiState.value as? GlyphUiState.Success)?.glyphState ?: GlyphState.INACTIVE
            )
            val result = glyphRepository.animateAll()
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun turnOff() {
        viewModelScope.launch {
            animatingUntilMs = 0L
            focusJob?.cancel()
            presetPlayer.stop(GlyphClient.PERFORM)
            val result = glyphRepository.turnOff()
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
            _heroState.update { it.copy(progressHudPercent = 0, progressHudLabel = "") }
        }
    }

    fun playPreset(preset: GlyphPreset) {
        viewModelScope.launch {
            when (val result = presetPlayer.playOnce(preset, viewModelScope, loop = preset.type == PresetType.PATH)) {
                is SdkResult.Error -> {
                    _heroState.update { it.copy(statusMessage = result.message) }
                }
                is SdkResult.Success -> {
                    _heroState.update { it.copy(statusMessage = "Playing ${preset.name}") }
                }
            }
        }
    }

    fun togglePin(preset: GlyphPreset) {
        viewModelScope.launch {
            presetRepository.setPinned(preset.id, !preset.pinned)
        }
    }

    fun forkPreset(preset: GlyphPreset) {
        viewModelScope.launch {
            val forked = presetRepository.fork(preset.id)
            _heroState.update {
                it.copy(statusMessage = forked?.let { p -> "Remixed as ${p.name}" } ?: "Fork failed")
            }
        }
    }

    fun sharePreset(preset: GlyphPreset) {
        viewModelScope.launch {
            val intent = glyphPackShare.exportAndShareIntent(listOf(preset.id))
            _heroState.update { it.copy(shareIntent = Intent.createChooser(intent, "Share .glyphpack")) }
        }
    }

    fun consumeShareIntent() {
        _heroState.update { it.copy(shareIntent = null) }
    }

    fun clearStatus() {
        _heroState.update { it.copy(statusMessage = null) }
    }

    fun startBatteryArc() {
        viewModelScope.launch {
            focusJob?.cancel()
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            glyphRepository.displayProgress(level, reverse = false)
            _heroState.update {
                it.copy(
                    progressHudPercent = level,
                    progressHudLabel = "Battery $level%",
                    statusMessage = "Battery arc $level%"
                )
            }
        }
    }

    fun startFiveMinFocus() {
        focusJob?.cancel()
        focusJob = viewModelScope.launch {
            val totalMs = 5 * 60_000L
            val start = System.currentTimeMillis()
            _heroState.update { it.copy(progressHudLabel = "5-min focus", progressHudPercent = 0) }
            while (isActive) {
                val elapsed = System.currentTimeMillis() - start
                val pct = ((elapsed * 100) / totalMs).toInt().coerceIn(0, 100)
                glyphRepository.displayProgress(pct, reverse = false)
                _heroState.update { it.copy(progressHudPercent = pct) }
                if (pct >= 100) break
                delay(1_000L)
            }
            _heroState.update { it.copy(statusMessage = "Focus complete", progressHudLabel = "Done") }
        }
    }

    fun startAmbient() {
        AmbientRitualService.start(context)
        _heroState.update { it.copy(statusMessage = "Ambient ritual started") }
    }

    fun stopAmbient() {
        AmbientRitualService.stop(context)
        _heroState.update { it.copy(statusMessage = "Ambient stopped") }
    }

    fun clearError() {
        _errorState.value = ErrorState.None
    }

    override fun onCleared() {
        super.onCleared()
        focusJob?.cancel()
        presetPlayer.stop(GlyphClient.PERFORM)
    }
}

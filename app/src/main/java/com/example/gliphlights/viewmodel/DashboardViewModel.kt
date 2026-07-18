package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.ErrorState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphUiState
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.SettingsRepository
import com.example.gliphlights.ui.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val continueDestination: StudioDestination = StudioDestination.Editor
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GlyphUiState>(GlyphUiState.Loading)
    val uiState: StateFlow<GlyphUiState> = _uiState.asStateFlow()

    private val _errorState = MutableStateFlow<ErrorState>(ErrorState.None)
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    private val _heroState = MutableStateFlow(DashboardHeroState())
    val heroState: StateFlow<DashboardHeroState> = _heroState.asStateFlow()

    private var updateCountWindow = 0
    private var animatingUntilMs = 0L

    init {
        initializeSdk()
        observeGlyphState()
        observeContinueDestination()
        startFpsTicker()
    }

    private fun initializeSdk() {
        viewModelScope.launch {
            _uiState.value = GlyphUiState.Loading

            val initResult = glyphRepository.initialize()
            if (initResult is com.example.gliphlights.models.SdkResult.Error) {
                _errorState.value = ErrorState.SdkUnavailable(initResult.message)
                _uiState.value = GlyphUiState.Error(initResult.message)
                return@launch
            }

            val registerResult = glyphRepository.register()
            if (registerResult is com.example.gliphlights.models.SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(registerResult.message, registerResult.exception)
                _uiState.value = GlyphUiState.Error(registerResult.message)
                return@launch
            }

            val sessionResult = glyphRepository.openSession()
            if (sessionResult is com.example.gliphlights.models.SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(sessionResult.message, sessionResult.exception)
                _uiState.value = GlyphUiState.Error(sessionResult.message)
                return@launch
            }
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
            if (result is com.example.gliphlights.models.SdkResult.Error) {
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
            if (result is com.example.gliphlights.models.SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun turnOff() {
        viewModelScope.launch {
            animatingUntilMs = 0L
            val result = glyphRepository.turnOff()
            if (result is com.example.gliphlights.models.SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun clearError() {
        _errorState.value = ErrorState.None
    }

    override fun onCleared() {
        super.onCleared()
        // Do not close the shared Glyph session — Editor and other screens own lifecycle.
    }
}

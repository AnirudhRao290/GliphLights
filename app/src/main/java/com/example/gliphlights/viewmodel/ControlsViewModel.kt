package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.models.AnimationParams
import com.example.gliphlights.models.ControlsUiState
import com.example.gliphlights.models.ErrorState
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.repository.GlyphRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlsViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val settingsRepository: com.example.gliphlights.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ControlsUiState>(ControlsUiState.Loading)
    val uiState: StateFlow<ControlsUiState> = _uiState.asStateFlow()

    private val _errorState = MutableStateFlow<ErrorState>(ErrorState.None)
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    private val _animationParams = MutableStateFlow(AnimationParams())
    val animationParams: StateFlow<AnimationParams> = _animationParams.asStateFlow()

    private val _progressValue = MutableStateFlow(0)
    val progressValue: StateFlow<Int> = _progressValue.asStateFlow()

    private val _progressZoneA = MutableStateFlow(0)
    val progressZoneA: StateFlow<Int> = _progressZoneA.asStateFlow()

    private val _progressZoneB = MutableStateFlow(0)
    val progressZoneB: StateFlow<Int> = _progressZoneB.asStateFlow()

    init {
        observeState()
        observeSettingsDefaults()
    }

    private fun observeSettingsDefaults() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                // Seed once from Settings; user can still tweak sliders on this screen.
                if (_animationParams.value == AnimationParams()) {
                    _animationParams.value = AnimationParams(
                        period = settings.animatePeriod,
                        cycles = settings.animateCycles,
                        interval = settings.animateInterval
                    )
                }
            }
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                glyphRepository.glyphState,
                glyphRepository.deviceInfo
            ) { state, device ->
                Pair(state, device)
            }.catch { e ->
                _errorState.value = ErrorState.RuntimeError(e.message ?: "Unknown error", e)
            }.collect { (state, device) ->
                _uiState.value = ControlsUiState.Success(
                    glyphState = state,
                    zones = device.availableZones.ifEmpty { listOf(GlyphZone.A, GlyphZone.B, GlyphZone.C) }
                )
            }
        }
    }

    fun toggleChannels(channels: List<Int>) {
        viewModelScope.launch {
            val result = glyphRepository.toggleChannels(channels)
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun toggleZone(zone: GlyphZone) {
        viewModelScope.launch {
            val currentState = (uiState.value as? ControlsUiState.Success)?.glyphState
            val zoneChannels = zone.channels.toSet()
            val isZoneActive = currentState?.activeChannels?.any { it in zoneChannels } == true

            val result = if (isZoneActive) {
                val channelsToKeep = currentState.activeChannels.filter { it !in zoneChannels }
                if (channelsToKeep.isEmpty()) {
                    glyphRepository.turnOff()
                } else {
                    glyphRepository.setChannels(channelsToKeep.toList())
                }
            } else {
                val newActive = (currentState?.activeChannels ?: emptySet()) + zoneChannels
                glyphRepository.setChannels(newActive.toList())
            }

            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun animateZone(zone: GlyphZone) {
        viewModelScope.launch {
            val result = glyphRepository.animateChannels(zone.channels, _animationParams.value)
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun animateChannels(channels: List<Int>) {
        viewModelScope.launch {
            val result = glyphRepository.animateChannels(channels, _animationParams.value)
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun updateAnimationParams(params: AnimationParams) {
        _animationParams.value = params
    }

    fun updateProgress(progress: Int) {
        _progressValue.value = progress
        viewModelScope.launch {
            val result = glyphRepository.displayProgress(progress, reverse = false)
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun updateProgressForZone(zone: GlyphZone, progress: Int) {
        when (zone) {
            GlyphZone.A -> _progressZoneA.value = progress
            GlyphZone.B -> _progressZoneB.value = progress
            GlyphZone.C -> {
                updateProgress(progress)
                return
            }
        }

        val channelCount = zone.channels.size
        val channelsToActivate = ((progress.toDouble() * channelCount / 100.0).toInt())
            .coerceIn(0, channelCount)
        val channels = zone.channels.take(channelsToActivate)

        viewModelScope.launch {
            val currentState = (uiState.value as? ControlsUiState.Success)?.glyphState
            val otherChannels = currentState?.activeChannels?.filter { it !in zone.channels } ?: emptyList()
            val newActive = otherChannels + channels

            val result = if (newActive.isEmpty()) {
                glyphRepository.turnOff()
            } else {
                glyphRepository.setChannels(newActive)
            }
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun turnOff() {
        viewModelScope.launch {
            val result = glyphRepository.turnOff()
            if (result is SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun clearError() {
        _errorState.value = ErrorState.None
    }
}

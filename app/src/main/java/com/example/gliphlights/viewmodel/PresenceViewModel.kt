package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.presets.GlyphPreset
import com.example.gliphlights.presets.PresencePatterns
import com.example.gliphlights.presets.PresenceStatus
import com.example.gliphlights.presets.PresetPlayer
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.PresetRepository
import com.example.gliphlights.sdk.GlyphClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PresenceUiState(
    val statuses: List<PresenceStatus> = PresenceStatus.entries,
    val active: PresenceStatus? = null,
    val customPads: List<GlyphPreset> = emptyList(),
    val statusMessage: String? = null
)

@HiltViewModel
class PresenceViewModel @Inject constructor(
    private val presetPlayer: PresetPlayer,
    private val glyphRepository: GlyphRepository,
    private val presetRepository: PresetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresenceUiState())
    val uiState: StateFlow<PresenceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            presetRepository.presets.collect { list ->
                _uiState.update {
                    it.copy(customPads = list.filter { p -> p.pinned }.take(4))
                }
            }
        }
    }

    fun selectStatus(status: PresenceStatus) {
        viewModelScope.launch {
            val preset = PresencePatterns.presetFor(status)
            _uiState.update { it.copy(active = status) }
            when (
                val result = presetPlayer.playOnce(
                    preset,
                    viewModelScope,
                    client = GlyphClient.PRESENCE,
                    intensity = 1f,
                    tempo = 1f,
                    loop = true
                )
            ) {
                is SdkResult.Error -> _uiState.update {
                    it.copy(statusMessage = result.message, active = null)
                }
                is SdkResult.Success -> _uiState.update {
                    it.copy(statusMessage = "${status.label} beacon on")
                }
            }
        }
    }

    fun playCustom(preset: GlyphPreset) {
        viewModelScope.launch {
            _uiState.update { it.copy(active = null) }
            when (
                val result = presetPlayer.playOnce(
                    preset,
                    viewModelScope,
                    client = GlyphClient.PRESENCE,
                    loop = true
                )
            ) {
                is SdkResult.Error -> _uiState.update { it.copy(statusMessage = result.message) }
                is SdkResult.Success -> _uiState.update {
                    it.copy(statusMessage = "Playing ${preset.name}")
                }
            }
        }
    }

    fun saveActiveAsPreset(name: String) {
        val status = _uiState.value.active ?: return
        viewModelScope.launch {
            val built = PresencePatterns.presetFor(status)
            presetRepository.savePath(
                name.ifBlank { "Presence ${status.label}" },
                built.pathNodes,
                built.pathSettings
            )
            _uiState.update { it.copy(statusMessage = "Saved presence preset") }
        }
    }

    fun stop() {
        viewModelScope.launch {
            presetPlayer.stop(GlyphClient.PRESENCE)
            if (glyphRepository.isSessionActive.value) glyphRepository.turnOff()
            _uiState.update { it.copy(active = null, statusMessage = "Beacon off") }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        presetPlayer.stop(GlyphClient.PRESENCE)
        super.onCleared()
    }
}

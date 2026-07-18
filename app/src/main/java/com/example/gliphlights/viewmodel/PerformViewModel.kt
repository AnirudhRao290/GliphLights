package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.presets.GlyphPreset
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

data class PerformUiState(
    val pads: List<GlyphPreset> = emptyList(),
    val activePadId: String? = null,
    val statusMessage: String? = null
)

@HiltViewModel
class PerformViewModel @Inject constructor(
    private val presetRepository: PresetRepository,
    private val presetPlayer: PresetPlayer,
    private val glyphRepository: GlyphRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerformUiState())
    val uiState: StateFlow<PerformUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            presetRepository.presets.collect { list ->
                val pinned = list.filter { it.pinned }.take(8)
                _uiState.update { it.copy(pads = pinned.ifEmpty { list.take(4) }) }
            }
        }
    }

    fun triggerPad(id: String, intensity: Float, tempo: Float) {
        val pad = _uiState.value.pads.find { it.id == id } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(activePadId = id) }
            when (
                val result = presetPlayer.playOnce(
                    pad,
                    viewModelScope,
                    client = GlyphClient.PERFORM,
                    intensity = intensity,
                    tempo = tempo,
                    loop = true
                )
            ) {
                is SdkResult.Error -> _uiState.update { it.copy(statusMessage = result.message) }
                is SdkResult.Success -> Unit
            }
        }
    }

    fun panicOff() {
        viewModelScope.launch {
            presetPlayer.stop(GlyphClient.PERFORM)
            if (glyphRepository.isSessionActive.value) glyphRepository.turnOff()
            _uiState.update { it.copy(activePadId = null, statusMessage = "All off") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        presetPlayer.stop(GlyphClient.PERFORM)
    }
}

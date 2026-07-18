package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.SettingsRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DoughnutTourUiState(
    val step: Int = 0,
    val headline: String = "Meet your Glyph ring",
    val body: String = "We'll light each arc so you can feel how zones map to the doughnut.",
    val highlightLabel: String = "A",
    val stepProgress: Float = 0f,
    val busy: Boolean = false,
    val isLast: Boolean = false,
    val showPathNudge: Boolean = false,
    val finished: Boolean = false
)

@HiltViewModel
class DoughnutTourViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionArbiter: GlyphSessionArbiter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoughnutTourUiState())
    val uiState: StateFlow<DoughnutTourUiState> = _uiState.asStateFlow()

    private var token: String? = null
    private val steps = listOf(
        Triple("Arc A", "Right arc — paint and path strokes often start here.", GlyphZone.A),
        Triple("Arc B", "Short bottom bridge connecting A to C.", GlyphZone.B),
        Triple("Arc C", "Long left/top arc — progress HUD and ambient breathe live here.", GlyphZone.C)
    )

    fun startTour() {
        viewModelScope.launch {
            when (val acquired = sessionArbiter.acquire(GlyphClient.EDITOR, UUID.randomUUID().toString())) {
                is AcquireResult.Granted -> token = acquired.token
                is AcquireResult.Denied -> {
                    _uiState.update {
                        it.copy(body = acquired.reason, busy = false)
                    }
                    return@launch
                }
            }
            if (!glyphRepository.isSessionActive.value) {
                if (!glyphRepository.isConnected.value) {
                    glyphRepository.initialize()
                    glyphRepository.register()
                }
                glyphRepository.openSession()
            }
            applyStep(0)
        }
    }

    fun nextStep() {
        val next = _uiState.value.step + 1
        if (next >= steps.size) {
            _uiState.update {
                it.copy(
                    showPathNudge = true,
                    isLast = true,
                    headline = "Try Path Builder",
                    body = "Take 30 seconds — draw one path on the doughnut and play it back.",
                    highlightLabel = "→",
                    stepProgress = 1f
                )
            }
            viewModelScope.launch { glyphRepository.turnOff() }
        } else {
            applyStep(next)
        }
    }

    fun complete() {
        viewModelScope.launch {
            settingsRepository.setTourCompleted(true)
            release()
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun skip() {
        viewModelScope.launch {
            settingsRepository.setTourCompleted(true)
            release()
            if (glyphRepository.isSessionActive.value) glyphRepository.turnOff()
            _uiState.update { it.copy(finished = true) }
        }
    }

    private fun applyStep(index: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val (title, body, zone) = steps[index]
            glyphRepository.setChannels(zone.channels)
            _uiState.update {
                it.copy(
                    step = index,
                    headline = title,
                    body = body,
                    highlightLabel = zone.name,
                    stepProgress = (index + 1f) / (steps.size + 1f),
                    busy = false,
                    isLast = index == steps.lastIndex
                )
            }
            delay(400)
        }
    }

    private suspend fun release() {
        val t = token
        token = null
        if (t != null) sessionArbiter.release(GlyphClient.EDITOR, t)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { release() }
    }
}

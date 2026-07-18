package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.editor.model.GlyphRegion
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class GlyphClockUiState(
    val isRunning: Boolean = false,
    val use24h: Boolean = true,
    val dimNight: Boolean = true,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
    val previewChannels: Set<Int> = emptySet(),
    val statusMessage: String? = null
)

@HiltViewModel
class GlyphClockViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val sessionArbiter: GlyphSessionArbiter
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlyphClockUiState())
    val uiState: StateFlow<GlyphClockUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var ownershipToken: String? = null

    init {
        viewModelScope.launch {
            sessionArbiter.preemptEvents.collect { event ->
                if (event.victim == GlyphClient.CLOCK && _uiState.value.isRunning) {
                    ownershipToken = null
                    tickJob?.cancel()
                    _uiState.update {
                        it.copy(isRunning = false, statusMessage = event.message)
                    }
                }
            }
        }
    }

    fun toggle24h() {
        _uiState.update { it.copy(use24h = !it.use24h) }
    }

    fun toggleDimNight() {
        _uiState.update { it.copy(dimNight = !it.dimNight) }
    }

    fun start() {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            when (val acquired = sessionArbiter.acquire(GlyphClient.CLOCK)) {
                is AcquireResult.Granted -> ownershipToken = acquired.token
                is AcquireResult.Denied -> {
                    _uiState.update { it.copy(statusMessage = acquired.reason) }
                    return@launch
                }
            }
            ensureSession()
            _uiState.update { it.copy(isRunning = true, statusMessage = null) }
            tickJob?.cancel()
            tickJob = viewModelScope.launch {
                while (isActive) {
                    val cal = Calendar.getInstance()
                    val h24 = cal.get(Calendar.HOUR_OF_DAY)
                    val m = cal.get(Calendar.MINUTE)
                    val s = cal.get(Calendar.SECOND)
                    val channels = channelsForTime(h24, m, s, _uiState.value.dimNight)
                    _uiState.update {
                        it.copy(
                            hour = if (it.use24h) h24 else ((h24 + 11) % 12) + 1,
                            minute = m,
                            second = s,
                            previewChannels = channels
                        )
                    }
                    if (sessionArbiter.canWrite(GlyphClient.CLOCK, ownershipToken)) {
                        if (channels.isEmpty()) glyphRepository.turnOff()
                        else glyphRepository.setChannels(channels.toList())
                    }
                    delay(1000)
                }
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        _uiState.update { it.copy(isRunning = false) }
        viewModelScope.launch {
            if (sessionArbiter.canWrite(GlyphClient.CLOCK, ownershipToken)) {
                glyphRepository.turnOff()
            }
            releaseOwnership()
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    /**
     * Hours → C-arc fill (0–19), minutes → A arc position, seconds → B pulse.
     */
    fun channelsForTime(hour24: Int, minute: Int, second: Int, dimNight: Boolean): Set<Int> {
        val night = dimNight && (hour24 < 7 || hour24 >= 23)
        val channels = LinkedHashSet<Int>()

        val hourProgress = ((hour24 % 12) / 12f * GlyphRegion.C.nodeCount)
            .toInt()
            .coerceIn(0, GlyphRegion.C.nodeCount)
        for (i in 0 until hourProgress.coerceAtLeast(1)) {
            channels.add(GlyphRegion.C.channelStart + i)
        }

        val minuteIdx = ((minute / 60f) * (GlyphRegion.A.nodeCount - 1))
            .toInt()
            .coerceIn(0, GlyphRegion.A.nodeCount - 1)
        channels.add(GlyphRegion.A.channelStart + minuteIdx)
        if (minuteIdx > 0) channels.add(GlyphRegion.A.channelStart + minuteIdx - 1)

        val bIdx = (second / 12).coerceIn(0, GlyphRegion.B.nodeCount - 1)
        if (!night || second % 2 == 0) {
            channels.add(GlyphRegion.B.channelStart + bIdx)
        }

        if (night) {
            // Keep a thinner set overnight
            return channels.filterIndexed { index, _ -> index % 2 == 0 }.toSet()
        }
        return channels
    }

    private suspend fun ensureSession() {
        if (glyphRepository.isSessionActive.value) return
        if (!glyphRepository.isConnected.value) {
            glyphRepository.initialize()
            glyphRepository.register()
        }
        glyphRepository.openSession()
    }

    private fun releaseOwnership() {
        val token = ownershipToken ?: return
        ownershipToken = null
        viewModelScope.launch {
            sessionArbiter.release(GlyphClient.CLOCK, token)
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        releaseOwnership()
        super.onCleared()
    }
}

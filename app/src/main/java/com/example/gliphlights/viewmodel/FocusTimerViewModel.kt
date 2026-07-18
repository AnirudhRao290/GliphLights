package com.example.gliphlights.viewmodel

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.PresetRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusTimerUiState(
    val durationSec: Int = 25 * 60,
    val remainingSec: Int = 25 * 60,
    val isRunning: Boolean = false,
    val progressPercent: Int = 0,
    val statusMessage: String? = null,
    val completed: Boolean = false
)

@HiltViewModel
class FocusTimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val glyphRepository: GlyphRepository,
    private val sessionArbiter: GlyphSessionArbiter,
    private val presetRepository: PresetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusTimerUiState())
    val uiState: StateFlow<FocusTimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var ownershipToken: String? = null

    init {
        viewModelScope.launch {
            sessionArbiter.preemptEvents.collect { event ->
                if (event.victim == GlyphClient.FOCUS && _uiState.value.isRunning) {
                    ownershipToken = null
                    tickJob?.cancel()
                    _uiState.update {
                        it.copy(isRunning = false, statusMessage = event.message)
                    }
                }
            }
        }
    }

    fun setPresetMinutes(minutes: Int) {
        if (_uiState.value.isRunning) return
        val sec = (minutes * 60).coerceIn(60, 90 * 60)
        _uiState.update {
            it.copy(
                durationSec = sec,
                remainingSec = sec,
                progressPercent = 0,
                completed = false
            )
        }
    }

    fun setCustomMinutes(minutes: Float) {
        setPresetMinutes(minutes.toInt().coerceIn(1, 90))
    }

    fun start() {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            when (val acquired = sessionArbiter.acquire(GlyphClient.FOCUS)) {
                is AcquireResult.Granted -> ownershipToken = acquired.token
                is AcquireResult.Denied -> {
                    _uiState.update { it.copy(statusMessage = acquired.reason) }
                    return@launch
                }
            }
            ensureSession()
            _uiState.update { it.copy(isRunning = true, completed = false, statusMessage = null) }
            tickJob?.cancel()
            tickJob = viewModelScope.launch {
                while (isActive && _uiState.value.remainingSec > 0) {
                    delay(1000)
                    val next = (_uiState.value.remainingSec - 1).coerceAtLeast(0)
                    val dur = _uiState.value.durationSec.coerceAtLeast(1)
                    val pct = (((dur - next).toFloat() / dur) * 100f).toInt().coerceIn(0, 100)
                    _uiState.update {
                        it.copy(remainingSec = next, progressPercent = pct)
                    }
                    if (sessionArbiter.canWrite(GlyphClient.FOCUS, ownershipToken)) {
                        glyphRepository.displayProgress(pct, reverse = false)
                    }
                }
                if (_uiState.value.remainingSec <= 0) {
                    onComplete()
                }
            }
        }
    }

    fun pause() {
        tickJob?.cancel()
        tickJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    fun reset() {
        tickJob?.cancel()
        tickJob = null
        val dur = _uiState.value.durationSec
        _uiState.update {
            it.copy(isRunning = false, remainingSec = dur, progressPercent = 0, completed = false)
        }
        viewModelScope.launch {
            if (sessionArbiter.canWrite(GlyphClient.FOCUS, ownershipToken)) {
                glyphRepository.turnOff()
            }
            releaseOwnership()
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun saveCompletionFrame() {
        viewModelScope.launch {
            val channels = glyphRepository.glyphState.value.activeChannels
            if (channels.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "Nothing lit to save") }
                return@launch
            }
            val name = "Focus ${System.currentTimeMillis() % 10000}"
            presetRepository.saveEditorFrame(name, channels)
            _uiState.update { it.copy(statusMessage = "Saved “$name”") }
        }
    }

    private suspend fun onComplete() {
        _uiState.update {
            it.copy(isRunning = false, progressPercent = 100, completed = true, statusMessage = "Focus complete")
        }
        vibrate()
        if (sessionArbiter.canWrite(GlyphClient.FOCUS, ownershipToken)) {
            glyphRepository.displayProgress(100, reverse = false)
            delay(800)
            glyphRepository.turnOff()
        }
        releaseOwnership()
    }

    private suspend fun ensureSession() {
        if (glyphRepository.isSessionActive.value) return
        if (!glyphRepository.isConnected.value) {
            glyphRepository.initialize()
            glyphRepository.register()
        }
        glyphRepository.openSession()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun releaseOwnership() {
        val token = ownershipToken ?: return
        ownershipToken = null
        viewModelScope.launch {
            sessionArbiter.release(GlyphClient.FOCUS, token)
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        releaseOwnership()
        super.onCleared()
    }
}

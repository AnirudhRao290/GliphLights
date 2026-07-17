package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.ErrorState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphUiState
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
class DashboardViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GlyphUiState>(GlyphUiState.Loading)
    val uiState: StateFlow<GlyphUiState> = _uiState.asStateFlow()

    private val _errorState = MutableStateFlow<ErrorState>(ErrorState.None)
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    init {
        initializeSdk()
        observeGlyphState()
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
                _uiState.value = GlyphUiState.Success(
                    glyphState = state,
                    deviceInfo = device
                )
            }
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
            val result = glyphRepository.animateAll()
            if (result is com.example.gliphlights.models.SdkResult.Error) {
                _errorState.value = ErrorState.RuntimeError(result.message, result.exception)
            }
        }
    }

    fun turnOff() {
        viewModelScope.launch {
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

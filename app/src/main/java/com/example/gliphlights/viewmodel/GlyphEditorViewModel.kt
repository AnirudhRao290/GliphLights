package com.example.gliphlights.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.editor.gesture.GestureEvent
import com.example.gliphlights.editor.model.AnimationModel
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.editor.sdk.GlyphSdkRenderer
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.models.SdkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlyphEditorUiState(
    val isSessionActive: Boolean = false,
    val activeChannels: Set<Int> = emptySet(),
    val activeCount: Int = 0,
    val layout: GlyphNodeLayout? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GlyphEditorViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val sdkRenderer: GlyphSdkRenderer
) : ViewModel() {

    companion object {
        private const val TAG = "GlyphEditorVM"
    }

    private val _uiState = MutableStateFlow(GlyphEditorUiState())
    val uiState: StateFlow<GlyphEditorUiState> = _uiState.asStateFlow()

    private var nodeStates = mutableMapOf<Int, Boolean>()
    private var currentModel = AnimationModel.empty()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            glyphRepository.isSessionActive.collect { active ->
                _uiState.update { it.copy(isSessionActive = active) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onLayoutCreated(layout: GlyphNodeLayout) {
        _uiState.update { it.copy(layout = layout) }
    }

    fun handleGestureEvent(event: GestureEvent) {
        val layout = _uiState.value.layout ?: return
        when (event) {
            is GestureEvent.Tap -> {
                val node = layout.findNearestNode(event.position)
                if (node != null) {
                    toggleNode(node)
                }
            }
            is GestureEvent.DragEnter -> {
                val node = layout.getNode(event.nodeId)
                if (node != null) {
                    toggleNode(node)
                }
            }
            else -> {}
        }
    }

    private fun toggleNode(node: GlyphNode) {
        val current = nodeStates[node.sdkIndex] ?: false
        nodeStates[node.sdkIndex] = !current
        updateModel()
    }

    private fun updateModel() {
        currentModel = AnimationModel.fromNodeStates(nodeStates.toMap())
        _uiState.update {
            it.copy(
                activeChannels = currentModel.activeChannels,
                activeCount = currentModel.activeCount
            )
        }
        if (_uiState.value.isSessionActive) {
            sdkRenderer.render(currentModel)
        }
    }

    fun startRenderer() {
        sdkRenderer.start()
    }

    fun startSession() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (glyphRepository.isSessionActive.first()) {
                sdkRenderer.start()
                if (currentModel.activeCount > 0) {
                    sdkRenderer.render(currentModel)
                }
                _uiState.update { it.copy(isLoading = false) }
                Log.d(TAG, "Session already active — renderer started")
                return@launch
            }

            // After Stop the SDK stays connected (SESSION_CLOSED) — only reopen.
            if (glyphRepository.isConnected.first()) {
                val sessionResult = glyphRepository.openSession()
                if (sessionResult is SdkResult.Error) {
                    Log.e(TAG, "Re-open session failed: ${sessionResult.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sessionResult.message ?: "Failed to open Glyph session"
                        )
                    }
                    return@launch
                }
                sdkRenderer.start()
                if (currentModel.activeCount > 0) {
                    sdkRenderer.render(currentModel)
                }
                _uiState.update { it.copy(isLoading = false) }
                Log.d(TAG, "Session re-opened successfully")
                return@launch
            }

            val initResult = glyphRepository.initialize()
            if (initResult is SdkResult.Error) {
                Log.e(TAG, "Init failed: ${initResult.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = initResult.message ?: "Failed to initialize Glyph SDK"
                    )
                }
                return@launch
            }

            val registerResult = glyphRepository.register()
            if (registerResult is SdkResult.Error) {
                Log.e(TAG, "Register failed: ${registerResult.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = registerResult.message ?: "Failed to register with Glyph SDK"
                    )
                }
                return@launch
            }

            val sessionResult = glyphRepository.openSession()
            if (sessionResult is SdkResult.Error) {
                Log.e(TAG, "Open session failed: ${sessionResult.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = sessionResult.message ?: "Failed to open Glyph session"
                    )
                }
                return@launch
            }

            sdkRenderer.start()
            _uiState.update { it.copy(isLoading = false) }
            Log.d(TAG, "Session started successfully")
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            sdkRenderer.stop()
            glyphRepository.turnOff()
            glyphRepository.closeSession()
            _uiState.update { it.copy(isLoading = false, isSessionActive = false) }
        }
    }

    fun clearAll() {
        nodeStates.clear()
        updateModel()
        if (_uiState.value.isSessionActive) {
            viewModelScope.launch {
                glyphRepository.turnOff()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sdkRenderer.destroy()
    }
}

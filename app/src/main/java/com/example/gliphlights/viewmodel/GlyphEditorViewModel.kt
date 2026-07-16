package com.example.gliphlights.viewmodel

import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlyphEditorUiState(
    val isSessionActive: Boolean = false,
    val activeChannels: Set<Int> = emptySet(),
    val activeCount: Int = 0,
    val layout: GlyphNodeLayout = GlyphNodeLayout(400f, 600f)
)

@HiltViewModel
class GlyphEditorViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val sdkRenderer: GlyphSdkRenderer
) : ViewModel() {

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

    fun handleGestureEvent(event: GestureEvent) {
        val layout = _uiState.value.layout
        when (event) {
            is GestureEvent.Tap -> {
                val node = layout.findNearestNode(event.position, maxDistance = 50f)
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
        sdkRenderer.render(currentModel)
    }

    fun startSession() {
        viewModelScope.launch {
            val initResult = glyphRepository.initialize()
            if (initResult is SdkResult.Error) return@launch

            val registerResult = glyphRepository.register()
            if (registerResult is SdkResult.Error) return@launch

            val sessionResult = glyphRepository.openSession()
            if (sessionResult is SdkResult.Error) return@launch

            sdkRenderer.start()
        }
    }

    fun stopSession() {
        sdkRenderer.stop()
        viewModelScope.launch {
            glyphRepository.turnOff()
            glyphRepository.closeSession()
        }
    }

    fun clearAll() {
        nodeStates.clear()
        updateModel()
        viewModelScope.launch {
            glyphRepository.turnOff()
        }
    }

    fun sendToDevice() {
        sdkRenderer.render(currentModel)
    }

    override fun onCleared() {
        super.onCleared()
        sdkRenderer.destroy()
    }
}

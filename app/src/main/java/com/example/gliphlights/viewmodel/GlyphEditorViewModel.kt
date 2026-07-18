package com.example.gliphlights.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.editor.gesture.GestureEvent
import com.example.gliphlights.editor.model.AnimationModel
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.editor.sdk.GlyphSdkRenderer
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.PresetRepository
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EditorTool(val label: String) {
    PAINT("Paint"),
    ERASE("Erase"),
    FILL("Fill"),
    PREVIEW("Preview")
}

data class GlyphEditorUiState(
    val isSessionActive: Boolean = false,
    val activeChannels: Set<Int> = emptySet(),
    val activeCount: Int = 0,
    val layout: GlyphNodeLayout? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val tool: EditorTool = EditorTool.PAINT,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val zoomScale: Float = 1f,
    val showSettings: Boolean = false,
    val showSaveDialog: Boolean = false,
    val glowIntensity: Float = 1f,
    val hapticsEnabled: Boolean = true,
    val previewPulse: Boolean = false
)

@HiltViewModel
class GlyphEditorViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val sdkRenderer: GlyphSdkRenderer,
    private val sessionArbiter: GlyphSessionArbiter,
    private val presetRepository: PresetRepository
) : ViewModel() {

    companion object {
        private const val TAG = "GlyphEditorVM"
        private const val MAX_HISTORY = 48
    }

    private val _uiState = MutableStateFlow(GlyphEditorUiState())
    val uiState: StateFlow<GlyphEditorUiState> = _uiState.asStateFlow()

    private var nodeStates = mutableMapOf<Int, Boolean>()
    private var currentModel = AnimationModel.empty()
    private val undoStack = ArrayDeque<Map<Int, Boolean>>()
    private val redoStack = ArrayDeque<Map<Int, Boolean>>()
    private var strokeStarted = false

    init {
        observeSession()
        observePreempt()
    }

    private fun observeSession() {
        viewModelScope.launch {
            glyphRepository.isSessionActive.collect { active ->
                _uiState.update { it.copy(isSessionActive = active) }
            }
        }
    }

    private fun observePreempt() {
        viewModelScope.launch {
            sessionArbiter.preemptEvents.collect { event ->
                if (event.victim == GlyphClient.EDITOR) {
                    _uiState.update {
                        it.copy(errorMessage = event.message)
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun showSaveDialog(show: Boolean) {
        _uiState.update { it.copy(showSaveDialog = show) }
    }

    fun saveFrame(name: String) {
        val channels = _uiState.value.activeChannels
        if (channels.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Paint some nodes first") }
            return
        }
        viewModelScope.launch {
            val saved = presetRepository.saveEditorFrame(name, channels)
            _uiState.update {
                it.copy(
                    showSaveDialog = false,
                    statusMessage = "Saved “${saved.name}”"
                )
            }
        }
    }

    fun onLayoutCreated(layout: GlyphNodeLayout) {
        _uiState.update { it.copy(layout = layout) }
    }

    fun setTool(tool: EditorTool) {
        _uiState.update {
            it.copy(
                tool = tool,
                previewPulse = tool == EditorTool.PREVIEW
            )
        }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun updateGlowIntensity(value: Float) {
        _uiState.update { it.copy(glowIntensity = value.coerceIn(0.4f, 1.5f)) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(hapticsEnabled = enabled) }
    }

    fun zoomIn() {
        _uiState.update { it.copy(zoomScale = (it.zoomScale * 1.2f).coerceIn(0.5f, 3f)) }
    }

    fun zoomOut() {
        _uiState.update { it.copy(zoomScale = (it.zoomScale / 1.2f).coerceIn(0.5f, 3f)) }
    }

    fun resetZoom() {
        _uiState.update { it.copy(zoomScale = 1f) }
    }

    fun onZoomChanged(scale: Float) {
        _uiState.update { it.copy(zoomScale = scale.coerceIn(0.5f, 3f)) }
    }

    fun handleGestureEvent(event: GestureEvent) {
        val layout = _uiState.value.layout ?: return
        val tool = _uiState.value.tool
        if (tool == EditorTool.PREVIEW) return

        when (event) {
            is GestureEvent.DragStart -> {
                strokeStarted = false
            }
            is GestureEvent.Tap -> {
                val node = layout.findNearestNode(event.position) ?: return
                beginStrokeIfNeeded()
                applyTool(node, layout)
            }
            is GestureEvent.DragEnter -> {
                val node = layout.getNode(event.nodeId) ?: return
                beginStrokeIfNeeded()
                applyTool(node, layout)
            }
            is GestureEvent.DragEnd -> {
                strokeStarted = false
            }
            else -> {}
        }
    }

    private fun beginStrokeIfNeeded() {
        if (strokeStarted) return
        strokeStarted = true
        pushHistory()
    }

    private fun applyTool(node: GlyphNode, layout: GlyphNodeLayout) {
        when (_uiState.value.tool) {
            EditorTool.PAINT -> setNode(node.sdkIndex, true)
            EditorTool.ERASE -> setNode(node.sdkIndex, false)
            EditorTool.FILL -> fillRegion(node, layout)
            EditorTool.PREVIEW -> {}
        }
        updateModel()
    }

    private fun setNode(sdkIndex: Int, on: Boolean) {
        nodeStates[sdkIndex] = on
        if (!on) nodeStates.remove(sdkIndex)
    }

    private fun fillRegion(node: GlyphNode, layout: GlyphNodeLayout) {
        layout.getNodesByRegion(node.region).forEach { n ->
            nodeStates[n.sdkIndex] = true
        }
    }

    private fun pushHistory() {
        undoStack.addLast(nodeStates.toMap())
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        publishHistoryFlags()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(nodeStates.toMap())
        nodeStates = undoStack.removeLast().toMutableMap()
        updateModel()
        publishHistoryFlags()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(nodeStates.toMap())
        nodeStates = redoStack.removeLast().toMutableMap()
        updateModel()
        publishHistoryFlags()
    }

    private fun publishHistoryFlags() {
        _uiState.update {
            it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
        }
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
            if (glyphRepository.isSessionActive.value) {
                glyphRepository.turnOff()
                glyphRepository.closeSession()
            }
            _uiState.update { it.copy(isLoading = false, isSessionActive = false) }
        }
    }

    fun clearAll() {
        pushHistory()
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

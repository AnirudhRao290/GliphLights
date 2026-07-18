package com.example.gliphlights.pathbuilder

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.pathbuilder.model.AnimationModel
import com.example.gliphlights.pathbuilder.model.EngineSnapshot
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.pathbuilder.model.SavedSequence
import com.example.gliphlights.repository.GlyphRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PathBuilderUiState(
    val drawMode: Boolean = true,
    val settings: PathSettings = PathSettings(),
    val layout: GlyphNodeLayout? = null,
    val liveTrail: List<Offset> = emptyList(),
    val pathNodes: List<PathNode> = emptyList(),
    val enteredNodeId: String? = null,
    val animation: AnimationModel = AnimationModel.empty(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showSettings: Boolean = false,
    val showLibrary: Boolean = false,
    val showAdvancedOps: Boolean = false,
    val showSaveDialog: Boolean = false,
    val nodeCount: Int = 0,
    val hardwareEnabled: Boolean = false,
    val hardwareBusy: Boolean = false,
    val presets: List<SavedSequence> = PredefinedSequences.all(),
    val savedSequences: List<SavedSequence> = emptyList(),
    val statusMessage: String? = null,
    val loadedSequenceName: String? = null
)

@HiltViewModel
class PathBuilderViewModel @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val sequenceRepository: SequenceRepository
) : ViewModel() {

    private val recorder = PathRecorder()
    private val optimizer = PathOptimizer()
    private val generator = AnimationGenerator()
    private val engine = AnimationEngine(viewModelScope)
    private val hardwarePlayer = PathHardwarePlayer(glyphRepository, viewModelScope)

    private var converter: PathConverter? = null
    private val liveTrailBuffer = ArrayList<Offset>(512)
    private val pathHistory = ArrayDeque<List<PathNode>>(32)
    private val pathRedo = ArrayDeque<List<PathNode>>(32)

    private val _uiState = MutableStateFlow(PathBuilderUiState())
    val uiState: StateFlow<PathBuilderUiState> = _uiState.asStateFlow()

    val engineSnapshot: StateFlow<EngineSnapshot> = engine.snapshot

    init {
        viewModelScope.launch {
            sequenceRepository.savedSequences.collect { saved ->
                _uiState.update { it.copy(savedSequences = saved) }
            }
        }
    }

    fun onLayoutCreated(layout: GlyphNodeLayout) {
        converter = PathConverter(layout)
        _uiState.update { it.copy(layout = layout) }
    }

    fun setDrawMode(enabled: Boolean) {
        _uiState.update { it.copy(drawMode = enabled) }
        if (!enabled) {
            _uiState.update { it.copy(enteredNodeId = null) }
        }
    }

    fun updateSettings(settings: PathSettings) {
        _uiState.update { it.copy(settings = settings) }
        rebuildAnimation(_uiState.value.pathNodes, settings)
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun toggleAdvancedOps() {
        _uiState.update { it.copy(showAdvancedOps = !it.showAdvancedOps) }
    }

    fun toggleLibrary() {
        _uiState.update { it.copy(showLibrary = !it.showLibrary) }
    }

    fun showSaveDialog(show: Boolean) {
        _uiState.update { it.copy(showSaveDialog = show) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun onStrokeStart() {
        recorder.beginStroke()
        liveTrailBuffer.clear()
        _uiState.update {
            it.copy(
                liveTrail = emptyList(),
                enteredNodeId = null,
                canUndo = recorder.canUndo(),
                canRedo = recorder.canRedo(),
                loadedSequenceName = null
            )
        }
        engine.pause()
    }

    fun onSample(x: Float, y: Float, tNanos: Long) {
        val density = _uiState.value.settings.samplingDensityPx
        if (!recorder.appendSample(x, y, tNanos, density)) return
        liveTrailBuffer.add(Offset(x, y))
        if (liveTrailBuffer.size > 2000) {
            liveTrailBuffer.removeAt(0)
        }
        _uiState.update { it.copy(liveTrail = ArrayList(liveTrailBuffer)) }
    }

    fun onNodeEntered(node: PathNode) {
        _uiState.update { it.copy(enteredNodeId = node.nodeId) }
    }

    fun onStrokeEnd() {
        recorder.endStroke()
        val layout = _uiState.value.layout ?: return
        val conv = converter ?: PathConverter(layout)
        val settings = _uiState.value.settings
        val raw = conv.convertSamples(recorder.allSamples())
        val optimized = optimizer.optimize(raw, settings)
        pushPathHistory(optimized)
        rebuildAnimation(optimized, settings)
        _uiState.update {
            it.copy(
                pathNodes = optimized,
                nodeCount = optimized.size,
                enteredNodeId = null,
                canUndo = pathHistory.isNotEmpty(),
                canRedo = pathRedo.isNotEmpty()
            )
        }
    }

    // --- Path operations ---

    fun undo() {
        if (pathHistory.isEmpty()) return
        pathRedo.addLast(_uiState.value.pathNodes)
        applyPath(pathHistory.removeLast())
    }

    fun redo() {
        if (pathRedo.isEmpty()) return
        pathHistory.addLast(_uiState.value.pathNodes)
        applyPath(pathRedo.removeLast())
    }

    fun clearPath() {
        pushPathHistory(emptyList())
        recorder.clear()
        liveTrailBuffer.clear()
        applyPath(emptyList())
        _uiState.update { it.copy(liveTrail = emptyList(), loadedSequenceName = null) }
        engine.stop()
        if (_uiState.value.hardwareEnabled) {
            hardwarePlayer.stopSync(turnOff = true)
            _uiState.update { it.copy(hardwareEnabled = false) }
        }
    }

    fun reversePath() = mutatePath { PathOperations.reverse(it) }
    fun mirrorPath() {
        val layout = _uiState.value.layout ?: return
        mutatePath { PathOperations.mirror(it, layout) }
    }
    fun closeLoop() = mutatePath { PathOperations.closeLoop(it) }
    fun duplicatePath() = mutatePath { PathOperations.duplicate(it) }
    fun simplifyPath() = mutatePath { PathOperations.simplify(it, keepEvery = 2) }
    fun smoothPath() = mutatePath {
        PathOperations.smooth(it, _uiState.value.settings.smoothingStrength.coerceAtLeast(0.4f))
    }
    fun trimPathEnds() = mutatePath {
        if (it.size <= 4) it else PathOperations.trim(it, 1, it.size - 1)
    }

    // --- Save / Load ---

    fun saveCurrentSequence(name: String) {
        val nodes = _uiState.value.pathNodes
        if (nodes.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Draw a path before saving") }
            return
        }
        viewModelScope.launch {
            val saved = sequenceRepository.save(name, nodes, _uiState.value.settings)
            _uiState.update {
                it.copy(
                    showSaveDialog = false,
                    loadedSequenceName = saved.name,
                    statusMessage = "Saved \"${saved.name}\""
                )
            }
        }
    }

    fun loadSequence(sequence: SavedSequence) {
        pushPathHistory(sequence.nodes)
        _uiState.update {
            it.copy(
                settings = sequence.settings,
                loadedSequenceName = sequence.name,
                showLibrary = false,
                drawMode = false,
                liveTrail = emptyList()
            )
        }
        applyPath(sequence.nodes)
        rebuildAnimation(sequence.nodes, sequence.settings)
        engine.restart()
        _uiState.update { it.copy(statusMessage = "Loaded \"${sequence.name}\"") }
    }

    fun deleteSavedSequence(id: String) {
        viewModelScope.launch {
            sequenceRepository.delete(id)
            _uiState.update { it.copy(statusMessage = "Sequence deleted") }
        }
    }

    // --- Transport ---

    fun play() {
        if (_uiState.value.hardwareEnabled) {
            hardwarePlayer.startSync(engine.snapshot)
        }
        engine.play()
    }

    fun pause() = engine.pause()

    fun restart() {
        if (_uiState.value.hardwareEnabled) {
            hardwarePlayer.startSync(engine.snapshot)
        }
        engine.restart()
    }

    fun seek(ms: Long) = engine.seek(ms)

    fun setPlaybackSpeed(speed: Float) {
        engine.setSpeed(speed)
        updateSettings(_uiState.value.settings.copy(animationSpeed = speed))
    }

    fun previewPath() {
        if (_uiState.value.animation.isEmpty) return
        // Software-only preview
        if (_uiState.value.hardwareEnabled) {
            stopHardware()
        }
        engine.restart()
    }

    /**
     * Play on device: open Glyph session and sync engine frames to hardware.
     */
    fun playOnGlyph() {
        if (_uiState.value.animation.isEmpty) {
            _uiState.update { it.copy(statusMessage = "No path to play") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(hardwareBusy = true) }
            when (val result = hardwarePlayer.ensureSession()) {
                is SdkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            hardwareBusy = false,
                            statusMessage = result.message ?: "Glyph session failed"
                        )
                    }
                }
                is SdkResult.Success -> {
                    hardwarePlayer.startSync(engine.snapshot)
                    _uiState.update {
                        it.copy(
                            hardwareEnabled = true,
                            hardwareBusy = false,
                            drawMode = false,
                            statusMessage = "Playing on Glyph lights"
                        )
                    }
                    engine.restart()
                }
            }
        }
    }

    fun stopHardware() {
        hardwarePlayer.stopSync(turnOff = true)
        engine.pause()
        _uiState.update {
            it.copy(hardwareEnabled = false, statusMessage = "Glyph playback stopped")
        }
    }

    override fun onCleared() {
        super.onCleared()
        hardwarePlayer.stopSync(turnOff = true)
        engine.release()
    }

    private fun mutatePath(transform: (List<PathNode>) -> List<PathNode>) {
        val next = transform(_uiState.value.pathNodes)
        pushPathHistory(next)
        applyPath(next)
    }

    private fun applyPath(path: List<PathNode>) {
        rebuildAnimation(path, _uiState.value.settings)
        _uiState.update {
            it.copy(
                pathNodes = path,
                nodeCount = path.size,
                canUndo = pathHistory.isNotEmpty(),
                canRedo = pathRedo.isNotEmpty()
            )
        }
    }

    private fun pushPathHistory(newPath: List<PathNode>) {
        pathHistory.addLast(_uiState.value.pathNodes)
        while (pathHistory.size > 32) pathHistory.removeFirst()
        pathRedo.clear()
    }

    private fun rebuildAnimation(path: List<PathNode>, settings: PathSettings) {
        val model = generator.generate(path, settings)
        engine.setModel(model)
        engine.setSpeed(settings.animationSpeed)
        _uiState.update { it.copy(animation = model) }
    }
}

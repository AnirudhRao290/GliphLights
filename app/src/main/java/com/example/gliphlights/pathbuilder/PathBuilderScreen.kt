package com.example.gliphlights.pathbuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.pathbuilder.model.EngineSnapshot
import com.example.gliphlights.pathbuilder.model.InterpolationMode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.pathbuilder.model.SavedSequence
import com.example.gliphlights.pathbuilder.render.PathBuilderMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathBuilderScreen(
    onBack: () -> Unit,
    viewModel: PathBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snapshot by viewModel.engineSnapshot.collectAsState()
    val view = LocalView.current
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val librarySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Path Builder", fontWeight = FontWeight.Bold)
                        uiState.loadedSequenceName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showSaveDialog(true) },
                        enabled = uiState.pathNodes.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save sequence")
                    }
                    IconButton(onClick = viewModel::toggleLibrary) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = "Library")
                    }
                    IconButton(onClick = viewModel::toggleSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PathBuilderMapView(
                    layout = uiState.layout,
                    nodeAlphas = snapshot.nodeAlphas,
                    liveTrail = uiState.liveTrail,
                    livePathNodes = uiState.pathNodes,
                    enteredNodeId = uiState.enteredNodeId,
                    drawMode = uiState.drawMode,
                    settings = uiState.settings,
                    onLayoutCreated = viewModel::onLayoutCreated,
                    onSample = viewModel::onSample,
                    onNodeEntered = viewModel::onNodeEntered,
                    onStrokeStart = viewModel::onStrokeStart,
                    onStrokeEnd = viewModel::onStrokeEnd,
                    modifier = Modifier.fillMaxSize(),
                    view = view
                )

                PathTimelineOverlay(
                    snapshot = snapshot,
                    nodeCount = uiState.nodeCount,
                    onSeek = viewModel::seek,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            PathPrimaryBar(
                drawMode = uiState.drawMode,
                isPlaying = snapshot.isPlaying,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                hasPath = uiState.pathNodes.isNotEmpty(),
                hardwareEnabled = uiState.hardwareEnabled,
                hardwareBusy = uiState.hardwareBusy,
                tapBpm = uiState.tapBpm,
                onDraw = { viewModel.setDrawMode(true) },
                onPreview = {
                    viewModel.setDrawMode(false)
                    viewModel.previewPath()
                },
                onPlayPause = {
                    viewModel.setDrawMode(false)
                    if (snapshot.isPlaying) viewModel.pause() else viewModel.play()
                },
                onRestart = viewModel::restart,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onAdvanced = viewModel::toggleAdvancedOps,
                onPlayOnGlyph = viewModel::playOnGlyph,
                onStopHardware = viewModel::stopHardware,
                onTapTempo = viewModel::tapTempoBeat
            )
        }
    }

    if (uiState.showAdvancedOps) {
        ModalBottomSheet(
            onDismissRequest = viewModel::toggleAdvancedOps,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PathAdvancedOpsSheet(
                hasPath = uiState.pathNodes.isNotEmpty(),
                onReverse = viewModel::reversePath,
                onMirror = viewModel::mirrorPath,
                onCloseLoop = viewModel::closeLoop,
                onDuplicate = viewModel::duplicatePath,
                onSimplify = viewModel::simplifyPath,
                onSmooth = viewModel::smoothPath,
                onTrim = viewModel::trimPathEnds,
                onClear = viewModel::clearPath,
                onClose = viewModel::toggleAdvancedOps
            )
        }
    }

    if (uiState.showSettings) {
        ModalBottomSheet(
            onDismissRequest = viewModel::toggleSettings,
            sheetState = settingsSheetState
        ) {
            PathSettingsSheet(
                settings = uiState.settings,
                onChange = viewModel::updateSettings,
                onClose = viewModel::toggleSettings
            )
        }
    }

    if (uiState.showLibrary) {
        ModalBottomSheet(
            onDismissRequest = viewModel::toggleLibrary,
            sheetState = librarySheetState
        ) {
            SequenceLibrarySheet(
                presets = uiState.presets,
                saved = uiState.savedSequences,
                onLoad = viewModel::loadSequence,
                onDelete = viewModel::deleteSavedSequence,
                onClose = viewModel::toggleLibrary
            )
        }
    }

    if (uiState.showSaveDialog) {
        SaveSequenceDialog(
            onDismiss = { viewModel.showSaveDialog(false) },
            onSave = viewModel::saveCurrentSequence
        )
    }
}

@Composable
private fun PathTimelineOverlay(
    snapshot: EngineSnapshot,
    nodeCount: Int,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "$nodeCount nodes · ${snapshot.playheadMs} / ${snapshot.totalDurationMs} ms",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (snapshot.totalDurationMs > 0) {
            Slider(
                value = snapshot.playheadMs.toFloat().coerceIn(0f, snapshot.totalDurationMs.toFloat()),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..snapshot.totalDurationMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PathPrimaryBar(
    drawMode: Boolean,
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    hasPath: Boolean,
    hardwareEnabled: Boolean,
    hardwareBusy: Boolean,
    tapBpm: Float?,
    onDraw: () -> Unit,
    onPreview: () -> Unit,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAdvanced: () -> Unit,
    onPlayOnGlyph: () -> Unit,
    onStopHardware: () -> Unit,
    onTapTempo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = drawMode, onClick = onDraw, label = { Text("Draw") })
            FilterChip(selected = !drawMode && !isPlaying, onClick = onPreview, label = { Text("Preview") })
            FilterChip(
                selected = isPlaying,
                onClick = onPlayPause,
                label = { Text(if (isPlaying) "Pause" else "Play") }
            )
            FilterChip(
                selected = tapBpm != null,
                onClick = onTapTempo,
                label = { Text(if (tapBpm != null) "${tapBpm.toInt()} BPM" else "Tap") }
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
            TextButton(onClick = onRedo, enabled = canRedo) { Text("Redo") }
            TextButton(onClick = onAdvanced) { Text("More") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRestart, enabled = hasPath) {
                Icon(Icons.Default.Refresh, contentDescription = "Restart")
            }
            Button(
                onClick = onPlayOnGlyph,
                enabled = hasPath && !hardwareBusy,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (hardwareBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (hardwareEnabled) "Resync Glyph" else "Send to Glyph")
            }
            if (hardwareEnabled) {
                OutlinedButton(onClick = onStopHardware) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PathAdvancedOpsSheet(
    hasPath: Boolean,
    onReverse: () -> Unit,
    onMirror: () -> Unit,
    onCloseLoop: () -> Unit,
    onDuplicate: () -> Unit,
    onSimplify: () -> Unit,
    onSmooth: () -> Unit,
    onTrim: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Path tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        listOf(
            "Reverse" to onReverse,
            "Mirror" to onMirror,
            "Close Loop" to onCloseLoop,
            "Duplicate" to onDuplicate,
            "Simplify" to onSimplify,
            "Smooth" to onSmooth,
            "Trim Ends" to onTrim
        ).chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, action) ->
                    OutlinedButton(
                        onClick = action,
                        enabled = hasPath,
                        modifier = Modifier.weight(1f)
                    ) { Text(label) }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onClear,
            enabled = hasPath,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear path")
        }
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Done") }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SequenceLibrarySheet(
    presets: List<SavedSequence>,
    saved: List<SavedSequence>,
    onLoad: (SavedSequence) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Sequences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Predefined", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        presets.forEach { seq ->
            SequenceRow(
                sequence = seq,
                onPlay = { onLoad(seq) },
                onDelete = null
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Saved", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        if (saved.isEmpty()) {
            Text(
                text = "No saved sequences yet. Draw a path and tap Save.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            saved.forEach { seq ->
                SequenceRow(
                    sequence = seq,
                    onPlay = { onLoad(seq) },
                    onDelete = { onDelete(seq.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text("Close")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SequenceRow(
    sequence: SavedSequence,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sequence.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${sequence.nodes.size} nodes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        TextButton(onClick = onPlay) { Text("Play") }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun SaveSequenceDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Sequence") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.ifBlank { "My Sequence" }) },
                enabled = true
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PathSettingsSheet(
    settings: PathSettings,
    onChange: (PathSettings) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Path Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        SettingsSlider("Animation Speed", settings.animationSpeed, 0.25f..3f) {
            onChange(settings.copy(animationSpeed = it))
        }
        SettingsSlider("Node Duration (ms)", settings.nodeDurationMs.toFloat(), 40f..400f) {
            onChange(settings.copy(nodeDurationMs = it.toLong()))
        }
        SettingsSlider("Fade Duration (ms)", settings.fadeDurationMs.toFloat(), 0f..250f) {
            onChange(settings.copy(fadeDurationMs = it.toLong(), attackMs = -1L, releaseMs = -1L))
        }
        SettingsSlider(
            "Attack (ms)",
            settings.effectiveAttackMs.toFloat(),
            0f..250f
        ) {
            onChange(settings.copy(attackMs = it.toLong()))
        }
        SettingsSlider(
            "Release (ms)",
            settings.effectiveReleaseMs.toFloat(),
            0f..250f
        ) {
            onChange(settings.copy(releaseMs = it.toLong()))
        }
        SettingsSlider("Sustain ratio", settings.sustainRatio, 0.1f..1f) {
            onChange(settings.copy(sustainRatio = it))
        }
        SettingsSlider("Brightness", settings.brightness, 0.1f..1f) {
            onChange(settings.copy(brightness = it))
        }
        SettingsSlider("Trail Length", settings.trailLength.toFloat(), 1f..8f) {
            onChange(settings.copy(trailLength = it.toInt()))
        }
        SettingsSlider("Trail Fade", settings.trailFade, 0f..1f) {
            onChange(settings.copy(trailFade = it))
        }
        SettingsSlider("Sampling Density (px)", settings.samplingDensityPx, 2f..16f) {
            onChange(settings.copy(samplingDensityPx = it))
        }
        SettingsSlider("Min Node Distance", settings.minimumNodeDistance.toFloat(), 1f..4f) {
            onChange(settings.copy(minimumNodeDistance = it.toInt()))
        }
        SettingsSlider("Smoothing Strength", settings.smoothingStrength, 0f..1f) {
            onChange(settings.copy(smoothingStrength = it))
        }
        SettingsSlider("Repeat Count", settings.repeatCount.toFloat().coerceAtLeast(1f), 1f..8f) {
            onChange(settings.copy(repeatCount = it.toInt(), infiniteLoop = false))
        }

        SettingsSwitch("Infinite Loop", settings.infiniteLoop) {
            onChange(settings.copy(infiniteLoop = it))
        }
        SettingsSwitch("Reverse Playback", settings.reversePlayback) {
            onChange(settings.copy(reversePlayback = it))
        }
        SettingsSwitch("Ping-Pong", settings.pingPong) {
            onChange(settings.copy(pingPong = it))
        }

        Text("Interpolation", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.interpolation == InterpolationMode.LINEAR,
                onClick = { onChange(settings.copy(interpolation = InterpolationMode.LINEAR)) },
                label = { Text("Linear") }
            )
            FilterChip(
                selected = settings.interpolation == InterpolationMode.STEP,
                onClick = { onChange(settings.copy(interpolation = InterpolationMode.STEP)) },
                label = { Text("Step") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text("Done")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "$label: ${"%.2f".format(value)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(value = value.coerceIn(range), onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

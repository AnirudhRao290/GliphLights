package com.example.gliphlights.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.editor.render.GlyphMapView
import com.example.gliphlights.viewmodel.EditorTool
import com.example.gliphlights.viewmodel.GlyphEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlyphEditorScreen(
    onBack: () -> Unit,
    viewModel: GlyphEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        if (uiState.isSessionActive) {
            viewModel.startRenderer()
        } else if (!uiState.isLoading && uiState.errorMessage == null) {
            viewModel.startSession()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatus()
        }
    }

    if (uiState.showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.showSaveDialog(false) },
            title = { Text("Save frame") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveFrame(name) }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showSaveDialog(false) }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Editor", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${uiState.activeCount}/36 · ${uiState.tool.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
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
                        enabled = uiState.activeCount > 0
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save frame")
                    }
                    IconButton(onClick = viewModel::undo, enabled = uiState.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = viewModel::redo, enabled = uiState.canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = viewModel::zoomOut) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                    }
                    IconButton(onClick = viewModel::zoomIn) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                    }
                    IconButton(onClick = viewModel::toggleSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            )
        },
        bottomBar = {
            EditorToolBar(
                tool = uiState.tool,
                isSessionActive = uiState.isSessionActive,
                isLoading = uiState.isLoading,
                onSelectTool = viewModel::setTool,
                onStartSession = viewModel::startSession,
                onStopSession = viewModel::stopSession
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            GlyphMapView(
                layout = uiState.layout,
                activeChannels = uiState.activeChannels,
                onGestureEvent = viewModel::handleGestureEvent,
                onLayoutCreated = viewModel::onLayoutCreated,
                controlledScale = uiState.zoomScale,
                onScaleChange = viewModel::onZoomChanged,
                glowIntensity = uiState.glowIntensity,
                hapticsEnabled = uiState.hapticsEnabled,
                previewPulse = uiState.previewPulse,
                modifier = Modifier.fillMaxSize(),
                view = view
            )

            AnimatedContent(
                targetState = uiState.tool,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "toolHint",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) { tool ->
                Text(
                    text = when (tool) {
                        EditorTool.PAINT -> "Paint nodes on"
                        EditorTool.ERASE -> "Erase nodes"
                        EditorTool.FILL -> "Tap a node to fill its arc"
                        EditorTool.PREVIEW -> "Preview pulse · editing locked"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (uiState.showSettings) {
        ModalBottomSheet(
            onDismissRequest = viewModel::toggleSettings,
            sheetState = sheet
        ) {
            EditorSettingsSheet(
                glowIntensity = uiState.glowIntensity,
                hapticsEnabled = uiState.hapticsEnabled,
                zoomScale = uiState.zoomScale,
                onGlowChange = viewModel::updateGlowIntensity,
                onHapticsChange = viewModel::setHapticsEnabled,
                onResetZoom = viewModel::resetZoom,
                onClearAll = viewModel::clearAll,
                onClose = viewModel::toggleSettings
            )
        }
    }
}

@Composable
private fun EditorToolBar(
    tool: EditorTool,
    isSessionActive: Boolean,
    isLoading: Boolean,
    onSelectTool: (EditorTool) -> Unit,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolChip(
                label = "Paint",
                icon = Icons.Default.Brush,
                selected = tool == EditorTool.PAINT,
                onClick = { onSelectTool(EditorTool.PAINT) },
                modifier = Modifier.weight(1f)
            )
            ToolChip(
                label = "Erase",
                icon = Icons.Default.Clear,
                selected = tool == EditorTool.ERASE,
                onClick = { onSelectTool(EditorTool.ERASE) },
                modifier = Modifier.weight(1f)
            )
            ToolChip(
                label = "Fill",
                icon = Icons.Default.FormatColorFill,
                selected = tool == EditorTool.FILL,
                onClick = { onSelectTool(EditorTool.FILL) },
                modifier = Modifier.weight(1f)
            )
            ToolChip(
                label = "Preview",
                icon = Icons.Default.Preview,
                selected = tool == EditorTool.PREVIEW,
                onClick = { onSelectTool(EditorTool.PREVIEW) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = if (isSessionActive) onStopSession else onStartSession,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = if (isSessionActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isSessionActive) "Stop Glyph" else "Send to Glyph")
        }
    }
}

@Composable
private fun ToolChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun EditorSettingsSheet(
    glowIntensity: Float,
    hapticsEnabled: Boolean,
    zoomScale: Float,
    onGlowChange: (Float) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onResetZoom: () -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Editor settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Glow intensity: ${"%.1f".format(glowIntensity)}", style = MaterialTheme.typography.bodyMedium)
        Slider(value = glowIntensity, onValueChange = onGlowChange, valueRange = 0.4f..1.5f)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Haptic feedback", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = hapticsEnabled, onCheckedChange = onHapticsChange)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Zoom: ${"%.0f".format(zoomScale * 100)}%", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onResetZoom, modifier = Modifier.fillMaxWidth()) {
            Text("Reset zoom")
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onClearAll, modifier = Modifier.fillMaxWidth()) {
            Text("Clear all nodes")
        }

        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text("Done")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

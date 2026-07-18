package com.example.gliphlights.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.editor.model.GlyphDoughnut3a
import com.example.gliphlights.editor.model.GlyphRegion
import com.example.gliphlights.ui.components.GlassMetricCard
import com.example.gliphlights.ui.components.GlassPill
import com.example.gliphlights.ui.components.StudioGrainOverlay
import com.example.gliphlights.ui.visualizer.VisualizerHeroStage
import com.example.gliphlights.viewmodel.MusicVisualizerViewModel
import com.example.gliphlights.viewmodel.VisualizationModeType

@Composable
fun MusicVisualizerScreen(
    viewModel: MusicVisualizerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdvanced by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatus()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    val fills = remember(uiState.audioLevel, uiState.isRunning, uiState.mode) {
        if (!uiState.isRunning) FloatArray(36)
        else tubeFillsFromAudio(uiState.audioLevel, uiState.mode)
    }

    val bass = remember(uiState.audioLevel, uiState.peakAmplitude) {
        ((uiState.audioLevel * 0.65f + uiState.peakAmplitude * 0.35f) * 100f).toInt().coerceIn(0, 100)
    }
    val treble = remember(uiState.audioLevel, uiState.rawAmplitude) {
        ((uiState.rawAmplitude * 0.8f + uiState.audioLevel * 0.2f) * 100f).toInt().coerceIn(0, 100)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color.Black)
    ) {
        StudioGrainOverlay(alpha = 0.10f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Visualizer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!uiState.hasPermission) {
                PermissionBanner(
                    denied = uiState.permissionRequested,
                    onRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                VisualizerHeroStage(
                    channelFills = fills,
                    audioLevel = if (uiState.isRunning) uiState.audioLevel else 0.12f,
                    spectrumSeed = (uiState.audioLevel * 1000).toInt(),
                    modifier = Modifier.fillMaxSize()
                )

                GlassPill(
                    text = "${uiState.mode.displayName} Mode",
                    leadingDot = Color(0xFFB44DFF),
                    accent = Color(0xFFE0B0FF),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                GlassPill(
                    text = "${uiState.fps.toInt().coerceAtLeast(0)} FPS",
                    accent = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassMetricCard(
                    title = "Bass",
                    value = "$bass%",
                    dotColor = Color(0xFFB44DFF),
                    modifier = Modifier.weight(1f)
                )
                GlassMetricCard(
                    title = "Treble",
                    value = "$treble%",
                    dotColor = Color(0xFF5B8CFF),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizationModeType.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.mode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!uiState.hasPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.start()
                        }
                    },
                    enabled = !uiState.isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start")
                }
                OutlinedButton(
                    onClick = viewModel::stop,
                    enabled = uiState.isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = viewModel::bakeFiveSeconds,
                enabled = uiState.isRunning && !uiState.isBaking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (uiState.isBaking) {
                        "Baking… ${(uiState.bakeProgressMs / 1000)}s"
                    } else {
                        "Bake 5s → Path preset"
                    }
                )
            }

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide tuning" else "Tuning")
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Sensitivity ${"%.1f".format(uiState.sensitivity)}x", color = Color.White)
                    Slider(
                        value = uiState.sensitivity,
                        onValueChange = viewModel::updateSensitivity,
                        valueRange = 0.1f..3f
                    )
                    Text("Noise gate ${"%.2f".format(uiState.noiseGate)}", color = Color.White)
                    Slider(
                        value = uiState.noiseGate,
                        onValueChange = viewModel::updateNoiseGate,
                        valueRange = 0f..0.3f
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun PermissionBanner(denied: Boolean, onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (denied) Color(0xFF3A1515) else Color(0xFF1A1A28))
            .padding(14.dp)
    ) {
        Text(
            text = if (denied) "Microphone blocked" else "Microphone needed",
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (denied) "Enable mic access in system settings."
            else "Visualizer listens on-device to drive Glyph tubes.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        if (!denied) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRequest) {
                Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Grant access")
            }
        }
    }
}

private fun tubeFillsFromAudio(level: Float, mode: VisualizationModeType): FloatArray {
    val fills = FloatArray(36)
    val energy = level.coerceIn(0f, 1f)
    if (energy < 0.02f) return fills

    val order = GlyphDoughnut3a.clockwiseIds
    val sdkById = buildMap {
        GlyphRegion.entries.forEach { region ->
            repeat(region.nodeCount) { i ->
                put("${region.name}${i + 1}", region.sdkIndex(i))
            }
        }
    }

    when (mode) {
        VisualizationModeType.PULSE, VisualizationModeType.GLOW -> {
            val lit = (energy * 36f).toInt().coerceIn(1, 36)
            val partial = (energy * 36f) - lit + 1f
            for (i in 0 until lit) {
                val sdk = sdkById[order[i % order.size]] ?: continue
                fills[sdk] = if (i == lit - 1) partial.coerceIn(0.2f, 1f) else 1f
            }
        }
        VisualizationModeType.WAVE -> {
            val head = ((energy * order.size).toInt()).coerceIn(0, order.size - 1)
            for (t in 0 until 7) {
                val idx = (head - t + order.size) % order.size
                val sdk = sdkById[order[idx]] ?: continue
                fills[sdk] = (1f - t / 7f) * energy.coerceAtLeast(0.4f)
            }
        }
        VisualizationModeType.BEAT -> {
            if (energy >= 0.35f) {
                listOf("A9", "A10", "A11", "B1", "B2", "B3", "B4", "B5", "C1", "C2", "C3").forEach { id ->
                    sdkById[id]?.let { fills[it] = energy }
                }
            } else {
                listOf("B1", "B2", "B3").forEach { id ->
                    sdkById[id]?.let { fills[it] = energy * 0.45f }
                }
            }
        }
    }
    return fills
}

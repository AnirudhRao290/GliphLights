package com.example.gliphlights.physics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.physics.model.PhysicsMode
import com.example.gliphlights.physics.model.PhysicsParams
import com.example.gliphlights.physics.render.PhysicsPreviewRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicsLabScreen(
    onBack: () -> Unit,
    viewModel: PhysicsLabViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val model by viewModel.animationModel.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(Unit) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    LaunchedEffect(ui.statusMessage) {
        ui.statusMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Physics Lab", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${ui.mode.displayName} · ${ui.fps} FPS",
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
                    IconButton(onClick = viewModel::toggleParams) {
                        Icon(Icons.Default.Settings, contentDescription = "Parameters")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PhysicsPreviewRenderer(
                layout = ui.layout,
                model = model,
                mode = ui.mode,
                onLayoutCreated = viewModel::onLayoutCreated,
                onTap = viewModel::onMagnetTap,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Text(
                text = ui.mode.description + if (ui.mode == PhysicsMode.MAGNET) " · Tap to place magnet" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PhysicsMode.entries.forEach { mode ->
                    FilterChip(
                        selected = ui.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = viewModel::playOnGlyph,
                    enabled = !ui.hardwareBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    if (ui.hardwareBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp).width(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (ui.hardwareEnabled) "Resync Glyph" else "Send to Glyph")
                }
                if (ui.hardwareEnabled) {
                    OutlinedButton(onClick = viewModel::stopGlyph) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.height(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
                if (ui.mode == PhysicsMode.MAGNET) {
                    TextButton(onClick = viewModel::clearMagnet) { Text("Clear magnet") }
                }
            }
        }
    }

    if (ui.showParams) {
        ModalBottomSheet(onDismissRequest = viewModel::toggleParams, sheetState = sheet) {
            PhysicsParamsSheet(
                mode = ui.mode,
                params = ui.params,
                onChange = viewModel::updateParams,
                onSave = viewModel::saveSettings,
                onReset = viewModel::resetSettings,
                onClose = viewModel::toggleParams
            )
        }
    }
}

@Composable
private fun PhysicsParamsSheet(
    mode: PhysicsMode,
    params: PhysicsParams,
    onChange: (PhysicsParams) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Parameters · ${mode.displayName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        ParamSlider("Gravity Strength", params.gravityStrength, 0f..2.5f) {
            onChange(params.copy(gravityStrength = it))
        }
        ParamSlider("Flow Speed", params.flowSpeed, 0.2f..2.5f) {
            onChange(params.copy(flowSpeed = it))
        }
        ParamSlider("Damping", params.damping, 0.5f..0.99f) {
            onChange(params.copy(damping = it))
        }
        ParamSlider("Trail Length", params.trailLength.toFloat(), 1f..8f) {
            onChange(params.copy(trailLength = it.toInt()))
        }
        ParamSlider("Brightness", params.brightness, 0.2f..1f) {
            onChange(params.copy(brightness = it))
        }
        ParamSlider("Glow Intensity", params.glowIntensity, 0.2f..1.5f) {
            onChange(params.copy(glowIntensity = it))
        }

        if (mode == PhysicsMode.FLUID || mode == PhysicsMode.MERCURY || mode == PhysicsMode.BUBBLE || mode == PhysicsMode.GRAVITY || mode == PhysicsMode.MAGNET) {
            ParamSlider("Fluid Amount", params.fluidAmount, 0.15f..0.9f) {
                onChange(params.copy(fluidAmount = it))
            }
            ParamSlider("Viscosity", params.viscosity, 0.1f..0.95f) {
                onChange(params.copy(viscosity = it))
            }
            ParamSlider("Surface Tension", params.surfaceTension, 0f..1f) {
                onChange(params.copy(surfaceTension = it))
            }
            ParamSlider("Gravity Multiplier", params.gravityMultiplier, 0.2f..2.5f) {
                onChange(params.copy(gravityMultiplier = it))
            }
            ParamSlider("Energy Loss", params.energyLoss, 0.7f..0.99f) {
                onChange(params.copy(energyLoss = it))
            }
        }

        if (mode == PhysicsMode.SAND || mode == PhysicsMode.ZERO_G || mode == PhysicsMode.PINBALL) {
            ParamSlider("Particle Count", params.particleCount.toFloat(), 8f..96f) {
                onChange(params.copy(particleCount = it.toInt()))
            }
        }

        ParamSlider("Simulation Speed", params.simulationSpeed, 0.4f..2f) {
            onChange(params.copy(simulationSpeed = it))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Done") }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ${"%.2f".format(value)}", style = MaterialTheme.typography.bodyMedium)
        Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range)
    }
}

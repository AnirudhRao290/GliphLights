package com.example.gliphlights.physics

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
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.physics.model.PhysicsMode
import com.example.gliphlights.physics.model.PhysicsParams
import com.example.gliphlights.physics.render.PhysicsPreviewRenderer
import com.example.gliphlights.ui.components.GlassPanel
import com.example.gliphlights.ui.components.GlassPill
import com.example.gliphlights.ui.components.StudioFluidBackdrop
import com.example.gliphlights.ui.components.StudioGrainOverlay
import kotlin.math.sqrt

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

    val energyPct = remember(model) {
        var s = 0f
        for (a in model.nodeAlphas) s += a
        ((s / model.nodeAlphas.size.coerceAtLeast(1)) * 100f).toInt().coerceIn(0, 100)
    }
    val gLen = sqrt(ui.gravityX * ui.gravityX + ui.gravityY * ui.gravityY)

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text("Physics Lab", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleParams) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            StudioFluidBackdrop(alpha = 0.18f)
            StudioGrainOverlay(alpha = 0.08f)

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    PhysicsPreviewRenderer(
                        layout = ui.layout,
                        model = model,
                        mode = ui.mode,
                        onLayoutCreated = viewModel::onLayoutCreated,
                        onTap = viewModel::onMagnetTap,
                        modifier = Modifier.fillMaxSize()
                    )

                    GlassPill(
                        text = "${ui.mode.displayName} Simulation",
                        leadingDot = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    )
                    GlassPill(
                        text = "${ui.fps} FPS",
                        accent = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatLine("Gravity", if (gLen > 1f) "↓" else "·")
                        StatLine("Particles", "${ui.params.particleCount.coerceAtLeast(48)}")
                        StatLine("Energy", "$energyPct%")
                        StatLine("Viscosity", "%.2f".format(ui.params.viscosity))
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SideAction(Icons.Default.Pause, "Pause") {
                            if (ui.running) viewModel.stop() else viewModel.start()
                        }
                        SideAction(Icons.Default.Refresh, "Reset") {
                            viewModel.resetSettings()
                            viewModel.start()
                        }
                        SideAction(Icons.Default.WbSunny, "Emit") {
                            viewModel.playOnGlyph()
                        }
                    }
                }

                GlassPanel(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Simulation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        primaryModes().forEach { (mode, icon) ->
                            ModeTile(
                                label = mode.displayName,
                                icon = icon,
                                selected = ui.mode == mode,
                                onClick = { viewModel.setMode(mode) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gravity Strength", color = Color.White.copy(alpha = 0.75f))
                        Text(
                            "%.2f".format(ui.params.gravityStrength),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Slider(
                        value = ui.params.gravityStrength,
                        onValueChange = {
                            viewModel.updateParams(ui.params.copy(gravityStrength = it))
                        },
                        valueRange = 0f..2.5f
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = viewModel::playOnGlyph,
                            enabled = !ui.hardwareBusy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (ui.hardwareBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (ui.hardwareEnabled) "Resync Glyph" else "Send to Glyph")
                            }
                        }
                        if (ui.hardwareEnabled) {
                            OutlinedButton(onClick = viewModel::stopGlyph) {
                                Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                            }
                        }
                        TextButton(onClick = viewModel::toggleParams) {
                            Text("More")
                        }
                    }
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

private fun primaryModes(): List<Pair<PhysicsMode, ImageVector>> = listOf(
    PhysicsMode.FLUID to Icons.Default.Science,
    PhysicsMode.MERCURY to Icons.Default.WaterDrop,
    PhysicsMode.SAND to Icons.Default.Grain,
    PhysicsMode.BUBBLE to Icons.Default.BubbleChart,
    PhysicsMode.GRAVITY to Icons.Default.LocalFireDepartment
)

@Composable
private fun StatLine(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x99101010))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SideAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC141414))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ModeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.55f)
        )
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
        Text("Simulation · ${mode.displayName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        ParamSlider("Flow Speed", params.flowSpeed, 0.2f..2.5f) { onChange(params.copy(flowSpeed = it)) }
        ParamSlider("Damping", params.damping, 0.5f..0.99f) { onChange(params.copy(damping = it)) }
        ParamSlider("Viscosity", params.viscosity, 0.1f..0.95f) { onChange(params.copy(viscosity = it)) }
        ParamSlider("Fluid Amount", params.fluidAmount, 0.15f..0.9f) { onChange(params.copy(fluidAmount = it)) }
        ParamSlider("Glow", params.glowIntensity, 0.2f..1.5f) { onChange(params.copy(glowIntensity = it)) }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Reset") }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
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
        Text("$label: ${"%.2f".format(value)}")
        Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range)
    }
}

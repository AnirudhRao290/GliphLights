package com.example.gliphlights.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.models.AnimationParams
import com.example.gliphlights.models.ControlsUiState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.ui.components.LiveGlyphPreview
import com.example.gliphlights.ui.components.ScreenHeader
import com.example.gliphlights.viewmodel.ControlsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlsScreen(
    viewModel: ControlsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val animationParams by viewModel.animationParams.collectAsState()
    val progressValue by viewModel.progressValue.collectAsState()
    val progressZoneA by viewModel.progressZoneA.collectAsState()
    val progressZoneB by viewModel.progressZoneB.collectAsState()
    var selectedZone by remember { mutableStateOf<GlyphZone?>(null) }
    var showAnimParams by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        ScreenHeader(
            title = "Controls",
            subtitle = "Select a Glyph arc to reveal tools"
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is ControlsUiState.Loading -> {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }

            is ControlsUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is ControlsUiState.Success -> {
                val channelFills = remember(
                    state.glyphState.activeChannels,
                    selectedZone,
                    progressZoneA,
                    progressZoneB,
                    progressValue
                ) {
                    buildsControlsFills(
                        active = state.glyphState.activeChannels,
                        selectedZone = selectedZone,
                        progressA = progressZoneA,
                        progressB = progressZoneB,
                        progressC = progressValue
                    )
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        LiveGlyphPreview(
                            activeChannels = state.glyphState.activeChannels,
                            channelFills = channelFills,
                            glowActive = state.glyphState.isActive,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.zones.forEach { zone ->
                            val active = zone in state.glyphState.activeZones
                            FilterChip(
                                selected = selectedZone == zone,
                                onClick = {
                                    selectedZone = if (selectedZone == zone) null else zone
                                },
                                label = {
                                    Text("Arc ${zone.name}" + if (active) " · on" else "")
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = selectedZone != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        selectedZone?.let { zone ->
                            val progress = when (zone) {
                                GlyphZone.A -> progressZoneA
                                GlyphZone.B -> progressZoneB
                                GlyphZone.C -> progressValue
                            }
                            ZoneContextPanel(
                                zone = zone,
                                glyphState = state.glyphState,
                                progress = progress,
                                onToggleZone = { viewModel.toggleZone(zone) },
                                onAnimateZone = { viewModel.animateZone(zone) },
                                onToggleChannel = { viewModel.toggleChannels(listOf(it)) },
                                onProgressChange = { viewModel.updateProgressForZone(zone, it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (showAnimParams) "Hide animation params" else "Animation params",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showAnimParams = !showAnimParams }
                            .padding(vertical = 4.dp)
                    )

                    AnimatedVisibility(visible = showAnimParams) {
                        CompactAnimParams(
                            params = animationParams,
                            onChange = viewModel::updateAnimationParams
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::turnOff,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Turn Off All")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneContextPanel(
    zone: GlyphZone,
    glyphState: GlyphState,
    progress: Int,
    onToggleZone: () -> Unit,
    onAnimateZone: () -> Unit,
    onToggleChannel: (Int) -> Unit,
    onProgressChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isZoneActive = zone in glyphState.activeZones

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Arc ${zone.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = zone.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onToggleZone, shape = RoundedCornerShape(12.dp)) {
                    Icon(
                        if (isZoneActive) Icons.Default.Lightbulb else Icons.Default.PowerSettingsNew,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Button(onClick = onAnimateZone, shape = RoundedCornerShape(12.dp)) {
                    Text("Animate")
                }
            }
        }

        Text(text = "Channels", style = MaterialTheme.typography.labelMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            zone.channels.forEach { channel ->
                FilterChip(
                    selected = channel in glyphState.activeChannels,
                    onClick = { onToggleChannel(channel) },
                    label = { Text(zone.getChannelName(channel)) }
                )
            }
        }

        Text(text = "Fill progress · $progress%", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = progress.toFloat(),
            onValueChange = { onProgressChange(it.toInt()) },
            valueRange = 0f..100f
        )
    }
}

@Composable
private fun CompactAnimParams(
    params: AnimationParams,
    onChange: (AnimationParams) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Period ${params.period} ms", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = params.period.toFloat(),
            onValueChange = { onChange(params.copy(period = it.toInt())) },
            valueRange = 500f..5000f
        )
        Text(text = "Cycles ${params.cycles}", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = params.cycles.toFloat(),
            onValueChange = { onChange(params.copy(cycles = it.toInt())) },
            valueRange = 1f..10f
        )
        Text(text = "Interval ${params.interval} ms", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = params.interval.toFloat(),
            onValueChange = { onChange(params.copy(interval = it.toInt())) },
            valueRange = 100f..1000f
        )
    }
}

/** Partial tube fill for the selected arc's progress slider (swipe-to-fill). */
private fun buildsControlsFills(
    active: Set<Int>,
    selectedZone: GlyphZone?,
    progressA: Int,
    progressB: Int,
    progressC: Int
): FloatArray {
    val fills = FloatArray(36) { i -> if (i in active) 1f else 0f }
    fun applyZone(zone: GlyphZone, progress: Int) {
        val n = zone.channels.size
        val lit = ((progress / 100.0) * n).toInt().coerceIn(0, n)
        val frac = ((progress / 100.0) * n) - lit
        zone.channels.forEachIndexed { index, channel ->
            fills[channel] = when {
                index < lit -> 1f
                index == lit && frac > 0.02 -> frac.toFloat().coerceIn(0.15f, 1f)
                else -> fills[channel]
            }
        }
    }
    when (selectedZone) {
        GlyphZone.A -> applyZone(GlyphZone.A, progressA)
        GlyphZone.B -> applyZone(GlyphZone.B, progressB)
        GlyphZone.C -> applyZone(GlyphZone.C, progressC)
        null -> {}
    }
    return fills
}

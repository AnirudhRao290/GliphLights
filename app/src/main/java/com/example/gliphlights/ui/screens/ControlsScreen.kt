package com.example.gliphlights.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.models.ControlsUiState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphZone
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Glyph Controls",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        when (val state = uiState) {
            is ControlsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            is ControlsUiState.Success -> {
                // Zone sections
                state.zones.forEach { zone ->
                    ZoneSection(
                        zone = zone,
                        glyphState = state.glyphState,
                        onToggleZone = { viewModel.toggleZone(zone) },
                        onAnimateZone = { viewModel.animateZone(zone) },
                        onToggleChannel = { channel ->
                            viewModel.toggleChannels(listOf(channel))
                        }
                    )

                    // Progress sliders for each zone
                    val zoneProgress = when (zone) {
                        GlyphZone.A -> progressZoneA
                        GlyphZone.B -> progressZoneB
                        GlyphZone.C -> progressValue
                    }
                    ProgressSection(
                        zone = zone,
                        progress = zoneProgress,
                        onProgressChange = { progress ->
                            viewModel.updateProgressForZone(zone, progress)
                        }
                    )
                }

                // Animation parameters
                AnimationParamsSection(
                    params = animationParams,
                    onParamsChange = viewModel::updateAnimationParams
                )

                // Turn off all
                OutlinedButton(
                    onClick = viewModel::turnOff,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Turn Off All",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            is ControlsUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneSection(
    zone: GlyphZone,
    glyphState: GlyphState,
    onToggleZone: () -> Unit,
    onAnimateZone: () -> Unit,
    onToggleChannel: (Int) -> Unit
) {
    val isZoneActive = zone in glyphState.activeZones

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Zone header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Zone ${zone.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = zone.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onToggleZone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isZoneActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isZoneActive) Icons.Default.Lightbulb else Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Button(
                        onClick = onAnimateZone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Animate", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Channel chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                zone.channels.forEach { channel ->
                    val isChannelActive = channel in glyphState.activeChannels
                    ChannelChip(
                        channelName = zone.getChannelName(channel),
                        isActive = isChannelActive,
                        onClick = { onToggleChannel(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelChip(
    channelName: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "channelChipColor"
    )

    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = {
            Text(
                text = channelName,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = if (isActive) {
            {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ProgressSection(
    zone: GlyphZone,
    progress: Int,
    onProgressChange: (Int) -> Unit
) {
    val description = when (zone) {
        GlyphZone.A -> "Progress on vertical bar (A1-A11)"
        GlyphZone.B -> "Progress on horizontal bar (B1-B5)"
        GlyphZone.C -> "Progress on camera ring (C1-C20)"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Zone ${zone.name} Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Slider(
                value = progress.toFloat(),
                onValueChange = { onProgressChange(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnimationParamsSection(
    params: com.example.gliphlights.models.AnimationParams,
    onParamsChange: (com.example.gliphlights.models.AnimationParams) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Animation Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Period slider
            Column {
                Text(
                    text = "Period: ${params.period}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = params.period.toFloat(),
                    onValueChange = { onParamsChange(params.copy(period = it.toInt())) },
                    valueRange = 500f..5000f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Cycles slider
            Column {
                Text(
                    text = "Cycles: ${params.cycles}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = params.cycles.toFloat(),
                    onValueChange = { onParamsChange(params.copy(cycles = it.toInt())) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Interval slider
            Column {
                Text(
                    text = "Interval: ${params.interval}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = params.interval.toFloat(),
                    onValueChange = { onParamsChange(params.copy(interval = it.toInt())) },
                    valueRange = 100f..1000f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

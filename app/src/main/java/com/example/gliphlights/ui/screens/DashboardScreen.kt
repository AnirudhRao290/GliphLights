package com.example.gliphlights.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.ErrorState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphUiState
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.ui.components.GlyphCard
import com.example.gliphlights.ui.components.QuickActionChip
import com.example.gliphlights.ui.components.ScreenHeader
import com.example.gliphlights.ui.components.SectionLabel
import com.example.gliphlights.ui.components.StatusDot
import com.example.gliphlights.ui.components.StudioTile
import com.example.gliphlights.ui.theme.GlyphActive
import com.example.gliphlights.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    onNavigateToEditor: () -> Unit = {},
    onNavigateToPathBuilder: () -> Unit = {},
    onNavigateToPhysicsLab: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is GlyphUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting to Glyph…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is GlyphUiState.Success -> {
                DashboardContent(
                    glyphState = state.glyphState,
                    deviceInfo = state.deviceInfo,
                    onToggleAll = viewModel::toggleAll,
                    onAnimateAll = viewModel::animateAll,
                    onTurnOff = viewModel::turnOff,
                    onNavigateToEditor = onNavigateToEditor,
                    onNavigateToPathBuilder = onNavigateToPathBuilder,
                    onNavigateToPhysicsLab = onNavigateToPhysicsLab
                )
            }

            is GlyphUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Couldn't connect",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = viewModel::clearError) {
                        Text("Dismiss")
                    }
                }
            }
        }

        if (errorState !is ErrorState.None) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = viewModel::clearError) { Text("OK") }
                }
            ) {
                Text(
                    when (val e = errorState) {
                        is ErrorState.SdkUnavailable -> e.message
                        is ErrorState.RuntimeError -> e.message
                        else -> "Something went wrong"
                    }
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    glyphState: GlyphState,
    deviceInfo: DeviceInfo,
    onToggleAll: () -> Unit,
    onAnimateAll: () -> Unit,
    onTurnOff: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToPathBuilder: () -> Unit,
    onNavigateToPhysicsLab: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        ScreenHeader(
            title = "Glyph",
            subtitle = deviceInfo.model.ifBlank { "Nothing Phone" }
        )

        HeroStatusCard(
            glyphState = glyphState,
            deviceInfo = deviceInfo,
            onPower = { if (glyphState.isActive) onTurnOff() else onToggleAll() }
        )

        Column {
            SectionLabel("Studio")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudioTile(
                    title = "Editor",
                    subtitle = "Tap nodes live",
                    icon = Icons.Default.Lightbulb,
                    onClick = onNavigateToEditor,
                    emphasized = true,
                    modifier = Modifier.weight(1f)
                )
                StudioTile(
                    title = "Paths",
                    subtitle = "Draw & play",
                    icon = Icons.Default.Create,
                    onClick = onNavigateToPathBuilder,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            StudioTile(
                title = "Physics Lab",
                subtitle = "Gravity, fluid, sand & more",
                icon = Icons.Default.Science,
                onClick = onNavigateToPhysicsLab,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            SectionLabel("Quick actions")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    label = if (glyphState.isActive) "All off" else "All on",
                    icon = Icons.Default.Lightbulb,
                    onClick = onToggleAll,
                    selected = glyphState.isActive,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "Animate",
                    icon = Icons.Default.Refresh,
                    onClick = onAnimateAll,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            QuickActionChip(
                label = "Turn off",
                icon = Icons.Default.PowerSettingsNew,
                onClick = onTurnOff,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            SectionLabel("Zones")
            GlyphCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlyphZone.entries.forEach { zone ->
                        ZonePill(
                            zone = zone,
                            active = zone in glyphState.activeZones,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HeroStatusCard(
    glyphState: GlyphState,
    deviceInfo: DeviceInfo,
    onPower: () -> Unit
) {
    val ringColor by animateColorAsState(
        targetValue = if (glyphState.isActive) {
            GlyphActive.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        },
        label = "ring"
    )

    GlyphCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(active = deviceInfo.isSupported)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (deviceInfo.isSupported) "SDK ready" else "Unsupported",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (glyphState.isActive) "Active" else "Idle",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (glyphState.isActive) {
                        "${glyphState.activeChannels.size} channels lit"
                    } else {
                        "Lights are off"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ringColor)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (glyphState.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable(onClick = onPower),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = if (glyphState.isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ZonePill(
    zone: GlyphZone,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusDot(active = active)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = zone.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

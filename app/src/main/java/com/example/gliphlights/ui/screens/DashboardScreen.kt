package com.example.gliphlights.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.ErrorState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphUiState
import com.example.gliphlights.presets.GlyphPreset
import com.example.gliphlights.ui.components.ContinueEditingCard
import com.example.gliphlights.ui.components.LiveHeroCard
import com.example.gliphlights.ui.components.QuickActionChip
import com.example.gliphlights.ui.components.QuickToolTile
import com.example.gliphlights.ui.components.ScreenHeader
import com.example.gliphlights.ui.components.SectionLabel
import com.example.gliphlights.viewmodel.DashboardHeroState
import com.example.gliphlights.viewmodel.DashboardViewModel
import com.example.gliphlights.viewmodel.StudioDestination

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onNavigateToEditor: () -> Unit = {},
    onNavigateToPathBuilder: () -> Unit = {},
    onNavigateToPhysicsLab: () -> Unit = {},
    onNavigateToPerform: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToPresence: () -> Unit = {},
    onNavigateToClock: () -> Unit = {},
    onEditPreset: (GlyphPreset) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val heroState by viewModel.heroState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(heroState.shareIntent) {
        heroState.shareIntent?.let { intent ->
            context.startActivity(intent)
            viewModel.consumeShareIntent()
        }
    }

    LaunchedEffect(heroState.statusMessage) {
        // snackbar handled below via error/status overlays
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = uiState,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "dashboardState"
        ) { state ->
            when (state) {
                is GlyphUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Preparing studio…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is GlyphUiState.Success -> {
                    DashboardContent(
                        glyphState = state.glyphState,
                        deviceInfo = state.deviceInfo,
                        heroState = heroState,
                        onToggleAll = viewModel::toggleAll,
                        onAnimateAll = viewModel::animateAll,
                        onTurnOff = viewModel::turnOff,
                        onContinue = {
                            val dest = heroState.continueDestination
                            viewModel.rememberStudio(dest)
                            when (dest.route) {
                                StudioDestination.PathBuilder.route -> onNavigateToPathBuilder()
                                StudioDestination.PhysicsLab.route -> onNavigateToPhysicsLab()
                                else -> onNavigateToEditor()
                            }
                        },
                        onOpenStudio = { destination ->
                            viewModel.rememberStudio(destination)
                            when (destination.route) {
                                StudioDestination.PathBuilder.route -> onNavigateToPathBuilder()
                                StudioDestination.PhysicsLab.route -> onNavigateToPhysicsLab()
                                else -> onNavigateToEditor()
                            }
                        },
                        onPlayPreset = viewModel::playPreset,
                        onEditPreset = onEditPreset,
                        onTogglePin = viewModel::togglePin,
                        onFork = viewModel::forkPreset,
                        onShare = viewModel::sharePreset,
                        onPerform = onNavigateToPerform,
                        onBatteryArc = viewModel::startBatteryArc,
                        onFocusTimer = onNavigateToFocus,
                        onPresence = onNavigateToPresence,
                        onClock = onNavigateToClock,
                        onStartAmbient = viewModel::startAmbient,
                        onStopAmbient = viewModel::stopAmbient
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
        }

        val snackMessage = when {
            errorState !is ErrorState.None -> when (val e = errorState) {
                is ErrorState.SdkUnavailable -> e.message
                is ErrorState.RuntimeError -> e.message
                else -> "Something went wrong"
            }
            heroState.statusMessage != null -> heroState.statusMessage
            else -> null
        }
        if (snackMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = {
                        viewModel.clearError()
                        viewModel.clearStatus()
                    }) { Text("OK") }
                }
            ) {
                Text(snackMessage)
            }
        }
    }
}

@Composable
private fun DashboardContent(
    glyphState: GlyphState,
    deviceInfo: DeviceInfo,
    heroState: DashboardHeroState,
    onToggleAll: () -> Unit,
    onAnimateAll: () -> Unit,
    onTurnOff: () -> Unit,
    onContinue: () -> Unit,
    onOpenStudio: (StudioDestination) -> Unit,
    onPlayPreset: (GlyphPreset) -> Unit,
    onEditPreset: (GlyphPreset) -> Unit,
    onTogglePin: (GlyphPreset) -> Unit,
    onFork: (GlyphPreset) -> Unit,
    onShare: (GlyphPreset) -> Unit,
    onPerform: () -> Unit,
    onBatteryArc: () -> Unit,
    onFocusTimer: () -> Unit,
    onPresence: () -> Unit,
    onClock: () -> Unit,
    onStartAmbient: () -> Unit,
    onStopAmbient: () -> Unit
) {
    val secondaryTools = StudioDestination.all.filter {
        it.route != heroState.continueDestination.route
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Glyph Studio",
            subtitle = "Design · Animate · Perform"
        )

        LiveHeroCard(
            glyphState = glyphState,
            deviceInfo = deviceInfo,
            activityMode = heroState.activityMode,
            fps = heroState.fps,
            onPower = { if (glyphState.isActive) onTurnOff() else onToggleAll() }
        )

        if (heroState.progressHudLabel.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(heroState.progressHudLabel, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { heroState.progressHudPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Workspace")
            ContinueEditingCard(
                destination = heroState.continueDestination,
                onClick = onContinue
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                secondaryTools.forEach { dest ->
                    QuickToolTile(
                        title = when (dest.route) {
                            StudioDestination.PathBuilder.route -> "Paths"
                            StudioDestination.PhysicsLab.route -> "Physics"
                            else -> "Editor"
                        },
                        icon = when (dest.route) {
                            StudioDestination.PathBuilder.route -> Icons.Default.Create
                            StudioDestination.PhysicsLab.route -> Icons.Default.Science
                            else -> Icons.Default.Lightbulb
                        },
                        onClick = { onOpenStudio(dest) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("My Presets")
            if (heroState.presets.isEmpty()) {
                Text(
                    "Save a frame in Editor or a path in Path Builder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                heroState.presets.take(8).forEach { preset ->
                    PresetRow(
                        preset = preset,
                        onPlay = { onPlayPreset(preset) },
                        onEdit = { onEditPreset(preset) },
                        onPin = { onTogglePin(preset) },
                        onLongPress = { onShare(preset) },
                        onFork = { onFork(preset) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Daily")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    label = "Focus",
                    icon = Icons.Default.Timelapse,
                    onClick = onFocusTimer,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "Presence",
                    icon = Icons.Default.Person,
                    onClick = onPresence,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "Clock",
                    icon = Icons.Default.Schedule,
                    onClick = onClock,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    label = "Perform",
                    icon = Icons.Default.MusicNote,
                    onClick = onPerform,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "Battery arc",
                    icon = Icons.Default.Lightbulb,
                    onClick = onBatteryArc,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    label = "Ambient on",
                    icon = Icons.Default.Refresh,
                    onClick = onStartAmbient,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "Ambient off",
                    icon = Icons.Default.PowerSettingsNew,
                    onClick = onStopAmbient,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                QuickActionChip(
                    label = "Off",
                    icon = Icons.Default.PowerSettingsNew,
                    onClick = onTurnOff,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetRow(
    preset: GlyphPreset,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onLongPress: () -> Unit,
    onFork: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(preset.name, fontWeight = FontWeight.SemiBold)
            Text(
                preset.type.badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        TextButton(onClick = onEdit) { Text("Edit") }
        TextButton(onClick = onFork) { Text("Remix") }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onPin)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = if (preset.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Pin",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

package com.example.gliphlights.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.ErrorState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphUiState
import com.example.gliphlights.ui.components.ContinueEditingCard
import com.example.gliphlights.ui.components.LiveHeroCard
import com.example.gliphlights.ui.components.QuickActionChip
import com.example.gliphlights.ui.components.QuickToolTile
import com.example.gliphlights.ui.components.ScreenHeader
import com.example.gliphlights.ui.components.SectionLabel
import com.example.gliphlights.viewmodel.DashboardHeroState
import com.example.gliphlights.viewmodel.DashboardViewModel
import com.example.gliphlights.viewmodel.StudioDestination

@Composable
fun DashboardScreen(
    onNavigateToEditor: () -> Unit = {},
    onNavigateToPathBuilder: () -> Unit = {},
    onNavigateToPhysicsLab: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val heroState by viewModel.heroState.collectAsState()

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
                        }
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
    heroState: DashboardHeroState,
    onToggleAll: () -> Unit,
    onAnimateAll: () -> Unit,
    onTurnOff: () -> Unit,
    onContinue: () -> Unit,
    onOpenStudio: (StudioDestination) -> Unit
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

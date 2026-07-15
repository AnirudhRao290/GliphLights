package com.example.gliphlights.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.ErrorState
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphUiState
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Glyph Control",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = uiState) {
            is GlyphUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Connecting to Glyph SDK...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            is GlyphUiState.Success -> {
                DashboardContent(
                    glyphState = state.glyphState,
                    deviceInfo = state.deviceInfo,
                    onToggleAll = viewModel::toggleAll,
                    onAnimateAll = viewModel::animateAll,
                    onTurnOff = viewModel::turnOff
                )
            }

            is GlyphUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.clearError() }
                )
            }
        }

        // Error snackbar
        if (errorState !is ErrorState.None) {
            ErrorSnackbar(
                errorState = errorState,
                onDismiss = viewModel::clearError
            )
        }
    }
}

@Composable
private fun DashboardContent(
    glyphState: GlyphState,
    deviceInfo: DeviceInfo,
    onToggleAll: () -> Unit,
    onAnimateAll: () -> Unit,
    onTurnOff: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Info Card
        DeviceInfoCard(deviceInfo = deviceInfo)

        // Glyph Status Card
        GlyphStatusCard(glyphState = glyphState)

        // Active Zones Card
        ActiveZonesCard(activeZones = glyphState.activeZones)

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons
        ActionButtons(
            onToggleAll = onToggleAll,
            onAnimateAll = onAnimateAll,
            onTurnOff = onTurnOff
        )
    }
}

@Composable
private fun DeviceInfoCard(deviceInfo: DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = deviceInfo.model,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (deviceInfo.isSupported) {
                Text(
                    text = "SDK Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            } else {
                Text(
                    text = "Not Supported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun GlyphStatusCard(glyphState: GlyphState) {
    val indicatorColor by animateColorAsState(
        targetValue = if (glyphState.isActive) Color(0xFF4CAF50) else Color(0xFF757575),
        label = "indicatorColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Glyph Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (glyphState.isActive) "Active" else "Inactive",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(indicatorColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (glyphState.isActive) Icons.Default.Lightbulb else Icons.Default.PowerSettingsNew,
                    contentDescription = if (glyphState.isActive) "Active" else "Inactive",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveZonesCard(activeZones: Set<GlyphZone>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Active Zones",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlyphZone.entries.forEach { zone ->
                    ZoneChip(
                        zone = zone,
                        isActive = zone in activeZones
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneChip(zone: GlyphZone, isActive: Boolean) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "zoneChipColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Zone ${zone.name}",
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionButtons(
    onToggleAll: () -> Unit,
    onAnimateAll: () -> Unit,
    onTurnOff: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onToggleAll,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Toggle All",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Button(
            onClick = onAnimateAll,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "Animate All",
                style = MaterialTheme.typography.titleMedium
            )
        }

        OutlinedButton(
            onClick = onTurnOff,
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
                text = "Turn Off",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connection Error",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun ErrorSnackbar(
    errorState: ErrorState,
    onDismiss: () -> Unit
) {
    val message = when (errorState) {
        is ErrorState.SdkUnavailable -> errorState.message
        is ErrorState.UnsupportedDevice -> errorState.message
        is ErrorState.PermissionDenied -> errorState.message
        is ErrorState.RuntimeError -> errorState.message
        is ErrorState.None -> return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onDismiss) {
                Text(text = "OK")
            }
        }
    }
}

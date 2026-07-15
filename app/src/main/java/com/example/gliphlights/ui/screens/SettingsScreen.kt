package com.example.gliphlights.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.models.AppSettings
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.models.SettingsUiState
import com.example.gliphlights.models.StartupBehavior
import com.example.gliphlights.models.ThemePreference
import com.example.gliphlights.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        when (val state = uiState) {
            is SettingsUiState.Loading -> {
                Text(
                    text = "Loading settings...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            is SettingsUiState.Success -> {
                SettingsContent(
                    settings = state.settings,
                    onAnimatePeriodChange = viewModel::updateAnimatePeriod,
                    onAnimateCyclesChange = viewModel::updateAnimateCycles,
                    onAnimateIntervalChange = viewModel::updateAnimateInterval,
                    onDefaultZoneChange = viewModel::updateDefaultZone,
                    onStartupBehaviorChange = viewModel::updateStartupBehavior,
                    onThemeChange = viewModel::updateTheme
                )
            }

            is SettingsUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onAnimatePeriodChange: (Int) -> Unit,
    onAnimateCyclesChange: (Int) -> Unit,
    onAnimateIntervalChange: (Int) -> Unit,
    onDefaultZoneChange: (GlyphZone?) -> Unit,
    onStartupBehaviorChange: (StartupBehavior) -> Unit,
    onThemeChange: (ThemePreference) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animation Settings
        SettingsSection(title = "Animation Defaults") {
            SettingsSlider(
                label = "Period",
                value = settings.animatePeriod.toFloat(),
                valueRange = 500f..5000f,
                steps = 9,
                valueLabel = "${settings.animatePeriod}ms",
                onValueChange = { onAnimatePeriodChange(it.toInt()) }
            )

            SettingsSlider(
                label = "Cycles",
                value = settings.animateCycles.toFloat(),
                valueRange = 1f..10f,
                steps = 8,
                valueLabel = "${settings.animateCycles}",
                onValueChange = { onAnimateCyclesChange(it.toInt()) }
            )

            SettingsSlider(
                label = "Interval",
                value = settings.animateInterval.toFloat(),
                valueRange = 100f..1000f,
                steps = 8,
                valueLabel = "${settings.animateInterval}ms",
                onValueChange = { onAnimateIntervalChange(it.toInt()) }
            )
        }

        // Default Zone
        SettingsSection(title = "Default Zone") {
            ZoneDropdown(
                selectedZone = settings.defaultZone,
                onZoneSelected = onDefaultZoneChange
            )
        }

        // Startup Behavior
        SettingsSection(title = "Startup Behavior") {
            StartupBehaviorDropdown(
                selectedBehavior = settings.startupBehavior,
                onBehaviorSelected = onStartupBehaviorChange
            )
        }

        // Theme
        SettingsSection(title = "Theme") {
            ThemeDropdown(
                selectedTheme = settings.theme,
                onThemeSelected = onThemeChange
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneDropdown(
    selectedZone: GlyphZone?,
    onZoneSelected: (GlyphZone?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedZone?.let { "Zone ${it.name}" } ?: "All Zones",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Zones") },
                onClick = {
                    onZoneSelected(null)
                    expanded = false
                }
            )
            GlyphZone.entries.forEach { zone ->
                DropdownMenuItem(
                    text = { Text("Zone ${zone.name}") },
                    onClick = {
                        onZoneSelected(zone)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartupBehaviorDropdown(
    selectedBehavior: StartupBehavior,
    onBehaviorSelected: (StartupBehavior) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedBehavior.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            StartupBehavior.entries.forEach { behavior ->
                DropdownMenuItem(
                    text = { Text(behavior.displayName) },
                    onClick = {
                        onBehaviorSelected(behavior)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(
    selectedTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedTheme.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ThemePreference.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(theme.displayName) },
                    onClick = {
                        onThemeSelected(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}

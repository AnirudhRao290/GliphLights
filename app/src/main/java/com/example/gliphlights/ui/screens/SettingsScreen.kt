package com.example.gliphlights.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.models.AppSettings
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.models.SettingsUiState
import com.example.gliphlights.models.StartupBehavior
import com.example.gliphlights.models.ThemePreference
import com.example.gliphlights.ui.components.ScreenHeader
import com.example.gliphlights.viewmodel.SettingsViewModel

private enum class SettingsSection(val title: String) {
    APPEARANCE("Appearance"),
    ANIMATION("Animation"),
    PERFORMANCE("Performance"),
    AMBIENT("Ambient Rituals"),
    TIPS("Tips"),
    DEVELOPER("Developer"),
    ABOUT("About")
}

@Composable
fun SettingsScreen(
    onReplayTour: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val expanded = remember {
        mutableStateMapOf(
            SettingsSection.APPEARANCE to true,
            SettingsSection.ANIMATION to false,
            SettingsSection.PERFORMANCE to false,
            SettingsSection.AMBIENT to false,
            SettingsSection.TIPS to false,
            SettingsSection.DEVELOPER to false,
            SettingsSection.ABOUT to true
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            subtitle = "Studio preferences"
        )

        when (val state = uiState) {
            is SettingsUiState.Loading -> {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            is SettingsUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }

            is SettingsUiState.Success -> {
                SettingsSection.entries.forEach { section ->
                    CollapsibleSection(
                        title = section.title,
                        expanded = expanded[section] == true,
                        onToggle = { expanded[section] = expanded[section] != true }
                    ) {
                        when (section) {
                            SettingsSection.APPEARANCE -> AppearanceSection(
                                settings = state.settings,
                                onThemeChange = viewModel::updateTheme
                            )
                            SettingsSection.ANIMATION -> AnimationSection(
                                settings = state.settings,
                                onPeriod = viewModel::updateAnimatePeriod,
                                onCycles = viewModel::updateAnimateCycles,
                                onInterval = viewModel::updateAnimateInterval
                            )
                            SettingsSection.PERFORMANCE -> PerformanceSection(
                                settings = state.settings,
                                onStartup = viewModel::updateStartupBehavior
                            )
                            SettingsSection.AMBIENT -> AmbientSection(
                                ritual = state.ambientRitual,
                                brightness = state.ambientBrightness,
                                onRitual = viewModel::updateAmbientRitual,
                                onBrightness = viewModel::updateAmbientBrightness
                            )
                            SettingsSection.TIPS -> TipsSection(
                                onResetTips = {
                                    viewModel.resetTips()
                                    onReplayTour()
                                }
                            )
                            SettingsSection.DEVELOPER -> DeveloperSection(
                                settings = state.settings,
                                onDefaultZone = viewModel::updateDefaultZone
                            )
                            SettingsSection.ABOUT -> AboutSection()
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    settings: AppSettings,
    onThemeChange: (ThemePreference) -> Unit
) {
    Text("Theme", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePreference.entries.forEach { theme ->
            FilterChip(
                selected = settings.theme == theme,
                onClick = { onThemeChange(theme) },
                label = { Text(theme.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun AnimationSection(
    settings: AppSettings,
    onPeriod: (Int) -> Unit,
    onCycles: (Int) -> Unit,
    onInterval: (Int) -> Unit
) {
    SettingsSliderRow("Period", "${settings.animatePeriod} ms", settings.animatePeriod.toFloat(), 500f..5000f) {
        onPeriod(it.toInt())
    }
    SettingsSliderRow("Cycles", "${settings.animateCycles}", settings.animateCycles.toFloat(), 1f..10f) {
        onCycles(it.toInt())
    }
    SettingsSliderRow("Interval", "${settings.animateInterval} ms", settings.animateInterval.toFloat(), 100f..1000f) {
        onInterval(it.toInt())
    }
}

@Composable
private fun PerformanceSection(
    settings: AppSettings,
    onStartup: (StartupBehavior) -> Unit
) {
    Text("Startup behavior", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))
    StartupBehavior.entries.forEach { behavior ->
        val selected = settings.startupBehavior == behavior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                )
                .clickable { onStartup(behavior) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(behavior.displayName)
            Switch(checked = selected, onCheckedChange = { onStartup(behavior) })
        }
    }
}

@Composable
private fun DeveloperSection(
    settings: AppSettings,
    onDefaultZone: (GlyphZone?) -> Unit
) {
    Text("Default zone for quick actions", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = settings.defaultZone == null,
            onClick = { onDefaultZone(null) },
            label = { Text("All") }
        )
        GlyphZone.entries.forEach { zone ->
            FilterChip(
                selected = settings.defaultZone == zone,
                onClick = { onDefaultZone(zone) },
                label = { Text("Arc ${zone.name}") }
            )
        }
    }
}

@Composable
private fun AmbientSection(
    ritual: String,
    brightness: Float,
    onRitual: (String) -> Unit,
    onBrightness: (Float) -> Unit
) {
    Text("Ritual", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("WAKE", "FOCUS", "WIND_DOWN", "CHARGING").forEach { id ->
            FilterChip(
                selected = ritual == id,
                onClick = { onRitual(id) },
                label = { Text(id.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }) }
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    SettingsSliderRow(
        "Brightness ceiling",
        "${(brightness * 100).toInt()}%",
        brightness,
        0.1f..1f,
        onBrightness
    )
}

@Composable
private fun TipsSection(onResetTips: () -> Unit) {
    Text(
        "Replay the guided doughnut tour that lights arcs A → B → C on first launch.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onResetTips) {
        Text("Reset tips & replay tour")
    }
}

@Composable
private fun AboutSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Glyph Studio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Created by Anirudh Rao",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Nothing Phone (3a) / (3a) Pro · Glyph Developer Kit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Design · Animate · Perform",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Terms & notices",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Glyph Studio is an independent companion app for creative lighting on " +
                "compatible Nothing phones. Nothing, Glyph, and related marks are trademarks " +
                "of their respective owners. This project is not affiliated with or endorsed " +
                "by Nothing Technology Limited.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Microphone access is used only while the Visualizer is running, to map " +
                "audio energy onto Glyph channels. Audio is processed on-device and is not " +
                "uploaded. You can revoke microphone permission at any time in system settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Use Glyph lighting responsibly. Prolonged high-brightness patterns may " +
                "increase device temperature and battery use. The author provides this software " +
                "as-is without warranty of merchantability or fitness for a particular purpose.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Questions or feedback: contact Anirudh Rao via the project repository.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

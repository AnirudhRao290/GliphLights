package com.example.gliphlights.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.gliphlights.ui.components.GlyphCard
import com.example.gliphlights.ui.components.ScreenHeader
import com.example.gliphlights.ui.components.SectionLabel
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            subtitle = "Preferences sync across the app"
        )

        when (val state = uiState) {
            is SettingsUiState.Loading -> {
                Text(
                    text = "Loading…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        Spacer(modifier = Modifier.height(12.dp))
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
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column {
            SectionLabel("Appearance")
            GlyphCard {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Applies instantly across Glyph Control",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
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
        }

        Column {
            SectionLabel("Animation defaults")
            GlyphCard {
                SettingsSliderRow(
                    label = "Period",
                    valueLabel = "${settings.animatePeriod} ms",
                    value = settings.animatePeriod.toFloat(),
                    range = 500f..5000f,
                    steps = 9,
                    onChange = { onAnimatePeriodChange(it.toInt()) }
                )
                SettingsSliderRow(
                    label = "Cycles",
                    valueLabel = "${settings.animateCycles}",
                    value = settings.animateCycles.toFloat(),
                    range = 1f..10f,
                    steps = 8,
                    onChange = { onAnimateCyclesChange(it.toInt()) }
                )
                SettingsSliderRow(
                    label = "Interval",
                    valueLabel = "${settings.animateInterval} ms",
                    value = settings.animateInterval.toFloat(),
                    range = 100f..1000f,
                    steps = 8,
                    onChange = { onAnimateIntervalChange(it.toInt()) }
                )
            }
        }

        Column {
            SectionLabel("Defaults")
            GlyphCard {
                Text(
                    text = "Default zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                ChoiceRow(
                    options = listOf(null to "All") + GlyphZone.entries.map { it to "Zone ${it.name}" },
                    selectedKey = settings.defaultZone,
                    onSelect = { onDefaultZoneChange(it.first) },
                    labelOf = { it.second },
                    keyOf = { it.first }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Startup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                ChoiceRow(
                    options = StartupBehavior.entries.toList(),
                    selectedKey = settings.startupBehavior,
                    onSelect = onStartupBehaviorChange,
                    labelOf = { it.displayName },
                    keyOf = { it }
                )
            }
        }

        Column {
            SectionLabel("About")
            GlyphCard {
                Text(
                    text = "Glyph Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nothing Phone (3a) Pro · Glyph Studio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selectedKey: Any?,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
    keyOf: (T) -> Any?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { option ->
                    val selected = keyOf(option) == selectedKey
                    val shape = RoundedCornerShape(14.dp)
                    Text(
                        text = labelOf(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = shape
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

package com.example.gliphlights.ui.screens

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.viewmodel.PerformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformScreen(
    onBack: () -> Unit,
    viewModel: PerformViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    var intensity by remember { mutableFloatStateOf(1f) }
    var tempo by remember { mutableFloatStateOf(1f) }
    var dimUi by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sm = context.getSystemService(SensorManager::class.java)
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Face-down heuristic: z strongly negative while flat
                dimUi = event.values[2] < -8.5f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (accel != null) sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Perform", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::panicOff) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Panic Off")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .alpha(if (dimUi) 0.18f else 1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (ui.pads.isEmpty()) "Pin presets from Dashboard to fill the pad deck"
                else "Tap a pad · Glyph keeps playing when UI dims",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ui.pads, key = { it.id }) { pad ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (ui.activePadId == pad.id) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                viewModel.triggerPad(pad.id, intensity, tempo)
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                pad.type.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ui.activePadId == pad.id) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                pad.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (ui.activePadId == pad.id) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            Text("Intensity ${(intensity * 100).toInt()}%")
            Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 0.1f..1f)
            Text("Tempo ${"%.1f".format(tempo)}×")
            Slider(value = tempo, onValueChange = { tempo = it }, valueRange = 0.25f..2f)
            val tapTempo = remember { com.example.gliphlights.audio.TapTempo() }
            var tapBpm by remember { mutableStateOf<Float?>(null) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val bpm = tapTempo.tap()
                        tapBpm = bpm
                        if (bpm != null) {
                            tempo = tapTempo.tempoMultiplier(bpm)
                        }
                    }
                ) {
                    Text(if (tapBpm != null) "Tap ${tapBpm!!.toInt()} BPM" else "Tap tempo")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(onClick = viewModel::panicOff)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Panic Off",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

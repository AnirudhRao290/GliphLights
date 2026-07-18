package com.example.gliphlights.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.viewmodel.FocusTimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    onBack: () -> Unit,
    viewModel: FocusTimerViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.statusMessage) {
        ui.statusMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    val mins = ui.remainingSec / 60
    val secs = ui.remainingSec % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Timer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "%02d:%02d".format(mins, secs),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { ui.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("${ui.progressPercent}% · doughnut fills as you focus")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 25).forEach { m ->
                    FilterChip(
                        selected = ui.durationSec == m * 60 && !ui.isRunning,
                        onClick = { viewModel.setPresetMinutes(m) },
                        enabled = !ui.isRunning,
                        label = { Text("${m}m") }
                    )
                }
            }

            Text("Custom (1–90 min)", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = (ui.durationSec / 60f).coerceIn(1f, 90f),
                onValueChange = { viewModel.setCustomMinutes(it) },
                valueRange = 1f..90f,
                enabled = !ui.isRunning,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { if (ui.isRunning) viewModel.pause() else viewModel.start() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (ui.isRunning) "Pause" else "Start")
                }
                OutlinedButton(
                    onClick = viewModel::reset,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Reset")
                }
            }

            if (ui.completed) {
                TextButton(onClick = viewModel::saveCompletionFrame) {
                    Text("Save end frame as preset")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Uses Glyph progress arc · pre-empted by Editor / Path / Viz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

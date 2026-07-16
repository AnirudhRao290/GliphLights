package com.example.gliphlights.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gliphlights.editor.render.GlyphMapView
import com.example.gliphlights.viewmodel.GlyphEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlyphEditorScreen(
    onBack: () -> Unit,
    viewModel: GlyphEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Glyph Editor",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            EditorBottomBar(
                activeCount = uiState.activeCount,
                isSessionActive = uiState.isSessionActive,
                onStartSession = viewModel::startSession,
                onStopSession = viewModel::stopSession,
                onClearAll = viewModel::clearAll,
                onSendToDevice = viewModel::sendToDevice
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GlyphMapView(
                layout = uiState.layout,
                activeChannels = uiState.activeChannels,
                onGestureEvent = viewModel::handleGestureEvent,
                modifier = Modifier.weight(1f),
                view = view
            )
        }
    }
}

@Composable
private fun EditorBottomBar(
    activeCount: Int,
    isSessionActive: Boolean,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onClearAll: () -> Unit,
    onSendToDevice: () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active nodes: $activeCount / 36",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Clear")
                }

                androidx.compose.material3.Button(
                    onClick = if (isSessionActive) onStopSession else onStartSession,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = if (isSessionActive) "Stop" else "Start")
                }

                androidx.compose.material3.Button(
                    onClick = onSendToDevice,
                    modifier = Modifier.weight(1f),
                    enabled = activeCount > 0 && isSessionActive
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Send")
                }
            }
        }
    }
}

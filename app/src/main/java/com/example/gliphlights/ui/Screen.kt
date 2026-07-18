package com.example.gliphlights.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    data object Controls : Screen("controls", "Controls", Icons.Default.Lightbulb)
    data object Visualizer : Screen("visualizer", "Visualizer", Icons.Default.MusicNote)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Editor : Screen("editor", "Editor", Icons.Default.Lightbulb)
    data object PathBuilder : Screen("path_builder", "Path Builder", Icons.Default.Create)
    data object PhysicsLab : Screen("physics_lab", "Physics Lab", Icons.Default.Science)
    data object Perform : Screen("perform", "Perform", Icons.Default.MusicNote)
    data object Tour : Screen("tour", "Tour", Icons.Default.Lightbulb)
    data object FocusTimer : Screen("focus_timer", "Focus Timer", Icons.Default.Timelapse)
    data object Presence : Screen("presence", "Presence", Icons.Default.Person)
    data object GlyphClock : Screen("glyph_clock", "Glyph Clock", Icons.Default.Schedule)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Controls,
    Screen.Visualizer,
    Screen.Settings
)

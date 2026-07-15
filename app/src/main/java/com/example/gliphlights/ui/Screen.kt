package com.example.gliphlights.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    data object Controls : Screen("controls", "Controls", Icons.Default.Lightbulb)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Controls,
    Screen.Settings
)

package com.example.gliphlights

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gliphlights.pathbuilder.PathBuilderScreen
import com.example.gliphlights.physics.PhysicsLabScreen
import com.example.gliphlights.presets.GlyphPackShare
import com.example.gliphlights.repository.SettingsRepository
import com.example.gliphlights.ui.Screen
import com.example.gliphlights.ui.bottomNavItems
import com.example.gliphlights.ui.screens.ControlsScreen
import com.example.gliphlights.ui.screens.DashboardScreen
import com.example.gliphlights.ui.screens.DoughnutTourScreen
import com.example.gliphlights.ui.screens.FocusTimerScreen
import com.example.gliphlights.ui.screens.GlyphClockScreen
import com.example.gliphlights.ui.screens.GlyphEditorScreen
import com.example.gliphlights.ui.screens.MusicVisualizerScreen
import com.example.gliphlights.ui.screens.PerformScreen
import com.example.gliphlights.ui.screens.PresenceScreen
import com.example.gliphlights.ui.screens.SettingsScreen
import com.example.gliphlights.ui.theme.GliphLightsTheme
import com.example.gliphlights.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var glyphPackShare: GlyphPackShare

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themePreference by mainViewModel.themePreference.collectAsState()
            GliphLightsTheme(themePreference = themePreference) {
                MainScreen(
                    importUri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data,
                    onImport = { uri -> glyphPackShare.importFromUri(uri) },
                    tourCompletedFlow = { settingsRepository.tourCompleted.first() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainScreen(
    importUri: Uri? = null,
    onImport: suspend (Uri) -> Unit = {},
    tourCompletedFlow: suspend () -> Boolean = { true }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    var tourChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!tourCompletedFlow()) {
            navController.navigate(Screen.Tour.route)
        }
        tourChecked = true
    }

    LaunchedEffect(importUri) {
        if (importUri != null) {
            onImport(importUri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = navBackStackEntry?.destination?.hierarchy
                            ?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = {
                                Text(
                                    text = screen.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToEditor = { navController.navigate(Screen.Editor.route) },
                    onNavigateToPathBuilder = { navController.navigate(Screen.PathBuilder.route) },
                    onNavigateToPhysicsLab = { navController.navigate(Screen.PhysicsLab.route) },
                    onNavigateToPerform = { navController.navigate(Screen.Perform.route) },
                    onNavigateToFocus = { navController.navigate(Screen.FocusTimer.route) },
                    onNavigateToPresence = { navController.navigate(Screen.Presence.route) },
                    onNavigateToClock = { navController.navigate(Screen.GlyphClock.route) },
                    onEditPreset = { preset ->
                        when (preset.type) {
                            com.example.gliphlights.presets.PresetType.PATH ->
                                navController.navigate(Screen.PathBuilder.route)
                            com.example.gliphlights.presets.PresetType.PHYSICS ->
                                navController.navigate(Screen.PhysicsLab.route)
                            else -> navController.navigate(Screen.Editor.route)
                        }
                    }
                )
            }
            composable(Screen.Controls.route) {
                ControlsScreen()
            }
            composable(Screen.Visualizer.route) {
                MusicVisualizerScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onReplayTour = { navController.navigate(Screen.Tour.route) }
                )
            }
            composable(Screen.Editor.route) {
                GlyphEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.PathBuilder.route) {
                PathBuilderScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.PhysicsLab.route) {
                PhysicsLabScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Perform.route) {
                PerformScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.FocusTimer.route) {
                FocusTimerScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Presence.route) {
                PresenceScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.GlyphClock.route) {
                GlyphClockScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Tour.route) {
                DoughnutTourScreen(
                    onFinished = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Tour.route) { inclusive = true }
                        }
                    },
                    onGoPathBuilder = {
                        navController.navigate(Screen.PathBuilder.route) {
                            popUpTo(Screen.Tour.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

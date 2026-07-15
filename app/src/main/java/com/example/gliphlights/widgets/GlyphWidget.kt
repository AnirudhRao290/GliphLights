package com.example.gliphlights.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.gliphlights.MainActivity
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences

class GlyphWidget : GlanceAppWidget() {

    override val sizeMode = androidx.glance.appwidget.SizeMode.Single

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val localContext = LocalContext.current
            GlanceTheme {
                WidgetContent(localContext)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val prefs = currentState<Preferences>()
        val isActive = prefs[IS_ACTIVE_KEY] ?: false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
                .clickable(actionStartActivity(launchIntent)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isActive) "\u25CF" else "\u25CB",
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(
                            Color(if (isActive) 0xFF4CAF50 else 0xFF757575)
                        )
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = if (isActive) "Glyph Active" else "Glyph Inactive",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color.White)
                    )
                )

                Spacer(modifier = GlanceModifier.height(16.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(40.dp)
                            .background(Color(0xFF2196F3))
                            .clickable(actionRunCallback<ToggleGlyphAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Toggle",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorProvider(Color.White)
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(40.dp)
                            .background(Color(0xFFF44336))
                            .clickable(actionRunCallback<TurnOffGlyphAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Turn Off",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorProvider(Color.White)
                            )
                        )
                    }
                }
            }
        }
    }
}

data class GlyphWidgetState(
    val isActive: Boolean = false
)

class ToggleGlyphAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { currentPrefs ->
            val current = currentPrefs[IS_ACTIVE_KEY] ?: false
            currentPrefs.toMutablePreferences().apply {
                this[IS_ACTIVE_KEY] = !current
            }
        }
        GlyphWidget().update(context, glanceId)
    }
}

class TurnOffGlyphAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[IS_ACTIVE_KEY] = false
            }
        }
        GlyphWidget().update(context, glanceId)
    }
}

private val IS_ACTIVE_KEY = booleanPreferencesKey("is_active")

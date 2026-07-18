package com.example.gliphlights.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import com.example.gliphlights.di.GlyphEntryPoint
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID

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

internal fun glyphEntryPoint(context: Context): GlyphEntryPoint {
    return EntryPointAccessors.fromApplication(
        context.applicationContext,
        GlyphEntryPoint::class.java
    )
}

internal suspend fun ensureGlyphSession(repository: GlyphRepository): SdkResult<Unit> {
    if (repository.isSessionActive.value) return SdkResult.Success(Unit)
    if (!repository.isConnected.value) {
        val init = repository.initialize()
        if (init is SdkResult.Error) return init
        val reg = repository.register()
        if (reg is SdkResult.Error) return reg
    }
    return repository.openSession()
}

internal suspend fun syncWidgetActiveState(
    context: Context,
    glanceId: GlanceId,
    repository: GlyphRepository
) {
    val active = repository.glyphState.value.isActive
    updateAppWidgetState(context, glanceId) { prefs ->
        prefs.toMutablePreferences().apply {
            this[IS_ACTIVE_KEY] = active
        }
    }
    GlyphWidget().update(context, glanceId)
}

private suspend fun withTileOwnership(
    arbiter: GlyphSessionArbiter,
    block: suspend () -> Unit
) {
    val token = UUID.randomUUID().toString()
    when (val acquire = arbiter.acquire(GlyphClient.TILE_WIDGET, token)) {
        is AcquireResult.Denied -> return
        is AcquireResult.Granted -> {
            try {
                block()
            } finally {
                arbiter.release(GlyphClient.TILE_WIDGET, acquire.token)
            }
        }
    }
}

class ToggleGlyphAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entry = glyphEntryPoint(context)
        val repository = entry.glyphRepository()
        val arbiter = entry.glyphSessionArbiter()

        withTileOwnership(arbiter) {
            val session = ensureGlyphSession(repository)
            if (session is SdkResult.Success) {
                repository.toggleAll()
            }
        }
        syncWidgetActiveState(context, glanceId, repository)
    }
}

class TurnOffGlyphAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entry = glyphEntryPoint(context)
        val repository = entry.glyphRepository()
        val arbiter = entry.glyphSessionArbiter()

        withTileOwnership(arbiter) {
            val session = ensureGlyphSession(repository)
            if (session is SdkResult.Success) {
                repository.turnOff()
            }
        }
        syncWidgetActiveState(context, glanceId, repository)
    }
}

private val IS_ACTIVE_KEY = booleanPreferencesKey("is_active")

package com.example.gliphlights.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.gliphlights.MainActivity
import com.example.gliphlights.R
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.repository.SettingsRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.sin

enum class AmbientRitual(val displayName: String) {
    WAKE("Wake"),
    FOCUS("Focus"),
    WIND_DOWN("Wind-down"),
    CHARGING("Charging")
}

@AndroidEntryPoint
class AmbientRitualService : Service() {

    @Inject lateinit var glyphRepository: GlyphRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var sessionArbiter: GlyphSessionArbiter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null
    private var ownershipToken: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelfSafe()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIF_ID, buildNotification("Ambient ritual running"))
                startLoop()
            }
        }
        return START_STICKY
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            when (val acquired = sessionArbiter.acquire(
                GlyphClient.AMBIENT,
                UUID.randomUUID().toString()
            )) {
                is AcquireResult.Granted -> ownershipToken = acquired.token
                is AcquireResult.Denied -> {
                    stopSelfSafe()
                    return@launch
                }
            }
            if (!glyphRepository.isSessionActive.value) {
                if (!glyphRepository.isConnected.value) {
                    glyphRepository.initialize()
                    glyphRepository.register()
                }
                glyphRepository.openSession()
            }

            val ritualName = settingsRepository.ambientRitual.first()
            val ritual = try {
                AmbientRitual.valueOf(ritualName)
            } catch (_: Exception) {
                AmbientRitual.FOCUS
            }
            val brightness = settingsRepository.ambientBrightness.first()
            var t = 0f
            while (isActive) {
                if (!sessionArbiter.canWrite(GlyphClient.AMBIENT, ownershipToken)) break
                when (ritual) {
                    AmbientRitual.WAKE -> {
                        val pct = ((t % 100f)).toInt().coerceIn(0, 100)
                        glyphRepository.displayProgress(pct, reverse = false)
                        t += 1.5f * brightness
                    }
                    AmbientRitual.FOCUS -> {
                        val breath = ((sin(t.toDouble()) + 1.0) / 2.0).toFloat()
                        val channels = GlyphZone.C.channels
                        val keep = (channels.size * breath * brightness).toInt().coerceAtLeast(1)
                        glyphRepository.setChannels(channels.take(keep))
                        t += 0.08f
                    }
                    AmbientRitual.WIND_DOWN -> {
                        val all = GlyphZone.A.channels + GlyphZone.B.channels + GlyphZone.C.channels
                        val idx = (t.toInt()) % all.size
                        val trail = listOf(all[idx], all[(idx + 1) % all.size])
                        glyphRepository.setChannels(trail)
                        t += 0.6f * brightness
                    }
                    AmbientRitual.CHARGING -> {
                        val pct = ((t % 100f)).toInt().coerceIn(0, 100)
                        glyphRepository.displayProgress(pct, reverse = true)
                        t += 1.2f * brightness
                    }
                }
                delay(80L) // capped FPS ~12.5
            }
        }
    }

    private fun buildNotification(content: String): Notification {
        val channelId = "ambient_rituals"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Ambient Rituals", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, AmbientRitualService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Glyph Studio")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_tile_glyph_active)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    private fun stopSelfSafe() {
        loopJob?.cancel()
        val token = ownershipToken
        ownershipToken = null
        scope.launch {
            if (token != null) sessionArbiter.release(GlyphClient.AMBIENT, token)
            if (glyphRepository.isSessionActive.value) glyphRepository.turnOff()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 42
        const val ACTION_STOP = "com.example.gliphlights.ambient.STOP"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AmbientRitualService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, AmbientRitualService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}

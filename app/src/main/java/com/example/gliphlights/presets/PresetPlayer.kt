package com.example.gliphlights.presets

import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.pathbuilder.AnimationEngine
import com.example.gliphlights.pathbuilder.AnimationGenerator
import com.example.gliphlights.repository.GlyphRepository
import com.example.gliphlights.sdk.AcquireResult
import com.example.gliphlights.sdk.GlyphClient
import com.example.gliphlights.sdk.GlyphSessionArbiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays unified presets onto Glyph hardware via setChannels.
 */
@Singleton
class PresetPlayer @Inject constructor(
    private val glyphRepository: GlyphRepository,
    private val sessionArbiter: GlyphSessionArbiter
) {
    private var playJob: Job? = null
    private var ownershipToken: String? = null
    private val generator = AnimationGenerator()

    suspend fun playOnce(
        preset: GlyphPreset,
        scope: CoroutineScope,
        client: GlyphClient = GlyphClient.PERFORM,
        intensity: Float = 1f,
        tempo: Float = 1f,
        loop: Boolean = false
    ): SdkResult<Unit> {
        stop()
        when (val acquired = sessionArbiter.acquire(client, UUID.randomUUID().toString())) {
            is AcquireResult.Granted -> ownershipToken = acquired.token
            is AcquireResult.Denied -> {
                return SdkResult.Error(Exception(acquired.reason), acquired.reason)
            }
        }
        val session = ensureSession()
        if (session is SdkResult.Error) {
            release(client)
            return session
        }

        return when (preset.type) {
            PresetType.EDITOR -> {
                val channels = scaleChannels(preset.channels, intensity)
                if (channels.isEmpty()) glyphRepository.turnOff()
                else glyphRepository.setChannels(channels.toList())
            }
            PresetType.PATH -> {
                playPath(preset, scope, intensity, tempo, loop, client)
                SdkResult.Success(Unit)
            }
            PresetType.PHYSICS, PresetType.VISUALIZER -> {
                // Snapshot recipe → flash representative channels / C-arc pulse
                val channels = if (preset.channels.isNotEmpty()) {
                    preset.channels
                } else {
                    (0..19).toSet() // C arc as fallback pulse
                }
                glyphRepository.setChannels(scaleChannels(channels, intensity).toList())
            }
        }
    }

    fun stop(client: GlyphClient = GlyphClient.PERFORM) {
        playJob?.cancel()
        playJob = null
        release(client)
    }

    private fun playPath(
        preset: GlyphPreset,
        scope: CoroutineScope,
        intensity: Float,
        tempo: Float,
        loop: Boolean,
        client: GlyphClient
    ) {
        val settings = preset.pathSettings.copy(
            animationSpeed = (preset.pathSettings.animationSpeed * tempo).coerceIn(0.25f, 4f),
            infiniteLoop = loop || preset.pathSettings.infiniteLoop,
            brightness = (preset.pathSettings.brightness * intensity).coerceIn(0.1f, 1f)
        )
        val model = generator.generate(preset.pathNodes, settings)
        val engine = AnimationEngine(scope)
        engine.setModel(model)
        engine.setSpeed(settings.animationSpeed)
        playJob = scope.launch {
            val sync = launch {
                engine.snapshot
                    .map { snap ->
                        buildList {
                            for (i in snap.nodeAlphas.indices) {
                                if (snap.nodeAlphas[i] >= 0.18f * intensity.coerceAtLeast(0.2f)) {
                                    add(i)
                                }
                            }
                        }
                    }
                    .distinctUntilChanged()
                    .collectLatest { channels ->
                        if (!sessionArbiter.canWrite(client, ownershipToken)) return@collectLatest
                        if (channels.isEmpty()) glyphRepository.turnOff()
                        else glyphRepository.setChannels(channels)
                    }
            }
            engine.restart()
            if (!loop && !settings.infiniteLoop) {
                val duration = (model.totalDurationMs / settings.animationSpeed).toLong().coerceAtLeast(200L)
                delay(duration)
                sync.cancel()
                engine.pause()
                engine.release()
                glyphRepository.turnOff()
                release(client)
            }
        }
    }

    private suspend fun ensureSession(): SdkResult<Unit> {
        if (glyphRepository.isSessionActive.value) return SdkResult.Success(Unit)
        if (!glyphRepository.isConnected.value) {
            val init = glyphRepository.initialize()
            if (init is SdkResult.Error) return init
            val reg = glyphRepository.register()
            if (reg is SdkResult.Error) return reg
        }
        return glyphRepository.openSession()
    }

    private fun scaleChannels(channels: Set<Int>, intensity: Float): Set<Int> {
        if (intensity >= 0.95f) return channels
        if (intensity <= 0.05f) return emptySet()
        val sorted = channels.sorted()
        val keep = (sorted.size * intensity).toInt().coerceAtLeast(1)
        return sorted.take(keep).toSet()
    }

    private fun release(client: GlyphClient) {
        val token = ownershipToken
        ownershipToken = null
        if (token != null) {
            // fire-and-forget via blocking-safe launch from callers' scopes preferred;
            // use a dedicated mini-scope here for stop() from non-suspend.
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
            ).launch {
                sessionArbiter.release(client, token)
            }
        }
    }
}

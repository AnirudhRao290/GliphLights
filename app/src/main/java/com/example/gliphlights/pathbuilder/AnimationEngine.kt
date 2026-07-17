package com.example.gliphlights.pathbuilder

import com.example.gliphlights.pathbuilder.model.AnimationModel
import com.example.gliphlights.pathbuilder.model.EngineSnapshot
import com.example.gliphlights.pathbuilder.model.InterpolationMode
import com.example.gliphlights.pathbuilder.model.PlaybackMode
import com.example.gliphlights.pathbuilder.model.TransitionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Software playback clock for path animations. No Glyph SDK.
 */
class AnimationEngine(
    private val scope: CoroutineScope
) {
    companion object {
        private const val FRAME_MS = 16L
    }

    private val alphas = FloatArray(EngineSnapshot.CHANNEL_COUNT)
    private val _snapshot = MutableStateFlow(EngineSnapshot.idle())
    val snapshot: StateFlow<EngineSnapshot> = _snapshot.asStateFlow()

    private var model: AnimationModel = AnimationModel.empty()
    private var playheadMs = 0L
    private var playing = false
    private var direction = 1 // 1 forward, -1 reverse (ping-pong)
    private var tickJob: Job? = null

    fun setModel(newModel: AnimationModel) {
        model = newModel
        playheadMs = 0L
        direction = if (newModel.settings.reversePlayback) -1 else 1
        if (newModel.settings.reversePlayback && newModel.totalDurationMs > 0) {
            playheadMs = newModel.totalDurationMs
        }
        evaluateAndEmit()
    }

    fun play() {
        if (model.isEmpty) return
        playing = true
        ensureTicker()
        evaluateAndEmit()
    }

    fun pause() {
        playing = false
        evaluateAndEmit()
    }

    fun restart() {
        direction = if (model.settings.reversePlayback) -1 else 1
        playheadMs = if (direction < 0) model.totalDurationMs else 0L
        evaluateAndEmit()
        play()
    }

    fun seek(ms: Long) {
        val total = model.totalDurationMs
        playheadMs = ms.coerceIn(0L, max(total, 0L))
        evaluateAndEmit()
    }

    fun setSpeed(speed: Float) {
        // Applied via settings on model; update live snapshot speed
        val s = speed.coerceIn(0.1f, 4f)
        _snapshot.value = _snapshot.value.copy(speed = s)
    }

    fun stop() {
        playing = false
        tickJob?.cancel()
        tickJob = null
        playheadMs = 0L
        alphas.fill(0f)
        emit()
    }

    fun release() {
        stop()
    }

    private fun ensureTicker() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            var last = System.nanoTime()
            while (isActive) {
                delay(FRAME_MS)
                if (!playing) continue
                val now = System.nanoTime()
                val dtMs = ((now - last) / 1_000_000L).coerceIn(1L, 50L)
                last = now
                advance(dtMs)
            }
        }
    }

    private fun advance(dtMs: Long) {
        val total = model.totalDurationMs
        if (total <= 0L) {
            playing = false
            evaluateAndEmit()
            return
        }

        val speed = (_snapshot.value.speed * model.settings.animationSpeed).coerceIn(0.1f, 4f)
        playheadMs += (dtMs * speed * direction).toLong()

        when (model.settings.playbackMode) {
            PlaybackMode.ONCE -> {
                if (playheadMs >= total) {
                    playheadMs = total
                    playing = false
                } else if (playheadMs <= 0L) {
                    playheadMs = 0L
                    playing = false
                }
            }
            PlaybackMode.LOOP -> {
                if (playheadMs >= total) playheadMs %= total
                if (playheadMs < 0L) playheadMs = (playheadMs % total + total) % total
            }
            PlaybackMode.PING_PONG -> {
                if (playheadMs >= total) {
                    playheadMs = total
                    direction = -1
                } else if (playheadMs <= 0L) {
                    playheadMs = 0L
                    direction = 1
                }
            }
        }
        evaluateAndEmit()
    }

    private fun evaluateAndEmit() {
        alphas.fill(0f)
        val trailLen = model.settings.trailLength.coerceAtLeast(1)
        val trailFade = model.settings.trailFade.coerceIn(0f, 1f)
        val linear = model.settings.interpolation == InterpolationMode.LINEAR

        val frames = model.frames
        for (i in frames.indices) {
            val frame = frames[i]
            val alpha = sampleFrameAlpha(frame, playheadMs, linear)
            if (alpha <= 0f) continue
            val idx = frame.sdkIndex
            if (idx in alphas.indices) {
                alphas[idx] = max(alphas[idx], alpha * frame.brightness)
            }
        }

        // Trail: dim previous path nodes relative to current head
        if (trailLen > 1 && frames.isNotEmpty()) {
            val headIndex = frames.indexOfLast { playheadMs >= it.startMs }
                .coerceAtLeast(0)
            for (t in 1 until trailLen) {
                val pi = headIndex - t
                if (pi < 0) break
                val fade = (1f - t.toFloat() / trailLen) * trailFade
                val idx = frames[pi].sdkIndex
                if (idx in alphas.indices) {
                    alphas[idx] = max(alphas[idx], fade * frames[pi].brightness)
                }
            }
        }

        emit()
    }

    private fun sampleFrameAlpha(
        frame: com.example.gliphlights.pathbuilder.model.AnimationFrame,
        t: Long,
        linear: Boolean
    ): Float {
        if (t < frame.startMs || t > frame.endMs) return 0f
        val local = t - frame.startMs
        if (frame.transition == TransitionType.CUT || !linear) {
            return 1f
        }
        val fadeIn = frame.fadeInMs
        val fadeOut = frame.fadeOutMs
        val dur = frame.durationMs
        return when {
            fadeIn > 0 && local < fadeIn -> local.toFloat() / fadeIn
            fadeOut > 0 && local > dur - fadeOut -> {
                val rem = dur - local
                (rem.toFloat() / fadeOut).coerceIn(0f, 1f)
            }
            else -> 1f
        }
    }

    private fun emit() {
        _snapshot.value = EngineSnapshot(
            playheadMs = playheadMs,
            nodeAlphas = alphas.copyOf(),
            isPlaying = playing,
            speed = _snapshot.value.speed,
            totalDurationMs = model.totalDurationMs
        )
    }
}

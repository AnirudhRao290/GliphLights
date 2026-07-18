package com.example.gliphlights.pathbuilder.model

/**
 * High-frequency touch sample. Allocated outside the hot path when possible.
 */
data class SamplePoint(
    val x: Float,
    val y: Float,
    val tNanos: Long
)

/**
 * A Glyph node reference in a drawn path (SDK-agnostic).
 */
data class PathNode(
    val nodeId: String,
    val sdkIndex: Int,
    val regionName: String
)

enum class TransitionType {
    CUT,
    FADE
}

enum class PlaybackMode {
    ONCE,
    LOOP,
    PING_PONG
}

enum class InterpolationMode {
    STEP,
    LINEAR
}

/**
 * Configurable path → animation parameters.
 */
data class PathSettings(
    val animationSpeed: Float = 1f,
    val nodeDurationMs: Long = 120L,
    val fadeDurationMs: Long = 80L,
    /** Attack (fade-in). When &lt; 0, falls back to [fadeDurationMs]. */
    val attackMs: Long = -1L,
    /** Release (fade-out). When &lt; 0, falls back to [fadeDurationMs]. */
    val releaseMs: Long = -1L,
    /** Fraction of node duration spent at full brightness after attack (0–1). */
    val sustainRatio: Float = 0.55f,
    val brightness: Float = 1f,
    val trailLength: Int = 3,
    val trailFade: Float = 0.55f,
    val repeatCount: Int = 1,
    val infiniteLoop: Boolean = false,
    val reversePlayback: Boolean = false,
    val pingPong: Boolean = false,
    val interpolation: InterpolationMode = InterpolationMode.LINEAR,
    val smoothingStrength: Float = 0.35f,
    val samplingDensityPx: Float = 4f,
    val minimumNodeDistance: Int = 1
) {
    val effectiveAttackMs: Long
        get() = if (attackMs >= 0) attackMs else fadeDurationMs

    val effectiveReleaseMs: Long
        get() = if (releaseMs >= 0) releaseMs else fadeDurationMs

    val playbackMode: PlaybackMode
        get() = when {
            pingPong -> PlaybackMode.PING_PONG
            infiniteLoop || repeatCount <= 0 -> PlaybackMode.LOOP
            else -> PlaybackMode.ONCE
        }
}

data class AnimationFrame(
    val nodeId: String,
    val sdkIndex: Int,
    val startMs: Long,
    val durationMs: Long,
    val brightness: Float = 1f,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0,
    val transition: TransitionType = TransitionType.FADE
) {
    val endMs: Long get() = startMs + durationMs
}

/**
 * Timeline animation produced by Path Builder. Never talks to the Glyph SDK.
 */
data class AnimationModel(
    val frames: List<AnimationFrame> = emptyList(),
    val settings: PathSettings = PathSettings(),
    val pathNodes: List<PathNode> = emptyList()
) {
    val totalDurationMs: Long
        get() = frames.maxOfOrNull { it.endMs } ?: 0L

    val isEmpty: Boolean get() = frames.isEmpty()

    companion object {
        fun empty(settings: PathSettings = PathSettings()) =
            AnimationModel(frames = emptyList(), settings = settings)
    }
}

/**
 * Per-tick preview state. [nodeAlphas] is reused by the engine (index = sdkIndex).
 */
data class EngineSnapshot(
    val playheadMs: Long = 0L,
    val nodeAlphas: FloatArray = FloatArray(CHANNEL_COUNT),
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    val totalDurationMs: Long = 0L
) {
    companion object {
        const val CHANNEL_COUNT = 36

        fun idle() = EngineSnapshot()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EngineSnapshot) return false
        return playheadMs == other.playheadMs &&
            isPlaying == other.isPlaying &&
            speed == other.speed &&
            totalDurationMs == other.totalDurationMs &&
            nodeAlphas.contentEquals(other.nodeAlphas)
    }

    override fun hashCode(): Int {
        var result = playheadMs.hashCode()
        result = 31 * result + nodeAlphas.contentHashCode()
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + totalDurationMs.hashCode()
        return result
    }
}

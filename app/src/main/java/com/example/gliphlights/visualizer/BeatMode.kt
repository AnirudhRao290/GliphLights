package com.example.gliphlights.visualizer

import com.example.gliphlights.models.GlyphZone

class BeatMode : VisualizationMode {
    override val displayName: String = "Beat"

    private val allChannels = GlyphZone.A.channels + GlyphZone.B.channels + GlyphZone.C.channels

    private var wasBelowThreshold = true
    private var lastBeatTime = 0L
    private val beatCooldownMs = 150L

    override fun render(amplitude: Float, timestamp: Long): GlyphCommand? {
        val isAboveThreshold = amplitude > 0.6f
        val isBelowThreshold = amplitude < 0.3f

        if (isBelowThreshold) {
            wasBelowThreshold = true
        }

        if (isAboveThreshold && wasBelowThreshold) {
            if (timestamp - lastBeatTime > beatCooldownMs) {
                wasBelowThreshold = false
                lastBeatTime = timestamp
                return GlyphCommand(
                    channels = allChannels,
                    brightness = 1.0f
                )
            }
        }

        return null
    }

    override fun reset() {
        wasBelowThreshold = true
        lastBeatTime = 0L
    }
}

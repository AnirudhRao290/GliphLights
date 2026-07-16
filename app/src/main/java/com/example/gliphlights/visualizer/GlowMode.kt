package com.example.gliphlights.visualizer

import com.example.gliphlights.models.GlyphZone

class GlowMode : VisualizationMode {
    override val displayName: String = "Glow"

    private val allChannels = GlyphZone.A.channels + GlyphZone.B.channels + GlyphZone.C.channels
    private val glowAlpha = 0.15f
    private var smoothedBrightness = 0.0f

    override fun render(amplitude: Float, timestamp: Long): GlyphCommand? {
        smoothedBrightness = glowAlpha * amplitude + (1.0f - glowAlpha) * smoothedBrightness

        if (smoothedBrightness <= 0.01f) return null

        return GlyphCommand(
            channels = allChannels,
            brightness = smoothedBrightness
        )
    }

    override fun reset() {
        smoothedBrightness = 0.0f
    }
}

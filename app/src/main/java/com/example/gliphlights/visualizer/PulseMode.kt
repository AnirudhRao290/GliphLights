package com.example.gliphlights.visualizer

import com.example.gliphlights.models.GlyphZone

class PulseMode : VisualizationMode {
    override val displayName: String = "Pulse"

    private val allChannels = GlyphZone.A.channels + GlyphZone.B.channels + GlyphZone.C.channels

    override fun render(amplitude: Float, timestamp: Long): GlyphCommand? {
        if (amplitude <= 0.0f) return null
        return GlyphCommand(
            channels = allChannels,
            brightness = amplitude
        )
    }

    override fun reset() {}
}

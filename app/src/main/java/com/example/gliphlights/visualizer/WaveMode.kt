package com.example.gliphlights.visualizer

import com.example.gliphlights.models.GlyphZone

class WaveMode : VisualizationMode {
    override val displayName: String = "Wave"

    private val zoneA = GlyphZone.A.channels
    private val zoneB = GlyphZone.B.channels
    private val zoneC = GlyphZone.C.channels

    override fun render(amplitude: Float, timestamp: Long): GlyphCommand? {
        if (amplitude <= 0.15f) return null

        val channels = mutableListOf<Int>()
        val brightness = amplitude

        if (amplitude > 0.2f) {
            channels.addAll(zoneA)
        }
        if (amplitude > 0.45f) {
            channels.addAll(zoneB)
        }
        if (amplitude > 0.7f) {
            channels.addAll(zoneC)
        }

        if (channels.isEmpty()) return null

        return GlyphCommand(
            channels = channels,
            brightness = brightness
        )
    }

    override fun reset() {}
}

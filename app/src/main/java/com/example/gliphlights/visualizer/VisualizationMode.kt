package com.example.gliphlights.visualizer

data class GlyphCommand(
    val channels: List<Int>,
    val brightness: Float
)

interface VisualizationMode {
    fun render(amplitude: Float, timestamp: Long): GlyphCommand?
    fun reset()
    val displayName: String
}

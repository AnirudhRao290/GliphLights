package com.example.gliphlights.presets

import com.example.gliphlights.editor.model.GlyphRegion
import com.example.gliphlights.pathbuilder.bake.VisualizerPathBaker
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings

enum class PresenceStatus(val label: String, val emojiHint: String) {
    FREE("Free", "○"),
    BUSY("Busy", "●"),
    DND("DND", "⊘"),
    AFK("AFK", "…"),
    MEETING("Meeting", "◎")
}

/**
 * Built-in looping presence patterns as PATH presets.
 */
object PresencePatterns {

    fun presetFor(status: PresenceStatus): GlyphPreset {
        val (nodes, settings) = when (status) {
            PresenceStatus.FREE -> softBreath() to PathSettings(
                nodeDurationMs = 160L,
                fadeDurationMs = 100L,
                attackMs = 90L,
                releaseMs = 110L,
                trailLength = 3,
                infiniteLoop = true,
                brightness = 0.7f
            )
            PresenceStatus.BUSY -> chaseC() to PathSettings(
                nodeDurationMs = 70L,
                fadeDurationMs = 40L,
                attackMs = 30L,
                releaseMs = 50L,
                trailLength = 5,
                infiniteLoop = true,
                brightness = 1f
            )
            PresenceStatus.DND -> solidA() to PathSettings(
                nodeDurationMs = 400L,
                fadeDurationMs = 200L,
                attackMs = 180L,
                releaseMs = 200L,
                trailLength = 1,
                infiniteLoop = true,
                brightness = 0.45f
            )
            PresenceStatus.AFK -> slowPing() to PathSettings(
                nodeDurationMs = 220L,
                fadeDurationMs = 140L,
                attackMs = 120L,
                releaseMs = 160L,
                trailLength = 2,
                infiniteLoop = true,
                brightness = 0.55f
            )
            PresenceStatus.MEETING -> pulseB() to PathSettings(
                nodeDurationMs = 100L,
                fadeDurationMs = 60L,
                attackMs = 50L,
                releaseMs = 70L,
                trailLength = 4,
                infiniteLoop = true,
                brightness = 0.85f
            )
        }
        return GlyphPreset(
            id = "presence_${status.name.lowercase()}",
            name = status.label,
            type = PresetType.PATH,
            pathNodes = nodes,
            pathSettings = settings,
            pinned = true
        )
    }

    private fun softBreath(): List<PathNode> =
        GlyphRegion.C.channels.mapNotNull { VisualizerPathBaker.pathNodeForSdk(it) }

    private fun chaseC(): List<PathNode> =
        GlyphRegion.C.channels.mapNotNull { VisualizerPathBaker.pathNodeForSdk(it) }

    private fun solidA(): List<PathNode> =
        GlyphRegion.A.channels.mapNotNull { VisualizerPathBaker.pathNodeForSdk(it) }

    private fun slowPing(): List<PathNode> {
        val a = GlyphRegion.A.channels.mapNotNull { VisualizerPathBaker.pathNodeForSdk(it) }
        val c = GlyphRegion.C.channels.take(6).mapNotNull { VisualizerPathBaker.pathNodeForSdk(it) }
        return a + c
    }

    private fun pulseB(): List<PathNode> =
        GlyphRegion.B.channels.mapNotNull { VisualizerPathBaker.pathNodeForSdk(it) } +
            GlyphRegion.C.channels.take(8).mapNotNull { VisualizerPathBaker.pathNodeForSdk(it) }
}

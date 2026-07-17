package com.example.gliphlights.pathbuilder

import com.example.gliphlights.editor.model.GlyphRegion
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.pathbuilder.model.SavedSequence

/**
 * Built-in Phone (3a) Pro glyph sequences — no drawing required.
 */
object PredefinedSequences {

    fun all(): List<SavedSequence> = listOf(
        sweepC(),
        sweepA(),
        sweepB(),
        fullRingChase(),
        bounceA(),
        bounceC(),
        cThenA(),
        reverseRing(),
        zigZag(),
        pulseCenters()
    )

    private fun node(region: GlyphRegion, localIndex: Int) = PathNode(
        nodeId = "${region.name}${localIndex + 1}",
        sdkIndex = region.sdkIndex(localIndex),
        regionName = region.name
    )

    private fun sweep(region: GlyphRegion, reverse: Boolean = false): List<PathNode> {
        val range = 0 until region.nodeCount
        val indices = if (reverse) range.reversed() else range
        return indices.map { node(region, it) }
    }

    private fun preset(
        id: String,
        name: String,
        nodes: List<PathNode>,
        settings: PathSettings = PathSettings(nodeDurationMs = 100L, fadeDurationMs = 60L, trailLength = 4)
    ) = SavedSequence(
        id = id,
        name = name,
        nodes = nodes,
        settings = settings,
        isPreset = true
    )

    private fun sweepC() = preset(
        id = "preset_sweep_c",
        name = "Sweep C (Camera Arc)",
        nodes = sweep(GlyphRegion.C)
    )

    private fun sweepA() = preset(
        id = "preset_sweep_a",
        name = "Sweep A (Right Arc)",
        nodes = sweep(GlyphRegion.A)
    )

    private fun sweepB() = preset(
        id = "preset_sweep_b",
        name = "Sweep B (Corner)",
        nodes = sweep(GlyphRegion.B),
        settings = PathSettings(nodeDurationMs = 140L, fadeDurationMs = 70L, trailLength = 2)
    )

    private fun fullRingChase() = preset(
        id = "preset_full_chase",
        name = "Full Ring Chase",
        nodes = sweep(GlyphRegion.C) + sweep(GlyphRegion.A) + sweep(GlyphRegion.B),
        settings = PathSettings(nodeDurationMs = 80L, fadeDurationMs = 50L, trailLength = 5, infiniteLoop = true)
    )

    private fun bounceA() = preset(
        id = "preset_bounce_a",
        name = "Bounce A",
        nodes = sweep(GlyphRegion.A) + sweep(GlyphRegion.A, reverse = true).drop(1),
        settings = PathSettings(nodeDurationMs = 90L, fadeDurationMs = 50L, trailLength = 3, pingPong = false, infiniteLoop = true)
    )

    private fun bounceC() = preset(
        id = "preset_bounce_c",
        name = "Bounce C",
        nodes = sweep(GlyphRegion.C) + sweep(GlyphRegion.C, reverse = true).drop(1),
        settings = PathSettings(nodeDurationMs = 70L, fadeDurationMs = 40L, trailLength = 4, infiniteLoop = true)
    )

    private fun cThenA() = preset(
        id = "preset_c_then_a",
        name = "C → A Wave",
        nodes = sweep(GlyphRegion.C) + sweep(GlyphRegion.A),
        settings = PathSettings(nodeDurationMs = 95L, fadeDurationMs = 55L, trailLength = 4)
    )

    private fun reverseRing() = preset(
        id = "preset_reverse_ring",
        name = "Reverse Ring",
        nodes = sweep(GlyphRegion.B, reverse = true) +
            sweep(GlyphRegion.A, reverse = true) +
            sweep(GlyphRegion.C, reverse = true),
        settings = PathSettings(nodeDurationMs = 85L, fadeDurationMs = 50L, trailLength = 5, infiniteLoop = true)
    )

    private fun zigZag() = preset(
        id = "preset_zigzag",
        name = "Zig-Zag C",
        nodes = buildList {
            val c = GlyphRegion.C
            var i = 0
            var j = c.nodeCount - 1
            while (i <= j) {
                add(node(c, i))
                if (i != j) add(node(c, j))
                i++
                j--
            }
        },
        settings = PathSettings(nodeDurationMs = 110L, fadeDurationMs = 60L, trailLength = 3)
    )

    private fun pulseCenters() = preset(
        id = "preset_pulse_centers",
        name = "Pulse Centers",
        nodes = listOf(
            node(GlyphRegion.C, GlyphRegion.C.nodeCount / 2),
            node(GlyphRegion.A, GlyphRegion.A.nodeCount / 2),
            node(GlyphRegion.B, GlyphRegion.B.nodeCount / 2),
            node(GlyphRegion.A, GlyphRegion.A.nodeCount / 2),
            node(GlyphRegion.C, GlyphRegion.C.nodeCount / 2)
        ),
        settings = PathSettings(nodeDurationMs = 200L, fadeDurationMs = 120L, trailLength = 1, infiniteLoop = true)
    )
}

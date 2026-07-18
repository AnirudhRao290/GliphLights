package com.example.gliphlights.editor.model

/**
 * Official Nothing Phone (3a) / (3a) Pro Glyph ring topology.
 *
 * SDK indices (Glyph Developer Kit):
 * - A1–A11 → 20–30 (A1 top, A11 bottom of right arc)
 * - B1–B5  → 31–35 (B1 bottom-right, B5 top-left of short strip)
 * - C1–C20 → 0–19  (C1 bottom-left, C20 top-right of left/top arc)
 *
 * Clockwise doughnut path (gaps bridged for continuous physics):
 * A1 → … → A11 → B1 → … → B5 → C1 → … → C20 → (gap) → A1
 *
 * Compose angles: 0° = right, 90° = down, 180° = left, 270° = up.
 */
object GlyphDoughnut3a {

    /** Clockwise node ids around the camera ring. */
    val clockwiseIds: List<String> = buildList {
        for (i in 1..11) add("A$i")
        for (i in 1..5) add("B$i")
        for (i in 1..20) add("C$i")
    }

    /**
     * Arc placement on the shared camera ring (start → end along increasing local index).
     */
    data class ArcSpec(
        val startAngleDeg: Float,
        val endAngleDeg: Float
    )

    val arcA = ArcSpec(startAngleDeg = -38f, endAngleDeg = 58f)   // A1 top → A11 bottom
    val arcB = ArcSpec(startAngleDeg = 98f, endAngleDeg = 158f)   // B1 bottom-right → B5 top-left
    val arcC = ArcSpec(startAngleDeg = 168f, endAngleDeg = 278f)  // C1 bottom-left → C20 top

    fun arcFor(region: GlyphRegion): ArcSpec = when (region) {
        GlyphRegion.A -> arcA
        GlyphRegion.B -> arcB
        GlyphRegion.C -> arcC
    }

    fun angleFor(region: GlyphRegion, localIndex: Int): Float {
        val arc = arcFor(region)
        val t = if (region.nodeCount > 1) {
            localIndex.toFloat() / (region.nodeCount - 1)
        } else {
            0.5f
        }
        return arc.startAngleDeg + t * (arc.endAngleDeg - arc.startAngleDeg)
    }

    /** Bidirectional doughnut neighbors for a node id (includes gap bridges). */
    fun neighborsOf(nodeId: String): List<String> {
        val idx = clockwiseIds.indexOf(nodeId)
        if (idx < 0) return emptyList()
        val prev = clockwiseIds[(idx - 1 + clockwiseIds.size) % clockwiseIds.size]
        val next = clockwiseIds[(idx + 1) % clockwiseIds.size]
        return listOf(prev, next)
    }

    fun isGapBridge(a: String, b: String): Boolean {
        val bridges = setOf(
            "A11" to "B1", "B1" to "A11",
            "B5" to "C1", "C1" to "B5",
            "C20" to "A1", "A1" to "C20"
        )
        return (a to b) in bridges
    }
}

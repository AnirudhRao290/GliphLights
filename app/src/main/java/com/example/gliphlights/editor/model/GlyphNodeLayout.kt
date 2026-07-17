package com.example.gliphlights.editor.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Glyph map for Nothing Phone (3a) / (3a) Pro — three arcs on one doughnut ring.
 */
class GlyphNodeLayout(viewportWidth: Float, viewportHeight: Float) {

    val nodes: List<GlyphNode>
    val center: Offset
    val cameraRadius: Float
    val glyphRadius: Float
    val segmentLength: Float
    val segmentWidth: Float
    val hitRadius: Float

    /** Node indices in clockwise doughnut order (same length as [nodes] permutation). */
    val doughnutIndexOrder: IntArray

    private val nodeMap: Map<String, GlyphNode>

    init {
        val centerX = viewportWidth / 2f
        val centerY = viewportHeight * 0.40f
        center = Offset(centerX, centerY)

        val maxRadius = minOf(viewportWidth, viewportHeight) / 2f * 0.70f
        glyphRadius = maxRadius
        cameraRadius = maxRadius * 0.72f

        val densestSweep = GlyphDoughnut3a.arcC.endAngleDeg - GlyphDoughnut3a.arcC.startAngleDeg
        val densestSpacing = (2.0 * Math.PI * glyphRadius * (densestSweep / 360.0) /
            (GlyphRegion.C.nodeCount - 1)).toFloat()
        segmentLength = densestSpacing * 0.72f
        segmentWidth = (maxRadius * 0.09f).coerceIn(14f, 24f)
        hitRadius = hypot(segmentLength, segmentWidth) * 0.7f

        val built = GlyphRegion.entries.flatMap { region ->
            computeRegionNodes(region, centerX, centerY)
        }

        // Wire doughnut neighbors (ring + gap bridges), not only in-region chains.
        nodes = built.map { node ->
            node.copy(neighbors = GlyphDoughnut3a.neighborsOf(node.id))
        }
        nodeMap = nodes.associateBy { it.id }

        doughnutIndexOrder = IntArray(GlyphDoughnut3a.clockwiseIds.size) { ringSlot ->
            val id = GlyphDoughnut3a.clockwiseIds[ringSlot]
            nodes.indexOfFirst { it.id == id }.coerceAtLeast(0)
        }
    }

    fun getNode(id: String): GlyphNode? = nodeMap[id]

    fun getNodesByRegion(region: GlyphRegion): List<GlyphNode> =
        nodes.filter { it.region == region }

    fun findNearestNode(position: Offset, maxDistance: Float = hitRadius): GlyphNode? {
        return nodes.minByOrNull { (it.position - position).getDistance() }
            ?.takeIf { (it.position - position).getDistance() <= maxDistance }
    }

    fun findPaintTarget(position: Offset, lastNodeId: String?): GlyphNode? {
        val last = lastNodeId?.let { nodeMap[it] }
        if (last != null) {
            val candidates = buildList {
                add(last)
                last.neighbors.forEach { id -> nodeMap[id]?.let { add(it) } }
                last.neighbors.forEach { nid ->
                    nodeMap[nid]?.neighbors?.forEach { id ->
                        if (id != last.id) nodeMap[id]?.let { add(it) }
                    }
                }
            }.distinctBy { it.id }

            val best = candidates.minByOrNull { (it.position - position).getDistance() }
            if (best != null && (best.position - position).getDistance() <= hitRadius * 1.35f) {
                return best
            }
        }
        return findNearestNode(position, hitRadius * 1.2f)
    }

    /**
     * Neighbor indices into [nodes] for physics — always the doughnut ring.
     */
    fun doughnutNeighborIndices(): Array<IntArray> {
        return Array(nodes.size) { i ->
            nodes[i].neighbors.mapNotNull { id -> nodeMap[id]?.let { nodes.indexOf(it) } }
                .filter { it >= 0 }
                .toIntArray()
        }
    }

    private fun computeRegionNodes(
        region: GlyphRegion,
        centerX: Float,
        centerY: Float
    ): List<GlyphNode> {
        return (0 until region.nodeCount).map { localIndex ->
            val angleDeg = GlyphDoughnut3a.angleFor(region, localIndex)
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val x = centerX + glyphRadius * cos(angleRad)
            val y = centerY + glyphRadius * sin(angleRad)

            GlyphNode(
                id = "${region.name}${localIndex + 1}",
                region = region,
                localIndex = localIndex,
                sdkIndex = region.sdkIndex(localIndex),
                position = Offset(x, y),
                angleDeg = angleDeg,
                neighbors = emptyList()
            )
        }
    }
}

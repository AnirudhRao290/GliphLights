package com.example.gliphlights.editor.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

class GlyphNodeLayout(viewportWidth: Float, viewportHeight: Float) {

    val nodes: List<GlyphNode>
    private val nodeMap: Map<String, GlyphNode>

    init {
        val centerX = viewportWidth * 0.42f
        val centerY = viewportHeight * 0.38f
        val baseRadius = minOf(viewportWidth, viewportHeight) * 0.35f

        val allNodes = mutableListOf<GlyphNode>()

        GlyphRegion.entries.forEach { region ->
            val regionNodes = computeRegionNodes(region, centerX, centerY, baseRadius)
            allNodes.addAll(regionNodes)
        }

        nodes = allNodes
        nodeMap = allNodes.associateBy { it.id }

        val finalNodes = allNodes.map { node ->
            val neighborIds = node.neighbors.mapNotNull { nodeMap[it]?.id }
            node.copy(neighbors = neighborIds)
        }
        nodes as MutableList
        nodes.clear()
        nodes.addAll(finalNodes)
    }

    fun getNode(id: String): GlyphNode? = nodeMap[id]

    fun getNodesByRegion(region: GlyphRegion): List<GlyphNode> =
        nodes.filter { it.region == region }

    fun findNearestNode(position: Offset, maxDistance: Float = Float.MAX_VALUE): GlyphNode? {
        return nodes.minByOrNull { (it.position - position).getDistance() }
            ?.takeIf { (it.position - position).getDistance() <= maxDistance }
    }

    private fun computeRegionNodes(
        region: GlyphRegion,
        centerX: Float,
        centerY: Float,
        baseRadius: Float
    ): List<GlyphNode> {
        val radius = when (region) {
            GlyphRegion.A -> baseRadius * 0.95f
            GlyphRegion.B -> baseRadius * 0.85f
            GlyphRegion.C -> baseRadius * 1.0f
        }

        val startAngleDeg = region.arcCenterAngle - region.arcSweep / 2f
        val endAngleDeg = region.arcCenterAngle + region.arcSweep / 2f

        return (0 until region.nodeCount).map { localIndex ->
            val t = if (region.nodeCount > 1) {
                localIndex.toFloat() / (region.nodeCount - 1)
            } else {
                0.5f
            }
            val angleDeg = startAngleDeg + t * (endAngleDeg - startAngleDeg)
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

            val x = centerX + radius * cos(angleRad)
            val y = centerY + radius * sin(angleRad)

            val sdkIndex = region.sdkIndex(localIndex)
            val nodeId = "${region.name}${localIndex + 1}"

            val neighborIds = buildList {
                if (localIndex > 0) add("${region.name}${localIndex}")
                if (localIndex < region.nodeCount - 1) add("${region.name}${localIndex + 2}")
            }

            GlyphNode(
                id = nodeId,
                region = region,
                localIndex = localIndex,
                sdkIndex = sdkIndex,
                position = Offset(x, y),
                neighbors = neighborIds
            )
        }
    }
}

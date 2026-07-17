package com.example.gliphlights.pathbuilder

import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.editor.model.GlyphRegion
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings

/**
 * Path-level operations after a sequence exists. Pure functions / in-place list ops.
 */
object PathOperations {

    fun reverse(path: List<PathNode>): List<PathNode> = path.asReversed()

    /**
     * Mirror across vertical axis of the camera: A ↔ C roughly by angle symmetry
     * using layout positions (sdkIndex remap via nearest mirrored position).
     */
    fun mirror(path: List<PathNode>, layout: GlyphNodeLayout): List<PathNode> {
        if (path.isEmpty()) return emptyList()
        val cx = layout.center.x
        return path.mapNotNull { node ->
            val glyph = layout.getNode(node.nodeId) ?: return@mapNotNull null
            val mirroredX = 2f * cx - glyph.position.x
            val mirroredY = glyph.position.y
            val nearest = layout.findNearestNode(
                androidx.compose.ui.geometry.Offset(mirroredX, mirroredY),
                maxDistance = Float.MAX_VALUE
            ) ?: return@mapNotNull null
            PathNode(nearest.id, nearest.sdkIndex, nearest.region.name)
        }.let { PathOptimizer().dedupeConsecutive(it) }
    }

    fun closeLoop(path: List<PathNode>): List<PathNode> {
        if (path.size < 2) return path
        val first = path.first()
        return if (path.last().nodeId == first.nodeId) path else path + first
    }

    fun duplicate(path: List<PathNode>): List<PathNode> {
        if (path.isEmpty()) return emptyList()
        return path + path
    }

    fun simplify(path: List<PathNode>, keepEvery: Int = 2): List<PathNode> {
        if (path.size <= 2 || keepEvery <= 1) return path
        val out = ArrayList<PathNode>((path.size / keepEvery) + 2)
        for (i in path.indices) {
            if (i % keepEvery == 0) out.add(path[i])
        }
        val last = path.last()
        if (out.last().nodeId != last.nodeId) out.add(last)
        return out
    }

    fun smooth(path: List<PathNode>, strength: Float = 0.5f): List<PathNode> =
        PathOptimizer().smooth(path, strength)

    fun trim(path: List<PathNode>, fromIndex: Int, toIndexExclusive: Int): List<PathNode> {
        if (path.isEmpty()) return emptyList()
        val from = fromIndex.coerceIn(0, path.size)
        val to = toIndexExclusive.coerceIn(from, path.size)
        return path.subList(from, to).toList()
    }

    fun append(base: List<PathNode>, extra: List<PathNode>): List<PathNode> {
        if (extra.isEmpty()) return base
        if (base.isEmpty()) return extra
        return if (base.last().nodeId == extra.first().nodeId) {
            base + extra.drop(1)
        } else {
            base + extra
        }
    }

    fun merge(paths: List<List<PathNode>>): List<PathNode> {
        var acc = emptyList<PathNode>()
        for (p in paths) {
            acc = append(acc, p)
        }
        return PathOptimizer().dedupeConsecutive(acc)
    }

    /** Region-aware helper for future consumers. */
    fun nodesInRegion(path: List<PathNode>, region: GlyphRegion): List<PathNode> =
        path.filter { it.regionName == region.name }
}

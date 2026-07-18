package com.example.gliphlights.pathbuilder

import androidx.compose.ui.geometry.Offset
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.pathbuilder.model.SamplePoint
import kotlin.math.hypot

/**
 * Records raw finger samples for a single stroke and multi-stroke sessions.
 */
class PathRecorder {

    private val currentStroke = ArrayList<SamplePoint>(256)
    private val strokes = ArrayList<List<SamplePoint>>(8)

    /** Undo stack of completed stroke lists (full path snapshots). */
    private val undoStack = ArrayDeque<List<List<SamplePoint>>>(16)
    private val redoStack = ArrayDeque<List<List<SamplePoint>>>(16)

    val isRecording: Boolean get() = recording
    private var recording = false

    fun beginStroke(tNanos: Long = System.nanoTime()) {
        if (recording) endStroke()
        pushUndoSnapshot()
        redoStack.clear()
        currentStroke.clear()
        recording = true
    }

    /**
     * Append a sample if it moved far enough ([minDistancePx]).
     * Returns true if the sample was kept.
     */
    fun appendSample(x: Float, y: Float, tNanos: Long, minDistancePx: Float): Boolean {
        if (!recording) return false
        if (currentStroke.isNotEmpty()) {
            val last = currentStroke.last()
            if (hypot(x - last.x, y - last.y) < minDistancePx) return false
        }
        currentStroke.add(SamplePoint(x, y, tNanos))
        return true
    }

    fun endStroke() {
        if (!recording) return
        recording = false
        if (currentStroke.isNotEmpty()) {
            strokes.add(ArrayList(currentStroke))
            currentStroke.clear()
        }
    }

    fun clear() {
        pushUndoSnapshot()
        redoStack.clear()
        currentStroke.clear()
        strokes.clear()
        recording = false
    }

    fun allSamples(): List<SamplePoint> {
        val out = ArrayList<SamplePoint>(strokes.sumOf { it.size } + currentStroke.size)
        strokes.forEach { out.addAll(it) }
        out.addAll(currentStroke)
        return out
    }

    fun strokeCount(): Int = strokes.size + if (currentStroke.isNotEmpty()) 1 else 0

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(snapshot())
        restore(undoStack.removeLast())
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(snapshot())
        restore(redoStack.removeLast())
    }

    private fun pushUndoSnapshot() {
        undoStack.addLast(snapshot())
        while (undoStack.size > 32) undoStack.removeFirst()
    }

    private fun snapshot(): List<List<SamplePoint>> =
        strokes.map { ArrayList(it) }

    private fun restore(state: List<List<SamplePoint>>) {
        strokes.clear()
        strokes.addAll(state.map { ArrayList(it) })
        currentStroke.clear()
        recording = false
    }
}

/**
 * Maps screen coordinates to nearest Glyph nodes.
 */
class PathConverter(
    private val layout: GlyphNodeLayout
) {
    fun toNode(point: Offset, maxDistance: Float = layout.hitRadius * 1.5f): PathNode? {
        val glyph = layout.findNearestNode(point, maxDistance) ?: return null
        return glyph.toPathNode()
    }

    fun toNode(x: Float, y: Float, maxDistance: Float = layout.hitRadius * 1.5f): PathNode? =
        toNode(Offset(x, y), maxDistance)

    fun convertSamples(
        samples: List<SamplePoint>,
        maxDistance: Float = layout.hitRadius * 1.5f
    ): List<PathNode> {
        if (samples.isEmpty()) return emptyList()
        val result = ArrayList<PathNode>(samples.size)
        for (i in samples.indices) {
            val s = samples[i]
            val node = toNode(s.x, s.y, maxDistance)
            if (node != null) result.add(node)
        }
        return result
    }

    private fun GlyphNode.toPathNode() = PathNode(
        nodeId = id,
        sdkIndex = sdkIndex,
        regionName = region.name
    )
}

/**
 * Cleans raw node sequences: dedupe, jitter, min distance, optional smoothing.
 */
class PathOptimizer {

    fun optimize(nodes: List<PathNode>, settings: PathSettings): List<PathNode> {
        if (nodes.isEmpty()) return emptyList()

        val deduped = dedupeConsecutive(nodes)
        val spaced = applyMinDistance(deduped, settings.minimumNodeDistance)
        return if (settings.smoothingStrength > 0.01f) {
            smooth(spaced, settings.smoothingStrength)
        } else {
            spaced
        }
    }

    fun dedupeConsecutive(nodes: List<PathNode>): List<PathNode> {
        if (nodes.isEmpty()) return emptyList()
        val out = ArrayList<PathNode>(nodes.size)
        var lastId: String? = null
        for (node in nodes) {
            if (node.nodeId != lastId) {
                out.add(node)
                lastId = node.nodeId
            }
        }
        return out
    }

    /**
     * Keep a node only if at least [minDistance] steps away in the path index
     * from the previous kept node (1 = keep all after dedupe).
     */
    fun applyMinDistance(nodes: List<PathNode>, minDistance: Int): List<PathNode> {
        if (nodes.isEmpty() || minDistance <= 1) return nodes
        val out = ArrayList<PathNode>(nodes.size)
        out.add(nodes.first())
        var lastIndex = 0
        for (i in 1 until nodes.size) {
            if (i - lastIndex >= minDistance) {
                out.add(nodes[i])
                lastIndex = i
            }
        }
        val last = nodes.last()
        if (out.last().nodeId != last.nodeId) out.add(last)
        return out
    }

    /**
     * Light neighborhood vote smoothing — preserves direction, reduces jitter IDs.
     */
    fun smooth(nodes: List<PathNode>, strength: Float): List<PathNode> {
        if (nodes.size < 3 || strength <= 0f) return nodes
        val window = (1 + (strength * 2).toInt()).coerceIn(1, 3)
        if (window <= 1) return nodes

        val out = ArrayList<PathNode>(nodes.size)
        out.add(nodes.first())
        for (i in 1 until nodes.lastIndex) {
            val from = (i - window).coerceAtLeast(0)
            val to = (i + window).coerceAtMost(nodes.lastIndex)
            val counts = HashMap<String, Int>(8)
            var best: PathNode = nodes[i]
            var bestCount = 0
            for (j in from..to) {
                val n = nodes[j]
                val c = (counts[n.nodeId] ?: 0) + 1
                counts[n.nodeId] = c
                if (c > bestCount) {
                    bestCount = c
                    best = n
                }
            }
            if (out.last().nodeId != best.nodeId) out.add(best)
        }
        val last = nodes.last()
        if (out.last().nodeId != last.nodeId) out.add(last)
        return out
    }
}

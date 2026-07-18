package com.example.gliphlights.pathbuilder.bake

import com.example.gliphlights.editor.model.GlyphRegion
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.visualizer.GlyphCommand

/**
 * Converts recorded visualizer [GlyphCommand] frames into a Path sequence.
 */
object VisualizerPathBaker {

    data class BakeResult(
        val nodes: List<PathNode>,
        val settings: PathSettings
    )

    /**
     * Samples channel activity from timed commands into ordered [PathNode]s.
     * Brightness-gated: only channels above [brightnessGate] become nodes.
     */
    fun bake(
        samples: List<Pair<Long, GlyphCommand>>,
        brightnessGate: Float = 0.12f,
        targetDurationMs: Long = 5_000L
    ): BakeResult {
        if (samples.isEmpty()) {
            return BakeResult(emptyList(), PathSettings())
        }

        val ordered = ArrayList<PathNode>(64)
        var lastSdk = -1
        for ((_, command) in samples) {
            if (command.brightness < brightnessGate) continue
            val activeCount = (command.channels.size * command.brightness.coerceIn(0f, 1f))
                .toInt()
                .coerceIn(1, command.channels.size.coerceAtLeast(1))
            val active = command.channels.take(activeCount)
            for (sdk in active) {
                if (sdk == lastSdk) continue
                val node = pathNodeForSdk(sdk) ?: continue
                ordered.add(node)
                lastSdk = sdk
            }
        }

        val nodes = if (ordered.size > 120) {
            // Downsample long recordings
            val step = ordered.size / 80f
            val sparse = ArrayList<PathNode>(80)
            var i = 0f
            while (i < ordered.size) {
                sparse.add(ordered[i.toInt()])
                i += step.coerceAtLeast(1f)
            }
            sparse
        } else {
            ordered
        }

        val nodeDur = if (nodes.isEmpty()) {
            120L
        } else {
            (targetDurationMs / nodes.size).coerceIn(40L, 220L)
        }

        return BakeResult(
            nodes = nodes,
            settings = PathSettings(
                nodeDurationMs = nodeDur,
                fadeDurationMs = (nodeDur * 0.4f).toLong().coerceIn(20L, 120L),
                attackMs = (nodeDur * 0.35f).toLong().coerceIn(16L, 100L),
                releaseMs = (nodeDur * 0.45f).toLong().coerceIn(16L, 120L),
                brightness = 1f,
                trailLength = 4,
                infiniteLoop = false
            )
        )
    }

    fun pathNodeForSdk(sdkIndex: Int): PathNode? {
        val region = when (sdkIndex) {
            in GlyphRegion.C.channelStart..GlyphRegion.C.channelEnd -> GlyphRegion.C
            in GlyphRegion.A.channelStart..GlyphRegion.A.channelEnd -> GlyphRegion.A
            in GlyphRegion.B.channelStart..GlyphRegion.B.channelEnd -> GlyphRegion.B
            else -> return null
        }
        val local = sdkIndex - region.channelStart
        val id = "${region.name}${local + 1}"
        return PathNode(nodeId = id, sdkIndex = sdkIndex, regionName = region.name)
    }
}

package com.example.gliphlights.pathbuilder

import com.example.gliphlights.pathbuilder.model.AnimationFrame
import com.example.gliphlights.pathbuilder.model.AnimationModel
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.pathbuilder.model.TransitionType

/**
 * Converts an optimized node sequence + settings into a timeline AnimationModel.
 */
class AnimationGenerator {

    fun generate(path: List<PathNode>, settings: PathSettings): AnimationModel {
        if (path.isEmpty()) return AnimationModel.empty(settings)

        val speed = settings.animationSpeed.coerceIn(0.1f, 4f)
        val nodeDur = (settings.nodeDurationMs / speed).toLong().coerceAtLeast(16L)
        val fade = (settings.fadeDurationMs / speed).toLong().coerceAtLeast(0L)
        val step = (nodeDur - fade).coerceAtLeast(1L)

        val frames = ArrayList<AnimationFrame>(path.size)
        var t = 0L
        for (node in path) {
            frames.add(
                AnimationFrame(
                    nodeId = node.nodeId,
                    sdkIndex = node.sdkIndex,
                    startMs = t,
                    durationMs = nodeDur,
                    brightness = settings.brightness.coerceIn(0f, 1f),
                    fadeInMs = fade,
                    fadeOutMs = fade,
                    transition = if (fade > 0) TransitionType.FADE else TransitionType.CUT
                )
            )
            t += step
        }

        return AnimationModel(
            frames = frames,
            settings = settings,
            pathNodes = path
        )
    }
}

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
        val attack = (settings.effectiveAttackMs / speed).toLong().coerceAtLeast(0L)
        val release = (settings.effectiveReleaseMs / speed).toLong().coerceAtLeast(0L)
        val sustainRatio = settings.sustainRatio.coerceIn(0.1f, 1f)
        val sustainMs = (nodeDur * sustainRatio).toLong().coerceAtLeast(1L)
        // Overlap step so trails connect; keep at least 1ms advance
        val step = (attack + sustainMs).coerceAtLeast(1L).coerceAtMost(nodeDur)

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
                    fadeInMs = attack.coerceAtMost(nodeDur / 2),
                    fadeOutMs = release.coerceAtMost(nodeDur / 2),
                    transition = if (attack > 0 || release > 0) TransitionType.FADE else TransitionType.CUT
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

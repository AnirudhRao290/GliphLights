package com.example.gliphlights.editor.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphRegion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val RegionAColor = Color(0xFF64B5F6)
private val RegionBColor = Color(0xFF81C784)
private val RegionCColor = Color(0xFFFFB74D)

private val ActiveGlowColor = Color(0xFFFFFFFF)
private val InactiveNodeColor = Color(0xFF424242)
private val HitTargetColor = Color(0xFFFFFF00)

private const val NODE_RADIUS = 12f
private const val GLOW_RADIUS = 24f
private const val GLOW_ON_DURATION = 150
private const val GLOW_OFF_DURATION = 100

@Composable
fun PreviewRenderer(
    nodes: List<GlyphNode>,
    activeChannels: Set<Int>,
    hitNodeId: String?,
    modifier: Modifier = Modifier
) {
    val glowAnimatables = remember {
        nodes.associate { it.id to Animatable(0f) }
    }

    LaunchedEffect(activeChannels) {
        coroutineScope {
            nodes.forEach { node ->
                val target = if (node.sdkIndex in activeChannels) 1f else 0f
                val animatable = glowAnimatables[node.id] ?: return@forEach
                if (animatable.targetValue != target) {
                    launch {
                        val duration = if (target > animatable.value) GLOW_ON_DURATION else GLOW_OFF_DURATION
                        animatable.animateTo(
                            targetValue = target,
                            animationSpec = tween(durationMillis = duration)
                        )
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        nodes.forEach { node ->
            val glow = glowAnimatables[node.id]?.value ?: 0f
            val isHit = node.id == hitNodeId
            val regionColor = getRegionColor(node.region)
            drawNode(node, regionColor, glow, isHit)
        }
    }
}

private fun DrawScope.drawNode(
    node: GlyphNode,
    regionColor: Color,
    glow: Float,
    isHit: Boolean
) {
    val pos = node.position

    if (glow > 0f) {
        drawCircle(
            color = ActiveGlowColor.copy(alpha = glow * 0.4f),
            radius = GLOW_RADIUS * glow,
            center = pos
        )
        drawCircle(
            color = regionColor.copy(alpha = glow * 0.6f),
            radius = GLOW_RADIUS * 0.7f * glow,
            center = pos
        )
    }

    val nodeColor = when {
        isHit -> HitTargetColor
        glow > 0f -> regionColor.copy(alpha = 0.5f + glow * 0.5f)
        else -> InactiveNodeColor
    }

    drawCircle(
        color = nodeColor,
        radius = NODE_RADIUS,
        center = pos
    )

    if (glow > 0.3f) {
        drawCircle(
            color = ActiveGlowColor.copy(alpha = glow * 0.8f),
            radius = NODE_RADIUS * 0.4f,
            center = pos
        )
    }
}

private fun getRegionColor(region: GlyphRegion): Color = when (region) {
    GlyphRegion.A -> RegionAColor
    GlyphRegion.B -> RegionBColor
    GlyphRegion.C -> RegionCColor
}

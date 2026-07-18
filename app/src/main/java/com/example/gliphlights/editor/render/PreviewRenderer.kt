package com.example.gliphlights.editor.render

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.gliphlights.editor.model.GlyphNodeLayout

@Composable
fun PreviewRenderer(
    layout: GlyphNodeLayout,
    activeChannels: Set<Int>,
    hitNodeId: String?,
    modifier: Modifier = Modifier,
    glowIntensity: Float = 1f,
    previewPulse: Boolean = false,
    /** Optional per-channel fill 0..1; falls back to on/off from [activeChannels]. */
    channelFills: FloatArray? = null
) {
    val pulse by rememberInfiniteTransition(label = "nodePulse").animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseFactor = if (previewPulse) pulse else 1f
    val glow = glowIntensity.coerceIn(0.4f, 1.5f) * pulseFactor

    Canvas(modifier = modifier.fillMaxSize()) {
        drawGlyphTubes(
            layout = layout,
            fillFor = { node ->
                channelFills?.getOrNull(node.sdkIndex)
                    ?: if (node.sdkIndex in activeChannels) 1f else 0f
            },
            highlightedId = hitNodeId,
            glowStrength = glow
        )
    }
}

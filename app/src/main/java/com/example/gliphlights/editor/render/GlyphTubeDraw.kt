package com.example.gliphlights.editor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout
import kotlin.math.max

private val TubeOn = Color(0xFFF5F2EA)
private val TubeGlow = Color(0xCCFFFFFF)
private val TubeOff = Color(0xFF2C2C2C)
private val TubeOffStroke = Color(0xFF555555)
private val HitStroke = Color(0xFFFFEB3B)
private val CameraFill = Color(0xFF141414)
private val CameraRing = Color(0xFF2C2C2C)
private val CameraLens = Color(0xFF0A0A0A)
private val CameraLensRing = Color(0xFF333333)

/**
 * Shared Nothing Phone (3a) Glyph drawing — bent LED tube segments on the doughnut,
 * not dots. [fill] 0..1 lights the capsule (partial fill for swipe/progress).
 */
fun DrawScope.drawGlyphCameraIsland(center: Offset, radius: Float) {
    drawCircle(color = CameraFill, radius = radius, center = center)
    drawCircle(
        color = CameraRing,
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.06f)
    )
    val lensR = radius * 0.145f
    listOf(
        center + Offset(-radius * 0.18f, -radius * 0.12f),
        center + Offset(-radius * 0.18f, radius * 0.22f),
        center + Offset(radius * 0.22f, -radius * 0.05f)
    ).forEachIndexed { index, lensCenter ->
        val r = if (index == 2) lensR * 0.72f else lensR
        drawCircle(color = CameraLens, radius = r, center = lensCenter)
        drawCircle(
            color = CameraLensRing,
            radius = r,
            center = lensCenter,
            style = Stroke(width = r * 0.18f)
        )
    }
}

fun DrawScope.drawGlyphBentTube(
    node: GlyphNode,
    layout: GlyphNodeLayout,
    fill: Float,
    highlighted: Boolean = false,
    glowStrength: Float = 1f
) {
    val amount = fill.coerceIn(0f, 1f)
    val thickness = layout.tubeThickness
    val radius = layout.glyphRadius
    val topLeft = Offset(layout.center.x - radius, layout.center.y - radius)
    val size = Size(radius * 2f, radius * 2f)
    val start = node.tubeStartDeg
    val sweep = max(node.tubeSweepDeg, 0.8f)

    // Inactive tube shell (always visible structure)
    drawArc(
        color = TubeOff,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = thickness, cap = StrokeCap.Round)
    )
    drawArc(
        color = TubeOffStroke,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = thickness * 0.22f, cap = StrokeCap.Round)
    )

    if (amount > 0.02f) {
        val litSweep = sweep * amount
        val glow = (0.35f + 0.55f * glowStrength) * amount
        drawArc(
            color = TubeGlow.copy(alpha = glow.coerceIn(0.15f, 0.9f)),
            startAngle = start,
            sweepAngle = litSweep,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = thickness * 1.35f, cap = StrokeCap.Round)
        )
        drawArc(
            color = TubeOn.copy(alpha = 0.45f + 0.55f * amount),
            startAngle = start,
            sweepAngle = litSweep,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = thickness * 0.92f, cap = StrokeCap.Round)
        )
    }

    if (highlighted) {
        drawArc(
            color = HitStroke,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = thickness * 1.15f, cap = StrokeCap.Round)
        )
    }
}

fun DrawScope.drawGlyphTubes(
    layout: GlyphNodeLayout,
    fillFor: (GlyphNode) -> Float,
    highlightedId: String? = null,
    glowStrength: Float = 1f
) {
    drawGlyphCameraIsland(layout.center, layout.cameraRadius)
    layout.nodes.forEach { node ->
        drawGlyphBentTube(
            node = node,
            layout = layout,
            fill = fillFor(node),
            highlighted = node.id == highlightedId,
            glowStrength = glowStrength
        )
    }
}

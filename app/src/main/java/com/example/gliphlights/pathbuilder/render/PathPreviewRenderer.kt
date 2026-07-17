package com.example.gliphlights.pathbuilder.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.pathbuilder.model.PathNode

private val ActiveFill = Color(0xFFF5F5F5)
private val ActiveGlow = Color(0xAAFFFFFF)
private val InactiveFill = Color(0xFF2A2A2A)
private val InactiveStroke = Color(0xFF4A4A4A)
private val LiveHighlight = Color(0xFFFFEB3B)
private val TrailColor = Color(0xFF90CAF9)
private val PathStroke = Color(0xFF64B5F6)
private val CameraFill = Color(0xFF141414)
private val CameraRing = Color(0xFF2C2C2C)
private val CameraLens = Color(0xFF0A0A0A)
private val CameraLensRing = Color(0xFF333333)

/**
 * Software-only path preview. Never talks to the Glyph SDK.
 */
@Composable
fun PathPreviewRenderer(
    layout: GlyphNodeLayout,
    nodeAlphas: FloatArray,
    liveTrail: List<Offset>,
    livePathNodes: List<PathNode>,
    enteredNodeId: String?,
    drawMode: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCameraIsland(layout.center, layout.cameraRadius)

        if (liveTrail.size >= 2) {
            val path = Path()
            path.moveTo(liveTrail[0].x, liveTrail[0].y)
            for (i in 1 until liveTrail.size) {
                path.lineTo(liveTrail[i].x, liveTrail[i].y)
            }
            drawPath(
                path = path,
                color = TrailColor.copy(alpha = 0.85f),
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Optimized path polyline through node centers
        if (livePathNodes.size >= 2) {
            val path = Path()
            val first = layout.getNode(livePathNodes[0].nodeId)
            if (first != null) {
                path.moveTo(first.position.x, first.position.y)
                for (i in 1 until livePathNodes.size) {
                    val n = layout.getNode(livePathNodes[i].nodeId) ?: continue
                    path.lineTo(n.position.x, n.position.y)
                }
                drawPath(
                    path = path,
                    color = PathStroke.copy(alpha = 0.55f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        layout.nodes.forEach { node ->
            val alpha = nodeAlphas.getOrElse(node.sdkIndex) { 0f }.coerceIn(0f, 1f)
            val isEntered = drawMode && node.id == enteredNodeId
            drawSegmentBox(
                node = node,
                length = layout.segmentLength,
                width = layout.segmentWidth,
                glow = alpha,
                highlight = isEntered
            )
        }
    }
}

private fun DrawScope.drawCameraIsland(center: Offset, radius: Float) {
    drawCircle(color = CameraFill, radius = radius, center = center)
    drawCircle(
        color = CameraRing,
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.07f)
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

private fun DrawScope.drawSegmentBox(
    node: GlyphNode,
    length: Float,
    width: Float,
    glow: Float,
    highlight: Boolean
) {
    val pos = node.position
    val rotation = node.angleDeg + 90f
    val corner = CornerRadius(width * 0.35f, width * 0.35f)

    rotate(degrees = rotation, pivot = pos) {
        val topLeft = Offset(pos.x - length / 2f, pos.y - width / 2f)
        val size = Size(length, width)

        when {
            glow > 0.02f -> {
                drawRoundRect(
                    color = ActiveGlow.copy(alpha = glow * 0.55f),
                    topLeft = Offset(topLeft.x - 3f, topLeft.y - 3f),
                    size = Size(size.width + 6f, size.height + 6f),
                    cornerRadius = CornerRadius(corner.x + 2f, corner.y + 2f)
                )
                drawRoundRect(
                    color = ActiveFill.copy(alpha = 0.35f + glow * 0.65f),
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = corner
                )
            }
            else -> {
                drawRoundRect(
                    color = InactiveFill,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = InactiveStroke,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = corner,
                    style = Stroke(width = 1.5f)
                )
            }
        }

        if (highlight) {
            drawRoundRect(
                color = LiveHighlight,
                topLeft = Offset(topLeft.x - 2f, topLeft.y - 2f),
                size = Size(size.width + 4f, size.height + 4f),
                cornerRadius = CornerRadius(corner.x + 1f, corner.y + 1f),
                style = Stroke(width = 2.5f)
            )
        }
    }
}

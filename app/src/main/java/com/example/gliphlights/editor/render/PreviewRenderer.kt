package com.example.gliphlights.editor.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout

private val ActiveFill = Color(0xFFF5F5F5)
private val ActiveGlow = Color(0xCCFFFFFF)
private val InactiveFill = Color(0xFF2A2A2A)
private val InactiveStroke = Color(0xFF4A4A4A)
private val HitStroke = Color(0xFFFFEB3B)
private val CameraFill = Color(0xFF141414)
private val CameraRing = Color(0xFF2C2C2C)
private val CameraLens = Color(0xFF0A0A0A)
private val CameraLensRing = Color(0xFF333333)

@Composable
fun PreviewRenderer(
    layout: GlyphNodeLayout,
    activeChannels: Set<Int>,
    hitNodeId: String?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCameraIsland(layout.center, layout.cameraRadius)

        layout.nodes.forEach { node ->
            val isOn = node.sdkIndex in activeChannels
            val isHit = node.id == hitNodeId
            drawSegmentBox(
                node = node,
                length = layout.segmentLength,
                width = layout.segmentWidth,
                isOn = isOn,
                isHit = isHit
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
    drawCircle(
        color = CameraRing.copy(alpha = 0.45f),
        radius = radius * 0.78f,
        center = center,
        style = Stroke(width = radius * 0.035f)
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
    isOn: Boolean,
    isHit: Boolean
) {
    val pos = node.position
    // Orient the long axis tangent to the circle (perpendicular to radial angle).
    val rotation = node.angleDeg + 90f
    val corner = CornerRadius(width * 0.35f, width * 0.35f)

    rotate(degrees = rotation, pivot = pos) {
        val topLeft = Offset(pos.x - length / 2f, pos.y - width / 2f)
        val size = Size(length, width)

        if (isOn) {
            drawRoundRect(
                color = ActiveGlow,
                topLeft = Offset(topLeft.x - 3f, topLeft.y - 3f),
                size = Size(size.width + 6f, size.height + 6f),
                cornerRadius = CornerRadius(corner.x + 2f, corner.y + 2f)
            )
            drawRoundRect(
                color = ActiveFill,
                topLeft = topLeft,
                size = size,
                cornerRadius = corner
            )
        } else {
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

        if (isHit) {
            drawRoundRect(
                color = HitStroke,
                topLeft = Offset(topLeft.x - 2f, topLeft.y - 2f),
                size = Size(size.width + 4f, size.height + 4f),
                cornerRadius = CornerRadius(corner.x + 1f, corner.y + 1f),
                style = Stroke(width = 2.5f)
            )
        }
    }
}

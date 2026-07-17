package com.example.gliphlights.physics.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.physics.model.PhysicsAnimationModel
import com.example.gliphlights.physics.model.PhysicsMode

private val ActiveFill = Color(0xFFF5F2EA)
private val ActiveGlow = Color(0xAAFFFFFF)
private val InactiveFill = Color(0xFF2A2A2A)
private val InactiveStroke = Color(0xFF4A4A4A)
private val CameraFill = Color(0xFF141414)
private val CameraRing = Color(0xFF2C2C2C)
private val MagnetColor = Color(0xFFFF8A65)

@Composable
fun PhysicsPreviewRenderer(
    layout: GlyphNodeLayout?,
    model: PhysicsAnimationModel,
    mode: PhysicsMode,
    onLayoutCreated: (GlyphNodeLayout) -> Unit,
    onTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val current = remember(width, height) {
            GlyphNodeLayout(width, height).also(onLayoutCreated)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(mode) {
                    if (mode == PhysicsMode.MAGNET) {
                        detectTapGestures { pos -> onTap(pos.x, pos.y) }
                    }
                }
        ) {
            drawCamera(current.center, current.cameraRadius)
            current.nodes.forEach { node ->
                val glow = model.nodeAlphas.getOrElse(node.sdkIndex) { 0f }
                drawSegment(node, current.segmentLength, current.segmentWidth, glow)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCamera(center: Offset, radius: Float) {
    drawCircle(CameraFill, radius, center)
    drawCircle(CameraRing, radius, center, style = Stroke(width = radius * 0.07f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegment(
    node: GlyphNode,
    length: Float,
    width: Float,
    glow: Float
) {
    val pos = node.position
    val rotation = node.angleDeg + 90f
    val corner = CornerRadius(width * 0.35f, width * 0.35f)
    rotate(degrees = rotation, pivot = pos) {
        val topLeft = Offset(pos.x - length / 2f, pos.y - width / 2f)
        val size = Size(length, width)
        if (glow > 0.04f) {
            drawRoundRect(
                color = ActiveGlow.copy(alpha = glow * 0.5f),
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
        } else {
            drawRoundRect(InactiveFill, topLeft, size, corner)
            drawRoundRect(InactiveStroke, topLeft, size, corner, style = Stroke(1.5f))
        }
    }
}

package com.example.gliphlights.pathbuilder.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.editor.render.drawGlyphTubes
import com.example.gliphlights.pathbuilder.model.PathNode

private val TrailColor = Color(0xFF90CAF9)
private val PathStroke = Color(0xFF64B5F6)

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

        drawGlyphTubes(
            layout = layout,
            fillFor = { node -> nodeAlphas.getOrElse(node.sdkIndex) { 0f }.coerceIn(0f, 1f) },
            highlightedId = if (drawMode) enteredNodeId else null
        )
    }
}

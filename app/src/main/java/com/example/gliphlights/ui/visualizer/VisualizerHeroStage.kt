package com.example.gliphlights.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.editor.render.drawGlyphTubes
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Premium Visualizer hero: radial spectrum + bent Glyph tubes + camera island.
 */
@Composable
fun VisualizerHeroStage(
    channelFills: FloatArray,
    audioLevel: Float,
    spectrumSeed: Int = 0,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val layout = remember(constraints.maxWidth, constraints.maxHeight) {
            GlyphNodeLayout(
                viewportWidth = with(density) { maxWidth.toPx() },
                viewportHeight = with(density) { maxHeight.toPx() },
                centerYFraction = 0.50f
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = layout.center
            val r = layout.glyphRadius

            // Soft vignette
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1A1A1A), Color.Black),
                    center = c,
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = c
            )

            // Radial frequency bars outside the Glyph ring
            val bars = 72
            val baseR = r * 1.18f
            val amp = audioLevel.coerceIn(0f, 1f)
            val rng = Random(spectrumSeed)
            for (i in 0 until bars) {
                val t = i / bars.toFloat()
                val angle = Math.toRadians(t * 360.0 - 90.0)
                val wobble = 0.35f + 0.65f * ((kotlin.math.sin(t * 18f + amp * 8f) + 1f) / 2f)
                val noise = 0.55f + 0.45f * rng.nextFloat()
                val h = baseR * 0.08f + baseR * 0.42f * amp * wobble * noise
                val x0 = c.x + baseR * cos(angle).toFloat()
                val y0 = c.y + baseR * sin(angle).toFloat()
                val x1 = c.x + (baseR + h) * cos(angle).toFloat()
                val y1 = c.y + (baseR + h) * sin(angle).toFloat()
                drawLine(
                    color = spectrumColor(t).copy(alpha = 0.55f + 0.45f * amp),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                    strokeWidth = 3.2f,
                    cap = StrokeCap.Round
                )
            }

            // Technical dashed guide ring
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(c.x - r * 1.08f, c.y - r * 1.08f),
                size = Size(r * 2.16f, r * 2.16f),
                style = Stroke(width = 1.5f)
            )

            drawGlyphTubes(
                layout = layout,
                fillFor = { node -> channelFills.getOrElse(node.sdkIndex) { 0f } },
                glowStrength = 1.15f
            )
        }
    }
}

private fun spectrumColor(t: Float): Color {
    // Orange → magenta → violet → cyan around the ring
    val stops = listOf(
        0.00f to Color(0xFFFF8A3D),
        0.20f to Color(0xFFFF4D8D),
        0.40f to Color(0xFFB44DFF),
        0.60f to Color(0xFF5B8CFF),
        0.80f to Color(0xFF2DE2E6),
        1.00f to Color(0xFFFF8A3D)
    )
    for (i in 0 until stops.lastIndex) {
        val (aT, aC) = stops[i]
        val (bT, bC) = stops[i + 1]
        if (t in aT..bT) {
            val u = (t - aT) / (bT - aT)
            return lerpColor(aC, bC, u)
        }
    }
    return stops.last().second
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f
    )
}

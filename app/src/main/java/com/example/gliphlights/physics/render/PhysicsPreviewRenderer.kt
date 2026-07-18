package com.example.gliphlights.physics.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.editor.render.drawGlyphTubes
import com.example.gliphlights.physics.model.PhysicsAnimationModel
import com.example.gliphlights.physics.model.PhysicsMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PhysicsPreviewRenderer(
    layout: GlyphNodeLayout?,
    model: PhysicsAnimationModel,
    mode: PhysicsMode,
    onLayoutCreated: (GlyphNodeLayout) -> Unit,
    onTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    showChamberFluid: Boolean = true
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val current = remember(width, height) {
            GlyphNodeLayout(width, height, centerYFraction = 0.48f).also(onLayoutCreated)
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
            val c = current.center
            val chamberR = current.cameraRadius * 0.98f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1C1C1C), Color(0xFF050505)),
                    center = c,
                    radius = size.minDimension * 0.5f
                ),
                radius = size.minDimension * 0.48f,
                center = c
            )

            // Chamber disc
            drawCircle(Color(0xFF0E0E0E), chamberR, c)
            drawCircle(
                Color.White.copy(alpha = 0.08f),
                chamberR,
                c,
                style = Stroke(width = 2f)
            )

            if (showChamberFluid) {
                drawChamberParticles(c, chamberR, model, mode)
            }

            drawGlyphTubes(
                layout = current,
                fillFor = { node -> model.nodeAlphas.getOrElse(node.sdkIndex) { 0f } },
                glowStrength = 1.2f
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChamberParticles(
    center: Offset,
    radius: Float,
    model: PhysicsAnimationModel,
    mode: PhysicsMode
) {
    var mass = 0f
    for (a in model.nodeAlphas) mass += a
    val energy = (mass / model.nodeAlphas.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    val count = when (mode) {
        PhysicsMode.SAND -> 180
        PhysicsMode.BUBBLE -> 90
        PhysicsMode.MERCURY -> 120
        else -> 150
    }
    val rng = Random((energy * 10_000).toInt() + mode.ordinal * 97)

    // Pool toward bottom of chamber (gravity)
    for (i in 0 until count) {
        val u = rng.nextFloat()
        val v = rng.nextFloat()
        val angle = Math.toRadians(70.0 + u * 40.0) // bottom-ish
        val dist = radius * (0.35f + v * 0.55f) * (0.55f + energy * 0.45f)
        val jitterX = (rng.nextFloat() - 0.5f) * radius * 0.35f
        val x = center.x + dist * cos(angle).toFloat() + jitterX
        val y = center.y + dist * sin(angle).toFloat() * (0.7f + energy * 0.3f)
        val r = 1.2f + rng.nextFloat() * 2.4f
        val a = 0.25f + energy * 0.7f * rng.nextFloat()
        drawCircle(Color.White.copy(alpha = a), r, Offset(x, y))
    }

    // Falling sprinkle
    val sprinkle = (energy * 40).toInt()
    for (i in 0 until sprinkle) {
        val x = center.x + (rng.nextFloat() - 0.5f) * radius * 1.2f
        val y = center.y - radius * (0.2f + rng.nextFloat() * 0.7f)
        drawCircle(Color.White.copy(alpha = 0.35f), 1.4f, Offset(x, y))
    }
}

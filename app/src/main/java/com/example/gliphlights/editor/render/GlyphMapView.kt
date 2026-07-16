package com.example.gliphlights.editor.render

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.example.gliphlights.editor.gesture.GestureEvent
import com.example.gliphlights.editor.gesture.GestureSampler
import com.example.gliphlights.editor.model.GlyphNodeLayout
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GlyphMapView(
    layout: GlyphNodeLayout?,
    activeChannels: Set<Int>,
    onGestureEvent: (GestureEvent) -> Unit,
    onLayoutCreated: (GlyphNodeLayout) -> Unit,
    modifier: Modifier = Modifier,
    view: View? = null
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var hitNodeId by remember { mutableStateOf<String?>(null) }

    var lastTouchCount by remember { mutableStateOf(0) }
    var lastPinchDist by remember { mutableFloatStateOf(0f) }
    var lastPinchCentroid by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val currentLayout = remember(width, height) {
            GlyphNodeLayout(width, height).also { onLayoutCreated(it) }
        }

        val sampler = remember(currentLayout) {
            GestureSampler(currentLayout) { event ->
                when (event) {
                    is GestureEvent.DragEnter -> hitNodeId = event.nodeId
                    is GestureEvent.DragEnd -> hitNodeId = null
                    else -> {}
                }
                onGestureEvent(event)
            }
        }

        LaunchedEffect(view) {
            view?.let { sampler.setView(it) }
        }

        PreviewRenderer(
            nodes = currentLayout.nodes,
            activeChannels = activeChannels,
            hitNodeId = hitNodeId,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
                .pointerInteropFilter { event ->
                    val pointerCount = event.pointerCount

                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            lastTouchCount = pointerCount
                            if (pointerCount == 1) {
                                val x = (event.x - offsetX) / scale
                                val y = (event.y - offsetY) / scale
                                sampler.processDown(Offset(x, y))
                            }
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (pointerCount >= 2) {
                                val p1x = event.getX(0)
                                val p1y = event.getY(0)
                                val p2x = event.getX(1)
                                val p2y = event.getY(1)

                                val dx = p1x - p2x
                                val dy = p1y - p2y
                                val currentDist = sqrt(dx * dx + dy * dy)
                                val centroid = Offset((p1x + p2x) / 2f, (p1y + p2y) / 2f)

                                if (lastPinchDist > 0f) {
                                    val zoomFactor = currentDist / lastPinchDist
                                    if (abs(zoomFactor - 1f) > 0.001f) {
                                        scale = (scale * zoomFactor).coerceIn(0.5f, 3f)
                                        sampler.processZoom(centroid, zoomFactor)
                                    }

                                    val panDelta = centroid - lastPinchCentroid
                                    if (abs(panDelta.x) > 0.5f || abs(panDelta.y) > 0.5f) {
                                        offsetX += panDelta.x
                                        offsetY += panDelta.y
                                        sampler.processPan(panDelta)
                                    }
                                }

                                lastPinchDist = currentDist
                                lastPinchCentroid = centroid
                            } else if (pointerCount == 1) {
                                val x = (event.x - offsetX) / scale
                                val y = (event.y - offsetY) / scale
                                sampler.processMove(Offset(x, y))
                            }
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                            if (pointerCount <= 2) {
                                lastPinchDist = 0f
                            }
                            if (event.action == MotionEvent.ACTION_UP || pointerCount == 1) {
                                val x = (event.x - offsetX) / scale
                                val y = (event.y - offsetY) / scale
                                sampler.processUp(Offset(x, y))
                            }
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            lastPinchDist = 0f
                            true
                        }
                        else -> false
                    }
                }
        )
    }
}

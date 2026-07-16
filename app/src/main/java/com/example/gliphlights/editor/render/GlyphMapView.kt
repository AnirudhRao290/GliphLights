package com.example.gliphlights.editor.render

import android.view.View
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import android.view.MotionEvent
import com.example.gliphlights.editor.gesture.GestureEvent
import com.example.gliphlights.editor.gesture.GestureSampler
import com.example.gliphlights.editor.model.GlyphNodeLayout

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GlyphMapView(
    layout: GlyphNodeLayout,
    activeChannels: Set<Int>,
    onGestureEvent: (GestureEvent) -> Unit,
    modifier: Modifier = Modifier,
    view: View? = null
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var hitNodeId by remember { mutableStateOf<String?>(null) }

    val sampler = remember(layout) {
        GestureSampler(layout) { event ->
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

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val currentLayout = remember(width, height) {
            GlyphNodeLayout(width, height)
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
                .transformable(state = transformState)
                .pointerInteropFilter { event ->
                    val x = event.x / scale - offsetX / scale
                    val y = event.y / scale - offsetY / scale
                    val position = Offset(x, y)

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            sampler.processDown(position)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            sampler.processMove(position)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            sampler.processUp(position)
                            true
                        }
                        else -> false
                    }
                }
        )
    }
}

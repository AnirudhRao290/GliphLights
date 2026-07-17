package com.example.gliphlights.pathbuilder.render

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.pathbuilder.gesture.PathGestureSampler
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings

@Composable
fun PathBuilderMapView(
    layout: GlyphNodeLayout?,
    nodeAlphas: FloatArray,
    liveTrail: List<Offset>,
    livePathNodes: List<PathNode>,
    enteredNodeId: String?,
    drawMode: Boolean,
    settings: PathSettings,
    onLayoutCreated: (GlyphNodeLayout) -> Unit,
    onSample: (Float, Float, Long) -> Unit,
    onNodeEntered: (PathNode) -> Unit,
    onStrokeStart: () -> Unit,
    onStrokeEnd: () -> Unit,
    modifier: Modifier = Modifier,
    view: View? = null
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val currentLayout = remember(width, height) {
            GlyphNodeLayout(width, height).also { onLayoutCreated(it) }
        }

        val settingsState = rememberUpdatedState(settings)
        val onSampleState = rememberUpdatedState(onSample)
        val onNodeEnteredState = rememberUpdatedState(onNodeEntered)
        val onStrokeStartState = rememberUpdatedState(onStrokeStart)
        val onStrokeEndState = rememberUpdatedState(onStrokeEnd)

        val sampler = remember(currentLayout) {
            PathGestureSampler(
                layout = currentLayout,
                settings = { settingsState.value },
                onSample = { x, y, t -> onSampleState.value(x, y, t) },
                onNodeEntered = { onNodeEnteredState.value(it) },
                onStrokeStart = { onStrokeStartState.value() },
                onStrokeEnd = { onStrokeEndState.value() }
            )
        }

        LaunchedEffect(view) {
            sampler.setView(view)
        }

        PathPreviewRenderer(
            layout = currentLayout,
            nodeAlphas = nodeAlphas,
            liveTrail = liveTrail,
            livePathNodes = livePathNodes,
            enteredNodeId = enteredNodeId,
            drawMode = drawMode,
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    if (!drawMode) return@pointerInteropFilter false
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            sampler.processDown(event.x, event.y)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            sampler.processMove(event.x, event.y)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                                sampler.cancel()
                            } else {
                                sampler.processUp(event.x, event.y)
                            }
                            true
                        }
                        else -> false
                    }
                }
        )
    }
}

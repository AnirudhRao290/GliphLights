package com.example.gliphlights.editor.gesture

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.geometry.Offset
import com.example.gliphlights.editor.model.GlyphNodeLayout

class GestureSampler(
    private val layout: GlyphNodeLayout,
    private val onEvent: (GestureEvent) -> Unit
) {
    private var lastHitNodeId: String? = null
    private var isDragging = false
    private var paintMode = false
    private var view: View? = null
    var hapticsEnabled: Boolean = true

    fun setView(view: View) {
        this.view = view
    }

    fun reset() {
        lastHitNodeId = null
        isDragging = false
        paintMode = false
    }

    fun processDown(position: Offset) {
        val node = layout.findNearestNode(position)
        if (node != null) {
            isDragging = true
            paintMode = false
            lastHitNodeId = node.id
            onEvent(GestureEvent.DragStart(position))
            onEvent(GestureEvent.DragEnter(node.id, position))
            performHaptic()
        } else {
            isDragging = false
            lastHitNodeId = null
        }
    }

    fun processMove(position: Offset) {
        if (!isDragging) return

        val node = layout.findPaintTarget(position, lastHitNodeId) ?: return
        if (node.id == lastHitNodeId) return

        paintMode = true
        lastHitNodeId = node.id
        onEvent(GestureEvent.DragEnter(node.id, position))
        performHaptic()
    }

    fun processUp(position: Offset) {
        if (isDragging) {
            onEvent(GestureEvent.DragEnd(position))
        }
        isDragging = false
        paintMode = false
        lastHitNodeId = null
    }

    fun processPan(delta: Offset) {
        onEvent(GestureEvent.Pan(delta))
    }

    fun processZoom(centroid: Offset, zoomFactor: Float) {
        onEvent(GestureEvent.Zoom(centroid, zoomFactor))
    }

    private fun performHaptic() {
        if (!hapticsEnabled) return
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

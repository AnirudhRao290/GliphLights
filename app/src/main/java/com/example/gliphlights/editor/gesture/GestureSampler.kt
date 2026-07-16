package com.example.gliphlights.editor.gesture

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout

class GestureSampler(
    private val layout: GlyphNodeLayout,
    private val onEvent: (GestureEvent) -> Unit
) {
    private var lastHitNodeId: String? = null
    private var isDragging = false
    private var dragStartNode: GlyphNode? = null
    private var view: View? = null

    fun setView(view: View) {
        this.view = view
    }

    fun reset() {
        lastHitNodeId = null
        isDragging = false
        dragStartNode = null
    }

    fun processDown(position: Offset) {
        val node = layout.findNearestNode(position, maxDistance = 50f)
        if (node != null) {
            isDragging = true
            dragStartNode = node
            lastHitNodeId = node.id
            onEvent(GestureEvent.DragStart(position))
            onEvent(GestureEvent.DragEnter(node.id, position))
            performHaptic()
        }
    }

    fun processMove(position: Offset) {
        if (!isDragging) return

        val node = layout.findNearestNode(position, maxDistance = 50f)
        if (node != null && node.id != lastHitNodeId) {
            lastHitNodeId = node.id
            onEvent(GestureEvent.DragEnter(node.id, position))
            performHaptic()
        }
    }

    fun processUp(position: Offset) {
        if (isDragging) {
            val dragNode = dragStartNode
            if (dragNode != null && lastHitNodeId == dragNode.id) {
                val dist = (position - dragNode.position).getDistance()
                if (dist < 10f) {
                    onEvent(GestureEvent.Tap(position))
                }
            }
            onEvent(GestureEvent.DragEnd(position))
        } else {
            val node = layout.findNearestNode(position, maxDistance = 50f)
            if (node != null) {
                onEvent(GestureEvent.Tap(position))
            }
        }

        isDragging = false
        lastHitNodeId = null
        dragStartNode = null
    }

    fun processPan(delta: Offset) {
        onEvent(GestureEvent.Pan(delta))
    }

    fun processZoom(centroid: Offset, zoomFactor: Float) {
        onEvent(GestureEvent.Zoom(centroid, zoomFactor))
    }

    private fun performHaptic() {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

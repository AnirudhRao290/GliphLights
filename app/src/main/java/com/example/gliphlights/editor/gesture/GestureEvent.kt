package com.example.gliphlights.editor.gesture

import androidx.compose.ui.geometry.Offset

sealed class GestureEvent {
    data class Tap(val position: Offset) : GestureEvent()
    data class DragStart(val position: Offset) : GestureEvent()
    data class DragEnter(val nodeId: String, val position: Offset) : GestureEvent()
    data class DragEnd(val position: Offset) : GestureEvent()
    data class Pan(val delta: Offset) : GestureEvent()
    data class Zoom(val centroid: Offset, val zoomFactor: Float) : GestureEvent()
}

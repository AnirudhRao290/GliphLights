package com.example.gliphlights.pathbuilder.gesture

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.geometry.Offset
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.pathbuilder.PathConverter
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings

/**
 * High-frequency draw-mode sampler. Emits raw points and newly entered nodes.
 * Does not talk to the Glyph SDK.
 */
class PathGestureSampler(
    private val layout: GlyphNodeLayout,
    private val settings: () -> PathSettings,
    private val onSample: (x: Float, y: Float, tNanos: Long) -> Unit,
    private val onNodeEntered: (PathNode) -> Unit,
    private val onStrokeStart: () -> Unit,
    private val onStrokeEnd: () -> Unit
) {
    private val converter = PathConverter(layout)
    private var drawing = false
    private var lastNodeId: String? = null
    private var view: View? = null

    // Reused — avoid allocating Offset in the hot path where possible
    private var lastX = 0f
    private var lastY = 0f
    private var hasLast = false

    fun setView(view: View?) {
        this.view = view
    }

    fun updateSettingsProvider(ignored: PathSettings) {
        // settings read lazily via lambda
    }

    fun processDown(x: Float, y: Float) {
        drawing = true
        lastNodeId = null
        hasLast = false
        onStrokeStart()
        ingest(x, y, force = true)
    }

    fun processMove(x: Float, y: Float) {
        if (!drawing) return
        ingest(x, y, force = false)
    }

    fun processUp(x: Float, y: Float) {
        if (!drawing) return
        ingest(x, y, force = true)
        drawing = false
        lastNodeId = null
        hasLast = false
        onStrokeEnd()
    }

    fun cancel() {
        if (!drawing) return
        drawing = false
        lastNodeId = null
        hasLast = false
        onStrokeEnd()
    }

    private fun ingest(x: Float, y: Float, force: Boolean) {
        val minDist = settings().samplingDensityPx.coerceAtLeast(1f)
        if (!force && hasLast) {
            val dx = x - lastX
            val dy = y - lastY
            if (dx * dx + dy * dy < minDist * minDist) return
        }
        lastX = x
        lastY = y
        hasLast = true

        val t = System.nanoTime()
        onSample(x, y, t)

        val node = converter.toNode(Offset(x, y)) ?: return
        if (node.nodeId == lastNodeId) return
        lastNodeId = node.nodeId
        onNodeEntered(node)
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

package com.example.gliphlights.editor.model

import androidx.compose.ui.geometry.Offset

enum class GlyphRegion(
    val displayName: String,
    val channelStart: Int,
    val channelEnd: Int,
    val nodeCount: Int
) {
    // Phone (3a) / (3a) Pro — SDK indices from Glyph Developer Kit
    A(
        displayName = "Region A",
        channelStart = 20,
        channelEnd = 30,
        nodeCount = 11
    ),
    B(
        displayName = "Region B",
        channelStart = 31,
        channelEnd = 35,
        nodeCount = 5
    ),
    C(
        displayName = "Region C",
        channelStart = 0,
        channelEnd = 19,
        nodeCount = 20
    );

    val channels: List<Int>
        get() = (channelStart..channelEnd).toList()

    fun sdkIndex(localIndex: Int): Int = channelStart + localIndex
}

data class GlyphNode(
    val id: String,
    val region: GlyphRegion,
    val localIndex: Int,
    val sdkIndex: Int,
    val position: Offset,
    /** Radial angle on the doughnut (degrees, Compose: 0°=right, 90°=down). */
    val angleDeg: Float,
    /** Bent-tube arc start on the ring (Compose degrees). */
    val tubeStartDeg: Float = angleDeg,
    /** Bent-tube sweep along the ring (degrees). */
    val tubeSweepDeg: Float = 4f,
    /** Doughnut-ring neighbors (includes gap bridges A11↔B1, B5↔C1, C20↔A1). */
    val neighbors: List<String> = emptyList()
)

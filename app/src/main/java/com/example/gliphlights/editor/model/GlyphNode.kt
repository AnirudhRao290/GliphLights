package com.example.gliphlights.editor.model

import androidx.compose.ui.geometry.Offset

enum class GlyphRegion(
    val displayName: String,
    val channelStart: Int,
    val channelEnd: Int,
    val nodeCount: Int,
    val arcCenterAngle: Float,
    val arcSweep: Float
) {
    A(
        displayName = "Region A",
        channelStart = 20,
        channelEnd = 30,
        nodeCount = 11,
        arcCenterAngle = -90f,
        arcSweep = 160f
    ),
    B(
        displayName = "Region B",
        channelStart = 31,
        channelEnd = 35,
        nodeCount = 5,
        arcCenterAngle = -210f,
        arcSweep = 60f
    ),
    C(
        displayName = "Region C",
        channelStart = 0,
        channelEnd = 19,
        nodeCount = 20,
        arcCenterAngle = -135f,
        arcSweep = 270f
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
    val neighbors: List<String> = emptyList()
)

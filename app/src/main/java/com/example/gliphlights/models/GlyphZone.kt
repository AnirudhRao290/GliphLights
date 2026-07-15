package com.example.gliphlights.models

enum class GlyphZone(val displayName: String, val channels: List<Int>) {
    A(
        displayName = "Zone A (Vertical Bar)",
        channels = (20..30).toList() // A1-A11
    ),
    B(
        displayName = "Zone B (Horizontal Bar)",
        channels = (31..35).toList() // B1-B5
    ),
    C(
        displayName = "Zone C (Camera Ring)",
        channels = (0..19).toList() // C1-C20
    );

    val channelCount: Int get() = channels.size

    fun getChannelName(index: Int): String {
        val channelIndex = channels.indexOf(index)
        return if (channelIndex >= 0) {
            "${name}${channelIndex + 1}"
        } else {
            "Channel $index"
        }
    }
}

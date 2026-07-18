package com.example.gliphlights.pathbuilder.model

/**
 * A saved or predefined path sequence that can be reloaded and played later.
 */
data class SavedSequence(
    val id: String,
    val name: String,
    val nodes: List<PathNode>,
    val settings: PathSettings = PathSettings(),
    val createdAtMs: Long = System.currentTimeMillis(),
    val isPreset: Boolean = false
)

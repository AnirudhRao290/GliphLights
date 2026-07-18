package com.example.gliphlights.presets

import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.physics.model.PhysicsMode
import com.example.gliphlights.physics.model.PhysicsParams

enum class PresetType(val badge: String) {
    EDITOR("Frame"),
    PATH("Path"),
    PHYSICS("Physics"),
    VISUALIZER("Viz")
}

/**
 * Unified preset covering Editor frames, Path sequences, Physics recipes, and Visualizer looks.
 */
data class GlyphPreset(
    val id: String,
    val name: String,
    val type: PresetType,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val forkedFromId: String? = null,
    // Editor
    val channels: Set<Int> = emptySet(),
    // Path
    val pathNodes: List<PathNode> = emptyList(),
    val pathSettings: PathSettings = PathSettings(),
    // Physics
    val physicsMode: PhysicsMode? = null,
    val physicsParams: PhysicsParams = PhysicsParams(),
    // Visualizer
    val visualizerMode: String? = null,
    val visualizerSensitivity: Float = 1f
)

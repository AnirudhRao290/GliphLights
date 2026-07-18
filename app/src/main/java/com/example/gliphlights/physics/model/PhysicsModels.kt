package com.example.gliphlights.physics.model

/**
 * Physics Lab simulation modes.
 */
enum class PhysicsMode(val displayName: String, val description: String) {
    GRAVITY("Gravity", "Light flows toward the lowest point"),
    FLUID("Fluid", "Glowing liquid that sloshes and pools"),
    MERCURY("Mercury", "Heavy liquid metal blob"),
    SAND("Sand", "Independent falling grains"),
    BUBBLE("Bubble", "Floats against gravity"),
    MAGNET("Magnet", "Tap to attract particles"),
    ZERO_G("Zero-G", "Drift and push with motion"),
    PINBALL("Pinball", "Bounce along glyph rails")
}

data class Vec2(var x: Float = 0f, var y: Float = 0f) {
    fun set(nx: Float, ny: Float) {
        x = nx
        y = ny
    }

    fun length(): Float = kotlin.math.sqrt(x * x + y * y)

    fun normalizeInPlace() {
        val len = length()
        if (len > 1e-5f) {
            x /= len
            y /= len
        }
    }

    fun copyFrom(o: Vec2) {
        x = o.x
        y = o.y
    }
}

/**
 * Filtered sensor sample mapped for screen physics.
 * Upright phone → gravityY ≈ +9.81 (toward bottom of the Glyph preview).
 */
data class SensorSample(
    val gravityX: Float = 0f,
    val gravityY: Float = 9.81f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val shakeEnergy: Float = 0f,
    val timestampNs: Long = 0L
)

/**
 * Tunable parameters shared across modes (subset applied per mode).
 */
data class PhysicsParams(
    val gravityStrength: Float = 1f,
    val flowSpeed: Float = 1f,
    val damping: Float = 0.85f,
    val trailLength: Int = 3,
    val brightness: Float = 1f,
    val fluidAmount: Float = 0.45f,
    val viscosity: Float = 0.55f,
    val surfaceTension: Float = 0.4f,
    val gravityMultiplier: Float = 1f,
    val energyLoss: Float = 0.92f,
    val glowIntensity: Float = 1f,
    val simulationSpeed: Float = 1f,
    val particleCount: Int = 48
)

/**
 * Reusable particle (object pool — mutate in place).
 */
class Particle {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var mass: Float = 1f
    var nodeIndex: Int = -1
    var energy: Float = 0f

    fun reset() {
        active = false
        x = 0f
        y = 0f
        vx = 0f
        vy = 0f
        mass = 1f
        nodeIndex = -1
        energy = 0f
    }
}

/**
 * Soft blob for fluid merge visualization (index into mass field).
 */
class Blob {
    var active: Boolean = false
    var cx: Float = 0f
    var cy: Float = 0f
    var mass: Float = 0f
    var radius: Float = 0f
}

/**
 * Output frame for preview + SDK — alphas per sdk channel 0..35.
 * No Glyph SDK types here.
 */
data class PhysicsAnimationModel(
    val nodeAlphas: FloatArray = FloatArray(CHANNEL_COUNT),
    val mode: PhysicsMode = PhysicsMode.GRAVITY,
    val shakeEnergy: Float = 0f
) {
    companion object {
        const val CHANNEL_COUNT = 36
        fun empty() = PhysicsAnimationModel()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicsAnimationModel) return false
        return mode == other.mode &&
            shakeEnergy == other.shakeEnergy &&
            nodeAlphas.contentEquals(other.nodeAlphas)
    }

    override fun hashCode(): Int {
        var result = nodeAlphas.contentHashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + shakeEnergy.hashCode()
        return result
    }
}

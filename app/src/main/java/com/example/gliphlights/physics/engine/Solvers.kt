package com.example.gliphlights.physics.engine

import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.physics.model.PhysicsParams
import kotlin.math.max
import kotlin.math.min

/**
 * Doughnut-ring gravity flow. Mass only moves along Glyph A/B/C ring edges
 * (including gap bridges), never jumps across the camera.
 */
class GravitySolver {
    private var heights = FloatArray(0)
    private var neighborIdx = Array(0) { IntArray(0) }

    fun bind(neighborIndices: Array<IntArray>) {
        neighborIdx = neighborIndices
        heights = FloatArray(neighborIndices.size)
    }

    @Deprecated("Use bind(neighborIndices)")
    fun bind(nodes: List<GlyphNode>, idToIndex: Map<String, Int>) {
        heights = FloatArray(nodes.size)
        neighborIdx = Array(nodes.size) { i ->
            nodes[i].neighbors.mapNotNull { idToIndex[it] }.toIntArray()
        }
    }

    fun computeHeights(nodes: List<GlyphNode>, gx: Float, gy: Float) {
        val glen = kotlin.math.sqrt(gx * gx + gy * gy).coerceAtLeast(1e-4f)
        val nx = gx / glen
        val ny = gy / glen
        if (heights.size != nodes.size) heights = FloatArray(nodes.size)
        for (i in nodes.indices) {
            val p = nodes[i].position
            // Higher value = further in gravity direction = "lower" physically
            heights[i] = p.x * nx + p.y * ny
        }
    }

    /**
     * Flow mass toward physically lower doughnut neighbors.
     * Splits flow across all downhill edges for smooth pooling.
     */
    fun flowMass(
        mass: FloatArray,
        scratch: FloatArray,
        params: PhysicsParams,
        dt: Float,
        reverseGravity: Boolean = false
    ): Float {
        val n = mass.size
        if (n == 0 || neighborIdx.size != n) return 0f
        System.arraycopy(mass, 0, scratch, 0, n)

        val speed = params.flowSpeed * params.gravityMultiplier * params.gravityStrength * dt * 10f
        var maxFlow = 0f

        for (i in 0 until n) {
            val neighbors = neighborIdx[i]
            if (neighbors.isEmpty()) continue

            // Collect downhill neighbors with positive drop
            var totalDrop = 0f
            val drops = FloatArray(neighbors.size)
            for (k in neighbors.indices) {
                val j = neighbors[k]
                val drop = if (reverseGravity) {
                    heights[i] - heights[j] // float "up" = against gravity direction
                } else {
                    heights[j] - heights[i] // downhill = higher height scalar along g
                }
                drops[k] = if (drop > 1e-4f) drop else 0f
                totalDrop += drops[k]
            }
            if (totalDrop < 1e-4f) continue

            val available = mass[i]
            if (available <= 1e-4f) continue

            // Move a fraction of mass downhill, proportional to slope
            val leave = min(available * min(1f, speed * 0.15f), available * 0.5f)
            if (leave <= 1e-5f) continue

            for (k in neighbors.indices) {
                if (drops[k] <= 0f) continue
                val share = leave * (drops[k] / totalDrop)
                scratch[i] -= share
                scratch[neighbors[k]] += share
                maxFlow = max(maxFlow, share)
            }
        }

        // Smooth blend so motion never snaps
        val blend = (0.4f + (1f - params.damping) * 0.35f).coerceIn(0.25f, 0.75f)
        for (i in 0 until n) {
            val m = scratch[i].coerceAtLeast(0f)
            mass[i] = mass[i] * (1f - blend) + m * blend
        }
        return maxFlow
    }

    fun neighborIndices(): Array<IntArray> = neighborIdx
    fun heights(): FloatArray = heights

    /** Index of the physically lowest node (pools here when upright). */
    fun lowestIndex(reverseGravity: Boolean = false): Int {
        if (heights.isEmpty()) return 0
        var best = 0
        var bestH = heights[0]
        for (i in 1 until heights.size) {
            val better = if (reverseGravity) heights[i] < bestH else heights[i] > bestH
            if (better) {
                bestH = heights[i]
                best = i
            }
        }
        return best
    }

    fun highestIndex(reverseGravity: Boolean = false): Int = lowestIndex(!reverseGravity)
}

/**
 * Shake / kinetic energy from sensors.
 */
class MotionSolver {
    var kineticEnergy: Float = 0f
        private set

    fun update(sample: com.example.gliphlights.physics.model.SensorSample, params: PhysicsParams, dt: Float) {
        val inject = sample.shakeEnergy * 0.4f * params.simulationSpeed
        kineticEnergy = kineticEnergy * params.energyLoss + inject
        if (kineticEnergy > 20f) kineticEnergy = 20f
        kineticEnergy *= (1f - 0.35f * dt).coerceIn(0.5f, 1f)
    }

    fun consumeSplash(): Float {
        val splash = kineticEnergy
        kineticEnergy *= 0.55f
        return splash
    }
}

/**
 * Fluid / mercury mass field constrained to the doughnut graph.
 */
class FluidSimulation(
    private val gravitySolver: GravitySolver
) {
    fun step(
        mass: FloatArray,
        scratch: FloatArray,
        nodes: List<GlyphNode>,
        sample: com.example.gliphlights.physics.model.SensorSample,
        params: PhysicsParams,
        dt: Float,
        motion: MotionSolver,
        mercury: Boolean,
        reverseGravity: Boolean
    ) {
        gravitySolver.computeHeights(
            nodes,
            sample.gravityX * params.gravityMultiplier,
            sample.gravityY * params.gravityMultiplier
        )

        val viscosity = if (mercury) {
            (params.viscosity * 1.4f + 0.35f).coerceIn(0.4f, 0.95f)
        } else {
            params.viscosity.coerceIn(0.1f, 0.95f)
        }

        val local = params.copy(
            flowSpeed = params.flowSpeed * (if (mercury) 0.45f else 1f) * (1.2f - viscosity * 0.5f),
            damping = viscosity
        )
        gravitySolver.flowMass(mass, scratch, local, dt, reverseGravity)

        val tension = if (mercury) params.surfaceTension * 1.6f else params.surfaceTension
        if (tension > 0.05f) {
            applySurfaceTension(mass, scratch, tension * dt * 2f, gravitySolver.neighborIndices())
        }

        motion.update(sample, params, dt)
        if (sample.shakeEnergy > 2.5f || motion.kineticEnergy > 3f) {
            splash(mass, scratch, gravitySolver.neighborIndices(), motion.consumeSplash() * (if (mercury) 0.4f else 1f))
        }

        normalizeAmount(mass, params.fluidAmount * mass.size)
    }

    private fun applySurfaceTension(
        mass: FloatArray,
        scratch: FloatArray,
        amount: Float,
        neighbors: Array<IntArray>
    ) {
        System.arraycopy(mass, 0, scratch, 0, mass.size)
        for (i in mass.indices) {
            val m = mass[i]
            val neigh = neighbors.getOrNull(i) ?: continue
            if (neigh.isEmpty()) continue
            var sum = 0f
            for (j in neigh) sum += mass[j]
            val avg = sum / neigh.size
            scratch[i] = (m + (m - avg) * amount * 0.5f).coerceAtLeast(0f)
        }
        System.arraycopy(scratch, 0, mass, 0, mass.size)
    }

    private fun splash(
        mass: FloatArray,
        scratch: FloatArray,
        neighbors: Array<IntArray>,
        energy: Float
    ) {
        if (energy < 0.5f) return
        System.arraycopy(mass, 0, scratch, 0, mass.size)
        val scatter = (energy * 0.08f).coerceIn(0.02f, 0.35f)
        for (i in mass.indices) {
            val give = mass[i] * scatter
            if (give <= 1e-4f) continue
            val neigh = neighbors.getOrNull(i)
            if (neigh == null || neigh.isEmpty()) continue
            scratch[i] -= give
            val share = give / neigh.size
            for (j in neigh) scratch[j] += share
        }
        System.arraycopy(scratch, 0, mass, 0, mass.size)
    }

    private fun normalizeAmount(mass: FloatArray, target: Float) {
        var sum = 0f
        for (m in mass) sum += m
        if (sum < 1e-4f) {
            val mid = mass.size / 2
            mass[mid] = target * 0.5f
            if (mid + 1 < mass.size) mass[mid + 1] = target * 0.25f
            if (mid - 1 >= 0) mass[mid - 1] = target * 0.25f
            return
        }
        val scale = target / sum
        for (i in mass.indices) mass[i] *= scale
    }
}

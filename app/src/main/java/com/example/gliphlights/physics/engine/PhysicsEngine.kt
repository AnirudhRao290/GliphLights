package com.example.gliphlights.physics.engine

import com.example.gliphlights.editor.model.GlyphNode
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.physics.model.Particle
import com.example.gliphlights.physics.model.PhysicsAnimationModel
import com.example.gliphlights.physics.model.PhysicsMode
import com.example.gliphlights.physics.model.PhysicsParams
import com.example.gliphlights.physics.model.SensorSample
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Real-time physics tick. Produces [PhysicsAnimationModel] only — no Glyph SDK.
 */
class PhysicsEngine {

    private val gravitySolver = GravitySolver()
    private val motionSolver = MotionSolver()
    private val fluid = FluidSimulation(gravitySolver)
    private val generator = AnimationModelGenerator()

    private var nodes: List<GlyphNode> = emptyList()
    private var idToIndex: Map<String, Int> = emptyMap()

    private var mass = FloatArray(0)
    private var scratch = FloatArray(0)
    private var trail = Array(0) { FloatArray(0) }
    private var trailWrite = 0

    private val particles = Array(96) { Particle() }
    private var magnetX = Float.NaN
    private var magnetY = Float.NaN
    private var hasMagnet = false

    private val outAlphas = FloatArray(PhysicsAnimationModel.CHANNEL_COUNT)
    private var lastModel = PhysicsAnimationModel.empty()

    var mode: PhysicsMode = PhysicsMode.GRAVITY
    var params: PhysicsParams = PhysicsParams()

    fun bindLayout(layout: GlyphNodeLayout) {
        nodes = layout.nodes
        idToIndex = nodes.mapIndexed { i, n -> n.id to i }.toMap()
        // Physics graph = doughnut ring (A→B→C→A), not isolated region chains
        gravitySolver.bind(layout.doughnutNeighborIndices())
        mass = FloatArray(nodes.size)
        scratch = FloatArray(nodes.size)
        val trails = params.trailLength.coerceIn(1, 8)
        trail = Array(trails) { FloatArray(nodes.size) }
        trailWrite = 0
        seedMass()
        spawnParticles()
    }

    fun setMagnet(x: Float, y: Float) {
        magnetX = x
        magnetY = y
        hasMagnet = true
    }

    fun clearMagnet() {
        hasMagnet = false
        magnetX = Float.NaN
        magnetY = Float.NaN
    }

    fun reset() {
        seedMass()
        spawnParticles()
        motionSolver.consumeSplash()
    }

    fun step(sample: SensorSample, dtRaw: Float): PhysicsAnimationModel {
        if (nodes.isEmpty()) return PhysicsAnimationModel.empty()
        val dt = (dtRaw * params.simulationSpeed).coerceIn(0.008f, 0.05f)

        when (mode) {
            PhysicsMode.GRAVITY -> stepGravity(sample, dt)
            PhysicsMode.FLUID -> fluid.step(mass, scratch, nodes, sample, params, dt, motionSolver, mercury = false, reverseGravity = false)
            PhysicsMode.MERCURY -> fluid.step(
                mass, scratch, nodes, sample,
                params.copy(viscosity = (params.viscosity + 0.35f).coerceAtMost(0.95f), surfaceTension = params.surfaceTension + 0.25f),
                dt, motionSolver, mercury = true, reverseGravity = false
            )
            PhysicsMode.BUBBLE -> fluid.step(mass, scratch, nodes, sample, params, dt, motionSolver, mercury = false, reverseGravity = true)
            PhysicsMode.SAND -> stepParticles(sample, dt, sand = true)
            PhysicsMode.ZERO_G -> stepParticles(sample, dt, sand = false, zeroG = true)
            PhysicsMode.PINBALL -> stepParticles(sample, dt, sand = false, pinball = true)
            PhysicsMode.MAGNET -> {
                fluid.step(mass, scratch, nodes, sample, params, dt, motionSolver, mercury = false, reverseGravity = false)
                if (hasMagnet) attractMassToMagnet(dt)
            }
        }

        pushTrail()
        when {
            mode.usesParticles() -> generator.fillFromParticles(particles, nodes, params, outAlphas)
            else -> generator.fillAlphasForNodes(nodes, mass, trail, trailWrite, params, outAlphas)
        }

        lastModel = PhysicsAnimationModel(
            nodeAlphas = outAlphas.copyOf(),
            mode = mode,
            shakeEnergy = sample.shakeEnergy
        )
        return lastModel
    }

    fun latestModel(): PhysicsAnimationModel = lastModel

    private fun PhysicsMode.usesParticles(): Boolean =
        this == PhysicsMode.SAND || this == PhysicsMode.ZERO_G || this == PhysicsMode.PINBALL

    private fun stepGravity(sample: SensorSample, dt: Float) {
        gravitySolver.computeHeights(
            nodes,
            sample.gravityX * params.gravityMultiplier,
            sample.gravityY * params.gravityMultiplier
        )
        var sum = 0f
        for (m in mass) sum += m
        if (sum < 0.5f) seedMass()

        gravitySolver.flowMass(mass, scratch, params, dt, reverseGravity = false)
        motionSolver.update(sample, params, dt)
        normalizeSoft(mass, params.fluidAmount.coerceIn(0.2f, 0.8f) * mass.size)
    }

    private fun stepParticles(
        sample: SensorSample,
        dt: Float,
        sand: Boolean,
        zeroG: Boolean = false,
        pinball: Boolean = false
    ) {
        motionSolver.update(sample, params, dt)
        val gx = if (zeroG) 0f else sample.gravityX * params.gravityMultiplier * params.gravityStrength
        val gy = if (zeroG) 0f else sample.gravityY * params.gravityMultiplier * params.gravityStrength
        val shake = sample.shakeEnergy

        val pushX = if (zeroG) sample.accelX * 0.8f else 0f
        val pushY = if (zeroG) sample.accelY * 0.8f else 0f

        mass.fill(0f)
        val ringNeighbors = gravitySolver.neighborIndices()

        for (p in particles) {
            if (!p.active) continue
            if (sand || !zeroG) {
                p.vx += gx * dt * 40f
                p.vy += gy * dt * 40f
            }
            p.vx += pushX * dt * 30f
            p.vy += pushY * dt * 30f

            if (shake > 2f) {
                p.vx += (Random.nextFloat() - 0.5f) * shake * 8f * dt * 60f
                p.vy += (Random.nextFloat() - 0.5f) * shake * 8f * dt * 60f
            }

            p.vx *= params.damping
            p.vy *= params.damping
            p.x += p.vx * dt * 60f
            p.y += p.vy * dt * 60f

            // Constrain to doughnut: prefer current ring neighbors, else nearest node
            val nearest = nearestOnDoughnut(p.x, p.y, p.nodeIndex, ringNeighbors)
            if (nearest >= 0) {
                val node = nodes[nearest]
                val dx = node.position.x - p.x
                val dy = node.position.y - p.y
                val dist = sqrt(dx * dx + dy * dy)
                if (pinball && dist < layoutHitRadius()) {
                    val nx = if (dist > 1e-3f) dx / dist else 0f
                    val ny = if (dist > 1e-3f) dy / dist else -1f
                    val dot = p.vx * nx + p.vy * ny
                    if (dot > 0f) {
                        p.vx -= 1.6f * dot * nx
                        p.vy -= 1.6f * dot * ny
                    }
                    p.vx *= 0.98f
                    p.vy *= 0.98f
                } else {
                    // Snap onto the ring segment
                    p.x += dx * 0.35f
                    p.y += dy * 0.35f
                    p.nodeIndex = nearest
                }
                if (p.nodeIndex in mass.indices) {
                    mass[p.nodeIndex] += p.mass * 0.35f
                }
            }
        }
    }

    private fun attractMassToMagnet(dt: Float) {
        if (!hasMagnet) return
        val idx = nearestNodeIndex(magnetX, magnetY)
        if (idx < 0) return
        // Pull only along doughnut edges toward magnet node
        val neigh = gravitySolver.neighborIndices()
        for (i in mass.indices) {
            if (i == idx) continue
            val connected = neigh.getOrNull(i)?.contains(idx) == true ||
                pathDistanceOnRing(i, idx) <= 4
            if (!connected) continue
            val pull = mass[i] * params.flowSpeed * dt * 1.5f
            if (pull <= 1e-4f) continue
            mass[i] -= pull
            mass[idx] += pull
        }
    }

    private fun pathDistanceOnRing(from: Int, to: Int): Int {
        val visited = BooleanArray(nodes.size)
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(from to 0)
        visited[from] = true
        val neigh = gravitySolver.neighborIndices()
        while (queue.isNotEmpty()) {
            val (i, d) = queue.removeFirst()
            if (i == to) return d
            if (d >= 20) continue
            for (j in neigh.getOrNull(i) ?: intArrayOf()) {
                if (!visited[j]) {
                    visited[j] = true
                    queue.add(j to d + 1)
                }
            }
        }
        return 99
    }

    private fun nearestOnDoughnut(
        x: Float,
        y: Float,
        current: Int,
        ringNeighbors: Array<IntArray>
    ): Int {
        val candidates = buildList {
            if (current in ringNeighbors.indices) {
                add(current)
                ringNeighbors[current].forEach { add(it) }
                ringNeighbors[current].forEach { n ->
                    ringNeighbors.getOrNull(n)?.forEach { add(it) }
                }
            }
        }.distinct()
        if (candidates.isEmpty()) return nearestNodeIndex(x, y)
        return candidates.minByOrNull { i ->
            val p = nodes[i].position
            val dx = p.x - x
            val dy = p.y - y
            dx * dx + dy * dy
        } ?: nearestNodeIndex(x, y)
    }

    private fun nearestNodeIndex(x: Float, y: Float): Int {
        var best = -1
        var bestD = Float.MAX_VALUE
        for (i in nodes.indices) {
            val p = nodes[i].position
            val dx = p.x - x
            val dy = p.y - y
            val d = dx * dx + dy * dy
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        return best
    }

    private fun layoutHitRadius(): Float = 28f

    private fun seedMass() {
        if (mass.isEmpty() || nodes.isEmpty()) return
        mass.fill(0f)
        // Seed at top of doughnut (against upright gravity) so mass flows to the bottom along the ring
        gravitySolver.computeHeights(nodes, 0f, 1f)
        val top = gravitySolver.highestIndex()
        val target = params.fluidAmount * mass.size
        mass[top] = target * 0.55f
        val neighbors = gravitySolver.neighborIndices().getOrNull(top)
        if (neighbors != null && neighbors.isNotEmpty()) {
            val share = (target * 0.45f) / neighbors.size
            for (j in neighbors) mass[j] += share
        }
    }

    private fun spawnParticles() {
        if (nodes.isEmpty()) return
        val count = params.particleCount.coerceIn(8, particles.size)
        for (i in particles.indices) {
            val p = particles[i]
            if (i < count) {
                val node = nodes[i % nodes.size]
                p.active = true
                p.x = node.position.x + (Random.nextFloat() - 0.5f) * 8f
                p.y = node.position.y + (Random.nextFloat() - 0.5f) * 8f
                p.vx = 0f
                p.vy = 0f
                p.mass = 1f
                p.nodeIndex = i % nodes.size
            } else {
                p.reset()
            }
        }
    }

    private fun pushTrail() {
        if (trail.isEmpty()) return
        System.arraycopy(mass, 0, trail[trailWrite % trail.size], 0, mass.size)
        trailWrite++
    }

    private fun normalizeSoft(mass: FloatArray, target: Float) {
        var sum = 0f
        for (m in mass) sum += m
        if (sum < 1e-3f) {
            seedMass()
            return
        }
        val scale = target / sum
        for (i in mass.indices) mass[i] *= scale
    }
}

/**
 * Converts mass / particles / trails → channel alphas.
 */
class AnimationModelGenerator {

    fun fillAlphasForNodes(
        nodes: List<GlyphNode>,
        mass: FloatArray,
        trail: Array<FloatArray>,
        trailWrite: Int,
        params: PhysicsParams,
        out: FloatArray
    ) {
        out.fill(0f)
        var maxM = 1e-4f
        for (m in mass) if (m > maxM) maxM = m

        val trailCount = trail.size.coerceAtLeast(1)
        for (i in nodes.indices) {
            val sdk = nodes[i].sdkIndex
            if (sdk !in out.indices) continue
            var a = (mass[i] / maxM).coerceIn(0f, 1f)
            for (t in 0 until trailCount) {
                val idx = ((trailWrite - 1 - t) % trailCount + trailCount) % trailCount
                val ghost = if (i < trail[idx].size) trail[idx][i] / maxM else 0f
                val fade = (1f - t.toFloat() / trailCount) * 0.35f
                a = maxOf(a, ghost.coerceIn(0f, 1f) * fade)
            }
            out[sdk] = (a * params.brightness * params.glowIntensity).coerceIn(0f, 1f)
        }
    }

    fun fillFromParticles(
        particles: Array<Particle>,
        nodes: List<GlyphNode>,
        params: PhysicsParams,
        out: FloatArray
    ) {
        out.fill(0f)
        val counts = FloatArray(nodes.size)
        for (p in particles) {
            if (!p.active) continue
            val idx = p.nodeIndex
            if (idx in counts.indices) counts[idx] += p.mass
        }
        var maxC = 1e-4f
        for (c in counts) if (c > maxC) maxC = c
        for (i in nodes.indices) {
            val sdk = nodes[i].sdkIndex
            if (sdk in out.indices) {
                out[sdk] = ((counts[i] / maxC) * params.brightness * params.glowIntensity).coerceIn(0f, 1f)
            }
        }
    }
}

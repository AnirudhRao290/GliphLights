package com.example.gliphlights.physics.sensor

import com.example.gliphlights.physics.model.SensorSample
import kotlin.math.sqrt

/**
 * Low-pass / complementary filtering to kill sensor jitter.
 */
class SensorFilter(
    private var alpha: Float = 0.18f
) {
    private var gx = 0f
    private var gy = 9.81f
    private var gz = 0f
    private var ax = 0f
    private var ay = 0f
    private var az = 0f
    private var shake = 0f
    private var lastAx = 0f
    private var lastAy = 0f
    private var lastAz = 0f
    private var initialized = false

    fun setSmoothing(amount: Float) {
        alpha = amount.coerceIn(0.05f, 0.5f)
    }

    fun pushGravity(x: Float, y: Float, z: Float) {
        // Android gravity/accel already report +Y when the phone is upright
        // (toward the top of the device). Compose +Y is down the screen, so
        // keeping the sign maps physical "down" to larger Compose Y.
        val sx = x
        val sy = y
        val sz = z
        if (!initialized) {
            gx = sx; gy = sy; gz = sz
            initialized = true
            return
        }
        gx = lerp(gx, sx)
        gy = lerp(gy, sy)
        gz = lerp(gz, sz)
    }

    fun pushAccelerometer(x: Float, y: Float, z: Float) {
        val sx = x
        val sy = y
        val sz = z
        if (!initialized) {
            ax = sx; ay = sy; az = sz
            lastAx = sx; lastAy = sy; lastAz = sz
            return
        }
        ax = lerp(ax, sx)
        ay = lerp(ay, sy)
        az = lerp(az, sz)

        val dx = sx - lastAx
        val dy = sy - lastAy
        val dz = sz - lastAz
        lastAx = sx
        lastAy = sy
        lastAz = sz
        val jerk = sqrt(dx * dx + dy * dy + dz * dz)
        // Shake energy decays slowly, spikes on motion
        shake = shake * 0.92f + jerk * 0.35f
        if (shake > 12f) shake = 12f
    }

    fun pushRotationVector(@Suppress("UNUSED_PARAMETER") values: FloatArray) {
        // Reserved for future orientation fusion; gravity sensor covers tilt.
    }

    fun sample(timestampNs: Long = System.nanoTime()): SensorSample {
        return SensorSample(
            gravityX = gx,
            gravityY = gy,
            accelX = ax,
            accelY = ay,
            accelZ = az,
            shakeEnergy = shake,
            timestampNs = timestampNs
        )
    }

    fun reset() {
        initialized = false
        shake = 0f
    }

    private fun lerp(current: Float, target: Float): Float =
        current + (target - current) * alpha
}

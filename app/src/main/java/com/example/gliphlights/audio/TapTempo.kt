package com.example.gliphlights.audio

/**
 * Simple tap-tempo estimator. Tap repeatedly; returns BPM after ≥2 taps.
 */
class TapTempo(
    private val maxIntervalMs: Long = 2_000L,
    private val historySize: Int = 8
) {
    private val taps = ArrayDeque<Long>(historySize)

    fun reset() {
        taps.clear()
    }

    /**
     * Registers a tap at [nowMs]. Returns estimated BPM, or null until enough taps.
     */
    fun tap(nowMs: Long = System.currentTimeMillis()): Float? {
        if (taps.isNotEmpty() && nowMs - taps.last() > maxIntervalMs) {
            taps.clear()
        }
        taps.addLast(nowMs)
        while (taps.size > historySize) taps.removeFirst()
        if (taps.size < 2) return null

        val intervals = ArrayList<Long>(taps.size - 1)
        for (i in 1 until taps.size) {
            intervals.add(taps.elementAt(i) - taps.elementAt(i - 1))
        }
        val avg = intervals.average()
        if (avg <= 1.0) return null
        return (60_000.0 / avg).toFloat().coerceIn(40f, 240f)
    }

    /** Maps BPM to a Path node-duration scale relative to a 120 BPM baseline. */
    fun nodeDurationForBpm(bpm: Float, baselineMs: Long = 120L, baselineBpm: Float = 120f): Long {
        val scale = baselineBpm / bpm.coerceIn(40f, 240f)
        return (baselineMs * scale).toLong().coerceIn(40L, 400L)
    }

    /** Tempo multiplier for Perform / animationSpeed (1f = 120 BPM feel). */
    fun tempoMultiplier(bpm: Float, baselineBpm: Float = 120f): Float =
        (bpm / baselineBpm).coerceIn(0.25f, 2.5f)
}

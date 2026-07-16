package com.example.gliphlights.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalProcessor @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var emaAmplitude = 0.0f
    private var emaAlpha = 0.3f
    private var noiseGateThreshold = 0.05f
    private var sensitivity = 1.0f

    private val _amplitude = MutableStateFlow(0.0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _rawAmplitude = MutableStateFlow(0.0f)
    val rawAmplitude: StateFlow<Float> = _rawAmplitude.asStateFlow()

    private val _peakAmplitude = MutableStateFlow(0.0f)
    val peakAmplitude: StateFlow<Float> = _peakAmplitude.asStateFlow()

    fun setEmaAlpha(alpha: Float) {
        emaAlpha = alpha.coerceIn(0.01f, 1.0f)
    }

    fun setNoiseGateThreshold(threshold: Float) {
        noiseGateThreshold = threshold.coerceIn(0.0f, 1.0f)
    }

    fun setSensitivity(factor: Float) {
        sensitivity = factor.coerceIn(0.1f, 3.0f)
    }

    fun processBuffer(buffer: ShortArray) {
        val rms = calculateRms(buffer)
        val peak = calculatePeak(buffer)

        _rawAmplitude.value = rms
        _peakAmplitude.value = peak

        val scaled = (rms * sensitivity).coerceIn(0.0f, 1.0f)

        val gated = if (scaled < noiseGateThreshold) 0.0f else scaled

        emaAmplitude = emaAlpha * gated + (1.0f - emaAlpha) * emaAmplitude
        _amplitude.value = emaAmplitude
    }

    fun reset() {
        emaAmplitude = 0.0f
        _amplitude.value = 0.0f
        _rawAmplitude.value = 0.0f
        _peakAmplitude.value = 0.0f
    }

    fun destroy() {
        scope.cancel()
    }

    private fun calculateRms(buffer: ShortArray): Float {
        if (buffer.isEmpty()) return 0.0f

        var sumSquares = 0.0
        for (sample in buffer) {
            val normalized = sample.toFloat() / Short.MAX_VALUE
            sumSquares += normalized * normalized
        }
        val rms = kotlin.math.sqrt(sumSquares / buffer.size).toFloat()
        return rms.coerceIn(0.0f, 1.0f)
    }

    private fun calculatePeak(buffer: ShortArray): Float {
        if (buffer.isEmpty()) return 0.0f

        var maxAbs = 0
        for (sample in buffer) {
            val abs = kotlin.math.abs(sample.toInt())
            if (abs > maxAbs) maxAbs = abs
        }
        return (maxAbs.toFloat() / Short.MAX_VALUE).coerceIn(0.0f, 1.0f)
    }
}
